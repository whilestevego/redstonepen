package wile.redstonepen.util

import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AuxiliariesAABBTest {
    private fun assertAABB(expected: AABB, actual: AABB) {
        val eps = 1e-9
        assertEquals(expected.minX, actual.minX, eps, "minX")
        assertEquals(expected.minY, actual.minY, eps, "minY")
        assertEquals(expected.minZ, actual.minZ, eps, "minZ")
        assertEquals(expected.maxX, actual.maxX, eps, "maxX")
        assertEquals(expected.maxY, actual.maxY, eps, "maxY")
        assertEquals(expected.maxZ, actual.maxZ, eps, "maxZ")
    }

    @Nested
    inner class PixeledAABB {
        @Test
        fun pixeledAABBDividesAllCoordinatesBySixteen() {
            assertAABB(
                AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
                Auxiliaries.getPixeledAABB(0.0, 0.0, 0.0, 16.0, 16.0, 16.0),
            )
        }

        @Test
        fun pixeledAABBPreservesAsymmetricBounds() {
            assertAABB(
                AABB(0.25, 0.5, 0.0, 0.75, 1.0, 0.25),
                Auxiliaries.getPixeledAABB(4.0, 8.0, 0.0, 12.0, 16.0, 4.0),
            )
        }

        @Test
        fun getPixeledAABBPartialPixels() {
            val bb = Auxiliaries.getPixeledAABB(2.0, 4.0, 6.0, 10.0, 12.0, 14.0)
            val eps = 1e-9
            assertEquals(2.0 / 16.0, bb.minX, eps)
            assertEquals(4.0 / 16.0, bb.minY, eps)
            assertEquals(6.0 / 16.0, bb.minZ, eps)
            assertEquals(10.0 / 16.0, bb.maxX, eps)
            assertEquals(12.0 / 16.0, bb.maxY, eps)
            assertEquals(14.0 / 16.0, bb.maxZ, eps)
        }
    }

    @Nested
    inner class RotatedAABB {
        @Test
        fun rotatedAABBNorthIsIdentity() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.NORTH))
        }

        @Test
        fun rotatedAABBSouthMirrorsXandZ() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7),
                Auxiliaries.getRotatedAABB(bb, Direction.SOUTH),
            )
        }

        @Test
        fun rotatedAABBDownSwapsYandZ() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.4, 0.3, 0.2, 0.9, 0.8, 0.7),
                Auxiliaries.getRotatedAABB(bb, Direction.DOWN),
            )
        }

        @Test
        fun rotatedAABBUpInvertsAllThreeAxes() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8),
                Auxiliaries.getRotatedAABB(bb, Direction.UP),
            )
        }

        @Test
        fun rotatedAABBWestSwapsXandZ() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.3, 0.2, 0.4, 0.8, 0.7, 0.9),
                Auxiliaries.getRotatedAABB(bb, Direction.WEST),
            )
        }

        @Test
        fun rotatedAABBEastSwapsXandZ() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.2, 0.2, 0.1, 0.7, 0.7, 0.6),
                Auxiliaries.getRotatedAABB(bb, Direction.EAST),
            )
        }

        @Test
        fun getRotatedAABBAllSixFacesProduceValidBoxes() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9)
            for (d in Direction.values()) {
                val out = Auxiliaries.getRotatedAABB(bb, d)
                assertNotNull(out)
                assertTrue(out.maxX > out.minX)
                assertTrue(out.maxY > out.minY)
                assertTrue(out.maxZ > out.minZ)
            }
        }

        @Test
        fun getRotatedAABBArrayPreservesLength() {
            val inn =
                arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), AABB(0.2, 0.2, 0.2, 0.8, 0.8, 0.8))
            assertEquals(inn.size, Auxiliaries.getRotatedAABB(inn, Direction.EAST).size)
        }

        @Test
        fun getRotatedAABBArrayWithEmptyInput() {
            assertEquals(0, Auxiliaries.getRotatedAABB(emptyArray<AABB>(), Direction.NORTH).size)
        }
    }

    @Nested
    inner class RotatedAABBHorizontal {
        @Test
        fun getRotatedAABBHorizontalKeepsYAxis() {
            val bb = AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9)
            val south = Auxiliaries.getRotatedAABB(bb, Direction.SOUTH, true)
            val eps = 1e-9
            assertEquals(bb.minY, south.minY, eps)
            assertEquals(bb.maxY, south.maxY, eps)
        }

        @Test
        fun getRotatedAABBHorizontalDownAndUpAreIdentity() {
            val bb = AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9)
            assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.DOWN, true))
            assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.UP, true))
        }

        @Test
        fun getRotatedAABBHorizontalAllSixFacesProduceValidBoxes() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9)
            for (d in Direction.values()) {
                val out = Auxiliaries.getRotatedAABB(bb, d, true)
                assertNotNull(out)
                assertTrue(out.maxX > out.minX)
                assertTrue(out.maxY > out.minY)
                assertTrue(out.maxZ > out.minZ)
            }
        }

        @Test
        fun getRotatedAABBArrayHorizontalPreservesLength() {
            val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
            assertEquals(inn.size, Auxiliaries.getRotatedAABB(inn, Direction.SOUTH, true).size)
        }
    }

    @Nested
    inner class YRotatedAABB {
        @Test
        fun yRotatedAABBZeroStepsIsIdentity() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(bb, Auxiliaries.getYRotatedAABB(bb, 0))
        }

        @Test
        fun yRotatedAABBFourStepsIsIdentity() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(bb, Auxiliaries.getYRotatedAABB(bb, 4))
        }

        @Test
        fun yRotatedAABBTwoStepsIs180Degrees() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7), Auxiliaries.getYRotatedAABB(bb, 2))
        }

        @Test
        fun getYRotatedAABBHandlesNegativeAndModularSteps() {
            val bb = AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9)
            val step1 = Auxiliaries.getYRotatedAABB(bb, 1)
            assertAABB(step1, Auxiliaries.getYRotatedAABB(bb, 5))
            assertAABB(step1, Auxiliaries.getYRotatedAABB(bb, -3))
        }

        @Test
        fun getYRotatedAABBAllFourQuartersProduceValidBoxes() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9)
            val eps = 1e-9
            for (q in 0 until 4) {
                val out = Auxiliaries.getYRotatedAABB(bb, q)
                assertNotNull(out)
                assertEquals(bb.minY, out.minY, eps)
                assertEquals(bb.maxY, out.maxY, eps)
            }
        }

        @Test
        fun getYRotatedAABBArrayPreservesLength() {
            val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
            assertEquals(inn.size, Auxiliaries.getYRotatedAABB(inn, 2).size)
        }

        @Test
        fun getYRotatedAABBArrayWithEmptyInput() {
            assertEquals(0, Auxiliaries.getYRotatedAABB(emptyArray<AABB>(), 1).size)
        }
    }

    @Nested
    inner class MirroredAABB {
        @Test
        fun mirroredAABBXAxisSwapsXCoordinates() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8),
                Auxiliaries.getMirroredAABB(bb, Direction.Axis.X),
            )
        }

        @Test
        fun mirroredAABBYAxisSwapsYCoordinates() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.1, 0.3, 0.3, 0.6, 0.8, 0.8),
                Auxiliaries.getMirroredAABB(bb, Direction.Axis.Y),
            )
        }

        @Test
        fun mirroredAABBZAxisSwapsZCoordinates() {
            val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
            assertAABB(
                AABB(0.1, 0.2, 0.2, 0.6, 0.7, 0.7),
                Auxiliaries.getMirroredAABB(bb, Direction.Axis.Z),
            )
        }

        @Test
        fun getMirroredAABBArrayPreservesLength() {
            val inn =
                arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), AABB(0.2, 0.2, 0.2, 0.8, 0.8, 0.8))
            assertEquals(inn.size, Auxiliaries.getMirroredAABB(inn, Direction.Axis.X).size)
        }

        @Test
        fun getMirroredAABBArrayWithEmptyInput() {
            assertEquals(0, Auxiliaries.getMirroredAABB(emptyArray<AABB>(), Direction.Axis.X).size)
        }
    }

    @Nested
    inner class MappedAABB {
        @Test
        fun getMappedAABBAppliesMapper() {
            val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
            assertAABB(
                AABB(2.0, 0.0, 0.0, 3.0, 1.0, 1.0),
                Auxiliaries.getMappedAABB(inn) { b -> b.move(2.0, 0.0, 0.0) }[0],
            )
        }

        @Test
        fun getMappedAABBHandlesEmptyInput() {
            assertEquals(0, Auxiliaries.getMappedAABB(emptyArray<AABB>()) { b -> b }.size)
        }
    }

    @Nested
    inner class UnionShape {
        @Test
        fun getUnionShapeOfSingleAABB() {
            assertFalse(Auxiliaries.getUnionShape(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)).isEmpty)
        }

        @Test
        fun getUnionShapeOfArrayOfAABB() {
            val s =
                Auxiliaries.getUnionShape(
                    arrayOf(AABB(0.0, 0.0, 0.0, 0.5, 1.0, 1.0)),
                    arrayOf(AABB(0.5, 0.0, 0.0, 1.0, 1.0, 1.0)),
                )
            assertFalse(s.isEmpty)
        }

        @Test
        fun getUnionShapeArrayOfArraysHandlesEmptyInput() {
            assertTrue(Auxiliaries.getUnionShape(*Array(0) { emptyArray<AABB>() }).isEmpty)
        }
    }
}
