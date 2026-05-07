package wile.redstonepen.blocks;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import wile.redstonepen.McBootstrap;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectedComponentBlockTest
{
  @BeforeAll
  static void bootstrap() { McBootstrap.bootstrap(); }

  static Stream<Arguments> placementRotationCases()
  {
    return Stream.of(
      // Floor / ceiling: NORTH=0, EAST=1, SOUTH=2, WEST=3
      Arguments.of(Direction.DOWN,  Direction.NORTH, 0),
      Arguments.of(Direction.DOWN,  Direction.EAST,  1),
      Arguments.of(Direction.DOWN,  Direction.SOUTH, 2),
      Arguments.of(Direction.DOWN,  Direction.WEST,  3),
      Arguments.of(Direction.DOWN,  Direction.DOWN,  0),
      Arguments.of(Direction.DOWN,  Direction.UP,    0),
      Arguments.of(Direction.UP,    Direction.NORTH, 0),
      Arguments.of(Direction.UP,    Direction.EAST,  1),
      Arguments.of(Direction.UP,    Direction.SOUTH, 2),
      Arguments.of(Direction.UP,    Direction.WEST,  3),
      Arguments.of(Direction.UP,    Direction.DOWN,  0),
      Arguments.of(Direction.UP,    Direction.UP,    0),
      // North wall: UP=0, EAST=1, DOWN=2, WEST=3
      Arguments.of(Direction.NORTH, Direction.UP,    0),
      Arguments.of(Direction.NORTH, Direction.EAST,  1),
      Arguments.of(Direction.NORTH, Direction.DOWN,  2),
      Arguments.of(Direction.NORTH, Direction.WEST,  3),
      Arguments.of(Direction.NORTH, Direction.NORTH, 0),
      Arguments.of(Direction.NORTH, Direction.SOUTH, 0),
      // South wall: UP=0, WEST=1, DOWN=2, EAST=3
      Arguments.of(Direction.SOUTH, Direction.UP,    0),
      Arguments.of(Direction.SOUTH, Direction.WEST,  1),
      Arguments.of(Direction.SOUTH, Direction.DOWN,  2),
      Arguments.of(Direction.SOUTH, Direction.EAST,  3),
      Arguments.of(Direction.SOUTH, Direction.SOUTH, 0),
      Arguments.of(Direction.SOUTH, Direction.NORTH, 0),
      // West wall: UP=0, NORTH=1, DOWN=2, SOUTH=3
      Arguments.of(Direction.WEST,  Direction.UP,    0),
      Arguments.of(Direction.WEST,  Direction.NORTH, 1),
      Arguments.of(Direction.WEST,  Direction.DOWN,  2),
      Arguments.of(Direction.WEST,  Direction.SOUTH, 3),
      Arguments.of(Direction.WEST,  Direction.WEST,  0),
      Arguments.of(Direction.WEST,  Direction.EAST,  0),
      // East wall: UP=0, SOUTH=1, DOWN=2, NORTH=3
      Arguments.of(Direction.EAST,  Direction.UP,    0),
      Arguments.of(Direction.EAST,  Direction.SOUTH, 1),
      Arguments.of(Direction.EAST,  Direction.DOWN,  2),
      Arguments.of(Direction.EAST,  Direction.NORTH, 3),
      Arguments.of(Direction.EAST,  Direction.EAST,  0),
      Arguments.of(Direction.EAST,  Direction.WEST,  0)
    );
  }

  @ParameterizedTest(name = "face={0} dir={1} → rotation {2}")
  @MethodSource("placementRotationCases")
  void placementRotation(Direction face, Direction dir, int expectedRotation)
  {
    assertEquals(expectedRotation,
      CircuitComponents.DirectedComponentBlock.placementRotation(face, dir),
      () -> "face=" + face + " dir=" + dir);
  }
}
