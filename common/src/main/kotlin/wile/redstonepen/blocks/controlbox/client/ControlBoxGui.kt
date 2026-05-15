package wile.redstonepen.blocks.controlbox.client

import java.util.Locale
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.util.Tuple
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import wile.redstonepen.ModContent
import wile.redstonepen.blocks.controlbox.ControlBoxUiContainer
import wile.redstonepen.blocks.controlbox.Defs
import wile.redstonepen.client.GuiTextEditing
import wile.redstonepen.client.Guis
import wile.redstonepen.client.TooltipDisplay
import wile.redstonepen.net.NetworkingClient
import wile.redstonepen.util.Auxiliaries

@Environment(EnvType.CLIENT)
class ControlBoxGui(
    container: ControlBoxUiContainer,
    playerInventory: Inventory,
    title: Component,
) :
    Guis.ContainerGui<ControlBoxUiContainer>(
        container,
        playerInventory,
        title,
        "textures/gui/control_box_gui.png",
        238,
        206,
    ) {

    private val VALUE_UPDATE_INTERVAL = 2
    private val tooltip_prefix: String = ModContent.References.CONTROLBOX_BLOCK.descriptionId
    private val textbox: GuiTextEditing.MultiLineTextBox
    private val start_stop: Guis.CheckBox
    private val cb_copy_all: Guis.ImageButton
    private val cb_paste_all: Guis.ImageButton
    private val cb_error_indicator: Guis.Image
    private val rca_enabled_indicator: Guis.Image
    private val port_stati: MutableList<Guis.TextBox> = ArrayList()
    private val port_stati_i_indicators: MutableList<Guis.Image> = ArrayList()
    private val port_stati_o_indicators: MutableList<Guis.Image> = ArrayList()
    private val symbols_: MutableMap<String, Int> = HashMap()
    private val errors_: MutableList<Tuple<Int, String>> = ArrayList()
    private var runtimeError_: String = ""
    private var update_counter_: Int = 0
    private var focus_editor_: Boolean = false
    private var debug_enabled_: Boolean = false
    private var code_requested_: Boolean = false
    private var activating_player_: Component = Component.empty()

    init {
        titleLabelX = 17
        titleLabelY = -10
        start_stop =
            Guis.CheckBox(
                getBackgroundImage(),
                12,
                12,
                Guis.Coord2d.of(15, 213),
                Guis.Coord2d.of(28, 213),
            )
        cb_copy_all = Guis.ImageButton(getBackgroundImage(), 12, 12, Guis.Coord2d.of(41, 213))
        cb_paste_all = Guis.ImageButton(getBackgroundImage(), 12, 12, Guis.Coord2d.of(54, 213))
        cb_error_indicator = Guis.Image(getBackgroundImage(), 5, 2, Guis.Coord2d.of(68, 213))
        rca_enabled_indicator = Guis.Image(getBackgroundImage(), 7, 7, Guis.Coord2d.of(90, 215))
        textbox = GuiTextEditing.MultiLineTextBox(29, 12, 156, 170, Component.literal("Code"))
    }

    private fun onGuiAction(message: String) = onGuiAction(message, CompoundTag())

    private fun onGuiAction(message: String, nbt: CompoundTag) {
        nbt.putString("action", message)
        NetworkingClient.PacketContainerSyncClientToServer.sendToServer(menu.containerId, nbt)
    }

    override fun init() {
        super.init()
        textbox
            .init(this, Guis.Coord2d.of(29, 12))
            .setFontColor(0xdddddd)
            .setCursorColor(0xdddddd)
            .setLineHeight(7)
            .onValueChanged { push_code(textbox.getValue()) }
        addRenderableWidget(textbox)
        start_stop
            .init(this, Guis.Coord2d.of(196, 14))
            .tooltip(Auxiliaries.localizable("$tooltip_prefix.tooltips.runstop"))
        start_stop.onclick { _ ->
            if (runtimeError_.isNotEmpty()) {
                onGuiAction("resume")
            } else {
                val nbt = CompoundTag()
                val rca = wile.api.rca.FmmRedstoneClientAdapter.Adapter.instance()
                if (rca != null && rca.isOpen()) nbt.putBoolean("withrca", true)
                onGuiAction("enabled", nbt)
            }
            focus_editor_ = true
        }
        addRenderableWidget(start_stop)
        cb_copy_all
            .init(this, Guis.Coord2d.of(212, 14))
            .tooltip(Auxiliaries.localizable("$tooltip_prefix.tooltips.copyall"))
        cb_copy_all.onclick { _ ->
            Auxiliaries.setClipboard(textbox.getValue())
            focus_editor_ = true
        }
        cb_copy_all.visible = false
        addRenderableWidget(cb_copy_all)
        cb_paste_all
            .init(this, Guis.Coord2d.of(212, 14))
            .tooltip(Auxiliaries.localizable("$tooltip_prefix.tooltips.pasteall"))
        cb_paste_all.onclick { _ ->
            textbox.setValue(Auxiliaries.getClipboard().orElse(""))
            push_code(textbox.getValue())
            focus_editor_ = true
        }
        cb_paste_all.visible = false
        addRenderableWidget(cb_paste_all)
        cb_error_indicator.init(this, Guis.Coord2d.of(230, 14))
        cb_error_indicator.visible = false
        addRenderableWidget(cb_error_indicator)
        rca_enabled_indicator.init(this, Guis.Coord2d.of(194, 40))
        rca_enabled_indicator.visible = false
        rca_enabled_indicator.tooltip { _ ->
            Auxiliaries.localizable("$tooltip_prefix.tooltips.rcaplayer", activating_player_)
        }
        addRenderableWidget(rca_enabled_indicator)

        val ygap = 12
        val x0 = getGuiLeft() + 205
        val y0 = getGuiTop() + 56
        val lineyMap = intArrayOf(5 * ygap, 4 * ygap, 0, 2 * ygap, 3 * ygap, ygap)
        port_stati.clear()
        port_stati_i_indicators.clear()
        port_stati_o_indicators.clear()
        port_stati.add(Guis.TextBox(x0, y0 + lineyMap[0], 30, 10, Component.literal("down"), font))
        port_stati.add(Guis.TextBox(x0, y0 + lineyMap[1], 30, 10, Component.literal("up"), font))
        port_stati.add(Guis.TextBox(x0, y0 + lineyMap[2], 30, 10, Component.literal("red"), font))
        port_stati.add(
            Guis.TextBox(x0, y0 + lineyMap[3], 30, 10, Component.literal("yellow"), font)
        )
        port_stati.add(Guis.TextBox(x0, y0 + lineyMap[4], 30, 10, Component.literal("green"), font))
        port_stati.add(Guis.TextBox(x0, y0 + lineyMap[5], 30, 10, Component.literal("blue"), font))
        for (i in port_stati.indices) {
            val tb = port_stati[i]
            tb.setEditable(false)
            tb.setBordered(false)
            tb.setTextColor(0xffdddddd.toInt())
            tb.setTextColorUneditable(0xffdddddd.toInt())
            tb.setValue(
                String.format(Locale.ROOT, "%1s=00", Defs.PORT_NAMES[i].uppercase(Locale.ROOT))
            )
            addRenderableWidget(tb)
            val imgI = Guis.Image(getBackgroundImage(), 3, 6, Guis.Coord2d.of(78, 215))
            imgI.init(this, Guis.Coord2d.of(191, 56 + lineyMap[i]))
            port_stati_i_indicators.add(imgI)
            addRenderableWidget(imgI)
            val imgO = Guis.Image(getBackgroundImage(), 3, 6, Guis.Coord2d.of(84, 215))
            imgO.init(this, Guis.Coord2d.of(189, 56 + lineyMap[i]))
            port_stati_o_indicators.add(imgO)
            addRenderableWidget(imgO)
        }

        val tooltips = mutableListOf<TooltipDisplay.TipRange>()
        tooltips.add(
            TooltipDisplay.TipRange(getGuiLeft() + 200, getGuiTop() + 36, 36, 16) {
                val c = Component.literal("")
                symbols_.entries
                    .sortedBy { it.key }
                    .forEach { (k, v) ->
                        val isInternalSymbol =
                            k.startsWith(".") ||
                                Defs.PORT_NAMES.contains(k) ||
                                k.endsWith(".re") ||
                                k.endsWith(".fe")
                        if (!debug_enabled_ && isInternalSymbol) return@forEach
                        val lf = if (c.siblings.isEmpty()) "" else "\n"
                        c.siblings.add(
                            Component.literal(
                                String.format(
                                    Locale.ROOT,
                                    "%s%s = %d",
                                    lf,
                                    k.uppercase(Locale.ROOT),
                                    v,
                                )
                            )
                        )
                    }
                c
            }
        )
        tooltips.add(
            TooltipDisplay.TipRange(getGuiLeft() + 196, getGuiTop() + 14, 16, 16) {
                if (errors_.isEmpty()) {
                    Component.empty()
                } else {
                    Auxiliaries.localizable("$tooltip_prefix.error.${errors_[0].b}")
                }
            }
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 12,
                5,
                8,
                Auxiliaries.localizable("$tooltip_prefix.help.1"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 22,
                5,
                3,
                Auxiliaries.localizable("$tooltip_prefix.help.2"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 27,
                5,
                5,
                Auxiliaries.localizable("$tooltip_prefix.help.3"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 34,
                5,
                5,
                Auxiliaries.localizable("$tooltip_prefix.help.4"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 41,
                5,
                6,
                Auxiliaries.localizable("$tooltip_prefix.help.5"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 49,
                5,
                4,
                Auxiliaries.localizable("$tooltip_prefix.help.6"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 55,
                5,
                5,
                Auxiliaries.localizable("$tooltip_prefix.help.7"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 62,
                5,
                3,
                Auxiliaries.localizable("$tooltip_prefix.help.8"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 67,
                5,
                7,
                Auxiliaries.localizable("$tooltip_prefix.help.9"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 76,
                5,
                3,
                Auxiliaries.localizable("$tooltip_prefix.help.10"),
            )
        )
        tooltip_.init(tooltips).delay(50)

        setInitialFocus(textbox)
        setFocused(textbox)
        textbox.active = false
        onGuiAction("serverdata")
    }

    override fun containerTick() {
        val nbt = menu.fetchReceivedServerData()
        if (!nbt.isEmpty) {
            if (nbt.contains("ports")) {
                val mask = nbt.getInt("inputs") or nbt.getInt("outputs")
                val io = nbt.getInt("ports")
                for (i in Defs.PORT_NAMES.indices) {
                    if ((mask and (0xf shl (4 * i))) == 0) continue
                    port_stati[i].setValue(
                        String.format(
                            Locale.ROOT,
                            "%1s=%02d",
                            Defs.PORT_NAMES[i].uppercase(Locale.ROOT),
                            (io shr (4 * i)) and 0xf,
                        )
                    )
                }
            }
            if (nbt.contains("code")) {
                textbox.setValue(nbt.getString("code"))
                focus_editor_ = true
            }
            if (nbt.contains("enabled")) {
                start_stop.checked(nbt.getBoolean("enabled"))
                focus_editor_ = true
            }
            if (nbt.contains("debug")) {
                debug_enabled_ = nbt.getBoolean("debug")
            }
            if (nbt.contains("inputs")) {
                var mask = nbt.getInt("inputs")
                for (i in Defs.PORT_NAMES.indices) {
                    port_stati_i_indicators[i].visible = (mask and 0xf) != 0
                    mask = mask shr 4
                }
            }
            if (nbt.contains("outputs")) {
                var mask = nbt.getInt("outputs")
                for (i in Defs.PORT_NAMES.indices) {
                    port_stati_o_indicators[i].visible = (mask and 0xf) != 0
                    mask = mask shr 4
                }
            }
            if (nbt.contains("symbols", Tag.TAG_COMPOUND.toInt())) {
                val symNbt = nbt.getCompound("symbols")
                symbols_.clear()
                symNbt.allKeys.forEach { k -> symbols_[k] = symNbt.getInt(k) }
            }
            if (nbt.contains("errors", Tag.TAG_COMPOUND.toInt())) {
                val errNbt = nbt.getCompound("errors")
                errors_.clear()
                errNbt.allKeys.forEach { k ->
                    try {
                        errors_.add(Tuple(k.toInt(), errNbt.getString(k)))
                    } catch (_: Throwable) {}
                }
                if (errors_.isEmpty()) {
                    cb_error_indicator.visible = false
                    cb_error_indicator.setX(0)
                    cb_error_indicator.setY(0)
                    cb_error_indicator.tooltip(Component.empty())
                } else {
                    val exy = textbox.getCoordinatesAtIndex(errors_[0].a)
                    cb_error_indicator.tooltip(
                        Auxiliaries.localizable("$tooltip_prefix.error.${errors_[0].b}")
                    )
                    cb_error_indicator.visible = true
                    cb_error_indicator.setX(exy.x)
                    cb_error_indicator.setY(exy.y + textbox.getLineHeight())
                }
            }
            runtimeError_ = nbt.getString("runtimeError")
            if (runtimeError_.isNotEmpty() && errors_.isEmpty()) {
                val exy = textbox.getCoordinatesAtIndex(0)
                cb_error_indicator.tooltip(Component.literal(runtimeError_))
                cb_error_indicator.visible = true
                cb_error_indicator.setX(exy.x)
                cb_error_indicator.setY(exy.y + textbox.getLineHeight())
            }
            if (nbt.contains("player", Tag.TAG_STRING.toInt())) {
                val playerName = nbt.getString("player")
                if (playerName.isEmpty()) {
                    activating_player_ = Component.empty()
                    rca_enabled_indicator.visible = false
                    rca_enabled_indicator.active = false
                } else {
                    activating_player_ = Component.literal(playerName)
                    rca_enabled_indicator.visible = true
                    rca_enabled_indicator.active = true
                }
            }
        } else if (--update_counter_ <= 0) {
            update_counter_ = VALUE_UPDATE_INTERVAL
            if (!code_requested_) {
                code_requested_ = true
                onGuiAction("serverdata")
            } else {
                onGuiAction("servervalues")
            }
        }

        start_stop.active = errors_.isEmpty()
        if (!start_stop.active || runtimeError_.isNotEmpty()) start_stop.checked(false)
        if (errors_.isEmpty() && runtimeError_.isEmpty()) cb_error_indicator.visible = false
        textbox.active = !start_stop.checked()
        textbox.setFontColor(if (textbox.active) 0xeeeeee else 0x999999)
        cb_paste_all.visible = textbox.active && textbox.getValue().trim().isEmpty()
        cb_copy_all.visible = !cb_paste_all.visible
        if (focus_editor_) {
            focus_editor_ = false
            if (!isDragging && !textbox.isFocused) {
                children().forEach { child ->
                    if (child != textbox && child is AbstractWidget) child.setFocused(false)
                }
                setFocused(textbox)
            }
        }
    }

    override fun render(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.render(gg, mouseX, mouseY, partialTicks)
    }

    override fun renderLabels(gg: GuiGraphics, x: Int, y: Int) {
        gg.drawString(font, title, titleLabelX + 1, titleLabelY + 1, 0x303030)
        gg.drawString(font, title, titleLabelX, titleLabelY, 0x707070)
    }

    override fun slotClicked(
        hoveredSlot: Slot?,
        hoveredIndex: Int,
        no: Int,
        clickType: ClickType,
    ) {}

    private fun push_code(text: String) {
        val nbt = CompoundTag()
        nbt.putString("code", text)
        onGuiAction("codeupdate", nbt)
    }
}
