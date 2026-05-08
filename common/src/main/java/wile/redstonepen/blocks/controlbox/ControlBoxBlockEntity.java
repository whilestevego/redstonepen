package wile.redstonepen.blocks.controlbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import wile.redstonepen.detail.RcaSync;
import wile.redstonepen.util.Auxiliaries;
import wile.redstonepen.net.Networking;
import wile.redstonepen.registry.Registries;
import wile.redstonepen.blocks.StandardEntityBlocks;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.UUID;


@SuppressWarnings("deprecation")
public class ControlBoxBlockEntity extends StandardEntityBlocks.StandardBlockEntity implements MenuProvider, Nameable, Networking.IPacketTileNotifyReceiver
{
  public static final int TICK_INTERVAL = 4;
  private final Container block_inventory_ = new SimpleContainer(1);
  private final ControlBoxLogic logic_ = new ControlBoxLogic();
  private UUID activating_player_ = null;
  private Component custom_name_ = null;
  private boolean trace_ = false;
  private int tick_timer_ = 0;
  private int tick_interval_ = 0;

  public ControlBoxBlockEntity(BlockPos pos, BlockState state)
  { super(Registries.getBlockEntityTypeOfBlock(state.getBlock()), pos, state); }

  @Override
  public CompoundTag readnbt(HolderLookup.Provider hlp, CompoundTag nbt)
  {
    if(nbt.contains("name", Tag.TAG_STRING)) custom_name_ = Auxiliaries.unserializeTextComponent(nbt.getString("name"), hlp);
    final CompoundTag logic_data = nbt.contains("logic", Tag.TAG_COMPOUND) ? nbt.getCompound("logic") : new CompoundTag();
    logic_.code(logic_data.getString("code"));
    logic_.input_data = logic_data.getInt("input");
    logic_.output_data = logic_data.getInt("output");
    final CompoundTag logic_symbols = logic_data.contains("symbols", Tag.TAG_COMPOUND) ? logic_data.getCompound("symbols") : new CompoundTag();
    logic_.clearSymbols();
    logic_symbols.getAllKeys().forEach(k->logic_.symbol(k, logic_symbols.getInt(k)));
    activating_player_ = nbt.hasUUID("player") ? nbt.getUUID("player") : null;
    return nbt;
  }

  @Override
  public CompoundTag writenbt(HolderLookup.Provider hlp, CompoundTag nbt, boolean sync_packet)
  {
    if(custom_name_ != null) nbt.putString("name", Auxiliaries.serializeTextComponent(custom_name_, hlp));
    final CompoundTag logic_data = new CompoundTag();
    logic_data.putString("code", logic_.code());
    logic_data.putInt("input", logic_.input_data);
    logic_data.putInt("output", logic_.output_data);
    final CompoundTag logic_symbols = new CompoundTag();
    logic_.symbols().forEach(logic_symbols::putInt);
    logic_data.put("symbols", logic_symbols);
    nbt.put("logic", logic_data);
    if(activating_player_ != null) nbt.putUUID("player", activating_player_);
    return nbt;
  }

  // BlockEntity/MenuProvider -------------------------------------------------------

  @Override
  public Component getName()
  {
    if(custom_name_ != null) return custom_name_;
    return Component.translatable(getBlockState().getBlock().getDescriptionId());
  }

  @Override
  @Nullable
  public Component getCustomName()
  { return custom_name_; }

  @Override
  public boolean hasCustomName()
  { return (custom_name_ != null); }

  public void setCustomName(Component name)
  { custom_name_ = name; }

  @Override
  public Component getDisplayName()
  { return Nameable.super.getDisplayName(); }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
  { return new ControlBoxUiContainer(id, inventory, block_inventory_, ContainerLevelAccess.create(level, worldPosition), new SimpleContainerData(1)); }

