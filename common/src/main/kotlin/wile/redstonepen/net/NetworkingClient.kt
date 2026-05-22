package wile.redstonepen.net

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.entity.BlockEntity
import wile.redstonepen.platform.PlatformServices

@Environment(EnvType.CLIENT)
private fun send(packetId: String, payloadNbt: CompoundTag) =
    PlatformServices.getNetworkingClient()
        .sendToServer(
            Networking.UnifiedPayload(Networking.UnifiedPayload.UnifiedData(packetId, payloadNbt))
        )

object NetworkingClient {

    fun clientInit() = PlatformServices.getNetworkingClient().registerClientReceiver()

    // ------------------------------------------------------------------
    // Tile entity notifications
    // ------------------------------------------------------------------

    @Environment(EnvType.CLIENT)
    object PacketTileNotifyClientToServer {
        fun sendToServer(pos: BlockPos?, nbt: CompoundTag?) {
            if (pos == null || nbt == null) return
            val payload = CompoundTag()
            payload.putLong("pos", pos.asLong())
            payload.put("nbt", nbt)
            send(Networking.PacketTileNotifyClientToServer.PACKET_ID, payload)
        }

        fun sendToServer(te: BlockEntity?, nbt: CompoundTag?) {
            if (te != null) sendToServer(te.blockPos, nbt)
        }
    }

    // ------------------------------------------------------------------
    // Container synchronization
    // ------------------------------------------------------------------

    @Environment(EnvType.CLIENT)
    object PacketContainerSyncClientToServer {
        fun sendToServer(containerId: Int, nbt: CompoundTag?) {
            if (nbt == null) return
            val payload = CompoundTag()
            payload.putInt("cid", containerId)
            payload.put("nbt", nbt)
            send(Networking.PacketContainerSyncClientToServer.PACKET_ID, payload)
        }

        fun sendToServer(container: AbstractContainerMenu, nbt: CompoundTag?) =
            sendToServer(container.containerId, nbt)
    }

    // ------------------------------------------------------------------
    // World notifications
    // ------------------------------------------------------------------

    @Environment(EnvType.CLIENT)
    object PacketNbtNotifyClientToServer {
        fun sendToServer(nbt: CompoundTag) =
            send(Networking.PacketNbtNotifyClientToServer.PACKET_ID, nbt)
    }
}
