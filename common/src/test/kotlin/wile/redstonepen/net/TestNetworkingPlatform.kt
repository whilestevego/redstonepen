package wile.redstonepen.net

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import wile.redstonepen.net.Networking.UnifiedPayload
import wile.redstonepen.platform.INetworkingPlatform

class TestNetworkingPlatform : INetworkingPlatform {
    companion object {
        val sendToPlayerCalls = mutableListOf<Pair<ServerPlayer, UnifiedPayload>>()
        val sendToAllPlayersCalls = mutableListOf<Pair<ServerLevel, UnifiedPayload>>()

        fun reset() {
            sendToPlayerCalls.clear()
            sendToAllPlayersCalls.clear()
        }
    }

    override fun registerPayloads() {}

    override fun sendToPlayer(player: ServerPlayer, payload: UnifiedPayload) {
        sendToPlayerCalls += player to payload
    }

    override fun sendToAllPlayers(world: ServerLevel, payload: UnifiedPayload) {
        sendToAllPlayersCalls += world to payload
    }
}
