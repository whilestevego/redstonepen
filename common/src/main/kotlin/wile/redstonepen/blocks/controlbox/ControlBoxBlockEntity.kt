package wile.redstonepen.blocks.controlbox

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.Container
import net.minecraft.world.MenuProvider
import net.minecraft.world.Nameable
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import wile.redstonepen.blocks.CircuitComponents
import wile.redstonepen.blocks.StandardEntityBlocks
import wile.redstonepen.detail.RcaSync
import wile.redstonepen.net.Networking
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries
import java.util.UUID

@Suppress("DEPRECATION")
class ControlBoxBlockEntity(pos: BlockPos, state: BlockState) :
    StandardEntityBlocks.StandardBlockEntity(Registries.getBlockEntityTypeOfBlock(state.block)!!, pos, state),
    MenuProvider, Nameable, Networking.IPacketTileNotifyReceiver {

    companion object {
        const val TICK_INTERVAL = 4
    }

    private val block_inventory_: Container = SimpleContainer(1)
    private val logic_ = ControlBoxLogic()
    private var activating_player_: UUID? = null
    private var custom_name_: Component? = null
    private var trace_ = false
    private var tick_timer_ = 0
    private var tick_interval_ = 0

    override fun readnbt(hlp: HolderLookup.Provider, nbt: CompoundTag): CompoundTag {
        if (nbt.contains("name", Tag.TAG_STRING.toInt())) custom_name_ = Auxiliaries.unserializeTextComponent(nbt.getString("name"), hlp)
        val logic_data = if (nbt.contains("logic", Tag.TAG_COMPOUND.toInt())) nbt.getCompound("logic") else CompoundTag()
        logic_.code(logic_data.getString("code"))
        logic_.input_data = logic_data.getInt("input")
        logic_.output_data = logic_data.getInt("output")
        val logic_symbols = if (logic_data.contains("symbols", Tag.TAG_COMPOUND.toInt())) logic_data.getCompound("symbols") else CompoundTag()
        logic_.clearSymbols()
        logic_symbols.allKeys.forEach { k -> logic_.symbol(k, logic_symbols.getInt(k)) }
        activating_player_ = if (nbt.hasUUID("player")) nbt.getUUID("player") else null
        return nbt
    }

    override fun writenbt(hlp: HolderLookup.Provider, nbt: CompoundTag, syncPacket: Boolean): CompoundTag {
        if (custom_name_ != null) nbt.putString("name", Auxiliaries.serializeTextComponent(custom_name_!!, hlp))
        val logic_data = CompoundTag()
        logic_data.putString("code", logic_.code())
        logic_data.putInt("input", logic_.input_data)
        logic_data.putInt("output", logic_.output_data)
        val logic_symbols = CompoundTag()
        logic_.symbols().forEach { (k, v) -> logic_symbols.putInt(k, v) }
        logic_data.put("symbols", logic_symbols)
        nbt.put("logic", logic_data)
        if (activating_player_ != null) nbt.putUUID("player", activating_player_!!)
        return nbt
    }

    override fun getName(): Component {
        if (custom_name_ != null) return custom_name_!!
        return Component.translatable(blockState.block.descriptionId)
    }

    override fun getCustomName(): Component? = custom_name_

    override fun hasCustomName(): Boolean = (custom_name_ != null)

    fun setCustomName(name: Component?) { custom_name_ = name }

    override fun getDisplayName(): Component = super<Nameable>.getDisplayName()

    override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractContainerMenu =
        ControlBoxUiContainer(id, inventory, block_inventory_, ContainerLevelAccess.create(level, worldPosition), SimpleContainerData(1))

    override fun tick() {
        if (--tick_timer_ > 0) return
        tick_timer_ = if (tick_interval_ > 0) tick_interval_ else TICK_INTERVAL
        val tick = System.nanoTime()
        val world = getLevel()!!
        val device_state = blockState
        val device_pos = blockPos
        val device_enabled = (device_state.getValue(CircuitComponents.DirectedComponentBlock.STATE) > 0) || (device_state.getValue(CircuitComponents.DirectedComponentBlock.POWERED))
        if (device_state.block !is ControlBoxBlock) return
        val device_block = device_state.block as ControlBoxBlock
        val last_output_data = logic_.output_data
        val last_input_data = logic_.input_data
        val rca_data = if ((logic_.rca_input_mask or logic_.rca_output_mask) == 0L) RcaSync.CommonRca.EMPTY else RcaSync.CommonRca.ofPlayer(activating_player_, false)
        try {
            run {
                logic_.input_data = 0
                for (d in Direction.values()) {
                    val world_dir = CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(device_state, d)
                    if (device_enabled) {
                        val port_name = Defs.PORT_NAMES[d.ordinal]
                        if (logic_.usesSymbol(port_name + ".co")) {
                            val target_pos = device_pos.relative(world_dir)
                            val target_state = world.getBlockState(target_pos)
                            logic_.symbol(port_name + ".co", if (target_state.hasAnalogOutputSignal()) target_state.getAnalogOutputSignal(world, target_pos) else 0)
                        }
                    }
                    if ((logic_.output_mask and (0xf shl (4 * d.ordinal))) == 0) {
                        val p = world.getSignal(device_pos.relative(world_dir), world_dir)
                        logic_.input_data = logic_.input_data or ((p and 0xf) shl (4 * d.ordinal))
                    }
                }
                if (logic_.rca_input_mask != 0L && rca_data != RcaSync.CommonRca.EMPTY) logic_.rca_input_data = rca_data.client_inputs() and logic_.rca_input_mask
            }
            run {
                if (trace_) logic_.symbol(".perf1", (Mth.clamp(System.nanoTime() - tick, 0L, 0x7fffffffL) / 1000L).toInt())
                if (!device_enabled) {
                    logic_.output_data = 0
                } else {
                    logic_.symbol(".clock", (world.gameTime and 0x7fffffffL).toInt())
                    logic_.symbol(".time", (world.dayTime % 24000L).toInt())
                    logic_.tick()
                    if (logic_.rca_output_mask != 0L && rca_data != RcaSync.CommonRca.EMPTY) rca_data.server_outputs(logic_.rca_output_data)
                }
            }
            run {
                if (logic_.output_data != last_output_data) {
                    for (d in Direction.values()) {
                        if ((logic_.output_mask and (0xf shl (4 * d.ordinal))) == 0) continue
                        val world_dir = CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(device_state, d)
                        device_block.notifyOutput(device_state, world, device_pos, world_dir)
                    }
                }
                if (logic_.output_data != last_output_data || logic_.input_data != last_input_data) world.blockEntityChanged(device_pos)
            }
        } catch (ex: Throwable) {
            Auxiliaries.logError("RLC tick exception!" + ex)
            world.removeBlock(blockPos, true)
            return
        }
        run {
            if (logic_.symbols().containsKey("tickrate")) {
                tick_interval_ = Mth.clamp(logic_.symbols().getOrDefault("tickrate", 0), 0, 200)
                if (tick_interval_ == 0) tick_interval_ = TICK_INTERVAL
            }
            tick_timer_ = tick_interval_
            val dl = logic_.symbol(".deadline")
            if (dl > 0 && dl < tick_timer_) tick_timer_ = dl
            logic_.intr_redges = 0
            logic_.intr_fedges = 0
            if (trace_) logic_.symbol(".perf2", (Mth.clamp(System.nanoTime() - tick, 0L, 0x7fffffffL) / 1000L).toInt())
        }
    }

    override fun onServerPacketReceived(nbt: CompoundTag) { readnbt(getLevel()!!.registryAccess(), nbt) }
    override fun onClientPacketReceived(player: Player, nbt: CompoundTag) { readnbt(getLevel()!!.registryAccess(), nbt) }

    fun getEnabled(): Boolean =
        (blockState.getValue(CircuitComponents.DirectedComponentBlock.STATE) != 0) || (blockState.getValue(CircuitComponents.DirectedComponentBlock.POWERED))

    fun setEnabled(en: Boolean) {
        if (en == getEnabled()) return
        getLevel()!!.setBlock(blockPos, blockState.setValue(CircuitComponents.DirectedComponentBlock.STATE, if (en) 1 else 0), 1 or 2 or 16)
        getLevel()!!.setBlock(blockPos, blockState.setValue(CircuitComponents.DirectedComponentBlock.POWERED, en), 1 or 2 or 16)
        if (!en) {
            logic_.clearSymbols()
            val rca_data = if (logic_.rca_output_mask == 0L) RcaSync.CommonRca.EMPTY else RcaSync.CommonRca.ofPlayer(activating_player_, false)
            if (rca_data != RcaSync.CommonRca.EMPTY) rca_data.server_outputs(0L)
        }
    }

    fun setRcaPlayerUUID(puid: UUID?) { activating_player_ = if (puid == null) null else UUID.fromString(puid.toString()) }

    fun getCode(): String = logic_.code()

    fun setCode(text: String) { logic_.code(text) }

    fun getOutputSignal(internalSide: Direction): Int = (logic_.output_data shr (4 * internalSide.ordinal)) and 0xf

    internal fun scheduleImmediateTick() { tick_timer_ = 0 }

    internal fun collectSyncData(full: Boolean): CompoundTag {
        val world = getLevel()!!
        val nbt = CompoundTag()
        nbt.putString("action", "serverdata")
        nbt.putBoolean("enabled", getEnabled())
        nbt.putInt("inputs", logic_.input_mask)
        nbt.putInt("outputs", logic_.output_mask)
        nbt.putInt("ports", (logic_.input_data and logic_.input_mask) or (logic_.output_data and logic_.output_mask))
        if (logic_.symbols().isNotEmpty()) {
            val sym_nbt = CompoundTag()
            logic_.symbols().forEach { (k, v) -> sym_nbt.putInt(k, v) }
            nbt.put("symbols", sym_nbt)
        }
        if (!logic_.valid()) {
            val err_nbt = CompoundTag()
            logic_.errors().forEach { (e, l) -> err_nbt.putString(e.toString(), l) }
            nbt.put("errors", err_nbt)
        } else {
            nbt.put("errors", CompoundTag())
        }
        if (!full) return nbt
        nbt.putBoolean("debug", trace_enabled())
        nbt.putString("code", getCode())
        if (activating_player_ != null) {
            val run_player = world.getPlayerByUUID(activating_player_!!)
            nbt.putString("player", if (run_player == null) "" else run_player.scoreboardName)
        }
        return nbt
    }

    fun signal_update(from_world_side: Direction, from_mapped_side: Direction) {
        if (tick_interval_ > 0) return
        val shift = 4 * from_mapped_side.ordinal
        val mask = 0xf shl shift
        if ((logic_.input_mask and mask) == 0) return
        val signal_intr = mask and (getLevel()!!.getSignal(blockPos.relative(from_world_side), from_world_side) shl shift)
        val signal_data = mask and logic_.input_data
        if (signal_intr == signal_data) return
        if (signal_intr != 0 && signal_data == 0) {
            logic_.intr_redges = logic_.intr_redges or mask
            tick_timer_ = 0
        } else if (signal_intr == 0) {
            logic_.intr_fedges = logic_.intr_fedges or mask
            tick_timer_ = 0
        }
    }

    fun toggle_trace(player: Player?) { trace_ = !trace_; if (player != null) Auxiliaries.playerChatMessage(player, "Trace: $trace_") }

    fun trace_enabled(): Boolean = trace_

    class TestHooks {
        private val logic_ = ControlBoxLogic()

        fun setCode(text: String): Boolean = logic_.code(text)
        fun valid(): Boolean = logic_.valid()
        fun errors(): Map<Int, String> = HashMap(logic_.errors())
        fun inputMask(): Int = logic_.input_mask
        fun outputMask(): Int = logic_.output_mask

        fun setInput(side: Direction, value: Int) {
            val shift = 4 * side.ordinal
            val mask = 0xf shl shift
            logic_.input_mask = logic_.input_mask or mask
            logic_.input_data = (logic_.input_data and mask.inv()) or ((value and 0xf) shl shift)
        }

        fun setSymbol(key: String, value: Int) { logic_.symbol(key, value) }
        fun getSymbol(key: String): Int = logic_.symbol(key)
        fun tick() { logic_.tick() }
        fun output(side: Direction): Int = (logic_.output_data shr (4 * side.ordinal)) and 0xf
        fun outputData(): Int = logic_.output_data

        fun setRcaInput(channel: Int, value: Int) {
            val mask = 0xfL shl (channel * 4)
            logic_.rca_input_mask = logic_.rca_input_mask or mask
            logic_.rca_input_data = (logic_.rca_input_data and mask.inv()) or ((value.toLong() and 0xfL) shl (channel * 4))
        }

        fun getRcaOutput(channel: Int): Int = ((logic_.rca_output_data shr (channel * 4)) and 0xfL).toInt()
        fun rcaOutputData(): Long = logic_.rca_output_data
    }
}
