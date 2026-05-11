package wile.redstonepen.blocks.track

import net.minecraft.core.Direction
import net.minecraft.util.Tuple
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import wile.redstonepen.util.Auxiliaries

object RedstoneTrackDefs {
    const val STATE_FLAG_WIR_MASK: Long = 0x0000000000ffffffL
    const val STATE_FLAG_CON_MASK: Long = 0x000000003f000000L
    const val STATE_FLAG_PWR_MASK: Long = 0x00ffffff00000000L
    const val STATE_FLAG_WIR_COUNT: Int = 24
    const val STATE_FLAG_CON_COUNT: Int = 6
    const val STATE_FLAG_WIR_POS: Int = 0
    const val STATE_FLAG_CON_POS: Int = 24
    const val STATE_FLAG_PWR_POS: Int = 32

    @JvmField
    val REDSTONE_UPDATE_DIRECTIONS: Array<Direction> =
        arrayOf(
            Direction.WEST,
            Direction.EAST,
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
        )

    object connections {
        @JvmField
        val CONNECTION_BIT_ORDER: Array<Direction> =
            arrayOf(
                Direction.DOWN,
                Direction.UP,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST,
            )

        @JvmField
        val CONNECTION_BIT_ORDER_REV: Map<Direction, Int> =
            mapOf(
                Direction.DOWN to 0,
                Direction.UP to 1,
                Direction.NORTH to 2,
                Direction.SOUTH to 3,
                Direction.EAST to 4,
                Direction.WEST to 5,
            )

        @JvmField
        val BULK_FACE_MAPPING: Map<Long, Direction> =
            mapOf(
                0x0000000000000000L to Direction.DOWN,
                0x0000000001000000L to Direction.DOWN,
                0x0000000002000000L to Direction.UP,
                0x0000000004000000L to Direction.NORTH,
                0x0000000008000000L to Direction.SOUTH,
                0x0000000010000000L to Direction.EAST,
                0x0000000020000000L to Direction.WEST,
            )

        @JvmField
        val BULK_FACE_MAPPING_REV: Map<Direction, Long> =
            mapOf(
                Direction.DOWN to 0x0000000001000000L,
                Direction.UP to 0x0000000002000000L,
                Direction.NORTH to 0x0000000004000000L,
                Direction.SOUTH to 0x0000000008000000L,
                Direction.EAST to 0x0000000010000000L,
                Direction.WEST to 0x0000000020000000L,
            )

        @JvmField
        val WIRE_FACE_DIRECTION_MAPPING: Map<Long, Tuple<Direction, Direction>> =
            mapOf(
                0x00000000L to Tuple(Direction.DOWN, Direction.DOWN),
                0x00000001L to Tuple(Direction.DOWN, Direction.NORTH),
                0x00000002L to Tuple(Direction.DOWN, Direction.SOUTH),
                0x00000004L to Tuple(Direction.DOWN, Direction.EAST),
                0x00000008L to Tuple(Direction.DOWN, Direction.WEST),
                0x00000010L to Tuple(Direction.UP, Direction.NORTH),
                0x00000020L to Tuple(Direction.UP, Direction.SOUTH),
                0x00000040L to Tuple(Direction.UP, Direction.EAST),
                0x00000080L to Tuple(Direction.UP, Direction.WEST),
                0x00000100L to Tuple(Direction.NORTH, Direction.UP),
                0x00000200L to Tuple(Direction.NORTH, Direction.DOWN),
                0x00000400L to Tuple(Direction.NORTH, Direction.EAST),
                0x00000800L to Tuple(Direction.NORTH, Direction.WEST),
                0x00001000L to Tuple(Direction.SOUTH, Direction.UP),
                0x00002000L to Tuple(Direction.SOUTH, Direction.DOWN),
                0x00004000L to Tuple(Direction.SOUTH, Direction.EAST),
                0x00008000L to Tuple(Direction.SOUTH, Direction.WEST),
                0x00010000L to Tuple(Direction.EAST, Direction.UP),
                0x00020000L to Tuple(Direction.EAST, Direction.DOWN),
                0x00040000L to Tuple(Direction.EAST, Direction.NORTH),
                0x00080000L to Tuple(Direction.EAST, Direction.SOUTH),
                0x00100000L to Tuple(Direction.WEST, Direction.UP),
                0x00200000L to Tuple(Direction.WEST, Direction.DOWN),
                0x00400000L to Tuple(Direction.WEST, Direction.NORTH),
                0x00800000L to Tuple(Direction.WEST, Direction.SOUTH),
            )

