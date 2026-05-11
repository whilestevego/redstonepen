package wile.redstonepen.blocks.track

import net.minecraft.core.Direction
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections

class TestHooks {
    var state: Long = 0L

    fun getWireFlags(): Int =
        ((state and RedstoneTrackDefs.STATE_FLAG_WIR_MASK) shr RedstoneTrackDefs.STATE_FLAG_WIR_POS)
            .toInt()

    fun getWireFlag(index: Int): Boolean =
        (state and (1L shl (RedstoneTrackDefs.STATE_FLAG_WIR_POS + index))) != 0L

    fun getWireFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_WIR_COUNT

    fun getConnectionFlags(): Int =
        ((state and RedstoneTrackDefs.STATE_FLAG_CON_MASK) shr RedstoneTrackDefs.STATE_FLAG_CON_POS)
            .toInt()

    fun getConnectionFlag(index: Int): Boolean =
        (state and (1L shl (RedstoneTrackDefs.STATE_FLAG_CON_POS + index))) != 0L

    fun getConnectionFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_CON_COUNT

    fun getSidePower(side: Direction): Int {
        val shift =
            RedstoneTrackDefs.STATE_FLAG_PWR_POS +
                4 * (connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0))
        return ((state shr shift) and 0xfL).toInt()
    }

    fun setSidePower(side: Direction, p: Int) {
        val shift =
            RedstoneTrackDefs.STATE_FLAG_PWR_POS +
                4 * (connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0))
        state = (state and (0xfL shl shift).inv()) or ((p.toLong() and 0xfL) shl shift)
    }

    fun addWireFlags(flags: Long): Int {
        var n_added = 0
        for (i in 0 until getWireFlagCount()) {
            val mask = 1L shl i
            if ((flags and mask) != 0L && (state and mask) == 0L) {
                state = state or mask
                ++n_added
            }
        }
        return n_added
    }

    fun getRedstoneDustCount(): Int {
        var n = 0
        var rem = getWireFlags()
        var i = 0
        while (rem != 0 && i < RedstoneTrackDefs.STATE_FLAG_WIR_COUNT) {
            if ((rem and 1) != 0) ++n
            rem = rem shr 1
            ++i
        }
        rem = getConnectionFlags()
        i = 0
        while (rem != 0 && i < RedstoneTrackDefs.STATE_FLAG_CON_COUNT) {
            if ((rem and 1) != 0) ++n
            rem = rem shr 1
            ++i
        }
        return n
    }
}
