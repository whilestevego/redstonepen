package wile.redstonepen.blocks.track

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class TrackNet(
    val neighbour_positions: List<BlockPos>,
    val neighbour_sides: List<Direction>,
    val internal_sides: List<Direction>,
    val power_sides: List<Direction>,
    power: Int = 0,
) {
    internal var power: Int = power

    override fun toString(): String {
        var s = "NET{"
        s += "p:$power"
        s += ", intsides:" + internal_sides.joinToString("") { TrackBlockEntity.dirstr(it) }
        s += ", pwrsides:" + power_sides.joinToString("") { TrackBlockEntity.dirstr(it) }
        s += ", nbsides:" + neighbour_sides.joinToString("") { TrackBlockEntity.dirstr(it) }
        s += ", nbpos:" + neighbour_positions.joinToString(",") { TrackBlockEntity.posstr(it) }
        return "$s}"
    }
}
