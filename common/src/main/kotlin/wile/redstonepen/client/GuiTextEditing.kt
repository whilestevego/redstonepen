package wile.redstonepen.client

import com.google.common.collect.Lists
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.StringSplitter
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.font.TextFieldHelper
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.Mth
import net.minecraft.util.StringUtil
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.mutable.MutableBoolean
import org.apache.commons.lang3.mutable.MutableInt
import java.util.Arrays
import java.util.function.BiConsumer
import java.util.function.Consumer

@Environment(EnvType.CLIENT)
object GuiTextEditing {

    @Environment(EnvType.CLIENT)
    class MultiLineTextBox(x: Int, y: Int, width: Int, height: Int, title: Component)
        : Guis.UiWidget(x, y, width, height, title), GuiEventListener {

        private var frame_tick_: Int = 0
        private var last_clicked_: Long = 0
        private var last_index_: Int = -1
        private val edit_: TextFieldHelper
        private var text_: String = ""
        private var font_: Font? = null
        private var line_height_: Int = NORM_LINE_HEIGHT
        private var font_scale_: Float = 1f
        private var max_text_size_: Int = 1024
        private var font_color_: Int = 0xff000000.toInt()
        private var cursor_color_: Int = 0xff000000.toInt()
        private var on_changed_: Consumer<MultiLineTextBox> = Consumer {}
        private var on_mouse_move_: BiConsumer<MultiLineTextBox, Guis.Coord2d> = BiConsumer { _, _ -> }

        init {
            edit_ = TextFieldHelper(
                this::getText, this::setText, this::getClipboard, this::setClipboard
            ) { s -> s.length < max_text_size_ && font_!!.wordWrapHeight(s, width * NORM_LINE_HEIGHT / line_height_) <= (height * NORM_LINE_HEIGHT / line_height_) }
        }

        fun getValue(): String = eotTrimmed(text_)

        fun setValue(text: String): MultiLineTextBox {
            var t = text
            if (t.length > getMaxLength()) t = t.substring(0, getMaxLength())
            if (text_ != t) {
                val toEnd = text_.isEmpty()
                val cur = edit_.cursorPos
                val sel = edit_.selectionPos
                edit_.selectAll()
                edit_.insertText(eotTrimmed(t))
                val newSel = if (toEnd) text_.length else sel
                val newCur = if (toEnd) text_.length else cur
                edit_.setSelectionPos(newSel)
                edit_.setCursorPos(newCur, false)
                clearDisplayCache()
            }
            return this
        }

        fun getMaxLength(): Int = max_text_size_

        fun setMaxLength(size: Int): MultiLineTextBox {
            max_text_size_ = Mth.clamp(size, 2, 1024)
            if (text_.length > max_text_size_) {
                text_ = eotTrimmed(text_.substring(0, max_text_size_))
                on_changed_.accept(this)
            }
            return this
        }

        fun getFont(): Font? = font_
        fun setFont(fnt: Font): MultiLineTextBox { font_ = fnt; return this }

        fun getFontColor(): Int = font_color_
        fun setFontColor(color: Int): MultiLineTextBox { font_color_ = color or 0xff000000.toInt(); return this }

        fun getCursorColor(): Int = cursor_color_
        fun setCursorColor(color: Int): MultiLineTextBox { cursor_color_ = color or 0xff000000.toInt(); return this }

        fun getLineHeight(): Int = line_height_
        fun setLineHeight(h: Int): MultiLineTextBox {
            line_height_ = Mth.clamp(h, 6, NORM_LINE_HEIGHT)
            font_scale_ = line_height_.toFloat() / 9f
            return this
        }

        fun onValueChanged(cb: Consumer<MultiLineTextBox>): MultiLineTextBox { on_changed_ = cb; return this }
        fun onMouseMove(cb: BiConsumer<MultiLineTextBox, Guis.Coord2d>): MultiLineTextBox { on_mouse_move_ = cb; return this }

        fun getIndexUnderMouse(mouseX: Double, mouseY: Double): Int =
            if (font_ == null) 0
            else getDisplayCache().getIndexAtPosition(font_!!, screenCoordinates(Guis.Coord2d.of(mouseX.toInt(), mouseY.toInt()), false))

        fun getCoordinatesAtIndex(textIndex: Int): Guis.Coord2d {
            if (font_ == null) return Guis.Coord2d.ORIGIN
            var idx = Mth.clamp(textIndex, 0, getDisplayCache().fullText.length)
            val lindex = findLineFromPos(getDisplayCache().lineStarts, idx)
            if (lindex < 0 || lindex >= getDisplayCache().lineStarts.size) return Guis.Coord2d.ORIGIN
            val li = getDisplayCache().lines[lindex]
            idx = Mth.clamp(idx - getDisplayCache().lineStarts[lindex], 0, li.contents.length)
            val ox = font_!!.splitter.stringWidth(li.contents.substring(0, idx)).toInt()
            val oy = getDisplayCache().lines[0].y
            return Guis.Coord2d.of(li.x + (ox * line_height_ / NORM_LINE_HEIGHT), oy + ((li.y - oy) * line_height_ / NORM_LINE_HEIGHT))
        }

        override fun init(parent: Screen): MultiLineTextBox = init(parent, Guis.Coord2d.of(getX(), getY()))

        override fun init(parent: Screen, position: Guis.Coord2d): MultiLineTextBox {
            super<Guis.UiWidget>.init(parent, position)
            font_ = Minecraft.getInstance().font
            font_color_ = 0xff000000.toInt()
            cursor_color_ = 0xff000000.toInt()
            clearDisplayCache()
            return this
        }

        override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
            if (!active || !visible || x < getX() || y < getY() || x > getX() + width || y > getY() + height) return false
            if (button != 0) return true
            val sc = screenCoordinates(Guis.Coord2d.of(x.toInt(), y.toInt()), false)
            val index = getDisplayCache().getIndexAtPosition(font_!!, Guis.Coord2d.of(sc.x * NORM_LINE_HEIGHT / line_height_, sc.y * NORM_LINE_HEIGHT / line_height_))
            if (index >= 0) {
                if (index == last_index_ && (Util.getMillis() - last_clicked_) < 250) {
                    if (edit_.isSelecting) edit_.selectAll()
                    else edit_.setSelectionRange(StringSplitter.getWordPosition(getText(), -1, index, false), StringSplitter.getWordPosition(getText(), 1, index, false))
                } else {
                    edit_.setCursorPos(index, Screen.hasShiftDown())
                }
                clearDisplayCache()
            }
            last_index_ = index
            last_clicked_ = index.toLong()
            if (isFocused) setFocused(true)
            return true
        }

