package wile.redstonepen.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class DemoBuilderTest
{
  @Test
  void cellOriginIndexZeroReturnsGridOrigin()
  {
    final BlockPos origin = new BlockPos(10, 64, 20);
    assertEquals(origin, DemoBuilder.cellOrigin(origin, 0, 4, 3));
  }

  @Test
  void cellOriginAdvancesAcrossColumnsBeforeAdvancingRow()
  {
    final BlockPos origin = new BlockPos(0, 0, 0);
    // columns=4, spacing=3
    assertEquals(new BlockPos(3, 0, 0), DemoBuilder.cellOrigin(origin, 1, 4, 3));
    assertEquals(new BlockPos(6, 0, 0), DemoBuilder.cellOrigin(origin, 2, 4, 3));
    assertEquals(new BlockPos(9, 0, 0), DemoBuilder.cellOrigin(origin, 3, 4, 3));
    // index 4 wraps to next row
    assertEquals(new BlockPos(0, 0, 3), DemoBuilder.cellOrigin(origin, 4, 4, 3));
    assertEquals(new BlockPos(3, 0, 3), DemoBuilder.cellOrigin(origin, 5, 4, 3));
  }

  @Test
  void cellOriginPreservesYCoordinate()
  {
    final BlockPos origin = new BlockPos(0, 100, 0);
    assertEquals(100, DemoBuilder.cellOrigin(origin, 7, 3, 5).getY());
  }

  @Test
  void cellOriginRejectsZeroColumns()
  {
    assertThrows(IllegalArgumentException.class,
      () -> DemoBuilder.cellOrigin(BlockPos.ZERO, 0, 0, 1));
  }

  @Test
  void cellOriginRejectsZeroSpacing()
  {
    assertThrows(IllegalArgumentException.class,
      () -> DemoBuilder.cellOrigin(BlockPos.ZERO, 0, 4, 0));
  }

  @Test
  void cellOriginRejectsNegativeIndex()
  {
    assertThrows(IllegalArgumentException.class,
      () -> DemoBuilder.cellOrigin(BlockPos.ZERO, -1, 4, 3));
  }

  @Test
  void regionVolumeIsInclusiveOnBothEnds()
  {
    assertEquals(1, DemoBuilder.regionVolume(BlockPos.ZERO, BlockPos.ZERO));
    assertEquals(8, DemoBuilder.regionVolume(BlockPos.ZERO, new BlockPos(1, 1, 1)));
    assertEquals(27, DemoBuilder.regionVolume(BlockPos.ZERO, new BlockPos(2, 2, 2)));
  }

  @Test
  void regionVolumeOrderIndependent()
  {
    final BlockPos a = new BlockPos(5, 5, 5);
    final BlockPos b = new BlockPos(0, 0, 0);
    assertEquals(DemoBuilder.regionVolume(a, b), DemoBuilder.regionVolume(b, a));
  }
}
