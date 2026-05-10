package wile.redstonepen.net

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.entity.BlockEntity
import wile.redstonepen.platform.PlatformServices


object NetworkingClient {

    @JvmStatic fun clientInit() {
        PlatformServices.getNetworkingClient().registerClientReceiver()
    }

    @Environment(EnvType.CLIENT)
    internal fun send(packet_id: String, payload_nbt: CompoundTag) {
        PlatformServices.getNetworkingClient().sendToServer(
            Networking.UnifiedPayload(Networking.UnifiedPayload.UnifiedData(packet_id, payload_nbt))
        )
    }

    //--------------------------------------------------------------------------------------------------------------------
    // Tile entity notifications
    //--------------------------------------------------------------------------------------------------------------------

    @Environment(EnvType.CLIENT)
    object PacketTileNotifyClientToServer {

        @JvmStatic fun sendToServer(pos: BlockPos, nbt: CompoundTag?) {
            if (nbt == null) return
            val payload = CompoundTag()
            payload.putLong("pos", pos.asLong())
            payload.put("nbt", nbt)
            NetworkingClient.send(Networking.PacketTileNotifyClientToServer.PACKET_ID, payload)
        }

        @JvmStatic fun sendToServer(te: BlockEntity?, nbt: CompoundTag?) {
            if (te != null) sendToServer(te.blockPos, nbt)
        }
    }

    //--------------------------------------------------------------------------------------------------------------------
    // (GUI) Container synchronization
    //--------------------------------------------------------------------------------------------------------------------

    @Environment(EnvType.CLIENT)
    object PacketContainerSyncClientToServer {

        @JvmStatic fun sendToServer(container_id: Int, nbt: CompoundTag?) {
            if (nbt == null) return
            val payload = CompoundTag()
            payload.putInt("cid", container_id)
            payload.put("nbt", nbt)
            NetworkingClient.send(Networking.PacketContainerSyncClientToServer.PACKET_ID, payload)
        }

        @JvmStatic fun sendToServer(container: AbstractContainerMenu, nbt: CompoundTag?) {
            sendToServer(container.containerId, nbt)
        }
    }

    //--------------------------------------------------------------------------------------------------------------------
    // World notifications
    //--------------------------------------------------------------------------------------------------------------------

    @Environment(EnvType.CLIENT)
    object PacketNbtNotifyClientToServer {

        @JvmStatic fun sendToServer(nbt: CompoundTag?) {
            if (nbt == null) return
            NetworkingClient.send(Networking.PacketNbtNotifyClientToServer.PACKET_ID, nbt)
        }
    }
}
