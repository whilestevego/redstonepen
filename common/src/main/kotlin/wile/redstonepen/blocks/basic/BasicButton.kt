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
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import wile.redstonepen.util.Auxiliaries

object BasicButton {

    class BasicButtonBlock(conf: BasicButtonConfig, properties: BlockBehaviour.Properties) :
        net.minecraft.world.level.block.ButtonBlock(
            BlockSetType.SPRUCE,
            conf.activeTime,
            properties,
        ) {

        val config: BasicButtonConfig = conf

        @Environment(EnvType.CLIENT)
        override fun appendHoverText(
            stack: ItemStack,
            ctx: Item.TooltipContext,
            tooltip: MutableList<Component>,
            flag: TooltipFlag,
        ) {
            Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true)
        }

        override fun useWithoutItem(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            player: Player?,
            brh: BlockHitResult,
        ): InteractionResult {
            if (state.getValue(POWERED)) return InteractionResult.CONSUME
            world.setBlock(pos, state.setValue(POWERED, true), 1 or 2)
            this.press(state, world, pos, player)
            if (world.isClientSide) makeParticle(state, world, pos, 1.0f)
            return InteractionResult.sidedSuccess(world.isClientSide)
        }

        override fun playSound(player: Player?, world: LevelAccessor, pos: BlockPos, on: Boolean) {
            world.playSound(
                null,
                pos,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.3f,
                if (on) config.soundPitchPowered else config.soundPitchUnpowered,
            )
        }

        companion object {
            private fun makeParticle(
                state: BlockState,
                world: LevelAccessor,
                pos: BlockPos,
                f: Float,
            ) {
                repeat(3) {
                    val vpos =
                        Vec3.atCenterOf(pos)
                            .add(
                                Vec3.atBottomCenterOf(state.getValue(FACING).opposite.normal)
                                    .scale(0.1)
                            )
                            .add(
                                Vec3.atLowerCornerOf(
                                        LeverBlock.getConnectedDirection(state).opposite.normal
                                    )
                                    .scale(0.4)
                            )
                    world.addParticle(
                        DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, f),
                        vpos.x(),
                        vpos.y(),
                        vpos.z(),
                        0.0,
                        0.0,
                        0.0,
                    )
                }
            }
        }
    }
}
