package wile.redstonepen.blocks.basic

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import wile.redstonepen.util.Auxiliaries

object BasicLever {

    class BasicLeverBlock(conf: BasicLeverConfig, properties: BlockBehaviour.Properties) :
        LeverBlock(properties) {

        val config: BasicLeverConfig = conf

        @Environment(EnvType.CLIENT)
        override fun appendHoverText(stack: ItemStack, ctx: Item.TooltipContext, tooltip: MutableList<Component>, flag: TooltipFlag) {
            Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true)
        }

        override fun useWithoutItem(state: BlockState, world: Level, pos: BlockPos, player: Player?, brh: BlockHitResult): InteractionResult {
            if (world.isClientSide) {
                val newState = state.cycle(POWERED)
                if (newState.getValue(POWERED)) makeParticle(newState, world, pos, 1.0f)
                return InteractionResult.SUCCESS
            } else {
                val newState = state.cycle(POWERED)
                world.setBlock(pos, newState, 1 or 2)
                world.updateNeighborsAt(pos, this)
                world.updateNeighborsAt(pos.relative(LeverBlock.getConnectedDirection(newState).opposite), this)
                world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f,
                    if (newState.getValue(POWERED)) config.soundPitchPowered else config.soundPitchUnpowered)
                world.gameEvent(player, if (newState.getValue(POWERED)) GameEvent.BLOCK_ACTIVATE else GameEvent.BLOCK_DEACTIVATE, pos)
                return InteractionResult.CONSUME
            }
        }

        companion object {
            private fun makeParticle(state: BlockState, world: LevelAccessor, pos: BlockPos, f: Float) {
                for (i in 0 until 2) {
                    val vpos = Vec3.atCenterOf(pos)
                        .add(Vec3.atBottomCenterOf(state.getValue(FACING).opposite.normal).scale(0.1))
                        .add(Vec3.atLowerCornerOf(LeverBlock.getConnectedDirection(state).opposite.normal).scale(0.2))
                    world.addParticle(DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, f), vpos.x(), vpos.y(), vpos.z(), 0.0, 0.0, 0.0)
                }
            }
        }
    }
}