        @JvmField
        val INTERNAL_EDGE_CONNECTION_MAPPING: Map<Long, Tuple<Direction, Direction>> =
            mapOf(
                (0x00000001L or 0x00000200L) to Tuple(Direction.DOWN, Direction.NORTH),
                (0x00000002L or 0x00002000L) to Tuple(Direction.DOWN, Direction.SOUTH),
                (0x00000004L or 0x00020000L) to Tuple(Direction.DOWN, Direction.EAST),
                (0x00000008L or 0x00200000L) to Tuple(Direction.DOWN, Direction.WEST),
                (0x00000010L or 0x00000100L) to Tuple(Direction.UP, Direction.NORTH),
                (0x00000020L or 0x00001000L) to Tuple(Direction.UP, Direction.SOUTH),
                (0x00000040L or 0x00010000L) to Tuple(Direction.UP, Direction.EAST),
                (0x00000080L or 0x00100000L) to Tuple(Direction.UP, Direction.WEST),
                (0x00000400L or 0x00040000L) to Tuple(Direction.NORTH, Direction.EAST),
                (0x00000800L or 0x00400000L) to Tuple(Direction.NORTH, Direction.WEST),
                (0x00004000L or 0x00080000L) to Tuple(Direction.SOUTH, Direction.EAST),
                (0x00008000L or 0x00800000L) to Tuple(Direction.SOUTH, Direction.WEST),
            )

        @JvmStatic fun getBulkConnectorBit(face: Direction): Long = BULK_FACE_MAPPING_REV[face]!!

        @JvmStatic
        fun getWireBit(face: Direction, wire_direction: Direction): Long =
            WIRE_FACE_DIRECTION_MAPPING.entries
                .firstOrNull { it.value.getA() == face && it.value.getB() == wire_direction }
                ?.key ?: 0L

        @JvmStatic
        fun getWireBitSideAndDirection(wirebit: Long): Tuple<Direction, Direction> =
            WIRE_FACE_DIRECTION_MAPPING.getOrDefault(wirebit, Tuple(Direction.DOWN, Direction.DOWN))

        @JvmStatic
        fun getVanillaWireConnectionDirections(mask: Long): List<Direction> {
            if ((mask and 0x0000000fL) == 0L) return emptyList()
            val r = ArrayList<Direction>(4)
            if ((mask and 0x00000001L) != 0L) r.add(Direction.NORTH)
            if ((mask and 0x00000002L) != 0L) r.add(Direction.SOUTH)
            if ((mask and 0x00000004L) != 0L) r.add(Direction.EAST)
            if ((mask and 0x00000008L) != 0L) r.add(Direction.WEST)
            return r
        }

        @JvmStatic
        fun hasVanillaWireConnection(mask: Long, side: Direction): Boolean =
            when (side) {
                Direction.NORTH -> (mask and 0x00000001L) != 0L
                Direction.SOUTH -> (mask and 0x00000002L) != 0L
                Direction.EAST -> (mask and 0x00000004L) != 0L
                Direction.WEST -> (mask and 0x00000008L) != 0L
                else -> false
            }

        @JvmStatic
        fun hasBulkConnection(mask: Long, side: Direction): Boolean =
            (BULK_FACE_MAPPING_REV[side]!! and mask) != 0L

        @JvmStatic
        fun hasRedstoneConnection(mask: Long, side: Direction): Boolean =
            when (side) {
                Direction.DOWN -> (mask and 0x01222200L) != 0L
                Direction.UP -> (mask and 0x02111100L) != 0L
                Direction.NORTH -> (mask and 0x04440011L) != 0L
                Direction.SOUTH -> (mask and 0x08880022L) != 0L
                Direction.EAST -> (mask and 0x10004444L) != 0L
                Direction.WEST -> (mask and 0x20008888L) != 0L
            }

        @JvmStatic
        fun getWireElementsOnFace(face: Direction): Long {
            val index = CONNECTION_BIT_ORDER_REV[face] ?: 0
            return 0xfL shl ((index * 4) + STATE_FLAG_WIR_POS)
        }

        @JvmStatic
        fun getAllElementsOnFace(face: Direction): Long {
            val index = CONNECTION_BIT_ORDER_REV[face] ?: 0
            return (0xfL shl ((index * 4) + STATE_FLAG_WIR_POS)) or
                (0x1L shl (index + STATE_FLAG_CON_POS))
        }
    }

    object shape {
        private const val SHAPE_LAYER_THICKNESS = 0.01
        private const val SHAPE_TRACK_HALFWIDTH = 2.0

