package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class RsSignalsTest
{
  @Test
  void fromContainerReturnsZeroForNullContainers()
  {
    assertEquals(0, RsSignals.fromContainer(null));
  }

  @Test
  void fromContainerMatchesVanillaComparatorStyleFillLevel()
  {
    final SimpleContainer container = new SimpleContainer(2);
    container.setItem(0, new ItemStack(Items.REDSTONE, 32));

    assertEquals(4, RsSignals.fromContainer(container));
  }
}
