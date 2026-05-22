package wile.redstonepen.fabric

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context as ClientContext
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.PlayPayloadHandler
import net.minecraft.client.player.LocalPlayer
import net.minecraft.nbt.CompoundTag
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

class NetworkingClientPlatformFabricTest :
    DescribeSpec({
        val impl = NetworkingClientPlatformFabric()

        afterEach { unmockkAll() }

        describe("registerClientReceiver") {
            it("registers a handler that calls Networking.handleClientReceive") {
                mockkStatic(ClientPlayNetworking::class)

                val handlerSlot = slot<PlayPayloadHandler<Networking.UnifiedPayload>>()
                every {
                    ClientPlayNetworking.registerGlobalReceiver(any(), capture(handlerSlot))
                } returns true

                impl.registerClientReceiver()

                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                val localPlayer = mockk<LocalPlayer>(relaxed = true)
                val context = mockk<ClientContext>()
                every { context.player() } returns localPlayer

                handlerSlot.captured.receive(payload, context)

                verify { localPlayer.level() }
            }
        }

        describe("sendToServer") {
            it("delegates to ClientPlayNetworking.send(payload)") {
                mockkStatic(ClientPlayNetworking::class)
                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                every { ClientPlayNetworking.send(any<Networking.UnifiedPayload>()) } just runs

                impl.sendToServer(payload)

                verify { ClientPlayNetworking.send(payload) }
            }
        }
    })