  @Override
  public void tick()
  {
    if(--tick_timer_ > 0) return;
    tick_timer_ = (tick_interval_>0) ? tick_interval_ : TICK_INTERVAL;
    final long tick = System.nanoTime();
    final Level world = getLevel();
    final BlockState device_state = getBlockState();
    final BlockPos device_pos = getBlockPos();
    final boolean device_enabled = (device_state.getValue(ControlBoxBlock.STATE) > 0) || (device_state.getValue(ControlBoxBlock.POWERED));
    if(!(device_state.getBlock() instanceof final ControlBoxBlock device_block)) return;
    final int last_output_data = logic_.output_data;
    final int last_input_data = logic_.input_data;
    final RcaSync.RcaData rca_data = (((logic_.rca_input_mask|logic_.rca_output_mask)==0) ? (RcaSync.CommonRca.EMPTY) : RcaSync.CommonRca.ofPlayer(activating_player_, false));
    try {
      // Input fetching
      {
        logic_.input_data = 0;
        for(Direction d:Direction.values()) {
          final Direction world_dir = ControlBoxBlock.getForwardStateMappedFacing(device_state, d);
          if(device_enabled) {
            // Comparator overrides only if really needed - may be inventories that do expensive lookups.
            final String port_name = Defs.PORT_NAMES.get(d.ordinal());
            if(logic_.usesSymbol(port_name+".co")) {
              final BlockPos target_pos = device_pos.relative(world_dir);
              final net.minecraft.world.level.block.state.BlockState target_state = world.getBlockState(target_pos);
              if(target_state.hasAnalogOutputSignal()) {
                final int cov = target_state.getAnalogOutputSignal(world, target_pos);
                logic_.symbol(port_name+".co", cov);
              } else {
                logic_.symbol(port_name+".co", 0);
              }
            }
          }
          if((logic_.output_mask & (0xf<<(4*d.ordinal()))) == 0) {
            final int p = world.getSignal(device_pos.relative(world_dir), world_dir);
            logic_.input_data |= (p & 0xf)<<(4*d.ordinal());
          }
        }
        if((logic_.rca_input_mask != 0) && (rca_data != RcaSync.CommonRca.EMPTY)) logic_.rca_input_data = (rca_data.client_inputs() & logic_.rca_input_mask);
      }
      // Logic processing
      {
        if(trace_) logic_.symbol(".perf1", (int)(Mth.clamp(System.nanoTime()-tick, 0, 0x7fffffff))/1000);
        if(!device_enabled) {
          logic_.output_data = 0;
        } else {
          logic_.symbol(".clock", (int)(world.getGameTime() & 0x7fffffffL)); // wraps over in >3years
          logic_.symbol(".time", (int)(world.getDayTime() % 24000));
          logic_.tick();
          if((logic_.rca_output_mask != 0) && (rca_data != RcaSync.CommonRca.EMPTY)) rca_data.server_outputs(logic_.rca_output_data);
        }
      }
      // Output setting
      {
        if(logic_.output_data != last_output_data) {
          for(Direction d:Direction.values()) {
            if((logic_.output_mask & 0xf<<(4*d.ordinal())) == 0) continue;
            final Direction world_dir = ControlBoxBlock.getForwardStateMappedFacing(device_state, d);
            device_block.notifyOutput(device_state, world, device_pos, world_dir);
          }
        }
        if((logic_.output_data != last_output_data) || (logic_.input_data != last_input_data)) world.blockEntityChanged(device_pos);
      }
    } catch(Throwable ex) {
      Auxiliaries.logError("RLC tick exception!" + ex);
      world.removeBlock(getBlockPos(), true);
      return;
    }
    // Elision of signal updates during this tick, including after output setting
    {
      if(logic_.symbols().containsKey("tickrate")) {
        tick_interval_ = Mth.clamp(logic_.symbols().getOrDefault("tickrate", 0), 0, 200);
        if(tick_interval_ == 0) tick_interval_ = TICK_INTERVAL;
      }
      tick_timer_ = tick_interval_;
      final int dl = logic_.symbol(".deadline");
      if((dl>0) && (dl<tick_timer_)) tick_timer_ = dl;
      logic_.intr_redges = 0;
      logic_.intr_fedges = 0;
      if(trace_) logic_.symbol(".perf2", (int)(Mth.clamp(System.nanoTime()-tick, 0, 0x7fffffff))/1000);
    }
  }

  @Override
  public void onServerPacketReceived(CompoundTag nbt)
  { readnbt(getLevel().registryAccess(), nbt); }

  // -------------------------------------------------------------------------------------------

  public boolean getEnabled()
  {
    // @todo: Transitional to prevent breaking setups. ON/OFF state will be "powered".
    return (getBlockState().getValue(ControlBoxBlock.STATE)!=0) || (getBlockState().getValue(ControlBoxBlock.POWERED));
  }

  public void setEnabled(boolean en)
  {
    if(en == getEnabled()) return;
    // @todo: Transitional to prevent breaking setups. ON/OFF state will be "powered".
    getLevel().setBlock(getBlockPos(), getBlockState().setValue(ControlBoxBlock.STATE, en?1:0), 1|2|16);
    getLevel().setBlock(getBlockPos(), getBlockState().setValue(ControlBoxBlock.POWERED, en), 1|2|16);
    if(!en) {
      logic_.clearSymbols();
      final RcaSync.RcaData rca_data = ((logic_.rca_output_mask)==0) ? (RcaSync.CommonRca.EMPTY) : RcaSync.CommonRca.ofPlayer(activating_player_, false);
      if(rca_data != RcaSync.CommonRca.EMPTY) rca_data.server_outputs(0);
    }
  }

