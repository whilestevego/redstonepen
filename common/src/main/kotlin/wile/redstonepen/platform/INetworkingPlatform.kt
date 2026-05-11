package wile.redstonepen.platform

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import wile.redstonepen.net.Networking.UnifiedPayload

interface INetworkingPlatform {
    fun registerPayloads()

    fun sendToPlayer(player: ServerPlayer, payload: UnifiedPayload)

    fun sendToAllPlayers(world: ServerLevel, payload: UnifiedPayload)
}
