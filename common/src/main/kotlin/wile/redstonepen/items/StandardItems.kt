package wile.redstonepen.items

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import wile.redstonepen.util.Auxiliaries

/** Common functionality classes for mod items. */
object StandardItems {

    interface IStandardItem

    open class BaseItem(properties: Properties) : Item(properties), IStandardItem {
        open fun doesSneakBypassUse(
            stack: ItemStack,
            world: LevelReader,
            pos: BlockPos,
            player: Player,
        ): Boolean = false

        open fun onBlockStartBreak(stack: ItemStack, pos: BlockPos, player: Player): Boolean = false

        override fun useOn(context: UseOnContext): InteractionResult =
            onItemUseFirst(context.itemInHand, context)

        open fun onItemUseFirst(stack: ItemStack, context: UseOnContext): InteractionResult =
            InteractionResult.PASS
    }

    open class BaseBlockItem(block: Block, properties: Item.Properties) :
        BlockItem(block, properties) {
        @Environment(EnvType.CLIENT)
        override fun appendHoverText(
            stack: ItemStack,
            ctx: Item.TooltipContext,
            tooltip: MutableList<Component>,
            flag: TooltipFlag,
        ) {
            Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true)
        }

        override fun useOn(context: UseOnContext): InteractionResult {
            val ir = onItemUseFirst(context.itemInHand, context)
            if (ir != InteractionResult.PASS) return ir
            return super.useOn(context)
        }

        open fun onItemUseFirst(stack: ItemStack, context: UseOnContext): InteractionResult =
            InteractionResult.PASS
    }
}
