package wile.redstonepen.libmc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface INetworkingPlatform
{
  void registerPayloads();
  void sendToPlayer(ServerPlayer player, Networking.UnifiedPayload payload);
  void sendToAllPlayers(ServerLevel world, Networking.UnifiedPayload payload);
}
