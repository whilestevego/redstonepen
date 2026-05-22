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
        logic.inputData = logicdata.getInt("input")
        logic.outputData = logicdata.getInt("output")
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
        logicdata.putInt("input", logic.inputData)
        logicdata.putInt("output", logic.outputData)
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
        val deviceState = blockState
        val devicePos = blockPos
        val deviceEnabled =
            (deviceState.getValue(CircuitComponents.DirectedComponentBlock.STATE) > 0) ||
                (deviceState.getValue(CircuitComponents.DirectedComponentBlock.POWERED))
        if (deviceState.block !is ControlBoxBlock) return
        val deviceBlock = deviceState.block as ControlBoxBlock
        val lastOutputData = logic.outputData
        val lastInputData = logic.inputData
        val rcaData =
            if ((logic.rcaInputMask or logic.rcaOutputMask) == 0L) {
                RcaSync.CommonRca.EMPTY
            } else {
                RcaSync.CommonRca.ofPlayer(activatingPlayer, false)
            }
        try {
            run {
                logic.inputData = 0
                for (d in Direction.entries) {
                    val worldDir =
                        CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(
                            deviceState,
                            d,
                        )
                    if (deviceEnabled) {
                        val portName = PortNames.ALL[d.ordinal]
                        if (logic.usesSymbol("${portName}.co")) {
                            val targetPos = devicePos.relative(worldDir)
                            val targetState = world.getBlockState(targetPos)
                            logic.symbol(
                                "${portName}.co",
                                if (targetState.hasAnalogOutputSignal()) {
                                    targetState.getAnalogOutputSignal(world, targetPos)
                                } else {
                                    0
                                },
                            )
                        }
                    }
                    if ((logic.outputMask and (0xf shl (4 * d.ordinal))) == 0) {
                        val p = world.getSignal(devicePos.relative(worldDir), worldDir)
                        logic.inputData = logic.inputData or ((p and 0xf) shl (4 * d.ordinal))
                    }
                }
                if (logic.rcaInputMask != 0L && rcaData != RcaSync.CommonRca.EMPTY) {
                    logic.rcaInputData = rcaData.client_inputs() and logic.rcaInputMask
                }
            }
            run {
                if (trace) {
                    logic.symbol(
                        ".perf1",
                        (Mth.clamp(System.nanoTime() - tick, 0L, 0x7fffffffL) / 1000L).toInt(),
                    )
                }
                if (!deviceEnabled) {
                    logic.outputData = 0
                } else {
                    logic.symbol(".clock", (world.gameTime and 0x7fffffffL).toInt())
                    logic.symbol(".time", (world.dayTime % 24000L).toInt())
                    logic.tick()
                    if (logic.rcaOutputMask != 0L && rcaData != RcaSync.CommonRca.EMPTY) {
                        rcaData.server_outputs(logic.rcaOutputData)
                    }
                }
            }
            run {
                if (logic.outputData != lastOutputData) {
                    for (d in Direction.entries) {
                        if ((logic.outputMask and (0xf shl (4 * d.ordinal))) == 0) continue
                        val worldDir =
                            CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(
                                deviceState,
                                d,
                            )
                        deviceBlock.notifyOutput(deviceState, world, devicePos, worldDir)
                    }
                }
                if (logic.outputData != lastOutputData || logic.inputData != lastInputData) {
                    world.blockEntityChanged(devicePos)
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
            val rcaData =
                if (logic.rcaOutputMask == 0L) {
                    RcaSync.CommonRca.EMPTY
                } else {
                    RcaSync.CommonRca.ofPlayer(activatingPlayer, false)
                }
            if (rcaData != RcaSync.CommonRca.EMPTY) rcaData.server_outputs(0L)
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
        (logic.outputData shr (4 * internalSide.ordinal)) and 0xf

    internal fun scheduleImmediateTick() {
        tickTimer = 0
    }

    internal fun collectSyncData(full: Boolean): CompoundTag {
        val nbt = CompoundTag()
        nbt.putString("action", "serverdata")
        nbt.putBoolean("enabled", getEnabled())
        nbt.putInt("inputs", logic.inputMask)
        nbt.putInt("outputs", logic.outputMask)
        nbt.putInt(
            "ports",
            (logic.inputData and logic.inputMask) or (logic.outputData and logic.outputMask),
        )
        if (logic.symbols().isNotEmpty()) {
            val symNbt = CompoundTag()
            logic.symbols().forEach { (k, v) -> symNbt.putInt(k, v) }
            nbt.put("symbols", symNbt)
        }
        if (!logic.valid()) {
            val errNbt = CompoundTag()
            logic.errors().forEach { (e, l) -> errNbt.putString(e.toString(), l) }
            nbt.put("errors", errNbt)
        } else {
            nbt.put("errors", CompoundTag())
        }
        tickErrorMessage?.let { nbt.putString("runtimeError", it) }
        if (!full) return nbt
        nbt.putBoolean("debug", trace_enabled())
        nbt.putString("code", getCode())
        activatingPlayer?.let { pid ->
            val runPlayer = getLevel()?.getPlayerByUUID(pid)
            nbt.putString("player", runPlayer?.scoreboardName ?: "")
        }
        return nbt
    }

    fun signal_update(fromWorldSide: Direction, fromMappedSide: Direction) {
        if (tickInterval > 0) return
        val shift = 4 * fromMappedSide.ordinal
        val mask = 0xf shl shift
        if ((logic.inputMask and mask) == 0) return
        val world = getLevel() ?: return
        val signalIntr =
            mask and (world.getSignal(blockPos.relative(fromWorldSide), fromWorldSide) shl shift)
        val signalData = mask and logic.inputData
        if (signalIntr == signalData) return
        if (signalIntr != 0 && signalData == 0) {
            logic.risingEdgeInterrupts = logic.risingEdgeInterrupts or mask
            tickTimer = 0
        } else if (signalIntr == 0) {
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

        fun inputMask(): Int = logic.inputMask

        fun outputMask(): Int = logic.outputMask

        fun setInput(side: Direction, value: Int) {
            val shift = 4 * side.ordinal
            val mask = 0xf shl shift
            logic.inputMask = logic.inputMask or mask
            logic.inputData = (logic.inputData and mask.inv()) or ((value and 0xf) shl shift)
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

        fun output(side: Direction): Int = (logic.outputData shr (4 * side.ordinal)) and 0xf

        fun outputData(): Int = logic.outputData

        fun setRcaInput(channel: Int, value: Int) {
            val mask = 0xfL shl (channel * 4)
            logic.rcaInputMask = logic.rcaInputMask or mask
            logic.rcaInputData =
                (logic.rcaInputData and mask.inv()) or ((value.toLong() and 0xfL) shl (channel * 4))
        }

        fun getRcaOutput(channel: Int): Int =
            ((logic.rcaOutputData shr (channel * 4)) and 0xfL).toInt()

        fun rcaOutputData(): Long = logic.rcaOutputData
    }
}
