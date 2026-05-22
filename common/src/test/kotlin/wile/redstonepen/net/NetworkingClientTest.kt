package wile.redstonepen.net

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.entity.BlockEntity

class NetworkingClientTest :
    DescribeSpec({
        beforeEach { TestNetworkingClientPlatform.reset() }

        afterEach { unmockkAll() }

        // ------------------------------------------------------------------
        // PacketTileNotifyClientToServer
        // ------------------------------------------------------------------

        describe("PacketTileNotifyClientToServer") {
            describe("sendToServer(BlockPos, CompoundTag)") {
                it("sends a payload encoding the position and data nbt") {
                    val pos = BlockPos(3, 71, -8)
                    val nbt = CompoundTag().also { it.putInt("power", 7) }
                    NetworkingClient.PacketTileNotifyClientToServer.sendToServer(pos, nbt)

                    val calls = TestNetworkingClientPlatform.sendToServerCalls
                    calls.size shouldBe 1
                    val data = calls[0].data()
                    assertSoftly {
                        data.id() shouldBe "tnc2s"
                        BlockPos.of(data.nbt().getLong("pos")) shouldBe pos
                        data.nbt().getCompound("nbt").getInt("power") shouldBe 7
                    }
                }

                it("is a no-op when pos is null") {
                    NetworkingClient.PacketTileNotifyClientToServer.sendToServer(
                        null as BlockPos?,
                        CompoundTag(),
                    )
                    TestNetworkingClientPlatform.sendToServerCalls shouldBe emptyList()
                }

                it("is a no-op when nbt is null") {
                    NetworkingClient.PacketTileNotifyClientToServer.sendToServer(
                        BlockPos.ZERO,
                        null,
                    )
                    TestNetworkingClientPlatform.sendToServerCalls shouldBe emptyList()
                }
            }

            describe("sendToServer(BlockEntity, CompoundTag)") {
                it("delegates to the BlockPos overload using the entity's position") {
                    val pos = BlockPos(5, 65, 5)
                    val te = mockk<BlockEntity>(relaxed = true)
                    every { te.getBlockPos() } returns pos

                    NetworkingClient.PacketTileNotifyClientToServer.sendToServer(
                        te,
                        CompoundTag().also { it.putByte("x", 1) },
                    )

                    val calls = TestNetworkingClientPlatform.sendToServerCalls
                    calls.size shouldBe 1
                    BlockPos.of(calls[0].data().nbt().getLong("pos")) shouldBe pos
                }

                it("is a no-op when block entity is null") {
                    NetworkingClient.PacketTileNotifyClientToServer.sendToServer(
                        null as BlockEntity?,
                        CompoundTag(),
                    )
                    TestNetworkingClientPlatform.sendToServerCalls shouldBe emptyList()
                }
            }
        }

        // ------------------------------------------------------------------
        // PacketContainerSyncClientToServer
        // ------------------------------------------------------------------

        describe("PacketContainerSyncClientToServer") {
            describe("sendToServer(container_id, CompoundTag)") {
                it("sends a payload with container ID and data nbt") {
                    val nbt = CompoundTag().also { it.putString("code", "b=d") }
                    NetworkingClient.PacketContainerSyncClientToServer.sendToServer(4, nbt)

                    val calls = TestNetworkingClientPlatform.sendToServerCalls
                    calls.size shouldBe 1
                    val data = calls[0].data()
                    assertSoftly {
                        data.id() shouldBe "csc2s"
                        data.nbt().getInt("cid") shouldBe 4
                        data.nbt().getCompound("nbt").getString("code") shouldBe "b=d"
                    }
                }

                it("is a no-op when nbt is null") {
                    NetworkingClient.PacketContainerSyncClientToServer.sendToServer(1, null)
                    TestNetworkingClientPlatform.sendToServerCalls shouldBe emptyList()
                }
            }

            describe("sendToServer(AbstractContainerMenu, CompoundTag)") {
                it("delegates to the container-ID overload using the menu's containerId") {
                    val menu = mockk<AbstractContainerMenu>(relaxed = true)
                    setField(menu, "containerId", 9)

                    NetworkingClient.PacketContainerSyncClientToServer.sendToServer(
                        menu,
                        CompoundTag(),
                    )

                    val calls = TestNetworkingClientPlatform.sendToServerCalls
                    calls.size shouldBe 1
                    calls[0].data().nbt().getInt("cid") shouldBe 9
                }
            }
        }

        // ------------------------------------------------------------------
        // PacketNbtNotifyClientToServer
        // ------------------------------------------------------------------

        describe("PacketNbtNotifyClientToServer") {
            it("sends the nbt payload directly to the server") {
                val nbt = CompoundTag().also { it.putDouble("v", 3.14) }
                NetworkingClient.PacketNbtNotifyClientToServer.sendToServer(nbt)

                val calls = TestNetworkingClientPlatform.sendToServerCalls
                calls.size shouldBe 1
                val data = calls[0].data()
                assertSoftly {
                    data.id() shouldBe "nnc2s"
                    data.nbt().getDouble("v") shouldBe 3.14
                }
            }
        }
    })
