package wile.redstonepen.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;
import wile.redstonepen.blocks.track.RedstoneTrackDefs;
import static wile.redstonepen.blocks.track.RedstoneTrackDefs.*;

class RedstoneTrackDefsTest
{
  // --- shape.get covers every face-bit combination -------------------------------------------

  @Test
  void shapeGetReturnsNonNullForEveryFaceMask()
  {
    for(int faces=0; faces<64; ++faces) {
      final VoxelShape s = RedstoneTrackDefs.shape.get(faces);
      assertNotNull(s);
    }
  }

  @Test
  void shapeGetIsCachedForRepeatedCalls()
  {
    final VoxelShape a = RedstoneTrackDefs.shape.get(0x3F);
    final VoxelShape b = RedstoneTrackDefs.shape.get(0x3F);
    assertEquals(a, b);
  }

  @Test
  void shapeForNoFacesIsEmpty()
  {
    assertTrue(RedstoneTrackDefs.shape.get(0).isEmpty());
  }

  @Test
  void shapeForAnyFaceIsNonEmpty()
  {
    for(int bit=0; bit<6; ++bit) {
      assertFalse(RedstoneTrackDefs.shape.get(1<<bit).isEmpty());
    }
  }

  // --- models maps are populated -------------------------------------------------------------

  @Test
  void wireMappingHas25Entries()
  { assertEquals(25, RedstoneTrackDefs.models.STATE_WIRE_MAPPING.size()); }

  @Test
  void connectMappingHas7Entries()
  { assertEquals(7, RedstoneTrackDefs.models.STATE_CONNECT_MAPPING.size()); }

  // --- connections bit helpers ---------------------------------------------------------------

  @Test
  void connectionBitOrderHasSixDirections()
  {
    assertEquals(6, RedstoneTrackDefs.connections.CONNECTION_BIT_ORDER.length);
    assertEquals(6, RedstoneTrackDefs.connections.CONNECTION_BIT_ORDER_REV.size());
  }

  @Test
  void getBulkConnectorBitMatchesForwardMapping()
  {
    for(Direction d : Direction.values()) {
      final long bit = RedstoneTrackDefs.connections.getBulkConnectorBit(d);
      assertEquals(d, RedstoneTrackDefs.connections.BULK_FACE_MAPPING.get(bit));
    }
  }

  @Test
  void getWireBitYieldsKnownValues()
  {
    assertEquals(0x00000001L, RedstoneTrackDefs.connections.getWireBit(Direction.DOWN, Direction.NORTH));
    assertEquals(0x00800000L, RedstoneTrackDefs.connections.getWireBit(Direction.WEST, Direction.SOUTH));
  }

  @Test
  void getWireBitForUnknownPairReturnsZero()
  {
    assertEquals(0L, RedstoneTrackDefs.connections.getWireBit(Direction.DOWN, Direction.DOWN));
  }

  @Test
  void getWireBitSideAndDirectionRoundTrip()
  {
    final long bit = RedstoneTrackDefs.connections.getWireBit(Direction.NORTH, Direction.EAST);
    final var t = RedstoneTrackDefs.connections.getWireBitSideAndDirection(bit);
    assertEquals(Direction.NORTH, t.getA());
    assertEquals(Direction.EAST, t.getB());
  }

  @Test
  void getWireBitSideAndDirectionUnknownDefaultsDownDown()
  {
    final var t = RedstoneTrackDefs.connections.getWireBitSideAndDirection(0xFFFFFFFFL);
    assertEquals(Direction.DOWN, t.getA());
    assertEquals(Direction.DOWN, t.getB());
  }

  @Test
  void getVanillaWireConnectionDirectionsReturnsAllSetSides()
  {
    final List<Direction> dirs = RedstoneTrackDefs.connections.getVanillaWireConnectionDirections(0x0FL);
    assertEquals(4, dirs.size());
    assertTrue(dirs.contains(Direction.NORTH));
    assertTrue(dirs.contains(Direction.SOUTH));
    assertTrue(dirs.contains(Direction.EAST));
    assertTrue(dirs.contains(Direction.WEST));
  }

  @Test
  void getVanillaWireConnectionDirectionsEmptyForZeroMask()
  {
    assertTrue(RedstoneTrackDefs.connections.getVanillaWireConnectionDirections(0L).isEmpty());
  }

  @Test
  void hasVanillaWireConnectionPerSide()
  {
    assertTrue(RedstoneTrackDefs.connections.hasVanillaWireConnection(0x01, Direction.NORTH));
    assertTrue(RedstoneTrackDefs.connections.hasVanillaWireConnection(0x02, Direction.SOUTH));
    assertTrue(RedstoneTrackDefs.connections.hasVanillaWireConnection(0x04, Direction.EAST));
    assertTrue(RedstoneTrackDefs.connections.hasVanillaWireConnection(0x08, Direction.WEST));
    assertFalse(RedstoneTrackDefs.connections.hasVanillaWireConnection(0x01, Direction.SOUTH));
    assertFalse(RedstoneTrackDefs.connections.hasVanillaWireConnection(0L, Direction.UP));
  }

  @Test
  void hasBulkConnectionDetectsFaceBit()
  {
    final long mask = RedstoneTrackDefs.connections.getBulkConnectorBit(Direction.EAST);
    assertTrue(RedstoneTrackDefs.connections.hasBulkConnection(mask, Direction.EAST));
    assertFalse(RedstoneTrackDefs.connections.hasBulkConnection(mask, Direction.WEST));
  }

  @Test
  void hasRedstoneConnectionAllSidesAtLeastOneBitTrue()
  {
    for(Direction d : Direction.values()) {
      // some bit per direction makes the predicate true
      final long full = 0xFFFFFFFFL;
      assertTrue(RedstoneTrackDefs.connections.hasRedstoneConnection(full, d));
    }
  }

  @Test
  void hasRedstoneConnectionFalseForZero()
  {
    for(Direction d : Direction.values()) {
      assertFalse(RedstoneTrackDefs.connections.hasRedstoneConnection(0L, d));
    }
  }

  @Test
  void getWireElementsOnFaceCovers4BitsPerFace()
  {
    for(Direction d : Direction.values()) {
      final long mask = RedstoneTrackDefs.connections.getWireElementsOnFace(d);
      assertEquals(4L, Long.bitCount(mask));
    }
  }

  @Test
  void getAllElementsOnFaceCoversWiresAndConnector()
  {
    for(Direction d : Direction.values()) {
      final long mask = RedstoneTrackDefs.connections.getAllElementsOnFace(d);
      assertEquals(5L, Long.bitCount(mask));
    }
  }

  @Test
  void redstoneUpdateDirectionsContainsAllSixFaces()
  {
    assertEquals(6, RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS.length);
  }
}
