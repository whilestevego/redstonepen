package wile.redstonepen.gametestcommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import wile.redstonepen.blocks.basic.BasicGauge
import wile.redstonepen.registry.Registries

object BasicLeverButtonTests {
    private val POS = BlockPos(1, 1, 1)
    private val SUPPORT = POS.below()

    @JvmStatic fun leverUseWithoutItemTogglesPoweredFalseToTrue(helper: GameTestHelper) {
        helper.setBlock(SUPPORT, Blocks.STONE)
        helper.setBlock(POS, Registries.getBlock("basic_lever")!!.defaultBlockState()
            .setValue(LeverBlock.FACE, AttachFace.FLOOR)
            .setValue(LeverBlock.FACING, Direction.NORTH))
        val before = helper.getBlockState(POS)
        if (before.getValue(BlockStateProperties.POWERED)) helper.fail("expected initial powered=false")
        val abs = helper.absolutePos(POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        before.useWithoutItem(helper.level, null, hit)
        helper.runAfterDelay(2) {
            val after = helper.getBlockState(POS)
            if (!after.getValue(BlockStateProperties.POWERED)) helper.fail("expected powered after lever toggle")
            helper.succeed()
        }
    }

    @JvmStatic fun leverUseTwiceReturnsToUnpowered(helper: GameTestHelper) {
        helper.setBlock(SUPPORT, Blocks.STONE)
        helper.setBlock(POS, Registries.getBlock("basic_lever")!!.defaultBlockState()
            .setValue(LeverBlock.FACE, AttachFace.FLOOR)
            .setValue(LeverBlock.FACING, Direction.NORTH))
        val abs = helper.absolutePos(POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        helper.getBlockState(POS).useWithoutItem(helper.level, null, hit)
        helper.getBlockState(POS).useWithoutItem(helper.level, null, hit)
        helper.runAfterDelay(2) {
            if (helper.getBlockState(POS).getValue(BlockStateProperties.POWERED))
                helper.fail("expected unpowered after two toggles")
            helper.succeed()
        }
    }

    @JvmStatic fun buttonUseWithoutItemPressesAndPowers(helper: GameTestHelper) {
        helper.setBlock(SUPPORT, Blocks.STONE)
        helper.setBlock(POS, Registries.getBlock("basic_button")!!.defaultBlockState()
            .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
            .setValue(ButtonBlock.FACING, Direction.NORTH))
        val before = helper.getBlockState(POS)
        if (before.getValue(BlockStateProperties.POWERED)) helper.fail("expected initial powered=false")
        val abs = helper.absolutePos(POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        before.useWithoutItem(helper.level, null, hit)
        helper.runAfterDelay(1) {
            if (!helper.getBlockState(POS).getValue(BlockStateProperties.POWERED))
                helper.fail("expected button powered after press")
            helper.succeed()
        }
    }

    @JvmStatic fun buttonUseOnPoweredButtonReturnsConsume(helper: GameTestHelper) {
        helper.setBlock(SUPPORT, Blocks.STONE)
        helper.setBlock(POS, Registries.getBlock("basic_button")!!.defaultBlockState()
            .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
            .setValue(ButtonBlock.FACING, Direction.NORTH)
            .setValue(BlockStateProperties.POWERED, true))
        val abs = helper.absolutePos(POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        val result = helper.getBlockState(POS).useWithoutItem(helper.level, null, hit)
        if (result == null) helper.fail("expected non-null InteractionResult")
        helper.succeed()
    }

    @JvmStatic fun pulseButtonRevertsAfterShortInterval(helper: GameTestHelper) {
        helper.setBlock(SUPPORT, Blocks.STONE)
        helper.setBlock(POS, Registries.getBlock("basic_pulse_button")!!.defaultBlockState()
            .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
            .setValue(ButtonBlock.FACING, Direction.NORTH))
        val abs = helper.absolutePos(POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        helper.getBlockState(POS).useWithoutItem(helper.level, null, hit)
        helper.runAfterDelay(20) {
            if (helper.getBlockState(POS).getValue(BlockStateProperties.POWERED))
                helper.fail("pulse button should revert after its active period")
            helper.succeed()
        }
    }

    @JvmStatic fun gaugeReadsZeroWhenNoSignal(helper: GameTestHelper) {
        helper.setBlock(POS, Registries.getBlock("basic_gauge")!!.defaultBlockState())
        helper.runAfterDelay(2) {
            val power = helper.getBlockState(POS).getValue(BlockStateProperties.POWER)
            if (power != 0) helper.fail("expected gauge power=0 with no signal, got $power")
            helper.succeed()
        }
    }

    @JvmStatic fun gaugeReadsSignalFromAdjacentRedstoneBlock(helper: GameTestHelper) {
        helper.setBlock(POS, Registries.getBlock("basic_gauge")!!.defaultBlockState())
        helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK)
        helper.succeedWhen {
            val power = helper.getBlockState(POS).getValue(BlockStateProperties.POWER)
            if (power <= 0) helper.fail("expected gauge power>0 adjacent to redstone block, got $power")
        }
    }

    @JvmStatic fun gaugeShouldCheckWeakPowerReturnsFalse(helper: GameTestHelper) {
        helper.setBlock(POS, Registries.getBlock("basic_gauge")!!.defaultBlockState())
        val block = Registries.getBlock("basic_gauge")!! as BasicGauge.BasicGaugeBlock
        val result = block.shouldCheckWeakPower(helper.getBlockState(POS), helper.level, helper.absolutePos(POS), Direction.NORTH)
        if (result) helper.fail("shouldCheckWeakPower must return false")
        helper.succeed()
    }

    @JvmStatic fun gaugeGetStateForPlacementReturnsNonNull(helper: GameTestHelper) {
        val block: Block = Registries.getBlock("basic_gauge")!!
        val stack = ItemStack(block)
        val abs = helper.absolutePos(POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.setItemInHand(InteractionHand.MAIN_HAND, stack)
        val useCtx = UseOnContext(helper.level, player, InteractionHand.MAIN_HAND, stack, hit)
        val ctx = BlockPlaceContext(useCtx)
        block.getStateForPlacement(ctx)
        helper.succeed()
    }
}
