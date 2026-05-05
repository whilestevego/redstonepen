package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoriesTest
{
  @BeforeAll static void bootstrap() { wile.redstonepen.McBootstrap.bootstrap(); }
  @Test
  void identicalIgnoreDamageTreatsDifferentDurabilityAsEquivalent()
  {
    final ItemStack a = new ItemStack(Items.IRON_PICKAXE);
    final ItemStack b = new ItemStack(Items.IRON_PICKAXE);

    a.setDamageValue(5);
    b.setDamageValue(42);

    assertTrue(Inventories.areItemStacksIdenticalIgnoreDamage(a, b));
  }

  @Test
  void identicalIgnoreDamageIgnoresComponentsOnlyPresentOnTheOtherStack()
  {
    final ItemStack a = new ItemStack(Items.IRON_PICKAXE);
    final ItemStack b = new ItemStack(Items.IRON_PICKAXE);

    a.setDamageValue(5);
    b.setDamageValue(42);
    b.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Renamed"));

    assertTrue(Inventories.areItemStacksIdenticalIgnoreDamage(a, b));
  }

  @Test
  void identicalIgnoreDamageStillRespectsSharedNonDamageComponents()
  {
    final ItemStack a = new ItemStack(Items.IRON_PICKAXE);
    final ItemStack b = new ItemStack(Items.IRON_PICKAXE);

    a.setDamageValue(5);
    b.setDamageValue(42);
    a.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Left"));
    b.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Right"));

    assertFalse(Inventories.areItemStacksIdenticalIgnoreDamage(a, b));
  }

  @Test
  void copyOfClonesContentsInsteadOfSharingItemStacks()
  {
    final SimpleContainer source = new SimpleContainer(2);
    source.setItem(0, new ItemStack(Items.REDSTONE, 3));

    final SimpleContainer copy = (SimpleContainer)Inventories.copyOf(source);
    copy.getItem(0).setCount(1);

    assertEquals(3, source.getItem(0).getCount());
    assertEquals(1, copy.getItem(0).getCount());
    assertNotSame(source.getItem(0), copy.getItem(0));
  }

  @Test
  void areItemStacksIdenticalTrueForSameItemAndComponents()
  {
    final ItemStack a = new ItemStack(Items.REDSTONE, 5);
    final ItemStack b = new ItemStack(Items.REDSTONE, 5);

    assertTrue(Inventories.areItemStacksIdentical(a, b));
  }

  @Test
  void areItemStacksIdenticalFalseForDifferentItems()
  {
    final ItemStack a = new ItemStack(Items.REDSTONE);
    final ItemStack b = new ItemStack(Items.COAL);

    assertFalse(Inventories.areItemStacksIdentical(a, b));
  }

  @Test
  void areItemStacksDifferentIsTrueWhenItemsDiffer()
  {
    final ItemStack a = new ItemStack(Items.REDSTONE);
    final ItemStack b = new ItemStack(Items.COAL);

    assertTrue(Inventories.areItemStacksDifferent(a, b));
  }

  @Test
  void areItemStacksDifferentIsFalseForSameItem()
  {
    final ItemStack a = new ItemStack(Items.REDSTONE, 3);
    final ItemStack b = new ItemStack(Items.REDSTONE, 3);

    assertFalse(Inventories.areItemStacksDifferent(a, b));
  }

  @Test
  void isItemStackableOnFalseForEmptySourceStack()
  {
    final ItemStack b = new ItemStack(Items.REDSTONE);

    assertFalse(Inventories.isItemStackableOn(ItemStack.EMPTY, b));
  }

  @Test
  void isItemStackableOnFalseForUnstackableItem()
  {
    // IRON_PICKAXE has max stack size of 1 → not stackable
    final ItemStack a = new ItemStack(Items.IRON_PICKAXE);
    final ItemStack b = new ItemStack(Items.IRON_PICKAXE);

    assertFalse(Inventories.isItemStackableOn(a, b));
  }

  @Test
  void isItemStackableOnTrueForMatchingStackableItems()
  {
    final ItemStack a = new ItemStack(Items.REDSTONE, 3);
    final ItemStack b = new ItemStack(Items.REDSTONE, 10);

    assertTrue(Inventories.isItemStackableOn(a, b));
  }

  @Test
  void isItemStackableOnFalseForDifferentItems()
  {
    final ItemStack a = new ItemStack(Items.REDSTONE);
    final ItemStack b = new ItemStack(Items.COAL);

    assertFalse(Inventories.isItemStackableOn(a, b));
  }

  @Test
  void copyOfPreservesContainerSize()
  {
    final SimpleContainer source = new SimpleContainer(5);

    final SimpleContainer copy = (SimpleContainer)Inventories.copyOf(source);

    assertEquals(5, copy.getContainerSize());
  }
}
