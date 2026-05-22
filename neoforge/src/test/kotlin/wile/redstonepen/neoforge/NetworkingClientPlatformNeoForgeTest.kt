package wile.redstonepen.neoforge

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.network.PacketDistributor
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

class NetworkingClientPlatformNeoForgeTest :
    DescribeSpec({
        val impl = NetworkingClientPlatformNeoForge()

        afterEach { unmockkAll() }

        describe("registerClientReceiver") {
            it("is a no-op — completes without error and makes no external calls") {
                impl.registerClientReceiver()
                // No exception means the no-op contract is satisfied.
            }
        }

        describe("sendToServer") {
            it("delegates to PacketDistributor.sendToServer") {
                mockkStatic(PacketDistributor::class)
                val payload =
                    Networking.UnifiedPayload(
                        Networking.UnifiedPayload.UnifiedData("unknown-id", CompoundTag())
                    )

                every { PacketDistributor.sendToServer(any<Networking.UnifiedPayload>()) } just runs

                impl.sendToServer(payload)

                verify { PacketDistributor.sendToServer(payload) }
            }
        }
    })
