@file:Suppress("DEPRECATION")
package wile.redstonepen.items

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import wile.redstonepen.ModConstants
import wile.redstonepen.blocks.controlbox.ControlBoxBlock
import wile.redstonepen.blocks.controlbox.ControlBoxBlockEntity
import wile.redstonepen.client.Overlay
import wile.redstonepen.util.Auxiliaries

class RemoteItem(properties: Item.Properties) : StandardItems.BaseItem(properties) {

    @Environment(EnvType.CLIENT)
    override fun appendHoverText(stack: ItemStack, ctx: Item.TooltipContext, tooltip: MutableList<Component>, flag: TooltipFlag) {
        val data = getRemoteData(stack)
        if (data != null) {
            tooltip.add(Auxiliaries.localizable("item.${ModConstants.MODID}.remote.tooltip.linkedto", data.pos.x, data.pos.y, data.pos.z, Component.translatable(data.name)))
        } else {
            tooltip.add(Auxiliaries.localizable("item.${ModConstants.MODID}.remote.tooltip.notlinked"))
        }
        Auxiliaries.Tooltip.addInformation(stack, ctx, tooltip, flag, true)
    }

    override fun doesSneakBypassUse(stack: ItemStack, world: LevelReader, pos: BlockPos, player: Player): Boolean = false
    override fun isBarVisible(stack: ItemStack): Boolean = false

    override fun canAttackBlock(state: net.minecraft.world.level.block.state.BlockState, world: Level, pos: BlockPos, player: Player): Boolean {
        val stack = if (player.mainHandItem.item is RemoteItem) player.mainHandItem else player.offhandItem
        attack(stack, pos, player)
        return false
    }

    override fun onBlockStartBreak(stack: ItemStack, pos: BlockPos, player: Player): Boolean {
        attack(stack, pos, player)
        return false
    }

    override fun getDestroySpeed(stack: ItemStack, state: net.minecraft.world.level.block.state.BlockState): Float = 10000f

    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (world !is ServerLevel) return InteractionResultHolder.success(player.getItemInHand(hand))
        if (player !is ServerPlayer) return InteractionResultHolder.fail(player.getItemInHand(hand))
        onTriggerRemoteLink(world, player, player.getItemInHand(hand))
        return InteractionResultHolder.fail(player.getItemInHand(hand))
    }

    override fun onItemUseFirst(stack: ItemStack, context: UseOnContext): InteractionResult {
        if (context.level !is ServerLevel) return InteractionResult.SUCCESS
        if (context.player !is ServerPlayer) return InteractionResult.CONSUME
        onTriggerRemoteLink(context.level as ServerLevel, context.player as ServerPlayer, stack)
        return InteractionResult.CONSUME
    }

    private fun onTriggerRemoteLink(world: ServerLevel, player: ServerPlayer, stack: ItemStack) {
        val data = getRemoteData(stack) ?: return
        val sound = { event: SoundEvent, pitch: Float -> world.playSound(null, player.blockPosition(), event, SoundSource.PLAYERS, 0.25f, pitch) }
        val fail = { sound(SoundEvents.ENDERMAN_HURT, 1.8f) }
        val pos = data.pos
        if (!world.isLoaded(pos)) { fail(); return }
        val state = world.getBlockState(pos)
        if (!state.hasProperty(BlockStateProperties.POWERED)) { fail(); return }
        val block = state.block
        val powered = state.getValue(BlockStateProperties.POWERED)
        when (block) {
            is ButtonBlock -> {
                if (powered) return
                block.press(state, world, pos, null)
                sound(SoundEvents.STONE_BUTTON_CLICK_ON, 1.7f)
            }
            is LeverBlock -> {
                block.pull(state, world, pos, null)
                sound(SoundEvents.LEVER_CLICK, if (powered) 1.3f else 1.5f)
            }
            is ControlBoxBlock -> {
                val te = world.getBlockEntity(pos)
                if (te !is ControlBoxBlockEntity) { fail(); return }
                te.setEnabled(!te.getEnabled())
                sound(SoundEvents.LEVER_CLICK, if (te.getEnabled()) 1.5f else 1.3f)
            }
            else -> fail()
        }
    }

    private data class RemoteData(val pos: BlockPos, val name: String)

    private fun getRemoteData(stack: ItemStack): RemoteData? {
        val nbt = Auxiliaries.getItemStackNbt(stack, "remote") ?: return null
        if (!nbt.contains("pos", 99) || !nbt.contains("name", 8)) return null
        return RemoteData(BlockPos.of(nbt.getLong("pos")), nbt.getString("name"))
    }

    private fun setRemoteData(stack: ItemStack, pos: BlockPos, name: String) {
        val nbt = CompoundTag()
        nbt.putLong("pos", pos.asLong())
        nbt.putString("name", name)
        Auxiliaries.setItemStackNbt(stack, "remote", nbt)
    }

    private fun attack(stack: ItemStack, pos: BlockPos, player: Player): Boolean {
        if (stack.item !is RemoteItem) return false
        if (player !is ServerPlayer) return false
        val state = player.serverLevel().getBlockState(pos)
        if (state.block is LeverBlock || state.block is ButtonBlock || state.block is ControlBoxBlock) {
            val name = state.block.descriptionId
            setRemoteData(stack, pos, name)
            Overlay.show(player, Auxiliaries.localizable("overlay.remote_saved", pos.x, pos.y, pos.z, Component.translatable(name)), 1500)
        }
        return true
    }
}
