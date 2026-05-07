package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.gametestcommon.ControlBoxTests;

public class ControlBoxGameTests
{
  private static final String TEMPLATE = "redstonepen:relay_activates_from_redstone";

  public ControlBoxGameTests() {}

  @GameTest(template = TEMPLATE, timeoutTicks = 20)
  public static void controlBoxEvaluatesConstantProgram(GameTestHelper helper)
  { ControlBoxTests.controlBoxEvaluatesConstantProgram(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 20)
  public static void controlBoxRejectsInvalidProgram(GameTestHelper helper)
  { ControlBoxTests.controlBoxRejectsInvalidProgram(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void writenbtRoundTripsCodeAndSymbols(GameTestHelper helper)
  { ControlBoxTests.writenbtRoundTripsCodeAndSymbols(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void readnbtRestoresCodeAndOutputData(GameTestHelper helper)
  { ControlBoxTests.readnbtRestoresCodeAndOutputData(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void readnbtPreservesSymbolMap(GameTestHelper helper)
  { ControlBoxTests.readnbtPreservesSymbolMap(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void initiallyDisabled(GameTestHelper helper)
  { ControlBoxTests.initiallyDisabled(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setEnabledTrueFlipsState(GameTestHelper helper)
  { ControlBoxTests.setEnabledTrueFlipsState(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setEnabledIdempotent(GameTestHelper helper)
  { ControlBoxTests.setEnabledIdempotent(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void tickOnDisabledBoxClearsOutput(GameTestHelper helper)
  { ControlBoxTests.tickOnDisabledBoxClearsOutput(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void tickWithInvalidCodeDoesNotThrow(GameTestHelper helper)
  { ControlBoxTests.tickWithInvalidCodeDoesNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void traceToggleFlipsFlag(GameTestHelper helper)
  { ControlBoxTests.traceToggleFlipsFlag(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getNameFallsBackToBlockTranslationKey(GameTestHelper helper)
  { ControlBoxTests.getNameFallsBackToBlockTranslationKey(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setCustomNameStoresAndReturnsIt(GameTestHelper helper)
  { ControlBoxTests.setCustomNameStoresAndReturnsIt(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void onServerPacketReceivedAppliesCodeFromNbt(GameTestHelper helper)
  { ControlBoxTests.onServerPacketReceivedAppliesCodeFromNbt(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void getSignalReturnsComputedOutput(GameTestHelper helper)
  { ControlBoxTests.getSignalReturnsComputedOutput(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void getDirectSignalMatchesGetSignal(GameTestHelper helper)
  { ControlBoxTests.getDirectSignalMatchesGetSignal(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dropListWithCodeSavesNbt(GameTestHelper helper)
  { ControlBoxTests.dropListWithCodeSavesNbt(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dropListWithNoCodeHasNoNbt(GameTestHelper helper)
  { ControlBoxTests.dropListWithNoCodeHasNoNbt(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setPlacedByWithNbtRestoresCode(GameTestHelper helper)
  { ControlBoxTests.setPlacedByWithNbtRestoresCode(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setPlacedByWithEmptyNbtIsNoOp(GameTestHelper helper)
  { ControlBoxTests.setPlacedByWithEmptyNbtIsNoOp(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void updateWithNullFromPosResetsTickTimer(GameTestHelper helper)
  { ControlBoxTests.updateWithNullFromPosResetsTickTimer(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void updateWithNeighborPosTriggersSideScan(GameTestHelper helper)
  { ControlBoxTests.updateWithNeighborPosTriggersSideScan(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void isBlockEntityTickingAlwaysTrue(GameTestHelper helper)
  { ControlBoxTests.isBlockEntityTickingAlwaysTrue(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void defsDefaultConstructorIsCallable(GameTestHelper helper)
  { ControlBoxTests.defsDefaultConstructorIsCallable(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void getDisplayNameReturnsNonNull(GameTestHelper helper)
  { ControlBoxTests.getDisplayNameReturnsNonNull(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void createMenuReturnsNonNull(GameTestHelper helper)
  { ControlBoxTests.createMenuReturnsNonNull(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void tickWithTraceEnabledCoversTracePaths(GameTestHelper helper)
  { ControlBoxTests.tickWithTraceEnabledCoversTracePaths(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void tickWithTickrateSymbolSetsInterval(GameTestHelper helper)
  { ControlBoxTests.tickWithTickrateSymbolSetsInterval(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setEnabledFalseFromEnabledClearsSymbols(GameTestHelper helper)
  { ControlBoxTests.setEnabledFalseFromEnabledClearsSymbols(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void setRcaPlayerUuidNonNullDoesNotThrow(GameTestHelper helper)
  { ControlBoxTests.setRcaPlayerUuidNonNullDoesNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void signalUpdateRisingEdgeSetsIntredge(GameTestHelper helper)
  { ControlBoxTests.signalUpdateRisingEdgeSetsIntredge(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void signalUpdateFallingEdgeSetsIntfedge(GameTestHelper helper)
  { ControlBoxTests.signalUpdateFallingEdgeSetsIntfedge(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void useItemOnWithDebugStickTogglesTrace(GameTestHelper helper)
  { ControlBoxTests.useItemOnWithDebugStickTogglesTrace(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void controlBoxBuiltinFunctionsExercised(GameTestHelper helper)
  { ControlBoxTests.controlBoxBuiltinFunctionsExercised(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void controlBoxCounterFunctionsExercised(GameTestHelper helper)
  { ControlBoxTests.controlBoxCounterFunctionsExercised(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void controlBoxTimerFunctionsExercised(GameTestHelper helper)
  { ControlBoxTests.controlBoxTimerFunctionsExercised(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void controlBoxRemainingTimerVariantsExercised(GameTestHelper helper)
  { ControlBoxTests.controlBoxRemainingTimerVariantsExercised(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 10)
  public static void controlBoxTimerEdgeCasesDoNotThrow(GameTestHelper helper)
  { ControlBoxTests.controlBoxTimerEdgeCasesDoNotThrow(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void controlBoxSetCodeSameCodeSkipsReparse(GameTestHelper helper)
  { ControlBoxTests.controlBoxSetCodeSameCodeSkipsReparse(helper); }
}