        private val DOWN_SHAPE: VoxelShape =
            Auxiliaries.getUnionShape(
                Auxiliaries.getPixeledAABB(
                    8 - SHAPE_TRACK_HALFWIDTH,
                    0.0,
                    0.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    SHAPE_LAYER_THICKNESS,
                    16.0,
                ),
                Auxiliaries.getPixeledAABB(
                    0.0,
                    0.0,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    16.0,
                    SHAPE_LAYER_THICKNESS,
                    8 + SHAPE_TRACK_HALFWIDTH,
                ),
            )
        private val UP_SHAPE: VoxelShape =
            Auxiliaries.getUnionShape(
                Auxiliaries.getPixeledAABB(
                    8 - SHAPE_TRACK_HALFWIDTH,
                    16 - SHAPE_LAYER_THICKNESS,
                    0.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    16.0,
                    16.0,
                ),
                Auxiliaries.getPixeledAABB(
                    0.0,
                    16 - SHAPE_LAYER_THICKNESS,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    16.0,
                    16.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                ),
            )
        private val WEST_SHAPE: VoxelShape =
            Auxiliaries.getUnionShape(
                Auxiliaries.getPixeledAABB(
                    0.0,
                    0.0,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    SHAPE_LAYER_THICKNESS,
                    16.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                ),
                Auxiliaries.getPixeledAABB(
                    0.0,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    0.0,
                    SHAPE_LAYER_THICKNESS,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    16.0,
                ),
            )
        private val EAST_SHAPE: VoxelShape =
            Auxiliaries.getUnionShape(
                Auxiliaries.getPixeledAABB(
                    16 - SHAPE_LAYER_THICKNESS,
                    0.0,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    16.0,
                    16.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                ),
                Auxiliaries.getPixeledAABB(
                    16 - SHAPE_LAYER_THICKNESS,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    0.0,
                    16.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    16.0,
                ),
            )
        private val NORTH_SHAPE: VoxelShape =
            Auxiliaries.getUnionShape(
                Auxiliaries.getPixeledAABB(
                    0.0,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    0.0,
                    16.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    SHAPE_LAYER_THICKNESS,
                ),
                Auxiliaries.getPixeledAABB(
                    8 - SHAPE_TRACK_HALFWIDTH,
                    0.0,
                    0.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    16.0,
                    SHAPE_LAYER_THICKNESS,
                ),
            )
        private val SOUTH_SHAPE: VoxelShape =
            Auxiliaries.getUnionShape(
                Auxiliaries.getPixeledAABB(
                    0.0,
                    8 - SHAPE_TRACK_HALFWIDTH,
                    16 - SHAPE_LAYER_THICKNESS,
                    16.0,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    16.0,
                ),
                Auxiliaries.getPixeledAABB(
                    8 - SHAPE_TRACK_HALFWIDTH,
                    0.0,
                    16 - SHAPE_LAYER_THICKNESS,
                    8 + SHAPE_TRACK_HALFWIDTH,
                    16.0,
                    16.0,
                ),
            )

        private val shape_cache: Array<VoxelShape?> = arrayOfNulls(64)

        @JvmStatic
        fun get(faces: Int): VoxelShape {
            if (shape_cache[faces] == null) {
                var shape = Shapes.empty()
                if ((faces and 0x01) != 0) shape = Shapes.join(shape, DOWN_SHAPE, BooleanOp.OR)
                if ((faces and 0x02) != 0) shape = Shapes.join(shape, UP_SHAPE, BooleanOp.OR)
                if ((faces and 0x04) != 0) shape = Shapes.join(shape, NORTH_SHAPE, BooleanOp.OR)
                if ((faces and 0x08) != 0) shape = Shapes.join(shape, SOUTH_SHAPE, BooleanOp.OR)
                if ((faces and 0x10) != 0) shape = Shapes.join(shape, EAST_SHAPE, BooleanOp.OR)
                if ((faces and 0x20) != 0) shape = Shapes.join(shape, WEST_SHAPE, BooleanOp.OR)
                shape_cache[faces] = shape
            }
            return shape_cache[faces]!!
        }
    }

    object models {
        @JvmField
        val STATE_WIRE_MAPPING: Map<Long, String> =
            mapOf(
                0x00000000L to "none",
                0x00000001L to "dn",
                0x00000002L to "ds",
                0x00000004L to "de",
                0x00000008L to "dw",
                0x00000010L to "un",
                0x00000020L to "us",
                0x00000040L to "ue",
                0x00000080L to "uw",
                0x00000100L to "nu",
                0x00000200L to "nd",
                0x00000400L to "ne",
                0x00000800L to "nw",
                0x00001000L to "su",
                0x00002000L to "sd",
                0x00004000L to "se",
                0x00008000L to "sw",
                0x00010000L to "eu",
                0x00020000L to "ed",
                0x00040000L to "en",
                0x00080000L to "es",
                0x00100000L to "wu",
                0x00200000L to "wd",
                0x00400000L to "wn",
                0x00800000L to "ws",
            )

        @JvmField
        val STATE_CONNECT_MAPPING: Map<Long, String> =
            mapOf(
                0x0000000000000000L to "none",
                0x0000000001000000L to "dc",
                0x0000000002000000L to "uc",
                0x0000000004000000L to "nc",
                0x0000000008000000L to "sc",
                0x0000000010000000L to "ec",
                0x0000000020000000L to "wc",
            )

        @JvmField
        val STATE_CNTWIRE_MAPPING: Map<Long, String> =
            mapOf(
                0x0000000000000000L to "none",
                0x0000000001000000L to "dm",
                0x0000000002000000L to "um",
                0x0000000004000000L to "nm",
                0x0000000008000000L to "sm",
                0x0000000010000000L to "em",
                0x0000000020000000L to "wm",
            )
    }
}
