package wile.redstonepen.neoforge

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import java.util.concurrent.CompletableFuture
import net.minecraft.client.player.LocalPlayer
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.handling.IPayloadHandler
import net.neoforged.neoforge.network.registration.PayloadRegistrar
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

class NetworkingPlatformNeoForgeTest :
    DescribeSpec({
        val registrar = mockk<PayloadRegistrar>(relaxed = true)
        val impl = NetworkingPlatformNeoForge()

        beforeEach { NetworkingPlatformNeoForge.setRegistrar(registrar) }

        afterEach { unmockkAll() }

        describe("registerPayloads") {
            it("calls registrar.playBidirectional with UnifiedPayload TYPE and STREAM_CODEC") {
                impl.registerPayloads()

                verify {
                    registrar.playBidirectional(
                        Networking.UnifiedPayload.TYPE,
                        Networking.UnifiedPayload.STREAM_CODEC,
                        any<IPayloadHandler<Networking.UnifiedPayload>>(),
                    )
                }
            }

            it("handler routes serverbound packets to Networking.handleServerReceive") {
                val handlerSlot = slot<IPayloadHandler<Networking.UnifiedPayload>>()
                every {
                    registrar.playBidirectional(
                        Networking.UnifiedPayload.TYPE,
                        Networking.UnifiedPayload.STREAM_CODEC,
                        capture(handlerSlot),
                    )
                } returns registrar

                impl.registerPayloads()

                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                val serverPlayer = mockk<ServerPlayer>(relaxed = true)
                val mockServer = mockk<MinecraftServer>(relaxed = true)
                every { mockServer.execute(any<Runnable>()) } answers { firstArg<Runnable>().run() }
                setField(serverPlayer, "server", mockServer)

                val context = mockk<IPayloadContext>()
                every { context.flow() } returns PacketFlow.SERVERBOUND
                every { context.player() } returns serverPlayer
                every { context.enqueueWork(any<Runnable>()) } answers
                    {
                        firstArg<Runnable>().run()
                        CompletableFuture.completedFuture(null)
                    }

                handlerSlot.captured.handle(payload, context)

                verify { serverPlayer.serverLevel() }
            }

            it("handler routes clientbound packets to Networking.handleClientReceive") {
                val handlerSlot = slot<IPayloadHandler<Networking.UnifiedPayload>>()
                every {
                    registrar.playBidirectional(
                        Networking.UnifiedPayload.TYPE,
                        Networking.UnifiedPayload.STREAM_CODEC,
                        capture(handlerSlot),
                    )
                } returns registrar

                impl.registerPayloads()

                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                val localPlayer = mockk<LocalPlayer>(relaxed = true)

                val context = mockk<IPayloadContext>()
                every { context.flow() } returns PacketFlow.CLIENTBOUND
                every { context.player() } returns localPlayer
                every { context.enqueueWork(any<Runnable>()) } answers
                    {
                        firstArg<Runnable>().run()
                        CompletableFuture.completedFuture(null)
                    }

                handlerSlot.captured.handle(payload, context)

                verify { localPlayer.level() }
            }
        }

        describe("sendToPlayer") {
            it("delegates to PacketDistributor.sendToPlayer") {
                mockkStatic(PacketDistributor::class)
                val player = mockk<ServerPlayer>(relaxed = true)
                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                every {
                    PacketDistributor.sendToPlayer(
                        any<ServerPlayer>(),
                        any<Networking.UnifiedPayload>(),
                    )
                } just runs

                impl.sendToPlayer(player, payload)

                verify { PacketDistributor.sendToPlayer(player, payload) }
            }
        }

        describe("sendToAllPlayers") {
            it("delegates to PacketDistributor.sendToPlayersInDimension") {
                mockkStatic(PacketDistributor::class)
                val world = mockk<ServerLevel>(relaxed = true)
                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                every {
                    PacketDistributor.sendToPlayersInDimension(
                        any<ServerLevel>(),
                        any<Networking.UnifiedPayload>(),
                    )
                } just runs

                impl.sendToAllPlayers(world, payload)

                verify { PacketDistributor.sendToPlayersInDimension(world, payload) }
            }
        }
    })
