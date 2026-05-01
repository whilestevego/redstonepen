package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class RelayGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_RELAY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos RELAY_POS = new BlockPos(1, 1, 1);
  private static final BlockPos INPUT_POS = RELAY_POS.east();

  private RelayGameTests()
  {}

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayActivatesFromRedstone(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("relay"), true, 0,
      "expected relay to become powered from adjacent redstone input"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void invertedRelayRegistersPoweredInput(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("inverted_relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("inverted_relay"), true, 0,
      "expected inverted relay to register its powered input"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
  public static void pulseRelayClearsItsPulseAfterTick(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("pulse_relay"));

    helper.runAtTickTime(1, () -> assertRelayState(helper, Registries.getBlock("pulse_relay"), true, 1,
      "expected pulse relay to enter its pulsing state"));
    helper.runAtTickTime(6, () -> assertRelayState(helper, Registries.getBlock("pulse_relay"), true, 0,
      "expected pulse relay to clear its pulse after the scheduled tick"));
    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("pulse_relay"), true, 0,
      "expected pulse relay to settle back to an unlatched state"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void bistableRelayLatchesOnFirstRisingEdge(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("bistable_relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("bistable_relay"), true, 1,
      "expected bistable relay to latch on after the first rising edge"));
  }

  // --- Facing / rotation ---

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation0ActivatesFromEast(GameTestHelper helper)
  {
    // FACING=DOWN ROTATION=0: getFrontFacing=NORTH (output). EAST is an accepted input side.
    Block relayBlock = Registries.getBlock("relay");
    helper.setBlock(RELAY_POS.east(), Blocks.REDSTONE_BLOCK);
    helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
      .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
      .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));

    helper.succeedWhen(() -> assertRelayState(helper, relayBlock, true, 0,
      "relay with FACING=DOWN ROTATION=0 must activate when input is from east (non-output side)"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation0DoesNotActivateFromNorth(GameTestHelper helper)
  {
    // FACING=DOWN ROTATION=0: getFrontFacing=NORTH (output). Input from NORTH must be ignored.
    Block relayBlock = Registries.getBlock("relay");
    helper.setBlock(RELAY_POS.north(), Blocks.REDSTONE_BLOCK);
    helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
      .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
      .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));

    helper.succeedWhen(() -> assertRelayState(helper, relayBlock, false, 0,
      "relay with FACING=DOWN ROTATION=0 must not activate when input is from north (output side)"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation1ShiftsOutputToEast(GameTestHelper helper)
  {
    // FACING=DOWN ROTATION=1: getFrontFacing=EAST (output). Input from EAST must now be ignored.
    Block relayBlock = Registries.getBlock("relay");
    helper.setBlock(RELAY_POS.east(), Blocks.REDSTONE_BLOCK);
    helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
      .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
      .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 1));

    helper.succeedWhen(() -> assertRelayState(helper, relayBlock, false, 0,
      "relay with FACING=DOWN ROTATION=1 must not activate when input is from east (now the output side)"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation1ActivatesFromNorth(GameTestHelper helper)
  {
    // FACING=DOWN ROTATION=1: getFrontFacing=EAST (output). NORTH is an accepted input side.
    Block relayBlock = Registries.getBlock("relay");
    helper.setBlock(RELAY_POS.north(), Blocks.REDSTONE_BLOCK);
    helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
      .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
      .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 1));

    helper.succeedWhen(() -> assertRelayState(helper, relayBlock, true, 0,
      "relay with FACING=DOWN ROTATION=1 must activate when input is from north (non-output side)"));
  }

  private static void placePoweredInput(GameTestHelper helper, Block relayBlock)
  {
    helper.setBlock(INPUT_POS, Blocks.REDSTONE_BLOCK);
    helper.setBlock(RELAY_POS, relayBlock.defaultBlockState());
  }

  private static void assertRelayState(GameTestHelper helper, Block relayBlock, boolean powered, int state, String message)
  {
    helper.assertBlockState(
      RELAY_POS,
      blockState -> blockState.is(relayBlock)
        && blockState.getValue(CircuitComponents.DirectedComponentBlock.POWERED) == powered
        && blockState.getValue(CircuitComponents.DirectedComponentBlock.STATE) == state,
      () -> message
    );
  }
}
