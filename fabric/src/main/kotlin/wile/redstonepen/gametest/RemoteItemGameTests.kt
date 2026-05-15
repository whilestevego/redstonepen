package wile.redstonepen.gametest

import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import wile.redstonepen.items.StandardItems
import wile.redstonepen.registry.Registries

class RemoteItemGameTests {
    companion object {
        private const val EMPTY = "redstonepen:relay_activates_from_redstone"
        private val POS = BlockPos(1, 1, 1)

        private fun placeLever(helper: GameTestHelper, leverPos: BlockPos) {
            helper.setBlock(leverPos.below(), Blocks.STONE)
            helper.setBlock(leverPos, Blocks.LEVER)
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun remoteBarNotVisible(helper: GameTestHelper) {
            val remote = ItemStack(Registries.requireItem("remote"))
            if (remote.item.isBarVisible(remote)) helper.fail("remote item must hide damage bar")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun remoteHasHighDestroySpeed(helper: GameTestHelper) {
            val remote = ItemStack(Registries.requireItem("remote"))
            val speed = remote.item.getDestroySpeed(remote, Blocks.STONE.defaultBlockState())
            if (speed < 100f) helper.fail("remote destroy speed must be high (got $speed)")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun remoteUseUnlinkedReturnsFail(helper: GameTestHelper) {
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val remote = ItemStack(Registries.requireItem("remote"))
            player.setItemInHand(InteractionHand.MAIN_HAND, remote)
            val result = remote.item.use(helper.level, player, InteractionHand.MAIN_HAND)
            if (result.result != InteractionResult.FAIL) {
                helper.fail("unlinked remote use must return fail, got ${result.result}")
            }
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun remoteCanAttackBlockAlwaysReturnsFalse(helper: GameTestHelper) {
            placeLever(helper, POS)
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val remote = ItemStack(Registries.requireItem("remote"))
            player.setItemInHand(InteractionHand.MAIN_HAND, remote)
            val leverAbs = helper.absolutePos(POS)
            val ls = helper.level.getBlockState(leverAbs)
            val result = remote.item.canAttackBlock(ls, helper.level, leverAbs, player)
            if (result) helper.fail("canAttackBlock must return false to prevent breaking")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun remoteOnBlockStartBreakDoesNotThrow(helper: GameTestHelper) {
            placeLever(helper, POS)
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            val remote = ItemStack(Registries.requireItem("remote"))
            player.setItemInHand(InteractionHand.MAIN_HAND, remote)
            val leverAbs = helper.absolutePos(POS)
            val result =
                (remote.item as StandardItems.BaseItem).onBlockStartBreak(remote, leverAbs, player)
            if (result) helper.fail("onBlockStartBreak must return false")
            helper.succeed()
        }

        @JvmStatic
        @GameTest(template = EMPTY, timeoutTicks = 5)
        fun remoteDoesNotSneakBypassUse(helper: GameTestHelper) {
            val remote = ItemStack(Registries.requireItem("remote"))
            val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
            if (
                (remote.item as StandardItems.BaseItem).doesSneakBypassUse(
                    remote,
                    helper.level,
                    helper.absolutePos(POS),
                    player,
                )
            ) {
                helper.fail("remote must not bypass sneak use")
            }
            helper.succeed()
        }
    }
}
