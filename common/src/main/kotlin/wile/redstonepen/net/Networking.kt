package wile.redstonepen.net

import java.util.function.BiConsumer
import java.util.function.Consumer
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

private fun sendToClient(player: ServerPlayer, packetId: String, payloadNbt: CompoundTag) =
    PlatformServices.NETWORKING.sendToPlayer(
        player,
        Networking.UnifiedPayload(Networking.UnifiedPayload.UnifiedData(packetId, payloadNbt)),
    )

private fun sendToClients(world: ServerLevel, packetId: String, payloadNbt: CompoundTag) =
    PlatformServices.NETWORKING.sendToAllPlayers(
        world,
        Networking.UnifiedPayload(Networking.UnifiedPayload.UnifiedData(packetId, payloadNbt)),
    )

object Networking {

    @JvmStatic fun init() = PlatformServices.NETWORKING.registerPayloads()

    @JvmStatic
    fun handleServerReceive(unifiedPayload: UnifiedPayload, player: ServerPlayer) {
        val world = player.serverLevel()
        val payload = unifiedPayload.data.nbt
        player.server.execute {
            when (unifiedPayload.data.id) {
                PacketTileNotifyClientToServer.PACKET_ID -> {
                    val pos = BlockPos.of(payload.getLong("pos"))
                    val nbt = payload.getCompound("nbt")
                    val te =
                        world.getBlockEntity(pos) as? IPacketTileNotifyReceiver ?: return@execute
                    te.onClientPacketReceived(player, nbt)
                }
                PacketContainerSyncClientToServer.PACKET_ID -> {
                    val containerId = payload.getInt("cid")
                    val nbt = payload.getCompound("nbt")
                    val nsc =
                        player.containerMenu as? INetworkSynchronisableContainer ?: return@execute
                    if (player.containerMenu.containerId != containerId) return@execute
                    nsc.onClientPacketReceived(containerId, player, nbt)
                }
                PacketNbtNotifyClientToServer.PACKET_ID -> {
                    val hnd = payload.getString("hnd")
                    val nbt = payload.getCompound("nbt")
                    if (hnd.isEmpty() || !PacketNbtNotifyClientToServer.handlers.containsKey(hnd))
                        return@execute
                    PacketNbtNotifyClientToServer.handlers[hnd]!!.accept(player, nbt)
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @JvmStatic
    fun handleClientReceive(unifiedPayload: UnifiedPayload, player: LocalPlayer) {
        val world = player.level()
        val payload = unifiedPayload.data.nbt
        when (unifiedPayload.data.id) {
            PacketTileNotifyServerToClient.PACKET_ID -> {
                val pos = BlockPos.of(payload.getLong("pos"))
                val nbt = payload.getCompound("nbt")
                val te = world.getBlockEntity(pos) as? IPacketTileNotifyReceiver ?: return
                te.onServerPacketReceived(nbt)
            }
            PacketContainerSyncServerToClient.PACKET_ID -> {
                val containerId = payload.getInt("cid")
                val nbt = payload.getCompound("nbt")
                val nsc = player.containerMenu as? INetworkSynchronisableContainer ?: return
                if (player.containerMenu.containerId != containerId) return
                nsc.onServerPacketReceived(containerId, nbt)
            }
            PacketNbtNotifyServerToClient.PACKET_ID -> {
                val hnd = payload.getString("hnd")
                val nbt = payload.getCompound("nbt")
                if (hnd.isEmpty() || !PacketNbtNotifyServerToClient.handlers.containsKey(hnd))
                    return
                PacketNbtNotifyServerToClient.handlers[hnd]!!.accept(nbt)
            }
            OverlayTextMessage.PACKET_ID -> handleOverlayTextMessage(payload, world)
        }
    }

    @Environment(EnvType.CLIENT)
    private fun handleOverlayTextMessage(payload: CompoundTag, world: Level) {
        OverlayTextMessage.handler?.let { handler ->
            val delay = payload.getInt("delay")
            if (delay <= 0) return@let
            val deserialized = payload.getString("msg")
            val m =
                try {
                    Auxiliaries.unserializeTextComponent(deserialized, world.registryAccess())
                        ?: Component.translatable("[incorrect translation]")
                } catch (e: Throwable) {
                    Auxiliaries.logger().warn("Failed to deserialize overlay message: $e")
                    Component.translatable("[incorrect translation]")
                }
            handler.accept(m, delay)
        }
    }

    // ------------------------------------------------------------------
    // Unified Packet Handling
    // ------------------------------------------------------------------

    data class UnifiedPayload(val data: UnifiedData) : CustomPacketPayload {
        companion object {
            @JvmField
            val STREAM_CODEC: StreamCodec<FriendlyByteBuf, UnifiedPayload> =
                CustomPacketPayload.codec(
                    { payload, buf -> payload.write(buf) },
                    { buf -> UnifiedPayload(UnifiedData(buf)) },
                )

            @JvmField
            val TYPE: CustomPacketPayload.Type<UnifiedPayload> =
                CustomPacketPayload.Type(
                    ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "unpnbt")
                )

            @JvmStatic fun getTYPE(): CustomPacketPayload.Type<UnifiedPayload> = TYPE
        }

        private fun write(buf: FriendlyByteBuf) = data.write(buf)

        override fun type() = TYPE

        data class UnifiedData(val id: String, val nbt: CompoundTag) {
            constructor(buf: FriendlyByteBuf) : this(buf.readUtf(), buf.readNbt() ?: CompoundTag())

            fun write(buf: FriendlyByteBuf) {
                buf.writeUtf(id)
                buf.writeNbt(nbt)
            }

            override fun toString() = "$id: $nbt"
        }
    }

    // ------------------------------------------------------------------
    // Tile entity notifications
    // ------------------------------------------------------------------

    interface IPacketTileNotifyReceiver {
        fun onServerPacketReceived(nbt: CompoundTag) {}

        fun onClientPacketReceived(player: Player, nbt: CompoundTag) {}
    }

    object PacketTileNotifyClientToServer {
        internal const val PACKET_ID = "tnc2s"
    }

    object PacketTileNotifyServerToClient {
        internal const val PACKET_ID = "tns2c"

        @JvmStatic
        fun sendToPlayer(player: ServerPlayer, te: BlockEntity?, nbt: CompoundTag?) {
            if (te == null || nbt == null) return
            val payload = CompoundTag()
            payload.putLong("pos", te.blockPos.asLong())
            payload.put("nbt", nbt)
            sendToClient(player, PACKET_ID, payload)
        }

        @JvmStatic
        fun sendToPlayers(te: BlockEntity?, nbt: CompoundTag) {
            val sworld = te?.level as? ServerLevel ?: return
            val payload = CompoundTag()
            payload.putLong("pos", te.blockPos.asLong())
            payload.put("nbt", nbt)
            sendToClients(sworld, PACKET_ID, payload)
        }
    }

    // ------------------------------------------------------------------
    // Container synchronization
    // ------------------------------------------------------------------

    interface INetworkSynchronisableContainer {
        fun onServerPacketReceived(windowId: Int, nbt: CompoundTag)

        fun onClientPacketReceived(windowId: Int, player: Player, nbt: CompoundTag)
    }

    object PacketContainerSyncClientToServer {
        internal const val PACKET_ID = "csc2s"
    }

    object PacketContainerSyncServerToClient {
        internal const val PACKET_ID = "css2c"

        @JvmStatic
        fun sendToPlayer(player: ServerPlayer?, windowId: Int, nbt: CompoundTag?) {
            if (nbt == null || player == null) return
            val payload = CompoundTag()
            payload.putInt("cid", windowId)
            payload.put("nbt", nbt)
            sendToClient(player, PACKET_ID, payload)
        }

        @JvmStatic
        fun sendToPlayer(
            player: ServerPlayer,
            container: AbstractContainerMenu?,
            nbt: CompoundTag,
        ) {
            if (container != null) sendToPlayer(player, container.containerId, nbt)
        }

        @JvmStatic
        fun <C> sendToListeners(world: Level, container: C, nbt: CompoundTag)
            where C : AbstractContainerMenu, C : INetworkSynchronisableContainer {
            for (player in world.players()) {
                if (player.containerMenu.containerId != container.containerId) continue
                sendToPlayer(player as ServerPlayer, container.containerId, nbt)
            }
        }
    }

    // ------------------------------------------------------------------
    // World notifications
    // ------------------------------------------------------------------

    object PacketNbtNotifyClientToServer {
        internal const val PACKET_ID = "nnc2s"

        @JvmField val handlers = HashMap<String, BiConsumer<Player, CompoundTag>>()
    }

    object PacketNbtNotifyServerToClient {
        internal const val PACKET_ID = "nns2c"

        @JvmField val handlers = HashMap<String, Consumer<CompoundTag>>()

        @JvmStatic
        fun sendToPlayer(player: Player, nbt: CompoundTag?) {
            if (nbt == null || player !is ServerPlayer) return
            sendToClient(player, PACKET_ID, nbt)
        }

        @Suppress("UnusedParameter")
        @JvmStatic
        fun sendToPlayers(world: Level?, handler: String, nbt: CompoundTag) {
            if (world != null) for (player in world.players()) sendToPlayer(player, nbt)
        }
    }

    // ------------------------------------------------------------------
    // Main window GUI text message
    // ------------------------------------------------------------------

    object OverlayTextMessage {
        internal const val PACKET_ID = "otms2c"
        const val DISPLAY_TIME_MS = 3000

        internal var handler: BiConsumer<Component, Int>? = null

        @JvmStatic
        fun setHandler(handler: BiConsumer<Component, Int>) {
            if (this.handler == null) this.handler = handler
        }

        @JvmStatic
        fun sendToPlayer(player: ServerPlayer, message: Component) =
            sendToPlayer(player, message, DISPLAY_TIME_MS)

        @JvmStatic
        fun sendToPlayer(player: ServerPlayer, message: Component, delay: Int) {
            if (Auxiliaries.isEmpty(message)) return
            try {
                val payload = CompoundTag()
                payload.putInt("delay", delay)
                payload.putString(
                    "msg",
                    Auxiliaries.serializeTextComponent(message, player.registryAccess()),
                )
                sendToClient(player, PACKET_ID, payload)
            } catch (e: Throwable) {
                Auxiliaries.logger().error("OverlayTextMessage.toBytes() failed: $e")
            }
        }
    }
}
