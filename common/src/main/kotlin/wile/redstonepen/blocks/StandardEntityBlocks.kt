package wile.redstonepen.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEventListener
import wile.redstonepen.registry.Registries

object StandardEntityBlocks {

    interface IStandardEntityBlock<ET : StandardBlockEntity> : EntityBlock {

        fun isBlockEntityTicking(world: Level, state: BlockState): Boolean = false

        fun useOpenGui(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            player: Player,
        ): InteractionResult {
            if (world.isClientSide()) return InteractionResult.SUCCESS
            val te = world.getBlockEntity(pos)
            if (te !is MenuProvider) return InteractionResult.FAIL
            player.openMenu(te)
            return InteractionResult.CONSUME
        }

        override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
            val tet = Registries.getBlockEntityTypeOfBlock(state.block)
            return tet?.create(pos, state)
        }

        override fun <T : BlockEntity> getTicker(
            world: Level,
            state: BlockState,
            teType: BlockEntityType<T>,
        ): BlockEntityTicker<T>? =
            if (world.isClientSide || !isBlockEntityTicking(world, state)) {
                null
            } else {
                BlockEntityTicker { _, _, _, te -> (te as StandardBlockEntity).tick() }
            }

        override fun <T : BlockEntity> getListener(world: ServerLevel, te: T): GameEventListener? =
            null
    }

    open class StandardBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
        BlockEntity(type, pos, state) {

        open fun tick() {}

        open fun writenbt(hlp: HolderLookup.Provider, nbt: CompoundTag): CompoundTag =
            writenbt(hlp, nbt, false)

        open fun writenbt(
            hlp: HolderLookup.Provider,
            nbt: CompoundTag,
            syncPacket: Boolean,
        ): CompoundTag = nbt

        open fun readnbt(hlp: HolderLookup.Provider, nbt: CompoundTag): CompoundTag = nbt

        override fun loadAdditional(nbt: CompoundTag, hlp: HolderLookup.Provider) {
            readnbt(hlp, nbt)
        }

        override fun saveAdditional(nbt: CompoundTag, hlp: HolderLookup.Provider) {
            super.saveAdditional(writenbt(hlp, nbt, false), hlp)
        }

        override fun getUpdateTag(hlp: HolderLookup.Provider): CompoundTag =
            writenbt(hlp, super.getUpdateTag(hlp), true)
    }
}