  public void setRcaPlayerUUID(@Nullable UUID puid)
  {  activating_player_ = (puid==null) ? (null) : (UUID.fromString(puid.toString())); }

  public String getCode()
  { return logic_.code(); }

  public void setCode(String text)
  { logic_.code(text); }

  public int getOutputSignal(Direction internalSide)
  { return (logic_.output_data >> (4 * internalSide.ordinal())) & 0xf; }

  void scheduleImmediateTick()
  { tick_timer_ = 0; }

  CompoundTag collectSyncData(boolean full)
  {
    final Level world = getLevel();
    final CompoundTag nbt = new CompoundTag();
    nbt.putString("action", "serverdata");
    nbt.putBoolean("enabled", getEnabled());
    nbt.putInt("inputs", logic_.input_mask);
    nbt.putInt("outputs", logic_.output_mask);
    nbt.putInt("ports", (logic_.input_data & logic_.input_mask) | (logic_.output_data & logic_.output_mask));
    if(!logic_.symbols().isEmpty()) {
      final CompoundTag sym_nbt = new CompoundTag();
      logic_.symbols().forEach(sym_nbt::putInt);
      nbt.put("symbols", sym_nbt);
    }
    if(!logic_.valid()) {
      final CompoundTag err_nbt = new CompoundTag();
      logic_.errors().forEach((e,l)->err_nbt.putString(e.toString(), l));
      nbt.put("errors", err_nbt);
    } else {
      nbt.put("errors", new CompoundTag());
    }
    if(!full) return nbt;
    nbt.putBoolean("debug", trace_enabled());
    nbt.putString("code", getCode());
    if(activating_player_ != null) {
      final Player run_player = world.getPlayerByUUID(activating_player_);
      nbt.putString("player", (run_player == null) ? "" : run_player.getScoreboardName());
    }
    return nbt;
  }

  public void signal_update(Direction from_world_side, Direction from_mapped_side)
  {
    if(tick_interval_ > 0) return; // Fixed sample tick interval, RLC not reacting to signal edges.
    final int shift = 4*from_mapped_side.ordinal();
    final int mask = 0xf<<shift;
    if((logic_.input_mask & mask) == 0) return; // no input there
    int signal_intr = mask & (getLevel().getSignal(getBlockPos().relative(from_world_side), from_world_side)<<shift);
    int signal_data = mask & (logic_.input_data);
    if(signal_intr == signal_data) return; // no signal change
    if((signal_intr!=0) && (signal_data==0)) {
      logic_.intr_redges |= mask;
      tick_timer_ = 0;
    } else if(signal_intr==0) {
      logic_.intr_fedges |= mask;
      tick_timer_ = 0;
    }
    // Else no boolean "powered" signal changed. No need to update next tick.
  }

  public void toggle_trace(@Nullable Player player)
  { trace_ = !trace_; if(player!=null) Auxiliaries.playerChatMessage(player, "Trace: " + trace_); }

  public boolean trace_enabled()
  { return trace_; }

  public static final class TestHooks
  {
    private final ControlBoxLogic logic_ = new ControlBoxLogic();

    public boolean setCode(String text)
    { return logic_.code(text); }

    public boolean valid()
    { return logic_.valid(); }

    public Map<Integer, String> errors()
    { return Map.copyOf(logic_.errors()); }

    public int inputMask()
    { return logic_.input_mask; }

    public int outputMask()
    { return logic_.output_mask; }

    public void setInput(Direction side, int value)
    {
      final int shift = 4 * side.ordinal();
      final int mask = 0xf << shift;
      logic_.input_mask |= mask;
      logic_.input_data = (logic_.input_data & ~mask) | ((value & 0xf) << shift);
    }

    public void setSymbol(String key, int value)
    { logic_.symbol(key, value); }

    public int getSymbol(String key)
    { return logic_.symbol(key); }

    public void tick()
    { logic_.tick(); }

    public int output(Direction side)
    { return (logic_.output_data >> (4 * side.ordinal())) & 0xf; }

    public int outputData()
    { return logic_.output_data; }

    public void setRcaInput(int channel, int value)
    {
      final long mask = 0xfL << (channel * 4);
      logic_.rca_input_mask |= mask;
      logic_.rca_input_data = (logic_.rca_input_data & ~mask) | (((long)(value & 0xf)) << (channel * 4));
    }

    public int getRcaOutput(int channel)
    { return (int)((logic_.rca_output_data >> (channel * 4)) & 0xfL); }

    public long rcaOutputData()
    { return logic_.rca_output_data; }

  }

}
