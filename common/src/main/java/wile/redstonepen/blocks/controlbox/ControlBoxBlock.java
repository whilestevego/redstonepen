package wile.redstonepen.blocks.controlbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.util.Auxiliaries;
import wile.redstonepen.blocks.StandardEntityBlocks;

import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@SuppressWarnings("deprecation")
public class ControlBoxBlock extends CircuitComponents.DirectedComponentBlock implements StandardEntityBlocks.IStandardEntityBlock<ControlBoxBlockEntity>
{
  public ControlBoxBlock(long config, BlockBehaviour.Properties builder, AABB[] aabb)
  { super(config, builder, aabb); }

  @Override
  public List<ItemStack> dropList(BlockState state, Level world, @Nullable BlockEntity te, boolean explosion)
  {
    final ItemStack stack = new ItemStack(this.asItem());
    if(te instanceof ControlBoxBlockEntity cb) {
      final CompoundTag tedata = cb.writenbt(world.registryAccess(), new CompoundTag());
      if(tedata.contains("logic") && !tedata.getCompound("logic").getString("code").trim().isEmpty()) {
        Auxiliaries.setItemStackNbt(stack, "tedata", tedata);
        Auxiliaries.setItemLabel(stack, cb.getCustomName());
      }
    }
    return Collections.singletonList(stack);
  }

  @Override
  public boolean isBlockEntityTicking(Level world, BlockState state)
  { return true; }

  @Override
  @Environment(EnvType.CLIENT)
  public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag)
  {
    Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true);
    if(!Auxiliaries.Tooltip.extendedTipCondition()) return;
    final CompoundTag nbt = Auxiliaries.getItemStackNbt(stack, "tedata");
    final CompoundTag nbt_logic = nbt.getCompound("tedata").getCompound("logic");
    if(nbt_logic.isEmpty()) return;
    Arrays.stream(nbt_logic.getString("code").split("\\n"))
      .map(s->s.replaceAll("#.*$", "").trim())
      .filter(s->!s.isEmpty())
      .map(s->(Component.literal(s).withStyle(ChatFormatting.DARK_GREEN)))
      .forEach(tooltip::add);
  }

  @Override
  public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
  {
    if(world.isClientSide) return;
    final CompoundTag nbt = Auxiliaries.getItemStackNbt(stack, "tedata");
    if(nbt.isEmpty()) return;
    final BlockEntity te = world.getBlockEntity(pos);
    if(!(te instanceof ControlBoxBlockEntity cbe)) return;
    cbe.readnbt(world.registryAccess(), nbt);
    cbe.setCustomName(Auxiliaries.getItemLabel(stack));
    te.setChanged();
  }

  @Override
  public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction redstone_side)
  {
    if(!(world.getBlockEntity(pos) instanceof ControlBoxBlockEntity cb)) return 0;
    final Direction internal_side = getReverseStateMappedFacing(state, redstone_side.getOpposite());
    return cb.getOutputSignal(internal_side);
  }

  @Override
  public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction redstone_side)
  { return getSignal(state, world, pos, redstone_side); }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult rtr)
  {
    return useOpenGui(state, world, pos, player);
  }

  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rtr)
  {
    if(stack.is(Items.DEBUG_STICK)) {
      if(world.isClientSide) return ItemInteractionResult.SUCCESS;
      if(world.getBlockEntity(pos) instanceof ControlBoxBlockEntity te) te.toggle_trace(player);
      return ItemInteractionResult.CONSUME;
    } else {
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
  }

  void notifyOutput(BlockState state, Level world, BlockPos pos, Direction dir)
  { notifyOutputNeighbourOfStateChange(state, world, pos, dir); }

  @Override
  public BlockState update(BlockState state, Level world, BlockPos pos, @Nullable BlockPos fromPos)
  {
    if(world.isClientSide) return state;
    if(!(world.getBlockEntity(pos) instanceof final ControlBoxBlockEntity cb)) return state;
    if(fromPos==null) { cb.scheduleImmediateTick(); return state; }
    final BlockPos dp = fromPos.subtract(pos);
    final Direction world_side = Direction.fromDelta(dp.getX(), dp.getY(), dp.getZ());
    if(world_side!=null) cb.signal_update(world_side, getReverseStateMappedFacing(state, world_side));
    return state;
  }
}
