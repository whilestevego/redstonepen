package wile.redstonepen.blocks.track

import net.minecraft.core.Direction
import wile.redstonepen.blocks.track.RedstoneTrackDefs.Connections
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_CON_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_CON_POS
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_PWR_POS
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_WIR_MASK

/**
 * Typed wrapper around the `Long` bit field that encodes wire presence, bulk connectors, and
 * per-face power levels for a [TrackBlockEntity]. All bit arithmetic is centralised here so callers
 * never need to handle shifts or masks directly.
 *
 * The layout (defined by [RedstoneTrackDefs]):
 * - Bits 0–23 — wire presence flags (one per face/direction pair).
 * - Bits 24–29 — bulk connector flags (one per face).
 * - Bits 32–55 — 4-bit power level for each of the 6 faces.
 */
@JvmInline
value class TrackStateFlags(val raw: Long) {
    /** Wire-presence bits (0–23) as a packed Int. */
    val wireFlags: Int
        get() = (raw and STATE_FLAG_WIR_MASK).toInt()

    /** `true` if the wire bit at [index] (0–23) is set. */
    fun wireFlag(index: Int): Boolean = (raw and (1L shl index)) != 0L

    /** Bulk-connector bits (24–29) shifted down to a 6-bit Int. */
    val connectionFlags: Int
        get() = ((raw and STATE_FLAG_CON_MASK) shr STATE_FLAG_CON_POS).toInt()

    /** `true` if the bulk connector bit at [index] (0–5) is set. */
    fun connectionFlag(index: Int): Boolean = (raw and (1L shl (STATE_FLAG_CON_POS + index))) != 0L

    /** Power level (0–15) stored for [side]. */
    fun sidePower(side: Direction): Int {
        val shift =
            STATE_FLAG_PWR_POS +
                POWER_BITS_PER_SIDE * Connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0)
        return ((raw shr shift) and 0xfL).toInt()
    }

    /**
     * Returns a copy with the power level for [side] set to [p]. Values above 15 are truncated to 4
     * bits.
     */
    fun withSidePower(side: Direction, p: Int): TrackStateFlags {
        val shift =
            STATE_FLAG_PWR_POS +
                POWER_BITS_PER_SIDE * Connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0)
        return TrackStateFlags(
            (raw and (0xfL shl shift).inv()) or ((p.toLong() and 0xfL) shl shift)
        )
    }

    /**
     * Sets each wire bit in [flags] that is not already set, limited to [STATE_FLAG_WIR_MASK].
     *
     * @return The updated flags and the count of bits newly set.
     */
    fun withAddedWireFlags(flags: Long): Pair<TrackStateFlags, Int> {
        val toAdd = flags and STATE_FLAG_WIR_MASK and raw.inv()
        return Pair(TrackStateFlags(raw or toAdd), java.lang.Long.bitCount(toAdd))
    }

    /** Returns a copy with every bit in [mask] cleared. */
    fun withBitsCleared(mask: Long): TrackStateFlags = TrackStateFlags(raw and mask.inv())

    /** Returns a copy with every bit in [mask] set. */
    fun withBitsSet(mask: Long): TrackStateFlags = TrackStateFlags(raw or mask)

    /** `true` if at least one bit in [mask] is set. */
    fun hasBits(mask: Long): Boolean = (raw and mask) != 0L

    companion object {
        val EMPTY = TrackStateFlags(0L)

        /** Number of bits allocated per side in the power region of the flag field. */
        private const val POWER_BITS_PER_SIDE = 4
    }
}
