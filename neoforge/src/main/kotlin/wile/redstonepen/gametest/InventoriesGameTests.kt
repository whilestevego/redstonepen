package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import wile.redstonepen.ModConstants
import wile.redstonepen.gametestcommon.InventoriesTests

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
object InventoriesGameTests {
    private const val RELAY_TEMPLATE = "relay_activates_from_redstone"

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun inventoryRangeInsertBetweenMatchedSlots(helper: GameTestHelper) =
        InventoriesTests.inventoryRangeInsertBetweenMatchedSlots(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun inventoryRangeInsertBeforeAfterMatchedSlot(helper: GameTestHelper) =
        InventoriesTests.inventoryRangeInsertBeforeAfterMatchedSlot(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun inventoryRangeInsertSimulateWithPartialStack(helper: GameTestHelper) =
        InventoriesTests.inventoryRangeInsertSimulateWithPartialStack(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun inventoryRangeInsertSimulateNoRoomReturnsInput(helper: GameTestHelper) =
        InventoriesTests.inventoryRangeInsertSimulateNoRoomReturnsInput(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun inventoryRangeExtractMultipleMatchingSlots(helper: GameTestHelper) =
        InventoriesTests.inventoryRangeExtractMultipleMatchingSlots(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun inventoryRangeExtractWithSimulateTrue(helper: GameTestHelper) =
        InventoriesTests.inventoryRangeExtractWithSimulateTrue(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun inventoryRangeInsertFillupOnlyStopsAtOnlyFillup(helper: GameTestHelper) =
        InventoriesTests.inventoryRangeInsertFillupOnlyStopsAtOnlyFillup(helper)
}
