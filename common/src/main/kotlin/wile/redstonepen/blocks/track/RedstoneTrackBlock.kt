package wile.redstonepen.blocks.track

import java.util.Collections
import java.util.Optional
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.piston.MovingPistonBlock
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.jetbrains.annotations.Nullable
import org.joml.Vector3f
import wile.redstonepen.blocks.StandardBlocks
import wile.redstonepen.items.RedstonePenItem
import wile.redstonepen.util.Auxiliaries

@Suppress("DEPRECATION")
open class RedstoneTrackBlock(config: Long, builder: BlockBehaviour.Properties) :
    StandardBlocks.WaterLoggable(config, builder.pushReaction(PushReaction.DESTROY)), EntityBlock {
    companion object {
        @JvmStatic
        fun tile(world: BlockGetter, pos: BlockPos): Optional<TrackBlockEntity> {
            val te = world.getBlockEntity(pos)
            return if (te is TrackBlockEntity && !te.isRemoved) {
                Optional.of(te)
            } else {
                Optional.empty()
            }
        }

        @JvmStatic
        fun canBePlacedOnFace(
            state: BlockState,
            world: Level,
            pos: BlockPos,
            face: Direction,
        ): Boolean {
            if (state.block is PistonBaseBlock) {
                val pface = state.getValue(PistonBaseBlock.FACING)
                return face != pface
            }
            if (state.block is MovingPistonBlock) return true
            if (state.`is`(Blocks.HOPPER)) return face == Direction.UP
            return state.isFaceSturdy(world, pos, face)
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        TrackBlockEntity(pos, state)

    override fun hasDynamicDropList(): Boolean = true

    override fun dropList(
        state: BlockState,
        world: Level,
        @Nullable te: BlockEntity?,
        explosion: Boolean,
    ): List<ItemStack> {
        if (te !is TrackBlockEntity) return Collections.emptyList()
        val num_connections = te.getRedstoneDustCount()
        if (num_connections <= 0) return Collections.emptyList()
        return Collections.singletonList(ItemStack(Items.REDSTONE, num_connections))
    }

    @Nullable
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? =
        if (context.level.getBlockState(context.clickedPos).canBeReplaced(context)) {
            super.getStateForPlacement(context)
        } else {
            null
        }

    override fun asItem(): Item = Items.REDSTONE

    public override fun isPathfindable(state: BlockState, type: PathComputationType): Boolean = true

    public override fun getShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape {
        val wires = tile(world, pos).map { it.getWireFlags() }.orElse(0)
        val faces =
            (if ((wires and 0x00000f) != 0) 0x01 else 0) or
                (if ((wires and 0x0000f0) != 0) 0x02 else 0) or
                (if ((wires and 0x000f00) != 0) 0x04 else 0) or
                (if ((wires and 0x00f000) != 0) 0x08 else 0) or
                (if ((wires and 0x0f0000) != 0) 0x10 else 0) or
                (if ((wires and 0xf00000.toInt()) != 0) 0x20 else 0)
        return RedstoneTrackDefs.shape.get(faces)
    }

    public override fun getCollisionShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape = Shapes.empty()

    public override fun propagatesSkylightDown(
        state: BlockState,
        reader: BlockGetter,
        pos: BlockPos,
    ): Boolean = !state.getValue(WATERLOGGED)

    public override fun useShapeForLightOcclusion(state: BlockState): Boolean = true

    public override fun getRenderShape(state: BlockState): RenderShape =
        RenderShape.ENTITYBLOCK_ANIMATED

    public override fun canSurvive(state: BlockState, world: LevelReader, pos: BlockPos): Boolean =
        true

    @Deprecated("Deprecated in favour of canConnectRedstone in IForgeBlockState")
    fun canConnectRedstone(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        @Nullable side: Direction?,
    ): Boolean =
        side != null &&
            tile(world, pos)
                .map { te -> te.hasVanillaRedstoneConnection(side.opposite) }
                .orElse(false)

    public override fun isSignalSource(state: BlockState): Boolean = can_provide_power_

    public override fun getSignal(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        redstone_side: Direction,
    ): Int =
        if (can_provide_power_) {
            tile(world, pos).map { te -> te.getRedstonePower(redstone_side, true) }.orElse(0)
        } else {
            0
        }

    public override fun getDirectSignal(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        redstone_side: Direction,
    ): Int =
        if (can_provide_power_) {
            tile(world, pos).map { te -> te.getRedstonePower(redstone_side, false) }.orElse(0)
        } else {
            0
        }

    override fun shouldCheckWeakPower(
        state: BlockState,
        level: LevelReader,
        pos: BlockPos,
        side: Direction,
    ): Boolean = false

    override fun tick(state: BlockState, world: ServerLevel, pos: BlockPos, rnd: RandomSource) {
        if (!tile(world, pos).map { te -> te.sync(false) }.orElse(false)) {
            world.removeBlock(pos, false)
        }
    }

    override fun updateShape(
        state: BlockState,
        facing: Direction,
        facingState: BlockState,
        world: LevelAccessor,
        pos: BlockPos,
        facingPos: BlockPos,
    ): BlockState {
        if (!world.isClientSide()) {
            if (
                tile(world, pos)
                    .map { te -> te.handleShapeUpdate(facing, facingState, facingPos, false) }
                    .orElse(true)
            ) {
                world.scheduleTick(pos, this, 1)
            } else {
                world.removeBlock(pos, false)
            }
        }
        return super.updateShape(state, facing, facingState, world, pos, facingPos)
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
        if (world.isClientSide()) return
        notifyAdjacent(world, pos)
    }

    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        rtr: BlockHitResult,
    ): InteractionResult =
        modifySegments(
            state,
            world,
            pos,
            player,
            ItemStack.EMPTY,
            InteractionHand.MAIN_HAND,
            rtr,
            true,
            false,
        )

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        rtr: BlockHitResult,
    ): ItemInteractionResult {
        if (stack.`is`(Items.DEBUG_STICK)) {
            if (world.isClientSide) return ItemInteractionResult.SUCCESS
            if (world.getBlockEntity(pos) is TrackBlockEntity) {
                (world.getBlockEntity(pos) as TrackBlockEntity).toggle_trace(player)
            }
            return ItemInteractionResult.CONSUME
        } else {
            return when (
                modifySegments(
                    state,
                    world,
                    pos,
                    player,
                    stack,
                    hand,
                    rtr,
                    false,
                    RedstonePenItem.isPen(stack),
                )
            ) {
                InteractionResult.SUCCESS -> ItemInteractionResult.SUCCESS
                InteractionResult.CONSUME -> ItemInteractionResult.CONSUME
                InteractionResult.PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                InteractionResult.FAIL -> ItemInteractionResult.FAIL
                InteractionResult.CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL
                else -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            }
        }
    }

    public override fun neighborChanged(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        fromBlock: Block,
        fromPos: BlockPos,
        isMoving: Boolean,
    ) {
        if (world.isClientSide()) return
        try {
            val blocks_to_update =
                tile(world, pos)
                    .map { te -> te.handleNeighborChanged(fromPos) }
                    .orElse(Collections.emptyMap())
            if (blocks_to_update.isEmpty()) return
            for ((key, value) in blocks_to_update) {
                if (key == value) continue
                world.neighborChanged(key, this, value)
            }
        } catch (ex: Throwable) {
            Auxiliaries.logError("Track neighborChanged recursion detected, dropping!")
            val num_redstone = tile(world, pos).map { it.getRedstoneDustCount() }.orElse(0)
            if (num_redstone > 0) {
                val p = Vec3.atCenterOf(pos)
                world.addFreshEntity(
                    ItemEntity(world, p.x, p.y, p.z, ItemStack(Items.REDSTONE, num_redstone))
                )
                world.setBlock(
                    pos,
                    world.getBlockState(pos).fluidState.createLegacyBlock(),
                    2 or 16,
                )
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private fun spawnPoweredParticle(
        world: Level,
        rand: RandomSource,
        pos: BlockPos,
        color: Vec3,
        from: Direction,
        to: Direction,
        minChance: Float,
        maxChance: Float,
    ) {
        val f = maxChance - minChance
        if (rand.nextFloat() < 0.3f * f) {
            val c1 = 0.4375
            val c2 = (minChance + f * rand.nextFloat()).toDouble()
            val p0 = 0.5 + c1 * from.stepX + c2 * .4 * to.stepX
            val p1 = 0.5 + c1 * from.stepY + c2 * .4 * to.stepY
            val p2 = 0.5 + c1 * from.stepZ + c2 * .4 * to.stepZ
            world.addParticle(
                DustParticleOptions(Vector3f(color.toVector3f()), 1.0f),
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
    override fun animateTick(state: BlockState, world: Level, pos: BlockPos, rand: RandomSource) {
        if (rand.nextFloat() > 0.4f) return
        val te = tile(world, pos).orElse(null) ?: return
        if ((te.getStateFlags() and RedstoneTrackDefs.STATE_FLAG_PWR_MASK) == 0L) return
        val color = Vec3(0.6, 0.0, 0.0)
        for (side in Direction.values()) {
            val p = te.getSidePower(side)
            if (p == 0) continue
            spawnPoweredParticle(world, rand, pos, color, side, side.opposite, -0.5f, 0.5f)
        }
    }

    fun modifySegments(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        stack: ItemStack,
        hand: InteractionHand,
        rtr: BlockHitResult,
        no_add: Boolean,
        no_remove: Boolean,
    ): InteractionResult {
        if (!stack.isEmpty && stack.item != Items.REDSTONE && !RedstonePenItem.isPen(stack)) {
            val behind_pos = pos.relative(rtr.direction)
            val behind_state = world.getBlockState(behind_pos)
            if (behind_state.isRedstoneConductor(world, behind_pos)) {
                return behind_state.useWithoutItem(world, player, rtr)
            }
            return InteractionResult.sidedSuccess(world.isClientSide())
        }
        if (world.isClientSide()) return InteractionResult.SUCCESS
        var no_add_mut = no_add
        if (!RedstonePenItem.hasEnoughRedstone(stack, 1, player)) no_add_mut = !no_remove
        val te = tile(world, pos).orElse(null) ?: return InteractionResult.FAIL
        val no_bulk = false
        val redstone_use =
            te.modifySegments(
                pos,
                player,
                player.getItemInHand(hand),
                rtr.direction,
                rtr.location,
                no_add_mut,
                no_remove,
                no_bulk,
            )
        if (redstone_use == 0) {
            return InteractionResult.CONSUME
        } else if (redstone_use < 0) {
            RedstonePenItem.pushRedstone(stack, -redstone_use, player)
            if (te.getWireFlags() == 0) {
                world.setBlock(pos, state.fluidState.createLegacyBlock(), 1 or 2)
            } else {
                val blocks_to_update = te.updateAllPowerValuesFromAdjacent()
                for ((key, value) in blocks_to_update) {
                    world.neighborChanged(key, this, value)
                }
            }
            world.playSound(
                null,
                pos,
                SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                SoundSource.BLOCKS,
                0.4f,
                2f,
            )
        } else {
            RedstonePenItem.popRedstone(stack, redstone_use, player, hand)
            world.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.4f, 2.4f)
        }
        updateNeighbourShapes(state, world, pos)
        notifyAdjacent(world, pos)
        return InteractionResult.CONSUME
    }

    private var can_provide_power_: Boolean = true

    internal fun disablePower(disable: Boolean) {
        can_provide_power_ = !disable
    }

    private fun updateNeighbourShapes(state: BlockState, world: Level, pos: BlockPos) {
        state.updateNeighbourShapes(world, pos, 1 or 2)
    }

    fun notifyAdjacent(world: Level, pos: BlockPos) {
        world.updateNeighborsAt(pos, this)
        for (dir0 in BlockBehaviour.UPDATE_SHAPE_ORDER) {
            var ppos = pos.relative(dir0)
            world.updateNeighborsAtExceptFromFacing(
                ppos,
                world.getBlockState(ppos).block,
                dir0.opposite,
            )
            for (dir1 in BlockBehaviour.UPDATE_SHAPE_ORDER) {
                if (dir0 == dir1.opposite) return
                ppos = pos.relative(dir0).relative(dir1)
                if (ppos == pos) continue
                val diagonal_state = world.getBlockState(ppos)
                if (diagonal_state.block != this) continue
                world.neighborChanged(ppos, this, pos)
            }
        }
    }
}
