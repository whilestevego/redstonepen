package wile.redstonepen.blocks.controlbox

import java.util.Arrays
import java.util.Collections
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import wile.redstonepen.blocks.CircuitComponents
import wile.redstonepen.blocks.StandardEntityBlocks
import wile.redstonepen.util.Auxiliaries

@Suppress("DEPRECATION")
class ControlBoxBlock(config: Long, builder: BlockBehaviour.Properties, aabb: Array<AABB>) :
    CircuitComponents.DirectedComponentBlock(config, builder, aabb),
    StandardEntityBlocks.IStandardEntityBlock<ControlBoxBlockEntity> {

    override fun dropList(
        state: BlockState,
        world: Level,
        te: BlockEntity?,
        explosion: Boolean,
    ): List<ItemStack> {
        val stack = ItemStack(asItem())
        if (te is ControlBoxBlockEntity) {
            val tedata = te.writenbt(world.registryAccess(), net.minecraft.nbt.CompoundTag())
            if (
                tedata.contains("logic") &&
                    !tedata.getCompound("logic").getString("code").trim().isEmpty()
            ) {
                Auxiliaries.setItemStackNbt(stack, "tedata", tedata)
                Auxiliaries.setItemLabel(stack, te.getCustomName())
            }
        }
        return Collections.singletonList(stack)
    }

    override fun isBlockEntityTicking(world: Level, state: BlockState): Boolean = true

    @Environment(EnvType.CLIENT)
    override fun appendHoverText(
        stack: ItemStack,
        ctx: Item.TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag,
    ) {
        Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true)
        if (!Auxiliaries.Tooltip.extendedTipCondition()) return
        val nbt = Auxiliaries.getItemStackNbt(stack, "tedata")
        val nbtLogic = nbt.getCompound("tedata").getCompound("logic")
        if (nbtLogic.isEmpty) return
        Arrays.stream(nbtLogic.getString("code").split("\\n".toRegex()).toTypedArray())
            .map { s -> s.replace(Regex("#.*$"), "").trim() }
            .filter { s -> s.isNotEmpty() }
            .map { s -> Component.literal(s).withStyle(ChatFormatting.DARK_GREEN) }
            .forEach { tooltip.add(it) }
    }

    override fun setPlacedBy(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack,
    ) {
        if (world.isClientSide) return
        val nbt = Auxiliaries.getItemStackNbt(stack, "tedata")
        if (nbt.isEmpty) return
        val te = world.getBlockEntity(pos)
        if (te !is ControlBoxBlockEntity) return
        te.readnbt(world.registryAccess(), nbt)
        te.setCustomName(Auxiliaries.getItemLabel(stack))
        te.setChanged()
    }

    public override fun getSignal(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        redstoneSide: Direction,
    ): Int {
        val cb = world.getBlockEntity(pos) as? ControlBoxBlockEntity ?: return 0
        val internalSide =
            CircuitComponents.DirectedComponentBlock.getReverseStateMappedFacing(
                state,
                redstoneSide.opposite,
            )
        return cb.getOutputSignal(internalSide)
    }

    public override fun getDirectSignal(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        redstoneSide: Direction,
    ): Int = getSignal(state, world, pos, redstoneSide)

    override fun useWithoutItem(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        brh: BlockHitResult,
    ): InteractionResult = useOpenGui(state, world, pos, player)

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
            (world.getBlockEntity(pos) as? ControlBoxBlockEntity)?.toggle_trace(player)
            return ItemInteractionResult.CONSUME
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    internal fun notifyOutput(state: BlockState, world: Level, pos: BlockPos, dir: Direction) {
        notifyOutputNeighbourOfStateChange(state, world, pos, dir)
    }

    override fun update(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        fromPos: BlockPos?,
    ): BlockState {
        if (world.isClientSide) return state
        val cb = world.getBlockEntity(pos) as? ControlBoxBlockEntity ?: return state
        if (fromPos == null) {
            cb.scheduleImmediateTick()
            return state
        }
        val dp = fromPos.subtract(pos)
        val worldSide = Direction.fromDelta(dp.x, dp.y, dp.z)
        if (worldSide != null) {
            cb.signal_update(
                worldSide,
                CircuitComponents.DirectedComponentBlock.getReverseStateMappedFacing(
                    state,
                    worldSide,
                ),
            )
        }
        return state
    }
}
