package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import wile.redstonepen.ModConstants
import wile.redstonepen.gametestcommon.BasicLeverButtonTests

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
object BasicLeverButtonGameTests {
    private const val EMPTY = "relay_activates_from_redstone"

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 10)
    fun leverUseWithoutItemTogglesPoweredFalseToTrue(helper: GameTestHelper) =
        BasicLeverButtonTests.leverUseWithoutItemTogglesPoweredFalseToTrue(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 10)
    fun leverUseTwiceReturnsToUnpowered(helper: GameTestHelper) =
        BasicLeverButtonTests.leverUseTwiceReturnsToUnpowered(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 10)
    fun buttonUseWithoutItemPressesAndPowers(helper: GameTestHelper) =
        BasicLeverButtonTests.buttonUseWithoutItemPressesAndPowers(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 10)
    fun buttonUseOnPoweredButtonReturnsConsume(helper: GameTestHelper) =
        BasicLeverButtonTests.buttonUseOnPoweredButtonReturnsConsume(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 30)
    fun pulseButtonRevertsAfterShortInterval(helper: GameTestHelper) =
        BasicLeverButtonTests.pulseButtonRevertsAfterShortInterval(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 10)
    fun gaugeReadsZeroWhenNoSignal(helper: GameTestHelper) =
        BasicLeverButtonTests.gaugeReadsZeroWhenNoSignal(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 10)
    fun gaugeReadsSignalFromAdjacentRedstoneBlock(helper: GameTestHelper) =
        BasicLeverButtonTests.gaugeReadsSignalFromAdjacentRedstoneBlock(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 5)
    fun gaugeShouldCheckWeakPowerReturnsFalse(helper: GameTestHelper) =
        BasicLeverButtonTests.gaugeShouldCheckWeakPowerReturnsFalse(helper)

    @JvmStatic
    @GameTest(template = EMPTY, timeoutTicks = 5)
    fun gaugeGetStateForPlacementReturnsNonNull(helper: GameTestHelper) =
        BasicLeverButtonTests.gaugeGetStateForPlacementReturnsNonNull(helper)
}
