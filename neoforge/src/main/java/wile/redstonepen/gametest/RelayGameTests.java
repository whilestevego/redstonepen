package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.libmc.Registries;

import java.util.HashSet;
import java.util.Set;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class RelayGameTests
{
  private static final String EMPTY_RELAY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos RELAY_POS = new BlockPos(1, 1, 1);
  private static final BlockPos INPUT_POS = RELAY_POS.east();

  public RelayGameTests()
  {}

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayActivatesFromRedstone(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("relay"), true, 0,
      "expected relay to become powered from adjacent redstone input"));
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void invertedRelayRegistersPoweredInput(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("inverted_relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("inverted_relay"), true, 0,
      "expected inverted relay to register its powered input"));
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void pulseRelaySetsPoweredFalseWhenInputRemoved(GameTestHelper helper)
  {
    // Provide permanent north mount so relay survives when power is removed.
    helper.setBlock(RELAY_POS.north(), Blocks.STONE);
    placePoweredInput(helper, Registries.getBlock("pulse_relay"));
    // At t=8, relay is POWERED=true (input present). Remove power to trigger the else-branch in
    // PulseRelayBlock.update (powered=false, pwstate=true → world.setBlock POWERED=false).
    helper.runAtTickTime(8, () -> helper.setBlock(INPUT_POS, Blocks.AIR));
    helper.runAtTickTime(10, () -> {
      assertRelayState(helper, Registries.getBlock("pulse_relay"), false, 0,
        "pulse relay must become POWERED=false when power source removed");
      helper.succeed();
    });
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void bistableRelayLatchesOnFirstRisingEdge(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("bistable_relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("bistable_relay"), true, 1,
      "expected bistable relay to latch on after the first rising edge"));
  }

  // --- Facing / rotation ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation0FrontIsNorth(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 0);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.NORTH)
      helper.fail("FACING=DOWN ROTATION=0: expected getFrontFacing=NORTH");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation1FrontIsEast(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 1);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.EAST)
      helper.fail("FACING=DOWN ROTATION=1: expected getFrontFacing=EAST");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation2FrontIsSouth(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 2);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.SOUTH)
      helper.fail("FACING=DOWN ROTATION=2: expected getFrontFacing=SOUTH");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation3FrontIsWest(GameTestHelper helper)
  {
    BlockState s = relayState(helper, Direction.DOWN, 3);
    if(CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.WEST)
      helper.fail("FACING=DOWN ROTATION=3: expected getFrontFacing=WEST");
    helper.succeed();
  }

  // --- getUpFacing / getDownFacing for all FACING values ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  // --- getStateForPlacement: cover all face-branch paths ---
  // Constructs a BlockPlaceContext by simulating a click on the given face of surfacePos,
  // with the click location offset by clickOffset relative to the center of the placement position.
  private static BlockState relayPlacementState(GameTestHelper helper, BlockPos surfacePos,
                                                 Direction clickedFace, Vec3 clickOffset)
  {
    final BlockPos surfaceAbs = helper.absolutePos(surfacePos);
    final BlockPos placementAbs = surfaceAbs.relative(clickedFace);
    final Vec3 clickLoc = Vec3.atCenterOf(placementAbs).add(clickOffset);
    final BlockHitResult hit = new BlockHitResult(clickLoc, clickedFace, surfaceAbs, false);
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack relayItem = new ItemStack(Registries.getItem("relay"));
    player.setItemInHand(InteractionHand.MAIN_HAND, relayItem);
    final UseOnContext uoc = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, relayItem, hit);
    final BlockPlaceContext bpc = new BlockPlaceContext(uoc);
    return Registries.getBlock("relay").getStateForPlacement(bpc);
  }

  // face=DOWN: click UP face of stone, rotation sub-cases ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation0(GameTestHelper helper)
  {
    // face=DOWN (clickedFace=UP), dir=NORTH (negative Z) → rotation=0 (default)
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.UP, new Vec3(0, 0, -0.3));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.DOWN)
      helper.fail("expected FACING=DOWN, got " + s.getValue(CircuitComponents.DirectedComponentBlock.FACING));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation1(GameTestHelper helper)
  {
    // face=DOWN, dir=EAST (+X) → rotation=1
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.UP, new Vec3(0.3, 0, 0));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
      helper.fail("expected ROTATION=1, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation2(GameTestHelper helper)
  {
    // face=DOWN, dir=SOUTH (+Z) → rotation=2
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.UP, new Vec3(0, 0, 0.3));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 2)
      helper.fail("expected ROTATION=2, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation3(GameTestHelper helper)
  {
    // face=DOWN, dir=WEST (-X) → rotation=3
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.UP, new Vec3(-0.3, 0, 0));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 3)
      helper.fail("expected ROTATION=3, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  // face=NORTH: clickedFace=SOUTH ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementNorthFaceRotation0(GameTestHelper helper)
  {
    // face=NORTH (clickedFace=SOUTH), dir=UP (+Y, zero X) → rotation=0 (default)
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.SOUTH, new Vec3(0, 0.3, 0));
    if(s == null) helper.fail("getStateForPlacement returned null for NORTH face");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.NORTH)
      helper.fail("expected FACING=NORTH, got " + s.getValue(CircuitComponents.DirectedComponentBlock.FACING));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementNorthFaceRotation1(GameTestHelper helper)
  {
    // face=NORTH, dir=EAST (+X dominant) → rotation=1
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.SOUTH, new Vec3(0.3, 0, 0));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
      helper.fail("expected ROTATION=1, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementNorthFaceRotation2(GameTestHelper helper)
  {
    // face=NORTH, dir=DOWN (-Y dominant) → rotation=2
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.SOUTH, new Vec3(0, -0.3, 0));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 2)
      helper.fail("expected ROTATION=2, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  // face=EAST: clickedFace=WEST ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementEastFaceRotation1(GameTestHelper helper)
  {
    // face=EAST (clickedFace=WEST), dir=SOUTH (+Z dominant) → rotation=1
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.WEST, new Vec3(0, 0, 0.3));
    if(s == null) helper.fail("getStateForPlacement returned null for EAST face");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.EAST)
      helper.fail("expected FACING=EAST, got " + s.getValue(CircuitComponents.DirectedComponentBlock.FACING));
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
      helper.fail("expected ROTATION=1, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementEastFaceRotation2(GameTestHelper helper)
  {
    // face=EAST, dir=DOWN (-Y dominant) → rotation=2
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.WEST, new Vec3(0, -0.3, 0));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 2)
      helper.fail("expected ROTATION=2, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  // face=SOUTH: clickedFace=NORTH ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementSouthFaceRotation1(GameTestHelper helper)
  {
    // face=SOUTH (clickedFace=NORTH), dir=WEST (-X dominant) → rotation=1
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.NORTH, new Vec3(-0.3, 0, 0));
    if(s == null) helper.fail("getStateForPlacement returned null for SOUTH face");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.SOUTH)
      helper.fail("expected FACING=SOUTH, got " + s.getValue(CircuitComponents.DirectedComponentBlock.FACING));
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
      helper.fail("expected ROTATION=1, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementSouthFaceRotation3(GameTestHelper helper)
  {
    // face=SOUTH, dir=EAST (+X dominant) → rotation=3
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.NORTH, new Vec3(0.3, 0, 0));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 3)
      helper.fail("expected ROTATION=3, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  // face=WEST: clickedFace=EAST ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementWestFaceRotation1(GameTestHelper helper)
  {
    // face=WEST (clickedFace=EAST), dir=NORTH (-Z dominant) → rotation=1
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.EAST, new Vec3(0, 0, -0.3));
    if(s == null) helper.fail("getStateForPlacement returned null for WEST face");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.WEST)
      helper.fail("expected FACING=WEST, got " + s.getValue(CircuitComponents.DirectedComponentBlock.FACING));
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
      helper.fail("expected ROTATION=1, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementWestFaceRotation3(GameTestHelper helper)
  {
    // face=WEST, dir=SOUTH (+Z dominant) → rotation=3
    helper.setBlock(RELAY_POS, Blocks.STONE);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.EAST, new Vec3(0, 0, 0.3));
    if(s == null) helper.fail("getStateForPlacement returned null");
    if(s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 3)
      helper.fail("expected ROTATION=3, got " + s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION));
    helper.succeed();
  }

  // canSurvive fails → getStateForPlacement returns null ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementReturnsNullWhenNoSurface(GameTestHelper helper)
  {
    // POS is AIR; clicking UP on AIR means canSurvive will fail → returns null.
    // We need the POS block itself to be AIR so relay can't attach.
    helper.setBlock(RELAY_POS, Blocks.AIR);
    final BlockState s = relayPlacementState(helper, RELAY_POS, Direction.UP, Vec3.ZERO);
    if(s != null) helper.fail("expected null from getStateForPlacement when surface is not sturdy");
    helper.succeed();
  }

  // onRemove: remove relay's support block → relay should disappear ---

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void relayDropsWhenSupportBlockRemoved(GameTestHelper helper)
  {
    // Place stone as support and relay on top.
    final BlockPos supportPos = RELAY_POS;
    final BlockPos relayPos = RELAY_POS.above();
    helper.setBlock(supportPos, Blocks.STONE);
    helper.setBlock(relayPos, Registries.getBlock("relay").defaultBlockState()
      .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
      .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    // Remove support → relay should revert to air via updateShape → onRemove.
    helper.setBlock(supportPos, Blocks.AIR);
    helper.succeedWhen(() -> {
      if(!helper.getBlockState(relayPos).isAir())
        helper.fail("expected relay to become air after support removed");
    });
  }

  // --- RelayBlock.tick deactivation ---------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
  public static void relayDeactivatesWhenPowerRemovedViaTick(GameTestHelper helper)
  {
    // Provide a permanent north-side mount so the relay survives when power is removed.
    // Without this, removing INPUT_POS triggers updateShape → canSurvive(FACING=NORTH, north=air) → relay drops.
    helper.setBlock(RELAY_POS.north(), Blocks.STONE);
    placePoweredInput(helper, Registries.getBlock("relay"));
    helper.runAfterDelay(5, () -> helper.setBlock(INPUT_POS, Blocks.AIR));
    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("relay"), false, 0,
      "relay must deactivate after power source removed"));
  }

  // --- BistableRelayBlock falling edge ------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
  public static void bistableRelayDeactivatesOnFallingEdge(GameTestHelper helper)
  {
    // Provide permanent north mount so relay survives when power is removed.
    helper.setBlock(RELAY_POS.north(), Blocks.STONE);
    placePoweredInput(helper, Registries.getBlock("bistable_relay"));
    // After 5 ticks the bistable is latched (POWERED=true, STATE=1). Remove power to hit the
    // falling-edge path in BistableRelayBlock.update (!powered && pwstate) → POWERED=false.
    helper.runAfterDelay(5, () -> helper.setBlock(INPUT_POS, Blocks.AIR));
    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("bistable_relay"), false, 1,
      "bistable relay must set POWERED=false after power removed while STATE stays latched"));
  }

  // --- InvertedRelayBlock deactivation path -------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void invertedRelayDeactivatesWhenPowerRemoved(GameTestHelper helper)
  {
    // Provide permanent north mount so relay survives when power is removed.
    helper.setBlock(RELAY_POS.north(), Blocks.STONE);
    placePoweredInput(helper, Registries.getBlock("inverted_relay"));
    // At t=4, assert relay activated (tick scheduled at t=0+2 already fired → POWERED=true).
    helper.runAtTickTime(4, () -> assertRelayState(helper, Registries.getBlock("inverted_relay"), true, 0,
      "inverted relay must be POWERED=true before power removal"));
    // Remove power at t=5; update() fires immediately → else-branch → POWERED=false (lines 541-542).
    helper.runAtTickTime(5, () -> helper.setBlock(INPUT_POS, Blocks.AIR));
    // At t=6, verify deactivated and succeed.
    helper.runAtTickTime(6, () -> {
      assertRelayState(helper, Registries.getBlock("inverted_relay"), false, 0,
        "inverted relay must set POWERED=false immediately when power source removed");
      helper.succeed();
    });
  }

  // --- BridgeRelayBlock coverage -----------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void bridgeRelayPlacedAndGetSignalDoesNotThrow(GameTestHelper helper)
  {
    // Place bridge relay at the default location and query its signal — exercises getSignal paths.
    helper.setBlock(RELAY_POS, Registries.getBlock("bridge_relay").defaultBlockState());
    // Query signal from multiple directions (output, left, right, unsupported).
    final BlockPos abs = helper.absolutePos(RELAY_POS);
    for(Direction d : Direction.values()) {
      final int sig = helper.getBlockState(RELAY_POS).getSignal(helper.getLevel(), abs, d);
      if(sig < 0) helper.fail("getSignal must be non-negative for " + d);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void bridgeRelayGetInputPowerFromRedstoneWire(GameTestHelper helper)
  {
    // Place redstone wire adjacent to bridge relay to exercise the REDSTONE_WIRE branch in getInputPower.
    final BlockPos wirePos = RELAY_POS.east();
    helper.setBlock(wirePos.below(), Blocks.STONE);
    helper.setBlock(wirePos, Blocks.REDSTONE_WIRE);
    helper.setBlock(RELAY_POS, Registries.getBlock("bridge_relay").defaultBlockState());
    // Query getSignal from EAST to trigger getInputPower via the wire path.
    final int sig = helper.getBlockState(RELAY_POS).getSignal(helper.getLevel(), helper.absolutePos(RELAY_POS), Direction.EAST);
    if(sig < 0) helper.fail("getSignal must be non-negative, got " + sig);
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void bridgeRelayGetSignalFromLeftWireConnection(GameTestHelper helper)
  {
    // Exercise BridgeRelayBlock.getSignal() left-wire path: place wire on north face.
    final BlockPos leftPos = RELAY_POS.north();
    helper.setBlock(leftPos.below(), Blocks.STONE);
    helper.setBlock(leftPos, Blocks.REDSTONE_WIRE);
    helper.setBlock(RELAY_POS, Registries.getBlock("bridge_relay").defaultBlockState());
    final int sig = helper.getBlockState(RELAY_POS).getSignal(helper.getLevel(), helper.absolutePos(RELAY_POS), Direction.NORTH);
    if(sig < 0) helper.fail("getSignal must return non-negative value");
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
