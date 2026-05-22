package wile.redstonepen.neoforge

import net.minecraft.client.player.LocalPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import wile.redstonepen.net.Networking
import wile.redstonepen.platform.INetworkingPlatform

class NetworkingPlatformNeoForge : INetworkingPlatform {
    companion object {
        private lateinit var registrar: PayloadRegistrar

        @JvmStatic
        fun setRegistrar(r: PayloadRegistrar) {
            registrar = r
        }
    }

    override fun registerPayloads() {
        registrar.playBidirectional(
            Networking.UnifiedPayload.TYPE,
            Networking.UnifiedPayload.STREAM_CODEC,
        ) { payload, context ->
            context.enqueueWork {
                if (context.flow().isServerbound) {
                    Networking.handleServerReceive(payload, context.player() as ServerPlayer)
                } else {
                    Networking.handleClientReceive(payload, context.player() as LocalPlayer)
                }
            }
        }
    }

    override fun sendToPlayer(player: ServerPlayer, payload: Networking.UnifiedPayload) {
        PacketDistributor.sendToPlayer(player, payload)
    }

    override fun sendToAllPlayers(world: ServerLevel, payload: Networking.UnifiedPayload) {
        PacketDistributor.sendToPlayersInDimension(world, payload)
    }
}
