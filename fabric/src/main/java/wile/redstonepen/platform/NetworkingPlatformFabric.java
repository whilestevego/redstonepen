package wile.redstonepen.platform;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import wile.redstonepen.libmc.INetworkingPlatform;
import wile.redstonepen.libmc.Networking;

public class NetworkingPlatformFabric implements INetworkingPlatform
{
  @Override
  public void registerPayloads()
  {
    PayloadTypeRegistry.playC2S().register(Networking.UnifiedPayload.TYPE, Networking.UnifiedPayload.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(Networking.UnifiedPayload.TYPE, Networking.UnifiedPayload.STREAM_CODEC);
    ServerPlayNetworking.registerGlobalReceiver(Networking.UnifiedPayload.TYPE, (payload, context) ->
      Networking.handleServerReceive(payload, context.player())
    );
  }

  @Override
  public void sendToPlayer(ServerPlayer player, Networking.UnifiedPayload payload)
  { ServerPlayNetworking.send(player, payload); }

  @Override
  public void sendToAllPlayers(ServerLevel world, Networking.UnifiedPayload payload)
  { world.players().forEach(player -> ServerPlayNetworking.send(player, payload)); }
}
