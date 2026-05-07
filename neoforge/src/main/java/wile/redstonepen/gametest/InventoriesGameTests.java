package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.ModConstants;
import wile.redstonepen.gametestcommon.InventoriesTests;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class InventoriesGameTests
{
  private static final String RELAY_TEMPLATE = "relay_activates_from_redstone";

  public InventoriesGameTests() {}

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertBetweenMatchedSlots(GameTestHelper helper)
  { InventoriesTests.inventoryRangeInsertBetweenMatchedSlots(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertBeforeAfterMatchedSlot(GameTestHelper helper)
  { InventoriesTests.inventoryRangeInsertBeforeAfterMatchedSlot(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertSimulateWithPartialStack(GameTestHelper helper)
  { InventoriesTests.inventoryRangeInsertSimulateWithPartialStack(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertSimulateNoRoomReturnsInput(GameTestHelper helper)
  { InventoriesTests.inventoryRangeInsertSimulateNoRoomReturnsInput(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeExtractMultipleMatchingSlots(GameTestHelper helper)
  { InventoriesTests.inventoryRangeExtractMultipleMatchingSlots(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeExtractWithSimulateTrue(GameTestHelper helper)
  { InventoriesTests.inventoryRangeExtractWithSimulateTrue(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void inventoryRangeInsertFillupOnlyStopsAtOnlyFillup(GameTestHelper helper)
  { InventoriesTests.inventoryRangeInsertFillupOnlyStopsAtOnlyFillup(helper); }
}
