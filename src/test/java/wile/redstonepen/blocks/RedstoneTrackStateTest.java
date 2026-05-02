package wile.redstonepen.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import wile.redstonepen.blocks.RedstoneTrack.TrackBlockEntity.TestHooks;
import wile.redstonepen.blocks.RedstoneTrack.defs;
import wile.redstonepen.blocks.RedstoneTrack.defs.connections;

class RedstoneTrackStateTest
{
  private static TestHooks h() { return new TestHooks(); }

  // --- Mask constants ---

  @Test
  void wireMaskCoversExactlyBits0To23()
  {
    assertEquals(0x0000000000ffffffL, defs.STATE_FLAG_WIR_MASK);
  }

  @Test
  void connectionMaskCoversExactlyBits24To29()
  {
    assertEquals(0x000000003f000000L, defs.STATE_FLAG_CON_MASK);
  }

  @Test
  void powerMaskCoversExactlyBits32To55()
  {
    assertEquals(0x00ffffff00000000L, defs.STATE_FLAG_PWR_MASK);
  }

  @Test
  void masksDontOverlap()
  {
    assertEquals(0L, defs.STATE_FLAG_WIR_MASK & defs.STATE_FLAG_CON_MASK);
    assertEquals(0L, defs.STATE_FLAG_WIR_MASK & defs.STATE_FLAG_PWR_MASK);
    assertEquals(0L, defs.STATE_FLAG_CON_MASK & defs.STATE_FLAG_PWR_MASK);
  }

  // --- Wire flags ---

  @Test
  void getWireFlagsZeroStateReturnsZero()
  {
    assertEquals(0, h().getWireFlags());
  }

  @Test
  void getWireFlagsIgnoresUpperBits()
  {
    final TestHooks th = h();
    th.setState(~defs.STATE_FLAG_WIR_MASK); // only non-wire bits set
    assertEquals(0, th.getWireFlags());
  }

  @Test
  void getWireFlagBit0TrueWhenSet()
  {
    final TestHooks th = h();
    th.setState(1L);
    assertTrue(th.getWireFlag(0));
  }

  @Test
  void getWireFlagBit0FalseWhenClear()
  {
    assertFalse(h().getWireFlag(0));
  }

  @Test
  void getWireFlagBit23TrueWhenSet()
  {
    final TestHooks th = h();
    th.setState(1L << 23);
    assertTrue(th.getWireFlag(23));
  }

  @Test
  void getWireFlagCountIs24()
  {
    assertEquals(24, h().getWireFlagCount());
  }

  @Test
  void addWireFlagsDoesNotIncrementForAlreadySetBit()
  {
    final TestHooks th = h();
    th.setState(1L);
    assertEquals(0, th.addWireFlags(1L));
  }

  @Test
  void addWireFlagsAll24FlagsIndividuallyReturnsOne()
  {
    for(int i = 0; i < 24; ++i) {
      final TestHooks th = h();
      assertEquals(1, th.addWireFlags(1L << i),
        "addWireFlags bit " + i + " on empty state must return 1");
    }
  }

  @Test
  void addWireFlagsAllAtOnceSetsAll24()
  {
    final TestHooks th = h();
    th.addWireFlags(defs.STATE_FLAG_WIR_MASK);
    assertEquals(0x00ffffff, th.getWireFlags());
  }

  // --- Connection flags ---

  @Test
  void getConnectionFlagsZeroStateReturnsZero()
  {
    assertEquals(0, h().getConnectionFlags());
  }

  @Test
  void getConnectionFlagsOnlyBit24SetReturnsOne()
  {
    final TestHooks th = h();
    th.setState(1L << 24);
    assertEquals(1, th.getConnectionFlags());
  }

  @Test
  void getConnectionFlagEachBitIndependently()
  {
    for(int i = 0; i < 6; ++i) {
      final TestHooks th = h();
      th.setState(1L << (24 + i));
      assertTrue(th.getConnectionFlag(i), "connection flag " + i + " must be true");
      for(int j = 0; j < 6; ++j) {
        if(j != i) assertFalse(th.getConnectionFlag(j), "connection flag " + j + " must be false");
      }
    }
  }

  @Test
  void getConnectionFlagCountIs6()
  {
    assertEquals(6, h().getConnectionFlagCount());
  }

  // --- Side power nibbles ---

  @Test
  void getSidePowerZeroStateReturnsZeroForAllDirections()
  {
    final TestHooks th = h();
    for(Direction dir : Direction.values()) {
      assertEquals(0, th.getSidePower(dir), "getSidePower(" + dir + ") must be 0 on zero state");
    }
  }

