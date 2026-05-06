package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import wile.redstonepen.ModConstants;
import wile.redstonepen.libmc.Inventories;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class InventoriesGameTests
{
  private static final String RELAY_TEMPLATE = "relay_activates_from_redstone";

  public InventoriesGameTests() {}

  // --- InventoryRange.insert complex second-iteration paths --------------------------------

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertBetweenMatchedSlots(GameTestHelper helper)
  {
    // "between" path: matches at slots 1 and 3 (full), empty slot 2 between them.
    // insert(stack) uses force_group_stacks=true, so second iteration runs.
    final SimpleContainer container = new SimpleContainer(5);
    container.setItem(1, new ItemStack(Items.REDSTONE, 64));
    container.setItem(3, new ItemStack(Items.REDSTONE, 64));
    // slots 0, 2, 4 empty → slot 2 is between the two matches
    final Inventories.InventoryRange ir = new Inventories.InventoryRange(container);
    final ItemStack remaining = ir.insert(new ItemStack(Items.REDSTONE, 1));
    if(!remaining.isEmpty())
      helper.fail("expected full insertion into slot 2 between two full match slots, got remaining=" + remaining.getCount());
    helper.succeed();
  }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertBeforeAfterMatchedSlot(GameTestHelper helper)
  {
    // "before/after" path: matches at slots 1 and 3, but slot 2 is occupied by a DIFFERENT
    // item (stone), so "between" finds nothing and "before/after" inserts adjacent to slot 1.
    final SimpleContainer container = new SimpleContainer(5);
    container.setItem(1, new ItemStack(Items.REDSTONE, 64));
    container.setItem(2, new ItemStack(Items.STONE, 1));      // blocks "between"
    container.setItem(3, new ItemStack(Items.REDSTONE, 64));
    // slot 0 is empty → adjacent (before) slot 1 match → "before/after" inserts at slot 0
    final Inventories.InventoryRange ir = new Inventories.InventoryRange(container);
    final ItemStack remaining = ir.insert(new ItemStack(Items.REDSTONE, 1));
    if(!remaining.isEmpty())
      helper.fail("expected insertion before match slot via before/after path, got remaining=" + remaining.getCount());
    helper.succeed();
  }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertSimulateWithPartialStack(GameTestHelper helper)
  {
    // simulate=true path in insert(ItemStack, boolean): partially-filled slot.
    // Stack at slot 0 has 60/64 → simulate=true should return 0 remaining if input ≤4.
    final SimpleContainer container = new SimpleContainer(3);
    container.setItem(0, new ItemStack(Items.REDSTONE, 60));
    final Inventories.InventoryRange ir = new Inventories.InventoryRange(container);
    final ItemStack remaining = ir.insert(new ItemStack(Items.REDSTONE, 3), true);
    if(!remaining.isEmpty())
      helper.fail("simulate insert into partial stack must return empty, got " + remaining.getCount());
    helper.succeed();
  }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertSimulateNoRoomReturnsInput(GameTestHelper helper)
  {
    // simulate=true when all stacks are full → input returned as-is.
    final SimpleContainer container = new SimpleContainer(2);
    container.setItem(0, new ItemStack(Items.REDSTONE, 64));
    container.setItem(1, new ItemStack(Items.STICK, 64));
    final Inventories.InventoryRange ir = new Inventories.InventoryRange(container);
    final ItemStack remaining = ir.insert(new ItemStack(Items.REDSTONE, 5), true);
    if(remaining.isEmpty())
      helper.fail("simulate with no room must return the input stack, got empty");
    helper.succeed();
  }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeExtractMultipleMatchingSlots(GameTestHelper helper)
  {
    // extract() multi-slot path: stackable items spread across two slots.
    final SimpleContainer container = new SimpleContainer(3);
    container.setItem(0, new ItemStack(Items.REDSTONE, 10));
    container.setItem(1, new ItemStack(Items.REDSTONE, 10));
    final Inventories.InventoryRange ir = new Inventories.InventoryRange(container);
    final ItemStack extracted = ir.extract(15);
    if(extracted.getCount() != 15)
      helper.fail("expected 15 extracted from two slots, got " + extracted.getCount());
    helper.succeed();
  }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeExtractWithSimulateTrue(GameTestHelper helper)
  {
    // extract(amount, random=false, simulate=true) must not modify container.
    final SimpleContainer container = new SimpleContainer(2);
    container.setItem(0, new ItemStack(Items.REDSTONE, 20));
    final Inventories.InventoryRange ir = new Inventories.InventoryRange(container);
    final ItemStack extracted = ir.extract(10, false, true);
    if(extracted.getCount() != 10)
      helper.fail("simulate extract must return the requested amount, got " + extracted.getCount());
    if(container.getItem(0).getCount() != 20)
      helper.fail("simulate extract must not modify the container, got " + container.getItem(0).getCount());
    helper.succeed();
  }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertFillupOnlyStopsAtOnlyFillup(GameTestHelper helper)
  {
    // only_fillup=true path via 5-arg insert: if no match to fill up, returns input.
    final SimpleContainer container = new SimpleContainer(3);
    container.setItem(0, new ItemStack(Items.STICK, 1));
    container.setItem(1, new ItemStack(Items.STICK, 1));
    // No redstone in container → only_fillup=true → no fill-up possible → returns input.
    final Inventories.InventoryRange ir = new Inventories.InventoryRange(container);
    final ItemStack remaining = ir.insert(new ItemStack(Items.REDSTONE, 5), true, 0, false, false);
    if(remaining.isEmpty())
      helper.fail("only_fillup with no matching slot must return the input, got empty");
    helper.succeed();
  }
}
