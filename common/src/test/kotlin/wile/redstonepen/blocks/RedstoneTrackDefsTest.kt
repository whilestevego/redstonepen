package wile.redstonepen.blocks

import net.minecraft.core.Direction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import wile.redstonepen.blocks.track.RedstoneTrackDefs
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections

class RedstoneTrackDefsTest {
    @Test
    fun shapeGetReturnsNonNullForEveryFaceMask() {
        for (faces in 0 until 64) {
            assertNotNull(RedstoneTrackDefs.shape.get(faces))
        }
    }

    @Test
    fun shapeGetIsCachedForRepeatedCalls() {
        val a = RedstoneTrackDefs.shape.get(0x3F)
        val b = RedstoneTrackDefs.shape.get(0x3F)
        assertEquals(a, b)
    }

    @Test
    fun shapeForNoFacesIsEmpty() {
        assertTrue(RedstoneTrackDefs.shape.get(0).isEmpty)
    }

    @Test
    fun shapeForAnyFaceIsNonEmpty() {
        for (bit in 0 until 6) {
            assertFalse(RedstoneTrackDefs.shape.get(1 shl bit).isEmpty)
        }
    }

    @Test
    fun wireMappingHas25Entries() {
        assertEquals(25, RedstoneTrackDefs.models.STATE_WIRE_MAPPING.size)
    }

    @Test
    fun connectMappingHas7Entries() {
        assertEquals(7, RedstoneTrackDefs.models.STATE_CONNECT_MAPPING.size)
    }

    @Test
    fun connectionBitOrderHasSixDirections() {
        assertEquals(6, connections.CONNECTION_BIT_ORDER.size)
        assertEquals(6, connections.CONNECTION_BIT_ORDER_REV.size)
    }

    @Test
    fun getBulkConnectorBitMatchesForwardMapping() {
        for (d in Direction.values()) {
            val bit = connections.getBulkConnectorBit(d)
            assertEquals(d, connections.BULK_FACE_MAPPING[bit])
        }
    }

    @Test
    fun getWireBitYieldsKnownValues() {
        assertEquals(0x00000001L, connections.getWireBit(Direction.DOWN, Direction.NORTH))
        assertEquals(0x00800000L, connections.getWireBit(Direction.WEST, Direction.SOUTH))
    }

    @Test
    fun getWireBitForUnknownPairReturnsZero() {
        assertEquals(0L, connections.getWireBit(Direction.DOWN, Direction.DOWN))
    }

    @Test
    fun getWireBitSideAndDirectionRoundTrip() {
        val bit = connections.getWireBit(Direction.NORTH, Direction.EAST)
        val t = connections.getWireBitSideAndDirection(bit)
        assertEquals(Direction.NORTH, t.a)
        assertEquals(Direction.EAST, t.b)
    }

    @Test
    fun getWireBitSideAndDirectionUnknownDefaultsDownDown() {
        val t = connections.getWireBitSideAndDirection(0xFFFFFFFFL)
        assertEquals(Direction.DOWN, t.a)
        assertEquals(Direction.DOWN, t.b)
    }

    @Test
    fun getVanillaWireConnectionDirectionsReturnsAllSetSides() {
        val dirs = connections.getVanillaWireConnectionDirections(0x0FL)
        assertEquals(4, dirs.size)
        assertTrue(dirs.contains(Direction.NORTH))
        assertTrue(dirs.contains(Direction.SOUTH))
        assertTrue(dirs.contains(Direction.EAST))
        assertTrue(dirs.contains(Direction.WEST))
    }

    @Test
    fun getVanillaWireConnectionDirectionsEmptyForZeroMask() {
        assertTrue(connections.getVanillaWireConnectionDirections(0L).isEmpty())
    }

    @Test
    fun hasVanillaWireConnectionPerSide() {
        assertTrue(connections.hasVanillaWireConnection(0x01, Direction.NORTH))
        assertTrue(connections.hasVanillaWireConnection(0x02, Direction.SOUTH))
        assertTrue(connections.hasVanillaWireConnection(0x04, Direction.EAST))
        assertTrue(connections.hasVanillaWireConnection(0x08, Direction.WEST))
        assertFalse(connections.hasVanillaWireConnection(0x01, Direction.SOUTH))
        assertFalse(connections.hasVanillaWireConnection(0L, Direction.UP))
    }

    @Test
    fun hasBulkConnectionDetectsFaceBit() {
        val mask = connections.getBulkConnectorBit(Direction.EAST)
        assertTrue(connections.hasBulkConnection(mask, Direction.EAST))
        assertFalse(connections.hasBulkConnection(mask, Direction.WEST))
    }

    @Test
    fun hasRedstoneConnectionAllSidesAtLeastOneBitTrue() {
        val full = 0xFFFFFFFFL
        for (d in Direction.values()) {
            assertTrue(connections.hasRedstoneConnection(full, d))
        }
    }

    @Test
    fun hasRedstoneConnectionFalseForZero() {
        for (d in Direction.values()) {
            assertFalse(connections.hasRedstoneConnection(0L, d))
        }
    }

    @Test
    fun getWireElementsOnFaceCovers4BitsPerFace() {
        for (d in Direction.values()) {
            val mask = connections.getWireElementsOnFace(d)
            assertEquals(4, java.lang.Long.bitCount(mask))
        }
    }

    @Test
    fun getAllElementsOnFaceCoversWiresAndConnector() {
        for (d in Direction.values()) {
            val mask = connections.getAllElementsOnFace(d)
            assertEquals(5, java.lang.Long.bitCount(mask))
        }
    }

    @Test
    fun redstoneUpdateDirectionsContainsAllSixFaces() {
        assertEquals(6, RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS.size)
    }
}
