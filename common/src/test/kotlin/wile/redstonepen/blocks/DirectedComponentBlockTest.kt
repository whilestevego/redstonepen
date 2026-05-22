package wile.redstonepen.blocks

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState

private data class RotationCase(val face: Direction, val dir: Direction, val expected: Int)

private data class FacingCase(
    val face: Direction,
    val rotation: Int,
    val front: Direction,
    val right: Direction,
    val back: Direction,
    val left: Direction,
)

private fun mockState(face: Direction, rotation: Int): BlockState {
    val state = mockk<BlockState>()
    every { state.getValue(CircuitComponents.DirectedComponentBlock.FACING) } returns face
    every { state.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) } returns rotation
    return state
}

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
                CircuitComponents.DirectedComponentBlock.placementRotation(face, dir) shouldBe
                    expected
            }
        }

        describe("getFrontFacing / getRightFacing / getBackFacing / getLeftFacing") {
            withData(
                //           face            rot  front          right          back           left
                FacingCase(
                    Direction.DOWN,
                    0,
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST,
                ),
                FacingCase(
                    Direction.DOWN,
                    1,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.NORTH,
                ),
                FacingCase(
                    Direction.DOWN,
                    2,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.NORTH,
                    Direction.EAST,
                ),
                FacingCase(
                    Direction.DOWN,
                    3,
                    Direction.WEST,
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                ),
                FacingCase(
                    Direction.UP,
                    0,
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST,
                ),
                FacingCase(
                    Direction.UP,
                    1,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.NORTH,
                ),
                FacingCase(
                    Direction.UP,
                    2,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.NORTH,
                    Direction.EAST,
                ),
                FacingCase(
                    Direction.UP,
                    3,
                    Direction.WEST,
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                ),
                FacingCase(
                    Direction.NORTH,
                    0,
                    Direction.UP,
                    Direction.EAST,
                    Direction.DOWN,
                    Direction.WEST,
                ),
                FacingCase(
                    Direction.NORTH,
                    1,
                    Direction.EAST,
                    Direction.DOWN,
                    Direction.WEST,
                    Direction.UP,
                ),
                FacingCase(
                    Direction.NORTH,
                    2,
                    Direction.DOWN,
                    Direction.WEST,
                    Direction.UP,
                    Direction.EAST,
                ),
                FacingCase(
                    Direction.NORTH,
                    3,
                    Direction.WEST,
                    Direction.UP,
                    Direction.EAST,
                    Direction.DOWN,
                ),
                FacingCase(
                    Direction.SOUTH,
                    0,
                    Direction.UP,
                    Direction.WEST,
                    Direction.DOWN,
                    Direction.EAST,
                ),
                FacingCase(
                    Direction.SOUTH,
                    1,
                    Direction.WEST,
                    Direction.DOWN,
                    Direction.EAST,
                    Direction.UP,
                ),
                FacingCase(
                    Direction.SOUTH,
                    2,
                    Direction.DOWN,
                    Direction.EAST,
                    Direction.UP,
                    Direction.WEST,
                ),
                FacingCase(
                    Direction.SOUTH,
                    3,
                    Direction.EAST,
                    Direction.UP,
                    Direction.WEST,
                    Direction.DOWN,
                ),
                FacingCase(
                    Direction.WEST,
                    0,
                    Direction.UP,
                    Direction.NORTH,
                    Direction.DOWN,
                    Direction.SOUTH,
                ),
                FacingCase(
                    Direction.WEST,
                    1,
                    Direction.NORTH,
                    Direction.DOWN,
                    Direction.SOUTH,
                    Direction.UP,
                ),
                FacingCase(
                    Direction.WEST,
                    2,
                    Direction.DOWN,
                    Direction.SOUTH,
                    Direction.UP,
                    Direction.NORTH,
                ),
                FacingCase(
                    Direction.WEST,
                    3,
                    Direction.SOUTH,
                    Direction.UP,
                    Direction.NORTH,
                    Direction.DOWN,
                ),
                FacingCase(
                    Direction.EAST,
                    0,
                    Direction.UP,
                    Direction.SOUTH,
                    Direction.DOWN,
                    Direction.NORTH,
                ),
                FacingCase(
                    Direction.EAST,
                    1,
                    Direction.SOUTH,
                    Direction.DOWN,
                    Direction.NORTH,
                    Direction.UP,
                ),
                FacingCase(
                    Direction.EAST,
                    2,
                    Direction.DOWN,
                    Direction.NORTH,
                    Direction.UP,
                    Direction.SOUTH,
                ),
                FacingCase(
                    Direction.EAST,
                    3,
                    Direction.NORTH,
                    Direction.UP,
                    Direction.SOUTH,
                    Direction.DOWN,
                ),
            ) { (face, rotation, front, right, back, left) ->
                val state = mockState(face, rotation)
                assertSoftly {
                    CircuitComponents.DirectedComponentBlock.getFrontFacing(state) shouldBe front
                    CircuitComponents.DirectedComponentBlock.getRightFacing(state) shouldBe right
                    CircuitComponents.DirectedComponentBlock.getBackFacing(state) shouldBe back
                    CircuitComponents.DirectedComponentBlock.getLeftFacing(state) shouldBe left
                }
            }
        }

        describe("getUpFacing / getDownFacing") {
            it("getUpFacing returns face.opposite for all six faces") {
                for (face in Direction.entries) {
                    val state = mockState(face, 0)
                    CircuitComponents.DirectedComponentBlock.getUpFacing(state) shouldBe
                        face.opposite
                }
            }

            it("getDownFacing returns face itself for all six faces") {
                for (face in Direction.entries) {
                    val state = mockState(face, 0)
                    CircuitComponents.DirectedComponentBlock.getDownFacing(state) shouldBe face
                }
            }
        }
    })
