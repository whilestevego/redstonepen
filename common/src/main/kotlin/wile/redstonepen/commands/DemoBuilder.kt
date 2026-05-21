package wile.redstonepen.commands

import kotlin.math.abs
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.WallSignBlock
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties

object DemoBuilder {

    const val FLAGS = Block.UPDATE_NEIGHBORS or Block.UPDATE_CLIENTS

    @JvmStatic
    fun cellOrigin(gridOrigin: BlockPos, cellIndex: Int, columns: Int, spacing: Int): BlockPos {
        require(columns > 0) { "columns must be > 0" }
        require(spacing > 0) { "spacing must be > 0" }
        require(cellIndex >= 0) { "cellIndex must be >= 0" }
        val col = cellIndex % columns
        val row = cellIndex / columns
        return gridOrigin.offset(col * spacing, 0, row * spacing)
    }

    @JvmStatic
    fun placeAttached(level: Level, pos: BlockPos, state: BlockState) {
        val mount =
            if (state.hasProperty(BlockStateProperties.FACING)) {
                state.getValue(BlockStateProperties.FACING)
            } else {
                Direction.DOWN
            }
        val supportPos = pos.relative(mount)
        level.setBlock(supportPos, Blocks.STONE.defaultBlockState(), FLAGS)
        level.setBlock(pos, state, FLAGS)
    }

    @JvmStatic
    fun clearRegion(level: Level, min: BlockPos, max: BlockPos) {
        val x0 = minOf(min.x, max.x)
        val x1 = maxOf(min.x, max.x)
        val y0 = minOf(min.y, max.y)
        val y1 = maxOf(min.y, max.y)
        val z0 = minOf(min.z, max.z)
        val z1 = maxOf(min.z, max.z)
        val air = Blocks.AIR.defaultBlockState()
        val m = BlockPos.MutableBlockPos()
        for (x in x0..x1) {
            for (y in y0..y1) {
                for (z in z0..z1) {
                    level.setBlock(m.set(x, y, z), air, FLAGS)
                }
            }
        }
    }

    @JvmStatic
    fun regionVolume(min: BlockPos, max: BlockPos): Int {
        val dx = abs(max.x - min.x) + 1
        val dy = abs(max.y - min.y) + 1
        val dz = abs(max.z - min.z) + 1
        return dx * dy * dz
    }

    @JvmStatic
    fun placeWallSign(level: Level, pos: BlockPos, facing: Direction, vararg lines: String) {
        val sign = Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing)
        level.setBlock(pos.relative(facing.opposite), Blocks.STONE.defaultBlockState(), FLAGS)
        level.setBlock(pos, sign, FLAGS)
        (level.getBlockEntity(pos) as? SignBlockEntity)?.let { configureSign(it, lines) }
    }

    @JvmStatic
    fun placeStandingSign(level: Level, pos: BlockPos, rotation: Int, vararg lines: String) {
        val sign =
            Blocks.OAK_SIGN.defaultBlockState()
                .setValue(BlockStateProperties.ROTATION_16, rotation and 0xf)
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), FLAGS)
        level.setBlock(pos, sign, FLAGS)
        (level.getBlockEntity(pos) as? SignBlockEntity)?.let { configureSign(it, lines) }
    }

    private fun configureSign(be: SignBlockEntity, lines: Array<out String>) {
        val msgs =
            Array(4) { i -> Component.literal(if (i < lines.size) truncate(lines[i]) else "") }
        be.updateText(
            { text ->
                text
                    .setMessage(0, msgs[0])
                    .setMessage(1, msgs[1])
                    .setMessage(2, msgs[2])
                    .setMessage(3, msgs[3])
            },
            true,
        )
        be.setChanged()
    }

    private fun truncate(s: String): String = if (s.length <= 15) s else s.substring(0, 15)
}
