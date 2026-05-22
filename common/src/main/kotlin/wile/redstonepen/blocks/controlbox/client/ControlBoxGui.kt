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
import wile.redstonepen.blocks.controlbox.PortNames
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

    private val valueUpdateInterval = 2
    private val tooltipPrefix: String = ModContent.References.CONTROLBOX_BLOCK.descriptionId
    private val textbox: GuiTextEditing.MultiLineTextBox
    private val startStop: Guis.CheckBox
    private val cbCopyAll: Guis.ImageButton
    private val cbPasteAll: Guis.ImageButton
    private val cbErrorIndicator: Guis.Image
    private val rcaEnabledIndicator: Guis.Image
    private val portStati: MutableList<Guis.TextBox> = ArrayList()
    private val portStatiIIndicators: MutableList<Guis.Image> = ArrayList()
    private val portStatiOIndicators: MutableList<Guis.Image> = ArrayList()
    private val symbols: MutableMap<String, Int> = HashMap()
    private val errors: MutableList<Tuple<Int, String>> = ArrayList()
    private var runtimeError: String = ""
    private var updateCounter: Int = 0
    private var focusEditor: Boolean = false
    private var debugEnabled: Boolean = false
    private var codeRequested: Boolean = false
    private var activatingPlayer: Component = Component.empty()

    init {
        titleLabelX = 17
        titleLabelY = -10
        startStop =
            Guis.CheckBox(
                getBackgroundImage(),
                12,
                12,
                Guis.Coord2d.of(15, 213),
                Guis.Coord2d.of(28, 213),
            )
        cbCopyAll = Guis.ImageButton(getBackgroundImage(), 12, 12, Guis.Coord2d.of(41, 213))
        cbPasteAll = Guis.ImageButton(getBackgroundImage(), 12, 12, Guis.Coord2d.of(54, 213))
        cbErrorIndicator = Guis.Image(getBackgroundImage(), 5, 2, Guis.Coord2d.of(68, 213))
        rcaEnabledIndicator = Guis.Image(getBackgroundImage(), 7, 7, Guis.Coord2d.of(90, 215))
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
            .setFontColor(COLOR_TEXT)
            .setCursorColor(COLOR_TEXT)
            .setLineHeight(7)
            .onValueChanged { push_code(textbox.getValue()) }
        addRenderableWidget(textbox)
        startStop
            .init(this, Guis.Coord2d.of(196, 14))
            .tooltip(Auxiliaries.localizable("$tooltipPrefix.tooltips.runstop"))
        startStop.onclick { _ ->
            val nbt = CompoundTag()
            val rca = wile.api.rca.FmmRedstoneClientAdapter.Adapter.instance()
            if (rca != null && rca.isOpen()) nbt.putBoolean("withrca", true)
            onGuiAction("enabled", nbt)
            focusEditor = true
        }
        addRenderableWidget(startStop)
        cbCopyAll
            .init(this, Guis.Coord2d.of(212, 14))
            .tooltip(Auxiliaries.localizable("$tooltipPrefix.tooltips.copyall"))
        cbCopyAll.onclick { _ ->
            Auxiliaries.setClipboard(textbox.getValue())
            focusEditor = true
        }
        cbCopyAll.visible = false
        addRenderableWidget(cbCopyAll)
        cbPasteAll
            .init(this, Guis.Coord2d.of(212, 14))
            .tooltip(Auxiliaries.localizable("$tooltipPrefix.tooltips.pasteall"))
        cbPasteAll.onclick { _ ->
            textbox.setValue(Auxiliaries.getClipboard().orElse(""))
            push_code(textbox.getValue())
            focusEditor = true
        }
        cbPasteAll.visible = false
        addRenderableWidget(cbPasteAll)
        cbErrorIndicator.init(this, Guis.Coord2d.of(230, 14))
        cbErrorIndicator.visible = false
        addRenderableWidget(cbErrorIndicator)
        rcaEnabledIndicator.init(this, Guis.Coord2d.of(194, 40))
        rcaEnabledIndicator.visible = false
        rcaEnabledIndicator.tooltip { _ ->
            Auxiliaries.localizable("$tooltipPrefix.tooltips.rcaplayer", activatingPlayer)
        }
        addRenderableWidget(rcaEnabledIndicator)

        val ygap = 12
        val x0 = getGuiLeft() + 205
        val y0 = getGuiTop() + 56
        val lineyMap = intArrayOf(5 * ygap, 4 * ygap, 0, 2 * ygap, 3 * ygap, ygap)
        portStati.clear()
        portStatiIIndicators.clear()
        portStatiOIndicators.clear()
        portStati.add(Guis.TextBox(x0, y0 + lineyMap[0], 30, 10, Component.literal("down"), font))
        portStati.add(Guis.TextBox(x0, y0 + lineyMap[1], 30, 10, Component.literal("up"), font))
        portStati.add(Guis.TextBox(x0, y0 + lineyMap[2], 30, 10, Component.literal("red"), font))
        portStati.add(Guis.TextBox(x0, y0 + lineyMap[3], 30, 10, Component.literal("yellow"), font))
        portStati.add(Guis.TextBox(x0, y0 + lineyMap[4], 30, 10, Component.literal("green"), font))
        portStati.add(Guis.TextBox(x0, y0 + lineyMap[5], 30, 10, Component.literal("blue"), font))
        for (i in portStati.indices) {
            val tb = portStati[i]
            tb.setEditable(false)
            tb.setBordered(false)
            tb.setTextColor(COLOR_TEXT_ARGB)
            tb.setTextColorUneditable(COLOR_TEXT_ARGB)
            tb.setValue(
                String.format(Locale.ROOT, "%1s=00", PortNames.ALL[i].uppercase(Locale.ROOT))
            )
            addRenderableWidget(tb)
            val imgI = Guis.Image(getBackgroundImage(), 3, 6, Guis.Coord2d.of(78, 215))
            imgI.init(this, Guis.Coord2d.of(191, 56 + lineyMap[i]))
            portStatiIIndicators.add(imgI)
            addRenderableWidget(imgI)
            val imgO = Guis.Image(getBackgroundImage(), 3, 6, Guis.Coord2d.of(84, 215))
            imgO.init(this, Guis.Coord2d.of(189, 56 + lineyMap[i]))
            portStatiOIndicators.add(imgO)
            addRenderableWidget(imgO)
        }

        val tooltips = mutableListOf<TooltipDisplay.TipRange>()
        tooltips.add(
            TooltipDisplay.TipRange(getGuiLeft() + 200, getGuiTop() + 36, 36, 16) {
                val c = Component.literal("")
                symbols.entries
                    .sortedBy { it.key }
                    .forEach { (k, v) ->
                        val isInternalSymbol =
                            k.startsWith(".") ||
                                PortNames.ALL.contains(k) ||
                                k.endsWith(".re") ||
                                k.endsWith(".fe")
                        if (!debugEnabled && isInternalSymbol) return@forEach
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
                if (errors.isEmpty()) {
                    Component.empty()
                } else {
                    Auxiliaries.localizable("$tooltipPrefix.error.${errors[0].b}")
                }
            }
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 12,
                5,
                8,
                Auxiliaries.localizable("$tooltipPrefix.help.1"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 22,
                5,
                3,
                Auxiliaries.localizable("$tooltipPrefix.help.2"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 27,
                5,
                5,
                Auxiliaries.localizable("$tooltipPrefix.help.3"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 34,
                5,
                5,
                Auxiliaries.localizable("$tooltipPrefix.help.4"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 41,
                5,
                6,
                Auxiliaries.localizable("$tooltipPrefix.help.5"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 49,
                5,
                4,
                Auxiliaries.localizable("$tooltipPrefix.help.6"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 55,
                5,
                5,
                Auxiliaries.localizable("$tooltipPrefix.help.7"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 62,
                5,
                3,
                Auxiliaries.localizable("$tooltipPrefix.help.8"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 67,
                5,
                7,
                Auxiliaries.localizable("$tooltipPrefix.help.9"),
            )
        )
        tooltips.add(
            TooltipDisplay.TipRange(
                getGuiLeft() + 18,
                getGuiTop() + 76,
                5,
                3,
                Auxiliaries.localizable("$tooltipPrefix.help.10"),
            )
        )
        tooltip.init(tooltips).delay(50)

        setInitialFocus(textbox)
        focused = textbox
        textbox.active = false
        onGuiAction("serverdata")
    }

    override fun containerTick() {
        val nbt = menu.fetchReceivedServerData()
        if (!nbt.isEmpty) {
            if (nbt.contains("ports")) {
                val mask = nbt.getInt("inputs") or nbt.getInt("outputs")
                val io = nbt.getInt("ports")
                for (i in PortNames.ALL.indices) {
                    if ((mask and (0xf shl (4 * i))) == 0) continue
                    portStati[i].setValue(
                        String.format(
                            Locale.ROOT,
                            "%1s=%02d",
                            PortNames.ALL[i].uppercase(Locale.ROOT),
                            (io shr (4 * i)) and 0xf,
                        )
                    )
                }
            }
            if (nbt.contains("code")) {
                textbox.setValue(nbt.getString("code"))
                focusEditor = true
            }
            if (nbt.contains("enabled")) {
                startStop.checked(nbt.getBoolean("enabled"))
                focusEditor = true
            }
            if (nbt.contains("debug")) {
                debugEnabled = nbt.getBoolean("debug")
            }
            if (nbt.contains("inputs")) {
                var mask = nbt.getInt("inputs")
                for (i in PortNames.ALL.indices) {
                    portStatiIIndicators[i].visible = (mask and 0xf) != 0
                    mask = mask shr 4
                }
            }
            if (nbt.contains("outputs")) {
                var mask = nbt.getInt("outputs")
                for (i in PortNames.ALL.indices) {
                    portStatiOIndicators[i].visible = (mask and 0xf) != 0
                    mask = mask shr 4
                }
            }
            if (nbt.contains("symbols", Tag.TAG_COMPOUND.toInt())) {
                val symNbt = nbt.getCompound("symbols")
                symbols.clear()
                symNbt.allKeys.forEach { k -> symbols[k] = symNbt.getInt(k) }
            }
            if (nbt.contains("errors", Tag.TAG_COMPOUND.toInt())) {
                val errNbt = nbt.getCompound("errors")
                errors.clear()
                errNbt.allKeys.forEach { k ->
                    try {
                        errors.add(Tuple(k.toInt(), errNbt.getString(k)))
                    } catch (_: Throwable) {}
                }
                if (errors.isEmpty()) {
                    cbErrorIndicator.visible = false
                    cbErrorIndicator.setX(0)
                    cbErrorIndicator.setY(0)
                    cbErrorIndicator.tooltip(Component.empty())
                } else {
                    val exy = textbox.getCoordinatesAtIndex(errors[0].a)
                    cbErrorIndicator.tooltip(
                        Auxiliaries.localizable("$tooltipPrefix.error.${errors[0].b}")
                    )
                    cbErrorIndicator.visible = true
                    cbErrorIndicator.setX(exy.x)
                    cbErrorIndicator.setY(exy.y + textbox.getLineHeight())
                }
            }
            runtimeError = nbt.getString("runtimeError")
            if (runtimeError.isNotEmpty() && errors.isEmpty()) {
                val exy = textbox.getCoordinatesAtIndex(0)
                cbErrorIndicator.tooltip(Component.literal(runtimeError))
                cbErrorIndicator.visible = true
                cbErrorIndicator.setX(exy.x)
                cbErrorIndicator.setY(exy.y + textbox.getLineHeight())
            }
            if (nbt.contains("player", Tag.TAG_STRING.toInt())) {
                val playerName = nbt.getString("player")
                if (playerName.isEmpty()) {
                    activatingPlayer = Component.empty()
                    rcaEnabledIndicator.visible = false
                    rcaEnabledIndicator.active = false
                } else {
                    activatingPlayer = Component.literal(playerName)
                    rcaEnabledIndicator.visible = true
                    rcaEnabledIndicator.active = true
                }
            }
        } else if (--updateCounter <= 0) {
            updateCounter = valueUpdateInterval
            if (!codeRequested) {
                codeRequested = true
                onGuiAction("serverdata")
            } else {
                onGuiAction("servervalues")
            }
        }

        startStop.active = errors.isEmpty() && runtimeError.isEmpty()
        if (!startStop.active) startStop.checked(false) else cbErrorIndicator.visible = false
        textbox.active = !startStop.checked()
        textbox.setFontColor(if (textbox.active) COLOR_TEXT_ACTIVE else COLOR_TEXT_INACTIVE)
        cbPasteAll.visible = textbox.active && textbox.getValue().trim().isEmpty()
        cbCopyAll.visible = !cbPasteAll.visible
        if (focusEditor) {
            focusEditor = false
            if (!isDragging && !textbox.isFocused) {
                children().forEach { child ->
                    if (child != textbox && child is AbstractWidget) child.setFocused(false)
                }
                focused = textbox
            }
        }
    }

    override fun render(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.render(gg, mouseX, mouseY, partialTicks)
    }

    override fun renderLabels(gg: GuiGraphics, x: Int, y: Int) {
        gg.drawString(font, title, titleLabelX + 1, titleLabelY + 1, COLOR_TITLE_SHADOW)
        gg.drawString(font, title, titleLabelX, titleLabelY, COLOR_TITLE)
    }

    override fun slotClicked(hoveredSlot: Slot, hoveredIndex: Int, no: Int, clickType: ClickType) {}

    private fun push_code(text: String) {
        val nbt = CompoundTag()
        nbt.putString("code", text)
        onGuiAction("codeupdate", nbt)
    }

    companion object {
        private const val COLOR_TITLE_SHADOW = 0x303030 // dark grey drop shadow behind the title
        private const val COLOR_TITLE = 0x707070 // medium grey title label text
        private const val COLOR_TEXT = 0xdddddd // light grey editor font and cursor
        private const val COLOR_TEXT_ACTIVE = 0xeeeeee // near-white font when editor is editable
        private const val COLOR_TEXT_INACTIVE = 0x999999 // dimmed font when editor is read-only
        @Suppress("MagicNumber")
        private val COLOR_TEXT_ARGB =
            (0xff shl 24) or COLOR_TEXT // fully opaque ARGB form of COLOR_TEXT
    }
}
