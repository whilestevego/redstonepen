package wile.redstonepen.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TooltipDisplayTipRangeTest {
    @Test fun boundsComputedFromWidthAndHeight() {
        val r = TooltipDisplay.TipRange(10, 20, 5, 3) { null }
        assertEquals(10, r.x0)
        assertEquals(20, r.y0)
        assertEquals(14, r.x1)
        assertEquals(22, r.y1)
    }

    @Test fun singlePixelWidthHeightCollapsesBounds() {
        val r = TooltipDisplay.TipRange(7, 4, 1, 1) { null }
        assertEquals(7, r.x0)
        assertEquals(4, r.y0)
        assertEquals(7, r.x1)
        assertEquals(4, r.y1)
    }

    @Test fun zeroOriginBounds() {
        val r = TooltipDisplay.TipRange(0, 0, 10, 10) { null }
        assertEquals(0, r.x0)
        assertEquals(0, r.y0)
        assertEquals(9, r.x1)
        assertEquals(9, r.y1)
    }

    @Test fun componentConstructorDelegatesSupplier() {
        val r = TooltipDisplay.TipRange(0, 0, 1, 1) { null }
        assertNotNull(r.text)
    }
}
