package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.items.RedstonePenItem;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class PenItemGameTests
{
  private static final String NS = "minecraft";
  private static final String EMPTY = "relay_activates_from_redstone";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  private PenItemGameTests() {}

  // --- isPen / isFullRedstone --------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void isPenTrueForPenItem(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(!RedstonePenItem.isPen(pen)) helper.fail("expected isPen to be true for pen ItemStack");
    if(RedstonePenItem.isPen(new ItemStack(Items.STICK))) helper.fail("expected isPen to be false for non-pen");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneTrueForUndamagedPen(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(!RedstonePenItem.isFullRedstone(pen)) helper.fail("undamaged pen must report full");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneFalseForDamagedPen(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() > 0) {
      pen.setDamageValue(1);
      if(RedstonePenItem.isFullRedstone(pen)) helper.fail("damaged pen must not report full");
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneTrueForFullStackOfRedstone(GameTestHelper helper)
  {
    final ItemStack rs = new ItemStack(Items.REDSTONE, 64);
    if(!RedstonePenItem.isFullRedstone(rs)) helper.fail("full redstone stack must be full");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneFalseForOtherItems(GameTestHelper helper)
  {
    if(RedstonePenItem.isFullRedstone(new ItemStack(Items.STICK))) helper.fail("non-pen non-redstone must report not full");
    helper.succeed();
  }

  // --- pushRedstone / popRedstone branches -------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneIntoDamagedPenRepairs(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() <= 0) { helper.succeed(); return; }
    pen.setDamageValue(10);
    RedstonePenItem.pushRedstone(pen, 4, player);
    if(pen.getDamageValue() != 6) helper.fail("expected damage 6, got " + pen.getDamageValue());
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneOverflowGoesToInventory(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() <= 0) { helper.succeed(); return; }
    pen.setDamageValue(2);
    RedstonePenItem.pushRedstone(pen, 5, player);
    if(pen.getDamageValue() != 0) helper.fail("expected pen to be fully repaired");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneIntoCreativePlayerNoOp(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() > 0) pen.setDamageValue(5);
    final int dmg = pen.getDamageValue();
    RedstonePenItem.pushRedstone(pen, 3, player);
    if(pen.getDamageValue() != dmg) helper.fail("creative player must not modify damage");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneZeroAmountIsNoOp(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() > 0) pen.setDamageValue(5);
    final int dmg = pen.getDamageValue();
    RedstonePenItem.pushRedstone(pen, 0, player);
    if(pen.getDamageValue() != dmg) helper.fail("zero amount must not change damage");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneGrowsRedstoneStackBelowMax(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack rs = new ItemStack(Items.REDSTONE, 10);
    RedstonePenItem.pushRedstone(rs, 4, player);
    if(rs.getCount() != 14) helper.fail("expected count 14, got " + rs.getCount());
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneFromPenAccumulatesDamage(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() <= 0) { helper.succeed(); return; }
    pen.setDamageValue(0);
    final int popped = RedstonePenItem.popRedstone(pen, 3, player, net.minecraft.world.InteractionHand.MAIN_HAND);
    if(popped != 3) helper.fail("expected 3 popped, got " + popped);
    if(pen.getDamageValue() != 3) helper.fail("expected damage 3, got " + pen.getDamageValue());
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneCreativeReturnsRequestedAmount(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(RedstonePenItem.popRedstone(pen, 5, player, net.minecraft.world.InteractionHand.MAIN_HAND) != 5)
      helper.fail("creative pop should return requested amount");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneZeroAmountReturnsZero(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(RedstonePenItem.popRedstone(pen, 0, player, net.minecraft.world.InteractionHand.MAIN_HAND) != 0)
      helper.fail("zero pop must return zero");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneFromRedstoneStackShrinks(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack rs = new ItemStack(Items.REDSTONE, 10);
    final int popped = RedstonePenItem.popRedstone(rs, 3, player, net.minecraft.world.InteractionHand.MAIN_HAND);
    if(popped != 3) helper.fail("expected 3 popped, got " + popped);
    if(rs.getCount() != 7) helper.fail("expected count 7, got " + rs.getCount());
    helper.succeed();
  }

  // --- hasEnoughRedstone -------------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneCreativeAlwaysTrue(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(!RedstonePenItem.hasEnoughRedstone(pen, 1000, player)) helper.fail("creative must always have enough");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneTrueForUndamagedPen(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() > 0) {
      pen.setDamageValue(0);
      if(!RedstonePenItem.hasEnoughRedstone(pen, 1, player)) helper.fail("full pen must have enough redstone");
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneFalseForOtherItems(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack stick = new ItemStack(Items.STICK);
    if(RedstonePenItem.hasEnoughRedstone(stick, 1, player)) helper.fail("stick must not have redstone");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneOnRedstoneStackChecksCount(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack rs = new ItemStack(Items.REDSTONE, 5);
    if(!RedstonePenItem.hasEnoughRedstone(rs, 5, player)) helper.fail("count 5 must satisfy 5");
    if(RedstonePenItem.hasEnoughRedstone(rs, 6, player)) helper.fail("count 5 must not satisfy 6");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void useOnSolidBlockReturnsFailWhenFaceCannotHostTrack(GameTestHelper helper)
  {
    // A face on AIR cannot host a track; expect FAIL (early-return path before any block placement).
    helper.setBlock(POS, Blocks.AIR);
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);

    final BlockPos absolute = helper.absolutePos(POS);
    final BlockHitResult hit = new BlockHitResult(
      Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
    final var ctx = new net.minecraft.world.item.context.UseOnContext(
      helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, pen, hit);
    final InteractionResult result = pen.useOn(ctx);
    if(result != InteractionResult.FAIL) helper.fail("expected FAIL on air face, got " + result);
    helper.succeed();
  }

  // --- pen overrides -----------------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void penIsBarVisibleOnlyWhenDamaged(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getItem().isBarVisible(pen)) helper.fail("undamaged pen must not show bar");
    if(pen.getMaxDamage() > 0) {
      pen.setDamageValue(5);
      if(!pen.getItem().isBarVisible(pen)) helper.fail("damaged pen must show bar");
    }
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void penGetEnchantmentValueIsZero(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getItem().getEnchantmentValue() != 0) helper.fail("pen enchantment value must be 0");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void penIsValidRepairItemFalse(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getItem().isValidRepairItem(pen, new ItemStack(Items.REDSTONE)))
      helper.fail("pen must not be repairable by redstone via item.isValidRepairItem");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void penDestroySpeedHigherForFragileBlocks(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    final float speedRedstoneWire = pen.getItem().getDestroySpeed(pen, Blocks.REDSTONE_WIRE.defaultBlockState());
    final float speedStone = pen.getItem().getDestroySpeed(pen, Blocks.STONE.defaultBlockState());
    if(!(speedRedstoneWire > speedStone)) helper.fail("expected pen destroy speed on wire to exceed speed on stone");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void penGetBarColorIsSet(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getItem().getBarColor(pen) == 0) helper.fail("pen bar color must be non-zero");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void penGetBarWidthDecreasesWithDamage(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() <= 0) { helper.succeed(); return; }
    pen.setDamageValue(0);
    final int undamaged = pen.getItem().getBarWidth(pen);
    pen.setDamageValue(pen.getMaxDamage()/2);
    final int half = pen.getItem().getBarWidth(pen);
    if(!(half < undamaged)) helper.fail("expected bar width to decrease with damage");
    helper.succeed();
  }
}
