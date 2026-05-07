package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.gametestcommon.DemoTests;

public class DemoGameTests
{
  private static final String EMPTY_PAD = "redstonepen:empty_demo_pad";

  public DemoGameTests() {}

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void leverDrivesLamp(GameTestHelper helper)
  { DemoTests.leverDrivesLamp(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void buttonDrivesPiston(GameTestHelper helper)
  { DemoTests.buttonDrivesPiston(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void invertedRelayNotGate(GameTestHelper helper)
  { DemoTests.invertedRelayNotGate(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void bistableToggle(GameTestHelper helper)
  { DemoTests.bistableToggle(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void pulseRelayMonostable(GameTestHelper helper)
  { DemoTests.pulseRelayMonostable(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void relayBuffer(GameTestHelper helper)
  { DemoTests.relayBuffer(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void bridgeRelayCrossover(GameTestHelper helper)
  { DemoTests.bridgeRelayCrossover(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void controlBoxAndGate(GameTestHelper helper)
  { DemoTests.controlBoxAndGate(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void gaugeReadout(GameTestHelper helper)
  { DemoTests.gaugeReadout(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void runAllSmoke(GameTestHelper helper)
  { DemoTests.runAllSmoke(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void penTrackWallClimb(GameTestHelper helper)
  { DemoTests.penTrackWallClimb(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 10)
  public static void clearRegionFillsWithAir(GameTestHelper helper)
  { DemoTests.clearRegionFillsWithAir(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 10)
  public static void placeWallSignPlacesSignBlock(GameTestHelper helper)
  { DemoTests.placeWallSignPlacesSignBlock(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void leverDrivesLampLightsOnPull(GameTestHelper helper)
  { DemoTests.leverDrivesLampLightsOnPull(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void relayBufferLightsLampOnPull(GameTestHelper helper)
  { DemoTests.relayBufferLightsLampOnPull(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void invertedRelayLampDarkensOnPull(GameTestHelper helper)
  { DemoTests.invertedRelayLampDarkensOnPull(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void bridgeRelayNorthSouthLineLights(GameTestHelper helper)
  { DemoTests.bridgeRelayNorthSouthLineLights(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void bridgeRelayEastWestLineLights(GameTestHelper helper)
  { DemoTests.bridgeRelayEastWestLineLights(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void pulseRelayProducesObservablePulse(GameTestHelper helper)
  { DemoTests.pulseRelayProducesObservablePulse(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void bistableRelayTogglesStateOnButtonPress(GameTestHelper helper)
  { DemoTests.bistableRelayTogglesStateOnButtonPress(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
  public static void buttonPressExtendsPiston(GameTestHelper helper)
  { DemoTests.buttonPressExtendsPiston(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void penTrackWallClimbLightsLamp(GameTestHelper helper)
  { DemoTests.penTrackWallClimbLightsLamp(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void penTrackWallClimbTracksArePoweredImmediately(GameTestHelper helper)
  { DemoTests.penTrackWallClimbTracksArePoweredImmediately(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void gaugeReadoutPowerRisesOnPull(GameTestHelper helper)
  { DemoTests.gaugeReadoutPowerRisesOnPull(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
  public static void controlBoxAndOneInputKeepsLampDark(GameTestHelper helper)
  { DemoTests.controlBoxAndOneInputKeepsLampDark(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 80)
  public static void controlBoxAndBothInputsLightLamp(GameTestHelper helper)
  { DemoTests.controlBoxAndBothInputsLightLamp(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void runCircuitsBuildsAllContraptions(GameTestHelper helper)
  { DemoTests.runCircuitsBuildsAllContraptions(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void trafficLightPlacesBlocksCorrectly(GameTestHelper helper)
  { DemoTests.trafficLightPlacesBlocksCorrectly(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void pulseCounterPlacesBlocksCorrectly(GameTestHelper helper)
  { DemoTests.pulseCounterPlacesBlocksCorrectly(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void srLatchPlacesBlocksCorrectly(GameTestHelper helper)
  { DemoTests.srLatchPlacesBlocksCorrectly(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void pwmDemoPlacesBlocksCorrectly(GameTestHelper helper)
  { DemoTests.pwmDemoPlacesBlocksCorrectly(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void stepSequencerPlacesBlocksCorrectly(GameTestHelper helper)
  { DemoTests.stepSequencerPlacesBlocksCorrectly(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void trafficLightExactlyOneLampLit(GameTestHelper helper)
  { DemoTests.trafficLightExactlyOneLampLit(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
  public static void pulseCounterFirstPressLightsNorthLamp(GameTestHelper helper)
  { DemoTests.pulseCounterFirstPressLightsNorthLamp(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 80)
  public static void pulseCounterSecondPressFillsEastLamp(GameTestHelper helper)
  { DemoTests.pulseCounterSecondPressFillsEastLamp(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 120)
  public static void pulseCounterFourthPressWrapsToZero(GameTestHelper helper)
  { DemoTests.pulseCounterFourthPressWrapsToZero(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
  public static void srLatchSetsAndHolds(GameTestHelper helper)
  { DemoTests.srLatchSetsAndHolds(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 80)
  public static void srLatchResetsAfterSet(GameTestHelper helper)
  { DemoTests.srLatchResetsAfterSet(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void pwmZeroDutyLampStaysDark(GameTestHelper helper)
  { DemoTests.pwmZeroDutyLampStaysDark(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
  public static void pwmFullDutyLightsLamp(GameTestHelper helper)
  { DemoTests.pwmFullDutyLightsLamp(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
  public static void stepSequencerStartsAtStepZero(GameTestHelper helper)
  { DemoTests.stepSequencerStartsAtStepZero(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
  public static void stepSequencerAdvancesToStepOne(GameTestHelper helper)
  { DemoTests.stepSequencerAdvancesToStepOne(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 100)
  public static void stepSequencerWrapsAroundToStepZero(GameTestHelper helper)
  { DemoTests.stepSequencerWrapsAroundToStepZero(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
  public static void holdTimerPlacesBlocksCorrectly(GameTestHelper helper)
  { DemoTests.holdTimerPlacesBlocksCorrectly(helper); }

  @GameTest(template = EMPTY_PAD, timeoutTicks = 120)
  public static void holdTimerLightsLampAfterPress(GameTestHelper helper)
  { DemoTests.holdTimerLightsLampAfterPress(helper); }
}
