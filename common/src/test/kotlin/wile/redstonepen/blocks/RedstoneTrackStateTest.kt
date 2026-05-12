package wile.redstonepen.blocks

import java.util.stream.IntStream
import net.minecraft.core.Direction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_CON_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_PWR_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_WIR_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections
import wile.redstonepen.blocks.track.TestHooks

class RedstoneTrackStateTest {
    companion object {
        @JvmStatic fun wireBitIndices(): IntStream = IntStream.range(0, 24)

        @JvmStatic fun dustCountIndices(): IntStream = IntStream.rangeClosed(0, 24)
    }

    private fun h() = TestHooks()

    @Nested
    inner class MaskConstants {
        @Test
        fun masksDontOverlap() {
            assertEquals(0L, STATE_FLAG_WIR_MASK and STATE_FLAG_CON_MASK)
            assertEquals(0L, STATE_FLAG_WIR_MASK and STATE_FLAG_PWR_MASK)
            assertEquals(0L, STATE_FLAG_CON_MASK and STATE_FLAG_PWR_MASK)
        }
    }

    @Nested
    inner class WireFlags {
        @Test
        fun getWireFlagsZeroStateReturnsZero() {
            assertEquals(0, h().getWireFlags())
        }

        @Test
        fun getWireFlagsIgnoresUpperBits() {
            val th = h()
            th.state = STATE_FLAG_WIR_MASK.inv()
            assertEquals(0, th.getWireFlags())
        }

        @Test
        fun getWireFlagBit0TrueWhenSet() {
            val th = h()
            th.state = 1L
            assertTrue(th.getWireFlag(0))
        }

        @Test
        fun getWireFlagBit0FalseWhenClear() {
            assertFalse(h().getWireFlag(0))
        }

        @Test
        fun getWireFlagBit23TrueWhenSet() {
            val th = h()
            th.state = 1L shl 23
            assertTrue(th.getWireFlag(23))
        }

        @Test
        fun getWireFlagCountIs24() {
            assertEquals(24, h().getWireFlagCount())
        }

        @Test
        fun addWireFlagsDoesNotIncrementForAlreadySetBit() {
            val th = h()
            th.state = 1L
            assertEquals(0, th.addWireFlags(1L))
        }

        @ParameterizedTest
        @MethodSource("wile.redstonepen.blocks.RedstoneTrackStateTest#wireBitIndices")
        fun addWireFlagEachBitIndividuallyReturnsOne(i: Int) {
            assertEquals(1, h().addWireFlags(1L shl i))
        }

        @Test
        fun addWireFlagsAllAtOnceSetsAll24() {
            val th = h()
            th.addWireFlags(STATE_FLAG_WIR_MASK)
            assertEquals(0x00ffffff, th.getWireFlags())
        }
    }

    @Nested
    inner class ConnectionFlags {
        @Test
        fun getConnectionFlagsZeroStateReturnsZero() {
            assertEquals(0, h().getConnectionFlags())
        }

        @Test
        fun getConnectionFlagsOnlyBit24SetReturnsOne() {
            val th = h()
            th.state = 1L shl 24
            assertEquals(1, th.getConnectionFlags())
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 3, 4, 5])
        fun getConnectionFlagEachBitIndependently(i: Int) {
            val th = h()
            th.state = 1L shl (24 + i)
            assertTrue(th.getConnectionFlag(i))
            for (j in 0 until 6) {
                if (j != i)
                    assertFalse(
                        th.getConnectionFlag(j),
                        "flag $j must be clear when only $i is set",
                    )
            }
        }

