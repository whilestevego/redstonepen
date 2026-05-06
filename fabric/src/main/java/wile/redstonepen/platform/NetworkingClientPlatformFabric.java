package wile.redstonepen.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import wile.redstonepen.libmc.INetworkingClientPlatform;
import wile.redstonepen.libmc.Networking;

@Environment(EnvType.CLIENT)
public class NetworkingClientPlatformFabric implements INetworkingClientPlatform
{
  @Override
  public void registerClientReceiver()
  {
    ClientPlayNetworking.registerGlobalReceiver(Networking.UnifiedPayload.TYPE, (payload, context) ->
      Networking.handleClientReceive(payload, context.player())
    );
  }

  @Override
  public void sendToServer(Networking.UnifiedPayload payload)
  { ClientPlayNetworking.send(payload); }
}
