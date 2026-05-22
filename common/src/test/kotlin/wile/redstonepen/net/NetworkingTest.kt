package wile.redstonepen.net

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.netty.buffer.Unpooled
import java.util.function.BiConsumer
import java.util.function.Consumer
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

// Abstract helpers that satisfy Objenesis construction (constructors never called in mocks).
private abstract class ReceiverBlockEntity :
    BlockEntity(null, BlockPos.ZERO, null), Networking.IPacketTileNotifyReceiver

private abstract class SyncableMenu :
    AbstractContainerMenu(null, 0), Networking.INetworkSynchronisableContainer

private fun payload(id: String, build: CompoundTag.() -> Unit = {}): Networking.UnifiedPayload =
    Networking.UnifiedPayload(Networking.UnifiedPayload.UnifiedData(id, CompoundTag().apply(build)))

private fun serverPlayer(world: ServerLevel, menu: AbstractContainerMenu? = null): ServerPlayer {
    val server = mockk<MinecraftServer>(relaxed = true)
    every { server.execute(any()) } answers { firstArg<Runnable>().run() }
    val player = mockk<ServerPlayer>(relaxed = true)
    every { player.serverLevel() } returns world
    setField(player, "server", server)
    setField(player, "containerMenu", menu ?: mockk<AbstractContainerMenu>(relaxed = true))
    return player
}

private fun localPlayer(world: Level, menu: AbstractContainerMenu? = null): LocalPlayer {
    val player = mockk<LocalPlayer>(relaxed = true)
    every { player.level() } returns world
    setField(player, "containerMenu", menu ?: mockk<AbstractContainerMenu>(relaxed = true))
    return player
}

private fun resetOverlayHandler() {
    val field = Networking.OverlayTextMessage::class.java.getDeclaredField("handler")
    field.isAccessible = true
    field.set(null, null)
}

