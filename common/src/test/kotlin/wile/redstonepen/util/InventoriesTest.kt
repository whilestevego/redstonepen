package wile.redstonepen.util

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap

class InventoriesTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    @Test fun identicalIgnoreDamageTreatsDifferentDurabilityAsEquivalent() {
        val a = ItemStack(Items.IRON_PICKAXE)
        val b = ItemStack(Items.IRON_PICKAXE)
        a.setDamageValue(5)
        b.setDamageValue(42)
        assertTrue(Inventories.areItemStacksIdenticalIgnoreDamage(a, b))
    }

    @Test fun identicalIgnoreDamageIgnoresComponentsOnlyPresentOnTheOtherStack() {
        val a = ItemStack(Items.IRON_PICKAXE)
        val b = ItemStack(Items.IRON_PICKAXE)
        a.setDamageValue(5)
        b.setDamageValue(42)
        b.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"))
        assertTrue(Inventories.areItemStacksIdenticalIgnoreDamage(a, b))
    }

    @Test fun identicalIgnoreDamageStillRespectsSharedNonDamageComponents() {
        val a = ItemStack(Items.IRON_PICKAXE)
        val b = ItemStack(Items.IRON_PICKAXE)
        a.setDamageValue(5)
        b.setDamageValue(42)
        a.set(DataComponents.CUSTOM_NAME, Component.literal("Left"))
        b.set(DataComponents.CUSTOM_NAME, Component.literal("Right"))
        assertFalse(Inventories.areItemStacksIdenticalIgnoreDamage(a, b))
    }

    @Test fun copyOfClonesContentsInsteadOfSharingItemStacks() {
        val source = SimpleContainer(2)
        source.setItem(0, ItemStack(Items.REDSTONE, 3))
        val copy = Inventories.copyOf(source) as SimpleContainer
        copy.getItem(0).count = 1
        assertEquals(3, source.getItem(0).count)
        assertEquals(1, copy.getItem(0).count)
        assertNotSame(source.getItem(0), copy.getItem(0))
    }

    @Test fun areItemStacksIdenticalTrueForSameItemAndComponents() {
        val a = ItemStack(Items.REDSTONE, 5)
        val b = ItemStack(Items.REDSTONE, 5)
        assertTrue(Inventories.areItemStacksIdentical(a, b))
    }

    @Test fun areItemStacksIdenticalFalseForDifferentItems() {
        val a = ItemStack(Items.REDSTONE)
        val b = ItemStack(Items.COAL)
        assertFalse(Inventories.areItemStacksIdentical(a, b))
    }

    @Test fun areItemStacksDifferentIsTrueWhenItemsDiffer() {
        val a = ItemStack(Items.REDSTONE)
        val b = ItemStack(Items.COAL)
        assertTrue(Inventories.areItemStacksDifferent(a, b))
    }

    @Test fun areItemStacksDifferentIsFalseForSameItem() {
        val a = ItemStack(Items.REDSTONE, 3)
        val b = ItemStack(Items.REDSTONE, 3)
        assertFalse(Inventories.areItemStacksDifferent(a, b))
    }

    @Test fun isItemStackableOnFalseForEmptySourceStack() {
        val b = ItemStack(Items.REDSTONE)
        assertFalse(Inventories.isItemStackableOn(ItemStack.EMPTY, b))
    }

    @Test fun isItemStackableOnFalseForUnstackableItem() {
        val a = ItemStack(Items.IRON_PICKAXE)
        val b = ItemStack(Items.IRON_PICKAXE)
        assertFalse(Inventories.isItemStackableOn(a, b))
    }

    @Test fun isItemStackableOnTrueForMatchingStackableItems() {
        val a = ItemStack(Items.REDSTONE, 3)
        val b = ItemStack(Items.REDSTONE, 10)
        assertTrue(Inventories.isItemStackableOn(a, b))
    }

    @Test fun isItemStackableOnFalseForDifferentItems() {
        val a = ItemStack(Items.REDSTONE)
        val b = ItemStack(Items.COAL)
        assertFalse(Inventories.isItemStackableOn(a, b))
    }

    @Test fun copyOfPreservesContainerSize() {
        val source = SimpleContainer(5)
        val copy = Inventories.copyOf(source) as SimpleContainer
        assertEquals(5, copy.containerSize)
    }
}