        override fun mouseDragged(x: Double, y: Double, button: Int, dx: Double, dy: Double): Boolean {
            if (super<Guis.UiWidget>.mouseDragged(x, y, button, dx, dy) || button != 0) return true
            if (!active || !visible) return false
            val sc = screenCoordinates(Guis.Coord2d.of(x.toInt(), y.toInt()), false)
            edit_.setCursorPos(getDisplayCache().getIndexAtPosition(font_!!, Guis.Coord2d.of(sc.x * NORM_LINE_HEIGHT / line_height_, sc.y * NORM_LINE_HEIGHT / line_height_)), true)
            clearDisplayCache()
            return true
        }

        override fun charTyped(key: Char, code: Int): Boolean {
            if (super<Guis.UiWidget>.charTyped(key, code)) return true
            if (!active || !visible) return false
            if (!StringUtil.isAllowedChatCharacter(key)) return false
            edit_.insertText(key.toString())
            clearDisplayCache()
            on_changed_.accept(this)
            return true
        }

        override fun keyPressed(key: Int, x: Int, y: Int): Boolean {
            if (super<Guis.UiWidget>.keyPressed(key, x, y)) return true
            if (!active || !visible) return false
            val textBefore = text_
            if (!specialKeyMatched(key)) return isFocused
            clearDisplayCache()
            if (key == 257 && !edit_.isSelecting && edit_.cursorPos < text_.length - 2) {
                val cp = edit_.cursorPos
                edit_.selectAll()
                edit_.insertText(eotTrimmed(text_))
                edit_.setSelectionPos(cp)
                edit_.setCursorPos(cp, false)
            }
            if (textBefore != text_) on_changed_.accept(this)
            return true
        }

