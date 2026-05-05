package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

  @Nested
  class PixeledAABB
  {
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

    @Test
    void getPixeledAABBPartialPixels()
    {
      final AABB bb = Auxiliaries.getPixeledAABB(2, 4, 6, 10, 12, 14);
      assertEquals(2.0/16.0, bb.minX, EPS);
      assertEquals(4.0/16.0, bb.minY, EPS);
      assertEquals(6.0/16.0, bb.minZ, EPS);
      assertEquals(10.0/16.0, bb.maxX, EPS);
      assertEquals(12.0/16.0, bb.maxY, EPS);
      assertEquals(14.0/16.0, bb.maxZ, EPS);
    }
  }

  @Nested
  class RotatedAABB
  {
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
      assertAABB(new AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7), Auxiliaries.getRotatedAABB(bb, Direction.SOUTH));
    }

    @Test
    void rotatedAABBDownSwapsYandZ()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
      assertAABB(new AABB(0.4, 0.3, 0.2, 0.9, 0.8, 0.7), Auxiliaries.getRotatedAABB(bb, Direction.DOWN));
    }

    @Test
    void rotatedAABBUpInvertsAllThreeAxes()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
      assertAABB(new AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8), Auxiliaries.getRotatedAABB(bb, Direction.UP));
    }

    @Test
    void rotatedAABBWestSwapsXandZ()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
      assertAABB(new AABB(0.3, 0.2, 0.4, 0.8, 0.7, 0.9), Auxiliaries.getRotatedAABB(bb, Direction.WEST));
    }

    @Test
    void rotatedAABBEastSwapsXandZ()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
      assertAABB(new AABB(0.2, 0.2, 0.1, 0.7, 0.7, 0.6), Auxiliaries.getRotatedAABB(bb, Direction.EAST));
    }

    @Test
    void getRotatedAABBAllSixFacesProduceValidBoxes()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
      for(Direction d : Direction.values()) {
        final AABB out = Auxiliaries.getRotatedAABB(bb, d);
        assertNotNull(out);
        assertTrue(out.maxX > out.minX);
        assertTrue(out.maxY > out.minY);
        assertTrue(out.maxZ > out.minZ);
      }
    }

    @Test
    void getRotatedAABBArrayPreservesLength()
    {
      final AABB[] in = { new AABB(0,0,0,1,1,1), new AABB(0.2,0.2,0.2,0.8,0.8,0.8) };
      assertEquals(in.length, Auxiliaries.getRotatedAABB(in, Direction.EAST).length);
    }

    @Test
    void getRotatedAABBArrayWithEmptyInput()
    {
      assertEquals(0, Auxiliaries.getRotatedAABB(new AABB[0], Direction.NORTH).length);
    }
  }

  @Nested
  class RotatedAABBHorizontal
  {
    @Test
    void getRotatedAABBHorizontalKeepsYAxis()
    {
      final AABB bb = new AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9);
      final AABB south = Auxiliaries.getRotatedAABB(bb, Direction.SOUTH, true);
      assertEquals(bb.minY, south.minY, EPS);
      assertEquals(bb.maxY, south.maxY, EPS);
    }

    @Test
    void getRotatedAABBHorizontalDownAndUpAreIdentity()
    {
      final AABB bb = new AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9);
      assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.DOWN, true));
      assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.UP, true));
    }

    @Test
    void getRotatedAABBHorizontalAllSixFacesProduceValidBoxes()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
      for(Direction d : Direction.values()) {
        final AABB out = Auxiliaries.getRotatedAABB(bb, d, true);
        assertNotNull(out);
        assertTrue(out.maxX > out.minX);
        assertTrue(out.maxY > out.minY);
        assertTrue(out.maxZ > out.minZ);
      }
    }

    @Test
    void getRotatedAABBArrayHorizontalPreservesLength()
    {
      final AABB[] in = { new AABB(0,0,0,1,1,1) };
      assertEquals(in.length, Auxiliaries.getRotatedAABB(in, Direction.SOUTH, true).length);
    }
  }

  @Nested
  class YRotatedAABB
  {
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
      assertAABB(new AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7), Auxiliaries.getYRotatedAABB(bb, 2));
    }

    @Test
    void getYRotatedAABBHandlesNegativeAndModularSteps()
    {
      final AABB bb = new AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9);
      final AABB step1 = Auxiliaries.getYRotatedAABB(bb, 1);
      assertAABB(step1, Auxiliaries.getYRotatedAABB(bb, 5));
      assertAABB(step1, Auxiliaries.getYRotatedAABB(bb, -3));
    }

    @Test
    void getYRotatedAABBAllFourQuartersProduceValidBoxes()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9);
      for(int q = 0; q < 4; q++) {
        final AABB out = Auxiliaries.getYRotatedAABB(bb, q);
        assertNotNull(out);
        assertEquals(bb.minY, out.minY, EPS);
        assertEquals(bb.maxY, out.maxY, EPS);
      }
    }

    @Test
    void getYRotatedAABBArrayPreservesLength()
    {
      final AABB[] in = { new AABB(0,0,0,1,1,1) };
      assertEquals(in.length, Auxiliaries.getYRotatedAABB(in, 2).length);
    }

    @Test
    void getYRotatedAABBArrayWithEmptyInput()
    {
      assertEquals(0, Auxiliaries.getYRotatedAABB(new AABB[0], 1).length);
    }
  }

  @Nested
  class MirroredAABB
  {
    @Test
    void mirroredAABBXAxisSwapsXCoordinates()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
      assertAABB(new AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8), Auxiliaries.getMirroredAABB(bb, Direction.Axis.X));
    }

    @Test
    void mirroredAABBYAxisSwapsYCoordinates()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
      assertAABB(new AABB(0.1, 0.3, 0.3, 0.6, 0.8, 0.8), Auxiliaries.getMirroredAABB(bb, Direction.Axis.Y));
    }

    @Test
    void mirroredAABBZAxisSwapsZCoordinates()
    {
      final AABB bb = new AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8);
      assertAABB(new AABB(0.1, 0.2, 0.2, 0.6, 0.7, 0.7), Auxiliaries.getMirroredAABB(bb, Direction.Axis.Z));
    }

    @Test
    void getMirroredAABBArrayPreservesLength()
    {
      final AABB[] in = { new AABB(0,0,0,1,1,1), new AABB(0.2,0.2,0.2,0.8,0.8,0.8) };
      assertEquals(in.length, Auxiliaries.getMirroredAABB(in, Direction.Axis.X).length);
    }

    @Test
    void getMirroredAABBArrayWithEmptyInput()
    {
      assertEquals(0, Auxiliaries.getMirroredAABB(new AABB[0], Direction.Axis.X).length);
    }
  }

  @Nested
  class MappedAABB
  {
    @Test
    void getMappedAABBAppliesMapper()
    {
      final AABB[] in = { new AABB(0,0,0,1,1,1) };
      assertAABB(new AABB(2,0,0,3,1,1), Auxiliaries.getMappedAABB(in, b -> b.move(2,0,0))[0]);
    }

    @Test
    void getMappedAABBHandlesEmptyInput()
    {
      assertEquals(0, Auxiliaries.getMappedAABB(new AABB[0], b -> b).length);
    }
  }

  @Nested
  class UnionShape
  {
    @Test
    void getUnionShapeOfSingleAABB()
    {
      assertFalse(Auxiliaries.getUnionShape(new AABB(0,0,0,1,1,1)).isEmpty());
    }

    @Test
    void getUnionShapeOfArrayOfAABB()
    {
      final VoxelShape s = Auxiliaries.getUnionShape(
        new AABB[]{ new AABB(0,0,0,0.5,1,1) },
        new AABB[]{ new AABB(0.5,0,0,1,1,1) }
      );
      assertFalse(s.isEmpty());
    }

    @Test
    void getUnionShapeArrayOfArraysHandlesEmptyInput()
    {
      assertTrue(Auxiliaries.getUnionShape(new AABB[0][0]).isEmpty());
    }
  }
}
