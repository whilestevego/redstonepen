package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.libmc.Registries;

import java.util.HashSet;
import java.util.Set;

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

  // --- getFrontFacing: FACING=DOWN all 4 rotations ---

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation0FrontIsNorth(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 0);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.NORTH)
      helper.fail("FACING=DOWN ROTATION=0: expected getFrontFacing=NORTH");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation1FrontIsEast(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 1);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.EAST)
      helper.fail("FACING=DOWN ROTATION=1: expected getFrontFacing=EAST");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation2FrontIsSouth(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 2);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.SOUTH)
      helper.fail("FACING=DOWN ROTATION=2: expected getFrontFacing=SOUTH");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation3FrontIsWest(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 3);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.WEST)
      helper.fail("FACING=DOWN ROTATION=3: expected getFrontFacing=WEST");
    helper.succeed();
  }

  // --- getUpFacing / getDownFacing for all FACING values ---

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void upFacingIsOppositeOfFacingForAllDirections(GameTestHelper helper)
  {
    Block relay = Registries.getBlock("relay");
    for(Direction face : Direction.values()) {
      BlockState s = relay.defaultBlockState()
        .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0);
      Direction expected = face.getOpposite();
      Direction actual = CircuitComponents.DirectedComponentBlock.getUpFacing(s);
      if(actual != expected)
        helper.fail("getUpFacing for FACING=" + face + ": expected " + expected + " got " + actual);
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingMethodEqualsFacingPropertyForAllDirections(GameTestHelper helper)
  {
    Block relay = Registries.getBlock("relay");
    for(Direction face : Direction.values()) {
      BlockState s = relay.defaultBlockState()
        .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0);
      Direction actual = CircuitComponents.DirectedComponentBlock.getDownFacing(s);
      if(actual != face)
        helper.fail("getDownFacing for FACING=" + face + ": expected " + face + " got " + actual);
    }
    helper.succeed();
  }

  // --- Rotation-relative methods ---

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void rightFacingIsRotationPlusOneRelativeToFront(GameTestHelper helper)
  {
    Block relay = Registries.getBlock("relay");
    for(Direction face : Direction.values()) {
      for(int r = 0; r < 4; ++r) {
        BlockState s = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r);
        BlockState sNext = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, (r + 1) & 3);
        Direction expected = CircuitComponents.DirectedComponentBlock.getFrontFacing(sNext);
        Direction actual = CircuitComponents.DirectedComponentBlock.getRightFacing(s);
        if(actual != expected)
          helper.fail("getRightFacing FACING=" + face + " ROTATION=" + r + ": expected " + expected + " got " + actual);
      }
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void backFacingIsRotationPlusTwoRelativeToFront(GameTestHelper helper)
  {
    Block relay = Registries.getBlock("relay");
    for(Direction face : Direction.values()) {
      for(int r = 0; r < 4; ++r) {
        BlockState s = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r);
        BlockState sTwo = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, (r + 2) & 3);
        Direction expected = CircuitComponents.DirectedComponentBlock.getFrontFacing(sTwo);
        Direction actual = CircuitComponents.DirectedComponentBlock.getBackFacing(s);
        if(actual != expected)
          helper.fail("getBackFacing FACING=" + face + " ROTATION=" + r + ": expected " + expected + " got " + actual);
      }
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void leftFacingIsRotationPlusThreeRelativeToFront(GameTestHelper helper)
  {
    Block relay = Registries.getBlock("relay");
    for(Direction face : Direction.values()) {
      for(int r = 0; r < 4; ++r) {
        BlockState s = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r);
        BlockState sThree = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, (r + 3) & 3);
        Direction expected = CircuitComponents.DirectedComponentBlock.getFrontFacing(sThree);
        Direction actual = CircuitComponents.DirectedComponentBlock.getLeftFacing(s);
        if(actual != expected)
          helper.fail("getLeftFacing FACING=" + face + " ROTATION=" + r + ": expected " + expected + " got " + actual);
      }
    }
    helper.succeed();
  }

  // --- All 6 mapped directions are distinct for every state ---

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void allSixMappedDirectionsAreDistinctForEveryState(GameTestHelper helper)
  {
    Block relay = Registries.getBlock("relay");
    for(Direction face : Direction.values()) {
      for(int r = 0; r < 4; ++r) {
        BlockState s = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r);
        Set<Direction> dirs = new HashSet<>();
        dirs.add(CircuitComponents.DirectedComponentBlock.getFrontFacing(s));
        dirs.add(CircuitComponents.DirectedComponentBlock.getBackFacing(s));
        dirs.add(CircuitComponents.DirectedComponentBlock.getLeftFacing(s));
        dirs.add(CircuitComponents.DirectedComponentBlock.getRightFacing(s));
        dirs.add(CircuitComponents.DirectedComponentBlock.getUpFacing(s));
        dirs.add(CircuitComponents.DirectedComponentBlock.getDownFacing(s));
        if(dirs.size() != 6)
          helper.fail("FACING=" + face + " ROTATION=" + r + ": expected 6 distinct facing directions, got " + dirs.size());
      }
    }
    helper.succeed();
  }

  // --- getForwardStateMappedFacing / getReverseStateMappedFacing are inverses ---

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void forwardThenReverseStateMappedFacingIsIdentity(GameTestHelper helper)
  {
    Block relay = Registries.getBlock("relay");
    for(Direction face : Direction.values()) {
      for(int r = 0; r < 4; ++r) {
        BlockState s = relay.defaultBlockState()
          .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
          .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r);
        for(Direction worldSide : Direction.values()) {
          Direction mapped = CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(s, worldSide);
          Direction back = CircuitComponents.DirectedComponentBlock.getReverseStateMappedFacing(s, mapped);
          if(back != worldSide)
            helper.fail("FACING=" + face + " ROTATION=" + r + " side=" + worldSide + ": reverse(forward(side)) != side, got " + back);
        }
      }
    }
    helper.succeed();
  }

  private static BlockState relayState(GameTestHelper helper, Direction facing, int rotation)
  {
    return Registries.getBlock("relay").defaultBlockState()
      .setValue(CircuitComponents.DirectedComponentBlock.FACING, facing)
      .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, rotation);
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
