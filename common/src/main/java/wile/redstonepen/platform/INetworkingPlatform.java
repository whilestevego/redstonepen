package wile.redstonepen.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkingPlatform
{
  void registerPayloads();
  void sendToPlayer(ServerPlayer player, wile.redstonepen.net.Networking.UnifiedPayload payload);
  void sendToAllPlayers(ServerLevel world, wile.redstonepen.net.Networking.UnifiedPayload payload);
}
