package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import wile.redstonepen.ModConstants
import wile.redstonepen.gametestcommon.ControlBoxTests

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
object ControlBoxGameTests {
    private const val TEMPLATE = "relay_activates_from_redstone"

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    fun controlBoxEvaluatesConstantProgram(helper: GameTestHelper) =
        ControlBoxTests.controlBoxEvaluatesConstantProgram(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    fun controlBoxRejectsInvalidProgram(helper: GameTestHelper) =
        ControlBoxTests.controlBoxRejectsInvalidProgram(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun writenbtRoundTripsCodeAndSymbols(helper: GameTestHelper) =
        ControlBoxTests.writenbtRoundTripsCodeAndSymbols(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun readnbtRestoresCodeAndOutputData(helper: GameTestHelper) =
        ControlBoxTests.readnbtRestoresCodeAndOutputData(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun readnbtPreservesSymbolMap(helper: GameTestHelper) =
        ControlBoxTests.readnbtPreservesSymbolMap(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun initiallyDisabled(helper: GameTestHelper) = ControlBoxTests.initiallyDisabled(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setEnabledTrueFlipsState(helper: GameTestHelper) =
        ControlBoxTests.setEnabledTrueFlipsState(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setEnabledIdempotent(helper: GameTestHelper) = ControlBoxTests.setEnabledIdempotent(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun tickOnDisabledBoxClearsOutput(helper: GameTestHelper) =
        ControlBoxTests.tickOnDisabledBoxClearsOutput(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun tickWithInvalidCodeDoesNotThrow(helper: GameTestHelper) =
        ControlBoxTests.tickWithInvalidCodeDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun traceToggleFlipsFlag(helper: GameTestHelper) = ControlBoxTests.traceToggleFlipsFlag(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getNameFallsBackToBlockTranslationKey(helper: GameTestHelper) =
        ControlBoxTests.getNameFallsBackToBlockTranslationKey(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setCustomNameStoresAndReturnsIt(helper: GameTestHelper) =
        ControlBoxTests.setCustomNameStoresAndReturnsIt(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun onServerPacketReceivedAppliesCodeFromNbt(helper: GameTestHelper) =
        ControlBoxTests.onServerPacketReceivedAppliesCodeFromNbt(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun getSignalReturnsComputedOutput(helper: GameTestHelper) =
        ControlBoxTests.getSignalReturnsComputedOutput(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun getDirectSignalMatchesGetSignal(helper: GameTestHelper) =
        ControlBoxTests.getDirectSignalMatchesGetSignal(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun dropListWithCodeSavesNbt(helper: GameTestHelper) =
        ControlBoxTests.dropListWithCodeSavesNbt(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun dropListWithNoCodeHasNoNbt(helper: GameTestHelper) =
        ControlBoxTests.dropListWithNoCodeHasNoNbt(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setPlacedByWithNbtRestoresCode(helper: GameTestHelper) =
        ControlBoxTests.setPlacedByWithNbtRestoresCode(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setPlacedByWithEmptyNbtIsNoOp(helper: GameTestHelper) =
        ControlBoxTests.setPlacedByWithEmptyNbtIsNoOp(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun updateWithNullFromPosResetsTickTimer(helper: GameTestHelper) =
        ControlBoxTests.updateWithNullFromPosResetsTickTimer(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun updateWithNeighborPosTriggersSideScan(helper: GameTestHelper) =
        ControlBoxTests.updateWithNeighborPosTriggersSideScan(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun isBlockEntityTickingAlwaysTrue(helper: GameTestHelper) =
        ControlBoxTests.isBlockEntityTickingAlwaysTrue(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun defsPortNamesHasSixEntries(helper: GameTestHelper) =
        ControlBoxTests.defsPortNamesHasSixEntries(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun getDisplayNameReturnsNonNull(helper: GameTestHelper) =
        ControlBoxTests.getDisplayNameReturnsNonNull(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun createMenuReturnsNonNull(helper: GameTestHelper) =
        ControlBoxTests.createMenuReturnsNonNull(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun tickWithTraceEnabledCoversTracePaths(helper: GameTestHelper) =
        ControlBoxTests.tickWithTraceEnabledCoversTracePaths(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun tickWithTickrateSymbolSetsInterval(helper: GameTestHelper) =
        ControlBoxTests.tickWithTickrateSymbolSetsInterval(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setEnabledFalseFromEnabledClearsSymbols(helper: GameTestHelper) =
        ControlBoxTests.setEnabledFalseFromEnabledClearsSymbols(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun setRcaPlayerUuidNonNullDoesNotThrow(helper: GameTestHelper) =
        ControlBoxTests.setRcaPlayerUuidNonNullDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun signalUpdateRisingEdgeSetsIntredge(helper: GameTestHelper) =
        ControlBoxTests.signalUpdateRisingEdgeSetsIntredge(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun signalUpdateFallingEdgeSetsIntfedge(helper: GameTestHelper) =
        ControlBoxTests.signalUpdateFallingEdgeSetsIntfedge(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun useItemOnWithDebugStickTogglesTrace(helper: GameTestHelper) =
        ControlBoxTests.useItemOnWithDebugStickTogglesTrace(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun controlBoxBuiltinFunctionsExercised(helper: GameTestHelper) =
        ControlBoxTests.controlBoxBuiltinFunctionsExercised(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun controlBoxCounterFunctionsExercised(helper: GameTestHelper) =
        ControlBoxTests.controlBoxCounterFunctionsExercised(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun controlBoxTimerFunctionsExercised(helper: GameTestHelper) =
        ControlBoxTests.controlBoxTimerFunctionsExercised(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun controlBoxRemainingTimerVariantsExercised(helper: GameTestHelper) =
        ControlBoxTests.controlBoxRemainingTimerVariantsExercised(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    fun controlBoxTimerEdgeCasesDoNotThrow(helper: GameTestHelper) =
        ControlBoxTests.controlBoxTimerEdgeCasesDoNotThrow(helper)

    @JvmStatic
    @GameTest(template = TEMPLATE, timeoutTicks = 5)
    fun controlBoxSetCodeSameCodeSkipsReparse(helper: GameTestHelper) =
        ControlBoxTests.controlBoxSetCodeSameCodeSkipsReparse(helper)
}
