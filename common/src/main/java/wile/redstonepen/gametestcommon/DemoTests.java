package wile.redstonepen.gametestcommon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.blocks.track.RedstoneTrackDefs;
import wile.redstonepen.blocks.track.TrackBlockEntity;
import wile.redstonepen.commands.DemoBuilder;
import wile.redstonepen.commands.DemoSections;
import wile.redstonepen.libmc.Registries;

public final class DemoTests
{
  // Each contraption is built with cell-origin = (1, 1, 1) in template-local coords
  // so the 9x9 footprint fits inside the 16x6x16 pad with room to spare on every side.
  private static final BlockPos CELL_LOCAL = new BlockPos(1, 1, 1);

  private DemoTests() {}

  public static void leverDrivesLamp(GameTestHelper helper)
  {
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 2), Blocks.REDSTONE_WIRE);
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2)); // middle of 3-wire run
    });
  }

  public static void buttonDrivesPiston(GameTestHelper helper)
  {
    DemoSections.buildButtonDrivesPiston(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_button");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), Blocks.STICKY_PISTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.GLOWSTONE);
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 3)); // south=button, north=wire
    });
  }

  public static void invertedRelayNotGate(GameTestHelper helper)
  {
    DemoSections.buildInvertedRelayNotGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "inverted_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2)); // between relay and wire(2,0,1)
    });
  }

  public static void bistableToggle(GameTestHelper helper)
  {
    DemoSections.buildBistableToggle(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.STONE_BUTTON);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "bistable_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2));
    });
  }

  public static void pulseRelayMonostable(GameTestHelper helper)
  {
    DemoSections.buildPulseRelayMonostable(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "pulse_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2));
    });
  }

  public static void relayBuffer(GameTestHelper helper)
  {
    DemoSections.buildRelayBuffer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2));
    });
  }

  public static void bridgeRelayCrossover(GameTestHelper helper)
  {
    DemoSections.buildBridgeRelayCrossover(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 0, 3), "bridge_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 5), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 1), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 4));
    });
  }

  public static void controlBoxAndGate(GameTestHelper helper)
  {
    DemoSections.buildControlBoxAndGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3));
    });
  }

  public static void gaugeReadout(GameTestHelper helper)
  {
    DemoSections.buildGaugeReadout(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), Blocks.REPEATER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), "basic_gauge");
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 4));
    });
  }

  public static void runAllSmoke(GameTestHelper helper)
  {
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    DemoSections.buildGaugeReadout(helper.getLevel(),
      helper.absolutePos(new BlockPos(7, 1, 1)));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever");
      assertModBlockAt(helper, new BlockPos(7, 1, 1).offset(2, 0, 1), "basic_gauge");
    });
  }

  public static void penTrackWallClimb(GameTestHelper helper)
  {
    DemoSections.buildPenTrackWallClimb(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 6), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 4), Blocks.STONE);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 1, 4), Blocks.STONE);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 2, 4), Blocks.STONE);
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 0, 5), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 1, 5), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 2, 5), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 4), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 3), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 2), "track");
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 1), "relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 3, 0), Blocks.REDSTONE_LAMP);
    });
  }

  public static void clearRegionFillsWithAir(GameTestHelper helper)
  {
    final BlockPos lo = CELL_LOCAL;
    final BlockPos hi = CELL_LOCAL.offset(2, 2, 2);
    helper.setBlock(lo, Blocks.STONE);
    helper.setBlock(hi, Blocks.STONE);
    DemoBuilder.clearRegion(helper.getLevel(), helper.absolutePos(lo), helper.absolutePos(hi));
    helper.succeedWhen(() -> {
      if(!helper.getBlockState(lo).isAir()) helper.fail("clearRegion lo corner must be air");
      if(!helper.getBlockState(hi).isAir()) helper.fail("clearRegion hi corner must be air");
    });
  }

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

  public static void leverDrivesLampLightsOnPull(GameTestHelper helper)
  {
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> toggleLeverPowered(helper, CELL_LOCAL.offset(2, 0, 4), true));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, true));
  }

  public static void relayBufferLightsLampOnPull(GameTestHelper helper)
  {
    DemoSections.buildRelayBuffer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(2, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, true));
  }

  public static void invertedRelayLampDarkensOnPull(GameTestHelper helper)
  {
    DemoSections.buildInvertedRelayNotGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(4, () -> helper.pullLever(CELL_LOCAL.offset(2, 0, 5)));
    helper.runAtTickTime(20, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, false);
      helper.succeed();
    });
  }

  public static void bridgeRelayNorthSouthLineLights(GameTestHelper helper)
  {
    DemoSections.buildBridgeRelayCrossover(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(4, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(4, 0, 1), BlockStateProperties.LIT, true));
  }

  public static void bridgeRelayEastWestLineLights(GameTestHelper helper)
  {
    DemoSections.buildBridgeRelayCrossover(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(3, 0, 3)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true));
  }

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

  public static void penTrackWallClimbLightsLamp(GameTestHelper helper)
  {
    DemoSections.buildPenTrackWallClimb(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(4, 3, 0), BlockStateProperties.LIT, true));
  }

  public static void penTrackWallClimbTracksArePoweredImmediately(GameTestHelper helper)
  {
    DemoSections.buildPenTrackWallClimb(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      final BlockPos[] trackPositions = {
        CELL_LOCAL.offset(4, 0, 5), CELL_LOCAL.offset(4, 1, 5), CELL_LOCAL.offset(4, 2, 5),
        CELL_LOCAL.offset(4, 3, 4), CELL_LOCAL.offset(4, 3, 3), CELL_LOCAL.offset(4, 3, 2)
      };
      for(BlockPos local : trackPositions) {
        final var te = helper.getLevel().getBlockEntity(helper.absolutePos(local));
        if(!(te instanceof TrackBlockEntity tbe)) {
          helper.fail("expected TrackBlockEntity at " + local, local);
          return;
        }
        if(tbe.getWireFlags() == 0) {
          helper.fail("track at " + local + " has no wire flags", local);
        }
        if((tbe.getStateFlags() & RedstoneTrackDefs.STATE_FLAG_PWR_MASK) == 0) {
          helper.fail("track at " + local + " has no power (state=0x" + Long.toHexString(tbe.getStateFlags()) + ")", local);
        }
      }
    });
  }

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

  public static void controlBoxAndOneInputKeepsLampDark(GameTestHelper helper)
  {
    DemoSections.buildControlBoxAndGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(4, () -> helper.pullLever(CELL_LOCAL.offset(3, 0, 5))); // south lever only
    helper.runAtTickTime(50, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false);
      helper.succeed();
    });
  }

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

  public static void runCircuitsBuildsAllContraptions(GameTestHelper helper)
  {
    DemoSections.runCircuits(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeed();
  }

  public static void trafficLightPlacesBlocksCorrectly(GameTestHelper helper)
  {
    DemoSections.buildTrafficLight(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    DemoSections.refreshWireConnections(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(3, 0, 2));
      assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 3));
      assertWireConnected(helper, CELL_LOCAL.offset(3, 0, 4));
    });
  }

  public static void pulseCounterPlacesBlocksCorrectly(GameTestHelper helper)
  {
    DemoSections.buildPulseCounter(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3));
    });
  }

  public static void srLatchPlacesBlocksCorrectly(GameTestHelper helper)
  {
    DemoSections.buildSrLatch(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.STONE_BUTTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3));
    });
  }

  public static void pwmDemoPlacesBlocksCorrectly(GameTestHelper helper)
  {
    DemoSections.buildPwmDemo(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3));
    });
  }

  public static void stepSequencerPlacesBlocksCorrectly(GameTestHelper helper)
  {
    DemoSections.buildStepSequencer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3));
    });
  }

  public static void trafficLightExactlyOneLampLit(GameTestHelper helper)
  {
    DemoSections.buildTrafficLight(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      final boolean rLit = helper.getBlockState(CELL_LOCAL.offset(3, 0, 1)).getValue(BlockStateProperties.LIT);
      final boolean yLit = helper.getBlockState(CELL_LOCAL.offset(3, 0, 5)).getValue(BlockStateProperties.LIT);
      final boolean gLit = helper.getBlockState(CELL_LOCAL.offset(1, 0, 3)).getValue(BlockStateProperties.LIT);
      final int litCount = (rLit ? 1 : 0) + (yLit ? 1 : 0) + (gLit ? 1 : 0);
      if(litCount != 1) helper.fail("expected exactly one traffic lamp lit, got " + litCount);
    });
  }

  public static void pulseCounterFirstPressLightsNorthLamp(GameTestHelper helper)
  {
    DemoSections.buildPulseCounter(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true));
  }

  public static void pulseCounterSecondPressFillsEastLamp(GameTestHelper helper)
  {
    DemoSections.buildPulseCounter(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2,  () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(7,  () -> releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(15, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.succeedWhen(() -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true);
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true);
    });
  }

  public static void pulseCounterFourthPressWrapsToZero(GameTestHelper helper)
  {
    DemoSections.buildPulseCounter(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2,  () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(7,  () -> releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(15, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(20, () -> releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(30, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(35, () -> releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(45, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(50, () -> releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(70, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, false);
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false);
      helper.assertBlockProperty(CELL_LOCAL.offset(1, 0, 3), BlockStateProperties.LIT, false);
      helper.succeed();
    });
  }

  public static void srLatchSetsAndHolds(GameTestHelper helper)
  {
    DemoSections.buildSrLatch(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 1)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true));
  }

  public static void srLatchResetsAfterSet(GameTestHelper helper)
  {
    DemoSections.buildSrLatch(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2,  () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 1)));  // set
    helper.runAfterDelay(15, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));  // reset
    helper.runAfterDelay(30, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false);
      helper.succeed();
    });
  }

  public static void pwmZeroDutyLampStaysDark(GameTestHelper helper)
  {
    DemoSections.buildPwmDemo(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    // Lever starts off (g=0); b = if(clock%32 < 0, 15, 0) = always 0.
    helper.runAfterDelay(20, () -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false);
      helper.succeed();
    });
  }

  public static void pwmFullDutyLightsLamp(GameTestHelper helper)
  {
    DemoSections.buildPwmDemo(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> helper.pullLever(CELL_LOCAL.offset(1, 0, 3)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true));
  }

  public static void stepSequencerStartsAtStepZero(GameTestHelper helper)
  {
    DemoSections.buildStepSequencer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true);
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false);
      helper.assertBlockProperty(CELL_LOCAL.offset(1, 0, 3), BlockStateProperties.LIT, false);
    });
  }

  public static void stepSequencerAdvancesToStepOne(GameTestHelper helper)
  {
    DemoSections.buildStepSequencer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true));
  }

  public static void stepSequencerWrapsAroundToStepZero(GameTestHelper helper)
  {
    DemoSections.buildStepSequencer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2,  () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(15, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.runAfterDelay(30, () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true));
  }

  public static void holdTimerPlacesBlocksCorrectly(GameTestHelper helper)
  {
    DemoSections.buildHoldTimer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
      assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3));
    });
  }

  public static void holdTimerLightsLampAfterPress(GameTestHelper helper)
  {
    DemoSections.buildHoldTimer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.runAfterDelay(2,  () -> helper.pullLever(CELL_LOCAL.offset(1, 0, 3)));
    helper.runAfterDelay(5,  () -> pressButton(helper, CELL_LOCAL.offset(3, 0, 5)));
    helper.succeedWhen(() ->
      helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true));
  }

  private static void toggleLeverPowered(GameTestHelper helper, BlockPos localPos, boolean powered)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState state = helper.getLevel().getBlockState(abs);
    helper.getLevel().setBlock(abs, state.setValue(BlockStateProperties.POWERED, powered), 3);
    helper.getLevel().updateNeighborsAt(abs, state.getBlock());
  }

  private static void pressButton(GameTestHelper helper, BlockPos localPos)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState state = helper.getLevel().getBlockState(abs);
    helper.getLevel().setBlock(abs, state.setValue(BlockStateProperties.POWERED, true), 3);
    helper.getLevel().updateNeighborsAt(abs, state.getBlock());
  }

  private static void releaseButton(GameTestHelper helper, BlockPos localPos)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState state = helper.getLevel().getBlockState(abs);
    if(state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)) {
      helper.getLevel().setBlock(abs, state.setValue(BlockStateProperties.POWERED, false), 3);
      helper.getLevel().updateNeighborsAt(abs, state.getBlock());
    }
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

  private static void assertVanillaBlockAt(GameTestHelper helper, BlockPos localPos, net.minecraft.world.level.block.Block expected)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState actual = helper.getLevel().getBlockState(abs);
    if(actual.getBlock() != expected) {
      helper.fail("expected " + expected + " at " + localPos
        + " but found " + actual.getBlock(), localPos);
    }
  }

  private static void assertWireConnected(GameTestHelper helper, BlockPos local)
  {
    final BlockState state = helper.getBlockState(local);
    final boolean connected =
      state.getValue(BlockStateProperties.NORTH_REDSTONE) != RedstoneSide.NONE
      || state.getValue(BlockStateProperties.SOUTH_REDSTONE) != RedstoneSide.NONE
      || state.getValue(BlockStateProperties.EAST_REDSTONE) != RedstoneSide.NONE
      || state.getValue(BlockStateProperties.WEST_REDSTONE) != RedstoneSide.NONE;
    if(!connected) helper.fail("redstone wire at " + local + " has no connections (dot state)", local);
  }
}
