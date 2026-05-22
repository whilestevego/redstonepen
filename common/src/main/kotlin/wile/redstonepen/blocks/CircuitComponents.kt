package wile.redstonepen.blocks

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.SignalGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import wile.redstonepen.ModContent
import wile.redstonepen.blocks.track.RedstoneTrackBlock
import wile.redstonepen.client.Overlay
import wile.redstonepen.util.Auxiliaries
import wile.redstonepen.util.RsSignals

object CircuitComponents {

    val RELAY_AABB: AABB = Auxiliaries.getPixeledAABB(5.0, 0.0, 0.0, 11.0, 1.0, 16.0)

    fun relayProperties(): BlockBehaviour.Properties =
        BlockBehaviour.Properties.of().noCollission().instabreak()

    open class DirectedComponentBlock : StandardBlocks.WaterLoggable {

        companion object {
            @JvmField val FACING: DirectionProperty = BlockStateProperties.FACING

            @JvmField val ROTATION: IntegerProperty = IntegerProperty.create("rotation", 0, 3)

            @JvmField val POWERED: BooleanProperty = BlockStateProperties.POWERED

            @JvmField val STATE: IntegerProperty = IntegerProperty.create("state", 0, 1)

            private val facingMapping: List<Direction> = makeFacingMappings()
            private val facingFwdStateMapping: Array<Array<Array<Direction?>>> =
                Array(6) { Array(4) { arrayOfNulls(6) } }
            private val facingRevStateMapping: Array<Array<Array<Direction?>>> =
                Array(6) { Array(4) { arrayOfNulls(6) } }

            // Placement rotation indexed by [face.ordinal()][hit-direction.ordinal()].
            // Mirrors the rotation→direction order defined in makeFacingMappings().
            //   columns: DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
            private val PLACEMENT_ROTATION =
                arrayOf(
                    intArrayOf(0, 0, 0, 2, 3, 1), // DOWN
                    intArrayOf(0, 0, 0, 2, 3, 1), // UP
                    intArrayOf(2, 0, 0, 0, 3, 1), // NORTH
                    intArrayOf(2, 0, 0, 0, 1, 3), // SOUTH
                    intArrayOf(2, 0, 1, 3, 0, 0), // WEST
                    intArrayOf(2, 0, 3, 1, 0, 0), // EAST
                )

            private fun makeFacingMappings(): List<Direction> {
                val maps = ArrayList<Direction>()
                Direction.entries.forEach { face ->
                    when (face) {
                        Direction.DOWN,
                        Direction.UP -> {
                            maps.add(Direction.NORTH)
                            maps.add(Direction.EAST)
                            maps.add(Direction.SOUTH)
                            maps.add(Direction.WEST)
                        }
                        Direction.NORTH -> {
                            maps.add(Direction.UP)
                            maps.add(Direction.EAST)
                            maps.add(Direction.DOWN)
                            maps.add(Direction.WEST)
                        }
                        Direction.EAST -> {
                            maps.add(Direction.UP)
                            maps.add(Direction.SOUTH)
                            maps.add(Direction.DOWN)
                            maps.add(Direction.NORTH)
                        }
                        Direction.SOUTH -> {
                            maps.add(Direction.UP)
                            maps.add(Direction.WEST)
                            maps.add(Direction.DOWN)
                            maps.add(Direction.EAST)
                        }
                        Direction.WEST -> {
                            maps.add(Direction.UP)
                            maps.add(Direction.NORTH)
                            maps.add(Direction.DOWN)
                            maps.add(Direction.SOUTH)
                        }
                    }
                }
                return maps
            }

            private fun fillStateFacingLookups(states: List<BlockState>) {
                if (
                    facingFwdStateMapping[0][0][0] != null && facingRevStateMapping[0][0][0] != null
                ) {
                    return
                }
                for (state in states) {
                    for (worldSide in Direction.entries) {
                        val sm =
                            when (worldSide) {
                                Direction.DOWN -> getDownFacing(state)
                                Direction.UP -> getUpFacing(state)
                                Direction.NORTH -> getFrontFacing(state)
                                Direction.SOUTH -> getBackFacing(state)
                                Direction.WEST -> getLeftFacing(state)
                                Direction.EAST -> getRightFacing(state)
                            }
                        facingFwdStateMapping[state.getValue(FACING).ordinal][
                            state.getValue(ROTATION)][worldSide.ordinal] = sm
                        facingRevStateMapping[state.getValue(FACING).ordinal][
                            state.getValue(ROTATION)][sm.ordinal] = worldSide
                    }
                }
            }

            fun mappedShape(state: BlockState, aabb: Array<AABB>): VoxelShape =
                when (state.getValue(FACING)) {
                    Direction.DOWN ->
                        when (state.getValue(ROTATION)) {
                            0 -> Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(aabb, 0))
                            1 -> Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(aabb, 1))
                            2 -> Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(aabb, 2))
                            else -> Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(aabb, 3))
                        }
                    Direction.UP ->
                        when (state.getValue(ROTATION)) {
                            0 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getMirroredAABB(
                                        Auxiliaries.getYRotatedAABB(aabb, 0),
                                        Direction.Axis.Y,
                                    )
                                )
                            1 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getMirroredAABB(
                                        Auxiliaries.getYRotatedAABB(aabb, 1),
                                        Direction.Axis.Y,
                                    )
                                )
                            2 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getMirroredAABB(
                                        Auxiliaries.getYRotatedAABB(aabb, 2),
                                        Direction.Axis.Y,
                                    )
                                )
                            else ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getMirroredAABB(
                                        Auxiliaries.getYRotatedAABB(aabb, 3),
                                        Direction.Axis.Y,
                                    )
                                )
                        }
                    Direction.NORTH ->
                        when (state.getValue(ROTATION)) {
                            0 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.SOUTH),
                                        Direction.DOWN,
                                    )
                                )
                            1 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.WEST),
                                        Direction.DOWN,
                                    )
                                )
                            2 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.NORTH),
                                        Direction.DOWN,
                                    )
                                )
                            else ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.EAST),
                                        Direction.DOWN,
                                    )
                                )
                        }
                    Direction.EAST ->
                        when (state.getValue(ROTATION)) {
                            0 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getYRotatedAABB(
                                        Auxiliaries.getRotatedAABB(
                                            Auxiliaries.getRotatedAABB(aabb, Direction.UP),
                                            Direction.WEST,
                                        ),
                                        0,
                                    )
                                )
                            1 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getYRotatedAABB(
                                        Auxiliaries.getRotatedAABB(
                                            Auxiliaries.getRotatedAABB(aabb, Direction.WEST),
                                            Direction.DOWN,
                                        ),
                                        1,
                                    )
                                )
                            2 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getYRotatedAABB(
                                        Auxiliaries.getRotatedAABB(
                                            Auxiliaries.getRotatedAABB(aabb, Direction.SOUTH),
                                            Direction.UP,
                                        ),
                                        3,
                                    )
                                )
                            else ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getYRotatedAABB(
                                        Auxiliaries.getRotatedAABB(
                                            Auxiliaries.getRotatedAABB(aabb, Direction.WEST),
                                            Direction.UP,
                                        ),
                                        3,
                                    )
                                )
                        }
                    Direction.SOUTH ->
                        when (state.getValue(ROTATION)) {
                            0 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.NORTH),
                                        Direction.UP,
                                    )
                                )
                            1 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.EAST),
                                        Direction.UP,
                                    )
                                )
                            2 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.SOUTH),
                                        Direction.UP,
                                    )
                                )
                            else ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.WEST),
                                        Direction.UP,
                                    )
                                )
                        }
                    Direction.WEST ->
                        when (state.getValue(ROTATION)) {
                            0 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.UP),
                                        Direction.EAST,
                                    )
                                )
                            1 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getYRotatedAABB(
                                        Auxiliaries.getRotatedAABB(
                                            Auxiliaries.getRotatedAABB(aabb, Direction.EAST),
                                            Direction.UP,
                                        ),
                                        1,
                                    )
                                )
                            2 ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getRotatedAABB(
                                        Auxiliaries.getRotatedAABB(aabb, Direction.DOWN),
                                        Direction.WEST,
                                    )
                                )
                            else ->
                                Auxiliaries.getUnionShape(
                                    Auxiliaries.getYRotatedAABB(
                                        Auxiliaries.getRotatedAABB(
                                            Auxiliaries.getRotatedAABB(aabb, Direction.WEST),
                                            Direction.UP,
                                        ),
                                        1,
                                    )
                                )
                        }
                    else -> Shapes.block()
                }

            @JvmStatic
            fun getFrontFacing(state: BlockState): Direction =
                facingMapping[
                    (state.getValue(FACING).get3DDataValue()) * 4 +
                        (state.getValue(ROTATION) and 0x3)]

            @JvmStatic
            fun getRightFacing(state: BlockState): Direction =
                facingMapping[
                    (state.getValue(FACING).get3DDataValue()) * 4 +
                        ((state.getValue(ROTATION) + 1) and 0x3)]

            @JvmStatic
            fun getBackFacing(state: BlockState): Direction =
                facingMapping[
                    (state.getValue(FACING).get3DDataValue()) * 4 +
                        ((state.getValue(ROTATION) + 2) and 0x3)]

            @JvmStatic
            fun getLeftFacing(state: BlockState): Direction =
                facingMapping[
                    (state.getValue(FACING).get3DDataValue()) * 4 +
                        ((state.getValue(ROTATION) + 3) and 0x3)]

            @JvmStatic
            fun getUpFacing(state: BlockState): Direction = state.getValue(FACING).opposite

            @JvmStatic fun getDownFacing(state: BlockState): Direction = state.getValue(FACING)

            @JvmStatic
            fun getForwardStateMappedFacing(state: BlockState, internalSide: Direction): Direction =
                requireNotNull(
                    facingFwdStateMapping[state.getValue(FACING).ordinal][state.getValue(ROTATION)][
                        internalSide.ordinal]
                )

            @JvmStatic
            fun getReverseStateMappedFacing(state: BlockState, worldSide: Direction): Direction =
                requireNotNull(
                    facingRevStateMapping[state.getValue(FACING).ordinal][state.getValue(ROTATION)][
                        worldSide.ordinal]
                )

            @JvmStatic
            protected fun getOutputFacing(state: BlockState): Direction = getFrontFacing(state)

            @JvmStatic
            fun placementRotation(face: Direction, dir: Direction): Int =
                PLACEMENT_ROTATION[face.ordinal][dir.ordinal]
        }

        protected val shapes: HashMap<BlockState, VoxelShape> = HashMap()

        constructor(
            config: Long,
            builder: BlockBehaviour.Properties,
            aabbs: Array<AABB>,
        ) : super(config, builder.pushReaction(PushReaction.DESTROY)) {
            registerDefaultState(
                defaultBlockState()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(ROTATION, 0)
                    .setValue(POWERED, false)
                    .setValue(STATE, 0)
            )
            stateDefinition.possibleStates.forEach { state ->
                shapes[state] = mappedShape(state, aabbs)
            }
            fillStateFacingLookups(stateDefinition.possibleStates)
        }

        constructor(
            config: Long,
            builder: BlockBehaviour.Properties,
            aabb: AABB,
        ) : this(config, builder, arrayOf(aabb))

        override fun createBlockStateDefinition(
            builder: StateDefinition.Builder<Block, BlockState>
        ) {
            super.createBlockStateDefinition(builder)
            builder.add(FACING, ROTATION, POWERED, STATE)
        }

        override fun hasDynamicDropList(): Boolean = true

        override fun dropList(
            state: BlockState,
            world: Level,
            te: BlockEntity?,
            explosion: Boolean,
        ): List<ItemStack> = listOf(ItemStack(this.asItem()))

        @Suppress("DEPRECATION")
        override fun isPathfindable(state: BlockState, type: PathComputationType): Boolean = true

        override fun getShape(
            state: BlockState,
            source: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = shapes.getOrDefault(state, Shapes.block())

        override fun getCollisionShape(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            selectionContext: CollisionContext,
        ): VoxelShape = getShape(state, world, pos, selectionContext)

        override fun getOcclusionShape(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
        ): VoxelShape = shapes.getOrDefault(state, Shapes.block())

        override fun propagatesSkylightDown(
            state: BlockState,
            reader: BlockGetter,
            pos: BlockPos,
        ): Boolean = !state.getValue(WATERLOGGED)

        fun canConnectRedstone(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            side: Direction?,
        ): Boolean = side == null || side != state.getValue(FACING)

        override fun isSignalSource(state: BlockState): Boolean = true

        override fun hasAnalogOutputSignal(state: BlockState): Boolean = false

        override fun getAnalogOutputSignal(state: BlockState, world: Level, pos: BlockPos): Int = 0

        override fun getSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int = 0

        override fun getDirectSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int = 0

        override fun tick(
            state: BlockState,
            world: ServerLevel,
            pos: BlockPos,
            rnd: RandomSource,
        ) {}

        override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
            val state = super.getStateForPlacement(context) ?: return null
            val face = context.clickedFace.opposite
            val hitR = context.clickLocation.subtract(Vec3.atCenterOf(context.clickedPos))
            val hit =
                when (face) {
                    Direction.WEST,
                    Direction.EAST -> hitR.multiply(0.0, 1.0, 1.0)
                    Direction.SOUTH,
                    Direction.NORTH -> hitR.multiply(1.0, 1.0, 0.0)
                    else -> hitR.multiply(1.0, 0.0, 1.0)
                }
            val dir = Direction.getNearest(hit.x(), hit.y(), hit.z())
            val rotation = placementRotation(face, dir)
            val placed =
                state
                    .setValue(FACING, face)
                    .setValue(ROTATION, rotation)
                    .setValue(POWERED, false)
                    .setValue(STATE, 0)
            if (!canSurvive(placed, context.level, context.clickedPos)) return null
            return placed
        }

        override fun updateShape(
            state: BlockState,
            facing: Direction,
            facingState: BlockState,
            world: LevelAccessor,
            pos: BlockPos,
            facingPos: BlockPos,
        ): BlockState {
            val updated = super.updateShape(state, facing, facingState, world, pos, facingPos)
            if (!canSurvive(updated, world, pos)) return Blocks.AIR.defaultBlockState()
            return if (world is ServerLevel) update(updated, world, pos, facingPos) else updated
        }

        override fun canSurvive(state: BlockState, world: LevelReader, pos: BlockPos): Boolean {
            val face = state.getValue(FACING)
            val adjPos = pos.relative(face)
            return world.getBlockState(adjPos).isFaceSturdy(world, adjPos, face.opposite)
        }

        override fun onPlace(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            oldState: BlockState,
            isMoving: Boolean,
        ) {
            update(state, world, pos, null)
        }

        override fun onRemove(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            newState: BlockState,
            isMoving: Boolean,
        ) {
            if (isMoving || state.`is`(newState.block)) return
            super.onRemove(state, world, pos, newState, isMoving)
            if (!world.isClientSide()) {
                notifyOutputNeighbourOfStateChange(state, world, pos)
                world.updateNeighborsAt(pos, this)
            }
        }

        @Suppress("FunctionOnlyReturningConstant")
        fun shouldCheckWeakPower(
            state: BlockState,
            level: SignalGetter,
            pos: BlockPos,
            side: Direction,
        ): Boolean = false

        override fun neighborChanged(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            fromBlock: Block,
            fromPos: BlockPos,
            isMoving: Boolean,
        ) {
            update(state, world, pos, fromPos)
        }

        @Environment(EnvType.CLIENT)
        private fun spawnPoweredParticle(
            world: Level,
            rand: RandomSource,
            pos: BlockPos,
            color: Vec3,
            side: Direction,
            chance: Float,
        ) {
            if (rand.nextFloat() < chance) {
                val c2 = chance * rand.nextFloat()
                val p0 = 0.5 + side.stepX * 0.4 + c2 * 0.1
                val p1 = 0.5 + side.stepY * 0.4 + c2 * 0.1
                val p2 = 0.5 + side.stepZ * 0.4 + c2 * 0.1
                world.addParticle(
                    DustParticleOptions(
                        org.joml.Vector3f(color.x.toFloat(), color.y.toFloat(), color.z.toFloat()),
                        1.0f,
                    ),
                    pos.x + p0,
                    pos.y + p1,
                    pos.z + p2,
                    0.0,
                    0.0,
                    0.0,
                )
            }
        }

        @Environment(EnvType.CLIENT)
        override fun animateTick(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            rand: RandomSource,
        ) {
            if (!state.getValue(POWERED) || rand.nextFloat() > 0.4f) return
            val color = Vec3(0.6, 0.0, 0.0)
            spawnPoweredParticle(world, rand, pos, color, state.getValue(FACING), 0.3f)
        }

        protected open fun notifyOutputNeighbourOfStateChange(
            state: BlockState,
            world: Level,
            pos: BlockPos,
        ) {
            notifyOutputNeighbourOfStateChange(state, world, pos, getOutputFacing(state))
        }

        protected open fun notifyOutputNeighbourOfStateChange(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            facing: Direction,
        ) {
            val adjacentPos = pos.relative(facing)
            val adjacentState = world.getBlockState(adjacentPos)
            try {
                adjacentState.handleNeighborChanged(world, adjacentPos, this, pos, false)
                if (RsSignals.canEmitWeakPower(adjacentState, world, adjacentPos, facing)) {
                    world.updateNeighborsAtExceptFromFacing(
                        adjacentPos,
                        state.block,
                        facing.opposite,
                    )
                }
            } catch (ex: Throwable) {
                Auxiliaries.logError("Curcuit neighborChanged recursion detected, dropping! ($ex)")
                val p = Vec3.atCenterOf(pos)
                world.addFreshEntity(ItemEntity(world, p.x, p.y, p.z, ItemStack(this, 1)))
                world.setBlock(
                    pos,
                    world.getBlockState(pos).fluidState.createLegacyBlock(),
                    2 or 16,
                )
            }
        }

        open fun update(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            fromPos: BlockPos?,
        ): BlockState = state
    }

    class DirectedComponentBlockItem(block: Block, builder: Item.Properties) :
        BlockItem(block, builder) {

        override fun inventoryTick(
            stack: ItemStack,
            world: Level,
            entity: Entity,
            itemSlot: Int,
            isSelected: Boolean,
        ) {
            if (!isSelected || !world.isClientSide || entity !is Player) return
            val hr = getPlayerPOVHitResult(world, entity, ClipContext.Fluid.ANY)
            val pc = BlockPlaceContext(UseOnContext(entity, InteractionHand.MAIN_HAND, hr))
            if (!pc.canPlace()) return
            val state = block.getStateForPlacement(pc) ?: return
            Overlay.show(state, pc.clickedPos)
        }
    }

    open class RelayBlock : DirectedComponentBlock {

        constructor() : this(StandardBlocks.CFG_CUTOUT, relayProperties(), RELAY_AABB)

        constructor(
            config: Long,
            builder: BlockBehaviour.Properties,
            aabb: AABB,
        ) : super(config, builder, aabb)

        protected open fun isPowered(state: BlockState, world: Level, pos: BlockPos): Boolean {
            val outputSide = getOutputFacing(state)
            val mountSide = state.getValue(FACING)
            for (side in Direction.entries) {
                if (side == outputSide) continue
                if (side == mountSide.opposite) continue
                if (world.getSignal(pos.relative(side), side) > 0) return true
            }
            return false
        }

        override fun getSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int =
            if (!state.getValue(POWERED) || redstoneSide != getOutputFacing(state).opposite) {
                0
            } else {
                15
            }

        override fun getDirectSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int = getSignal(state, world, pos, redstoneSide)

        override fun tick(state: BlockState, world: ServerLevel, pos: BlockPos, rnd: RandomSource) {
            val powered = isPowered(state, world, pos)
            if (powered == state.getValue(POWERED)) return
            if (!powered) {
                world.setBlock(pos, state.setValue(POWERED, false), 2 or 16)
                notifyOutputNeighbourOfStateChange(state, world, pos)
            }
        }

        override fun update(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            fromPos: BlockPos?,
        ): BlockState {
            val powered = isPowered(state, world, pos)
            if (powered == state.getValue(POWERED)) return state
            if (world.blockTicks.hasScheduledTick(pos, this)) return state
            if (powered) {
                world.setBlock(pos, state.setValue(POWERED, true), 2 or 16)
                notifyOutputNeighbourOfStateChange(state, world, pos)
            } else {
                world.scheduleTick(pos, this, 2)
            }
            return state
        }
    }

    open class InvertedRelayBlock : RelayBlock {

        constructor() : this(StandardBlocks.CFG_CUTOUT, relayProperties(), RELAY_AABB)

        constructor(
            config: Long,
            builder: BlockBehaviour.Properties,
            aabb: AABB,
        ) : super(config, builder, aabb)

        override fun getSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int =
            if (state.getValue(POWERED) || redstoneSide != getOutputFacing(state).opposite) {
                0
            } else {
                15
            }

        override fun tick(state: BlockState, world: ServerLevel, pos: BlockPos, rnd: RandomSource) {
            val powered = isPowered(state, world, pos)
            if (powered == state.getValue(POWERED)) return
            if (powered) {
                world.setBlock(pos, state.setValue(POWERED, true), 2 or 16)
                notifyOutputNeighbourOfStateChange(state, world, pos)
            }
        }

        override fun update(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            fromPos: BlockPos?,
        ): BlockState {
            val powered = isPowered(state, world, pos)
            if (powered == state.getValue(POWERED)) return state
            if (world.blockTicks.hasScheduledTick(pos, this)) return state
            if (powered) {
                world.scheduleTick(pos, this, 2)
            } else {
                world.setBlock(pos, state.setValue(POWERED, false), 2 or 16)
                notifyOutputNeighbourOfStateChange(state, world, pos)
            }
            return state
        }
    }

    open class BistableRelayBlock : RelayBlock {

        constructor() : this(StandardBlocks.CFG_CUTOUT, relayProperties(), RELAY_AABB)

        constructor(
            config: Long,
            builder: BlockBehaviour.Properties,
            aabb: AABB,
        ) : super(config, builder, aabb)

        override fun getSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int =
            if (state.getValue(STATE) == 0 || redstoneSide != getOutputFacing(state).opposite) {
                0
            } else {
                15
            }

        override fun tick(
            state: BlockState,
            world: ServerLevel,
            pos: BlockPos,
            rnd: RandomSource,
        ) {}

        override fun update(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            fromPos: BlockPos?,
        ): BlockState {
            val powered = isPowered(state, world, pos)
            val pwstate = state.getValue(POWERED)
            if (powered == pwstate) return state
            var s = state.setValue(POWERED, powered)
            if (powered && !pwstate) {
                s = s.setValue(STATE, if (s.getValue(STATE) == 0) 1 else 0)
                world.setBlock(pos, s, 2 or 16)
                notifyOutputNeighbourOfStateChange(s, world, pos)
            } else if (!powered && pwstate) {
                world.setBlock(pos, s, 2 or 16)
            }
            return s
        }
    }

    open class PulseRelayBlock : RelayBlock {

        constructor() : this(StandardBlocks.CFG_CUTOUT, relayProperties(), RELAY_AABB)

        constructor(
            config: Long,
            builder: BlockBehaviour.Properties,
            aabb: AABB,
        ) : super(config, builder, aabb)

        override fun getSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int =
            if (state.getValue(STATE) == 0 || redstoneSide != getOutputFacing(state).opposite) {
                0
            } else {
                15
            }

        override fun tick(state: BlockState, world: ServerLevel, pos: BlockPos, rnd: RandomSource) {
            if (state.getValue(STATE) == 0) return
            val s = state.setValue(STATE, 0)
            world.setBlock(pos, s, 2 or 16)
            notifyOutputNeighbourOfStateChange(s, world, pos)
        }

        override fun update(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            fromPos: BlockPos?,
        ): BlockState {
            var s = state
            val powered = isPowered(s, world, pos)
            if (powered != s.getValue(POWERED)) {
                s = s.setValue(POWERED, powered)
                if (powered) {
                    val trig = s.getValue(STATE) == 0
                    s = s.setValue(STATE, 1)
                    world.setBlock(pos, s, 2 or 16)
                    if (trig) notifyOutputNeighbourOfStateChange(s, world, pos)
                } else {
                    world.setBlock(pos, s, 2 or 16)
                }
            }
            if (!world.blockTicks.hasScheduledTick(pos, this)) world.scheduleTick(pos, this, 2)
            return s
        }
    }

    open class BridgeRelayBlock : RelayBlock {

        private var powerUpdateRecursionLevel: Int = 0

        constructor() : this(StandardBlocks.CFG_CUTOUT, relayProperties(), RELAY_AABB)

        constructor(
            config: Long,
            builder: BlockBehaviour.Properties,
            aabb: AABB,
        ) : super(config, builder, aabb)

        protected open fun getInputPower(
            world: Level,
            relayPos: BlockPos,
            redstoneSide: Direction,
        ): Int {
            val pos = relayPos.relative(redstoneSide)
            val state = world.getBlockState(pos)
            var p = 0
            if (powerUpdateRecursionLevel < 32) {
                ++powerUpdateRecursionLevel
                p =
                    when {
                        state.`is`(Blocks.REDSTONE_WIRE) ->
                            maxOf(0, state.getDirectSignal(world, pos, redstoneSide) - 2)
                        state.`is`(ModContent.References.TRACK_BLOCK) ->
                            maxOf(
                                0,
                                RedstoneTrackBlock.tile(world, pos)
                                    .map { te -> te.getRedstonePower(redstoneSide, true) }
                                    .orElse(0) - 2,
                            )
                        state.`is`(ModContent.References.BRIDGE_RELAY_BLOCK) ->
                            if (
                                state.getValue(FACING) !=
                                    world.getBlockState(relayPos).getValue(FACING)
                            ) {
                                0
                            } else if (
                                (state.getValue(ROTATION) and 0x1) !=
                                    (world.getBlockState(relayPos).getValue(ROTATION) and 0x1)
                            ) {
                                0
                            } else {
                                getInputPower(world, pos, redstoneSide)
                            }
                        else -> {
                            var sp = state.getSignal(world, pos, redstoneSide)
                            if (
                                sp < 15 &&
                                    !state.isSignalSource &&
                                    RsSignals.canEmitWeakPower(state, world, pos, redstoneSide)
                            ) {
                                for (d in Direction.entries) {
                                    if (d == redstoneSide.opposite) continue
                                    sp =
                                        maxOf(
                                            sp,
                                            world
                                                .getBlockState(pos.relative(d))
                                                .getDirectSignal(world, pos.relative(d), d),
                                        )
                                    if (sp >= 15) break
                                }
                            }
                            sp
                        }
                    }
                if (--powerUpdateRecursionLevel < 0) powerUpdateRecursionLevel = 0
            }
            return p
        }

        protected open fun isWireConnected(
            world: Level,
            relayPos: BlockPos,
            side: Direction,
        ): Boolean {
            val state = world.getBlockState(relayPos.relative(side))
            return state.`is`(Blocks.REDSTONE_WIRE) || state.`is`(ModContent.References.TRACK_BLOCK)
        }

        protected open fun isSidePowered(world: Level, pos: BlockPos, side: Direction): Boolean =
            world.getSignal(pos.relative(side), side) > 0

        override fun isPowered(state: BlockState, world: Level, pos: BlockPos): Boolean =
            isSidePowered(world, pos, state.getValue(FACING)) ||
                isSidePowered(world, pos, getOutputFacing(state).opposite)

        override fun getSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int {
            if (redstoneSide == getOutputFacing(state).opposite) {
                return if (state.getValue(POWERED)) 15 else 0
            }
            val sworld = world as? ServerLevel ?: return 0
            val left = getLeftFacing(state)
            val right = getRightFacing(state)
            if (redstoneSide != left && redstoneSide != right) return 0
            return if (isWireConnected(sworld, pos, redstoneSide)) {
                getInputPower(sworld, pos, redstoneSide)
            } else if (world.getBlockState(pos.relative(redstoneSide)).isSignalSource) {
                getInputPower(sworld, pos, left)
            } else {
                0
            }
        }

        override fun getDirectSignal(
            state: BlockState,
            world: BlockGetter,
            pos: BlockPos,
            redstoneSide: Direction,
        ): Int = getSignal(state, world, pos, redstoneSide)

        override fun update(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            fromPos: BlockPos?,
        ): BlockState {
            var s = state
            val powered = isPowered(s, world, pos)
            if (powered != s.getValue(POWERED)) {
                if (!world.blockTicks.hasScheduledTick(pos, this)) {
                    if (powered) {
                        s = s.setValue(POWERED, true)
                        world.setBlock(pos, s, 2 or 16)
                        world.neighborChanged(pos.relative(getOutputFacing(s)), this, pos)
                    } else {
                        world.scheduleTick(pos, this, 2)
                    }
                }
                return s
            }
            if (fromPos != null) {
                val v: Vec3i = pos.subtract(fromPos)
                val redstoneSide =
                    Direction.getNearest(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
                val left = getLeftFacing(s)
                val right = getRightFacing(s)
                if (redstoneSide != left && redstoneSide != right) return s
                powerUpdateRecursionLevel = 0
                val npos = pos.relative(redstoneSide)
                world.getBlockState(npos).handleNeighborChanged(world, npos, this, pos, false)
                val pr = getInputPower(world, pos, right)
                val pl = getInputPower(world, pos, left)
                val trackPowered = pr > 0 || pl > 0
                if (trackPowered != (s.getValue(STATE) == 1)) {
                    s = s.setValue(STATE, if (trackPowered) 1 else 0)
                    world.setBlock(pos, s, 2 or 16)
                }
            }
            return s
        }
    }
}
