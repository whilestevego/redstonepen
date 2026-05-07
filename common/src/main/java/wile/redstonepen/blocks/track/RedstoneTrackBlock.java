/*
 * @file RedstoneTrackBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen.blocks.track;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import wile.redstonepen.items.RedstonePenItem;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.StandardBlocks;

import org.jetbrains.annotations.Nullable;
import java.util.*;

@SuppressWarnings("deprecation")
public class RedstoneTrackBlock extends StandardBlocks.WaterLoggable implements EntityBlock
{
  public RedstoneTrackBlock(long config, BlockBehaviour.Properties builder)
  { super(config, builder.pushReaction(PushReaction.DESTROY)); }

  public static Optional<TrackBlockEntity> tile(BlockGetter world, BlockPos pos)
  { final BlockEntity te=world.getBlockEntity(pos); return (((te instanceof TrackBlockEntity) && (!te.isRemoved())) ? Optional.of((TrackBlockEntity)te) : Optional.empty()); }

  public static boolean canBePlacedOnFace(BlockState state, Level world, BlockPos pos, Direction face)
  {
    if(state.getBlock() instanceof PistonBaseBlock) {
      Direction pface = state.getValue(PistonBaseBlock.FACING);
      return (face != pface);
    }
    if(state.getBlock() instanceof MovingPistonBlock) return true;
    if(state.is(Blocks.HOPPER)) return (face == Direction.UP);
    return state.isFaceSturdy(world, pos, face);
  }

  //------------------------------------------------------------------------------------------------------------------

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
  { return new TrackBlockEntity(pos, state); }

  @Override
  public boolean hasDynamicDropList()
  { return true; }

  @Override
  public List<ItemStack> dropList(BlockState state, Level world, @Nullable BlockEntity te, boolean explosion)
  {
    if(!(te instanceof TrackBlockEntity)) return Collections.emptyList();
    int num_connections = ((TrackBlockEntity)te).getRedstoneDustCount();
    if(num_connections <= 0) return Collections.emptyList();
    return Collections.singletonList(new ItemStack(Items.REDSTONE, num_connections));
  }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  { return context.getLevel().getBlockState(context.getClickedPos()).canBeReplaced(context) ? super.getStateForPlacement(context) : null; }

  @Override
  public Item asItem()
  { return Items.REDSTONE; }

  @Override
  public boolean isPathfindable(BlockState state, PathComputationType type)
  { return true; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
  {
    final int wires = tile(world, pos).map(TrackBlockEntity::getWireFlags).orElse(0);
    final int faces = (((wires & 0x00000f) != 0) ? 0x01 : 0)
      | (((wires & 0x0000f0) != 0) ? 0x02 : 0)
      | (((wires & 0x000f00) != 0) ? 0x04 : 0)
      | (((wires & 0x00f000) != 0) ? 0x08 : 0)
      | (((wires & 0x0f0000) != 0) ? 0x10 : 0)
      | (((wires & 0xf00000) != 0) ? 0x20 : 0);
    return RedstoneTrackDefs.shape.get(faces);
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
  { return Shapes.empty(); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
  { return !state.getValue(WATERLOGGED); }

  @Override
  public boolean useShapeForLightOcclusion(BlockState state)
  { return true; }

  @Override
  public RenderShape getRenderShape(BlockState state)
  { return RenderShape.ENTITYBLOCK_ANIMATED; }

  @Override
  public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos)
  { return true; }

  @Deprecated
  public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side)
  { return (side != null) && (tile(world,pos).map(te->te.hasVanillaRedstoneConnection(side.getOpposite()))).orElse(false); }

  @Override
  public boolean isSignalSource(BlockState state)
  { return can_provide_power_; }

  @Override
  public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction redstone_side)
  { return can_provide_power_ ? tile(world, pos).map(te->te.getRedstonePower(redstone_side, true)).orElse(0) : 0; }

  @Override
  public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction redstone_side)
  { return can_provide_power_ ? tile(world, pos).map(te->te.getRedstonePower(redstone_side, false)).orElse(0) : 0; }

  @Override
  public boolean shouldCheckWeakPower(BlockState state, LevelReader level, BlockPos pos, Direction side)
  { return false; }

  @Override
  public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rnd)
  { if(!tile(world,pos).map(te->te.sync(false)).orElse(false)) world.removeBlock(pos, false); }

  @Override
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
  {
    if(!world.isClientSide()) {
      if(tile(world, pos).map(te->te.handleShapeUpdate(facing, facingState, facingPos, false)).orElse(true)) {
        world.scheduleTick(pos, this, 1);
      } else {
        world.removeBlock(pos, false);
      }
    }
    return super.updateShape(state, facing, facingState, world, pos, facingPos);
  }

  @Override
  public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving)
  {
    if(isMoving || state.is(newState.getBlock())) return;
    super.onRemove(state, world, pos, newState, isMoving);
    if(world.isClientSide()) return;
    notifyAdjacent(world, pos);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult rtr)
  {
    // Allows removing a Redstone dust segment.
    return modifySegments(state, world, pos, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND, rtr, true, false);
  }

  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rtr)
  {
    if(stack.is(Items.DEBUG_STICK)) {
      if(world.isClientSide) return ItemInteractionResult.SUCCESS;
      if(world.getBlockEntity(pos) instanceof TrackBlockEntity te) te.toggle_trace(player);
      return ItemInteractionResult.CONSUME;
    } else {
      // Place segment using Quill/Pen or Redstone dust.
      return switch(modifySegments(state, world, pos, player, stack, hand, rtr, false, RedstonePenItem.isPen(stack))) {
        case SUCCESS -> ItemInteractionResult.SUCCESS;
        case CONSUME -> ItemInteractionResult.CONSUME;
        case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        case FAIL -> ItemInteractionResult.FAIL;
        case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
        case SUCCESS_NO_ITEM_USED -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      };
    }
  }

  @Override
  public void neighborChanged(BlockState state, Level world, BlockPos pos, Block fromBlock, BlockPos fromPos, boolean isMoving)
  {
    if(world.isClientSide()) return;
    try {
      final Map<BlockPos,BlockPos> blocks_to_update = tile(world, pos).map(te->te.handleNeighborChanged(fromPos)).orElse(Collections.emptyMap());
      if(blocks_to_update.isEmpty()) return;
      for(Map.Entry<BlockPos,BlockPos> update_pos:blocks_to_update.entrySet()) {
        if(update_pos.getKey().equals(update_pos.getValue())) continue;
        world.neighborChanged(update_pos.getKey(), this, update_pos.getValue());
      }
    } catch(Throwable ex) {
      Auxiliaries.logError("Track neighborChanged recursion detected, dropping!");
      final int num_redstone = tile(world, pos).map(TrackBlockEntity::getRedstoneDustCount).orElse(0);
      if(num_redstone > 0) {
        Vec3 p = Vec3.atCenterOf(pos);
        world.addFreshEntity(new ItemEntity(world, p.x, p.y, p.z, new ItemStack(Items.REDSTONE, num_redstone)));
        world.setBlock(pos, world.getBlockState(pos).getFluidState().createLegacyBlock(), 2|16);
      }
    }
  }

  @Environment(EnvType.CLIENT)
  private void spawnPoweredParticle(Level world, RandomSource rand, BlockPos pos, Vec3 color, Direction from, Direction to, float minChance, float maxChance) {
    float f = maxChance - minChance;
    if(rand.nextFloat() < 0.3f * f) {
      double c1 = 0.4375;
      double c2 = minChance + f * rand.nextFloat();
      double p0 = 0.5 + (c1 * from.getStepX()) + (c2*.4 * to.getStepX());
      double p1 = 0.5 + (c1 * from.getStepY()) + (c2*.4 * to.getStepY());
      double p2 = 0.5 + (c1 * from.getStepZ()) + (c2*.4 * to.getStepZ());
      world.addParticle(new DustParticleOptions(new Vector3f(color.toVector3f()),1.0F), pos.getX()+p0, pos.getY()+p1, pos.getZ()+p2, 0, 0., 0);
    }
  }

  @Environment(EnvType.CLIENT)
  @Override
  public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand)
  {
    if(rand.nextFloat() > 0.4) return;
    final TrackBlockEntity te = tile(world,pos).orElse(null);
    if((te == null) || ((te.getStateFlags() & RedstoneTrackDefs.STATE_FLAG_PWR_MASK) == 0)) return;
    final Vec3 color = new Vec3(0.6f,0,0);
    for(Direction side: Direction.values()) {
      int p = te.getSidePower(side);
      if(p == 0) continue;
      spawnPoweredParticle(world, rand, pos, color, side, side.getOpposite(), -0.5F, 0.5F);
    }
  }

  //------------------------------------------------------------------------------------------------------------------

  public InteractionResult modifySegments(BlockState state, Level world, BlockPos pos, Player player, ItemStack stack, InteractionHand hand, BlockHitResult rtr, boolean no_add, boolean no_remove)
  {
    if((!stack.isEmpty()) && (stack.getItem()!=Items.REDSTONE) && (!RedstonePenItem.isPen(stack))) {
      BlockPos behind_pos = pos.relative(rtr.getDirection());
      BlockState behind_state = world.getBlockState(behind_pos);
      if(behind_state.isRedstoneConductor(world, behind_pos)) {
        return behind_state.useWithoutItem(world, player, rtr);
      }
      return InteractionResult.sidedSuccess(world.isClientSide());
    }
    if(world.isClientSide()) return InteractionResult.SUCCESS;
    if(!RedstonePenItem.hasEnoughRedstone(stack, 1, player)) no_add = !no_remove;
    TrackBlockEntity te = tile(world, pos).orElse(null);
    if(te==null) return InteractionResult.FAIL;
    final boolean no_bulk = false; //!player.isCrouching(); // Sneak-click to enable adding bulk connectors.
    int redstone_use = te.modifySegments(pos, player, player.getItemInHand(hand), rtr.getDirection(), rtr.getLocation(), no_add, no_remove, no_bulk);
    if(redstone_use == 0) {
      return InteractionResult.CONSUME;
    } else if(redstone_use < 0) {
      RedstonePenItem.pushRedstone(stack, -redstone_use, player);
      if(te.getWireFlags() == 0) {
        world.setBlock(pos, state.getFluidState().createLegacyBlock(), 1|2);
      } else {
        final Map<BlockPos,BlockPos> blocks_to_update = te.updateAllPowerValuesFromAdjacent();
        for(Map.Entry<BlockPos,BlockPos> update_pos:blocks_to_update.entrySet()) {
          world.neighborChanged(update_pos.getKey(), this, update_pos.getValue());
        }
      }
      world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.4f, 2f);
    } else {
      RedstonePenItem.popRedstone(stack, redstone_use, player, hand);
      world.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.4f, 2.4f);
    }
    updateNeighbourShapes(state, world, pos);
    notifyAdjacent(world, pos);
    return InteractionResult.CONSUME;
  }

  //------------------------------------------------------------------------------------------------------------------

  private boolean can_provide_power_ = true;

  void disablePower(boolean disable)
  { can_provide_power_ = !disable; }

  private void updateNeighbourShapes(final BlockState state, final Level world, final BlockPos pos)
  { state.updateNeighbourShapes(world, pos, 1|2); }

  public void notifyAdjacent(final Level world, final BlockPos pos)
  {
    world.updateNeighborsAt(pos, this);
    for(Direction dir0: BlockBehaviour.UPDATE_SHAPE_ORDER) {
      BlockPos ppos = pos.relative(dir0);
      world.updateNeighborsAtExceptFromFacing(ppos, world.getBlockState(ppos).getBlock(), dir0.getOpposite());
      for(Direction dir1: BlockBehaviour.UPDATE_SHAPE_ORDER) {
        if(dir0 == dir1.getOpposite()) return;
        ppos = pos.relative(dir0).relative(dir1);
        if(ppos == pos) continue;
        final BlockState diagonal_state = world.getBlockState(ppos);
        if(diagonal_state.getBlock() != this) continue;
        world.neighborChanged(ppos, this, pos);
      }
    }
  }

}
