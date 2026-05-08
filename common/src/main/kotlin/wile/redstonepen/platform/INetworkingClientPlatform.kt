package wile.redstonepen.platform

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.CLIENT)
interface INetworkingClientPlatform {
    fun registerClientReceiver()
    fun sendToServer(payload: wile.redstonepen.net.Networking.UnifiedPayload)
}
