package wile.redstonepen.platform

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import wile.redstonepen.net.Networking.UnifiedPayload

@Environment(EnvType.CLIENT)
interface INetworkingClientPlatform {
    fun registerClientReceiver()

    fun sendToServer(payload: UnifiedPayload)
}
