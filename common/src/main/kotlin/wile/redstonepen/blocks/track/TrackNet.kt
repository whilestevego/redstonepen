package wile.redstonepen.blocks.track

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class TrackNet(
    val neighbourPositions: List<BlockPos>,
    val neighbourSides: List<Direction>,
    val internalSides: List<Direction>,
    val powerSides: List<Direction>,
    internal var power: Int = 0,
) {

    override fun toString(): String {
        var s = "NET{"
        s += "p:$power"
        s += ", intsides:" + internalSides.joinToString("") { TrackBlockEntity.dirstr(it) }
        s += ", pwrsides:" + powerSides.joinToString("") { TrackBlockEntity.dirstr(it) }
        s += ", nbsides:" + neighbourSides.joinToString("") { TrackBlockEntity.dirstr(it) }
        s += ", nbpos:" + neighbourPositions.joinToString(",") { TrackBlockEntity.posstr(it) }
        return "$s}"
    }
}
