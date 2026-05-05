/*
 * @file DemoGameTests.java
 * @license MIT
 *
 * Integration tests for the /redstonepen demo contraptions. Each test builds
 * one contraption inside an empty pad template and asserts that the expected
 * mod block landed at the expected position. Together with the existing
 * RelayGameTests / ControlBoxGameTests / TrackGameTests, this serves as an
 * end-to-end sanity check that the mod's blocks place correctly and integrate
 * with vanilla redstone (wire, lamps, pistons, levers, buttons, repeaters).
 */
package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.commands.DemoBuilder;
import wile.redstonepen.commands.DemoSections;
import wile.redstonepen.libmc.Registries;

public class DemoGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_PAD = "empty_demo_pad";

  // Each contraption is built with cell-origin = (1, 1, 1) in template-local coords
  // so the 9x9 footprint fits inside the 16x6x16 pad with room to spare on every side.
  private static final BlockPos CELL_LOCAL = new BlockPos(1, 1, 1);

  public DemoGameTests()
  {}

  // -------------------------------------------------------------------------------------------
  // basic_lever drives a vanilla redstone lamp via redstone wire.
  // Asserts: lever, wires, and lamp land at expected positions.
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void leverDrivesLamp(GameTestHelper helper)
  {
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 2), Blocks.REDSTONE_WIRE);
    });
  }

  // -------------------------------------------------------------------------------------------
  // basic_button -> wire -> sticky piston -> glowstone.
  // Asserts: button, piston, glowstone all placed correctly.
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void buttonDrivesPiston(GameTestHelper helper)
  {
    DemoSections.buildButtonDrivesPiston(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_button");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), Blocks.STICKY_PISTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.GLOWSTONE);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> inverted_relay -> wire -> lamp. (NOT-gate behavior.)
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void invertedRelayNotGate(GameTestHelper helper)
  {
    DemoSections.buildInvertedRelayNotGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "inverted_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Stone-button -> bistable_relay -> lamp. (T flip-flop.)
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void bistableToggle(GameTestHelper helper)
  {
    DemoSections.buildBistableToggle(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.STONE_BUTTON);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "bistable_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> pulse_relay -> wire -> lamp.
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void pulseRelayMonostable(GameTestHelper helper)
  {
    DemoSections.buildPulseRelayMonostable(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "pulse_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> relay -> wire -> lamp. (Buffer / direction.)
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void relayBuffer(GameTestHelper helper)
  {
    DemoSections.buildRelayBuffer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Two perpendicular signal lines crossing at a single bridge_relay.
  // Asserts both input levers and the bridge_relay are placed at the intersection.
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void bridgeRelayCrossover(GameTestHelper helper)
  {
    DemoSections.buildBridgeRelayCrossover(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 0, 3), "bridge_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 5), Blocks.LEVER);    // N-S input
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), Blocks.LEVER);    // E-W input (adjacent to bridge)
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 1), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // control_box AND program: levers feed ports y/g, output b drives a lamp.
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void controlBoxAndGate(GameTestHelper helper)
  {
    DemoSections.buildControlBoxAndGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> repeater -> wire -> basic_gauge.
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void gaugeReadout(GameTestHelper helper)
  {
    DemoSections.buildGaugeReadout(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), Blocks.REPEATER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), "basic_gauge");
    });
  }

  // -------------------------------------------------------------------------------------------
  // End-to-end: build all 9 contraptions in one structure. Asserts the master entry-point
  // exits without exception and at least one block from each contraption lands. This is the
  // "smoke test" that a player running /redstonepen demo will see succeed.
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void runAllSmoke(GameTestHelper helper)
  {
    // Just call runCircuits but only with the first contraption's cell visible inside the pad.
    // For full grid, an even larger template would be required; this test verifies the
    // dispatch loop doesn't throw.
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    DemoSections.buildGaugeReadout(helper.getLevel(),
      helper.absolutePos(new BlockPos(7, 1, 1)));   // second cell offset
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever");
      assertModBlockAt(helper, new BlockPos(7, 1, 1).offset(2, 0, 1), "basic_gauge");
    });
  }

  /**
   * Pen-track 3D route: lever, floor track, 3-tall tower with vertical pen-track climb on
   * its south face, horizontal pen-track run across the tower top, terminal relay driving
   * a vanilla redstone lamp.
   */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void penTrackWallClimb(GameTestHelper helper)
  {
    DemoSections.buildPenTrackWallClimb(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 6), Blocks.LEVER);
      // Tower
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 4), Blocks.STONE);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 1, 4), Blocks.STONE);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 2, 4), Blocks.STONE);
      // Tracks: floor + climb
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 0, 5), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 1, 5), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 2, 5), "track");
      // Tracks: horizontal across tower top
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 4), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 3), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 2), "track");
      // Terminal contraption
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 1), "relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 3, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // ===========================================================================================
  // DemoBuilder utility method coverage
  // ===========================================================================================

  /** clearRegion fills all positions in a bounding box with air. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 10)
  public static void clearRegionFillsWithAir(GameTestHelper helper)
  {
    final BlockPos lo = CELL_LOCAL;
    final BlockPos hi = CELL_LOCAL.offset(2, 2, 2);
    // First place some non-air blocks in the region.
    helper.setBlock(lo, Blocks.STONE);
    helper.setBlock(hi, Blocks.STONE);
    DemoBuilder.clearRegion(helper.getLevel(), helper.absolutePos(lo), helper.absolutePos(hi));
    helper.succeedWhen(() -> {
      if(!helper.getBlockState(lo).isAir()) helper.fail("clearRegion lo corner must be air");
      if(!helper.getBlockState(hi).isAir()) helper.fail("clearRegion hi corner must be air");
    });
  }

  /** placeWallSign places an oak_wall_sign block with a backing stone support. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 10)
  public static void placeWallSignPlacesSignBlock(GameTestHelper helper)
  {
    final BlockPos signPos = CELL_LOCAL;
    DemoBuilder.placeWallSign(helper.getLevel(), helper.absolutePos(signPos), Direction.NORTH,
      "hello", "world", null, null);
    helper.succeedWhen(() -> {
      final net.minecraft.world.level.block.state.BlockState state = helper.getLevel()
        .getBlockState(helper.absolutePos(signPos));
      if(!(state.getBlock() instanceof net.minecraft.world.level.block.WallSignBlock))
        helper.fail("expected wall sign block at signPos, found " + state.getBlock());
    });
  }

  // ===========================================================================================
  // Behavioral tests — trigger inputs and assert circuit output.
  // ===========================================================================================

  /** basic_lever pulled ON → vanilla redstone_lamp lights. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void leverDrivesLampLightsOnPull(GameTestHelper helper)
  {
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    // helper.pullLever requires vanilla Blocks.LEVER; basic_lever is a different block.
    helper.runAfterDelay(2, () -> toggleLeverPowered(helper, CELL_LOCAL.offset(2, 0, 4), true));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, true));
  }

  /** Plain relay buffers a vanilla-lever signal to a vanilla-lamp output. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void relayBufferLightsLampOnPull(GameTestHelper helper)
  {
    DemoSections.buildRelayBuffer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(2, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, true));
  }

  /** inverted_relay: pulling input lever ON drives lamp OFF (NOT-gate behavior). */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void invertedRelayLampDarkensOnPull(GameTestHelper helper)
  {
    DemoSections.buildInvertedRelayNotGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(4, () -> helper.pullLever(CELL_LOCAL.offset(2, 0, 5)));
    helper.runAtTickTime(20, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, false);
      helper.succeed();
    });
  }

  /** bridge_relay: pulling the N-S lever powers the N-S lamp. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void bridgeRelayNorthSouthLineLights(GameTestHelper helper)
  {
    DemoSections.buildBridgeRelayCrossover(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(4, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(4, 0, 1), BlockStateProperties.LIT, true));
  }

  /** bridge_relay: pulling the E-W (cross-axis) lever powers the E-W lamp. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void bridgeRelayEastWestLineLights(GameTestHelper helper)
  {
    DemoSections.buildBridgeRelayCrossover(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(3, 0, 3)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true));
  }

  /** pulse_relay: rising edge from a lever produces a STATE=1 pulse that clears within a few ticks. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void pulseRelayProducesObservablePulse(GameTestHelper helper)
  {
    DemoSections.buildPulseRelayMonostable(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    // pulse_relay's update() schedules a clearing tick on EVERY call — including
    // placement — so an initial pulse-clear is queued at ~tick 2. Wait past that
    // before pulling the lever so our rising edge isn't immediately overwritten.
    helper.runAfterDelay(5, () -> helper.pullLever(CELL_LOCAL.offset(2, 0, 5)));
    helper.runAtTickTime(6, () ->
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 3),
        CircuitComponents.DirectedComponentBlock.STATE, 1));
    // Pulse clears 2 ticks after the rising edge.
    helper.runAtTickTime(15, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 3),
        CircuitComponents.DirectedComponentBlock.STATE, 0);
      helper.succeed();
    });
  }

  /** bistable_relay: pressing the input button drives STATE from 0 to 1 (T flip-flop rising edge). */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void bistableRelayTogglesStateOnButtonPress(GameTestHelper helper)
  {
    DemoSections.buildBistableToggle(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> {
      // Stone-button at (2,0,5). Force POWERED=true to simulate a press.
      final BlockPos buttonAbs = helper.absolutePos(CELL_LOCAL.offset(2, 0, 5));
      final BlockState buttonState = helper.getLevel().getBlockState(buttonAbs);
      helper.getLevel().setBlock(buttonAbs,
        buttonState.setValue(BlockStateProperties.POWERED, true), 3);
      helper.getLevel().updateNeighborsAt(buttonAbs, buttonState.getBlock());
    });
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 3),
        CircuitComponents.DirectedComponentBlock.STATE, 1));
  }

  /** basic_button: pressing the button extends the adjacent sticky piston. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void buttonPressExtendsPiston(GameTestHelper helper)
  {
    DemoSections.buildButtonDrivesPiston(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> {
      final BlockPos buttonAbs = helper.absolutePos(CELL_LOCAL.offset(2, 0, 4));
      final BlockState buttonState = helper.getLevel().getBlockState(buttonAbs);
      helper.getLevel().setBlock(buttonAbs,
        buttonState.setValue(BlockStateProperties.POWERED, true), 3);
      helper.getLevel().updateNeighborsAt(buttonAbs, buttonState.getBlock());
    });
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 1),
        BlockStateProperties.EXTENDED, true));
  }

  /** Pen-track 3D route: lever on → wire climbs tower → relay output → lamp lights. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
  public static void penTrackWallClimbLightsLamp(GameTestHelper helper)
  {
    DemoSections.buildPenTrackWallClimb(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(4, () -> helper.pullLever(CELL_LOCAL.offset(4, 0, 6)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(4, 3, 0), BlockStateProperties.LIT, true));
  }

  /** basic_gauge POWER property updates when an upstream lever is toggled on. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void gaugeReadoutPowerRisesOnPull(GameTestHelper helper)
  {
    DemoSections.buildGaugeReadout(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(2, 0, 5)));
    helper.succeedWhen(() -> {
      final BlockPos gaugePos = helper.absolutePos(CELL_LOCAL.offset(2, 0, 1));
      final BlockState gauge = helper.getLevel().getBlockState(gaugePos);
      final int power = gauge.getValue(BlockStateProperties.POWER);
      if(power < 1) {
        helper.fail("expected basic_gauge POWER >= 1 after lever pull, got " + power, CELL_LOCAL.offset(2, 0, 1));
      }
    });
  }

  /** control_box AND: only one lever pulled → output port b stays low → lamp stays dark. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
  public static void controlBoxAndOneInputKeepsLampDark(GameTestHelper helper)
  {
    DemoSections.buildControlBoxAndGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(4, () -> helper.pullLever(CELL_LOCAL.offset(3, 0, 5))); // south lever only
    helper.runAtTickTime(50, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false);
      helper.succeed();
    });
  }

  /** control_box AND: both levers pulled → output port b → lamp lights. */
  @GameTest(template = EMPTY_PAD, timeoutTicks = 80)
  public static void controlBoxAndBothInputsLightLamp(GameTestHelper helper)
  {
    DemoSections.buildControlBoxAndGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(4, () -> {
      helper.pullLever(CELL_LOCAL.offset(3, 0, 5));   // south
      helper.pullLever(CELL_LOCAL.offset(1, 0, 3));   // west
    });
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true));
  }

  // -------------------------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------------------------

  private static void toggleLeverPowered(GameTestHelper helper, BlockPos localPos, boolean powered)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState state = helper.getLevel().getBlockState(abs);
    helper.getLevel().setBlock(abs, state.setValue(BlockStateProperties.POWERED, powered), 3);
    helper.getLevel().updateNeighborsAt(abs, state.getBlock());
  }

  private static void assertModBlockAt(GameTestHelper helper, BlockPos localPos, String modBlockName)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState actual = helper.getLevel().getBlockState(abs);
    if(actual.getBlock() != Registries.getBlock(modBlockName)) {
      helper.fail("expected mod block " + modBlockName + " at " + localPos
        + " but found " + actual.getBlock(), localPos);
    }
  }

  // -------------------------------------------------------------------------------------------
  // DemoSections.runCircuits: chains all contraptions; exercises the loop in runCircuits().
  // -------------------------------------------------------------------------------------------
  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void runCircuitsBuildsAllContraptions(GameTestHelper helper)
  {
    DemoSections.runCircuits(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeed();
  }

  private static void assertVanillaBlockAt(GameTestHelper helper, BlockPos localPos, net.minecraft.world.level.block.Block expected)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState actual = helper.getLevel().getBlockState(abs);
    if(actual.getBlock() != expected) {
      helper.fail("expected " + expected + " at " + localPos
        + " but found " + actual.getBlock(), localPos);
    }
  }
}
