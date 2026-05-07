/*
 * @file TrackBlockEntity.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen.blocks.track;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModContent;
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections;
import wile.redstonepen.items.RedstonePenItem;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.Networking;
import wile.redstonepen.libmc.Registries;
import wile.redstonepen.libmc.RsSignals;
import wile.redstonepen.libmc.StandardEntityBlocks;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class TrackBlockEntity extends StandardEntityBlocks.StandardBlockEntity implements Networking.IPacketTileNotifyReceiver
{
  private long state_flags_ = 0;      // server/client
  private final List<TrackNet> nets_ = new ArrayList<>();
  private final Block[] block_change_tracking_ = {Blocks.AIR,Blocks.AIR,Blocks.AIR,Blocks.AIR,Blocks.AIR,Blocks.AIR};
  private boolean trace_ = false;

  public TrackBlockEntity(BlockPos pos, BlockState state)
  { super(Registries.getBlockEntityTypeOfBlock(state.getBlock()), pos, state); }

  @Override
  public CompoundTag readnbt(HolderLookup.Provider hlp, CompoundTag nbt)
  {
    state_flags_ = nbt.getLong("sflags");
    nets_.clear();
    if(nbt.contains("nets", Tag.TAG_LIST)) {
      final ListTag lst = nbt.getList("nets", Tag.TAG_COMPOUND);
      try {
        for(int i=0; i<lst.size(); ++i) {
          CompoundTag route_nbt = lst.getCompound(i);
          nets_.add(new TrackNet(
            Arrays.stream(route_nbt.getLongArray("npos")).mapToObj(BlockPos::of).collect(Collectors.toList()),
            Arrays.stream(route_nbt.getIntArray("nsid")).mapToObj(Direction::from3DDataValue).collect(Collectors.toList()),
            Arrays.stream(route_nbt.getIntArray("ifac")).mapToObj(Direction::from3DDataValue).collect(Collectors.toList()),
            Arrays.stream(route_nbt.getIntArray("pfac")).mapToObj(Direction::from3DDataValue).collect(Collectors.toList()),
            route_nbt.getInt("power")
          ));
        }
      } catch(Throwable ex) {
        nets_.clear();
        Auxiliaries.logError("Dropped invalid NBT for Redstone Track at pos " + getBlockPos());
      }
    }
    return nbt;
  }

  @Override
  public CompoundTag writenbt(HolderLookup.Provider hlp, CompoundTag nbt, boolean sync_packet)
  {
    nbt.putLong("sflags", state_flags_);
    if(sync_packet) return nbt;
    if(!nets_.isEmpty()) {
      final ListTag lst = new ListTag();
      for(TrackNet net: nets_) {
        CompoundTag route_nbt = new CompoundTag();
        route_nbt.putInt("power", net.getPower());
        route_nbt.put("npos", new LongArrayTag(net.neighbour_positions.stream().map(BlockPos::asLong).collect(Collectors.toList())));
        route_nbt.put("nsid", new IntArrayTag(net.neighbour_sides.stream().map(Direction::get3DDataValue).collect(Collectors.toList())));
        route_nbt.put("ifac", new IntArrayTag(net.internal_sides.stream().map(Direction::get3DDataValue).collect(Collectors.toList())));
        route_nbt.put("pfac", new IntArrayTag(net.power_sides.stream().map(Direction::get3DDataValue).collect(Collectors.toList())));
        lst.add(route_nbt);
      }
      nbt.put("nets", lst);
    }
    return nbt;
  }

  @Override
  public void onServerPacketReceived(CompoundTag nbt)
  { readnbt(getLevel().registryAccess(), nbt); }

  @Override
  public void onClientPacketReceived(Player player, CompoundTag nbt)
  {}

  @Nullable
  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket()
  { return ClientboundBlockEntityDataPacket.create(this); }

  @Environment(EnvType.CLIENT)
  public double getViewDistance()
  { return 64; }

  /// -------------------------------------------------------------------------------------------

  public boolean sync(boolean schedule)
  {
    if(level.isClientSide()) return true;
    setChanged();
    if(schedule && (!getLevel().getBlockTicks().hasScheduledTick(getBlockPos(), ModContent.references.TRACK_BLOCK))) {
      getLevel().scheduleTick(getBlockPos(), ModContent.references.TRACK_BLOCK, 1);
    } else {
      Networking.PacketTileNotifyServerToClient.sendToPlayers(this, writenbt(getLevel().registryAccess(), new CompoundTag(), true));
    }
    return true;
  }

  public long getStateFlags()
  { return state_flags_; }

  public int addWireFlags(long flags)
  {
    int n_added = 0;
    for(int i=0; i<getWireFlagCount(); ++i) {
      long mask = 1L<<i;
      if(((flags & mask)!=0) && ((state_flags_ & mask))==0) {
        state_flags_ |= mask;
        ++n_added;
      }
    }
    return n_added;
  }

  public int getWireFlags()
  { return (int)((state_flags_ & RedstoneTrackDefs.STATE_FLAG_WIR_MASK)>>RedstoneTrackDefs.STATE_FLAG_WIR_POS); }

  public boolean getWireFlag(int index)
  { return (state_flags_ & (1L<<(RedstoneTrackDefs.STATE_FLAG_WIR_POS+index))) != 0; }

  public int getWireFlagCount()
  { return RedstoneTrackDefs.STATE_FLAG_WIR_COUNT; }

  public int getConnectionFlags()
  { return (int)((state_flags_ & RedstoneTrackDefs.STATE_FLAG_CON_MASK)>>RedstoneTrackDefs.STATE_FLAG_CON_POS); }

  public boolean getConnectionFlag(int index)
  { return (state_flags_ & (1L<<(RedstoneTrackDefs.STATE_FLAG_CON_POS+index))) != 0; }

  public int getConnectionFlagCount()
  { return RedstoneTrackDefs.STATE_FLAG_CON_COUNT; }

  public int getSidePower(Direction side)
  {
    final int shift = RedstoneTrackDefs.STATE_FLAG_PWR_POS + 4*connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0);
    return (int)((state_flags_>>shift) & 0xf);
  }

  public void setSidePower(Direction side, int p)
  {
    final int shift = RedstoneTrackDefs.STATE_FLAG_PWR_POS + 4*connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0);
    state_flags_ = (state_flags_ & ~(((long)(0xf))<<shift)) | (((long)(p & 0xf))<<shift);
  }

  public boolean hasVanillaRedstoneConnection(Direction side)
  { return RedstoneTrackDefs.connections.hasVanillaWireConnection(getStateFlags(), side) || ((state_flags_ & RedstoneTrackDefs.connections.getBulkConnectorBit(side))!=0); }

  public int getRedstonePower(Direction redstone_side, boolean weak)
  {
    if(isRemoved()) return 0;
    final Direction own_side = redstone_side.getOpposite();
    int p = 0;
    for(TrackNet net:nets_) {
      if(!net.power_sides.contains(own_side)) continue;
      p = Math.max(p, net.getPower());
      if(p >= 15) break;
    }
    p = ((p <= 0) || (!(getLevel().getBlockState(getBlockPos().relative(own_side)).is(Blocks.REDSTONE_WIRE)))) ? p : (p-1);
    // if(trace_) Auxiliaries.logWarn(String.format("POWR: %s @%s==%d", posstr(getBlockPos()), redstone_side, p));
    return p;
  }

  public int getRedstoneDustCount()
  {
    int n = 0;
    {
      int rem = getWireFlags();
      for(int i=0; (rem!=0) && (i<RedstoneTrackDefs.STATE_FLAG_WIR_COUNT); ++i) {
        if((rem & 1L) != 0) ++n;
        rem >>= 1;
      }
    }
    {
      int rem = getConnectionFlags();
      for(int i=0; (rem!=0) && (i<RedstoneTrackDefs.STATE_FLAG_CON_COUNT); ++i) {
        if((rem & 1L) != 0) ++n;
        rem >>= 1;
      }
    }
    return n;
  }

  public void toggle_trace(@Nullable Player player)
  {
    if(!Auxiliaries.isDevelopmentMode()) {
      trace_ = false;
      if(player!=null) Auxiliaries.playerChatMessage(player, "Trace disabled, not in development mode.");
    } else {
      trace_ = !trace_;
      if(player!=null) Auxiliaries.playerChatMessage(player, "Trace: " + trace_);
    }
  }

  int modifySegments(BlockPos pos, Player player, ItemStack used_stack, Direction clicked_face, Vec3 hitvec, boolean no_add, boolean no_remove, boolean no_bulk)
  {
    if((!used_stack.isEmpty()) && (used_stack.getItem()!=Items.REDSTONE) && (!RedstonePenItem.isPen(used_stack))) return 0;
    long flip_mask;
    final Direction face = clicked_face.getOpposite();
    {
      final Vec3 hit_r = hitvec.subtract(Vec3.atCenterOf(pos));
      Vec3 hit = switch (clicked_face) {
        case WEST, EAST   -> hit_r.multiply(0, 1, 1);
        case SOUTH, NORTH -> hit_r.multiply(1, 1, 0);
        default           -> hit_r.multiply(1, 0, 1);
      };
      final Direction dir = Direction.getNearest(hit.x(), hit.y(), hit.z());
      final boolean face_is_empty = (getWireFlags() & RedstoneTrackDefs.connections.getAllElementsOnFace(face)) == 0;
      if((!no_bulk) && (!face_is_empty) && (hit.length() < 0.10) && ((!no_add) || (getConnectionFlags()!=0))) {
        // Centre connection
        flip_mask = RedstoneTrackDefs.connections.getBulkConnectorBit(face);
      } else if(no_add || (hit.length() > 0.11)) {
        // Wire
        flip_mask = RedstoneTrackDefs.connections.getWireBit(face, dir);
        if(!no_add) {
          if(isAdjacentWireSegmentAddable(face, dir.getOpposite())) flip_mask |= RedstoneTrackDefs.connections.getWireBit(face, dir.getOpposite());
          if(isAdjacentWireSegmentAddable(face, dir.getClockWise(face.getAxis()))) flip_mask |= RedstoneTrackDefs.connections.getWireBit(face, dir.getClockWise(face.getAxis()));
          if(isAdjacentWireSegmentAddable(face, dir.getCounterClockWise(face.getAxis()))) flip_mask |= RedstoneTrackDefs.connections.getWireBit(face, dir.getCounterClockWise(face.getAxis()));
        }
      } else {
        // Not far enough from the centre to distinghush the inended segment direction.
        flip_mask = 0;
      }
    }
    // Explicit assignment not just `state_flags_ ^ flip_mask`.
    int material_use = 0;
    {
      if((state_flags_ & flip_mask) != 0) {
        if(!no_remove) {
          // Remove segment.
          state_flags_ &= ~flip_mask;
          material_use -= 1;
          // Implicitly remove left-over unconnected bulk connector.
          final long bc = RedstoneTrackDefs.connections.getBulkConnectorBit(face);
          if((state_flags_ & RedstoneTrackDefs.connections.getAllElementsOnFace(face)) == bc) {
            state_flags_ &= ~bc;
            material_use -= 1;
          }
          // Reset BC also on other faces.
          if(getWireFlags()==0) {
            material_use -= getRedstoneDustCount();
            state_flags_ = 0;
          }
        }
      } else if(!no_add) {
        // Check if segment already there, add to state.
        for(int i=0; i<RedstoneTrackDefs.STATE_FLAG_PWR_POS; ++i) {
          final long mask = 1L<<i;
          if(((flip_mask & mask) == 0) || ((getStateFlags() & mask) != 0)) continue;
          if(!RedstonePenItem.hasEnoughRedstone(used_stack, material_use+1, player)) break;
          state_flags_ |= mask;
          material_use += 1;
        }
      }
    }
    // Selecively update power of internal tracks and external connected blocks.
    if(material_use != 0) {
      List<BlockPos> connected, disconnected;
      final int initial_side_power = getSidePower(face);
      {
        // updateConnections() covers about all situations for internal power track updates
        // and external notification required, except for bulk connections, where external wire
        // connections are hidden due to the "around-upate" of the bulk connector. In this case
        // the net list for the corresponding side is inspected separately to add the changed
        // external wire connections.
        setSidePower(face,0);
        final Map<BlockPos,BlockPos> change_notifications_before = updateAllPowerValuesFromAdjacent();
        final Set<BlockPos> net_neighbours_before = nets_.stream().filter(net->net.internal_sides.contains(face)).map(net->net.neighbour_positions).findFirst().map(HashSet::new).orElse(new HashSet<>());
        updateConnections(1);
        setSidePower(face,0);
        final Map<BlockPos,BlockPos> change_notifications_after = updateAllPowerValuesFromAdjacent();
        disconnected = change_notifications_before.keySet().stream().filter(p -> !change_notifications_after.containsKey(p)).collect(Collectors.toList());
        connected = change_notifications_after.keySet().stream().filter((p) -> !change_notifications_before.containsKey(p)).collect(Collectors.toList());
        if(connected.isEmpty() && disconnected.isEmpty() && RedstoneTrackDefs.connections.hasBulkConnection(getStateFlags(), face)) {
          // Bulk may update everything around, hiding external tracks that need updating.
          final Set<BlockPos> net_neighbours_after = nets_.stream().filter(net->net.internal_sides.contains(face)).map(net->net.neighbour_positions).findFirst().map(HashSet::new).orElse(new HashSet<>());
          for(BlockPos p:net_neighbours_after) { if(!net_neighbours_before.contains(p)) connected.add(p); }
          for(BlockPos p:net_neighbours_before) { if(!net_neighbours_after.contains(p)) disconnected.add(p); }
        }
      }
      if(connected.isEmpty() && disconnected.isEmpty()) {
        setSidePower(face, initial_side_power);
      } else {
        setSidePower(face, 0);
        nets_.forEach(net->{ if(net.internal_sides.contains(face)) net.setPower(0); });
        disconnected.forEach((p)->{
          BlockEntity te = getLevel().getBlockEntity(p);
          getLevel().getBlockState(p).handleNeighborChanged(getLevel(), p, getBlock(), pos, false);
          if(te instanceof TrackBlockEntity) ((TrackBlockEntity)te).updateConnections(1);
        });
        connected.forEach((p)->{
          BlockEntity te = getLevel().getBlockEntity(p);
          if(te instanceof TrackBlockEntity) ((TrackBlockEntity)te).updateConnections(1);
          getLevel().getBlockState(p).handleNeighborChanged(getLevel(), p, getBlock(), pos, false);
          getBlock().neighborChanged(getBlockState(), getLevel(), getBlockPos(), getBlock(), p, false);
        });
      }
      sync(true);
    }
    return material_use;
  }

  private boolean isAdjacentWireSegmentAddable(Direction face, Direction dir)
  {
    if((getStateFlags() & RedstoneTrackDefs.connections.getWireBit(face, dir)) != 0) return false; // Already there.
    final BlockPos pos = getBlockPos().relative(dir);
    // Check track
    final TrackBlockEntity te = RedstoneTrackBlock.tile(getLevel(), pos).orElse(null);
    if(te != null) return ((te.getStateFlags() & RedstoneTrackDefs.connections.getWireBit(face, dir.getOpposite())) != 0);
    // Check wires and sources
    final BlockState state = getLevel().getBlockState(pos);
    return (state.is(Blocks.REDSTONE_WIRE) || state.isSignalSource());
  }

  private static final List<Vec3i> updatepower_order = new ArrayList<>();

  public Map<BlockPos,BlockPos> updateAllPowerValuesFromAdjacent()
  {
    if(updatepower_order.isEmpty()) {
      for(Direction side:Direction.values()) {
        updatepower_order.add(new Vec3i(0, 0, 0).relative(side, 1));
      }
      for(int x=-1; x<=1; ++x) {
        for(int y=-1; y<=1; ++y) {
          for(int z=-1; z<=1; ++z) {
            if(Math.abs(x)+Math.abs(y)+Math.abs(z) == 2) updatepower_order.add(new Vec3i(x,y,z));
          }
        }
      }
    }
    final Map<BlockPos,BlockPos> all_change_notifications = new HashMap<>();
    for(Vec3i ofs:updatepower_order) {
      handleNeighborChanged(getBlockPos().offset(ofs)).forEach(all_change_notifications::putIfAbsent);
    }
    return all_change_notifications;
  }

  private void spawnRedstoneItems(int count)
  {
    if(count <= 0) return;
    final ItemEntity e = new ItemEntity(getLevel(), getBlockPos().getX()+.5, getBlockPos().getY()+.5, getBlockPos().getZ()+.5, new ItemStack(Items.REDSTONE, count));
    e.setDefaultPickUpDelay();
    e.setDeltaMovement(new Vec3(getLevel().getRandom().nextDouble()-.5, getLevel().getRandom().nextDouble()-.5, getLevel().getRandom().nextDouble()).scale(0.1));
    getLevel().addFreshEntity(e);
  }

  private RedstoneTrackBlock getBlock()
  { return ModContent.references.TRACK_BLOCK; }

  public boolean handleShapeUpdate(Direction facing, BlockState facingState, BlockPos fromPos, boolean isMoving)
  {
    boolean update_neighbours = false;
    if(!RedstoneTrackBlock.canBePlacedOnFace(facingState, getLevel(), fromPos, facing.getOpposite())) {
      final long to_remove = RedstoneTrackDefs.connections.getAllElementsOnFace(facing);
      final long new_flags = (state_flags_ & ~to_remove);
      if(new_flags != state_flags_) {
        if(trace_) Auxiliaries.logWarn(String.format("SHUP: %s <-%s(=%s) removed.", posstr(getBlockPos()), posstr(fromPos), facingState.getBlock().getDescriptionId()));
        int count = getRedstoneDustCount();
        state_flags_ = new_flags;
        count -= getRedstoneDustCount();
        spawnRedstoneItems(count);
        updateConnections(1);
        update_neighbours = true;
      }
    }
    Block bltv = block_change_tracking_[facing.get3DDataValue()];
    if(bltv != facingState.getBlock()) {
      if(bltv == null) bltv = Blocks.AIR;
      if(trace_) Auxiliaries.logWarn(String.format("SHUP: %s <-%s changed (%s->%s).", posstr(getBlockPos()), posstr(fromPos), bltv.getDescriptionId(), facingState.getBlock().getDescriptionId()));
      block_change_tracking_[facing.get3DDataValue()] = facingState.getBlock();
      if(!isMoving && (bltv != Blocks.REDSTONE_BLOCK)) updateConnections(1); // Redstone Blocks are frequently used with Pistons and implicitly emit a neighbour changed.
      update_neighbours = true;
    }
    if(update_neighbours) {
      final Level world = getLevel();
      final Block block = getBlock();
      handleNeighborChanged(fromPos).forEach((chpos, frpos)->world.neighborChanged(chpos, block, frpos));
    }
    return (getWireFlags()!=0);
  }

  private int getNonWireSignal(Level world, BlockPos pos, Direction redstone_side)
  {
    // According to world.getRedstonePower():
    getBlock().disablePower(true);
    final BlockState state = world.getBlockState(pos);
    int p = (!state.is(Blocks.REDSTONE_WIRE) && (!state.is(getBlock()))) ? state.getSignal(world, pos, redstone_side) : 0;
    //if(trace_) Auxiliaries.logWarn(String.format("GETNWS from [%s @ %s] = %dw", posstr(getPos()), redstone_side, p));
    if(!RsSignals.canEmitWeakPower(state, world, pos, redstone_side)) { getBlock().disablePower(false); return p; }
    // According to world.getDirectSignalTo():
    for(Direction rs_side: Direction.values()) {
      final BlockPos side_pos = pos.relative(rs_side);
      final BlockState side_state = world.getBlockState(side_pos);
      if(side_state.is(Blocks.REDSTONE_WIRE) || side_state.is(getBlock())) continue;
      final int p_in = side_state.getDirectSignal(world, side_pos, rs_side);
      if(p_in > p) {
        p = p_in;
        if(p >= 15) break;
      }
    }
    getBlock().disablePower(false);
    //if(trace_) Auxiliaries.logWarn(String.format("GETNWS from [%s @ %s] = %dS", posstr(getPos()), redstone_side, p));
    return p;
  }

  private boolean isNetConnectedTo(BlockPos pos, TrackNet net, BlockPos otherPos, @Nullable Direction otherSide, @Nullable TrackNet otherNet)
  {
    if(otherNet == null) return net.neighbour_positions.stream().anyMatch(np->np.equals(otherPos)); // no track, only positional block-connection check.
    for(var i=0; i<net.neighbour_positions.size(); ++i) {
      if(!net.neighbour_positions.get(i).equals(otherPos)) continue;
      final Direction nb_side = net.neighbour_sides.get(i);
      if((otherSide != null) && (!otherSide.equals(nb_side))) continue;
      if(!otherNet.internal_sides.contains(nb_side)) continue;
      //if(trace_) Auxiliaries.logWarn(String.format("NBCH:       -> isNetConnectedTo()==true, pos=%s, net=%s ||| from=%s, fromnet=%s)?", posstr(pos), net, posstr(otherPos), otherNet));
      return true;
    }
    //if(trace_) Auxiliaries.logWarn(String.format("NBCH:       -> isNetConnectedTo()==false, pos=%s, net=%s ||| from=%s, fromnet=%s)?", posstr(pos), net, posstr(otherPos), otherNet));
    return false;
  }

  public Map<BlockPos,BlockPos> handleNeighborChanged(BlockPos fromPos)
  {
    final Map<BlockPos,BlockPos> notifications = new LinkedHashMap<>();
    nets_.stream().filter(net->net.neighbour_positions.contains(fromPos)).forEach((net)->handleNetNeighborChanged(net, fromPos, null, notifications));
    final BlockState fst = getLevel().getBlockState(fromPos);
    if(fst.is(getBlock()) || fst.isSignalSource()) notifications.remove(fromPos);
    if(trace_ && (notifications.size() > 0)) Auxiliaries.logWarn(String.format("NBCH: %s updates: [%s]", posstr(getBlockPos()), notifications.entrySet().stream().map(kv-> posstr(kv.getValue())+">"+posstr(kv.getKey())).collect(Collectors.joining(", "))));
    return notifications;
  }

  public void handleNetNeighborChanged(TrackNet net, BlockPos fromPos, @Nullable TrackNet fromNet, @Nullable Map<BlockPos,BlockPos> change_notifications)
  {
    record Neighbor(BlockPos pos, Direction side, int power, boolean direct_update, boolean needs_indirect) {}
    final BlockPos my_pos = getBlockPos();
    if(!isNetConnectedTo(my_pos, net, fromPos, null, fromNet)) return;
    final Level world = getLevel();
    final List<Neighbor> neighbors = new LinkedList<>();
    if(trace_) Auxiliaries.logWarn(String.format("NBCH: %s from %s (%s)", posstr(my_pos), posstr(fromPos), world.getBlockState(fromPos).getBlock().getDescriptionId()));
    int pmax = 0;
    for(int i = 0; i<net.neighbour_positions.size(); ++i) {
      final BlockPos ext_pos = net.neighbour_positions.get(i);
      final Direction ext_side = net.neighbour_sides.get(i);
      final BlockState ext_state = level.getBlockState(ext_pos);
      if(ext_state.is(Blocks.REDSTONE_WIRE)) {
        final int p_vanilla_wire = ext_state.getValue(RedStoneWireBlock.POWER);
        neighbors.add(new Neighbor(ext_pos, ext_side, p_vanilla_wire, false, false));
        pmax = Math.max(pmax, p_vanilla_wire-1);
      } else if(ext_state.is(getBlock())) {
        final TrackNet nb_net = RedstoneTrackBlock.tile(world, ext_pos).flatMap( te->te.nets_.stream().filter( nbn->isNetConnectedTo(my_pos, net, ext_pos, ext_side, nbn) ).findFirst() ).orElse(null);
        if(nb_net != null) {
          final int p_track = Math.max(0, nb_net.getPower());
          neighbors.add(new Neighbor(ext_pos, ext_side, p_track, true, false));
          pmax = Math.max(pmax, p_track-1);
        }
      } else if(ext_state.is(ModContent.references.BRIDGE_RELAY_BLOCK)) {
        final int p_nowire = getNonWireSignal(world, ext_pos, ext_side.getOpposite());
        neighbors.add(new Neighbor(ext_pos, ext_side, p_nowire, true, false));
        pmax = Math.max(pmax, p_nowire);
      } else {
        final int p_nowire = getNonWireSignal(world, ext_pos, ext_side.getOpposite());
        final boolean weak_updates = (!ext_state.isSignalSource()) && (p_nowire == 0) && ext_state.isRedstoneConductor(world, ext_pos);
        neighbors.add(new Neighbor(ext_pos, ext_side, p_nowire, false, weak_updates));
        pmax = Math.max(pmax, p_nowire);
      }
    }
    boolean power_changed = false;
    if(net.getPower() != pmax) {
      if(trace_) Auxiliaries.logWarn(String.format("NBCH: %s net power %d->%d", posstr(my_pos), net.getPower(), pmax));
      net.setPower(pmax);
      power_changed = true;
    }
    for(Direction side: net.internal_sides) {
      if(getSidePower(side) != pmax) {
        setSidePower(side, pmax);
        power_changed = true;
      }
    }
    if(!power_changed) {
      return;
    }
    //if(trace_) Auxiliaries.logWarn(String.format("NBCH: %s updating %d neighbours ...", posstr(my_pos), neighbors.size()));
    for(Neighbor neighbor: neighbors) {
      if(neighbor.direct_update) {
        if(world.getBlockEntity(neighbor.pos) instanceof TrackBlockEntity te) {
          //if(trace_) Auxiliaries.logWarn(String.format("NBCH: %s trackupdate %s->%d", posstr(my_pos), posstr(neighbor.pos), pmax));
          for(var nb_net: te.nets_) {
            te.handleNetNeighborChanged(nb_net, my_pos, net, change_notifications);
          }
        } else {
          world.getBlockState(neighbor.pos).handleNeighborChanged(world, neighbor.pos, getBlock(), my_pos, false);
        }
      } else {
        change_notifications.putIfAbsent(neighbor.pos, my_pos);
        if(neighbor.needs_indirect) {
          for(Direction update_direction: RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS) {
            if(neighbor.side == update_direction) continue;
            change_notifications.putIfAbsent(neighbor.pos.relative(update_direction), neighbor.pos);
          }
        }
      }
    }
    sync(true);
  }

  static String posstr(BlockPos pos)
  { return "[" +pos.getX()+ "," +pos.getY()+ "," +pos.getZ()+ "]"; }

  static String dirstr(@Nullable Direction dir)
  { return (dir==null) ? ("?") : (dir.toString().substring(0,1)); }

  @SuppressWarnings("all")
  private boolean isRedstoneInsulator(BlockState state, BlockPos pos)
  { return state.is(Blocks.GLASS) || state.is(Blocks.AIR); } // don't care about isRedstoneConductor(), messes up depending on block implementations.

  void updateConnections(int recursion_left)
  {
    final Set<BlockPos> all_neighbours = new HashSet<>();
    final int[] current_side_powers = {0,0,0,0,0,0};
    final Set<TrackBlockEntity> track_connection_updates = new HashSet<>();
    final long[] internal_connected_sides = {0,0,0,0,0,0};
    final long[] external_connected_routes = {0,0,0,0,0,0};
    // Cache and reset current net data
    {
      nets_.forEach((net)->{
        net.internal_sides.forEach(ps->current_side_powers[ps.ordinal()] = net.getPower());
        all_neighbours.addAll(net.neighbour_positions);
      });
      if(trace_) Auxiliaries.logWarn(String.format("UCON: %s SIDPW: [%01x %01x %01x %01x %01x %01x]", posstr(getBlockPos()), current_side_powers[0], current_side_powers[1], current_side_powers[2], current_side_powers[3], current_side_powers[4], current_side_powers[5]));
      nets_.clear();
    }
    // Own internal and external connections.
    {
      long external_connection_flags = getStateFlags() & (RedstoneTrackDefs.STATE_FLAG_WIR_MASK|RedstoneTrackDefs.STATE_FLAG_CON_MASK);
      for(Map.Entry<Long,net.minecraft.util.Tuple<Direction,Direction>> kv: RedstoneTrackDefs.connections.INTERNAL_EDGE_CONNECTION_MAPPING.entrySet()) {
        final long wire_bit_pair = kv.getKey();
        if((getStateFlags() & wire_bit_pair) != wire_bit_pair) continue; // no internal connection.
        external_connection_flags &= ~wire_bit_pair;
        for(int i=0; i<6; ++i) {
          if(((0xfL<<(4*i)) & wire_bit_pair) == 0) continue;
          internal_connected_sides[i] |= wire_bit_pair;
        }
      }
      if(trace_) Auxiliaries.logWarn(String.format("UCON: %s CONFL: ext:%08x | int:[%08x %08x %08x %08x %08x %08x]", posstr(getBlockPos()), external_connection_flags, internal_connected_sides[0], internal_connected_sides[1], internal_connected_sides[2], internal_connected_sides[3], internal_connected_sides[4], internal_connected_sides[5]));
      // Condense internal connections.
      for(int k=0; k<2; ++k) {
        for(int i=0; i<6; ++i) {
          if(internal_connected_sides[i] == 0) continue;
          for(int j=i+1; j<6; ++j) {
            if((internal_connected_sides[i] & internal_connected_sides[j]) == 0) continue;
            internal_connected_sides[i] |= internal_connected_sides[j];
            internal_connected_sides[j] = 0;
          }
        }
      }
      // Track nets
      for(int i=0; i<6; ++i) {
        if(internal_connected_sides[i] != 0) {
          for(int j=i; j<6; ++j) {
            final long mask = (0xfL<<(4*j));
            if((internal_connected_sides[i] & mask) == 0) continue;
            final long bulk = (0x1L<<(RedstoneTrackDefs.STATE_FLAG_CON_POS+j));
            external_connected_routes[i] |= (external_connection_flags & (mask|bulk));
            external_connection_flags &= ~(mask|bulk);
          }
        } else {
          final long mask = (0xfL<<(4*i));
          final long bulk = (0x1L<<(RedstoneTrackDefs.STATE_FLAG_CON_POS+i));
          external_connected_routes[i] |= (external_connection_flags & (mask|bulk));
          external_connection_flags &= ~(mask|bulk);
        }
      }
      if(trace_) {
        Auxiliaries.logWarn(String.format("UCON: %s CONSD: ext:%08x | int:[%08x %08x %08x %08x %08x %08x]", posstr(getBlockPos()), external_connection_flags, internal_connected_sides[0], internal_connected_sides[1], internal_connected_sides[2], internal_connected_sides[3], internal_connected_sides[4], internal_connected_sides[5]));
        Auxiliaries.logWarn(String.format("UCON: %s CONRT: ext:%08x | ext:[%08x %08x %08x %08x %08x %08x]", posstr(getBlockPos()), external_connection_flags, external_connected_routes[0], external_connected_routes[1], external_connected_routes[2], external_connected_routes[3], external_connected_routes[4], external_connected_routes[5]));
      }
    }
    // Net list.
    {
      Set<Direction> used_sides = new HashSet<>();
      for(int i=0; i<6; ++i) {
        if(external_connected_routes[i] == 0) continue;
        final Set<Direction> int_sides = new HashSet<>();         // Internal faces of the net, all have the same power.
        final List<Direction> pwr_sides = new ArrayList<>(6);     // Power reading sides.
        final List<BlockPos> positions = new ArrayList<>(6);      // Block positions of connected blocks.
        final List<Direction> ext_sides = new ArrayList<>(6);     // Sides externally connectable.
        for(int j=0; j<6; ++j) {
          final long mask = (0xfL<<(4*j));
          final long bulk = (0x1L<<(RedstoneTrackDefs.STATE_FLAG_CON_POS+j));
          final Direction side = connections.CONNECTION_BIT_ORDER[j];
          // Internal net route sides
          if((internal_connected_sides[i] & mask) != 0) {
            int_sides.add(side);
          }
          // External wire net routes
          if((external_connected_routes[i] & mask) != 0) {
            for(int k=0; k<4; ++k) {
              final long wire_bit = (0x1L<<(4*j+k));
              if((external_connected_routes[i] & wire_bit) == 0) continue;
              final net.minecraft.util.Tuple<Direction,Direction> side_dir = RedstoneTrackDefs.connections.getWireBitSideAndDirection(wire_bit);
              final Direction tsid = side_dir.getA();
              final Direction tdir = side_dir.getB();
              final BlockPos wire_pos = getBlockPos().relative(tdir);
              final BlockState wire_state = getLevel().getBlockState(wire_pos);
              boolean diagonal_check = false;
              if(wire_state.is(getBlock())) {
                // adjacent track
                long adjacent_mask = RedstoneTrackDefs.connections.getWireBit(tsid, tdir.getOpposite());
                TrackBlockEntity adj_te = RedstoneTrackBlock.tile(getLevel(), wire_pos).orElse(null);
                if((adj_te==null) || (adj_te.getStateFlags() & adjacent_mask) != adjacent_mask) {
                  diagonal_check = true;
                } else {
                  positions.add(wire_pos);
                  ext_sides.add(tsid);
                  int_sides.add(side);
                  pwr_sides.add(tdir);
                  track_connection_updates.add(adj_te);
                  continue;
                }
              }
              // adjacent vanilla wire
              if((!diagonal_check) && wire_state.is(Blocks.REDSTONE_WIRE)) {
                // adjacent vanilla redstone wire, only connected on the bottom face.
                if(side!=Direction.DOWN) {
                  diagonal_check = true;
                } else {
                  positions.add(wire_pos);
                  ext_sides.add(tdir.getOpposite()); // NOT the redstone side, the real face.
                  int_sides.add(side);
                  pwr_sides.add(tdir);
                  continue;
                }
              }
              // power source
              if((!diagonal_check) && wire_state.isSignalSource()) {
                // adjacent power block
                positions.add(wire_pos);
                ext_sides.add(tdir.getOpposite()); // real face.
                int_sides.add(side);
                pwr_sides.add(tdir);
                continue;
              }
              // diagonal track
              {
                final BlockPos track_pos = wire_pos.relative(tsid);
                final BlockState track_state = getLevel().getBlockState(track_pos);
                if(track_state.is(getBlock())) {
                  long adjacent_mask = RedstoneTrackDefs.connections.getWireBit(tdir.getOpposite(), tsid.getOpposite());
                  TrackBlockEntity adj_te = RedstoneTrackBlock.tile(getLevel(), track_pos).orElse(null);
                  if((adj_te==null) || (adj_te.getStateFlags() & adjacent_mask) != adjacent_mask) continue;
                  positions.add(track_pos);
                  ext_sides.add(tdir.getOpposite()); // real face.
                  int_sides.add(side);
                  pwr_sides.add(tdir);
                  track_connection_updates.add(adj_te);
                  continue;
                }
              }
              // air or full block
              if(!isRedstoneInsulator(wire_state, wire_pos)) {
                positions.add(wire_pos);
                ext_sides.add(tdir.getOpposite()); // real face.
                int_sides.add(side);
                pwr_sides.add(tdir);
              }
            }
          }
          // External bulk connector net routes
          if((external_connected_routes[i] & bulk) != 0) {
            final BlockPos bulk_pos = getBlockPos().relative(side);
            final BlockState bulk_state = getLevel().getBlockState(bulk_pos);
            if(isRedstoneInsulator(bulk_state, bulk_pos)) continue;
            positions.add(bulk_pos);
            ext_sides.add(side.getOpposite()); // NOT the redstone side, the real face.
            int_sides.add(side);
            pwr_sides.add(side);
          }
        }
        // Update net
        if(!positions.isEmpty()) {
          TrackNet net = new TrackNet(positions, ext_sides, new ArrayList<>(int_sides), new ArrayList<>(pwr_sides));
          net.setPower(net.internal_sides.stream().mapToInt(side->current_side_powers[side.ordinal()]).max().orElse(0));
          nets_.add(net);
          used_sides.addAll(int_sides);
        }
      }
      Arrays.stream(Direction.values()).filter(side->!used_sides.contains(side)).forEach(side->setSidePower(side, 0));
      setChanged();
    }
    // -- Prepare neighbour updates
    {
      final Set<BlockPos> disconnected_neighbours = new HashSet<>(all_neighbours);
      final Set<BlockPos> connected_neighbours = new HashSet<>();
      nets_.forEach(net->net.neighbour_positions.forEach(disconnected_neighbours::remove));
      nets_.forEach(net->connected_neighbours.addAll(net.neighbour_positions));
      all_neighbours.forEach(connected_neighbours::remove);
      if(trace_) {
        final String poss = posstr(getBlockPos());
        for(TrackNet net:nets_) {
          final List<String> ss = new ArrayList<>();
          for(int i = 0; i<net.neighbour_positions.size(); ++i) ss.add(posstr(net.neighbour_positions.get(i)) + ":" + net.neighbour_sides.get(i).toString());
          String int_sides = net.internal_sides.stream().map(Direction::toString).collect(Collectors.joining(","));
          String pwr_sides = net.power_sides.stream().map(Direction::toString).collect(Collectors.joining(","));
          Auxiliaries.logWarn(String.format("UCON: %s adj:%s | ints:%s | pwrs:%s", poss, String.join(", ", ss), int_sides, pwr_sides));
        }
        if(!disconnected_neighbours.isEmpty()) Auxiliaries.logWarn(String.format("UCON: %s DISCONNECTED NEIGHBOURS: %s", posstr(getBlockPos()), disconnected_neighbours.stream().map(TrackBlockEntity::posstr).collect(Collectors.joining(","))));
        if(!connected_neighbours.isEmpty()) Auxiliaries.logWarn(String.format("UCON: %s CONNECTED NEIGHBOURS: %s", posstr(getBlockPos()), connected_neighbours.stream().map(TrackBlockEntity::posstr).collect(Collectors.joining(","))));
      }
      (new HashSet<>(disconnected_neighbours)).forEach(p->RedstoneTrackBlock.tile(getLevel(), p).ifPresent(te->{ track_connection_updates.add(te); disconnected_neighbours.remove(p); }));
      if(trace_ && (!disconnected_neighbours.isEmpty())) Auxiliaries.logWarn(String.format("UCON: %s DISCONNECTED NONTRACK: %s", posstr(getBlockPos()), disconnected_neighbours.stream().map(TrackBlockEntity::posstr).collect(Collectors.joining(","))));
    }
    // Update neighbour tracks
    {
      if(recursion_left > 0) {
        for(TrackBlockEntity te:track_connection_updates) {
          if(trace_) Auxiliaries.logWarn(String.format("UCON: %s UPDATE NET OF %s", posstr(getBlockPos()), posstr(te.getBlockPos())));
          te.updateConnections(recursion_left-1);
        }
      }
    }
    // Update removed/added non-track connections
    {
      nets_.stream().filter((net)->net.getPower() > 0).forEach((net)->all_neighbours.addAll(net.neighbour_positions));
      final Level world = getLevel();
      final BlockState state = getBlockState();
      all_neighbours.forEach((pos)->{
        final BlockState st = world.getBlockState(pos);
        if(trace_) Auxiliaries.logWarn(String.format("UCON: %s UPDATE TRACK CHANGES TO %s.", posstr(getBlockPos()), posstr(pos)));
        st.handleNeighborChanged(world, pos, state.getBlock(), getBlockPos(), false);
        world.updateNeighborsAt(pos, st.getBlock());
      });
    }
  }
}
