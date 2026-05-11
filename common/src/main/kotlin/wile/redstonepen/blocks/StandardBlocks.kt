package wile.redstonepen.blocks

import java.util.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import wile.redstonepen.util.Auxiliaries

object StandardBlocks {

    const val CFG_DEFAULT: Long = 0x0000000000000000L
    const val CFG_CUTOUT: Long = 0x0000000000000001L
    const val CFG_MIPPED: Long = 0x0000000000000002L
    const val CFG_TRANSLUCENT: Long = 0x0000000000000004L
    const val CFG_WATERLOGGABLE: Long = 0x0000000000000008L
    const val CFG_HORIZIONTAL: Long = 0x0000000000000010L
    const val CFG_LOOK_PLACEMENT: Long = 0x0000000000000020L
    const val CFG_FACING_PLACEMENT: Long = 0x0000000000000040L
    const val CFG_OPPOSITE_PLACEMENT: Long = 0x0000000000000080L
    const val CFG_FLIP_PLACEMENT_IF_SAME: Long = 0x0000000000000100L
    const val CFG_FLIP_PLACEMENT_SHIFTCLICK: Long = 0x0000000000000200L
    const val CFG_STRICT_CONNECTIONS: Long = 0x0000000000000400L
    const val CFG_AI_PASSABLE: Long = 0x0000000000000800L

    interface IStandardBlock {

        fun config(): Long = 0

        fun hasDynamicDropList(): Boolean = false

        fun dropList(
            state: BlockState,
            world: Level,
            te: BlockEntity?,
            explosion: Boolean,
        ): List<ItemStack> =
            Collections.singletonList(
                if (!world.isClientSide) ItemStack(state.block.asItem()) else ItemStack.EMPTY
            )

        enum class RenderTypeHint {
            SOLID,
            CUTOUT,
            CUTOUT_MIPPED,
            TRANSLUCENT,
            TRANSLUCENT_NO_CRUMBLING,
        }

        fun getRenderTypeHint(): RenderTypeHint = getRenderTypeHint(config())

        fun getRenderTypeHint(config: Long): RenderTypeHint =
            when {
                (config and CFG_CUTOUT) != 0L -> RenderTypeHint.CUTOUT
                (config and CFG_MIPPED) != 0L -> RenderTypeHint.CUTOUT_MIPPED
                (config and CFG_TRANSLUCENT) != 0L -> RenderTypeHint.TRANSLUCENT
                else -> RenderTypeHint.SOLID
            }
    }

    open class BaseBlock(conf: Long, properties: BlockBehaviour.Properties) :
        Block(properties), IStandardBlock, SimpleWaterloggedBlock {

        companion object {
            @JvmField val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
        }

        val config: Long = conf

        init {
            var state = stateDefinition.any()
            if ((conf and CFG_WATERLOGGABLE) != 0L) state = state.setValue(WATERLOGGED, false)
            registerDefaultState(state)
        }

        override fun config(): Long = config

        @Environment(EnvType.CLIENT)
        override fun appendHoverText(
            stack: ItemStack,
            ctx: Item.TooltipContext,
            tooltip: MutableList<Component>,
            flag: TooltipFlag,
        ) {
            Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true)
        }

        override fun getRenderTypeHint(): IStandardBlock.RenderTypeHint = getRenderTypeHint(config)

        @Suppress("DEPRECATION")
        override fun isPathfindable(state: BlockState, type: PathComputationType): Boolean =
            ((config and CFG_AI_PASSABLE) != 0L) && super.isPathfindable(state, type)

        override fun onRemove(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            newState: BlockState,
            isMoving: Boolean,
        ) {
            val rsup = state.hasBlockEntity() && (state.block != newState.block)
            super.onRemove(state, world, pos, newState, isMoving)
            if (rsup) world.updateNeighbourForOutputSignal(pos, this)
        }

