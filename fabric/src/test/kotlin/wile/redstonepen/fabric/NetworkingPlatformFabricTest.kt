package wile.redstonepen.fabric

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context as ServerContext
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.PlayPayloadHandler
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import wile.redstonepen.net.Networking

private fun setField(obj: Any, fieldName: String, value: Any?) {
    var clazz: Class<*>? = obj.javaClass
    while (clazz != null) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(obj, value)
            return
        } catch (_: NoSuchFieldException) {
            clazz = clazz.superclass
        }
    }
    throw NoSuchFieldException("$fieldName not found in ${obj.javaClass} hierarchy")
}

class NetworkingPlatformFabricTest :
    DescribeSpec({
        val impl = NetworkingPlatformFabric()

        afterEach { unmockkAll() }

        describe("registerPayloads") {
            it("registers TYPE and STREAM_CODEC with both C2S and S2C payload type registries") {
                mockkStatic(PayloadTypeRegistry::class)
                mockkStatic(ServerPlayNetworking::class)

                val c2sRegistry =
                    mockk<PayloadTypeRegistry<RegistryFriendlyByteBuf>>(relaxed = true)
                val s2cRegistry =
                    mockk<PayloadTypeRegistry<RegistryFriendlyByteBuf>>(relaxed = true)

                every { PayloadTypeRegistry.playC2S() } returns c2sRegistry
                every { PayloadTypeRegistry.playS2C() } returns s2cRegistry
                every {
                    ServerPlayNetworking.registerGlobalReceiver(
                        any(),
                        any<PlayPayloadHandler<Networking.UnifiedPayload>>(),
                    )
                } returns true

                impl.registerPayloads()

                verify {
                    c2sRegistry.register(
                        Networking.UnifiedPayload.TYPE,
                        Networking.UnifiedPayload.STREAM_CODEC,
                    )
                }
                verify {
                    s2cRegistry.register(
                        Networking.UnifiedPayload.TYPE,
                        Networking.UnifiedPayload.STREAM_CODEC,
                    )
                }
            }

            it("registers a server receiver that calls Networking.handleServerReceive") {
                mockkStatic(PayloadTypeRegistry::class)
                mockkStatic(ServerPlayNetworking::class)

                val c2sRegistry =
                    mockk<PayloadTypeRegistry<RegistryFriendlyByteBuf>>(relaxed = true)
                val s2cRegistry =
                    mockk<PayloadTypeRegistry<RegistryFriendlyByteBuf>>(relaxed = true)

                every { PayloadTypeRegistry.playC2S() } returns c2sRegistry
                every { PayloadTypeRegistry.playS2C() } returns s2cRegistry

                val handlerSlot = slot<PlayPayloadHandler<Networking.UnifiedPayload>>()
                every {
                    ServerPlayNetworking.registerGlobalReceiver(any(), capture(handlerSlot))
                } returns true

                impl.registerPayloads()

                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                val serverPlayer = mockk<ServerPlayer>(relaxed = true)
                val mockServer = mockk<MinecraftServer>(relaxed = true)
                every { mockServer.execute(any<Runnable>()) } answers { firstArg<Runnable>().run() }
                setField(serverPlayer, "server", mockServer)

                val context = mockk<ServerContext>()
                every { context.player() } returns serverPlayer

                handlerSlot.captured.receive(payload, context)

                verify { serverPlayer.serverLevel() }
            }
        }

        describe("sendToPlayer") {
            it("delegates to ServerPlayNetworking.send(player, payload)") {
                mockkStatic(ServerPlayNetworking::class)
                val player = mockk<ServerPlayer>(relaxed = true)
                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                every {
                    ServerPlayNetworking.send(any<ServerPlayer>(), any<Networking.UnifiedPayload>())
                } just runs

                impl.sendToPlayer(player, payload)

                verify { ServerPlayNetworking.send(player, payload) }
            }
        }

        describe("sendToAllPlayers") {
            it("calls ServerPlayNetworking.send for each player in the world") {
                mockkStatic(ServerPlayNetworking::class)
                val world = mockk<ServerLevel>(relaxed = true)
                val player1 = mockk<ServerPlayer>(relaxed = true)
                val player2 = mockk<ServerPlayer>(relaxed = true)
                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                every { world.players() } returns mutableListOf(player1, player2)
                every {
                    ServerPlayNetworking.send(any<ServerPlayer>(), any<Networking.UnifiedPayload>())
                } just runs

                impl.sendToAllPlayers(world, payload)

                verify(exactly = 1) { ServerPlayNetworking.send(player1, payload) }
                verify(exactly = 1) { ServerPlayNetworking.send(player2, payload) }
            }

            it("makes no sends when world has zero players") {
                mockkStatic(ServerPlayNetworking::class)
                val world = mockk<ServerLevel>(relaxed = true)
                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                every { world.players() } returns mutableListOf()
                every {
                    ServerPlayNetworking.send(any<ServerPlayer>(), any<Networking.UnifiedPayload>())
                } just runs

                impl.sendToAllPlayers(world, payload)

                verify(exactly = 0) {
                    ServerPlayNetworking.send(any<ServerPlayer>(), any<Networking.UnifiedPayload>())
                }
            }
        }
    })
