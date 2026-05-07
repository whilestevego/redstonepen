package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.gametestcommon.TrackTests;

public class TrackGameTests
{
  private static final String TEMPLATE = "redstonepen:relay_activates_from_redstone";

  public TrackGameTests() {}

  @GameTest(template = TEMPLATE, timeoutTicks = 20)
  public static void trackStoresSeededPowerRoute(GameTestHelper helper)
  { TrackTests.trackStoresSeededPowerRoute(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 20)
  public static void trackReplacesSeededRouteWhenPowerClears(GameTestHelper helper)
  { TrackTests.trackReplacesSeededRouteWhenPowerClears(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void writenbtSyncOmitsNetsList(GameTestHelper helper)
  { TrackTests.writenbtSyncOmitsNetsList(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void writenbtFullIncludesNetsList(GameTestHelper helper)
  { TrackTests.writenbtFullIncludesNetsList(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void readnbtRestoresStateFlagsAndNets(GameTestHelper helper)
  { TrackTests.readnbtRestoresStateFlagsAndNets(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void readnbtAcceptsCorruptNetsListWithoutThrowing(GameTestHelper helper)
  { TrackTests.readnbtAcceptsCorruptNetsListWithoutThrowing(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void addWireFlagsRecordsOnlyNewBits(GameTestHelper helper)
  { TrackTests.addWireFlagsRecordsOnlyNewBits(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getWireFlagsReadsIndividualBits(GameTestHelper helper)
  { TrackTests.getWireFlagsReadsIndividualBits(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setSidePowerAndGetSidePowerRoundTrip(GameTestHelper helper)
  { TrackTests.setSidePowerAndGetSidePowerRoundTrip(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void hasVanillaRedstoneConnectionReadsBitsAndConnectorMask(GameTestHelper helper)
  { TrackTests.hasVanillaRedstoneConnectionReadsBitsAndConnectorMask(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void updateAllPowerValuesOnIsolatedTrackReturnsMap(GameTestHelper helper)
  { TrackTests.updateAllPowerValuesOnIsolatedTrackReturnsMap(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void updateAllPowerValuesOnTrackWithSeededNetReturnsMap(GameTestHelper helper)
  { TrackTests.updateAllPowerValuesOnTrackWithSeededNetReturnsMap(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateForAirNeighborReturns(GameTestHelper helper)
  { TrackTests.handleShapeUpdateForAirNeighborReturns(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateMovingFlagSkipsRecursion(GameTestHelper helper)
  { TrackTests.handleShapeUpdateMovingFlagSkipsRecursion(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateRedstoneBlockNeighborSkipsConnectionRefresh(GameTestHelper helper)
  { TrackTests.handleShapeUpdateRedstoneBlockNeighborSkipsConnectionRefresh(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getShapeReflectsWireFlags(GameTestHelper helper)
  { TrackTests.getShapeReflectsWireFlags(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void emptyTrackShapeIsEmpty(GameTestHelper helper)
  { TrackTests.emptyTrackShapeIsEmpty(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getCollisionShapeIsAlwaysEmpty(GameTestHelper helper)
  { TrackTests.getCollisionShapeIsAlwaysEmpty(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getSignalForUnpoweredTrackIsZero(GameTestHelper helper)
  { TrackTests.getSignalForUnpoweredTrackIsZero(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getDirectSignalForUnpoweredTrackIsZero(GameTestHelper helper)
  { TrackTests.getDirectSignalForUnpoweredTrackIsZero(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void canSurviveAlwaysTrue(GameTestHelper helper)
  { TrackTests.canSurviveAlwaysTrue(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void shouldCheckWeakPowerIsFalse(GameTestHelper helper)
  { TrackTests.shouldCheckWeakPowerIsFalse(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void canConnectRedstoneFalseForUnconnectedTrack(GameTestHelper helper)
  { TrackTests.canConnectRedstoneFalseForUnconnectedTrack(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void canConnectRedstoneFalseForNullSide(GameTestHelper helper)
  { TrackTests.canConnectRedstoneFalseForNullSide(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void propagatesSkylightDownDependsOnWaterlogged(GameTestHelper helper)
  { TrackTests.propagatesSkylightDownDependsOnWaterlogged(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void useShapeForLightOcclusionTrue(GameTestHelper helper)
  { TrackTests.useShapeForLightOcclusionTrue(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getRenderShapeIsAnimatedEntityBlock(GameTestHelper helper)
  { TrackTests.getRenderShapeIsAnimatedEntityBlock(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void notifyAdjacentRunsWithoutThrowing(GameTestHelper helper)
  { TrackTests.notifyAdjacentRunsWithoutThrowing(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dropListReturnsRedstoneDustMatchingWireCount(GameTestHelper helper)
  { TrackTests.dropListReturnsRedstoneDustMatchingWireCount(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dropListEmptyForNoWires(GameTestHelper helper)
  { TrackTests.dropListEmptyForNoWires(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void isPathfindableAlwaysTrue(GameTestHelper helper)
  { TrackTests.isPathfindableAlwaysTrue(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void neighborChangedDoesNotThrow(GameTestHelper helper)
  { TrackTests.neighborChangedDoesNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void onRemoveNotifiesAdjacentWhenReplaced(GameTestHelper helper)
  { TrackTests.onRemoveNotifiesAdjacentWhenReplaced(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void modifySegmentsAddThenRemoveReturnsConsume(GameTestHelper helper)
  { TrackTests.modifySegmentsAddThenRemoveReturnsConsume(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void modifySegmentsAddRemoveUntilEmptyRemovesBlock(GameTestHelper helper)
  { TrackTests.modifySegmentsAddRemoveUntilEmptyRemovesBlock(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void toggleTraceWithNullPlayerDoesNotThrow(GameTestHelper helper)
  { TrackTests.toggleTraceWithNullPlayerDoesNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void toggleTraceWithMockPlayerDoesNotThrow(GameTestHelper helper)
  { TrackTests.toggleTraceWithMockPlayerDoesNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void connectionFlagAccessorsDoNotThrow(GameTestHelper helper)
  { TrackTests.connectionFlagAccessorsDoNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackNetToStringProducesNonEmptyString(GameTestHelper helper)
  { TrackTests.trackNetToStringProducesNonEmptyString(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getRedstonePowerZeroOnIsolatedTrack(GameTestHelper helper)
  { TrackTests.getRedstonePowerZeroOnIsolatedTrack(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getRedstoneDustCountZeroForFreshTrack(GameTestHelper helper)
  { TrackTests.getRedstoneDustCountZeroForFreshTrack(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getRedstoneDustCountMatchesWireFlags(GameTestHelper helper)
  { TrackTests.getRedstoneDustCountMatchesWireFlags(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackHasDynamicDropListTrue(GameTestHelper helper)
  { TrackTests.trackHasDynamicDropListTrue(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackAsItemReturnsRedstone(GameTestHelper helper)
  { TrackTests.trackAsItemReturnsRedstone(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackIsSignalSourceTrue(GameTestHelper helper)
  { TrackTests.trackIsSignalSourceTrue(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackUseWithoutItemWithPlayerDoesNotThrow(GameTestHelper helper)
  { TrackTests.trackUseWithoutItemWithPlayerDoesNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackUseItemOnWithDebugStickTogglesTrace(GameTestHelper helper)
  { TrackTests.trackUseItemOnWithDebugStickTogglesTrace(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void trackCanBePlacedOnFaceOfPiston(GameTestHelper helper)
  { TrackTests.trackCanBePlacedOnFaceOfPiston(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackCanBePlacedOnFaceOfHopper(GameTestHelper helper)
  { TrackTests.trackCanBePlacedOnFaceOfHopper(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackNeighborChangedWithRedstoneBlockTriggersUpdate(GameTestHelper helper)
  { TrackTests.trackNeighborChangedWithRedstoneBlockTriggersUpdate(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackGetRedstoneDustCountZeroWhenNoWiresLargeConfig(GameTestHelper helper)
  { TrackTests.trackGetRedstoneDustCountZeroWhenNoWiresLargeConfig(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void trackReadnbtWithSflagsField(GameTestHelper helper)
  { TrackTests.trackReadnbtWithSflagsField(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void trackGetNonWireSignalFromRedstoneBlock(GameTestHelper helper)
  { TrackTests.trackGetNonWireSignalFromRedstoneBlock(helper); }
}
