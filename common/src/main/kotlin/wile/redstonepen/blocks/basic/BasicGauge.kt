package wile.redstonepen.blocks.basic

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import wile.redstonepen.blocks.StandardBlocks

object BasicGauge {

    class BasicGaugeBlock(config: Long, properties: BlockBehaviour.Properties) :
        StandardBlocks.Cutout(config, properties.isRedstoneConductor { _, _, _ -> false }) {

        companion object {
            @JvmField val POWER: IntegerProperty = BlockStateProperties.POWER
        }

        init {
            registerDefaultState(defaultBlockState().setValue(POWER, 0))
        }

        override fun createBlockStateDefinition(
            builder: StateDefinition.Builder<Block, BlockState>
        ) {
            super.createBlockStateDefinition(builder)
            builder.add(POWER)
        }

        override fun getShape(
            state: BlockState,
            source: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = Shapes.block()

        override fun getCollisionShape(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = Shapes.block()

        override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
            val state = super.getStateForPlacement(context) ?: return null
            return state.setValue(POWER, context.level.getBestNeighborSignal(context.clickedPos))
        }

        override fun neighborChanged(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            block: Block,
            fromPos: BlockPos,
            isMoving: Boolean,
        ) {
            if (world.isClientSide) return
            val p = world.getBestNeighborSignal(pos)
            if (p == state.getValue(POWER)) return
            world.setBlock(pos, state.setValue(POWER, p), 2)
        }

        override fun updateShape(
            state: BlockState,
            dir: Direction,
            fromState: BlockState,
            worldAccessor: LevelAccessor,
            pos: BlockPos,
            fromPos: BlockPos,
        ): BlockState {
            if (worldAccessor !is ServerLevel) return state
            return state.setValue(POWER, worldAccessor.getBestNeighborSignal(pos))
        }

        fun canConnectRedstone(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            side: Direction?,
        ): Boolean = true

        override fun shouldCheckWeakPower(
            state: BlockState,
            world: LevelReader,
            pos: BlockPos,
            side: Direction,
        ): Boolean = false
    }
}