        @Test
        fun getConnectionFlagCountIs6() {
            assertEquals(6, h().getConnectionFlagCount())
        }
    }

    @Nested
    inner class SidePower {
        @ParameterizedTest
        @EnumSource(Direction::class)
        fun getSidePowerZeroStateReturnsZero(dir: Direction) {
            assertEquals(0, h().getSidePower(dir))
        }

        @ParameterizedTest
        @EnumSource(Direction::class)
        fun setSidePowerRoundTripsAtMax(dir: Direction) {
            val th = h()
            th.setSidePower(dir, 15)
            assertEquals(15, th.getSidePower(dir))
        }

        @ParameterizedTest
        @EnumSource(Direction::class)
        fun setSidePowerRoundTripsAtZero(dir: Direction) {
            val th = h()
            th.setSidePower(dir, 15)
            th.setSidePower(dir, 0)
            assertEquals(0, th.getSidePower(dir))
        }

        @Test
        fun setSidePowerDoesNotCorruptAdjacentDirection() {
            val dirs = Direction.values()
            for (i in dirs.indices) {
                val th = h()
                for (d in dirs) th.setSidePower(d, 15)
                th.setSidePower(dirs[i], 0)
                for (j in dirs.indices) {
                    val expected = if (j == i) 0 else 15
                    assertEquals(
                        expected,
                        th.getSidePower(dirs[j]),
                        "after clearing ${dirs[i]}, getSidePower(${dirs[j]}) wrong",
                    )
                }
            }
        }

        @Test
        fun setSidePowerTruncatesTo4Bits() {
            val th = h()
            th.setSidePower(Direction.DOWN, 16)
            assertEquals(0, th.getSidePower(Direction.DOWN))
        }
    }

    @Nested
    inner class DustCount {
        @Test
        fun redstoneDustCountZeroStateReturnsZero() {
            assertEquals(0, h().getRedstoneDustCount())
        }

        @Test
        fun redstoneDustCountAllWireBitsSetReturns24() {
            val th = h()
            th.state = STATE_FLAG_WIR_MASK
            assertEquals(24, th.getRedstoneDustCount())
        }

        @Test
        fun redstoneDustCountAllConnectionBitsSetZeroWireBitsReturns6() {
            val th = h()
            th.state = STATE_FLAG_CON_MASK
            assertEquals(6, th.getRedstoneDustCount())
        }

        @ParameterizedTest
        @MethodSource("wile.redstonepen.blocks.RedstoneTrackStateTest#dustCountIndices")
        fun redstoneDustCountEqualsSetBitCount(k: Int) {
            val th = h()
            th.state = (1L shl k) - 1
            assertEquals(k, th.getRedstoneDustCount())
        }
    }

    @Nested
    inner class StaticMappings {
        @Test
        fun wireFaceDirectionMappingHas24Entries() {
            assertEquals(24, connections.WIRE_FACE_DIRECTION_MAPPING.size - 1)
        }

        @Test
        fun wireFaceDirectionMappingKeysAreDistinctPowersOfTwo() {
            val seen = mutableSetOf<Long>()
            for (key in connections.WIRE_FACE_DIRECTION_MAPPING.keys) {
                if (key == 0L) continue
                assertEquals(
                    1,
                    java.lang.Long.bitCount(key),
                    "key ${java.lang.Long.toHexString(key)} must be a single-bit mask",
                )
                assertTrue(seen.add(key), "duplicate key ${java.lang.Long.toHexString(key)}")
            }
        }

        @Test
        fun bulkFaceMappingHasOneEntryPerFacePlusZero() {
            assertEquals(7, connections.BULK_FACE_MAPPING.size)
        }

        @Test
        fun bulkFaceMappingRevNonNullForAllDirections() {
            for (dir in Direction.values()) {
                assertNotEquals(
                    null,
                    connections.BULK_FACE_MAPPING_REV[dir],
                    "BULK_FACE_MAPPING_REV must have entry for $dir",
                )
            }
        }

        @Test
        fun connectionBitOrderRevHasAllSixDirections() {
            assertEquals(6, connections.CONNECTION_BIT_ORDER_REV.size)
            for (dir in Direction.values()) {
                assertTrue(
                    connections.CONNECTION_BIT_ORDER_REV.containsKey(dir),
                    "CONNECTION_BIT_ORDER_REV missing $dir",
                )
            }
        }
    }
}
