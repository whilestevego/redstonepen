package wile.redstonepen.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import wile.redstonepen.ModConstants
import wile.redstonepen.ModContent
import wile.redstonepen.items.RemoteItem
import wile.redstonepen.items.StandardItems
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
object RemoteItemGameTests {
    private const val EMPTY = "relay_activates_from_redstone"
    private val POS = BlockPos(1, 1, 1)

    private fun placeLever(helper: GameTestHelper, leverPos: BlockPos) {
        helper.setBlock(leverPos.below(), Blocks.STONE)
        helper.setBlock(leverPos, Blocks.LEVER)
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 5)
    fun remoteBarNotVisible(helper: GameTestHelper) {
        val remote = ItemStack(Registries.getItem("remote"))
        if (remote.item.isBarVisible(remote)) helper.fail("remote item must hide damage bar")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 5)
    fun remoteHasHighDestroySpeed(helper: GameTestHelper) {
        val remote = ItemStack(Registries.getItem("remote"))
        val speed = remote.item.getDestroySpeed(remote, Blocks.STONE.defaultBlockState())
        if (speed < 100f) helper.fail("remote destroy speed must be high (got $speed)")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 5)
    fun remoteUseUnlinkedReturnsFail(helper: GameTestHelper) {
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val remote = ItemStack(Registries.getItem("remote"))
        player.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val result = remote.item.use(helper.level, player, InteractionHand.MAIN_HAND)
        if (result.result != InteractionResult.FAIL) helper.fail("unlinked remote use must return fail, got ${result.result}")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 5)
    fun remoteCanAttackBlockAlwaysReturnsFalse(helper: GameTestHelper) {
        placeLever(helper, POS)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val remote = ItemStack(Registries.getItem("remote"))
        player.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val leverAbs = helper.absolutePos(POS)
        val ls = helper.level.getBlockState(leverAbs)
        val result = remote.item.canAttackBlock(ls, helper.level, leverAbs, player)
        if (result) helper.fail("canAttackBlock must return false to prevent breaking")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 5)
    fun remoteOnBlockStartBreakDoesNotThrow(helper: GameTestHelper) {
        placeLever(helper, POS)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val remote = ItemStack(Registries.getItem("remote"))
        player.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val leverAbs = helper.absolutePos(POS)
        val result = (remote.item as StandardItems.BaseItem).onBlockStartBreak(remote, leverAbs, player)
        if (result) helper.fail("onBlockStartBreak must return false")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 5)
    fun remoteDoesNotSneakBypassUse(helper: GameTestHelper) {
        val remote = ItemStack(Registries.getItem("remote"))
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        if (remote.item.doesSneakBypassUse(remote, helper.level, helper.absolutePos(POS), player))
            helper.fail("remote must not bypass sneak use")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 5)
    fun remoteAttackLinksToLeverWithServerPlayer(helper: GameTestHelper) {
        placeLever(helper, POS)
        val fp = FakePlayerFactory.getMinecraft(helper.level)
        val remote = ItemStack(Registries.getItem("remote"))
        fp.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val leverAbs = helper.absolutePos(POS)
        remote.item.canAttackBlock(helper.level.getBlockState(leverAbs), helper.level, leverAbs, fp)
        if (Auxiliaries.getItemStackNbt(remote, "remote") == null)
            helper.fail("remote must have link data after attack on lever")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 10)
    fun remoteTriggerLinkedLeverViaUse(helper: GameTestHelper) {
        placeLever(helper, POS)
        val fp = FakePlayerFactory.getMinecraft(helper.level)
        val remote = ItemStack(Registries.getItem("remote"))
        fp.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val leverAbs = helper.absolutePos(POS)
        remote.item.canAttackBlock(helper.level.getBlockState(leverAbs), helper.level, leverAbs, fp)
        remote.item.use(helper.level, fp, InteractionHand.MAIN_HAND)
        if (!helper.level.getBlockState(leverAbs).getValue(BlockStateProperties.POWERED))
            helper.fail("lever must be powered after remote trigger")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 10)
    fun remoteOnItemUseFirstConsumeWhenLinked(helper: GameTestHelper) {
        placeLever(helper, POS)
        val fp = FakePlayerFactory.getMinecraft(helper.level)
        val remote = ItemStack(Registries.getItem("remote"))
        fp.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val leverAbs = helper.absolutePos(POS)
        remote.item.canAttackBlock(helper.level.getBlockState(leverAbs), helper.level, leverAbs, fp)
        val hit = BlockHitResult(Vec3.atCenterOf(leverAbs), Direction.UP, leverAbs, false)
        val ctx = UseOnContext(helper.level, fp, InteractionHand.MAIN_HAND, remote, hit)
        val result = (remote.item as RemoteItem).onItemUseFirst(remote, ctx)
        if (result != InteractionResult.CONSUME && result != InteractionResult.CONSUME_PARTIAL)
            helper.fail("onItemUseFirst with linked remote must CONSUME, got $result")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 10)
    fun remoteTriggerLinkedButRemovedBlockTakesFailPath(helper: GameTestHelper) {
        placeLever(helper, POS)
        val fp = FakePlayerFactory.getMinecraft(helper.level)
        val remote = ItemStack(Registries.getItem("remote"))
        fp.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val leverAbs = helper.absolutePos(POS)
        remote.item.canAttackBlock(helper.level.getBlockState(leverAbs), helper.level, leverAbs, fp)
        helper.setBlock(POS, Blocks.STONE)
        remote.item.use(helper.level, fp, InteractionHand.MAIN_HAND)
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 10)
    fun remoteTriggerLinkedButtonActivatesButton(helper: GameTestHelper) {
        helper.setBlock(POS.below(), Blocks.STONE)
        helper.setBlock(POS, Blocks.STONE_BUTTON)
        val fp = FakePlayerFactory.getMinecraft(helper.level)
        val remote = ItemStack(Registries.getItem("remote"))
        fp.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val buttonAbs = helper.absolutePos(POS)
        remote.item.canAttackBlock(helper.level.getBlockState(buttonAbs), helper.level, buttonAbs, fp)
        remote.item.use(helper.level, fp, InteractionHand.MAIN_HAND)
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 10)
    fun remoteTriggerLinkedControlBoxTogglesEnabled(helper: GameTestHelper) {
        helper.setBlock(POS.below(), Blocks.STONE)
        helper.setBlock(POS, ModContent.references.CONTROLBOX_BLOCK)
        val fp = FakePlayerFactory.getMinecraft(helper.level)
        val remote = ItemStack(Registries.getItem("remote"))
        fp.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val cbAbs = helper.absolutePos(POS)
        remote.item.canAttackBlock(helper.level.getBlockState(cbAbs), helper.level, cbAbs, fp)
        val wasEnabled = helper.level.getBlockState(cbAbs).getValue(BlockStateProperties.POWERED)
        remote.item.use(helper.level, fp, InteractionHand.MAIN_HAND)
        val nowEnabled = helper.level.getBlockState(cbAbs).getValue(BlockStateProperties.POWERED)
        if (nowEnabled == wasEnabled) helper.fail("control box enabled state must change after remote trigger")
        helper.succeed()
    }

    @JvmStatic @GameTest(template = EMPTY, timeoutTicks = 10)
    fun remoteTriggerLinkedObserverHitsElseFail(helper: GameTestHelper) {
        helper.setBlock(POS.below(), Blocks.STONE)
        helper.setBlock(POS, Blocks.OBSERVER)
        val fp = FakePlayerFactory.getMinecraft(helper.level)
        val remote = ItemStack(Registries.getItem("remote"))
        fp.setItemInHand(InteractionHand.MAIN_HAND, remote)
        val obsAbs = helper.absolutePos(POS)
        val nbt = CompoundTag()
        nbt.putLong("pos", obsAbs.asLong())
        nbt.putString("name", Blocks.OBSERVER.descriptionId)
        Auxiliaries.setItemStackNbt(remote, "remote", nbt)
        remote.item.use(helper.level, fp, InteractionHand.MAIN_HAND)
        helper.succeed()
    }
}
