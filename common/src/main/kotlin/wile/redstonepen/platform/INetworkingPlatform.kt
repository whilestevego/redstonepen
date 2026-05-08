package wile.redstonepen.platform

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

interface INetworkingPlatform {
    fun registerPayloads()
    fun sendToPlayer(player: ServerPlayer, payload: wile.redstonepen.net.Networking.UnifiedPayload)
    fun sendToAllPlayers(world: ServerLevel, payload: wile.redstonepen.net.Networking.UnifiedPayload)
}
