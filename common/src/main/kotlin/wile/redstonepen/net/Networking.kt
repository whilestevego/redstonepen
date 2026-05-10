package wile.redstonepen.net

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import wile.redstonepen.ModConstants
import wile.redstonepen.platform.PlatformServices
import wile.redstonepen.util.Auxiliaries
import java.util.function.BiConsumer
import java.util.function.Consumer


object Networking {

    @JvmStatic fun init() { PlatformServices.NETWORKING.registerPayloads() }

    @JvmStatic fun handleServerReceive(unified_payload: UnifiedPayload, player: ServerPlayer) {
        val world = player.serverLevel()
        player.server.execute { dispatchServerReceive(unified_payload, world, player) }
    }

    @JvmStatic fun dispatchServerReceive(unified_payload: UnifiedPayload, world: ServerLevel, player: Player) {
        val payload = unified_payload.data().nbt ?: return
        when (unified_payload.data().id) {
            PacketTileNotifyClientToServer.PACKET_ID -> {
                val pos = BlockPos.of(payload.getLong("pos"))
                val nbt = payload.getCompound("nbt")
                val te = world.getBlockEntity(pos)
                if (te !is IPacketTileNotifyReceiver) return
                te.onClientPacketReceived(player, nbt)
            }
            PacketContainerSyncClientToServer.PACKET_ID -> {
                val container_id = payload.getInt("cid")
                val nbt = payload.getCompound("nbt")
                val nsc = player.containerMenu as? INetworkSynchronisableContainer ?: return
                if (player.containerMenu.containerId != container_id) return
                nsc.onClientPacketReceived(container_id, player, nbt)
            }
            PacketNbtNotifyClientToServer.PACKET_ID -> {
                val hnd = payload.getString("hnd")
                val nbt = payload.getCompound("nbt")
                if (hnd.isEmpty() || !PacketNbtNotifyClientToServer.handlers.containsKey(hnd)) return
                PacketNbtNotifyClientToServer.handlers[hnd]!!.accept(player, nbt)
            }
        }
    }

    @JvmStatic @Environment(EnvType.CLIENT)
    fun handleClientReceive(unified_payload: UnifiedPayload, player: LocalPlayer) {
        dispatchClientReceive(unified_payload, player.level(), player.containerMenu)
    }

    @JvmStatic fun dispatchClientReceive(unified_payload: UnifiedPayload, world: Level, containerMenu: AbstractContainerMenu) {
        val payload = unified_payload.data().nbt ?: return
        when (unified_payload.data().id) {
            PacketTileNotifyServerToClient.PACKET_ID -> {
                val pos = BlockPos.of(payload.getLong("pos"))
                val nbt = payload.getCompound("nbt")
                val te = world.getBlockEntity(pos)
                if (te !is IPacketTileNotifyReceiver) return
                te.onServerPacketReceived(nbt)
            }
            PacketContainerSyncServerToClient.PACKET_ID -> {
                val container_id = payload.getInt("cid")
                val nbt = payload.getCompound("nbt")
                val nsc = containerMenu as? INetworkSynchronisableContainer ?: return
                if (containerMenu.containerId != container_id) return
                nsc.onServerPacketReceived(container_id, nbt)
            }
            PacketNbtNotifyServerToClient.PACKET_ID -> {
                val hnd = payload.getString("hnd")
                val nbt = payload.getCompound("nbt")
                if (hnd.isEmpty() || !PacketNbtNotifyServerToClient.handlers.containsKey(hnd)) return
                PacketNbtNotifyServerToClient.handlers[hnd]!!.accept(nbt)
            }
            OverlayTextMessage.PACKET_ID -> {
                if (OverlayTextMessage.handler_ == null) return
                val delay = payload.getInt("delay")
                if (delay <= 0) return
                val deserialized = payload.getString("msg")
                val m: Component = try {
                    Auxiliaries.unserializeTextComponent(deserialized, world.registryAccess()) ?: Component.translatable("[incorrect translation]")
                } catch (e: Throwable) {
                    Component.translatable("[incorrect translation]")
                }
                OverlayTextMessage.handler_!!.accept(m, delay)
            }
        }
    }

    //--------------------------------------------------------------------------------------------------------------------
    // Unified Packet Handling
    //--------------------------------------------------------------------------------------------------------------------

    class UnifiedPayload(private val _data: UnifiedData) : CustomPacketPayload {
        constructor(buf: FriendlyByteBuf) : this(UnifiedData(buf.readUtf(), buf.readNbt()))
        private fun write(buf: FriendlyByteBuf) { _data.write(buf) }
        fun data(): UnifiedData = _data
        override fun type(): CustomPacketPayload.Type<UnifiedPayload> = TYPE

