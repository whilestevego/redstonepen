package wile.redstonepen.gametestcommon

import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import wile.redstonepen.util.Inventories

object InventoriesTests {

    @JvmStatic
    fun inventoryRangeInsertBetweenMatchedSlots(helper: GameTestHelper) {
        val container = SimpleContainer(5)
        container.setItem(1, ItemStack(Items.REDSTONE, 64))
        container.setItem(3, ItemStack(Items.REDSTONE, 64))
        val ir = Inventories.InventoryRange(container)
        val remaining = ir.insert(ItemStack(Items.REDSTONE, 1))
        if (!remaining.isEmpty) {
            helper.fail(
                "expected full insertion into slot 2 between two full match slots, got remaining=${remaining.count}"
            )
        }
        helper.succeed()
    }

    @JvmStatic
    fun inventoryRangeInsertBeforeAfterMatchedSlot(helper: GameTestHelper) {
        val container = SimpleContainer(5)
        container.setItem(1, ItemStack(Items.REDSTONE, 64))
        container.setItem(2, ItemStack(Items.STONE, 1))
        container.setItem(3, ItemStack(Items.REDSTONE, 64))
        val ir = Inventories.InventoryRange(container)
        val remaining = ir.insert(ItemStack(Items.REDSTONE, 1))
        if (!remaining.isEmpty) {
            helper.fail(
                "expected insertion before match slot via before/after path, got remaining=${remaining.count}"
            )
        }
        helper.succeed()
    }

    @JvmStatic
    fun inventoryRangeInsertSimulateWithPartialStack(helper: GameTestHelper) {
        val container = SimpleContainer(3)
        container.setItem(0, ItemStack(Items.REDSTONE, 60))
        val ir = Inventories.InventoryRange(container)
        val remaining = ir.insert(ItemStack(Items.REDSTONE, 3), true)
        if (!remaining.isEmpty) {
            helper.fail(
                "simulate insert into partial stack must return empty, got ${remaining.count}"
            )
        }
        helper.succeed()
    }

    @JvmStatic
    fun inventoryRangeInsertSimulateNoRoomReturnsInput(helper: GameTestHelper) {
        val container = SimpleContainer(2)
        container.setItem(0, ItemStack(Items.REDSTONE, 64))
        container.setItem(1, ItemStack(Items.STICK, 64))
        val ir = Inventories.InventoryRange(container)
        val remaining = ir.insert(ItemStack(Items.REDSTONE, 5), true)
        if (remaining.isEmpty) {
            helper.fail("simulate with no room must return the input stack, got empty")
        }
        helper.succeed()
    }

    @JvmStatic
    fun inventoryRangeExtractMultipleMatchingSlots(helper: GameTestHelper) {
        val container = SimpleContainer(3)
        container.setItem(0, ItemStack(Items.REDSTONE, 10))
        container.setItem(1, ItemStack(Items.REDSTONE, 10))
        val ir = Inventories.InventoryRange(container)
        val extracted = ir.extract(15)
        if (extracted.count != 15) {
            helper.fail("expected 15 extracted from two slots, got ${extracted.count}")
        }
        helper.succeed()
    }

    @JvmStatic
    fun inventoryRangeExtractWithSimulateTrue(helper: GameTestHelper) {
        val container = SimpleContainer(2)
        container.setItem(0, ItemStack(Items.REDSTONE, 20))
        val ir = Inventories.InventoryRange(container)
        val extracted = ir.extract(10, false, true)
        if (extracted.count != 10) {
            helper.fail("simulate extract must return the requested amount, got ${extracted.count}")
        }
        if (container.getItem(0).count != 20) {
            helper.fail(
                "simulate extract must not modify the container, got ${container.getItem(0).count}"
            )
        }
        helper.succeed()
    }

    @JvmStatic
    fun inventoryRangeInsertFillupOnlyStopsAtOnlyFillup(helper: GameTestHelper) {
        val container = SimpleContainer(3)
        container.setItem(0, ItemStack(Items.STICK, 1))
        container.setItem(1, ItemStack(Items.STICK, 1))
        val ir = Inventories.InventoryRange(container)
        val remaining = ir.insert(ItemStack(Items.REDSTONE, 5), true, 0, false, false)
        if (remaining.isEmpty) {
            helper.fail("only_fillup with no matching slot must return the input, got empty")
        }
        helper.succeed()
    }
}
