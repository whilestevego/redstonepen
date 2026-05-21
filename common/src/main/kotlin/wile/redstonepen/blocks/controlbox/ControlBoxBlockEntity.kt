package wile.redstonepen.blocks.controlbox

import java.util.UUID
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
import net.minecraft.world.level.block.state.BlockState
import wile.redstonepen.blocks.CircuitComponents
import wile.redstonepen.blocks.StandardEntityBlocks
import wile.redstonepen.detail.RcaSync
import wile.redstonepen.net.Networking
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries

@Suppress("DEPRECATION")
class ControlBoxBlockEntity(pos: BlockPos, state: BlockState) :
    StandardEntityBlocks.StandardBlockEntity(
        requireNotNull(Registries.getBlockEntityTypeOfBlock(state.block)),
        pos,
        state,
    ),
    MenuProvider,
    Nameable,
    Networking.IPacketTileNotifyReceiver {

    companion object {
        const val TICK_INTERVAL = 4
    }

    private val blockInventory: Container = SimpleContainer(1)
    private val logic = ControlBoxLogic()
    private var activatingPlayer: UUID? = null
    private var customName: Component? = null
    private var trace = false
    private var tickTimer = 0
    private var tickInterval = 0
    private var tickErrorMessage: String? = null

    override fun readnbt(hlp: HolderLookup.Provider, nbt: CompoundTag): CompoundTag {
        if (nbt.contains("name", Tag.TAG_STRING.toInt())) {
            customName = Auxiliaries.unserializeTextComponent(nbt.getString("name"), hlp)
        }
        val logicdata =
            if (nbt.contains("logic", Tag.TAG_COMPOUND.toInt())) {
                nbt.getCompound("logic")
            } else {
                CompoundTag()
            }
        logic.code(logicdata.getString("code"))
        logic.input_data = logicdata.getInt("input")
        logic.output_data = logicdata.getInt("output")
        val logicsymbols =
            if (logicdata.contains("symbols", Tag.TAG_COMPOUND.toInt())) {
                logicdata.getCompound("symbols")
            } else {
                CompoundTag()
            }
        logic.clearSymbols()
        logicsymbols.allKeys.forEach { k -> logic.symbol(k, logicsymbols.getInt(k)) }
        activatingPlayer = if (nbt.hasUUID("player")) nbt.getUUID("player") else null
        return nbt
    }

    override fun writenbt(
        hlp: HolderLookup.Provider,
        nbt: CompoundTag,
        syncPacket: Boolean,
    ): CompoundTag {
        customName?.let { nbt.putString("name", Auxiliaries.serializeTextComponent(it, hlp)) }
        val logicdata = CompoundTag()
        logicdata.putString("code", logic.code())
        logicdata.putInt("input", logic.input_data)
        logicdata.putInt("output", logic.output_data)
        val logicsymbols = CompoundTag()
        logic.symbols().forEach { (k, v) -> logicsymbols.putInt(k, v) }
        logicdata.put("symbols", logicsymbols)
        nbt.put("logic", logicdata)
        activatingPlayer?.let { nbt.putUUID("player", it) }
        return nbt
    }

    override fun getName(): Component =
        customName ?: Component.translatable(blockState.block.descriptionId)

    override fun getCustomName(): Component? = customName

    override fun hasCustomName(): Boolean = (customName != null)

    fun setCustomName(name: Component?) {
        customName = name
    }

    override fun getDisplayName(): Component = super.getDisplayName()

    override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractContainerMenu =
        ControlBoxUiContainer(
            id,
            inventory,
            blockInventory,
            ContainerLevelAccess.create(
                requireNotNull(level) {
                    "BlockEntity must be attached to a level before createMenu is called"
                },
                worldPosition,
            ),
            SimpleContainerData(1),
        )

    override fun tick() {
        if (tickErrorMessage != null || --tickTimer > 0) return
        tickTimer = if (tickInterval > 0) tickInterval else TICK_INTERVAL
        val tick = System.nanoTime()
        val world = getLevel() ?: return
        val device_state = blockState
        val device_pos = blockPos
        val device_enabled =
            (device_state.getValue(CircuitComponents.DirectedComponentBlock.STATE) > 0) ||
                (device_state.getValue(CircuitComponents.DirectedComponentBlock.POWERED))
        if (device_state.block !is ControlBoxBlock) return
        val device_block = device_state.block as ControlBoxBlock
        val last_output_data = logic.output_data
        val last_input_data = logic.input_data
        val rca_data =
            if ((logic.rca_input_mask or logic.rca_output_mask) == 0L) {
                RcaSync.CommonRca.EMPTY
            } else {
                RcaSync.CommonRca.ofPlayer(activatingPlayer, false)
            }
        try {
            run {
                logic.input_data = 0
                for (d in Direction.entries) {
                    val world_dir =
                        CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(
                            device_state,
                            d,
                        )
                    if (device_enabled) {
                        val port_name = PortNames.ALL[d.ordinal]
                        if (logic.usesSymbol("${port_name}.co")) {
                            val target_pos = device_pos.relative(world_dir)
                            val target_state = world.getBlockState(target_pos)
                            logic.symbol(
                                "${port_name}.co",
                                if (target_state.hasAnalogOutputSignal()) {
                                    target_state.getAnalogOutputSignal(world, target_pos)
                                } else {
                                    0
                                },
                            )
                        }
                    }
                    if ((logic.output_mask and (0xf shl (4 * d.ordinal))) == 0) {
                        val p = world.getSignal(device_pos.relative(world_dir), world_dir)
                        logic.input_data = logic.input_data or ((p and 0xf) shl (4 * d.ordinal))
                    }
                }
                if (logic.rca_input_mask != 0L && rca_data != RcaSync.CommonRca.EMPTY) {
                    logic.rca_input_data = rca_data.client_inputs() and logic.rca_input_mask
                }
            }
            run {
                if (trace) {
                    logic.symbol(
                        ".perf1",
                        (Mth.clamp(System.nanoTime() - tick, 0L, 0x7fffffffL) / 1000L).toInt(),
                    )
                }
                if (!device_enabled) {
                    logic.output_data = 0
                } else {
                    logic.symbol(".clock", (world.gameTime and 0x7fffffffL).toInt())
                    logic.symbol(".time", (world.dayTime % 24000L).toInt())
                    logic.tick()
                    if (logic.rca_output_mask != 0L && rca_data != RcaSync.CommonRca.EMPTY) {
                        rca_data.server_outputs(logic.rca_output_data)
                    }
                }
            }
            run {
                if (logic.output_data != last_output_data) {
                    for (d in Direction.entries) {
                        if ((logic.output_mask and (0xf shl (4 * d.ordinal))) == 0) continue
                        val world_dir =
                            CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(
                                device_state,
                                d,
                            )
                        device_block.notifyOutput(device_state, world, device_pos, world_dir)
                    }
                }
                if (logic.output_data != last_output_data || logic.input_data != last_input_data) {
                    world.blockEntityChanged(device_pos)
                }
            }
        } catch (ex: Throwable) {
            val msg = "ControlBox halted: ${ex.message ?: ex.javaClass.simpleName}"
            Auxiliaries.logError(msg)
            tickErrorMessage = msg
            activatingPlayer?.let { uuid ->
                world.getPlayerByUUID(uuid)?.let { Auxiliaries.playerChatMessage(it, msg) }
            }
            setEnabled(false)
            setChanged()
            return
        }
        run {
            if (logic.symbols().containsKey("tickrate")) {
                tickInterval = Mth.clamp(logic.symbols().getOrDefault("tickrate", 0), 0, 200)
                if (tickInterval == 0) tickInterval = TICK_INTERVAL
            }
            tickTimer = tickInterval
            val dl = logic.symbol(".deadline")
            if (dl > 0 && dl < tickTimer) tickTimer = dl
            logic.risingEdgeInterrupts = 0
            logic.fallingEdgeInterrupts = 0
            if (trace) {
                logic.symbol(
                    ".perf2",
                    (Mth.clamp(System.nanoTime() - tick, 0L, 0x7fffffffL) / 1000L).toInt(),
                )
            }
        }
    }

    override fun onServerPacketReceived(nbt: CompoundTag) {
        val world = getLevel() ?: return
        readnbt(world.registryAccess(), nbt)
    }

    fun getEnabled(): Boolean =
        (blockState.getValue(CircuitComponents.DirectedComponentBlock.STATE) != 0) ||
            (blockState.getValue(CircuitComponents.DirectedComponentBlock.POWERED))

    fun setEnabled(en: Boolean) {
        if (en == getEnabled()) return
        val world = getLevel() ?: return
        world.setBlock(
            blockPos,
            blockState.setValue(CircuitComponents.DirectedComponentBlock.STATE, if (en) 1 else 0),
            1 or 2 or 16,
        )
        world.setBlock(
            blockPos,
            blockState.setValue(CircuitComponents.DirectedComponentBlock.POWERED, en),
            1 or 2 or 16,
        )
        if (!en) {
            logic.clearSymbols()
            val rca_data =
                if (logic.rca_output_mask == 0L) {
                    RcaSync.CommonRca.EMPTY
                } else {
                    RcaSync.CommonRca.ofPlayer(activatingPlayer, false)
                }
            if (rca_data != RcaSync.CommonRca.EMPTY) rca_data.server_outputs(0L)
        }
    }

    fun setRcaPlayerUUID(puid: UUID?) {
        activatingPlayer = if (puid == null) null else UUID.fromString(puid.toString())
    }

    fun getCode(): String = logic.code()

    fun setCode(text: String) {
        tickErrorMessage = null
        logic.code(text)
    }

    fun getOutputSignal(internalSide: Direction): Int =
        (logic.output_data shr (4 * internalSide.ordinal)) and 0xf

    internal fun scheduleImmediateTick() {
        tickTimer = 0
    }

    internal fun collectSyncData(full: Boolean): CompoundTag {
        val nbt = CompoundTag()
        nbt.putString("action", "serverdata")
        nbt.putBoolean("enabled", getEnabled())
        nbt.putInt("inputs", logic.input_mask)
        nbt.putInt("outputs", logic.output_mask)
        nbt.putInt(
            "ports",
            (logic.input_data and logic.input_mask) or (logic.output_data and logic.output_mask),
        )
        if (logic.symbols().isNotEmpty()) {
            val sym_nbt = CompoundTag()
            logic.symbols().forEach { (k, v) -> sym_nbt.putInt(k, v) }
            nbt.put("symbols", sym_nbt)
        }
        if (!logic.valid()) {
            val err_nbt = CompoundTag()
            logic.errors().forEach { (e, l) -> err_nbt.putString(e.toString(), l) }
            nbt.put("errors", err_nbt)
        } else {
            nbt.put("errors", CompoundTag())
        }
        tickErrorMessage?.let { nbt.putString("runtimeError", it) }
        if (!full) return nbt
        nbt.putBoolean("debug", trace_enabled())
        nbt.putString("code", getCode())
        activatingPlayer?.let { pid ->
            val run_player = getLevel()?.getPlayerByUUID(pid)
            nbt.putString("player", run_player?.scoreboardName ?: "")
        }
        return nbt
    }

    fun signal_update(from_world_side: Direction, from_mapped_side: Direction) {
        if (tickInterval > 0) return
        val shift = 4 * from_mapped_side.ordinal
        val mask = 0xf shl shift
        if ((logic.input_mask and mask) == 0) return
        val world = getLevel() ?: return
        val signal_intr =
            mask and
                (world.getSignal(blockPos.relative(from_world_side), from_world_side) shl shift)
        val signal_data = mask and logic.input_data
        if (signal_intr == signal_data) return
        if (signal_intr != 0 && signal_data == 0) {
            logic.risingEdgeInterrupts = logic.risingEdgeInterrupts or mask
            tickTimer = 0
        } else if (signal_intr == 0) {
            logic.fallingEdgeInterrupts = logic.fallingEdgeInterrupts or mask
            tickTimer = 0
        }
    }

    fun toggle_trace(player: Player?) {
        trace = !trace
        if (player != null) Auxiliaries.playerChatMessage(player, "Trace: $trace")
    }

    fun trace_enabled(): Boolean = trace

    class TestHooks {
        private val logic = ControlBoxLogic()
        private var tickErrorMessage: String? = null

        fun setCode(text: String): Boolean {
            tickErrorMessage = null
            return logic.code(text)
        }

        fun valid(): Boolean = logic.valid()

        fun errors(): Map<Int, String> = HashMap(logic.errors())

        fun inputMask(): Int = logic.input_mask

        fun outputMask(): Int = logic.output_mask

        fun setInput(side: Direction, value: Int) {
            val shift = 4 * side.ordinal
            val mask = 0xf shl shift
            logic.input_mask = logic.input_mask or mask
            logic.input_data = (logic.input_data and mask.inv()) or ((value and 0xf) shl shift)
        }

        fun setSymbol(key: String, value: Int) {
            logic.symbol(key, value)
        }

        fun getSymbol(key: String): Int = logic.symbol(key)

        fun tick() {
            logic.tick()
        }

        fun simulateTick() {
            if (tickErrorMessage != null) return
            try {
                logic.tick()
            } catch (ex: Throwable) {
                tickErrorMessage = ex.message ?: ex.javaClass.simpleName
            }
        }

        fun tickErrorMessage(): String? = tickErrorMessage

        fun injectTickError(message: String) {
            tickErrorMessage = message
        }

        fun output(side: Direction): Int = (logic.output_data shr (4 * side.ordinal)) and 0xf

        fun outputData(): Int = logic.output_data

        fun setRcaInput(channel: Int, value: Int) {
            val mask = 0xfL shl (channel * 4)
            logic.rca_input_mask = logic.rca_input_mask or mask
            logic.rca_input_data =
                (logic.rca_input_data and mask.inv()) or
                    ((value.toLong() and 0xfL) shl (channel * 4))
        }

        fun getRcaOutput(channel: Int): Int =
            ((logic.rca_output_data shr (channel * 4)) and 0xfL).toInt()

        fun rcaOutputData(): Long = logic.rca_output_data
    }
}
