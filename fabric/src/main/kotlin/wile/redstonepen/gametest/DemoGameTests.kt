package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import wile.redstonepen.gametestcommon.DemoTests

@Suppress("UtilityClassWithPublicConstructor")
class DemoGameTests {
    companion object {
        private const val EMPTY_PAD = "redstonepen:empty_demo_pad"

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun leverDrivesLamp(helper: GameTestHelper) = DemoTests.leverDrivesLamp(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun buttonDrivesPiston(helper: GameTestHelper) = DemoTests.buttonDrivesPiston(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun invertedRelayNotGate(helper: GameTestHelper) = DemoTests.invertedRelayNotGate(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun bistableToggle(helper: GameTestHelper) = DemoTests.bistableToggle(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun pulseRelayMonostable(helper: GameTestHelper) = DemoTests.pulseRelayMonostable(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun relayBuffer(helper: GameTestHelper) = DemoTests.relayBuffer(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun bridgeRelayCrossover(helper: GameTestHelper) = DemoTests.bridgeRelayCrossover(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun controlBoxAndGate(helper: GameTestHelper) = DemoTests.controlBoxAndGate(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun gaugeReadout(helper: GameTestHelper) = DemoTests.gaugeReadout(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun runAllSmoke(helper: GameTestHelper) = DemoTests.runAllSmoke(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun penTrackWallClimb(helper: GameTestHelper) = DemoTests.penTrackWallClimb(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 10)
        fun clearRegionFillsWithAir(helper: GameTestHelper) =
            DemoTests.clearRegionFillsWithAir(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 10)
        fun placeWallSignPlacesSignBlock(helper: GameTestHelper) =
            DemoTests.placeWallSignPlacesSignBlock(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
        fun leverDrivesLampLightsOnPull(helper: GameTestHelper) =
            DemoTests.leverDrivesLampLightsOnPull(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
        fun relayBufferLightsLampOnPull(helper: GameTestHelper) =
            DemoTests.relayBufferLightsLampOnPull(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun invertedRelayLampDarkensOnPull(helper: GameTestHelper) =
            DemoTests.invertedRelayLampDarkensOnPull(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
        fun bridgeRelayNorthSouthLineLights(helper: GameTestHelper) =
            DemoTests.bridgeRelayNorthSouthLineLights(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
        fun bridgeRelayEastWestLineLights(helper: GameTestHelper) =
            DemoTests.bridgeRelayEastWestLineLights(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun pulseRelayProducesObservablePulse(helper: GameTestHelper) =
            DemoTests.pulseRelayProducesObservablePulse(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
        fun bistableRelayTogglesStateOnButtonPress(helper: GameTestHelper) =
            DemoTests.bistableRelayTogglesStateOnButtonPress(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 30)
        fun buttonPressExtendsPiston(helper: GameTestHelper) =
            DemoTests.buttonPressExtendsPiston(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun penTrackWallClimbLightsLamp(helper: GameTestHelper) =
            DemoTests.penTrackWallClimbLightsLamp(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun penTrackWallClimbTracksArePoweredImmediately(helper: GameTestHelper) =
            DemoTests.penTrackWallClimbTracksArePoweredImmediately(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun gaugeReadoutPowerRisesOnPull(helper: GameTestHelper) =
            DemoTests.gaugeReadoutPowerRisesOnPull(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
        fun controlBoxAndOneInputKeepsLampDark(helper: GameTestHelper) =
            DemoTests.controlBoxAndOneInputKeepsLampDark(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 80)
        fun controlBoxAndBothInputsLightLamp(helper: GameTestHelper) =
            DemoTests.controlBoxAndBothInputsLightLamp(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun runCircuitsBuildsAllContraptions(helper: GameTestHelper) =
            DemoTests.runCircuitsBuildsAllContraptions(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun trafficLightPlacesBlocksCorrectly(helper: GameTestHelper) =
            DemoTests.trafficLightPlacesBlocksCorrectly(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun pulseCounterPlacesBlocksCorrectly(helper: GameTestHelper) =
            DemoTests.pulseCounterPlacesBlocksCorrectly(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun srLatchPlacesBlocksCorrectly(helper: GameTestHelper) =
            DemoTests.srLatchPlacesBlocksCorrectly(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun pwmDemoPlacesBlocksCorrectly(helper: GameTestHelper) =
            DemoTests.pwmDemoPlacesBlocksCorrectly(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun stepSequencerPlacesBlocksCorrectly(helper: GameTestHelper) =
            DemoTests.stepSequencerPlacesBlocksCorrectly(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun trafficLightExactlyOneLampLit(helper: GameTestHelper) =
            DemoTests.trafficLightExactlyOneLampLit(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
        fun pulseCounterFirstPressLightsNorthLamp(helper: GameTestHelper) =
            DemoTests.pulseCounterFirstPressLightsNorthLamp(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 80)
        fun pulseCounterSecondPressFillsEastLamp(helper: GameTestHelper) =
            DemoTests.pulseCounterSecondPressFillsEastLamp(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 120)
        fun pulseCounterFourthPressWrapsToZero(helper: GameTestHelper) =
            DemoTests.pulseCounterFourthPressWrapsToZero(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
        fun srLatchSetsAndHolds(helper: GameTestHelper) = DemoTests.srLatchSetsAndHolds(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 80)
        fun srLatchResetsAfterSet(helper: GameTestHelper) = DemoTests.srLatchResetsAfterSet(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun pwmZeroDutyLampStaysDark(helper: GameTestHelper) =
            DemoTests.pwmZeroDutyLampStaysDark(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
        fun pwmFullDutyLightsLamp(helper: GameTestHelper) = DemoTests.pwmFullDutyLightsLamp(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 40)
        fun stepSequencerStartsAtStepZero(helper: GameTestHelper) =
            DemoTests.stepSequencerStartsAtStepZero(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 60)
        fun stepSequencerAdvancesToStepOne(helper: GameTestHelper) =
            DemoTests.stepSequencerAdvancesToStepOne(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 100)
        fun stepSequencerWrapsAroundToStepZero(helper: GameTestHelper) =
            DemoTests.stepSequencerWrapsAroundToStepZero(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 20)
        fun holdTimerPlacesBlocksCorrectly(helper: GameTestHelper) =
            DemoTests.holdTimerPlacesBlocksCorrectly(helper)

        @JvmStatic
        @GameTest(template = EMPTY_PAD, timeoutTicks = 120)
        fun holdTimerLightsLampAfterPress(helper: GameTestHelper) =
            DemoTests.holdTimerLightsLampAfterPress(helper)
    }
}
