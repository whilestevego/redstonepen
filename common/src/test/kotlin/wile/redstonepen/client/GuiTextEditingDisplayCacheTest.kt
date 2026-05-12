package wile.redstonepen.client

import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap

class GuiTextEditingDisplayCacheTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    private fun twoLineCache(): GuiTextEditing.MultiLineTextBox.DisplayCache {
        val lineStarts = intArrayOf(0, 6)
        val lines =
            arrayOf(
                GuiTextEditing.MultiLineTextBox.LineInfo(Style.EMPTY, "hello", 0, 0),
                GuiTextEditing.MultiLineTextBox.LineInfo(Style.EMPTY, "world", 0, 9),
            )
        return GuiTextEditing.MultiLineTextBox.DisplayCache(
            "hello\nworld",
            Guis.Coord2d(0, 0),
            false,
            lineStarts,
            lines,
            emptyArray<Rect2i>(),
        )
    }

    @Test
    fun emptyDisplayCacheHasCursorAtEnd() {
        assertTrue(GuiTextEditing.MultiLineTextBox.DisplayCache.EMPTY.cursorAtEnd)
    }

    @Test
    fun emptyDisplayCacheHasCursorAtOrigin() {
        val c = GuiTextEditing.MultiLineTextBox.DisplayCache.EMPTY.cursor
        assertEquals(0, c.x)
        assertEquals(0, c.y)
    }

    @Test
    fun changeLineReturnsUnchangedPosWhenMovingPastLastLine() {
        assertEquals(7, twoLineCache().changeLine(7, 1))
    }

    @Test
    fun changeLineReturnsUnchangedPosWhenMovingBeforeFirstLine() {
        assertEquals(2, twoLineCache().changeLine(2, -1))
    }

    @Test
    fun changeLineMovesDownPreservingColumnOffset() {
        assertEquals(8, twoLineCache().changeLine(2, 1))
    }

    @Test
    fun changeLineMovesUpPreservingColumnOffset() {
        assertEquals(3, twoLineCache().changeLine(9, -1))
    }

    @Test
    fun changeLineClampedToLineLength() {
        assertEquals(11, twoLineCache().changeLine(5, 1))
    }

    @Test
    fun findLineStartForFirstLineReturnsZero() {
        assertEquals(0, twoLineCache().findLineStart(0))
    }

    @Test
    fun findLineStartForMidFirstLineReturnsZero() {
        assertEquals(0, twoLineCache().findLineStart(3))
    }

    @Test
    fun findLineStartForSecondLineReturnsItsStart() {
        assertEquals(6, twoLineCache().findLineStart(7))
    }

    @Test
    fun findLineEndForFirstLineReturnsLengthOfContents() {
        assertEquals(5, twoLineCache().findLineEnd(2))
    }

    @Test
    fun findLineEndForSecondLineReturnsItsEnd() {
        assertEquals(11, twoLineCache().findLineEnd(8))
    }
}
