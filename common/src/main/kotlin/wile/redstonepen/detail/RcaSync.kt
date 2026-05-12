package wile.redstonepen.detail

import java.util.Locale
import java.util.UUID
import java.util.function.BiConsumer
import java.util.function.Consumer
import net.minecraft.nbt.CompoundTag
import wile.redstonepen.net.Networking
import wile.redstonepen.net.NetworkingClient
import wile.redstonepen.util.Auxiliaries

object RcaSync {
    private const val MESSAGE_HANDLER_ID = "rcadata"

    class RcaData(val puid: UUID) {
        var client_inputs_: Long = 0
        var client_outputs_: Long = 0
        var server_outputs_: Long = 0

        fun isValid(): Boolean = puid.leastSignificantBits != 0L || puid.mostSignificantBits != 0L

        @Synchronized fun client_inputs(): Long = client_inputs_

        @Synchronized
        fun client_inputs(value: Long) {
            client_inputs_ = value
        }

        @Synchronized fun server_outputs(): Long = server_outputs_

        @Synchronized
        fun server_outputs(value: Long) {
            server_outputs_ = value
        }

        override fun toString(): String =
            "{player:\"$puid\", " +
                "ci:${String.format(Locale.ROOT, "%016x", client_inputs_)}, " +
                "co:${String.format(Locale.ROOT, "%016x", client_outputs_)}, " +
                "so:${String.format(Locale.ROOT, "%016x", server_outputs_)}}"
    }

    object CommonRca {
        @JvmField val EMPTY: RcaData = RcaData(UUID(0, 0)) // intentionally not immutable

        private val data_cache: MutableMap<UUID, RcaData> = HashMap()
        private var num_exceptions: Long = 0
        private const val ERROR_CUTOFF_COUNT: Long = 32

        @JvmStatic
        @Synchronized
        fun ofPlayer(puid: UUID?, allow_create: Boolean): RcaData {
            if (puid == null) return EMPTY
            if (allow_create && !data_cache.containsKey(puid)) data_cache[puid] = RcaData(puid)
            return data_cache.getOrDefault(puid, EMPTY)
        }

        @JvmStatic
        fun init() {
            Networking.PacketNbtNotifyClientToServer.handlers[MESSAGE_HANDLER_ID] =
                BiConsumer { player, nbt ->
                    if (applyRcaUpdate(player.uuid, nbt)) {
                        Networking.PacketNbtNotifyServerToClient.sendToPlayer(player, nbt)
                    }
                }
        }

        @JvmStatic
        fun applyRcaUpdate(uid: UUID, nbt: CompoundTag): Boolean {
            if (!nbt.contains("i") || num_exceptions >= ERROR_CUTOFF_COUNT) return false
            return try {
                val rca = ofPlayer(uid, true)
                rca.client_inputs(nbt.getLong("i"))
                nbt.remove("i")
                nbt.putLong("o", rca.server_outputs())
                true
            } catch (ignored: Throwable) {
                ++num_exceptions
                false
            }
        }
    }

    object ClientRca {
        @JvmStatic
        fun init(): Boolean {
            val rca =
                wile.api.rca.FmmRedstoneClientAdapter.Adapter.instance()
                    ?: run {
                        Auxiliaries.logInfo("Redstone Pen RCA disabled (default).")
                        return false
                    }
            Networking.PacketNbtNotifyServerToClient.handlers[MESSAGE_HANDLER_ID] =
                Consumer { nbt ->
                    if (nbt.contains("o")) rca.setOutputs(nbt.getLong("o"))
                }
            Auxiliaries.logInfo("Redstone Pen RCA detected and enabled on this client machine.")
            return true
        }

        @JvmStatic
        fun tick() {
            val rca = wile.api.rca.FmmRedstoneClientAdapter.Adapter.instance() ?: return
            rca.tick()
            val nbt = CompoundTag()
            nbt.putString("hnd", MESSAGE_HANDLER_ID)
            nbt.putLong("i", rca.getInputs())
            NetworkingClient.PacketNbtNotifyClientToServer.sendToServer(nbt)
            rca.setInputsChanged(false)
        }
    }
}
