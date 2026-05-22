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
        var clientInputs: Long = 0
        var clientOutputs: Long = 0
        var serverOutputs: Long = 0

        fun isValid(): Boolean = puid.leastSignificantBits != 0L || puid.mostSignificantBits != 0L

        @Synchronized fun client_inputs(): Long = clientInputs

        @Synchronized
        fun client_inputs(value: Long) {
            clientInputs = value
        }

        @Synchronized fun server_outputs(): Long = serverOutputs

        @Synchronized
        fun server_outputs(value: Long) {
            serverOutputs = value
        }

        override fun toString(): String =
            "{player:\"$puid\", " +
                "ci:${String.format(Locale.ROOT, "%016x", clientInputs)}, " +
                "co:${String.format(Locale.ROOT, "%016x", clientOutputs)}, " +
                "so:${String.format(Locale.ROOT, "%016x", serverOutputs)}}"
    }

    object CommonRca {
        @JvmField val EMPTY: RcaData = RcaData(UUID(0, 0)) // intentionally not immutable

        private val dataCache: MutableMap<UUID, RcaData> = HashMap()
        private var numExceptions: Long = 0
        private const val ERROR_CUTOFF_COUNT: Long = 32

        @JvmStatic
        @Synchronized
        fun ofPlayer(puid: UUID?, allowCreate: Boolean): RcaData {
            if (puid == null) return EMPTY
            if (allowCreate && !dataCache.containsKey(puid)) dataCache[puid] = RcaData(puid)
            return dataCache.getOrDefault(puid, EMPTY)
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
            if (!nbt.contains("i") || numExceptions >= ERROR_CUTOFF_COUNT) return false
            return try {
                val rca = ofPlayer(uid, true)
                rca.client_inputs(nbt.getLong("i"))
                nbt.remove("i")
                nbt.putLong("o", rca.server_outputs())
                true
            } catch (ignored: Throwable) {
                ++numExceptions
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