class NetworkingTest :
    DescribeSpec({
        beforeEach {
            TestNetworkingPlatform.reset()
            Networking.PacketNbtNotifyClientToServer.handlers.clear()
            Networking.PacketNbtNotifyServerToClient.handlers.clear()
            resetOverlayHandler()
        }

        afterEach { unmockkAll() }

        // ------------------------------------------------------------------
        // handleServerReceive
        // ------------------------------------------------------------------

        describe("handleServerReceive") {
            describe("PacketTileNotifyClientToServer") {
                it("dispatches to onClientPacketReceived with the correct player and nbt") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    val te = mockk<ReceiverBlockEntity>()
                    val pos = BlockPos(4, 5, 6)
                    every { world.getBlockEntity(pos) } returns te

                    val received = mutableListOf<Pair<Player, CompoundTag>>()
                    every { te.onClientPacketReceived(any(), any()) } answers
                        {
                            received += firstArg<Player>() to secondArg<CompoundTag>()
                        }

                    val nbt = CompoundTag().also { it.putInt("sig", 9) }
                    Networking.handleServerReceive(
                        payload("tnc2s") {
                            putLong("pos", pos.asLong())
                            put("nbt", nbt)
                        },
                        serverPlayer(world),
                    )

                    received.size shouldBe 1
                    received[0].second.getInt("sig") shouldBe 9
                }

                it("is a no-op when block entity is null") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    every { world.getBlockEntity(any()) } returns null
                    Networking.handleServerReceive(
                        payload("tnc2s") {
                            putLong("pos", BlockPos.ZERO.asLong())
                            put("nbt", CompoundTag())
                        },
                        serverPlayer(world),
                    )
                    // no exception, dispatch skipped
                }

                it("is a no-op when block entity does not implement IPacketTileNotifyReceiver") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    every { world.getBlockEntity(any()) } returns mockk<BlockEntity>(relaxed = true)
                    Networking.handleServerReceive(
                        payload("tnc2s") {
                            putLong("pos", BlockPos.ZERO.asLong())
                            put("nbt", CompoundTag())
                        },
                        serverPlayer(world),
                    )
                }
            }

            describe("PacketContainerSyncClientToServer") {
                it("dispatches to onClientPacketReceived when container ID matches") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    val menu = mockk<SyncableMenu>(relaxed = true)
                    setField(menu, "containerId", 7)

                    val received = mutableListOf<Triple<Int, Player, CompoundTag>>()
                    every { menu.onClientPacketReceived(any(), any(), any()) } answers
                        {
                            received +=
                                Triple(
                                    firstArg<Int>(),
                                    secondArg<Player>(),
                                    thirdArg<CompoundTag>(),
                                )
                        }

                    val nbt = CompoundTag().also { it.putString("k", "v") }
                    Networking.handleServerReceive(
                        payload("csc2s") {
                            putInt("cid", 7)
                            put("nbt", nbt)
                        },
                        serverPlayer(world, menu),
                    )

                    received.size shouldBe 1
                    assertSoftly {
                        received[0].first shouldBe 7
                        received[0].third.getString("k") shouldBe "v"
                    }
                }

                it("is a no-op when container ID does not match") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    val menu = mockk<SyncableMenu>(relaxed = true)
                    setField(menu, "containerId", 7)

                    val called = mutableListOf<Unit>()
                    every { menu.onClientPacketReceived(any(), any(), any()) } answers
                        {
                            called += Unit
                        }

                    Networking.handleServerReceive(
                        payload("csc2s") {
                            putInt("cid", 99)
                            put("nbt", CompoundTag())
                        },
                        serverPlayer(world, menu),
                    )

                    called shouldBe emptyList()
                }

                it("is a no-op when menu does not implement INetworkSynchronisableContainer") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    val menu = mockk<AbstractContainerMenu>(relaxed = true)
                    setField(menu, "containerId", 7)

                    Networking.handleServerReceive(
                        payload("csc2s") {
                            putInt("cid", 7)
                            put("nbt", CompoundTag())
                        },
                        serverPlayer(world, menu),
                    )
                }
            }

            describe("PacketNbtNotifyClientToServer") {
                it("calls the registered handler with the player and nbt") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    val received = mutableListOf<Pair<Player, CompoundTag>>()
                    Networking.PacketNbtNotifyClientToServer.handlers["hnd"] =
                        BiConsumer { p, nbt ->
                            received += p to nbt
                        }

                    val nbt = CompoundTag().also { it.putInt("sig", 15) }
                    Networking.handleServerReceive(
                        payload("nnc2s") {
                            putString("hnd", "hnd")
                            put("nbt", nbt)
                        },
                        serverPlayer(world),
                    )

                    received.size shouldBe 1
                    received[0].second.getInt("sig") shouldBe 15
                }

                it("is a no-op for an unregistered handler name") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    Networking.handleServerReceive(
                        payload("nnc2s") {
                            putString("hnd", "no-such")
                            put("nbt", CompoundTag())
                        },
                        serverPlayer(world),
                    )
                }

                it("is a no-op when handler name is empty") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    val called = mutableListOf<Unit>()
                    Networking.PacketNbtNotifyClientToServer.handlers[""] = BiConsumer { _, _ ->
                        called += Unit
                    }

                    Networking.handleServerReceive(
                        payload("nnc2s") {
                            putString("hnd", "")
                            put("nbt", CompoundTag())
                        },
                        serverPlayer(world),
                    )

                    called shouldBe emptyList()
                }
            }

            it("is a no-op for an unknown packet ID") {
                val world = mockk<ServerLevel>(relaxed = true)
                Networking.handleServerReceive(payload("xyzzy"), serverPlayer(world))
            }
        }

        // ------------------------------------------------------------------
        // handleClientReceive
        // ------------------------------------------------------------------

        describe("handleClientReceive") {
            describe("PacketTileNotifyServerToClient") {
                it("dispatches to onServerPacketReceived with the correct nbt") {
                    val world = mockk<Level>(relaxed = true)
                    val te = mockk<ReceiverBlockEntity>()
                    val pos = BlockPos(1, 64, 1)
                    every { world.getBlockEntity(pos) } returns te

                    val received = mutableListOf<CompoundTag>()
                    every { te.onServerPacketReceived(any()) } answers
                        {
                            received += firstArg<CompoundTag>()
                        }

                    val nbt = CompoundTag().also { it.putFloat("power", 15f) }
                    Networking.handleClientReceive(
                        payload("tns2c") {
                            putLong("pos", pos.asLong())
                            put("nbt", nbt)
                        },
                        localPlayer(world),
                    )

                    received.size shouldBe 1
                    received[0].getFloat("power") shouldBe 15f
                }

                it("is a no-op when block entity is null") {
                    val world = mockk<Level>(relaxed = true)
                    every { world.getBlockEntity(any()) } returns null
                    Networking.handleClientReceive(
                        payload("tns2c") {
                            putLong("pos", BlockPos.ZERO.asLong())
                            put("nbt", CompoundTag())
                        },
                        localPlayer(world),
                    )
                }

                it("is a no-op when block entity does not implement IPacketTileNotifyReceiver") {
                    val world = mockk<Level>(relaxed = true)
                    every { world.getBlockEntity(any()) } returns mockk<BlockEntity>(relaxed = true)
                    Networking.handleClientReceive(
                        payload("tns2c") {
                            putLong("pos", BlockPos.ZERO.asLong())
                            put("nbt", CompoundTag())
                        },
                        localPlayer(world),
                    )
                }
            }

            describe("PacketContainerSyncServerToClient") {
                it("dispatches to onServerPacketReceived when container ID matches") {
                    val world = mockk<Level>(relaxed = true)
                    val menu = mockk<SyncableMenu>(relaxed = true)
                    setField(menu, "containerId", 3)

                    val received = mutableListOf<Pair<Int, CompoundTag>>()
                    every { menu.onServerPacketReceived(any(), any()) } answers
                        {
                            received += firstArg<Int>() to secondArg<CompoundTag>()
                        }

                    val nbt = CompoundTag().also { it.putBoolean("open", true) }
                    Networking.handleClientReceive(
                        payload("css2c") {
                            putInt("cid", 3)
                            put("nbt", nbt)
                        },
                        localPlayer(world, menu),
                    )

                    received.size shouldBe 1
                    assertSoftly {
                        received[0].first shouldBe 3
                        received[0].second.getBoolean("open") shouldBe true
                    }
                }

                it("is a no-op when container ID does not match") {
                    val world = mockk<Level>(relaxed = true)
                    val menu = mockk<SyncableMenu>(relaxed = true)
                    setField(menu, "containerId", 3)

                    val called = mutableListOf<Unit>()
                    every { menu.onServerPacketReceived(any(), any()) } answers { called += Unit }

                    Networking.handleClientReceive(
                        payload("css2c") {
                            putInt("cid", 99)
                            put("nbt", CompoundTag())
                        },
                        localPlayer(world, menu),
                    )

                    called shouldBe emptyList()
                }

                it("is a no-op when menu does not implement INetworkSynchronisableContainer") {
                    val world = mockk<Level>(relaxed = true)
                    val menu = mockk<AbstractContainerMenu>(relaxed = true)
                    setField(menu, "containerId", 5)

                    Networking.handleClientReceive(
                        payload("css2c") {
                            putInt("cid", 5)
                            put("nbt", CompoundTag())
                        },
                        localPlayer(world, menu),
                    )
                }
            }

            describe("PacketNbtNotifyServerToClient") {
                it("calls the registered handler with the nbt") {
                    val world = mockk<Level>(relaxed = true)
                    val received = mutableListOf<CompoundTag>()
                    Networking.PacketNbtNotifyServerToClient.handlers["hnd"] = Consumer { nbt ->
                        received += nbt
                    }

                    val nbt = CompoundTag().also { it.putLong("ts", 42L) }
                    Networking.handleClientReceive(
                        payload("nns2c") {
                            putString("hnd", "hnd")
                            put("nbt", nbt)
                        },
                        localPlayer(world),
                    )

                    received.size shouldBe 1
                    received[0].getLong("ts") shouldBe 42L
                }

                it("is a no-op for an unregistered handler name") {
                    val world = mockk<Level>(relaxed = true)
                    Networking.handleClientReceive(
                        payload("nns2c") {
                            putString("hnd", "no-such")
                            put("nbt", CompoundTag())
                        },
                        localPlayer(world),
                    )
                }

                it("is a no-op when handler name is empty") {
                    val world = mockk<Level>(relaxed = true)
                    val called = mutableListOf<Unit>()
                    Networking.PacketNbtNotifyServerToClient.handlers[""] = Consumer { _ ->
                        called += Unit
                    }

                    Networking.handleClientReceive(
                        payload("nns2c") {
                            putString("hnd", "")
                            put("nbt", CompoundTag())
                        },
                        localPlayer(world),
                    )

                    called shouldBe emptyList()
                }
            }

            describe("OverlayTextMessage") {
                it("calls handler with the component and delay when payload is valid") {
                    val world = mockk<Level>(relaxed = true)
                    val received = mutableListOf<Pair<Component, Int>>()
                    Networking.OverlayTextMessage.setHandler(
                        BiConsumer { c, d -> received += c to d }
                    )

                    // Bad JSON triggers the catch block; handler still fires with the fallback
                    // component.
                    Networking.handleClientReceive(
                        payload("otms2c") {
                            putInt("delay", 2000)
                            putString("msg", "not-json")
                        },
                        localPlayer(world),
                    )

                    received.size shouldBe 1
                    received[0].second shouldBe 2000
                }

                it("is a no-op when delay is zero") {
                    val world = mockk<Level>(relaxed = true)
                    val called = mutableListOf<Unit>()
                    Networking.OverlayTextMessage.setHandler(BiConsumer { _, _ -> called += Unit })

                    Networking.handleClientReceive(
                        payload("otms2c") {
                            putInt("delay", 0)
                            putString("msg", "x")
                        },
                        localPlayer(world),
                    )

                    called shouldBe emptyList()
                }

                it("is a no-op when delay is negative") {
                    val world = mockk<Level>(relaxed = true)
                    val called = mutableListOf<Unit>()
                    Networking.OverlayTextMessage.setHandler(BiConsumer { _, _ -> called += Unit })

                    Networking.handleClientReceive(
                        payload("otms2c") {
                            putInt("delay", -1)
                            putString("msg", "x")
                        },
                        localPlayer(world),
                    )

                    called shouldBe emptyList()
                }

                it("is a no-op when no handler has been registered") {
                    val world = mockk<Level>(relaxed = true)
                    Networking.handleClientReceive(
                        payload("otms2c") {
                            putInt("delay", 1000)
                            putString("msg", "x")
                        },
                        localPlayer(world),
                    )
                }
            }

            it("is a no-op for an unknown packet ID") {
                val world = mockk<Level>(relaxed = true)
                Networking.handleClientReceive(payload("xyzzy"), localPlayer(world))
            }
        }

        // ------------------------------------------------------------------
        // PacketTileNotifyServerToClient — send methods
        // ------------------------------------------------------------------

        describe("PacketTileNotifyServerToClient") {
            describe("sendToPlayer") {
                it("sends a payload encoding the block position and data nbt") {
                    val player = mockk<ServerPlayer>(relaxed = true)
                    val te = mockk<BlockEntity>(relaxed = true)
                    val pos = BlockPos(10, 64, -5)
                    every { te.getBlockPos() } returns pos

                    val nbt = CompoundTag().also { it.putInt("power", 12) }
                    Networking.PacketTileNotifyServerToClient.sendToPlayer(player, te, nbt)

                    val calls = TestNetworkingPlatform.sendToPlayerCalls
                    calls.size shouldBe 1
                    val data = calls[0].second.data
                    assertSoftly {
                        data.id shouldBe "tns2c"
                        BlockPos.of(data.nbt.getLong("pos")) shouldBe pos
                        data.nbt.getCompound("nbt").getInt("power") shouldBe 12
                    }
                }

                it("is a no-op when block entity is null") {
                    Networking.PacketTileNotifyServerToClient.sendToPlayer(
                        mockk(relaxed = true),
                        null,
                        CompoundTag(),
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }

                it("is a no-op when nbt is null") {
                    val te = mockk<BlockEntity>(relaxed = true)
                    every { te.getBlockPos() } returns BlockPos.ZERO
                    Networking.PacketTileNotifyServerToClient.sendToPlayer(
                        mockk(relaxed = true),
                        te,
                        null,
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }
            }

            describe("sendToPlayers") {
                it("broadcasts to all players when the block entity's level is a ServerLevel") {
                    val world = mockk<ServerLevel>(relaxed = true)
                    val te = mockk<BlockEntity>(relaxed = true)
                    every { te.getBlockPos() } returns BlockPos(0, 0, 0)
                    every { te.getLevel() } returns world

                    val nbt = CompoundTag().also { it.putByte("v", 1) }
                    Networking.PacketTileNotifyServerToClient.sendToPlayers(te, nbt)

                    TestNetworkingPlatform.sendToAllPlayersCalls.size shouldBe 1
                    TestNetworkingPlatform.sendToAllPlayersCalls[0]
                        .second
                        .data
                        .nbt
                        .getCompound("nbt")
                        .getByte("v") shouldBe 1.toByte()
                }

                it("is a no-op when block entity level is not a ServerLevel") {
                    val te = mockk<BlockEntity>(relaxed = true)
                    every { te.getLevel() } returns mockk<Level>(relaxed = true)
                    Networking.PacketTileNotifyServerToClient.sendToPlayers(te, CompoundTag())
                    TestNetworkingPlatform.sendToAllPlayersCalls shouldBe emptyList()
                }

                it("is a no-op when block entity has no level") {
                    val te = mockk<BlockEntity>(relaxed = true)
                    every { te.getLevel() } returns null
                    Networking.PacketTileNotifyServerToClient.sendToPlayers(te, CompoundTag())
                    TestNetworkingPlatform.sendToAllPlayersCalls shouldBe emptyList()
                }
            }
        }

        // ------------------------------------------------------------------
        // PacketContainerSyncServerToClient — send methods
        // ------------------------------------------------------------------

        describe("PacketContainerSyncServerToClient") {
            describe("sendToPlayer with explicit window ID") {
                it("sends payload with container ID and data nbt") {
                    val player = mockk<ServerPlayer>(relaxed = true)
                    val nbt = CompoundTag().also { it.putString("code", "b=d") }
                    Networking.PacketContainerSyncServerToClient.sendToPlayer(player, 5, nbt)

                    val calls = TestNetworkingPlatform.sendToPlayerCalls
                    calls.size shouldBe 1
                    val data = calls[0].second.data
                    assertSoftly {
                        data.id shouldBe "css2c"
                        data.nbt.getInt("cid") shouldBe 5
                        data.nbt.getCompound("nbt").getString("code") shouldBe "b=d"
                    }
                }

                it("is a no-op when nbt is null") {
                    Networking.PacketContainerSyncServerToClient.sendToPlayer(
                        mockk(relaxed = true),
                        1,
                        null,
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }

                it("is a no-op when player is null") {
                    Networking.PacketContainerSyncServerToClient.sendToPlayer(
                        null,
                        1,
                        CompoundTag(),
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }
            }

            describe("sendToPlayer with container") {
                it("delegates to the window-ID overload using the container's containerId") {
                    val player = mockk<ServerPlayer>(relaxed = true)
                    val menu = mockk<AbstractContainerMenu>(relaxed = true)
                    setField(menu, "containerId", 11)

                    Networking.PacketContainerSyncServerToClient.sendToPlayer(
                        player,
                        menu,
                        CompoundTag(),
                    )

                    val calls = TestNetworkingPlatform.sendToPlayerCalls
                    calls.size shouldBe 1
                    calls[0].second.data.nbt.getInt("cid") shouldBe 11
                }

                it("is a no-op when container is null") {
                    Networking.PacketContainerSyncServerToClient.sendToPlayer(
                        mockk(relaxed = true),
                        null as AbstractContainerMenu?,
                        CompoundTag(),
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }
            }

            describe("sendToListeners") {
                it("sends only to players whose open container ID matches") {
                    val world = mockk<Level>(relaxed = true)
                    val container = mockk<SyncableMenu>(relaxed = true)
                    setField(container, "containerId", 8)

                    val matchPlayer = mockk<ServerPlayer>(relaxed = true)
                    val matchMenu = mockk<AbstractContainerMenu>(relaxed = true)
                    setField(matchMenu, "containerId", 8)
                    setField(matchPlayer, "containerMenu", matchMenu)

                    val otherPlayer = mockk<ServerPlayer>(relaxed = true)
                    val otherMenu = mockk<AbstractContainerMenu>(relaxed = true)
                    setField(otherMenu, "containerId", 99)
                    setField(otherPlayer, "containerMenu", otherMenu)

                    every { world.players() } returns listOf(matchPlayer, otherPlayer)

                    Networking.PacketContainerSyncServerToClient.sendToListeners(
                        world,
                        container,
                        CompoundTag(),
                    )

                    TestNetworkingPlatform.sendToPlayerCalls.size shouldBe 1
                    TestNetworkingPlatform.sendToPlayerCalls[0].first shouldBe matchPlayer
                }

                it("sends nothing when no player has the container open") {
                    val world = mockk<Level>(relaxed = true)
                    val container = mockk<SyncableMenu>(relaxed = true)
                    setField(container, "containerId", 8)

                    val player = mockk<ServerPlayer>(relaxed = true)
                    val menu = mockk<AbstractContainerMenu>(relaxed = true)
                    setField(menu, "containerId", 99)
                    setField(player, "containerMenu", menu)

                    every { world.players() } returns listOf(player)

                    Networking.PacketContainerSyncServerToClient.sendToListeners(
                        world,
                        container,
                        CompoundTag(),
                    )

                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }
            }
        }

        // ------------------------------------------------------------------
        // PacketNbtNotifyServerToClient — send methods
        // ------------------------------------------------------------------

        describe("PacketNbtNotifyServerToClient") {
            describe("sendToPlayer") {
                it("sends nbt payload directly to a ServerPlayer") {
                    val player = mockk<ServerPlayer>(relaxed = true)
                    val nbt = CompoundTag().also { it.putInt("val", 42) }
                    Networking.PacketNbtNotifyServerToClient.sendToPlayer(player, nbt)

                    val calls = TestNetworkingPlatform.sendToPlayerCalls
                    calls.size shouldBe 1
                    val data = calls[0].second.data
                    assertSoftly {
                        data.id shouldBe "nns2c"
                        data.nbt.getInt("val") shouldBe 42
                    }
                }

                it("is a no-op when nbt is null") {
                    Networking.PacketNbtNotifyServerToClient.sendToPlayer(
                        mockk<ServerPlayer>(relaxed = true),
                        null,
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }

                it("is a no-op when player is not a ServerPlayer") {
                    Networking.PacketNbtNotifyServerToClient.sendToPlayer(
                        mockk<Player>(relaxed = true),
                        CompoundTag(),
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }
            }

            describe("sendToPlayers") {
                it("sends to every player in the level") {
                    val world = mockk<Level>(relaxed = true)
                    val p1 = mockk<ServerPlayer>(relaxed = true)
                    val p2 = mockk<ServerPlayer>(relaxed = true)
                    every { world.players() } returns listOf(p1, p2)

                    Networking.PacketNbtNotifyServerToClient.sendToPlayers(
                        world,
                        "hnd",
                        CompoundTag(),
                    )

                    TestNetworkingPlatform.sendToPlayerCalls.size shouldBe 2
                }

                it("is a no-op when world is null") {
                    Networking.PacketNbtNotifyServerToClient.sendToPlayers(
                        null,
                        "hnd",
                        CompoundTag(),
                    )
                    TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
                }
            }
        }

        // ------------------------------------------------------------------
        // OverlayTextMessage — sendToPlayer
        // ------------------------------------------------------------------

        describe("OverlayTextMessage") {
            // The two-arg isEmpty() check is pure logic — no registry or serialization needed.
            it("is a no-op when the message is empty") {
                Networking.OverlayTextMessage.sendToPlayer(
                    mockk(relaxed = true),
                    Component.empty(),
                    1000,
                )
                TestNetworkingPlatform.sendToPlayerCalls shouldBe emptyList()
            }
        }

        // ------------------------------------------------------------------
        // UnifiedPayload codec
        // ------------------------------------------------------------------

        describe("UnifiedPayload") {
            it("round-trips packet ID and NBT through FriendlyByteBuf") {
                val original =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData(
                            "round-trip",
                            CompoundTag().also { it.putLong("v", 0xDEAD_BEEFL) },
                        )
                    )
                val buf = FriendlyByteBuf(Unpooled.buffer())
                Networking.UnifiedPayload.STREAM_CODEC.encode(buf, original)
                val decoded = Networking.UnifiedPayload.STREAM_CODEC.decode(buf)

                assertSoftly {
                    decoded.data.id shouldBe "round-trip"
                    decoded.data.nbt.getLong("v") shouldBe 0xDEAD_BEEFL
                }
            }
        }
    })
