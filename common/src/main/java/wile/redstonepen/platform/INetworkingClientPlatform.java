package wile.redstonepen.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface INetworkingClientPlatform
{
  void registerClientReceiver();
  void sendToServer(wile.redstonepen.net.Networking.UnifiedPayload payload);
}
