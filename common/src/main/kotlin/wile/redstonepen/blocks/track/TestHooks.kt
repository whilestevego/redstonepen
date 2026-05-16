package wile.redstonepen.blocks.track

import net.minecraft.core.Direction

class TestHooks {
    var state: Long = 0L

    private val flags
        get() = TrackStateFlags(state)

    private fun update(f: TrackStateFlags) {
        state = f.raw
    }

    fun getWireFlags(): Int = flags.wireFlags

    fun getWireFlag(index: Int): Boolean = flags.wireFlag(index)

    fun getWireFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_WIR_COUNT

    fun getConnectionFlags(): Int = flags.connectionFlags

    fun getConnectionFlag(index: Int): Boolean = flags.connectionFlag(index)

    fun getConnectionFlagCount(): Int = RedstoneTrackDefs.STATE_FLAG_CON_COUNT

    fun getSidePower(side: Direction): Int = flags.sidePower(side)

    fun setSidePower(side: Direction, p: Int) {
        update(flags.withSidePower(side, p))
    }

    fun addWireFlags(flags: Long): Int {
        val (newFlags, added) = this.flags.withAddedWireFlags(flags)
        update(newFlags)
        return added
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
