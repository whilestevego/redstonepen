package wile.redstonepen.libmc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface INetworkingClientPlatform
{
  void registerClientReceiver();
  void sendToServer(Networking.UnifiedPayload payload);
}
