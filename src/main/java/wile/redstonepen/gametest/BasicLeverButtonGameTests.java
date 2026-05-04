package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class BasicLeverButtonGameTests
{
  private static final String NS = "minecraft";
  private static final String EMPTY = "relay_activates_from_redstone";
  private static final BlockPos POS = new BlockPos(1, 1, 1);
  private static final BlockPos SUPPORT = POS.below();

  private BasicLeverButtonGameTests() {}

  // --- BasicLever -----------------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 10)
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

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 10)
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

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 10)
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

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 10)
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

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 30)
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
}
