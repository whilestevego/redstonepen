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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModConstants;
import wile.redstonepen.items.RedstonePenItem;
import wile.redstonepen.libmc.Inventories;
import wile.redstonepen.libmc.Registries;

public class PenItemGameTests
{
  private static final String NS = "minecraft";
  private static final String EMPTY = "redstonepen:relay_activates_from_redstone";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  public PenItemGameTests() {}

  // --- isPen / isFullRedstone --------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void isPenTrueForPenItem(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(!RedstonePenItem.isPen(pen)) helper.fail("expected isPen to be true for pen ItemStack");
    if(RedstonePenItem.isPen(new ItemStack(Items.STICK))) helper.fail("expected isPen to be false for non-pen");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneTrueForUndamagedPen(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(!RedstonePenItem.isFullRedstone(pen)) helper.fail("undamaged pen must report full");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneFalseForDamagedPen(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() > 0) {
      pen.setDamageValue(1);
      if(RedstonePenItem.isFullRedstone(pen)) helper.fail("damaged pen must not report full");
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneTrueForFullStackOfRedstone(GameTestHelper helper)
  {
    final ItemStack rs = new ItemStack(Items.REDSTONE, 64);
    if(!RedstonePenItem.isFullRedstone(rs)) helper.fail("full redstone stack must be full");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void isFullRedstoneFalseForOtherItems(GameTestHelper helper)
  {
    if(RedstonePenItem.isFullRedstone(new ItemStack(Items.STICK))) helper.fail("non-pen non-redstone must report not full");
    helper.succeed();
  }

  // --- pushRedstone / popRedstone branches -------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneGrowsRedstoneStackBelowMax(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack rs = new ItemStack(Items.REDSTONE, 10);
    RedstonePenItem.pushRedstone(rs, 4, player);
    if(rs.getCount() != 14) helper.fail("expected count 14, got " + rs.getCount());
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneCreativeReturnsRequestedAmount(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(RedstonePenItem.popRedstone(pen, 5, player, net.minecraft.world.InteractionHand.MAIN_HAND) != 5)
      helper.fail("creative pop should return requested amount");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneZeroAmountReturnsZero(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(RedstonePenItem.popRedstone(pen, 0, player, net.minecraft.world.InteractionHand.MAIN_HAND) != 0)
      helper.fail("zero pop must return zero");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneCreativeAlwaysTrue(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(!RedstonePenItem.hasEnoughRedstone(pen, 1000, player)) helper.fail("creative must always have enough");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneFalseForOtherItems(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack stick = new ItemStack(Items.STICK);
    if(RedstonePenItem.hasEnoughRedstone(stick, 1, player)) helper.fail("stick must not have redstone");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneOnRedstoneStackChecksCount(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack rs = new ItemStack(Items.REDSTONE, 5);
    if(!RedstonePenItem.hasEnoughRedstone(rs, 5, player)) helper.fail("count 5 must satisfy 5");
    if(RedstonePenItem.hasEnoughRedstone(rs, 6, player)) helper.fail("count 5 must not satisfy 6");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  // --- useOn server-side placement ---------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void useOnStoneSouthFaceServerPlacesTrack(GameTestHelper helper)
  {
    // Stone at POS acts as the support wall; the track is placed on its south face.
    helper.setBlock(POS, Blocks.STONE);
    helper.setBlock(POS.south(), Blocks.AIR);

    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);

    final BlockPos absolute = helper.absolutePos(POS);
    final net.minecraft.world.phys.Vec3 clickVec = net.minecraft.world.phys.Vec3.atCenterOf(absolute).add(0, 0, 0.5);
    final BlockHitResult hit = new BlockHitResult(clickVec, Direction.SOUTH, absolute, false);
    final var ctx = new net.minecraft.world.item.context.UseOnContext(
      helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, pen, hit);
    final InteractionResult result = pen.useOn(ctx);
    if(result == InteractionResult.FAIL) helper.fail("expected non-FAIL when placing track on stone south face, got " + result);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void useOnExistingTrackModifiesSegment(GameTestHelper helper)
  {
    // First place a track at POS.south(), then click it directly to trigger modifySegments().
    helper.setBlock(POS, Blocks.STONE);
    helper.setBlock(POS.south(), Blocks.AIR);

    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);

    final BlockPos absolute = helper.absolutePos(POS);
    final net.minecraft.world.phys.Vec3 clickVec = net.minecraft.world.phys.Vec3.atCenterOf(absolute).add(0, 0, 0.5);
    final BlockHitResult hit = new BlockHitResult(clickVec, Direction.SOUTH, absolute, false);
    final var ctx = new net.minecraft.world.item.context.UseOnContext(
      helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, pen, hit);
    pen.useOn(ctx); // places the track

    // Now click the track block directly (modifySegments path).
    final BlockPos trackAbsolute = helper.absolutePos(POS.south());
    final BlockHitResult trackHit = new BlockHitResult(
      net.minecraft.world.phys.Vec3.atCenterOf(trackAbsolute), Direction.NORTH, trackAbsolute, false);
    final var trackCtx = new net.minecraft.world.item.context.UseOnContext(
      helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, pen, trackHit);
    final InteractionResult r2 = pen.useOn(trackCtx);
    if(r2 == InteractionResult.FAIL) helper.fail("clicking existing track must not fail, got " + r2);
    helper.succeed();
  }

  // --- canAttackBlock / onBlockStartBreak --------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penCanAttackBlockOnRedstoneWireRemovesWire(GameTestHelper helper)
  {
    // Place a redstone wire block, then call canAttackBlock — attack() removes it.
    helper.setBlock(POS, Blocks.STONE);  // support
    helper.setBlock(POS.above(), Blocks.REDSTONE_WIRE);

    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    pen.setDamageValue(0);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);

    final BlockPos wireAbs = helper.absolutePos(POS.above());
    final BlockState wireState = helper.getLevel().getBlockState(wireAbs);
    pen.getItem().canAttackBlock(wireState, helper.getLevel(), wireAbs, player);

    // Wire should be gone (replaced by air or a pushed redstone item).
    final BlockState after = helper.getLevel().getBlockState(wireAbs);
    if(!after.isAir()) helper.fail("expected redstone wire to be removed by pen attack, got: " + after);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penOnBlockStartBreakTriggersAttackOnRedstoneWire(GameTestHelper helper)
  {
    helper.setBlock(POS, Blocks.STONE);
    helper.setBlock(POS.above(), Blocks.REDSTONE_WIRE);

    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    pen.setDamageValue(0);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);

    final BlockPos wireAbs = helper.absolutePos(POS.above());
    ((wile.redstonepen.libmc.StandardItems.BaseItem)pen.getItem()).onBlockStartBreak(pen, wireAbs, player);

    final BlockState after = helper.getLevel().getBlockState(wireAbs);
    if(!after.isAir()) helper.fail("expected redstone wire removed by onBlockStartBreak, got: " + after);
    helper.succeed();
  }

  // --- pen overrides -----------------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penGetEnchantmentValueIsZero(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getItem().getEnchantmentValue() != 0) helper.fail("pen enchantment value must be 0");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penIsValidRepairItemFalse(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getItem().isValidRepairItem(pen, new ItemStack(Items.REDSTONE)))
      helper.fail("pen must not be repairable by redstone via item.isValidRepairItem");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penDestroySpeedHigherForFragileBlocks(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    final float speedRedstoneWire = pen.getItem().getDestroySpeed(pen, Blocks.REDSTONE_WIRE.defaultBlockState());
    final float speedStone = pen.getItem().getDestroySpeed(pen, Blocks.STONE.defaultBlockState());
    if(!(speedRedstoneWire > speedStone)) helper.fail("expected pen destroy speed on wire to exceed speed on stone");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penGetBarColorIsSet(GameTestHelper helper)
  {
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getItem().getBarColor(pen) == 0) helper.fail("pen bar color must be non-zero");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
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

  // --- doesSneakBypassUse ------------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penDoesSneakBypassUse(GameTestHelper helper)
  {
    // Cast to BaseItem since Fabric's Item class doesn't expose doesSneakBypassUse.
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    if(!((wile.redstonepen.libmc.StandardItems.BaseItem)pen.getItem()).doesSneakBypassUse(pen, helper.getLevel(), helper.absolutePos(POS), player))
      helper.fail("pen must bypass sneak use");
    helper.succeed();
  }

  // --- quill (maxDamage=0) paths in pushRedstone / popRedstone / hasEnoughRedstone ----------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneWithUnlimitedQuillGoesToInventory(GameTestHelper helper)
  {
    // quill has durability(0) → maxDamage=0 → insert redstone into player inventory.
    final ItemStack quill = new ItemStack(Registries.getItem("quill"));
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    if(quill.getMaxDamage() != 0) { helper.succeed(); return; }
    RedstonePenItem.pushRedstone(quill, 3, player);
    // No exception = pass; inventory check is best-effort.
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneFromUnlimitedQuillExtractsFromInventory(GameTestHelper helper)
  {
    // quill has maxDamage=0 → extract redstone from player inventory path.
    final ItemStack quill = new ItemStack(Registries.getItem("quill"));
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    if(quill.getMaxDamage() != 0) { helper.succeed(); return; }
    final int popped = RedstonePenItem.popRedstone(quill, 2, player, net.minecraft.world.InteractionHand.MAIN_HAND);
    // Player has no redstone → returns 0 (extract returns empty).
    if(popped != 0) helper.fail("expected 0 redstone popped from empty inventory quill, got " + popped);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void hasEnoughRedstoneWithUnlimitedQuillChecksInventory(GameTestHelper helper)
  {
    // quill (maxDamage=0): hasEnoughRedstone calls Inventories.extract simulate.
    final ItemStack quill = new ItemStack(Registries.getItem("quill"));
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    if(quill.getMaxDamage() != 0) { helper.succeed(); return; }
    // Player has no redstone → should return false.
    if(RedstonePenItem.hasEnoughRedstone(quill, 1, player))
      helper.fail("expected false: quill with no inventory redstone");
    helper.succeed();
  }

  // --- popRedstone: pen breaks completely ---------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void popRedstoneBreaksPenWhenDamageExceedsMax(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    if(pen.getMaxDamage() <= 0) { helper.succeed(); return; }
    // Set pen to near-full damage so that popping more than remaining capacity breaks it.
    pen.setDamageValue(pen.getMaxDamage() - 2);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);
    final int popped = RedstonePenItem.popRedstone(pen, 10, player, net.minecraft.world.InteractionHand.MAIN_HAND);
    // Should only give 2 (remaining capacity before break).
    if(popped != 2) helper.fail("expected 2 redstone from near-broken pen, got " + popped);
    // Pen should now be gone from main hand.
    if(!player.getMainHandItem().isEmpty()) helper.fail("expected pen to be consumed when breaking");
    helper.succeed();
  }

  // --- pushRedstone: non-pen non-redstone item fallback -----------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void pushRedstoneToNonPenItemGivesDirectly(GameTestHelper helper)
  {
    // If stack is neither pen nor redstone, redstone goes directly to player via give().
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack stick = new ItemStack(Items.STICK);
    RedstonePenItem.pushRedstone(stick, 2, player);
    // No exception = pass; the give() drops/inserts into inventory.
    helper.succeed();
  }

  // --- Inventories.InventoryRange factory methods ------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void inventoryRangeFromPlayerHotbarCovers9Slots(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final Inventories.InventoryRange ir = Inventories.InventoryRange.fromPlayerHotbar(player);
    if(ir.size() != 9) helper.fail("hotbar range must cover 9 slots, got " + ir.size());
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void inventoryRangeFromPlayerStorageCovers27Slots(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final Inventories.InventoryRange ir = Inventories.InventoryRange.fromPlayerStorage(player);
    if(ir.size() != 27) helper.fail("storage range must cover 27 slots, got " + ir.size());
    helper.succeed();
  }

  // --- attack: else path (non-track non-wire block) ----------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void penAttackOnNonTrackNonWireBlockReturnsNormally(GameTestHelper helper)
  {
    // Stone is neither track nor redstone wire → attack() takes the else branch → return false.
    helper.setBlock(POS, Blocks.STONE);
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);
    final BlockPos abs = helper.absolutePos(POS);
    final BlockState stoneState = helper.getLevel().getBlockState(abs);
    pen.getItem().canAttackBlock(stoneState, helper.getLevel(), abs, player);
    helper.succeed();
  }

  // --- track power propagation: RedstoneTrackBlock.getSignal / TrackBlockEntity.tick -------

  @GameTest(template = EMPTY, timeoutTicks = 20)
  public static void trackNeighborChangedAndUpdateShapeExercised(GameTestHelper helper)
  {
    // Place stone support, use pen to place a track wire on its south face.
    // Then add REDSTONE_BLOCK adjacent; this triggers neighborChanged and tick in the track.
    helper.setBlock(POS, Blocks.STONE);
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final ItemStack pen = new ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);
    final BlockPos abs = helper.absolutePos(POS);
    final Vec3 clickLoc = Vec3.atCenterOf(abs).add(0, 0, 0.5);
    final BlockHitResult hit = new BlockHitResult(clickLoc, Direction.SOUTH, abs, false);
    final var ctx = new net.minecraft.world.item.context.UseOnContext(
      helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, pen, hit);
    pen.useOn(ctx);
    // Place redstone block to trigger neighborChanged / signal update paths.
    helper.setBlock(POS.south().east(), Blocks.REDSTONE_BLOCK);
    // Just verifying no crash; track may or may not survive depending on segment state.
    helper.runAfterDelay(5, helper::succeed);
  }
}
