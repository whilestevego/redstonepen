package wile.redstonepen.blocks.controlbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import wile.redstonepen.libmc.Networking;
import wile.redstonepen.libmc.Registries;

import org.jetbrains.annotations.Nullable;


@SuppressWarnings("deprecation")
public class ControlBoxUiContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer
{
  protected static final int NUM_OF_SLOTS = 1;
  protected final Player player_;
  protected final Container inventory_;
  protected final ContainerLevelAccess wpc_;
  private final ContainerData fields_;
  private volatile CompoundTag received_server_data_ = new CompoundTag();
  //------------------------------------------------------------------------------------------------------------------
  public int field(int index) { return fields_.get(index); }
  public Player player() { return player_ ; }
  public Container inventory() { return inventory_ ; }
  public Level world() { return player_.level(); }
  public @Nullable ControlBoxBlockEntity te() { return wpc_.evaluate((w,p)->{net.minecraft.world.level.block.entity.BlockEntity te=w.getBlockEntity(p); return (te instanceof ControlBoxBlockEntity cbte) ? (cbte): (null); }).orElse(null); }
  //------------------------------------------------------------------------------------------------------------------

  public ControlBoxUiContainer(int cid, Inventory player_inventory)
  { this(cid, player_inventory, new SimpleContainer(ControlBoxUiContainer.NUM_OF_SLOTS), ContainerLevelAccess.NULL, new SimpleContainerData(1)); }

  public ControlBoxUiContainer(int cid, Inventory player_inventory, Container block_inventory, ContainerLevelAccess wpc, ContainerData fields)
  {
    super(Registries.getMenuTypeOfBlock("control_box"), cid);
    player_ = player_inventory.player;
    inventory_ = block_inventory;
    wpc_ = wpc;
    wpc_.execute((w,p)->inventory_.startOpen(player_));
    fields_ = fields;
    addDataSlots(fields_);
    for(int x=0; x<9; ++x) addSlot(new Slot(player_inventory, x, 28+x*18, 183)); // player hotbar slots: 0..8
  }

  @Override
  public boolean stillValid(Player player)
  { return inventory_.stillValid(player); }

  @Override
  public void removed(Player player)
  { super.removed(player); inventory_.stopOpen(player); }

  @Override
  public void sendAllDataToRemote()
  {
    super.sendAllDataToRemote();
    if((world().isClientSide) || (te()==null)) return;
    Networking.PacketContainerSyncServerToClient.sendToListeners(world(), this, composeServerData(te(), true));
  }

  @Override
  public ItemStack quickMoveStack(Player player, int slot)
  { return ItemStack.EMPTY; }

  // Container client/server synchronization --------------------------------------------------

  public CompoundTag composeServerData(ControlBoxBlockEntity te, boolean full)
  { return te.collectSyncData(full); }

  public CompoundTag fetchReceivedServerData()
  {
    final CompoundTag received = received_server_data_;
    received_server_data_ = new CompoundTag();
    return received;
  }

  @Override
  public void onServerPacketReceived(int windowId, CompoundTag nbt)
  {
    switch(nbt.getString("action")) {
      case "serverdata" -> { received_server_data_ = nbt; }
      default -> {}
    }
  }

  @Override
  public void onClientPacketReceived(int windowId, Player player, CompoundTag nbt)
  {
    final ControlBoxBlockEntity te = te();
    if(te==null) return;
    int sync = 0;
    switch(nbt.getString("action")) {
      case "codeupdate" -> { te.setCode(nbt.getString("code")); }
      case "serverdata" -> { sync = 2; }
      case "servervalues" -> { sync = 1; }
      case "enabled" -> {
        te.setEnabled(!te.getEnabled());
        te.setRcaPlayerUUID((te.getEnabled() && nbt.getBoolean("withrca")) ? player.getUUID() : null);
        sync = 2;
      }
      default -> {
      }
    }
    if(sync > 0) Networking.PacketContainerSyncServerToClient.sendToListeners(world(), this, composeServerData(te, sync>1));
  }
}
