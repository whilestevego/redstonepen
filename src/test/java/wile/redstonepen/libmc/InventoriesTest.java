package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class InventoriesTest
{
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
}
