package wile.redstonepen.fabric

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import wile.redstonepen.net.Networking
import wile.redstonepen.platform.INetworkingPlatform

class NetworkingPlatformFabric : INetworkingPlatform {
    override fun registerPayloads() {
        PayloadTypeRegistry.playC2S()
            .register(Networking.UnifiedPayload.TYPE, Networking.UnifiedPayload.STREAM_CODEC)
        PayloadTypeRegistry.playS2C()
            .register(Networking.UnifiedPayload.TYPE, Networking.UnifiedPayload.STREAM_CODEC)
        ServerPlayNetworking.registerGlobalReceiver(Networking.UnifiedPayload.TYPE) {
            payload,
            context ->
            Networking.handleServerReceive(payload, context.player())
        }
    }

    override fun sendToPlayer(player: ServerPlayer, payload: Networking.UnifiedPayload) {
        ServerPlayNetworking.send(player, payload)
    }

    override fun sendToAllPlayers(world: ServerLevel, payload: Networking.UnifiedPayload) {
        world.players().forEach { player -> ServerPlayNetworking.send(player, payload) }
    }
}
