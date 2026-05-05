/*
 * @file NetworkingClient.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main client/server message handling.
 */
package wile.redstonepen.libmc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;


public class NetworkingClient
{
  public static void clientInit()
  {
    PlatformServices.getNetworkingClient().registerClientReceiver();
  }

  @Environment(EnvType.CLIENT)
  private static void send(String packet_id, CompoundTag payload_nbt)
  {
    PlatformServices.getNetworkingClient().sendToServer(new Networking.UnifiedPayload(new Networking.UnifiedPayload.UnifiedData(packet_id, payload_nbt)));
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity notifications
  //--------------------------------------------------------------------------------------------------------------------

  @Environment(EnvType.CLIENT)
  public static class PacketTileNotifyClientToServer extends Networking.PacketTileNotifyClientToServer
  {
    public static void sendToServer(BlockPos pos, CompoundTag nbt)
    {
      if((pos==null) || (nbt==null)) return;
      final CompoundTag payload = new CompoundTag();
      payload.putLong("pos", pos.asLong());
      payload.put("nbt", nbt);
      send(PacketTileNotifyClientToServer.PACKET_ID, payload);
    }

    public static void sendToServer(BlockEntity te, CompoundTag nbt)
    { if(te!=null) sendToServer(te.getBlockPos(), nbt); }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // (GUI) Container synchronization
  //--------------------------------------------------------------------------------------------------------------------

  @Environment(EnvType.CLIENT)
  public static class PacketContainerSyncClientToServer extends Networking.PacketContainerSyncClientToServer
  {
    public static void sendToServer(int container_id, CompoundTag nbt)
    {
      if(nbt==null) return;
      final CompoundTag payload = new CompoundTag();
      payload.putInt("cid", container_id);
      payload.put("nbt", nbt);
      send(PacketContainerSyncClientToServer.PACKET_ID, payload);
    }

    public static void sendToServer(AbstractContainerMenu container, CompoundTag nbt)
    { sendToServer(container.containerId, nbt); }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // World notifications
  //--------------------------------------------------------------------------------------------------------------------

  @Environment(EnvType.CLIENT)
  public static class PacketNbtNotifyClientToServer extends Networking.PacketNbtNotifyClientToServer
  {
    public static void sendToServer(CompoundTag nbt)
    {
      send(PacketNbtNotifyClientToServer.PACKET_ID, nbt);
    }
  }

}
