package wile.redstonepen.util

import java.util.NoSuchElementException
import java.util.Optional
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap

class InventoryRangeTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    private fun container(size: Int) = SimpleContainer(size)

    private fun filled(size: Int, redstone: Int) =
        SimpleContainer(size).also { c ->
            for (i in 0 until size) c.setItem(i, ItemStack(Items.REDSTONE, redstone))
        }

    @Nested
    inner class Construction {
        @Test
        fun constructorClampsOffsetAndSizeToContainerBounds() {
            val c = container(9)
            val r = Inventories.InventoryRange(c, 100, 100, 1)
            assertTrue(r.size() <= 9)
            assertTrue(r.offset() >= 0)
            assertEquals(c, r.inventory())
        }

        @Test
        fun singleArgConstructorWrapsEntireContainer() {
            val c = container(7)
            val r = Inventories.InventoryRange(c)
            assertEquals(7, r.size())
            assertEquals(0, r.offset())
            assertEquals(7, r.containerSize)
        }

        @Test
        fun twoArgConstructorDefaultsRowsToOne() {
            val r = Inventories.InventoryRange(container(9), 0, 9)
            assertEquals(9, r.size())
        }

        @Test
        fun getAndSetUseTheRangeOffset() {
            val c = container(9)
            val r = Inventories.InventoryRange(c, 3, 4, 1)
            r.set(0, ItemStack(Items.REDSTONE, 2))
            assertEquals(Items.REDSTONE, c.getItem(3).item)
            assertEquals(2, r.get(0).count)
        }

        @Test
        fun maxStackSizeIsBoundedAndAtLeastOne() {
            val r = Inventories.InventoryRange(container(4))
            r.setMaxStackSize(0)
            assertTrue(r.maxStackSize >= 1)
            r.setMaxStackSize(8)
            assertEquals(8, r.maxStackSize)
        }

        @Test
        fun validatorCanRejectItems() {
            val r = Inventories.InventoryRange(container(4))
            r.setValidator { _, stack -> stack.`is`(Items.REDSTONE) }
            assertNotNull(r.getValidator())
            assertTrue(r.canPlaceItem(0, ItemStack(Items.REDSTONE)))
            assertFalse(r.canPlaceItem(0, ItemStack(Items.COAL)))
        }

        @Test
        fun multiRangeInsertConsumesUntilEmpty() {
            val a = container(1)
            a.setItem(0, ItemStack(Items.REDSTONE, 60))
            val b = container(2)
            val ranges = arrayOf(Inventories.InventoryRange(a), Inventories.InventoryRange(b))
            val remaining = Inventories.insert(ranges, ItemStack(Items.REDSTONE, 10))
            assertTrue(remaining.isEmpty)
        }
    }

    @Nested
    inner class ContainerDelegates {
        @Test
        fun clearContentEmptiesEverySlotInRange() {
            val r = Inventories.InventoryRange(filled(4, 2))
            r.clearContent()
            assertTrue(r.isEmpty)
        }

        @Test
        fun removeItemNoUpdateRemovesAndReturnsStack() {
            val c = filled(2, 4)
            val r = Inventories.InventoryRange(c)
            val out = r.removeItemNoUpdate(0)
            assertEquals(4, out.count)
            assertTrue(r.getItem(0).isEmpty)
        }

        @Test
        fun removeItemSplitsStack() {
            val c = filled(2, 5)
            val r = Inventories.InventoryRange(c)
            val out = r.removeItem(0, 3)
            assertEquals(3, out.count)
            assertEquals(2, r.getItem(0).count)
        }

        @Test
        fun stillValidDelegatesToInventory() {
            val r = Inventories.InventoryRange(container(1))
            assertTrue(r.stillValid(null))
        }

        @Test
        fun startAndStopOpenAreNoOps() {
            val r = Inventories.InventoryRange(container(1))
            r.startOpen(null)
            r.stopOpen(null)
            r.setChanged()
        }
    }

    @Nested
    inner class Iteration {
        @Test
        fun iterateStopsWhenPredicateMatches() {
            val c = container(3)
            c.setItem(1, ItemStack(Items.COAL))
            val r = Inventories.InventoryRange(c)
            val seen = mutableListOf<Int>()
            val matched = r.iterate { i, s ->
                seen.add(i)
                s.`is`(Items.COAL)
            }
            assertTrue(matched)
            assertEquals(listOf(0, 1), seen)
        }

        @Test
        fun iterateReturnsFalseWhenNothingMatches() {
            val r = Inventories.InventoryRange(filled(3, 1))
            assertFalse(r.iterate { _, _ -> false })
        }

        @Test
        fun containsAndIndexOfFindMatchingStack() {
            val c = container(3)
            c.setItem(2, ItemStack(Items.REDSTONE, 1))
            val r = Inventories.InventoryRange(c)
            assertTrue(r.contains(ItemStack(Items.REDSTONE, 1)))
            assertEquals(2, r.indexOf(ItemStack(Items.REDSTONE, 1)))
            assertEquals(-1, r.indexOf(ItemStack(Items.COAL, 1)))
        }

        @Test
        fun findReturnsFirstPresentResult() {
            val c = container(3)
            c.setItem(0, ItemStack(Items.COAL))
            c.setItem(2, ItemStack(Items.REDSTONE))
            val r = Inventories.InventoryRange(c)
            val idx = r.find { i, s ->
                if (s.`is`(Items.REDSTONE)) Optional.of(i) else Optional.empty()
            }
            assertEquals(Optional.of(2), idx)
        }

        @Test
        fun findReturnsEmptyWhenNothingMatches() {
            val r = Inventories.InventoryRange(container(2))
            assertTrue(r.find { _, _ -> Optional.empty<Int>() }.isEmpty)
        }

        @Test
        fun collectGathersAllPresentResults() {
            val c = container(4)
            c.setItem(0, ItemStack(Items.REDSTONE))
            c.setItem(2, ItemStack(Items.REDSTONE))
            val r = Inventories.InventoryRange(c)
            val hits = r.collect { i, s ->
                if (s.`is`(Items.REDSTONE)) Optional.of(i) else Optional.empty()
            }
            assertEquals(listOf(0, 2), hits)
        }

        @Test
        fun streamYieldsAllSlotsIncludingEmpty() {
            val c = container(3)
            c.setItem(1, ItemStack(Items.REDSTONE, 4))
            val r = Inventories.InventoryRange(c)
            assertEquals(3, r.stream().count())
        }

        @Test
        fun iteratorWalksRangeAndThrowsAtEnd() {
            val r = Inventories.InventoryRange(filled(2, 1))
            val it = r.iterator()
            assertTrue(it.hasNext())
            it.next()
            assertTrue(it.hasNext())
            it.next()
            assertFalse(it.hasNext())
            org.junit.jupiter.api.assertThrows<NoSuchElementException> { it.next() }
        }
    }

    @Nested
    inner class MatchCounts {
        @Test
        fun stackMatchCountAndTotalCount() {
            val c = container(4)
            c.setItem(0, ItemStack(Items.REDSTONE, 3))
            c.setItem(1, ItemStack(Items.COAL, 5))
            c.setItem(3, ItemStack(Items.REDSTONE, 7))
            val r = Inventories.InventoryRange(c)
            assertEquals(2, r.stackMatchCount(ItemStack(Items.REDSTONE, 3)))
            assertEquals(10, r.totalMatchingItemCount(ItemStack(Items.REDSTONE, 3)))
        }
    }

    @Nested
    inner class Insert {
        @Test
        fun insertEmptyStackReturnsEmpty() {
            val r = Inventories.InventoryRange(container(3))
            assertTrue(r.insert(ItemStack.EMPTY).isEmpty)
        }

        @Test
        fun insertFillsExistingMatchingStackBeforeAnyEmptySlot() {
            val c = container(3)
            c.setItem(2, ItemStack(Items.REDSTONE, 10))
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(ItemStack(Items.REDSTONE, 5))
            assertTrue(remaining.isEmpty)
            assertEquals(15, c.getItem(2).count)
            assertTrue(c.getItem(0).isEmpty)
        }

        @Test
        fun insertReturnsOverflowWhenNoCapacity() {
            val c = container(1)
            c.setItem(0, ItemStack(Items.REDSTONE, 64))
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(ItemStack(Items.REDSTONE, 5))
            assertEquals(5, remaining.count)
        }

        @Test
        fun insertOnlyFillupSkipsEmptySlots() {
            val c = container(3)
            c.setItem(0, ItemStack(Items.REDSTONE, 60))
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(ItemStack(Items.REDSTONE, 10), true, 0, false, false)
            assertEquals(64, c.getItem(0).count)
            assertEquals(6, remaining.count)
            assertTrue(c.getItem(1).isEmpty)
        }

        @Test
        fun insertUsesEmptySlotWhenNoMatchExists() {
            val c = container(3)
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(ItemStack(Items.REDSTONE, 4))
            assertTrue(remaining.isEmpty)
            assertEquals(Items.REDSTONE, c.getItem(0).item)
            assertEquals(4, c.getItem(0).count)
        }

        @Test
        fun insertReverseFillsFromTheEnd() {
            val c = container(3)
            val r = Inventories.InventoryRange(c)
            r.insert(ItemStack(Items.REDSTONE, 4), false, 0, true, true)
            assertEquals(Items.REDSTONE, c.getItem(2).item)
        }

        @Test
        fun insertSimulateDoesNotMutate() {
            val c = container(2)
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(ItemStack(Items.REDSTONE, 4), true)
            assertTrue(remaining.isEmpty)
            assertTrue(c.getItem(0).isEmpty)
        }

        @Test
        fun insertSimulateReportsLeftoverWhenFull() {
            val c = container(1)
            c.setItem(0, ItemStack(Items.REDSTONE, 64))
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(ItemStack(Items.REDSTONE, 4), true)
            assertEquals(4, remaining.count)
        }

        @Test
        fun insertAtIndexFillsTargetSlotAndReturnsRemainder() {
            val c = container(2)
            c.setItem(0, ItemStack(Items.REDSTONE, 60))
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(0, ItemStack(Items.REDSTONE, 10))
            assertEquals(64, c.getItem(0).count)
            assertEquals(6, remaining.count)
        }

        @Test
        fun insertAtIndexIntoEmptyPlacesFullStack() {
            val c = container(2)
            val r = Inventories.InventoryRange(c)
            val remaining = r.insert(0, ItemStack(Items.REDSTONE, 10))
            assertTrue(remaining.isEmpty)
            assertEquals(10, c.getItem(0).count)
        }

        @Test
        fun insertAtIndexIntoMismatchedSlotReturnsInputUnchanged() {
            val c = container(2)
            c.setItem(0, ItemStack(Items.COAL, 4))
            val r = Inventories.InventoryRange(c)
            val input = ItemStack(Items.REDSTONE, 3)
            val remaining = r.insert(0, input)
            assertEquals(3, remaining.count)
            assertEquals(Items.REDSTONE, remaining.item)
        }

        @Test
        fun insertAtIndexEmptyInputReturnsEmpty() {
            val r = Inventories.InventoryRange(container(1))
            assertTrue(r.insert(0, ItemStack.EMPTY).isEmpty)
        }
    }

    @Nested
    inner class Extract {
        @Test
        fun extractAmountFromEmptyRangeReturnsEmpty() {
            val r = Inventories.InventoryRange(container(3))
            assertTrue(r.extract(5).isEmpty)
        }

        @Test
        fun extractTakesFromFirstNonEmptyStack() {
            val c = container(3)
            c.setItem(1, ItemStack(Items.REDSTONE, 10))
            val r = Inventories.InventoryRange(c)
            val out = r.extract(4)
            assertEquals(4, out.count)
            assertEquals(6, c.getItem(1).count)
        }

        @Test
        fun extractAggregatesAcrossIdenticalStacks() {
            val c = container(3)
            c.setItem(0, ItemStack(Items.REDSTONE, 3))
            c.setItem(1, ItemStack(Items.COAL, 8))
            c.setItem(2, ItemStack(Items.REDSTONE, 2))
            val r = Inventories.InventoryRange(c)
            val out = r.extract(5)
            assertEquals(Items.REDSTONE, out.item)
            assertEquals(5, out.count)
            assertTrue(c.getItem(0).isEmpty)
            assertTrue(c.getItem(2).isEmpty)
            assertEquals(8, c.getItem(1).count)
        }

        @Test
        fun extractSimulateDoesNotMutate() {
            val c = container(2)
            c.setItem(0, ItemStack(Items.REDSTONE, 5))
            val r = Inventories.InventoryRange(c)
            val out = r.extract(2, false, true)
            assertEquals(2, out.count)
            assertEquals(5, c.getItem(0).count)
        }

        @Test
        fun extractByStackReturnsEmptyForEmptyRequest() {
            val r = Inventories.InventoryRange(filled(2, 4))
            assertTrue(r.extract(ItemStack.EMPTY).isEmpty)
        }

        @Test
        fun extractByStackPullsRequestedAmount() {
            val c = container(3)
            c.setItem(0, ItemStack(Items.REDSTONE, 4))
            c.setItem(2, ItemStack(Items.REDSTONE, 3))
            val r = Inventories.InventoryRange(c)
            val out = r.extract(ItemStack(Items.REDSTONE, 5))
            assertEquals(5, out.count)
        }

        @Test
        fun extractByStackSimulateReturnsAvailableAmountAndDoesNotMutate() {
            val c = container(3)
            c.setItem(0, ItemStack(Items.REDSTONE, 4))
            c.setItem(2, ItemStack(Items.REDSTONE, 3))
            val r = Inventories.InventoryRange(c)
            val out = r.extract(ItemStack(Items.REDSTONE, 50), true)
            assertEquals(7, out.count)
            assertEquals(4, c.getItem(0).count)
            assertEquals(3, c.getItem(2).count)
        }

        @Test
        fun extractByStackReturnsEmptyWhenNoMatch() {
            val c = container(2)
            c.setItem(0, ItemStack(Items.COAL, 4))
            val r = Inventories.InventoryRange(c)
            assertTrue(r.extract(ItemStack(Items.REDSTONE, 1)).isEmpty)
        }
    }

    @Nested
    inner class Move {
        @Test
        fun moveSlotMovesIntoEmptyTarget() {
            val src = container(2)
            src.setItem(0, ItemStack(Items.REDSTONE, 5))
            val dst = container(2)
            val srcR = Inventories.InventoryRange(src)
            val dstR = Inventories.InventoryRange(dst)
            assertTrue(srcR.move(0, dstR))
            assertTrue(src.getItem(0).isEmpty)
            assertEquals(5, dst.getItem(0).count)
        }

        @Test
        fun moveSlotEmptySourceReturnsFalse() {
            val src = Inventories.InventoryRange(container(2))
            val dst = Inventories.InventoryRange(container(2))
            assertFalse(src.move(0, dst))
        }

        @Test
        fun moveAllIdenticalDrainsSourceWhenTargetHasRoom() {
            val src = container(3)
            src.setItem(0, ItemStack(Items.REDSTONE, 30))
            src.setItem(1, ItemStack(Items.REDSTONE, 20))
            src.setItem(2, ItemStack(Items.REDSTONE, 10))
            val dst = container(2)
            val srcR = Inventories.InventoryRange(src)
            val dstR = Inventories.InventoryRange(dst)
            assertTrue(srcR.move(0, dstR, true, false, false, true))
        }

        @Test
        fun moveRangeMovesEverythingPossible() {
            val src = container(3)
            src.setItem(0, ItemStack(Items.REDSTONE, 5))
            src.setItem(2, ItemStack(Items.COAL, 3))
            val dst = container(3)
            val srcR = Inventories.InventoryRange(src)
            val dstR = Inventories.InventoryRange(dst)
            assertTrue(srcR.move(dstR))
            assertTrue(src.getItem(0).isEmpty)
            assertTrue(src.getItem(2).isEmpty)
        }

        @Test
        fun moveRangeReturnsFalseWhenSourceEmpty() {
            val srcR = Inventories.InventoryRange(container(3))
            val dstR = Inventories.InventoryRange(container(3))
            assertFalse(srcR.move(dstR))
        }

        @Test
        fun moveRangeFillupVariantsCompile() {
            val src = container(2)
            src.setItem(0, ItemStack(Items.REDSTONE, 5))
            val dst = container(2)
            val srcR = Inventories.InventoryRange(src)
            val dstR = Inventories.InventoryRange(dst)
            srcR.move(dstR, true)
        }
    }
}
