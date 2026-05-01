package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import wile.redstonepen.libmc.Auxiliaries.BlockPosRange;

import java.util.HashSet;
import java.util.Set;

class AuxiliariesAABBTest
{
  private static final double EPS = 1e-9;

  private static void assertAABB(AABB expected, AABB actual)
  {
    assertEquals(expected.minX, actual.minX, EPS, "minX");
    assertEquals(expected.minY, actual.minY, EPS, "minY");
    assertEquals(expected.minZ, actual.minZ, EPS, "minZ");
    assertEquals(expected.maxX, actual.maxX, EPS, "maxX");
    assertEquals(expected.maxY, actual.maxY, EPS, "maxY");
    assertEquals(expected.maxZ, actual.maxZ, EPS, "maxZ");
  }

  // --- getPixeledAABB ---

  @Test
  void pixeledAABBDividesAllCoordinatesBySixteen()
  {
    assertAABB(new AABB(0, 0, 0, 1, 1, 1), Auxiliaries.getPixeledAABB(0, 0, 0, 16, 16, 16));
  }

  @Test
  void pixeledAABBPreservesAsymmetricBounds()
  {
    assertAABB(new AABB(0.25, 0.5, 0, 0.75, 1.0, 0.25), Auxiliaries.getPixeledAABB(4, 8, 0, 12, 16, 4));
  }

  // --- getRotatedAABB (non-horizontal) ---

  @Test
  void rotatedAABBNorthIsIdentity()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.NORTH));
  }

  @Test
  void rotatedAABBSouthMirrorsXandZ()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // case 3: (1-maxX, minY, 1-maxZ, 1-minX, maxY, 1-minZ)
    assertAABB(new AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7), Auxiliaries.getRotatedAABB(bb, Direction.SOUTH));
  }

  @Test
  void rotatedAABBDownSwapsYandZ()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // case 0: (1-maxX, minZ, minY, 1-minX, maxZ, maxY)
    assertAABB(new AABB(0.4, 0.3, 0.2, 0.9, 0.8, 0.7), Auxiliaries.getRotatedAABB(bb, Direction.DOWN));
  }

  @Test
  void rotatedAABBUpInvertsAllThreeAxes()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // case 1: (1-maxX, 1-maxZ, 1-maxY, 1-minX, 1-minZ, 1-minY)
    assertAABB(new AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8), Auxiliaries.getRotatedAABB(bb, Direction.UP));
  }

  @Test
  void rotatedAABBWestSwapsXandZ()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // case 4: (minZ, minY, 1-maxX, maxZ, maxY, 1-minX)
    assertAABB(new AABB(0.3, 0.2, 0.4, 0.8, 0.7, 0.9), Auxiliaries.getRotatedAABB(bb, Direction.WEST));
  }

  @Test
  void rotatedAABBEastSwapsXandZ()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // case 5: (1-maxZ, minY, minX, 1-minZ, maxY, maxX)
    assertAABB(new AABB(0.2, 0.2, 0.1, 0.7, 0.7, 0.6), Auxiliaries.getRotatedAABB(bb, Direction.EAST));
  }

  // --- getYRotatedAABB ---

  @Test
  void yRotatedAABBZeroStepsIsIdentity()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    assertAABB(bb, Auxiliaries.getYRotatedAABB(bb, 0));
  }

  @Test
  void yRotatedAABBFourStepsIsIdentity()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    assertAABB(bb, Auxiliaries.getYRotatedAABB(bb, 4));
  }

  @Test
  void yRotatedAABBTwoStepsIs180Degrees()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // 2 steps = SOUTH horizontal: (1-maxX, minY, 1-maxZ, 1-minX, maxY, 1-minZ)
    assertAABB(new AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7), Auxiliaries.getYRotatedAABB(bb, 2));
  }

  @Test
  void yRotatedAABBNegativeStepNormalizesViaModulo()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // (-1 + 4096) & 3 = 3 → WEST horizontal: (minZ, minY, 1-maxX, maxZ, maxY, 1-minX)
    assertAABB(Auxiliaries.getYRotatedAABB(bb, 3), Auxiliaries.getYRotatedAABB(bb, -1));
  }

  // --- getMirroredAABB ---

  @Test
  void mirroredAABBXAxisSwapsXCoordinates()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // X: (1-maxX, minY, minZ, 1-minX, maxY, maxZ)
    assertAABB(new AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8), Auxiliaries.getMirroredAABB(bb, Direction.Axis.X));
  }

  @Test
  void mirroredAABBYAxisSwapsYCoordinates()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // Y: (minX, 1-maxY, minZ, maxX, 1-minY, maxZ)
    assertAABB(new AABB(0.1, 0.3, 0.3, 0.6, 0.8, 0.8), Auxiliaries.getMirroredAABB(bb, Direction.Axis.Y));
  }

  @Test
  void mirroredAABBZAxisSwapsZCoordinates()
  {
    final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
    // Z: (minX, minY, 1-maxZ, maxX, maxY, 1-minZ)
    assertAABB(new AABB(0.1, 0.2, 0.2, 0.6, 0.7, 0.7), Auxiliaries.getMirroredAABB(bb, Direction.Axis.Z));
  }

  // --- BlockPosRange ---

  @Test
  void blockPosRangeVolumeEqualsProductOfDimensions()
  {
    final BlockPosRange range = new BlockPosRange(0, 0, 0, 2, 1, 1);
    assertEquals(3, range.getXSize());
    assertEquals(2, range.getYSize());
    assertEquals(2, range.getZSize());
    assertEquals(range.getXSize() * range.getYSize() * range.getZSize(), range.getVolume());
  }

  @Test
  void blockPosRangeByXZYIndexZeroReturnsOriginCorner()
  {
    final BlockPosRange range = new BlockPosRange(1, 2, 3, 3, 4, 5);
    assertEquals(new BlockPos(1, 2, 3), range.byXZYIndex(0));
  }

  @Test
  void blockPosRangeByXZYIndexLastReturnsMaxCorner()
  {
    final BlockPosRange range = new BlockPosRange(0, 0, 0, 2, 1, 1);
    // xsz=3, ysz=2, zsz=2, volume=12; last index=11
    // y=11/6=1, rem=5; z=5/3=1, rem=2; x=2 → (2,1,1)
    assertEquals(new BlockPos(2, 1, 1), range.byXZYIndex(range.getVolume() - 1));
  }

  @Test
  void blockPosRangeIteratorCoversEveryPositionExactlyOnce()
  {
    final BlockPosRange range = new BlockPosRange(0, 0, 0, 1, 1, 1); // 2x2x2 = 8 positions
    final Set<BlockPos> visited = new HashSet<>();
    for(BlockPos pos : range) {
      assertNotSame(null, pos);
      visited.add(pos);
    }
    assertEquals(range.getVolume(), visited.size());
  }
}