        override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            if (!visible) return
            RenderSystem.setShader(GameRenderer::getPositionTexShader)
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            val ox = (getX() * (1.0 - font_scale_)).toInt()
            val oy = (getY() * (1.0 - font_scale_)).toInt()
            val mxs: PoseStack = gg.pose()
            mxs.pushPose()
            mxs.translate(ox.toDouble(), oy.toDouble(), 0.0)
            mxs.scale(font_scale_, font_scale_, font_scale_)
            val cache = getDisplayCache()
            for (li in cache.lines) gg.drawString(font_, li.asComponent, li.x, li.y, font_color_)
            renderCursor(gg, cache.cursor, cache.cursorAtEnd)
            renderHighlight(gg, cache.selection)
            val xy = getMousePosition()
            if (xy.x >= 0 && xy.y >= 0 && xy.x < width && xy.y < height) on_mouse_move_.accept(this, getMousePosition())
            mxs.popPose()
        }

        private fun getText(): String = text_
        private fun setText(text: String) { text_ = text; clearDisplayCache() }

        private fun setClipboard(text: String) {
            if (Minecraft.getInstance() != null) TextFieldHelper.setClipboardContents(Minecraft.getInstance(), eotTrimmed(text))
        }

        private fun getClipboard(): String =
            if (Minecraft.getInstance() != null) eotTrimmed(TextFieldHelper.getClipboardContents(Minecraft.getInstance())) else ""

        private fun specialKeyMatched(key: Int): Boolean {
            if (Screen.isSelectAll(key)) { edit_.selectAll(); return true }
            if (Screen.isCopy(key)) { edit_.copy(); return true }
            if (Screen.isPaste(key)) { edit_.paste(); return true }
            if (Screen.isCut(key)) { edit_.cut(); return true }
            return when (key) {
                257, 335 -> { edit_.insertText("\n"); true }
                259 -> { edit_.removeCharsFromCursor(-1); true }
                261 -> { edit_.removeCharsFromCursor(1); true }
                262 -> { edit_.moveByChars(1, Screen.hasShiftDown()); true }
                263 -> { edit_.moveByChars(-1, Screen.hasShiftDown()); true }
                264 -> { changeLine(1); true }
                265 -> { changeLine(-1); true }
                266 -> { edit_.setCursorPos(0, Screen.hasShiftDown()); true }
                267 -> { edit_.setCursorPos(text_.length, Screen.hasShiftDown()); true }
                268 -> { edit_.setCursorPos(getDisplayCache().findLineStart(edit_.cursorPos), Screen.hasShiftDown()); true }
                269 -> { edit_.setCursorPos(getDisplayCache().findLineEnd(edit_.cursorPos), Screen.hasShiftDown()); true }
                else -> false
            }
        }

        private fun changeLine(incr: Int) {
            edit_.setCursorPos(getDisplayCache().changeLine(edit_.cursorPos, incr), Screen.hasShiftDown())
        }

        private fun renderCursor(gg: GuiGraphics, pos: Guis.Coord2d, atEnd: Boolean) {
            if (!active || !visible) frame_tick_ = 0
            if ((++frame_tick_ and 0x3f) < 0x20) return
            val p = screenCoordinates(pos, true)
            if (!atEnd) gg.fill(p.x, p.y - 1, p.x + 1, p.y + NORM_LINE_HEIGHT, cursor_color_)
            else gg.drawString(font_, "_", p.x, p.y, cursor_color_)
        }

        private fun renderHighlight(gg: GuiGraphics, lineRects: Array<Rect2i>) {
            val fillColor = 0x339999ff
            var firstYOffsetPx = -1
            for (rc in lineRects) {
                val x = rc.x - getX()
                val y = rc.y - getY()
                val pos0 = screenCoordinates(Guis.Coord2d.of(x, y), true)
                val pos1 = screenCoordinates(Guis.Coord2d.of(x + rc.width, y + rc.height), true)
                gg.fill(pos0.x - 1, pos0.y + firstYOffsetPx, pos1.x - 1, pos1.y + firstYOffsetPx, fillColor)
                firstYOffsetPx = 0
            }
        }

        private var display_cache_: DisplayCache? = DisplayCache.EMPTY

        private fun getDisplayCache(): DisplayCache {
            if (display_cache_ == null) display_cache_ = rebuildDisplayCache()
            return display_cache_!!
        }

        private fun clearDisplayCache() { display_cache_ = null }

        private fun rebuildDisplayCache(): DisplayCache {
            val fullText = getText()
            if (fullText.isEmpty()) return DisplayCache.EMPTY
            val curPos = edit_.cursorPos
            val selPos = edit_.selectionPos
            val lsp: IntList = IntArrayList()
            val lineInfos: MutableList<LineInfo> = Lists.newArrayList()
            val lineNo = MutableInt()
            val lineTerminated = MutableBoolean()
            val ssp = font_!!.splitter
            ssp.splitLines(fullText, width * NORM_LINE_HEIGHT / line_height_, Style.EMPTY, true) { text, spos, epos ->
                val fullLine = fullText.substring(spos, epos)
                lineTerminated.setValue(fullLine.endsWith("\n"))
                val line = StringUtils.stripEnd(fullLine, " \n")
                lsp.add(spos)
                val pxy = screenCoordinates(Guis.Coord2d(0, lineNo.andIncrement * NORM_LINE_HEIGHT), true)
                lineInfos.add(LineInfo(text, line, pxy.x, pxy.y))
            }
            val lineStarts = lsp.toIntArray()
            val curAtEos = curPos == fullText.length
            val ppos: Guis.Coord2d
            if (curAtEos && lineTerminated.isTrue) {
                ppos = Guis.Coord2d(0, lineInfos.size * NORM_LINE_HEIGHT)
            } else {
                val lno = findLineFromPos(lineStarts, curPos)
                val lpx = font_!!.width(fullText.substring(lineStarts[lno], curPos))
                ppos = Guis.Coord2d(lpx, lno * NORM_LINE_HEIGHT)
            }
            val selectionBlocks: MutableList<Rect2i> = Lists.newArrayList()
            if (curPos != selPos) {
                val l2 = minOf(curPos, selPos)
                val i1 = maxOf(curPos, selPos)
                val j1 = findLineFromPos(lineStarts, l2)
                val k1 = findLineFromPos(lineStarts, i1)
                if (j1 == k1) {
                    val l1 = j1 * NORM_LINE_HEIGHT
                    val i2 = lineStarts[j1]
                    selectionBlocks.add(createPartialLineSelection(fullText, ssp, l2, i1, l1, i2))
                } else {
                    val i3 = if (j1 + 1 > lineStarts.size) fullText.length else lineStarts[j1 + 1]
                    selectionBlocks.add(createPartialLineSelection(fullText, ssp, l2, i3, j1 * NORM_LINE_HEIGHT, lineStarts[j1]))
                    for (j3 in j1 + 1 until k1) {
                        val j2 = j3 * NORM_LINE_HEIGHT
                        val s1 = fullText.substring(lineStarts[j3], lineStarts[j3 + 1])
                        val k2 = ssp.stringWidth(s1).toInt()
                        selectionBlocks.add(createSelection(Guis.Coord2d(0, j2), Guis.Coord2d(k2, j2 + NORM_LINE_HEIGHT)))
                    }
                    selectionBlocks.add(createPartialLineSelection(fullText, ssp, lineStarts[k1], i1, k1 * NORM_LINE_HEIGHT, lineStarts[k1]))
                }
            }
            return DisplayCache(fullText, ppos, curAtEos, lineStarts, lineInfos.toTypedArray(), selectionBlocks.toTypedArray())
        }

        private fun createPartialLineSelection(text: String, ssp: StringSplitter, spos: Int, epos: Int, liney: Int, lineStartPos: Int): Rect2i {
            val s0 = text.substring(lineStartPos, spos)
            val s1 = text.substring(lineStartPos, epos).replace("[\\r\\n]+$".toRegex(), "")
            return createSelection(Guis.Coord2d(ssp.stringWidth(s0).toInt(), liney), Guis.Coord2d(ssp.stringWidth(s1).toInt(), liney + NORM_LINE_HEIGHT))
        }

        private fun createSelection(pos1: Guis.Coord2d, pos2: Guis.Coord2d): Rect2i {
            val cd1 = screenCoordinates(pos1, true)
            val cd2 = screenCoordinates(pos2, true)
            val x0 = minOf(cd1.x, cd2.x)
            val x1 = maxOf(cd1.x, cd2.x)
            val y0 = minOf(cd1.y, cd2.y)
            val y1 = maxOf(cd1.y, cd2.y)
            return Rect2i(x0, y0, x1 - x0, y1 - y0)
        }

        @Environment(EnvType.CLIENT)
        class DisplayCache(
            @JvmField val fullText: String,
            @JvmField val cursor: Guis.Coord2d,
            @JvmField val cursorAtEnd: Boolean,
            @JvmField val lineStarts: IntArray,
            @JvmField val lines: Array<LineInfo>,
            @JvmField val selection: Array<Rect2i>
        ) {
            fun getIndexAtPosition(font: Font, pos: Guis.Coord2d): Int {
                val i = pos.y / NORM_LINE_HEIGHT
                if (i < 0) return 0
                if (i >= lines.size) return fullText.length
                return lineStarts[i] + font.splitter.plainIndexAtWidth(lines[i].contents, pos.x, lines[i].style)
            }

            fun changeLine(cursorPos: Int, length: Int): Int {
                val i = findLineFromPos(lineStarts, cursorPos)
                val j = i + length
                return if (j in 0 until lineStarts.size) {
                    val l = cursorPos - lineStarts[i]
                    lineStarts[j] + minOf(l, lines[j].contents.length)
                } else {
                    cursorPos
                }
            }

            fun findLineStart(cursorPos: Int): Int = lineStarts[findLineFromPos(lineStarts, cursorPos)]

            fun findLineEnd(cursorPos: Int): Int {
                val i = findLineFromPos(lineStarts, cursorPos)
                return lineStarts[i] + lines[i].contents.length
            }

            companion object {
                @JvmField val EMPTY: DisplayCache = DisplayCache(
                    "", Guis.Coord2d(0, 0), true, intArrayOf(0),
                    arrayOf(LineInfo(Style.EMPTY, "", 0, 0)), emptyArray()
                )
            }
        }

        @Environment(EnvType.CLIENT)
        class LineInfo(val style: Style, val contents: String, val x: Int, val y: Int) {
            val asComponent: Component = Component.literal(contents).setStyle(style)
        }

        companion object {
            private const val NORM_LINE_HEIGHT = 9

            private fun findLineFromPos(lineStarts: IntArray, cursorPos: Int): Int {
                val i = Arrays.binarySearch(lineStarts, cursorPos)
                return if (i < 0) -(i + 2) else i
            }

            private fun eotTrimmed(text: String): String = text.replace("\\s+$".toRegex(), "\n")
        }
    }
}
