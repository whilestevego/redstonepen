package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import wile.redstonepen.gametestcommon.RelayTests

@Suppress("UtilityClassWithPublicConstructor")
class RelayGameTests {
    companion object {
        private const val EMPTY_RELAY_TEMPLATE = "redstonepen:relay_activates_from_redstone"

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun relayActivatesFromRedstone(helper: GameTestHelper) =
            RelayTests.relayActivatesFromRedstone(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun invertedRelayRegistersPoweredInput(helper: GameTestHelper) =
            RelayTests.invertedRelayRegistersPoweredInput(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun pulseRelayClearsItsPulseAfterTick(helper: GameTestHelper) =
            RelayTests.pulseRelayClearsItsPulseAfterTick(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun pulseRelaySetsPoweredFalseWhenInputRemoved(helper: GameTestHelper) =
            RelayTests.pulseRelaySetsPoweredFalseWhenInputRemoved(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun bistableRelayLatchesOnFirstRisingEdge(helper: GameTestHelper) =
            RelayTests.bistableRelayLatchesOnFirstRisingEdge(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun relayDownFacingRotation0ActivatesFromEast(helper: GameTestHelper) =
            RelayTests.relayDownFacingRotation0ActivatesFromEast(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun relayDownFacingRotation0DoesNotActivateFromNorth(helper: GameTestHelper) =
            RelayTests.relayDownFacingRotation0DoesNotActivateFromNorth(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun relayDownFacingRotation1ShiftsOutputToEast(helper: GameTestHelper) =
            RelayTests.relayDownFacingRotation1ShiftsOutputToEast(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun relayDownFacingRotation1ActivatesFromNorth(helper: GameTestHelper) =
            RelayTests.relayDownFacingRotation1ActivatesFromNorth(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun downFacingRotation0FrontIsNorth(helper: GameTestHelper) =
            RelayTests.downFacingRotation0FrontIsNorth(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun downFacingRotation1FrontIsEast(helper: GameTestHelper) =
            RelayTests.downFacingRotation1FrontIsEast(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun downFacingRotation2FrontIsSouth(helper: GameTestHelper) =
            RelayTests.downFacingRotation2FrontIsSouth(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun downFacingRotation3FrontIsWest(helper: GameTestHelper) =
            RelayTests.downFacingRotation3FrontIsWest(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun upFacingIsOppositeOfFacingForAllDirections(helper: GameTestHelper) =
            RelayTests.upFacingIsOppositeOfFacingForAllDirections(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun downFacingMethodEqualsFacingPropertyForAllDirections(helper: GameTestHelper) =
            RelayTests.downFacingMethodEqualsFacingPropertyForAllDirections(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun rightFacingIsRotationPlusOneRelativeToFront(helper: GameTestHelper) =
            RelayTests.rightFacingIsRotationPlusOneRelativeToFront(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun backFacingIsRotationPlusTwoRelativeToFront(helper: GameTestHelper) =
            RelayTests.backFacingIsRotationPlusTwoRelativeToFront(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun leftFacingIsRotationPlusThreeRelativeToFront(helper: GameTestHelper) =
            RelayTests.leftFacingIsRotationPlusThreeRelativeToFront(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun allSixMappedDirectionsAreDistinctForEveryState(helper: GameTestHelper) =
            RelayTests.allSixMappedDirectionsAreDistinctForEveryState(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun forwardThenReverseStateMappedFacingIsIdentity(helper: GameTestHelper) =
            RelayTests.forwardThenReverseStateMappedFacingIsIdentity(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementDownFaceRotation0(helper: GameTestHelper) =
            RelayTests.getStateForPlacementDownFaceRotation0(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementDownFaceRotation1(helper: GameTestHelper) =
            RelayTests.getStateForPlacementDownFaceRotation1(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementDownFaceRotation2(helper: GameTestHelper) =
            RelayTests.getStateForPlacementDownFaceRotation2(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementDownFaceRotation3(helper: GameTestHelper) =
            RelayTests.getStateForPlacementDownFaceRotation3(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementNorthFaceRotation0(helper: GameTestHelper) =
            RelayTests.getStateForPlacementNorthFaceRotation0(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementNorthFaceRotation1(helper: GameTestHelper) =
            RelayTests.getStateForPlacementNorthFaceRotation1(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementNorthFaceRotation2(helper: GameTestHelper) =
            RelayTests.getStateForPlacementNorthFaceRotation2(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementEastFaceRotation1(helper: GameTestHelper) =
            RelayTests.getStateForPlacementEastFaceRotation1(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementEastFaceRotation2(helper: GameTestHelper) =
            RelayTests.getStateForPlacementEastFaceRotation2(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementSouthFaceRotation1(helper: GameTestHelper) =
            RelayTests.getStateForPlacementSouthFaceRotation1(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementSouthFaceRotation3(helper: GameTestHelper) =
            RelayTests.getStateForPlacementSouthFaceRotation3(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementWestFaceRotation1(helper: GameTestHelper) =
            RelayTests.getStateForPlacementWestFaceRotation1(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementWestFaceRotation3(helper: GameTestHelper) =
            RelayTests.getStateForPlacementWestFaceRotation3(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun getStateForPlacementReturnsNullWhenNoSurface(helper: GameTestHelper) =
            RelayTests.getStateForPlacementReturnsNullWhenNoSurface(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
        fun relayDropsWhenSupportBlockRemoved(helper: GameTestHelper) =
            RelayTests.relayDropsWhenSupportBlockRemoved(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
        fun relayDeactivatesWhenPowerRemovedViaTick(helper: GameTestHelper) =
            RelayTests.relayDeactivatesWhenPowerRemovedViaTick(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
        fun bistableRelayDeactivatesOnFallingEdge(helper: GameTestHelper) =
            RelayTests.bistableRelayDeactivatesOnFallingEdge(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
        fun invertedRelayDeactivatesWhenPowerRemoved(helper: GameTestHelper) =
            RelayTests.invertedRelayDeactivatesWhenPowerRemoved(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun bridgeRelayPlacedAndGetSignalDoesNotThrow(helper: GameTestHelper) =
            RelayTests.bridgeRelayPlacedAndGetSignalDoesNotThrow(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun bridgeRelayGetInputPowerFromRedstoneWire(helper: GameTestHelper) =
            RelayTests.bridgeRelayGetInputPowerFromRedstoneWire(helper)

        @JvmStatic
        @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
        fun bridgeRelayGetSignalFromLeftWireConnection(helper: GameTestHelper) =
            RelayTests.bridgeRelayGetSignalFromLeftWireConnection(helper)
    }
}
