package wile.redstonepen.blocks

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import net.minecraft.core.Direction

private data class RotationCase(val face: Direction, val dir: Direction, val expected: Int)

class DirectedComponentBlockTest :
    DescribeSpec({
        describe("placementRotation") {
            withData(
                // Floor / ceiling: NORTH=0, EAST=1, SOUTH=2, WEST=3
                RotationCase(Direction.DOWN, Direction.NORTH, 0),
                RotationCase(Direction.DOWN, Direction.EAST, 1),
                RotationCase(Direction.DOWN, Direction.SOUTH, 2),
                RotationCase(Direction.DOWN, Direction.WEST, 3),
                RotationCase(Direction.DOWN, Direction.DOWN, 0),
                RotationCase(Direction.DOWN, Direction.UP, 0),
                RotationCase(Direction.UP, Direction.NORTH, 0),
                RotationCase(Direction.UP, Direction.EAST, 1),
                RotationCase(Direction.UP, Direction.SOUTH, 2),
                RotationCase(Direction.UP, Direction.WEST, 3),
                RotationCase(Direction.UP, Direction.DOWN, 0),
                RotationCase(Direction.UP, Direction.UP, 0),
                // North wall: UP=0, EAST=1, DOWN=2, WEST=3
                RotationCase(Direction.NORTH, Direction.UP, 0),
                RotationCase(Direction.NORTH, Direction.EAST, 1),
                RotationCase(Direction.NORTH, Direction.DOWN, 2),
                RotationCase(Direction.NORTH, Direction.WEST, 3),
                RotationCase(Direction.NORTH, Direction.NORTH, 0),
                RotationCase(Direction.NORTH, Direction.SOUTH, 0),
                // South wall: UP=0, WEST=1, DOWN=2, EAST=3
                RotationCase(Direction.SOUTH, Direction.UP, 0),
                RotationCase(Direction.SOUTH, Direction.WEST, 1),
                RotationCase(Direction.SOUTH, Direction.DOWN, 2),
                RotationCase(Direction.SOUTH, Direction.EAST, 3),
                RotationCase(Direction.SOUTH, Direction.SOUTH, 0),
                RotationCase(Direction.SOUTH, Direction.NORTH, 0),
                // West wall: UP=0, NORTH=1, DOWN=2, SOUTH=3
                RotationCase(Direction.WEST, Direction.UP, 0),
                RotationCase(Direction.WEST, Direction.NORTH, 1),
                RotationCase(Direction.WEST, Direction.DOWN, 2),
                RotationCase(Direction.WEST, Direction.SOUTH, 3),
                RotationCase(Direction.WEST, Direction.WEST, 0),
                RotationCase(Direction.WEST, Direction.EAST, 0),
                // East wall: UP=0, SOUTH=1, DOWN=2, NORTH=3
                RotationCase(Direction.EAST, Direction.UP, 0),
                RotationCase(Direction.EAST, Direction.SOUTH, 1),
                RotationCase(Direction.EAST, Direction.DOWN, 2),
                RotationCase(Direction.EAST, Direction.NORTH, 3),
                RotationCase(Direction.EAST, Direction.EAST, 0),
                RotationCase(Direction.EAST, Direction.WEST, 0),
            ) { (face, dir, expected) ->
                CircuitComponents.DirectedComponentBlock.placementRotation(face, dir) shouldBe expected
            }
        }
    })