        override fun getDrops(state: BlockState, builder: LootParams.Builder): List<ItemStack> {
            val world = builder.level
            val explosionRadius = builder.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS)
            val te = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
            if (!hasDynamicDropList() || world == null) return super.getDrops(state, builder)
            val isExplosion = explosionRadius != null && explosionRadius > 0
            return dropList(state, world, te, isExplosion)
        }

        override fun propagatesSkylightDown(
            state: BlockState,
            reader: BlockGetter,
            pos: BlockPos,
        ): Boolean =
            (((config and CFG_WATERLOGGABLE) == 0L) || !state.getValue(WATERLOGGED)) &&
                super.propagatesSkylightDown(state, reader, pos)

        override fun getFluidState(state: BlockState): FluidState =
            if (((config and CFG_WATERLOGGABLE) != 0L) && state.getValue(WATERLOGGED)) {
                Fluids.WATER.getSource(false)
            } else {
                super.getFluidState(state)
            }

        override fun updateShape(
            state: BlockState,
            facing: Direction,
            facingState: BlockState,
            world: LevelAccessor,
            pos: BlockPos,
            facingPos: BlockPos,
        ): BlockState {
            if (((config and CFG_WATERLOGGABLE) != 0L) && state.getValue(WATERLOGGED)) {
                world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world))
            }
            return state
        }

        override fun canPlaceLiquid(
            player: Player?,
            world: BlockGetter,
            pos: BlockPos,
            state: BlockState,
            fluid: Fluid,
        ): Boolean =
            ((config and CFG_WATERLOGGABLE) != 0L) &&
                super.canPlaceLiquid(player, world, pos, state, fluid)

        override fun placeLiquid(
            world: LevelAccessor,
            pos: BlockPos,
            state: BlockState,
            fluidState: FluidState,
        ): Boolean =
            ((config and CFG_WATERLOGGABLE) != 0L) &&
                super.placeLiquid(world, pos, state, fluidState)

        override fun pickupBlock(
            player: Player?,
            world: LevelAccessor,
            pos: BlockPos,
            state: BlockState,
        ): ItemStack =
            if ((config and CFG_WATERLOGGABLE) != 0L) {
                super.pickupBlock(player, world, pos, state)
            } else {
                ItemStack.EMPTY
            }

        override fun getPickupSound(): Optional<SoundEvent> =
            if ((config and CFG_WATERLOGGABLE) != 0L) super.getPickupSound() else Optional.empty()

        open fun shouldCheckWeakPower(
            state: BlockState,
            world: LevelReader,
            pos: BlockPos,
            side: Direction,
        ): Boolean = state.isRedstoneConductor(world, pos)
    }

    open class Cutout : BaseBlock, IStandardBlock {

        protected lateinit var vshape: VoxelShape

        constructor(
            conf: Long,
            properties: BlockBehaviour.Properties,
        ) : this(conf, properties, Auxiliaries.getPixeledAABB(0.0, 0.0, 0.0, 16.0, 16.0, 16.0))

        constructor(
            conf: Long,
            properties: BlockBehaviour.Properties,
            aabb: AABB,
        ) : this(conf, properties, Shapes.create(aabb))

        constructor(
            conf: Long,
            properties: BlockBehaviour.Properties,
            aabbs: Array<AABB>,
        ) : this(
            conf,
            properties,
            aabbs
                .map { Shapes.create(it) }
                .fold(Shapes.empty()) { shape, aabb ->
                    Shapes.joinUnoptimized(shape, aabb, BooleanOp.OR)
                },
        )

        constructor(
            conf: Long,
            properties: BlockBehaviour.Properties,
            voxelShape: VoxelShape,
        ) : super(conf, properties) {
            vshape = voxelShape
        }

        override fun getShape(
            state: BlockState,
            source: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = vshape

        override fun getCollisionShape(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = vshape

        override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
            var state = super.getStateForPlacement(context) ?: return null
            if ((config and CFG_WATERLOGGABLE) != 0L) {
                val fs = context.level.getFluidState(context.clickedPos)
                state = state.setValue(WATERLOGGED, fs.type == Fluids.WATER)
            }
            return state
        }

        override fun isPossibleToRespawnInThis(state: BlockState): Boolean = false

        override fun propagatesSkylightDown(
            state: BlockState,
            reader: BlockGetter,
            pos: BlockPos,
        ): Boolean {
            if ((config and CFG_WATERLOGGABLE) != 0L && state.getValue(WATERLOGGED)) return false
            return super.propagatesSkylightDown(state, reader, pos)
        }

        override fun getFluidState(state: BlockState): FluidState {
            if ((config and CFG_WATERLOGGABLE) != 0L) {
                return if (state.getValue(WATERLOGGED)) {
                    Fluids.WATER.getSource(false)
                } else {
                    super.getFluidState(state)
                }
            }
            return super.getFluidState(state)
        }

        override fun updateShape(
            state: BlockState,
            facing: Direction,
            facingState: BlockState,
            world: LevelAccessor,
            pos: BlockPos,
            facingPos: BlockPos,
        ): BlockState {
            if ((config and CFG_WATERLOGGABLE) != 0L && state.getValue(WATERLOGGED)) {
                world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world))
            }
            return state
        }
    }

    open class WaterLoggable : Cutout, IStandardBlock {

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
        ) : super(config or CFG_WATERLOGGABLE, properties)

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            aabb: AABB,
        ) : super(config or CFG_WATERLOGGABLE, properties, aabb)

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            voxelShape: VoxelShape,
        ) : super(config or CFG_WATERLOGGABLE, properties, voxelShape)

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            aabbs: Array<AABB>,
        ) : super(config or CFG_WATERLOGGABLE, properties, aabbs)

        override fun createBlockStateDefinition(
            builder: StateDefinition.Builder<Block, BlockState>
        ) {
            super.createBlockStateDefinition(builder)
            builder.add(WATERLOGGED)
        }
    }

    open class Directed : Cutout, IStandardBlock {

        companion object {
            @JvmField val FACING: DirectionProperty = BlockStateProperties.FACING
        }

        protected lateinit var vshapes: Map<BlockState, VoxelShape>

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            shapeSupplier: (List<BlockState>) -> Map<BlockState, VoxelShape>,
        ) : super(config, properties) {
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP))
            vshapes = shapeSupplier(stateDefinition.possibleStates)
        }

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            shapeSupplier: () -> ArrayList<VoxelShape>,
        ) : this(
            config,
            properties,
            { states ->
                val vshapes = HashMap<BlockState, VoxelShape>()
                val indexedShapes = shapeSupplier()
                for (state in states) {
                    vshapes[state] = indexedShapes[state.getValue(FACING).get3DDataValue()]
                }
                vshapes
            },
        )

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            unrotatedAABBs: Array<AABB>,
        ) : this(
            config,
            properties.isValidSpawn { _, _, _, _ -> false },
            { states ->
                val isHorizontal = (config and CFG_HORIZIONTAL) != 0L
                val vshapes = HashMap<BlockState, VoxelShape>()
                for (state in states) {
                    vshapes[state] =
                        Auxiliaries.getUnionShape(
                            Auxiliaries.getRotatedAABB(
                                unrotatedAABBs,
                                state.getValue(FACING),
                                isHorizontal,
                            )
                        )
                }
                vshapes
            },
        )

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            unrotatedAABB: AABB,
        ) : this(config, properties, arrayOf(unrotatedAABB))

        override fun isPossibleToRespawnInThis(state: BlockState): Boolean = false

        override fun getShape(
            state: BlockState,
            source: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = vshapes[state]!!

        override fun getCollisionShape(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = getShape(state, world, pos, selectionContext)

        override fun createBlockStateDefinition(
            builder: StateDefinition.Builder<Block, BlockState>
        ) {
            super.createBlockStateDefinition(builder)
            builder.add(FACING)
        }

        override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
            val state = super.getStateForPlacement(context) ?: return null
            var facing = context.clickedFace
            if (
                (config and (CFG_HORIZIONTAL or CFG_LOOK_PLACEMENT)) ==
                    (CFG_HORIZIONTAL or CFG_LOOK_PLACEMENT)
            ) {
                facing = context.horizontalDirection
            } else if ((config and (CFG_HORIZIONTAL or CFG_LOOK_PLACEMENT)) == CFG_HORIZIONTAL) {
                if (facing == Direction.UP || facing == Direction.DOWN) return null
            } else if ((config and CFG_LOOK_PLACEMENT) != 0L) {
                facing = context.nearestLookingDirection
            }
            if ((config and CFG_OPPOSITE_PLACEMENT) != 0L) facing = facing.opposite
            val player = context.player
            if (
                ((config and CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0L) &&
                    player != null &&
                    player.isShiftKeyDown
            ) {
                facing = facing.opposite
            }
            return state.setValue(FACING, facing)
        }
    }

    open class Horizontal : Cutout, IStandardBlock {

        companion object {
            @JvmField
            val HORIZONTAL_FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
        }

        protected lateinit var vshapes: Map<BlockState, VoxelShape>
        protected lateinit var cshapes: Map<BlockState, VoxelShape>

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            shapeSupplier: (List<BlockState>) -> Map<BlockState, VoxelShape>,
        ) : super(config or CFG_HORIZIONTAL, properties) {
            registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH))
            vshapes = shapeSupplier(stateDefinition.possibleStates)
            cshapes = shapeSupplier(stateDefinition.possibleStates)
        }

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            shapeSupplier: () -> ArrayList<VoxelShape>,
        ) : this(
            config,
            properties,
            { states ->
                val vshapes = HashMap<BlockState, VoxelShape>()
                val indexedShapes = shapeSupplier()
                for (state in states) {
                    vshapes[state] =
                        indexedShapes[state.getValue(HORIZONTAL_FACING).get3DDataValue()]
                }
                vshapes
            },
        )

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            unrotatedAABB: AABB,
        ) : this(config, properties, arrayOf(unrotatedAABB))

        constructor(
            config: Long,
            properties: BlockBehaviour.Properties,
            unrotatedAABBs: Array<AABB>,
        ) : this(
            config,
            properties,
            { states ->
                val vshapes = HashMap<BlockState, VoxelShape>()
                for (state in states) {
                    vshapes[state] =
                        Auxiliaries.getUnionShape(
                            Auxiliaries.getRotatedAABB(
                                unrotatedAABBs,
                                state.getValue(HORIZONTAL_FACING),
                                true,
                            )
                        )
                }
                vshapes
            },
        )

        override fun createBlockStateDefinition(
            builder: StateDefinition.Builder<Block, BlockState>
        ) {
            super.createBlockStateDefinition(builder)
            builder.add(HORIZONTAL_FACING)
        }

        override fun getShape(
            state: BlockState,
            source: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = vshapes[state]!!

        override fun getCollisionShape(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = cshapes[state]!!

        override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
            val state = super.getStateForPlacement(context) ?: return null
            var facing = context.clickedFace
            if ((config and CFG_LOOK_PLACEMENT) != 0L) {
                facing = context.horizontalDirection
            } else {
                facing =
                    if (facing == Direction.UP || facing == Direction.DOWN) {
                        context.horizontalDirection
                    } else {
                        facing
                    }
            }
            if ((config and CFG_OPPOSITE_PLACEMENT) != 0L) facing = facing.opposite
            val player = context.player
            if (
                ((config and CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0L) &&
                    player != null &&
                    player.isShiftKeyDown
            ) {
                facing = facing.opposite
            }
            return state.setValue(HORIZONTAL_FACING, facing)
        }

        override fun rotate(state: BlockState, rot: Rotation): BlockState =
            state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING)))

        override fun mirror(state: BlockState, mirrorIn: Mirror): BlockState =
            state.rotate(mirrorIn.getRotation(state.getValue(HORIZONTAL_FACING)))
    }
}
