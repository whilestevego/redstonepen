package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import wile.redstonepen.ModConstants
import wile.redstonepen.gametestcommon.TrackTests

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
object TrackGameTests {
    private const val TEMPLATE = "relay_activates_from_redstone"

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    fun trackStoresSeededPowerRoute(helper: GameTestHelper) =
        TrackTests.trackStoresSeededPowerRoute(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    fun trackReplacesSeededRouteWhenPowerClears(helper: GameTestHelper) =
        TrackTests.trackReplacesSeededRouteWhenPowerClears(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun writenbtSyncOmitsNetsList(helper: GameTestHelper) =
        TrackTests.writenbtSyncOmitsNetsList(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun writenbtFullIncludesNetsList(helper: GameTestHelper) =
        TrackTests.writenbtFullIncludesNetsList(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun readnbtRestoresStateFlagsAndNets(helper: GameTestHelper) =
        TrackTests.readnbtRestoresStateFlagsAndNets(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun readnbtAcceptsCorruptNetsListWithoutThrowing(helper: GameTestHelper) =
        TrackTests.readnbtAcceptsCorruptNetsListWithoutThrowing(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun addWireFlagsRecordsOnlyNewBits(helper: GameTestHelper) =
        TrackTests.addWireFlagsRecordsOnlyNewBits(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getWireFlagsReadsIndividualBits(helper: GameTestHelper) =
        TrackTests.getWireFlagsReadsIndividualBits(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setSidePowerAndGetSidePowerRoundTrip(helper: GameTestHelper) =
        TrackTests.setSidePowerAndGetSidePowerRoundTrip(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun hasVanillaRedstoneConnectionReadsBitsAndConnectorMask(helper: GameTestHelper) =
        TrackTests.hasVanillaRedstoneConnectionReadsBitsAndConnectorMask(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun updateAllPowerValuesOnIsolatedTrackReturnsMap(helper: GameTestHelper) =
        TrackTests.updateAllPowerValuesOnIsolatedTrackReturnsMap(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun updateAllPowerValuesOnTrackWithSeededNetReturnsMap(helper: GameTestHelper) =
        TrackTests.updateAllPowerValuesOnTrackWithSeededNetReturnsMap(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun handleShapeUpdateForAirNeighborReturns(helper: GameTestHelper) =
        TrackTests.handleShapeUpdateForAirNeighborReturns(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun handleShapeUpdateMovingFlagSkipsRecursion(helper: GameTestHelper) =
        TrackTests.handleShapeUpdateMovingFlagSkipsRecursion(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun handleShapeUpdateRedstoneBlockNeighborSkipsConnectionRefresh(helper: GameTestHelper) =
        TrackTests.handleShapeUpdateRedstoneBlockNeighborSkipsConnectionRefresh(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getShapeReflectsWireFlags(helper: GameTestHelper) =
        TrackTests.getShapeReflectsWireFlags(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun emptyTrackShapeIsEmpty(helper: GameTestHelper) = TrackTests.emptyTrackShapeIsEmpty(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getCollisionShapeIsAlwaysEmpty(helper: GameTestHelper) =
        TrackTests.getCollisionShapeIsAlwaysEmpty(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getSignalForUnpoweredTrackIsZero(helper: GameTestHelper) =
        TrackTests.getSignalForUnpoweredTrackIsZero(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getDirectSignalForUnpoweredTrackIsZero(helper: GameTestHelper) =
        TrackTests.getDirectSignalForUnpoweredTrackIsZero(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun canSurviveAlwaysTrue(helper: GameTestHelper) = TrackTests.canSurviveAlwaysTrue(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun shouldCheckWeakPowerIsFalse(helper: GameTestHelper) =
        TrackTests.shouldCheckWeakPowerIsFalse(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun canConnectRedstoneFalseForUnconnectedTrack(helper: GameTestHelper) =
        TrackTests.canConnectRedstoneFalseForUnconnectedTrack(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun canConnectRedstoneFalseForNullSide(helper: GameTestHelper) =
        TrackTests.canConnectRedstoneFalseForNullSide(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun propagatesSkylightDownDependsOnWaterlogged(helper: GameTestHelper) =
        TrackTests.propagatesSkylightDownDependsOnWaterlogged(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun useShapeForLightOcclusionTrue(helper: GameTestHelper) =
        TrackTests.useShapeForLightOcclusionTrue(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getRenderShapeIsAnimatedEntityBlock(helper: GameTestHelper) =
        TrackTests.getRenderShapeIsAnimatedEntityBlock(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun notifyAdjacentRunsWithoutThrowing(helper: GameTestHelper) =
        TrackTests.notifyAdjacentRunsWithoutThrowing(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun dropListReturnsRedstoneDustMatchingWireCount(helper: GameTestHelper) =
        TrackTests.dropListReturnsRedstoneDustMatchingWireCount(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun dropListEmptyForNoWires(helper: GameTestHelper) = TrackTests.dropListEmptyForNoWires(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun isPathfindableAlwaysTrue(helper: GameTestHelper) =
        TrackTests.isPathfindableAlwaysTrue(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun neighborChangedDoesNotThrow(helper: GameTestHelper) =
        TrackTests.neighborChangedDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun onRemoveNotifiesAdjacentWhenReplaced(helper: GameTestHelper) =
        TrackTests.onRemoveNotifiesAdjacentWhenReplaced(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun modifySegmentsAddThenRemoveReturnsConsume(helper: GameTestHelper) =
        TrackTests.modifySegmentsAddThenRemoveReturnsConsume(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun modifySegmentsAddRemoveUntilEmptyRemovesBlock(helper: GameTestHelper) =
        TrackTests.modifySegmentsAddRemoveUntilEmptyRemovesBlock(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun toggleTraceWithNullPlayerDoesNotThrow(helper: GameTestHelper) =
        TrackTests.toggleTraceWithNullPlayerDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun toggleTraceWithMockPlayerDoesNotThrow(helper: GameTestHelper) =
        TrackTests.toggleTraceWithMockPlayerDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun connectionFlagAccessorsDoNotThrow(helper: GameTestHelper) =
        TrackTests.connectionFlagAccessorsDoNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackNetToStringProducesNonEmptyString(helper: GameTestHelper) =
        TrackTests.trackNetToStringProducesNonEmptyString(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getRedstonePowerZeroOnIsolatedTrack(helper: GameTestHelper) =
        TrackTests.getRedstonePowerZeroOnIsolatedTrack(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getRedstoneDustCountZeroForFreshTrack(helper: GameTestHelper) =
        TrackTests.getRedstoneDustCountZeroForFreshTrack(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getRedstoneDustCountMatchesWireFlags(helper: GameTestHelper) =
        TrackTests.getRedstoneDustCountMatchesWireFlags(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackHasDynamicDropListTrue(helper: GameTestHelper) =
        TrackTests.trackHasDynamicDropListTrue(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackAsItemReturnsRedstone(helper: GameTestHelper) =
        TrackTests.trackAsItemReturnsRedstone(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackIsSignalSourceTrue(helper: GameTestHelper) = TrackTests.trackIsSignalSourceTrue(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackUseWithoutItemWithPlayerDoesNotThrow(helper: GameTestHelper) =
        TrackTests.trackUseWithoutItemWithPlayerDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackUseItemOnWithDebugStickTogglesTrace(helper: GameTestHelper) =
        TrackTests.trackUseItemOnWithDebugStickTogglesTrace(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun trackCanBePlacedOnFaceOfPiston(helper: GameTestHelper) =
        TrackTests.trackCanBePlacedOnFaceOfPiston(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackCanBePlacedOnFaceOfHopper(helper: GameTestHelper) =
        TrackTests.trackCanBePlacedOnFaceOfHopper(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackNeighborChangedWithRedstoneBlockTriggersUpdate(helper: GameTestHelper) =
        TrackTests.trackNeighborChangedWithRedstoneBlockTriggersUpdate(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackGetRedstoneDustCountZeroWhenNoWiresLargeConfig(helper: GameTestHelper) =
        TrackTests.trackGetRedstoneDustCountZeroWhenNoWiresLargeConfig(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun trackReadnbtWithSflagsField(helper: GameTestHelper) =
        TrackTests.trackReadnbtWithSflagsField(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun trackGetNonWireSignalFromRedstoneBlock(helper: GameTestHelper) =
        TrackTests.trackGetNonWireSignalFromRedstoneBlock(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun verticalFaceTrackPowerPreservedAfterBottomSegmentRemoveAndReplace(helper: GameTestHelper) =
        TrackTests.verticalFaceTrackPowerPreservedAfterBottomSegmentRemoveAndReplace(helper)
}
