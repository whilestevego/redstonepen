package wile.redstonepen.client

import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap

class OverlayTextOverlayGuiTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    @BeforeEach
    fun reset() {
        Overlay.TextOverlayGui.hide()
    }

    @Test
    fun hideZerosDeadlineAndClearsText() {
        Overlay.TextOverlayGui.show("hello", 5000)
        Overlay.TextOverlayGui.hide()
        assertEquals(0, Overlay.TextOverlayGui.deadline())
        assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text())
    }

    @Test
    fun showStringUpdatesTextToMatchInput() {
        Overlay.TextOverlayGui.show("ping", 1000)
        assertEquals("ping", Overlay.TextOverlayGui.text().string)
    }

    @Test
    fun showNullStringFallsBackToEmptyText() {
        Overlay.TextOverlayGui.show(null as String?, 1000)
        assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text())
    }

    @Test
    fun showEmptyStringFallsBackToEmptyText() {
        Overlay.TextOverlayGui.show("", 1000)
        assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text())
    }

    @Test
    fun showStringSetsDeadlineInFuture() {
        val before = System.currentTimeMillis()
        Overlay.TextOverlayGui.show("x", 500)
        assertTrue(Overlay.TextOverlayGui.deadline() >= before + 500)
    }

    @Test
    fun showComponentUpdatesTextToMatchInput() {
        Overlay.TextOverlayGui.show(Component.literal("world"), 1000)
        assertEquals("world", Overlay.TextOverlayGui.text().string)
    }

    @Test
    fun showNullComponentFallsBackToEmptyText() {
        Overlay.TextOverlayGui.show(null as Component?, 1000)
        assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text())
    }

    @Test
    fun showComponentSetsDeadlineInFuture() {
        val before = System.currentTimeMillis()
        Overlay.TextOverlayGui.show(Component.literal("x"), 1000)
        assertTrue(Overlay.TextOverlayGui.deadline() >= before + 1000)
    }
}