  @Test
  void setSidePowerRoundTripsAllDirectionsAtMax()
  {
    for(Direction dir : Direction.values()) {
      final TestHooks th = h();
      th.setSidePower(dir, 15);
      assertEquals(15, th.getSidePower(dir), "round-trip getSidePower after setSidePower(15) for " + dir);
    }
  }

  @Test
  void setSidePowerRoundTripsAllDirectionsAtZero()
  {
    for(Direction dir : Direction.values()) {
      final TestHooks th = h();
      th.setSidePower(dir, 15); // set first to confirm clearing works
      th.setSidePower(dir, 0);
      assertEquals(0, th.getSidePower(dir), "getSidePower(" + dir + ") after setSidePower(0) must be 0");
    }
  }

  @Test
  void setSidePowerDoesNotCorruptAdjacentDirection()
  {
    // Set every direction to 15, then clear each one and verify neighbours unchanged.
    final Direction[] dirs = Direction.values();
    for(int i = 0; i < dirs.length; ++i) {
      final TestHooks th = h();
      for(Direction d : dirs) th.setSidePower(d, 15);
      th.setSidePower(dirs[i], 0);
      for(int j = 0; j < dirs.length; ++j) {
        int expected = (j == i) ? 0 : 15;
        assertEquals(expected, th.getSidePower(dirs[j]),
          "after clearing " + dirs[i] + ", getSidePower(" + dirs[j] + ") wrong");
      }
    }
  }

  @Test
  void setSidePowerTruncatesTo4Bits()
  {
    // 16 = 0b10000 → 4-bit mask → stored as 0
    final TestHooks th = h();
    th.setSidePower(Direction.DOWN, 16);
    assertEquals(0, th.getSidePower(Direction.DOWN));
  }

  // --- getRedstoneDustCount ---

  @Test
  void redstoneDustCountZeroStateReturnsZero()
  {
    assertEquals(0, h().getRedstoneDustCount());
  }

  @Test
  void redstoneDustCountAllWireBitsSetReturns24()
  {
    final TestHooks th = h();
    th.setState(defs.STATE_FLAG_WIR_MASK);
    assertEquals(24, th.getRedstoneDustCount());
  }

  @Test
  void redstoneDustCountAllConnectionBitsSetZeroWireBitsReturns6()
  {
    final TestHooks th = h();
    th.setState(defs.STATE_FLAG_CON_MASK);
    assertEquals(6, th.getRedstoneDustCount());
  }

  @Test
  void redstoneDustCountKWireBitsSetReturnsK()
  {
    for(int k = 0; k <= 24; ++k) {
      final TestHooks th = h();
      th.setState((1L << k) - 1); // lowest k bits set
      assertEquals(k, th.getRedstoneDustCount(), "k=" + k);
    }
  }

  // --- defs.connections static mappings ---

  @Test
  void wireFaceDirectionMappingHas24Entries()
  {
    assertEquals(24, connections.WIRE_FACE_DIRECTION_MAPPING.size() - 1); // key 0L is the zero entry
  }

  @Test
  void wireFaceDirectionMappingKeysAreDistinctPowersOfTwo()
  {
    // Every non-zero key must be a distinct power of two (single bit set).
    final java.util.Set<Long> seen = new java.util.HashSet<>();
    for(Long key : connections.WIRE_FACE_DIRECTION_MAPPING.keySet()) {
      if(key == 0L) continue;
      assertEquals(1, Long.bitCount(key), "key " + Long.toHexString(key) + " must be a single-bit mask");
      assertTrue(seen.add(key), "duplicate key " + Long.toHexString(key));
    }
  }

  @Test
  void bulkFaceMappingHasOneEntryPerFacePlusZero()
  {
    // 6 faces + 1 zero entry = 7
    assertEquals(7, connections.BULK_FACE_MAPPING.size());
  }

  @Test
  void bulkFaceMappingRevNonNullForAllDirections()
  {
    for(Direction dir : Direction.values()) {
      assertNotEquals(null, connections.BULK_FACE_MAPPING_REV.get(dir),
        "BULK_FACE_MAPPING_REV must have entry for " + dir);
    }
  }

  @Test
  void connectionBitOrderRevHasAllSixDirections()
  {
    assertEquals(6, connections.CONNECTION_BIT_ORDER_REV.size());
    for(Direction dir : Direction.values()) {
      assertTrue(connections.CONNECTION_BIT_ORDER_REV.containsKey(dir),
        "CONNECTION_BIT_ORDER_REV missing " + dir);
    }
  }
}
