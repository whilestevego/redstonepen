package wile.redstonepen.platform;

import wile.redstonepen.libmc.*;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkingPlatformNeoForge implements INetworkingPlatform
{
  private static PayloadRegistrar registrar;

  public static void setRegistrar(PayloadRegistrar r)
  { registrar = r; }

  @Override
  public void registerPayloads()
  {
    registrar.playBidirectional(
      Networking.UnifiedPayload.TYPE,
      Networking.UnifiedPayload.STREAM_CODEC,
      (payload, context) -> {
        context.enqueueWork(() -> {
          if(context.flow().isServerbound()) {
            Networking.handleServerReceive(payload, (net.minecraft.server.level.ServerPlayer)context.player());
          } else {
            Networking.handleClientReceive(payload, (net.minecraft.client.player.LocalPlayer)context.player());
          }
        });
      }
    );
  }

  @Override
  public void sendToPlayer(ServerPlayer player, Networking.UnifiedPayload payload)
  { PacketDistributor.sendToPlayer(player, payload); }

  @Override
  public void sendToAllPlayers(ServerLevel world, Networking.UnifiedPayload payload)
  { PacketDistributor.sendToPlayersInDimension(world, payload); }
}
