package wile.redstonepen.util

import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap

class RsSignalsTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    @Test
    fun fromContainerReturnsZeroForNullContainers() {
        assertEquals(0, RsSignals.fromContainer(null))
    }

    @Test
    fun fromContainerMatchesVanillaComparatorStyleFillLevel() {
        val container = SimpleContainer(2)
        container.setItem(0, ItemStack(Items.REDSTONE, 32))
        assertEquals(4, RsSignals.fromContainer(container))
    }

    @Test
    fun fromContainerReturnsZeroForEmptyContainer() {
        assertEquals(0, RsSignals.fromContainer(SimpleContainer(1)))
    }

    @Test
    fun fromContainerReturnsFifteenForFullSingleSlot() {
        val container = SimpleContainer(1)
        container.setItem(0, ItemStack(Items.REDSTONE, 64))
        assertEquals(15, RsSignals.fromContainer(container))
    }

    @Test
    fun fromContainerAppliesNonemptyBonusWhenFillRoundsToZero() {
        val container = SimpleContainer(27)
        container.setItem(0, ItemStack(Items.REDSTONE, 1))
        assertEquals(1, RsSignals.fromContainer(container))
    }

    @Test
    fun fromContainerReturnsFifteenForFullContainer() {
        val container = SimpleContainer(27)
        for (i in 0 until 27) container.setItem(i, ItemStack(Items.REDSTONE, 64))
        assertEquals(15, RsSignals.fromContainer(container))
    }

    @Test
    fun fromContainerSignalBoundaryAtOneFourteenth() {
        val at4 = SimpleContainer(1)
        at4.setItem(0, ItemStack(Items.REDSTONE, 4))
        assertEquals(1, RsSignals.fromContainer(at4))

        val at5 = SimpleContainer(1)
        at5.setItem(0, ItemStack(Items.REDSTONE, 5))
        assertEquals(2, RsSignals.fromContainer(at5))
    }
}