        companion object {
            @JvmField val STREAM_CODEC: StreamCodec<FriendlyByteBuf, UnifiedPayload> =
                CustomPacketPayload.codec({ payload, buf -> payload.write(buf) }, { buf -> UnifiedPayload(buf) })
            @JvmField val TYPE: CustomPacketPayload.Type<UnifiedPayload> =
                CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "unpnbt"))
            @JvmStatic fun getTYPE(): CustomPacketPayload.Type<UnifiedPayload> = TYPE
        }

        class UnifiedData(@get:JvmName("id") val id: String, @get:JvmName("nbt") val nbt: CompoundTag?) {
            constructor(buf: FriendlyByteBuf) : this(buf.readUtf(), buf.readNbt())
            fun write(buf: FriendlyByteBuf) { buf.writeUtf(id); buf.writeNbt(nbt) }
            override fun toString(): String = "$id: $nbt"
        }
    }

    internal fun sendToClient(player: ServerPlayer, packet_id: String, payload_nbt: CompoundTag) {
        PlatformServices.NETWORKING.sendToPlayer(player, UnifiedPayload(UnifiedPayload.UnifiedData(packet_id, payload_nbt)))
    }

    internal fun sendToClients(world: ServerLevel, packet_id: String, payload_nbt: CompoundTag) {
        PlatformServices.NETWORKING.sendToAllPlayers(world, UnifiedPayload(UnifiedPayload.UnifiedData(packet_id, payload_nbt)))
    }

    //--------------------------------------------------------------------------------------------------------------------
    // Tile entity notifications
    //--------------------------------------------------------------------------------------------------------------------

    interface IPacketTileNotifyReceiver {
        fun onServerPacketReceived(nbt: CompoundTag) {}
        fun onClientPacketReceived(player: Player, nbt: CompoundTag) {}
    }

    object PacketTileNotifyClientToServer {
        const val PACKET_ID = "tnc2s"
    }

    object PacketTileNotifyServerToClient {
        const val PACKET_ID = "tns2c"

        @JvmStatic fun sendToPlayer(player: ServerPlayer, te: BlockEntity?, nbt: CompoundTag?) {
            if (te == null || nbt == null) return
            val payload = CompoundTag()
            payload.putLong("pos", te.blockPos.asLong())
            payload.put("nbt", nbt)
            Networking.sendToClient(player, PACKET_ID, payload)
        }

        @JvmStatic fun sendToPlayers(te: BlockEntity?, nbt: CompoundTag?) {
            val sworld = te?.level as? ServerLevel ?: return
            if (nbt == null) return
            val payload = CompoundTag()
            payload.putLong("pos", te.blockPos.asLong())
            payload.put("nbt", nbt)
            Networking.sendToClients(sworld, PACKET_ID, payload)
        }
    }

    //--------------------------------------------------------------------------------------------------------------------
    // (GUI) Container synchronization
    //--------------------------------------------------------------------------------------------------------------------

    interface INetworkSynchronisableContainer {
        fun onServerPacketReceived(windowId: Int, nbt: CompoundTag)
        fun onClientPacketReceived(windowId: Int, player: Player, nbt: CompoundTag)
    }

    object PacketContainerSyncClientToServer {
        const val PACKET_ID = "csc2s"
    }

    object PacketContainerSyncServerToClient {
        const val PACKET_ID = "css2c"

        @JvmStatic fun sendToPlayer(player: ServerPlayer, windowId: Int, nbt: CompoundTag?) {
            if (nbt == null) return
            val payload = CompoundTag()
            payload.putInt("cid", windowId)
            payload.put("nbt", nbt)
            Networking.sendToClient(player, PACKET_ID, payload)
        }

        @JvmStatic fun sendToPlayer(player: ServerPlayer, container: AbstractContainerMenu?, nbt: CompoundTag?) {
            if (container != null) sendToPlayer(player, container.containerId, nbt)
        }

        @JvmStatic fun <C> sendToListeners(world: Level, container: C, nbt: CompoundTag?)
                where C : AbstractContainerMenu, C : INetworkSynchronisableContainer {
            for (player in world.players()) {
                if (player.containerMenu.containerId != container.containerId) continue
                sendToPlayer(player as ServerPlayer, container.containerId, nbt)
            }
        }
    }

    //--------------------------------------------------------------------------------------------------------------------
    // World notifications
    //--------------------------------------------------------------------------------------------------------------------

    object PacketNbtNotifyClientToServer {
        const val PACKET_ID = "nnc2s"
        @JvmField val handlers: MutableMap<String, BiConsumer<Player, CompoundTag>> = HashMap()
    }

    object PacketNbtNotifyServerToClient {
        const val PACKET_ID = "nns2c"
        @JvmField val handlers: MutableMap<String, Consumer<CompoundTag>> = HashMap()

        @JvmStatic fun sendToPlayer(player: Player, nbt: CompoundTag?) {
            if (nbt == null || player !is ServerPlayer) return
            Networking.sendToClient(player, PACKET_ID, nbt)
        }

        @JvmStatic fun sendToPlayers(world: Level?, handler: String, nbt: CompoundTag?) {
            if (world != null) for (player in world.players()) sendToPlayer(player, nbt)
        }
    }

    //--------------------------------------------------------------------------------------------------------------------
    // Main window GUI text message
    //--------------------------------------------------------------------------------------------------------------------

    object OverlayTextMessage {
        const val PACKET_ID = "otms2c"
        const val DISPLAY_TIME_MS = 3000
        @JvmField var handler_: BiConsumer<Component, Int>? = null

        @JvmStatic fun setHandler(handler: BiConsumer<Component, Int>) {
            if (handler_ == null) handler_ = handler
        }

        @JvmStatic fun sendToPlayer(player: ServerPlayer, message: Component) {
            sendToPlayer(player, message, DISPLAY_TIME_MS)
        }

        @JvmStatic fun sendToPlayer(player: ServerPlayer, message: Component, delay: Int) {
            if (Auxiliaries.isEmpty(message)) return
            try {
                val payload = CompoundTag()
                payload.putInt("delay", delay)
                payload.putString("msg", Auxiliaries.serializeTextComponent(message, player.registryAccess()))
                Networking.sendToClient(player, PACKET_ID, payload)
            } catch (e: Throwable) {
                Auxiliaries.logger().error("OverlayTextMessage.toBytes() failed: $e")
            }
        }
    }
}
