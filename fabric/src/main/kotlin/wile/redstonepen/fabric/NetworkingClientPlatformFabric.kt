package wile.redstonepen.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import wile.redstonepen.net.Networking
import wile.redstonepen.platform.INetworkingClientPlatform

@Environment(EnvType.CLIENT)
class NetworkingClientPlatformFabric : INetworkingClientPlatform {
    override fun registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(Networking.UnifiedPayload.TYPE) {
            payload,
            context ->
            Networking.handleClientReceive(payload, context.player())
        }
    }

    override fun sendToServer(payload: Networking.UnifiedPayload) {
        ClientPlayNetworking.send(payload)
    }
}
