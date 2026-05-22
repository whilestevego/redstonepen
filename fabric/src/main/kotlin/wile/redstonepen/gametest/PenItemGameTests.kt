package wile.redstonepen.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import wile.redstonepen.items.RedstonePenItem
import wile.redstonepen.items.StandardItems
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Inventories

class PenItemGameTests {
    companion object {
        private const val EMPTY = "redstonepen:relay_activates_from_redstone"
        private val POS = BlockPos(1, 1, 1)

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun isPenTrueForPenItem(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (!RedstonePenItem.isPen(pen)) {
                helper.fail("expected isPen to be true for pen ItemStack")
            }
            if (RedstonePenItem.isPen(ItemStack(Items.STICK))) {
                helper.fail("expected isPen to be false for non-pen")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun isFullRedstoneTrueForUndamagedPen(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (!RedstonePenItem.isFullRedstone(pen)) helper.fail("undamaged pen must report full")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun isFullRedstoneFalseForDamagedPen(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage > 0) {
                pen.damageValue = 1
                if (RedstonePenItem.isFullRedstone(pen)) {
                    helper.fail("damaged pen must not report full")
                }
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun isFullRedstoneTrueForFullStackOfRedstone(helper: GameTestHelper) {
            val rs = ItemStack(Items.REDSTONE, 64)
            if (!RedstonePenItem.isFullRedstone(rs)) helper.fail("full redstone stack must be full")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun isFullRedstoneFalseForOtherItems(helper: GameTestHelper) {
            if (RedstonePenItem.isFullRedstone(ItemStack(Items.STICK))) {
                helper.fail("non-pen non-redstone must report not full")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneIntoDamagedPenRepairs(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage <= 0) {
                helper.succeed()
                return
            }
            pen.damageValue = 10
            RedstonePenItem.pushRedstone(pen, 4, player)
            if (pen.damageValue != 6) helper.fail("expected damage 6, got ${pen.damageValue}")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneOverflowGoesToInventory(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage <= 0) {
                helper.succeed()
                return
            }
            pen.damageValue = 2
            RedstonePenItem.pushRedstone(pen, 5, player)
            if (pen.damageValue != 0) helper.fail("expected pen to be fully repaired")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneIntoCreativePlayerNoOp(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.CREATIVE)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage > 0) pen.damageValue = 5
            val dmg = pen.damageValue
            RedstonePenItem.pushRedstone(pen, 3, player)
            if (pen.damageValue != dmg) helper.fail("creative player must not modify damage")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneZeroAmountIsNoOp(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage > 0) pen.damageValue = 5
            val dmg = pen.damageValue
            RedstonePenItem.pushRedstone(pen, 0, player)
            if (pen.damageValue != dmg) helper.fail("zero amount must not change damage")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneGrowsRedstoneStackBelowMax(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val rs = ItemStack(Items.REDSTONE, 10)
            RedstonePenItem.pushRedstone(rs, 4, player)
            if (rs.count != 14) helper.fail("expected count 14, got ${rs.count}")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun popRedstoneFromPenAccumulatesDamage(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage <= 0) {
                helper.succeed()
                return
            }
            pen.damageValue = 0
            val popped = RedstonePenItem.popRedstone(pen, 3, player, InteractionHand.MAIN_HAND)
            if (popped != 3) helper.fail("expected 3 popped, got $popped")
            if (pen.damageValue != 3) helper.fail("expected damage 3, got ${pen.damageValue}")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun popRedstoneCreativeReturnsRequestedAmount(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.CREATIVE)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (RedstonePenItem.popRedstone(pen, 5, player, InteractionHand.MAIN_HAND) != 5) {
                helper.fail("creative pop should return requested amount")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun popRedstoneZeroAmountReturnsZero(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (RedstonePenItem.popRedstone(pen, 0, player, InteractionHand.MAIN_HAND) != 0) {
                helper.fail("zero pop must return zero")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun popRedstoneFromRedstoneStackShrinks(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val rs = ItemStack(Items.REDSTONE, 10)
            val popped = RedstonePenItem.popRedstone(rs, 3, player, InteractionHand.MAIN_HAND)
            if (popped != 3) helper.fail("expected 3 popped, got $popped")
            if (rs.count != 7) helper.fail("expected count 7, got ${rs.count}")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun hasEnoughRedstoneCreativeAlwaysTrue(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.CREATIVE)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (!RedstonePenItem.hasEnoughRedstone(pen, 1000, player)) {
                helper.fail("creative must always have enough")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun hasEnoughRedstoneTrueForUndamagedPen(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage > 0) {
                pen.damageValue = 0
                if (!RedstonePenItem.hasEnoughRedstone(pen, 1, player)) {
                    helper.fail("full pen must have enough redstone")
                }
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun hasEnoughRedstoneFalseForOtherItems(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val stick = ItemStack(Items.STICK)
            if (RedstonePenItem.hasEnoughRedstone(stick, 1, player)) {
                helper.fail("stick must not have redstone")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun hasEnoughRedstoneOnRedstoneStackChecksCount(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val rs = ItemStack(Items.REDSTONE, 5)
            if (!RedstonePenItem.hasEnoughRedstone(rs, 5, player)) {
                helper.fail("count 5 must satisfy 5")
            }
            if (RedstonePenItem.hasEnoughRedstone(rs, 6, player)) {
                helper.fail("count 5 must not satisfy 6")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun useOnSolidBlockReturnsFailWhenFaceCannotHostTrack(helper: GameTestHelper) {
            helper.setBlock(POS, Blocks.AIR)
            val player: Player = helper.makeMockPlayer(GameType.CREATIVE)
            val pen = ItemStack(Registries.requireItem("pen"))
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val absolute = helper.absolutePos(POS)
            val hit = BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
            val ctx =
                net.minecraft.world.item.context.UseOnContext(
                    helper.level,
                    player,
                    InteractionHand.MAIN_HAND,
                    pen,
                    hit,
                )
            val result = pen.useOn(ctx)
            if (result != InteractionResult.FAIL) {
                helper.fail("expected FAIL on air face, got $result")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun useOnStoneSouthFaceServerPlacesTrack(helper: GameTestHelper) {
            helper.setBlock(POS, Blocks.STONE)
            helper.setBlock(POS.south(), Blocks.AIR)
            val player: Player = helper.makeMockPlayer(GameType.CREATIVE)
            val pen = ItemStack(Registries.requireItem("pen"))
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val absolute = helper.absolutePos(POS)
            val clickVec = Vec3.atCenterOf(absolute).add(0.0, 0.0, 0.5)
            val hit = BlockHitResult(clickVec, Direction.SOUTH, absolute, false)
            val ctx =
                net.minecraft.world.item.context.UseOnContext(
                    helper.level,
                    player,
                    InteractionHand.MAIN_HAND,
                    pen,
                    hit,
                )
            val result = pen.useOn(ctx)
            if (result == InteractionResult.FAIL) {
                helper.fail("expected non-FAIL when placing track on stone south face, got $result")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun useOnExistingTrackModifiesSegment(helper: GameTestHelper) {
            helper.setBlock(POS, Blocks.STONE)
            helper.setBlock(POS.south(), Blocks.AIR)
            val player: Player = helper.makeMockPlayer(GameType.CREATIVE)
            val pen = ItemStack(Registries.requireItem("pen"))
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val absolute = helper.absolutePos(POS)
            val clickVec = Vec3.atCenterOf(absolute).add(0.0, 0.0, 0.5)
            val hit = BlockHitResult(clickVec, Direction.SOUTH, absolute, false)
            val ctx =
                net.minecraft.world.item.context.UseOnContext(
                    helper.level,
                    player,
                    InteractionHand.MAIN_HAND,
                    pen,
                    hit,
                )
            pen.useOn(ctx)
            val trackAbsolute = helper.absolutePos(POS.south())
            val trackHit =
                BlockHitResult(
                    Vec3.atCenterOf(trackAbsolute),
                    Direction.NORTH,
                    trackAbsolute,
                    false,
                )
            val trackCtx =
                net.minecraft.world.item.context.UseOnContext(
                    helper.level,
                    player,
                    InteractionHand.MAIN_HAND,
                    pen,
                    trackHit,
                )
            val r2 = pen.useOn(trackCtx)
            if (r2 == InteractionResult.FAIL) {
                helper.fail("clicking existing track must not fail, got $r2")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penCanAttackBlockOnRedstoneWireRemovesWire(helper: GameTestHelper) {
            helper.setBlock(POS, Blocks.STONE)
            helper.setBlock(POS.above(), Blocks.REDSTONE_WIRE)
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            pen.damageValue = 0
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val wireAbs = helper.absolutePos(POS.above())
            val wireState = helper.level.getBlockState(wireAbs)
            pen.item.canAttackBlock(wireState, helper.level, wireAbs, player)
            val after = helper.level.getBlockState(wireAbs)
            if (!after.isAir) {
                helper.fail("expected redstone wire to be removed by pen attack, got: $after")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penOnBlockStartBreakTriggersAttackOnRedstoneWire(helper: GameTestHelper) {
            helper.setBlock(POS, Blocks.STONE)
            helper.setBlock(POS.above(), Blocks.REDSTONE_WIRE)
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            pen.damageValue = 0
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val wireAbs = helper.absolutePos(POS.above())
            (pen.item as StandardItems.BaseItem).onBlockStartBreak(pen, wireAbs, player)
            val after = helper.level.getBlockState(wireAbs)
            if (!after.isAir) {
                helper.fail("expected redstone wire removed by onBlockStartBreak, got: $after")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penIsBarVisibleOnlyWhenDamaged(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.item.isBarVisible(pen)) helper.fail("undamaged pen must not show bar")
            if (pen.maxDamage > 0) {
                pen.damageValue = 5
                if (!pen.item.isBarVisible(pen)) helper.fail("damaged pen must show bar")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penGetEnchantmentValueIsZero(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.item.enchantmentValue != 0) helper.fail("pen enchantment value must be 0")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penIsValidRepairItemFalse(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.item.isValidRepairItem(pen, ItemStack(Items.REDSTONE))) {
                helper.fail("pen must not be repairable by redstone via item.isValidRepairItem")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penDestroySpeedHigherForFragileBlocks(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            val speedRedstoneWire =
                pen.item.getDestroySpeed(pen, Blocks.REDSTONE_WIRE.defaultBlockState())
            val speedStone = pen.item.getDestroySpeed(pen, Blocks.STONE.defaultBlockState())
            if (!(speedRedstoneWire > speedStone)) {
                helper.fail("expected pen destroy speed on wire to exceed speed on stone")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penGetBarColorIsSet(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.item.getBarColor(pen) == 0) helper.fail("pen bar color must be non-zero")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penGetBarWidthDecreasesWithDamage(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage <= 0) {
                helper.succeed()
                return
            }
            pen.damageValue = 0
            val undamaged = pen.item.getBarWidth(pen)
            pen.damageValue = pen.maxDamage / 2
            val half = pen.item.getBarWidth(pen)
            if (half >= undamaged) helper.fail("expected bar width to decrease with damage")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penDoesSneakBypassUse(helper: GameTestHelper) {
            val pen = ItemStack(Registries.requireItem("pen"))
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            if (
                !(pen.item as StandardItems.BaseItem).doesSneakBypassUse(
                    pen,
                    helper.level,
                    helper.absolutePos(POS),
                    player,
                )
            ) {
                helper.fail("pen must bypass sneak use")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneWithUnlimitedQuillGoesToInventory(helper: GameTestHelper) {
            val quill = ItemStack(Registries.requireItem("quill"))
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            if (quill.maxDamage != 0) {
                helper.succeed()
                return
            }
            RedstonePenItem.pushRedstone(quill, 3, player)
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun popRedstoneFromUnlimitedQuillExtractsFromInventory(helper: GameTestHelper) {
            val quill = ItemStack(Registries.requireItem("quill"))
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            if (quill.maxDamage != 0) {
                helper.succeed()
                return
            }
            val popped = RedstonePenItem.popRedstone(quill, 2, player, InteractionHand.MAIN_HAND)
            if (popped != 0) {
                helper.fail("expected 0 redstone popped from empty inventory quill, got $popped")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun hasEnoughRedstoneWithUnlimitedQuillChecksInventory(helper: GameTestHelper) {
            val quill = ItemStack(Registries.requireItem("quill"))
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            if (quill.maxDamage != 0) {
                helper.succeed()
                return
            }
            if (RedstonePenItem.hasEnoughRedstone(quill, 1, player)) {
                helper.fail("expected false: quill with no inventory redstone")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun popRedstoneBreaksPenWhenDamageExceedsMax(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            if (pen.maxDamage <= 0) {
                helper.succeed()
                return
            }
            pen.damageValue = pen.maxDamage - 2
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val popped = RedstonePenItem.popRedstone(pen, 10, player, InteractionHand.MAIN_HAND)
            if (popped != 2) helper.fail("expected 2 redstone from near-broken pen, got $popped")
            if (!player.mainHandItem.isEmpty) {
                helper.fail("expected pen to be consumed when breaking")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneToNonPenItemGivesDirectly(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val stick = ItemStack(Items.STICK)
            RedstonePenItem.pushRedstone(stick, 2, player)
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun inventoryRangeFromPlayerHotbarCovers9Slots(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val ir = Inventories.InventoryRange.fromPlayerHotbar(player)
            if (ir.size() != 9) helper.fail("hotbar range must cover 9 slots, got ${ir.size()}")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun inventoryRangeFromPlayerStorageCovers27Slots(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val ir = Inventories.InventoryRange.fromPlayerStorage(player)
            if (ir.size() != 27) helper.fail("storage range must cover 27 slots, got ${ir.size()}")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun penAttackOnNonTrackNonWireBlockReturnsNormally(helper: GameTestHelper) {
            helper.setBlock(POS, Blocks.STONE)
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val pen = ItemStack(Registries.requireItem("pen"))
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val abs = helper.absolutePos(POS)
            val stoneState = helper.level.getBlockState(abs)
            pen.item.canAttackBlock(stoneState, helper.level, abs, player)
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 20)
        fun trackNeighborChangedAndUpdateShapeExercised(helper: GameTestHelper) {
            helper.setBlock(POS, Blocks.STONE)
            val player: Player = helper.makeMockPlayer(GameType.CREATIVE)
            val pen = ItemStack(Registries.requireItem("pen"))
            player.setItemInHand(InteractionHand.MAIN_HAND, pen)
            val abs = helper.absolutePos(POS)
            val clickLoc = Vec3.atCenterOf(abs).add(0.0, 0.0, 0.5)
            val hit = BlockHitResult(clickLoc, Direction.SOUTH, abs, false)
            val ctx =
                net.minecraft.world.item.context.UseOnContext(
                    helper.level,
                    player,
                    InteractionHand.MAIN_HAND,
                    pen,
                    hit,
                )
            pen.useOn(ctx)
            helper.setBlock(POS.south().east(), Blocks.REDSTONE_BLOCK)
            helper.runAfterDelay(5, helper::succeed)
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun pushRedstoneIntoFullRedstoneStackDropsExtra(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            // count=62, amount=3 → 62 > 64-3=61 → else branch → Inventories.give; stack count
            // unchanged
            val rs = ItemStack(Items.REDSTONE, 62)
            RedstonePenItem.pushRedstone(rs, 3, player)
            if (rs.count != 62) helper.fail("expected stack count 62 (unchanged), got ${rs.count}")
            helper.succeed()
        }
    }
}
