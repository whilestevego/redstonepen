package wile.redstonepen.commands

import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DemoBuilderTest {
    @Test
    fun cellOriginIndexZeroReturnsGridOrigin() {
        val origin = BlockPos(10, 64, 20)
        assertEquals(origin, DemoBuilder.cellOrigin(origin, 0, 4, 3))
    }

    @Test
    fun cellOriginAdvancesAcrossColumnsBeforeAdvancingRow() {
        val origin = BlockPos(0, 0, 0)
        assertEquals(BlockPos(3, 0, 0), DemoBuilder.cellOrigin(origin, 1, 4, 3))
        assertEquals(BlockPos(6, 0, 0), DemoBuilder.cellOrigin(origin, 2, 4, 3))
        assertEquals(BlockPos(9, 0, 0), DemoBuilder.cellOrigin(origin, 3, 4, 3))
        assertEquals(BlockPos(0, 0, 3), DemoBuilder.cellOrigin(origin, 4, 4, 3))
        assertEquals(BlockPos(3, 0, 3), DemoBuilder.cellOrigin(origin, 5, 4, 3))
    }

    @Test
    fun cellOriginPreservesYCoordinate() {
        val origin = BlockPos(0, 100, 0)
        assertEquals(100, DemoBuilder.cellOrigin(origin, 7, 3, 5).y)
    }

    @Test
    fun cellOriginRejectsZeroColumns() {
        assertThrows<IllegalArgumentException> { DemoBuilder.cellOrigin(BlockPos.ZERO, 0, 0, 1) }
    }

    @Test
    fun cellOriginRejectsZeroSpacing() {
        assertThrows<IllegalArgumentException> { DemoBuilder.cellOrigin(BlockPos.ZERO, 0, 4, 0) }
    }

    @Test
    fun cellOriginRejectsNegativeIndex() {
        assertThrows<IllegalArgumentException> { DemoBuilder.cellOrigin(BlockPos.ZERO, -1, 4, 3) }
    }

    @Test
    fun regionVolumeIsInclusiveOnBothEnds() {
        assertEquals(1, DemoBuilder.regionVolume(BlockPos.ZERO, BlockPos.ZERO))
        assertEquals(8, DemoBuilder.regionVolume(BlockPos.ZERO, BlockPos(1, 1, 1)))
        assertEquals(27, DemoBuilder.regionVolume(BlockPos.ZERO, BlockPos(2, 2, 2)))
    }

    @Test
    fun regionVolumeOrderIndependent() {
        val a = BlockPos(5, 5, 5)
        val b = BlockPos(0, 0, 0)
        assertEquals(DemoBuilder.regionVolume(a, b), DemoBuilder.regionVolume(b, a))
    }
}
