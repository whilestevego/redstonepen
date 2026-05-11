package wile.redstonepen.neoforge

import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.network.PacketDistributor
import wile.redstonepen.net.Networking
import wile.redstonepen.platform.INetworkingClientPlatform

@OnlyIn(Dist.CLIENT)
class NetworkingClientPlatformNeoForge : INetworkingClientPlatform {
    override fun registerClientReceiver() {}

    override fun sendToServer(payload: Networking.UnifiedPayload) =
        PacketDistributor.sendToServer(payload)
}
