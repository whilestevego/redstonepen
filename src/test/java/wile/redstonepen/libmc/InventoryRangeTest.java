package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class InventoryRangeTest
{
  private static SimpleContainer container(int size)
  { return new SimpleContainer(size); }

  private static SimpleContainer filled(int size, int redstone)
  {
    final SimpleContainer c = new SimpleContainer(size);
    for(int i=0; i<size; ++i) c.setItem(i, new ItemStack(Items.REDSTONE, redstone));
    return c;
  }

  // ---- construction / accessors ---------------------------------------------------------------

  @Test
  void constructorClampsOffsetAndSizeToContainerBounds()
  {
    final SimpleContainer c = container(9);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c, 100, 100, 1);
    assertTrue(r.size() <= 9);
    assertTrue(r.offset() >= 0);
    assertEquals(c, r.inventory());
  }

  @Test
  void singleArgConstructorWrapsEntireContainer()
  {
    final SimpleContainer c = container(7);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    assertEquals(7, r.size());
    assertEquals(0, r.offset());
    assertEquals(7, r.getContainerSize());
  }

  @Test
  void twoArgConstructorDefaultsRowsToOne()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(9), 0, 9);
    assertEquals(9, r.size());
  }

  @Test
  void getAndSetUseTheRangeOffset()
  {
    final SimpleContainer c = container(9);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c, 3, 4, 1);
    r.set(0, new ItemStack(Items.REDSTONE, 2));
    assertEquals(Items.REDSTONE, c.getItem(3).getItem());
    assertEquals(2, r.get(0).getCount());
  }

  @Test
  void maxStackSizeIsBoundedAndAtLeastOne()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(4));
    r.setMaxStackSize(0);
    assertTrue(r.getMaxStackSize() >= 1);
    r.setMaxStackSize(8);
    assertEquals(8, r.getMaxStackSize());
  }

  @Test
  void validatorCanRejectItems()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(4));
    r.setValidator((slot, stack) -> stack.is(Items.REDSTONE));
    assertNotNull(r.getValidator());
    assertTrue(r.canPlaceItem(0, new ItemStack(Items.REDSTONE)));
    assertFalse(r.canPlaceItem(0, new ItemStack(Items.COAL)));
  }

  // ---- Container delegates --------------------------------------------------------------------

  @Test
  void clearContentEmptiesEverySlotInRange()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(filled(4, 2));
    r.clearContent();
    assertTrue(r.isEmpty());
  }

  @Test
  void removeItemNoUpdateRemovesAndReturnsStack()
  {
    final SimpleContainer c = filled(2, 4);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack out = r.removeItemNoUpdate(0);
    assertEquals(4, out.getCount());
    assertTrue(r.getItem(0).isEmpty());
  }

  @Test
  void removeItemSplitsStack()
  {
    final SimpleContainer c = filled(2, 5);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack out = r.removeItem(0, 3);
    assertEquals(3, out.getCount());
    assertEquals(2, r.getItem(0).getCount());
  }

  @Test
  void stillValidDelegatesToInventory()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(1));
    assertTrue(r.stillValid(null));
  }

  @Test
  void startAndStopOpenAreNoOps()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(1));
    r.startOpen(null);
    r.stopOpen(null);
    r.setChanged();
  }

  // ---- iterate / contains / indexOf / find / collect / stream / iterator ---------------------

  @Test
  void iterateStopsWhenPredicateMatches()
  {
    final SimpleContainer c = container(3);
    c.setItem(1, new ItemStack(Items.COAL));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final List<Integer> seen = new ArrayList<>();
    final boolean matched = r.iterate((i, s) -> { seen.add(i); return s.is(Items.COAL); });
    assertTrue(matched);
    assertEquals(List.of(0, 1), seen);
  }

  @Test
  void iterateReturnsFalseWhenNothingMatches()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(filled(3, 1));
    assertFalse(r.iterate((i, s) -> false));
  }

  @Test
  void containsAndIndexOfFindMatchingStack()
  {
    final SimpleContainer c = container(3);
    c.setItem(2, new ItemStack(Items.REDSTONE, 1));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    assertTrue(r.contains(new ItemStack(Items.REDSTONE, 1)));
    assertEquals(2, r.indexOf(new ItemStack(Items.REDSTONE, 1)));
    assertEquals(-1, r.indexOf(new ItemStack(Items.COAL, 1)));
  }

  @Test
  void findReturnsFirstPresentResult()
  {
    final SimpleContainer c = container(3);
    c.setItem(0, new ItemStack(Items.COAL));
    c.setItem(2, new ItemStack(Items.REDSTONE));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final Optional<Integer> idx = r.find((i, s) -> s.is(Items.REDSTONE) ? Optional.of(i) : Optional.empty());
    assertEquals(Optional.of(2), idx);
  }

  @Test
  void findReturnsEmptyWhenNothingMatches()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(2));
    assertTrue(r.find((i, s) -> Optional.<Integer>empty()).isEmpty());
  }

  @Test
  void collectGathersAllPresentResults()
  {
    final SimpleContainer c = container(4);
    c.setItem(0, new ItemStack(Items.REDSTONE));
    c.setItem(2, new ItemStack(Items.REDSTONE));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final List<Integer> hits = r.collect((i, s) -> s.is(Items.REDSTONE) ? Optional.of(i) : Optional.empty());
    assertEquals(List.of(0, 2), hits);
  }

  @Test
  void streamYieldsAllSlotsIncludingEmpty()
  {
    final SimpleContainer c = container(3);
    c.setItem(1, new ItemStack(Items.REDSTONE, 4));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    assertEquals(3, r.stream().count());
  }

  @Test
  void iteratorWalksRangeAndThrowsAtEnd()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(filled(2, 1));
    final Iterator<ItemStack> it = r.iterator();
    assertTrue(it.hasNext()); it.next();
    assertTrue(it.hasNext()); it.next();
    assertFalse(it.hasNext());
    org.junit.jupiter.api.Assertions.assertThrows(NoSuchElementException.class, it::next);
  }

  // ---- match counts ---------------------------------------------------------------------------

  @Test
  void stackMatchCountAndTotalCount()
  {
    final SimpleContainer c = container(4);
    c.setItem(0, new ItemStack(Items.REDSTONE, 3));
    c.setItem(1, new ItemStack(Items.COAL, 5));
    c.setItem(3, new ItemStack(Items.REDSTONE, 7));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    assertEquals(2, r.stackMatchCount(new ItemStack(Items.REDSTONE, 3)));
    assertEquals(10, r.totalMatchingItemCount(new ItemStack(Items.REDSTONE, 3)));
  }

  // ---- insert ---------------------------------------------------------------------------------

  @Test
  void insertEmptyStackReturnsEmpty()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(3));
    assertTrue(r.insert(ItemStack.EMPTY).isEmpty());
  }

  @Test
  void insertFillsExistingMatchingStackBeforeAnyEmptySlot()
  {
    final SimpleContainer c = container(3);
    c.setItem(2, new ItemStack(Items.REDSTONE, 10));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(new ItemStack(Items.REDSTONE, 5));
    assertTrue(remaining.isEmpty());
    assertEquals(15, c.getItem(2).getCount());
    assertTrue(c.getItem(0).isEmpty());
  }

  @Test
  void insertReturnsOverflowWhenNoCapacity()
  {
    final SimpleContainer c = container(1);
    c.setItem(0, new ItemStack(Items.REDSTONE, 64));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(new ItemStack(Items.REDSTONE, 5));
    assertEquals(5, remaining.getCount());
  }

  @Test
  void insertOnlyFillupSkipsEmptySlots()
  {
    final SimpleContainer c = container(3);
    c.setItem(0, new ItemStack(Items.REDSTONE, 60));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(new ItemStack(Items.REDSTONE, 10), true, 0, false, false);
    assertEquals(64, c.getItem(0).getCount());
    assertEquals(6, remaining.getCount());
    assertTrue(c.getItem(1).isEmpty());
  }

  @Test
  void insertUsesEmptySlotWhenNoMatchExists()
  {
    final SimpleContainer c = container(3);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(new ItemStack(Items.REDSTONE, 4));
    assertTrue(remaining.isEmpty());
    assertEquals(Items.REDSTONE, c.getItem(0).getItem());
    assertEquals(4, c.getItem(0).getCount());
  }

  @Test
  void insertReverseFillsFromTheEnd()
  {
    final SimpleContainer c = container(3);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    r.insert(new ItemStack(Items.REDSTONE, 4), false, 0, true, true);
    assertEquals(Items.REDSTONE, c.getItem(2).getItem());
  }

  @Test
  void insertSimulateDoesNotMutate()
  {
    final SimpleContainer c = container(2);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(new ItemStack(Items.REDSTONE, 4), true);
    assertTrue(remaining.isEmpty());
    assertTrue(c.getItem(0).isEmpty());
  }

  @Test
  void insertSimulateReportsLeftoverWhenFull()
  {
    final SimpleContainer c = container(1);
    c.setItem(0, new ItemStack(Items.REDSTONE, 64));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(new ItemStack(Items.REDSTONE, 4), true);
    assertEquals(4, remaining.getCount());
  }

  @Test
  void insertAtIndexFillsTargetSlotAndReturnsRemainder()
  {
    final SimpleContainer c = container(2);
    c.setItem(0, new ItemStack(Items.REDSTONE, 60));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(0, new ItemStack(Items.REDSTONE, 10));
    assertEquals(64, c.getItem(0).getCount());
    assertEquals(6, remaining.getCount());
  }

  @Test
  void insertAtIndexIntoEmptyPlacesFullStack()
  {
    final SimpleContainer c = container(2);
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack remaining = r.insert(0, new ItemStack(Items.REDSTONE, 10));
    assertTrue(remaining.isEmpty());
    assertEquals(10, c.getItem(0).getCount());
  }

  @Test
  void insertAtIndexIntoMismatchedSlotReturnsInputUnchanged()
  {
    final SimpleContainer c = container(2);
    c.setItem(0, new ItemStack(Items.COAL, 4));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack input = new ItemStack(Items.REDSTONE, 3);
    final ItemStack remaining = r.insert(0, input);
    assertEquals(3, remaining.getCount());
    assertEquals(Items.REDSTONE, remaining.getItem());
  }

  @Test
  void insertAtIndexEmptyInputReturnsEmpty()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(1));
    assertTrue(r.insert(0, ItemStack.EMPTY).isEmpty());
  }

  // ---- extract --------------------------------------------------------------------------------

  @Test
  void extractAmountFromEmptyRangeReturnsEmpty()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(container(3));
    assertTrue(r.extract(5).isEmpty());
  }

  @Test
  void extractTakesFromFirstNonEmptyStack()
  {
    final SimpleContainer c = container(3);
    c.setItem(1, new ItemStack(Items.REDSTONE, 10));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack out = r.extract(4);
    assertEquals(4, out.getCount());
    assertEquals(6, c.getItem(1).getCount());
  }

  @Test
  void extractAggregatesAcrossIdenticalStacks()
  {
    final SimpleContainer c = container(3);
    c.setItem(0, new ItemStack(Items.REDSTONE, 3));
    c.setItem(1, new ItemStack(Items.COAL, 8));
    c.setItem(2, new ItemStack(Items.REDSTONE, 2));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack out = r.extract(5);
    assertEquals(Items.REDSTONE, out.getItem());
    assertEquals(5, out.getCount());
    assertTrue(c.getItem(0).isEmpty());
    assertTrue(c.getItem(2).isEmpty());
    assertEquals(8, c.getItem(1).getCount()); // untouched
  }

  @Test
  void extractSimulateDoesNotMutate()
  {
    final SimpleContainer c = container(2);
    c.setItem(0, new ItemStack(Items.REDSTONE, 5));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack out = r.extract(2, false, true);
    assertEquals(2, out.getCount());
    assertEquals(5, c.getItem(0).getCount());
  }

  @Test
  void extractByStackReturnsEmptyForEmptyRequest()
  {
    final Inventories.InventoryRange r = new Inventories.InventoryRange(filled(2, 4));
    assertTrue(r.extract(ItemStack.EMPTY).isEmpty());
  }

  @Test
  void extractByStackPullsRequestedAmount()
  {
    final SimpleContainer c = container(3);
    c.setItem(0, new ItemStack(Items.REDSTONE, 4));
    c.setItem(2, new ItemStack(Items.REDSTONE, 3));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack req = new ItemStack(Items.REDSTONE, 5);
    final ItemStack out = r.extract(req);
    assertEquals(5, out.getCount());
  }

  @Test
  void extractByStackSimulateReturnsAvailableAmountAndDoesNotMutate()
  {
    final SimpleContainer c = container(3);
    c.setItem(0, new ItemStack(Items.REDSTONE, 4));
    c.setItem(2, new ItemStack(Items.REDSTONE, 3));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    final ItemStack out = r.extract(new ItemStack(Items.REDSTONE, 50), true);
    assertEquals(7, out.getCount());
    assertEquals(4, c.getItem(0).getCount());
    assertEquals(3, c.getItem(2).getCount());
  }

  @Test
  void extractByStackReturnsEmptyWhenNoMatch()
  {
    final SimpleContainer c = container(2);
    c.setItem(0, new ItemStack(Items.COAL, 4));
    final Inventories.InventoryRange r = new Inventories.InventoryRange(c);
    assertTrue(r.extract(new ItemStack(Items.REDSTONE, 1)).isEmpty());
  }

  // ---- move -----------------------------------------------------------------------------------

  @Test
  void moveSlotMovesIntoEmptyTarget()
  {
    final SimpleContainer src = container(2);
    src.setItem(0, new ItemStack(Items.REDSTONE, 5));
    final SimpleContainer dst = container(2);
    final Inventories.InventoryRange srcR = new Inventories.InventoryRange(src);
    final Inventories.InventoryRange dstR = new Inventories.InventoryRange(dst);
    assertTrue(srcR.move(0, dstR));
    assertTrue(src.getItem(0).isEmpty());
    assertEquals(5, dst.getItem(0).getCount());
  }

  @Test
  void moveSlotEmptySourceReturnsFalse()
  {
    final Inventories.InventoryRange src = new Inventories.InventoryRange(container(2));
    final Inventories.InventoryRange dst = new Inventories.InventoryRange(container(2));
    assertFalse(src.move(0, dst));
  }

  @Test
  void moveAllIdenticalDrainsSourceWhenTargetHasRoom()
  {
    final SimpleContainer src = container(3);
    src.setItem(0, new ItemStack(Items.REDSTONE, 30));
    src.setItem(1, new ItemStack(Items.REDSTONE, 20));
    src.setItem(2, new ItemStack(Items.REDSTONE, 10));
    final SimpleContainer dst = container(2);
    final Inventories.InventoryRange srcR = new Inventories.InventoryRange(src);
    final Inventories.InventoryRange dstR = new Inventories.InventoryRange(dst);
    assertTrue(srcR.move(0, dstR, true, false, false, true));
  }

  @Test
  void moveRangeMovesEverythingPossible()
  {
    final SimpleContainer src = container(3);
    src.setItem(0, new ItemStack(Items.REDSTONE, 5));
    src.setItem(2, new ItemStack(Items.COAL, 3));
    final SimpleContainer dst = container(3);
    final Inventories.InventoryRange srcR = new Inventories.InventoryRange(src);
    final Inventories.InventoryRange dstR = new Inventories.InventoryRange(dst);
    assertTrue(srcR.move(dstR));
    assertTrue(src.getItem(0).isEmpty());
    assertTrue(src.getItem(2).isEmpty());
  }

  @Test
  void moveRangeReturnsFalseWhenSourceEmpty()
  {
    final Inventories.InventoryRange srcR = new Inventories.InventoryRange(container(3));
    final Inventories.InventoryRange dstR = new Inventories.InventoryRange(container(3));
    assertFalse(srcR.move(dstR));
  }

  @Test
  void moveRangeFillupVariantsCompile()
  {
    final SimpleContainer src = container(2);
    src.setItem(0, new ItemStack(Items.REDSTONE, 5));
    final SimpleContainer dst = container(2);
    final Inventories.InventoryRange srcR = new Inventories.InventoryRange(src);
    final Inventories.InventoryRange dstR = new Inventories.InventoryRange(dst);
    srcR.move(dstR, true);
  }

  // ---- module-level helpers -------------------------------------------------------------------

  @Test
  void multiRangeInsertConsumesUntilEmpty()
  {
    final SimpleContainer a = container(1);
    a.setItem(0, new ItemStack(Items.REDSTONE, 60));
    final SimpleContainer b = container(2);
    final Inventories.InventoryRange[] ranges = {
      new Inventories.InventoryRange(a),
      new Inventories.InventoryRange(b),
    };
    final ItemStack remaining = Inventories.insert(ranges, new ItemStack(Items.REDSTONE, 10));
    assertTrue(remaining.isEmpty());
  }
}
