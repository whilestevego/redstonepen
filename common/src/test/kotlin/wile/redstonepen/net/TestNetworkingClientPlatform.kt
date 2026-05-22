package wile.redstonepen.net

import wile.redstonepen.net.Networking.UnifiedPayload
import wile.redstonepen.platform.INetworkingClientPlatform

class TestNetworkingClientPlatform : INetworkingClientPlatform {
    companion object {
        val sendToServerCalls = mutableListOf<UnifiedPayload>()

        fun reset() {
            sendToServerCalls.clear()
        }
    }

    override fun registerClientReceiver() {}

    override fun sendToServer(payload: UnifiedPayload) {
        sendToServerCalls += payload
    }
}
