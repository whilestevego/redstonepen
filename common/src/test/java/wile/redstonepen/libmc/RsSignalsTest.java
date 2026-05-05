package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import wile.redstonepen.libmc.RsSignals;

class RsSignalsTest
{
  @BeforeAll static void bootstrap() { wile.redstonepen.McBootstrap.bootstrap(); }
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

  @Test
  void fromContainerReturnsZeroForEmptyContainer()
  {
    assertEquals(0, RsSignals.fromContainer(new SimpleContainer(1)));
  }

  @Test
  void fromContainerReturnsFifteenForFullSingleSlot()
  {
    final SimpleContainer container = new SimpleContainer(1);
    container.setItem(0, new ItemStack(Items.REDSTONE, 64));

    assertEquals(15, RsSignals.fromContainer(container));
  }

  @Test
  void fromContainerAppliesNonemptyBonusWhenFillRoundsToZero()
  {
    final SimpleContainer container = new SimpleContainer(27);
    container.setItem(0, new ItemStack(Items.REDSTONE, 1));

    // fill_level = 1/64 / 27 ≈ 0.000578; floor(0.000578 * 14) = 0; nonempty bonus = +1 → 1
    assertEquals(1, RsSignals.fromContainer(container));
  }

  @Test
  void fromContainerReturnsFifteenForFullContainer()
  {
    final SimpleContainer container = new SimpleContainer(27);
    for(int i = 0; i < 27; ++i) {
      container.setItem(i, new ItemStack(Items.REDSTONE, 64));
    }

    assertEquals(15, RsSignals.fromContainer(container));
  }

  @Test
  void fromContainerSignalBoundaryAtOneFourteenth()
  {
    // Formula: floor(fill * 14) + 1. fill = count/64 for a 1-slot container.
    // At count=4: floor(4/64 * 14) + 1 = floor(0.875) + 1 = 0 + 1 = 1
    // At count=5: floor(5/64 * 14) + 1 = floor(1.09375) + 1 = 1 + 1 = 2
    final SimpleContainer at4 = new SimpleContainer(1);
    at4.setItem(0, new ItemStack(Items.REDSTONE, 4));
    assertEquals(1, RsSignals.fromContainer(at4));

    final SimpleContainer at5 = new SimpleContainer(1);
    at5.setItem(0, new ItemStack(Items.REDSTONE, 5));
    assertEquals(2, RsSignals.fromContainer(at5));
  }
}
