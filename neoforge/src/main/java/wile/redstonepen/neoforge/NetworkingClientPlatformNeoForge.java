package wile.redstonepen.neoforge;

import wile.redstonepen.net.Networking;
import wile.redstonepen.platform.INetworkingClientPlatform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class NetworkingClientPlatformNeoForge implements INetworkingClientPlatform
{
  @Override
  public void registerClientReceiver()
  { /* NeoForge bidirectional payload registration handles client receive in NetworkingPlatformNeoForge */ }

  @Override
  public void sendToServer(Networking.UnifiedPayload payload)
  { PacketDistributor.sendToServer(payload); }
}
