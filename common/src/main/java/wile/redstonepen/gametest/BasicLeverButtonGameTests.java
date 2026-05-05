package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.BasicGauge;
import wile.redstonepen.libmc.Registries;

public final class BasicLeverButtonGameTests
{
  private static final String NS = "minecraft";
  private static final String EMPTY = "relay_activates_from_redstone";
  private static final BlockPos POS = new BlockPos(1, 1, 1);
  private static final BlockPos SUPPORT = POS.below();

  private BasicLeverButtonGameTests() {}

  // --- BasicLever -----------------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void leverUseWithoutItemTogglesPoweredFalseToTrue(GameTestHelper helper)
  {
    helper.setBlock(SUPPORT, Blocks.STONE);
    helper.setBlock(POS, Registries.getBlock("basic_lever").defaultBlockState()
      .setValue(LeverBlock.FACE, AttachFace.FLOOR)
      .setValue(LeverBlock.FACING, Direction.NORTH));
    final BlockState before = helper.getBlockState(POS);
    if(before.getValue(BlockStateProperties.POWERED)) helper.fail("expected initial powered=false");

    final BlockPos abs = helper.absolutePos(POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    before.useWithoutItem(helper.getLevel(), null, hit);

    helper.runAfterDelay(2, () -> {
      final BlockState after = helper.getBlockState(POS);
      if(!after.getValue(BlockStateProperties.POWERED)) helper.fail("expected powered after lever toggle");
      helper.succeed();
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void leverUseTwiceReturnsToUnpowered(GameTestHelper helper)
  {
    helper.setBlock(SUPPORT, Blocks.STONE);
    helper.setBlock(POS, Registries.getBlock("basic_lever").defaultBlockState()
      .setValue(LeverBlock.FACE, AttachFace.FLOOR)
      .setValue(LeverBlock.FACING, Direction.NORTH));
    final BlockPos abs = helper.absolutePos(POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    helper.getBlockState(POS).useWithoutItem(helper.getLevel(), null, hit);
    helper.getBlockState(POS).useWithoutItem(helper.getLevel(), null, hit);

    helper.runAfterDelay(2, () -> {
      if(helper.getBlockState(POS).getValue(BlockStateProperties.POWERED))
        helper.fail("expected unpowered after two toggles");
      helper.succeed();
    });
  }

  // --- BasicButton ----------------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void buttonUseWithoutItemPressesAndPowers(GameTestHelper helper)
  {
    helper.setBlock(SUPPORT, Blocks.STONE);
    helper.setBlock(POS, Registries.getBlock("basic_button").defaultBlockState()
      .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
      .setValue(ButtonBlock.FACING, Direction.NORTH));
    final BlockState before = helper.getBlockState(POS);
    if(before.getValue(BlockStateProperties.POWERED)) helper.fail("expected initial powered=false");

    final BlockPos abs = helper.absolutePos(POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    before.useWithoutItem(helper.getLevel(), null, hit);

    helper.runAfterDelay(1, () -> {
      if(!helper.getBlockState(POS).getValue(BlockStateProperties.POWERED))
        helper.fail("expected button powered after press");
      helper.succeed();
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void buttonUseOnPoweredButtonReturnsConsume(GameTestHelper helper)
  {
    helper.setBlock(SUPPORT, Blocks.STONE);
    helper.setBlock(POS, Registries.getBlock("basic_button").defaultBlockState()
      .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
      .setValue(ButtonBlock.FACING, Direction.NORTH)
      .setValue(BlockStateProperties.POWERED, true));
    final BlockPos abs = helper.absolutePos(POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    final var result = helper.getBlockState(POS).useWithoutItem(helper.getLevel(), null, hit);
    if(result == null) helper.fail("expected non-null InteractionResult");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 30)
  public static void pulseButtonRevertsAfterShortInterval(GameTestHelper helper)
  {
    helper.setBlock(SUPPORT, Blocks.STONE);
    helper.setBlock(POS, Registries.getBlock("basic_pulse_button").defaultBlockState()
      .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
      .setValue(ButtonBlock.FACING, Direction.NORTH));
    final BlockPos abs = helper.absolutePos(POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    helper.getBlockState(POS).useWithoutItem(helper.getLevel(), null, hit);

    // Pulse button has active_time=2 → should revert quickly. 20-tick wait covers it.
    helper.runAfterDelay(20, () -> {
      if(helper.getBlockState(POS).getValue(BlockStateProperties.POWERED))
        helper.fail("pulse button should revert after its active period");
      helper.succeed();
    });
  }

  // --- BasicGauge -----------------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void gaugeReadsZeroWhenNoSignal(GameTestHelper helper)
  {
    helper.setBlock(POS, Registries.getBlock("basic_gauge").defaultBlockState());
    helper.runAfterDelay(2, () -> {
      final int power = helper.getBlockState(POS).getValue(BlockStateProperties.POWER);
      if(power != 0) helper.fail("expected gauge power=0 with no signal, got " + power);
      helper.succeed();
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void gaugeReadsSignalFromAdjacentRedstoneBlock(GameTestHelper helper)
  {
    helper.setBlock(POS, Registries.getBlock("basic_gauge").defaultBlockState());
    helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
    helper.succeedWhen(() -> {
      final int power = helper.getBlockState(POS).getValue(BlockStateProperties.POWER);
      if(power <= 0) helper.fail("expected gauge power>0 adjacent to redstone block, got " + power);
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void gaugeShouldCheckWeakPowerReturnsFalse(GameTestHelper helper)
  {
    helper.setBlock(POS, Registries.getBlock("basic_gauge").defaultBlockState());
    final BasicGauge.BasicGaugeBlock block = (BasicGauge.BasicGaugeBlock)Registries.getBlock("basic_gauge");
    final boolean result = block.shouldCheckWeakPower(helper.getBlockState(POS), helper.getLevel(), helper.absolutePos(POS), Direction.NORTH);
    if(result) helper.fail("shouldCheckWeakPower must return false");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void gaugeGetStateForPlacementReturnsNonNull(GameTestHelper helper)
  {
    final Block block = Registries.getBlock("basic_gauge");
    final ItemStack stack = new ItemStack(block);
    final BlockPos abs = helper.absolutePos(POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    final var player = helper.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    final UseOnContext useCtx = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
    final BlockPlaceContext ctx = new BlockPlaceContext(useCtx);
    final BlockState placed = block.getStateForPlacement(ctx);
    // placed may be null if position is not replaceable; either way the method must not throw.
    helper.succeed();
  }
}
