package wile.redstonepen.items

import java.util.Locale
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ComparatorBlock
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.RepeaterBlock
import net.minecraft.world.level.block.entity.ComparatorBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import wile.redstonepen.ModConstants
import wile.redstonepen.ModContent
import wile.redstonepen.blocks.track.RedstoneTrackBlock
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.client.Overlay
import wile.redstonepen.util.Auxiliaries
import wile.redstonepen.util.Inventories
import wile.redstonepen.util.RsSignals

class RedstonePenItem(properties: Item.Properties) : StandardItems.BaseItem(properties) {

    @Environment(EnvType.CLIENT)
    override fun appendHoverText(
        stack: ItemStack,
        ctx: Item.TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag,
    ) {
        if (stack.maxDamage > 0) {
            tooltip.add(
                Auxiliaries.localizable(
                    "item.${ModConstants.MODID}.pen.tooltip.numstored",
                    stack.maxDamage - stack.damageValue,
                )
            )
        } else {
            tooltip.add(
                Auxiliaries.localizable("item.${ModConstants.MODID}.pen.tooltip.rsfrominventory")
            )
        }
        Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true)
    }

    override fun getEnchantmentValue(): Int = 0

    override fun isValidRepairItem(toRepair: ItemStack, repair: ItemStack): Boolean = false

    override fun isBarVisible(stack: ItemStack): Boolean =
        stack.isDamageableItem && stack.damageValue > 0

    override fun getBarWidth(stack: ItemStack): Int =
        if (stack.maxDamage <= 0) {
            13
        } else {
            13 - Mth.clamp(Math.round(13f * stack.damageValue / stack.maxDamage), 0, 13)
        }

    override fun getBarColor(stack: ItemStack): Int = 0x663333

    override fun doesSneakBypassUse(
        stack: ItemStack,
        world: LevelReader,
        pos: BlockPos,
        player: Player,
    ): Boolean = true

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float =
        if (state.block.defaultDestroyTime() < 0.5f) 10000f else 0f

    override fun canAttackBlock(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
    ): Boolean {
        // Hand needs to be guessed here.
        var stack = player.getItemInHand(player.usedItemHand)
        if (!isPen(stack)) stack = player.mainHandItem
        if (!isPen(stack)) stack = player.offhandItem
        if (isPen(stack)) attack(stack, pos, player)
        return false
    }

    override fun onBlockStartBreak(stack: ItemStack, pos: BlockPos, player: Player): Boolean {
        attack(stack, pos, player)
        return false
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player
        val hand = context.hand
        val pos = context.clickedPos
        val facing = context.clickedFace
        val world = context.level
        val state = world.getBlockState(pos)
        val stack = context.itemInHand
        // Add to track
        if (state.block is RedstoneTrackBlock) {
            val track = state.block as RedstoneTrackBlock
            if (world.isClientSide) return InteractionResult.SUCCESS
            val rtr =
                BlockHitResult(
                    context.clickLocation,
                    context.clickedFace,
                    context.clickedPos,
                    context.isInside,
                )
            return track.modifySegments(
                state,
                world,
                pos,
                player ?: return InteractionResult.FAIL,
                stack,
                hand,
                rtr,
                false,
                true,
            )
        }
        // Check if a new track can be placed.
        if (!RedstoneTrackBlock.canBePlacedOnFace(state, world, pos, facing)) {
            return InteractionResult.FAIL
        }
        if (world.isClientSide) return InteractionResult.SUCCESS
        // Place new track
        val targetPos = pos.relative(facing)
        val targetState = world.getBlockState(targetPos)
        if (targetState.block is RedstoneTrackBlock) {
            val trackBlock = targetState.block as RedstoneTrackBlock
            val rtr =
                BlockHitResult(
                    context.clickLocation,
                    context.clickedFace,
                    targetPos,
                    context.isInside,
                )
            return trackBlock.modifySegments(
                targetState,
                world,
                targetPos,
                player ?: return InteractionResult.FAIL,
                stack,
                hand,
                rtr,
                false,
                true,
            )
        } else {
            val rtr =
                BlockHitResult(
                    context.clickLocation,
                    context.clickedFace,
                    targetPos,
                    context.isInside,
                )
            val ctx =
                BlockPlaceContext(
                    player ?: return InteractionResult.FAIL,
                    context.hand,
                    ItemStack(Items.REDSTONE),
                    rtr,
                )
            val rsState =
                ModContent.References.TRACK_BLOCK.getStateForPlacement(ctx)
                    ?: return InteractionResult.FAIL
            if (!targetState.canBeReplaced(ctx)) return InteractionResult.FAIL
            if (!world.setBlock(targetPos, rsState, 1 or 2 or 16)) return InteractionResult.FAIL
            val placedState = world.getBlockState(targetPos)
            return if (placedState.block is RedstoneTrackBlock) {
                val trackBlock = placedState.block as RedstoneTrackBlock
                if (
                    trackBlock.modifySegments(
                        targetState,
                        world,
                        targetPos,
                        player,
                        stack,
                        hand,
                        rtr,
                        false,
                        true,
                    ) == InteractionResult.FAIL
                ) {
                    InteractionResult.FAIL
                } else {
                    InteractionResult.CONSUME
                }
            } else {
                world.removeBlock(targetPos, false)
                InteractionResult.FAIL
            }
        }
    }

    override fun inventoryTick(
        stack: ItemStack,
        world: Level,
        entity: Entity,
        itemSlot: Int,
        isSelected: Boolean,
    ) {
        if (
            !isSelected ||
                !entity.isShiftKeyDown ||
                world.isClientSide ||
                (world.gameTime and 0x1L) != 0L ||
                entity !is ServerPlayer
        ) {
            return
        }
        val rt = entity.pick(10.0, 0f, false)
        if (rt.type != HitResult.Type.BLOCK) return
        val brtr = rt as BlockHitResult
        val pos = brtr.blockPos
        val state = world.getBlockState(pos)
        val block = state.block
        val rsSide = brtr.direction.opposite
        var tc: MutableComponent = Component.empty()
        when {
            block == Blocks.REDSTONE_WIRE ->
                tc =
                    Auxiliaries.localizable(
                        "overlay.wire_power",
                        powerFormatted(state.getValue(RedStoneWireBlock.POWER)),
                    )
            block == ModContent.References.TRACK_BLOCK -> {
                val te: TrackBlockEntity =
                    RedstoneTrackBlock.tile(world, pos).orElse(null) ?: return
                tc =
                    Auxiliaries.localizable(
                        "overlay.track_power",
                        powerFormatted(te.getSidePower(rsSide)),
                    )
                if (Auxiliaries.isDevelopmentMode()) {
                    tc.append(
                        Component.literal(
                            String.format(Locale.ROOT, " | flags: %016x, p: ", te.getStateFlags())
                        )
                    )
                    tc.append(
                        Component.literal(
                            Direction.values().joinToString(",") { side ->
                                side.toString().substring(0, 1) +
                                    te.getRedstonePower(side.opposite, false)
                            }
                        )
                    )
                }
            }
            state.`is`(Blocks.REPEATER) -> {
                tc =
                    Auxiliaries.localizable(
                        "overlay.direct_power",
                        powerFormatted(if (state.getValue(RepeaterBlock.POWERED)) 15 else 0),
                    )
                tc.append(
                    Auxiliaries.localizable(
                        "overlay.repeater_delay",
                        state.getValue(RepeaterBlock.DELAY),
                    )
                )
            }
            state.`is`(Blocks.COMPARATOR) -> {
                val te = world.getBlockEntity(pos)
                if (te is ComparatorBlockEntity) {
                    tc =
                        Auxiliaries.localizable(
                            "overlay.direct_power",
                            powerFormatted(te.outputSignal),
                        )
                    when (state.getValue(ComparatorBlock.MODE)) {
                        net.minecraft.world.level.block.state.properties.ComparatorMode.COMPARE ->
                            tc.append(Auxiliaries.localizable("overlay.comparator_compare"))
                        net.minecraft.world.level.block.state.properties.ComparatorMode.SUBTRACT ->
                            tc.append(Auxiliaries.localizable("overlay.comparator_subtract"))
                        else -> {}
                    }
                }
            }
            state.isSignalSource -> {
                var p =
                    maxOf(
                        state.getDirectSignal(world, pos, rsSide),
                        state.getSignal(world, pos, rsSide),
                    )
                if (p > 0) {
                    tc = Auxiliaries.localizable("overlay.direct_power", powerFormatted(p))
                } else {
                    var maxSide: Direction? = null
                    for (side in Direction.values()) {
                        if (side == rsSide) continue
                        val ps =
                            maxOf(
                                state.getDirectSignal(world, pos, side),
                                state.getSignal(world, pos, side),
                            )
                        if (ps > p) {
                            p = ps
                            maxSide = side
                            if (p >= 15) break
                        }
                    }
                    tc =
                        if (p == 0 || maxSide == null) {
                            Auxiliaries.localizable("overlay.direct_power", powerFormatted(p))
                        } else {
                            Auxiliaries.localizable(
                                "overlay.direct_power_at",
                                powerFormatted(p),
                                maxSide.opposite.toString(),
                            )
                        }
                }
            }
            RsSignals.canEmitWeakPower(state, world, pos, rsSide) -> {
                var maxSide = Direction.values()[0]
                var p = 0
                for (d in Direction.values()) {
                    val ps = world.getSignal(pos.relative(d), d)
                    if (ps > p) {
                        p = ps
                        maxSide = d
                        if (p >= 15) break
                    }
                }
                if (p > 0) {
                    tc =
                        Auxiliaries.localizable(
                            "overlay.indirect_power",
                            powerFormatted(p),
                            maxSide.toString(),
                        )
                }
            }
        }
        if (Auxiliaries.isDevelopmentMode()) {
            val lookDir = Direction.orderedByNearest(entity)[0].toString().substring(0, 1)
            tc.append(
                Component.literal(
                    String.format(Locale.ROOT, " | %s [%d,%d,%d]", lookDir, pos.x, pos.y, pos.z)
                )
            )
        }
        Overlay.show(entity, tc, 400)
    }

    private fun attack(stack: ItemStack, pos: BlockPos, player: Player): Boolean {
        val world = player.commandSenderWorld
        val state = world.getBlockState(pos)
        if (state.`is`(ModContent.References.TRACK_BLOCK)) {
            val rt = player.pick(10.0, 0f, false)
            if (rt.type != HitResult.Type.BLOCK) return false
            val hand =
                if (player.getItemInHand(InteractionHand.MAIN_HAND).item === this) {
                    InteractionHand.MAIN_HAND
                } else {
                    InteractionHand.OFF_HAND
                }
            if (state.block !is RedstoneTrackBlock) return false
            (state.block as RedstoneTrackBlock).modifySegments(
                state,
                world,
                pos,
                player,
                stack,
                hand,
                rt as BlockHitResult,
                true,
                false,
            )
            return true
        } else if (state.`is`(Blocks.REDSTONE_WIRE)) {
            pushRedstone(stack, 1, player)
            world.removeBlock(pos, false)
            return true
        }
        return false
    }

    private fun powerFormatted(p: Int): String = String.format(Locale.ROOT, "%02d", p)

    companion object {
        @JvmStatic
        fun pushRedstone(stack: ItemStack, amount: Int, player: Player) {
            if (player.isCreative) return
            if (amount <= 0) return
            if (isPen(stack)) {
                if (stack.maxDamage <= 0) {
                    val remaining =
                        Inventories.insert(player, ItemStack(Items.REDSTONE, amount), false)
                    if (!remaining.isEmpty) Inventories.give(player, remaining)
                } else if (stack.damageValue >= amount) {
                    stack.damageValue = stack.damageValue - amount
                } else {
                    val left = amount - stack.damageValue
                    stack.damageValue = 0
                    Inventories.give(player, ItemStack(Items.REDSTONE, left))
                }
            } else if (stack.item === Items.REDSTONE) {
                if (stack.count <= stack.maxStackSize - amount) {
                    stack.grow(amount)
                } else {
                    Inventories.give(player, ItemStack(Items.REDSTONE, amount))
                }
            } else {
                Inventories.give(player, ItemStack(Items.REDSTONE, amount))
            }
        }

        @JvmStatic
        fun popRedstone(stack: ItemStack, amount: Int, player: Player, hand: InteractionHand): Int {
            if (player.isCreative) return amount
            if (amount <= 0) return 0
            var remaining = amount
            if (isPen(stack)) {
                if (stack.maxDamage > 0) {
                    val dmg = stack.damageValue + remaining
                    if (dmg >= stack.maxDamage) {
                        remaining = stack.maxDamage - stack.damageValue
                        player.setItemInHand(hand, ItemStack.EMPTY)
                    } else {
                        stack.damageValue = dmg
                    }
                } else {
                    remaining =
                        Inventories.extract(player, ItemStack(Items.REDSTONE), remaining, false)
                            .count
                }
            } else if (stack.item === Items.REDSTONE) {
                if (stack.count <= remaining) {
                    remaining = stack.count
                    player.setItemInHand(hand, ItemStack.EMPTY)
                } else {
                    stack.shrink(remaining)
                }
            }
            return remaining
        }

        @JvmStatic
        fun hasEnoughRedstone(stack: ItemStack, amount: Int, player: Player): Boolean {
            if (player.isCreative) return true
            return when {
                isPen(stack) ->
                    if (stack.maxDamage > 0) {
                        stack.damageValue < (stack.maxDamage - amount)
                    } else {
                        Inventories.extract(player, ItemStack(Items.REDSTONE), amount, true)
                            .count >= amount
                    }
                stack.item === Items.REDSTONE -> stack.count >= amount
                else -> false
            }
        }

        @JvmStatic
        fun isFullRedstone(stack: ItemStack): Boolean {
            if (isPen(stack)) return stack.damageValue <= 0
            if (stack.item === Items.REDSTONE) return stack.count >= stack.maxStackSize
            return false
        }

        @JvmStatic fun isPen(stack: ItemStack): Boolean = stack.item is RedstonePenItem
    }
}
