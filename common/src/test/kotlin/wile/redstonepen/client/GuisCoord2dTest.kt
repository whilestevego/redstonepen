package wile.redstonepen.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import wile.redstonepen.client.Guis.Coord2d

class GuisCoord2dTest {
    @Test
    fun constructorStoresXAndY() {
        val c = Coord2d(3, 7)
        assertEquals(3, c.x)
        assertEquals(7, c.y)
    }

    @Test
    fun ofFactoryCreatesEquivalentCoord() {
        val a = Coord2d(5, 9)
        val b = Coord2d.of(5, 9)
        assertEquals(a.x, b.x)
        assertEquals(a.y, b.y)
    }

    @Test
    fun originIsZeroZero() {
        assertEquals(0, Coord2d.ORIGIN.x)
        assertEquals(0, Coord2d.ORIGIN.y)
    }

    @Test
    fun toStringFormatsAsBracketedXCommaY() {
        assertEquals("[3,4]", Coord2d(3, 4).toString())
    }

    @Test
    fun toStringZeroZero() {
        assertEquals("[0,0]", Coord2d.ORIGIN.toString())
    }

    @Test
    fun toStringNegativeValues() {
        assertEquals("[-1,-2]", Coord2d(-1, -2).toString())
    }
}
