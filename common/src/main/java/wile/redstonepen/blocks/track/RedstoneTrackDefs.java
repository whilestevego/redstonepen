/*
 * @file RedstoneTrackDefs.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen.blocks.track;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import wile.redstonepen.util.Auxiliaries;

import java.util.*;

public final class RedstoneTrackDefs
{
  public static final long STATE_FLAG_WIR_MASK  = 0x0000000000ffffffL;
  public static final long STATE_FLAG_CON_MASK  = 0x000000003f000000L;
  public static final long STATE_FLAG_PWR_MASK  = 0x00ffffff00000000L;
  public static final int  STATE_FLAG_WIR_COUNT = 24;
  public static final int  STATE_FLAG_CON_COUNT = 6;
  public static final int  STATE_FLAG_WIR_POS   = 0;
  public static final int  STATE_FLAG_CON_POS   = 24;
  public static final int  STATE_FLAG_PWR_POS   = 32;

  public static final Direction[] REDSTONE_UPDATE_DIRECTIONS = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

  public static final class connections
  {
    public static final Direction[] CONNECTION_BIT_ORDER  = {
      Direction.DOWN,Direction.UP, Direction.NORTH,Direction.SOUTH, Direction.EAST,Direction.WEST
    };

    // don't want extended enum for that small thing.
    public static final ImmutableMap<Direction,Integer> CONNECTION_BIT_ORDER_REV = new ImmutableMap.Builder<Direction,Integer>()
      .put(Direction.DOWN, 0)
      .put(Direction.UP, 1)
      .put(Direction.NORTH, 2)
      .put(Direction.SOUTH, 3)
      .put(Direction.EAST, 4)
      .put(Direction.WEST, 5)
      .build();

    public static final ImmutableMap<Long,Direction> BULK_FACE_MAPPING = new ImmutableMap.Builder<Long,Direction>()
      .put(0x0000000000000000L, Direction.DOWN)
      .put(0x0000000001000000L, Direction.DOWN)
      .put(0x0000000002000000L, Direction.UP)
      .put(0x0000000004000000L, Direction.NORTH)
      .put(0x0000000008000000L, Direction.SOUTH)
      .put(0x0000000010000000L, Direction.EAST)
      .put(0x0000000020000000L, Direction.WEST)
      .build();

    public static final ImmutableMap<Direction,Long> BULK_FACE_MAPPING_REV = new ImmutableMap.Builder<Direction,Long>()
      .put(Direction.DOWN,  0x0000000001000000L)
      .put(Direction.UP,    0x0000000002000000L)
      .put(Direction.NORTH, 0x0000000004000000L)
      .put(Direction.SOUTH, 0x0000000008000000L)
      .put(Direction.EAST,  0x0000000010000000L)
      .put(Direction.WEST,  0x0000000020000000L)
      .build();

    public static final ImmutableMap<Long, Tuple<Direction,Direction>> WIRE_FACE_DIRECTION_MAPPING = new ImmutableMap.Builder<Long,Tuple<Direction,Direction>>()
      .put(0x00000000L, new Tuple<>(Direction.DOWN,Direction.DOWN))
      .put(0x00000001L, new Tuple<>(Direction.DOWN,Direction.NORTH))
      .put(0x00000002L, new Tuple<>(Direction.DOWN,Direction.SOUTH))
      .put(0x00000004L, new Tuple<>(Direction.DOWN,Direction.EAST))
      .put(0x00000008L, new Tuple<>(Direction.DOWN,Direction.WEST))
      .put(0x00000010L, new Tuple<>(Direction.UP,Direction.NORTH))
      .put(0x00000020L, new Tuple<>(Direction.UP,Direction.SOUTH))
      .put(0x00000040L, new Tuple<>(Direction.UP,Direction.EAST))
      .put(0x00000080L, new Tuple<>(Direction.UP,Direction.WEST))
      .put(0x00000100L, new Tuple<>(Direction.NORTH,Direction.UP))
      .put(0x00000200L, new Tuple<>(Direction.NORTH,Direction.DOWN))
      .put(0x00000400L, new Tuple<>(Direction.NORTH,Direction.EAST))
      .put(0x00000800L, new Tuple<>(Direction.NORTH,Direction.WEST))
      .put(0x00001000L, new Tuple<>(Direction.SOUTH,Direction.UP))
      .put(0x00002000L, new Tuple<>(Direction.SOUTH,Direction.DOWN))
      .put(0x00004000L, new Tuple<>(Direction.SOUTH,Direction.EAST))
      .put(0x00008000L, new Tuple<>(Direction.SOUTH,Direction.WEST))
      .put(0x00010000L, new Tuple<>(Direction.EAST,Direction.UP))
      .put(0x00020000L, new Tuple<>(Direction.EAST,Direction.DOWN))
      .put(0x00040000L, new Tuple<>(Direction.EAST,Direction.NORTH))
      .put(0x00080000L, new Tuple<>(Direction.EAST,Direction.SOUTH))
      .put(0x00100000L, new Tuple<>(Direction.WEST,Direction.UP))
      .put(0x00200000L, new Tuple<>(Direction.WEST,Direction.DOWN))
      .put(0x00400000L, new Tuple<>(Direction.WEST,Direction.NORTH))
      .put(0x00800000L, new Tuple<>(Direction.WEST,Direction.SOUTH))
      .build();

    public static final ImmutableMap<Long,Tuple<Direction,Direction>> INTERNAL_EDGE_CONNECTION_MAPPING = new ImmutableMap.Builder<Long,Tuple<Direction,Direction>>()
      .put(0x00000001L|0x00000200L, new Tuple<>(Direction.DOWN,Direction.NORTH))
      .put(0x00000002L|0x00002000L, new Tuple<>(Direction.DOWN,Direction.SOUTH))
      .put(0x00000004L|0x00020000L, new Tuple<>(Direction.DOWN,Direction.EAST))
      .put(0x00000008L|0x00200000L, new Tuple<>(Direction.DOWN,Direction.WEST))
      .put(0x00000010L|0x00000100L, new Tuple<>(Direction.UP,Direction.NORTH))
      .put(0x00000020L|0x00001000L, new Tuple<>(Direction.UP,Direction.SOUTH))
      .put(0x00000040L|0x00010000L, new Tuple<>(Direction.UP,Direction.EAST))
      .put(0x00000080L|0x00100000L, new Tuple<>(Direction.UP,Direction.WEST))
      .put(0x00000400L|0x00040000L, new Tuple<>(Direction.NORTH,Direction.EAST))
      .put(0x00000800L|0x00400000L, new Tuple<>(Direction.NORTH,Direction.WEST))
      .put(0x00004000L|0x00080000L, new Tuple<>(Direction.SOUTH,Direction.EAST))
      .put(0x00008000L|0x00800000L, new Tuple<>(Direction.SOUTH,Direction.WEST))
      .build();

    // -- bit mapping access -------------------------------------------------------------------------------------

    /**
     * Returns the state bit for a connector on a specific face.
     */
    public static long getBulkConnectorBit(Direction face)
    { return connections.BULK_FACE_MAPPING_REV.get(face); }

    /**
     * Returns the state bit for a wire (with direction `wire_direction`)
     * on a specific face.
     */
    public static long getWireBit(Direction face, Direction wire_direction)
    {
      return connections.WIRE_FACE_DIRECTION_MAPPING.entrySet().stream()
        .filter(kv->kv.getValue().getA()==face && kv.getValue().getB()==wire_direction)
        .findFirst()
        .map(Map.Entry::getKey).orElse(0L);
    }

    public static Tuple<Direction,Direction> getWireBitSideAndDirection(long wirebit)
    { return WIRE_FACE_DIRECTION_MAPPING.getOrDefault(wirebit, new Tuple<>(Direction.DOWN,Direction.DOWN)); }

    public static List<Direction> getVanillaWireConnectionDirections(long mask)
    {
      if((mask & 0x0000000fL)==0) return Collections.emptyList();
      final List<Direction> r = new ArrayList<>(4);
      if((mask & 0x00000001L) != 0) r.add(Direction.NORTH);
      if((mask & 0x00000002L) != 0) r.add(Direction.SOUTH);
      if((mask & 0x00000004L) != 0) r.add(Direction.EAST);
      if((mask & 0x00000008L) != 0) r.add(Direction.WEST);
      return r;
    }

    public static boolean hasVanillaWireConnection(long mask, Direction side)
    {
      return switch (side) {
        case NORTH -> ((mask & 0x00000001L) != 0);
        case SOUTH -> ((mask & 0x00000002L) != 0);
        case EAST -> ((mask & 0x00000004L) != 0);
        case WEST -> ((mask & 0x00000008L) != 0);
        default -> false;
      };
    }

    public static boolean hasBulkConnection(long mask, Direction side)
    { return ((connections.BULK_FACE_MAPPING_REV.get(side) & mask) != 0); }

    public static boolean hasRedstoneConnection(long mask, Direction side)
    {
      return switch (side) {
        case DOWN -> ((mask & 0x01222200L) != 0);
        case UP -> ((mask & 0x02111100L) != 0);
        case NORTH -> ((mask & 0x04440011L) != 0);
        case SOUTH -> ((mask & 0x08880022L) != 0);
        case EAST -> ((mask & 0x10004444L) != 0);
        case WEST -> ((mask & 0x20008888L) != 0);
      };
    }

    public static long getWireElementsOnFace(Direction face)
    { return (0xfL<<((connections.CONNECTION_BIT_ORDER_REV.get(face)*4)+STATE_FLAG_WIR_POS)); }

    public static long getAllElementsOnFace(Direction face)
    {
      final int index = connections.CONNECTION_BIT_ORDER_REV.get(face);
      return (0xfL<<((index*4)+STATE_FLAG_WIR_POS))|(0x1L<<(index+STATE_FLAG_CON_POS));
    }

  }

  public static class shape
  {
    private static final double SHAPE_LAYER_THICKNESS = 0.01;
    private static final double SHAPE_TRACK_HALFWIDTH = 2;

    private static final VoxelShape DOWN_SHAPE = Auxiliaries.getUnionShape(
      Auxiliaries.getPixeledAABB(8-SHAPE_TRACK_HALFWIDTH,0,0, 8+SHAPE_TRACK_HALFWIDTH,SHAPE_LAYER_THICKNESS,16),
      Auxiliaries.getPixeledAABB(0,0,8-SHAPE_TRACK_HALFWIDTH,16,SHAPE_LAYER_THICKNESS, 8+SHAPE_TRACK_HALFWIDTH)
    );
    private static final VoxelShape UP_SHAPE = Auxiliaries.getUnionShape(
      Auxiliaries.getPixeledAABB(8-SHAPE_TRACK_HALFWIDTH,16-SHAPE_LAYER_THICKNESS,0, 8+SHAPE_TRACK_HALFWIDTH,16,16),
      Auxiliaries.getPixeledAABB(0,16-SHAPE_LAYER_THICKNESS,8-SHAPE_TRACK_HALFWIDTH,16,16, 8+SHAPE_TRACK_HALFWIDTH)
    );
    private static final VoxelShape WEST_SHAPE = Auxiliaries.getUnionShape(
      Auxiliaries.getPixeledAABB(0,0,8-SHAPE_TRACK_HALFWIDTH, SHAPE_LAYER_THICKNESS,16, 8+SHAPE_TRACK_HALFWIDTH),
      Auxiliaries.getPixeledAABB(0,8-SHAPE_TRACK_HALFWIDTH,0, SHAPE_LAYER_THICKNESS, 8+SHAPE_TRACK_HALFWIDTH,16)
    );
    private static final VoxelShape EAST_SHAPE = Auxiliaries.getUnionShape(
      Auxiliaries.getPixeledAABB(16-SHAPE_LAYER_THICKNESS,0,8-SHAPE_TRACK_HALFWIDTH, 16,16, 8+SHAPE_TRACK_HALFWIDTH),
      Auxiliaries.getPixeledAABB(16-SHAPE_LAYER_THICKNESS,8-SHAPE_TRACK_HALFWIDTH,0, 16, 8+SHAPE_TRACK_HALFWIDTH,16)
    );

    private static final VoxelShape NORTH_SHAPE = Auxiliaries.getUnionShape(
      Auxiliaries.getPixeledAABB(0,8-SHAPE_TRACK_HALFWIDTH,0, 16, 8+SHAPE_TRACK_HALFWIDTH,SHAPE_LAYER_THICKNESS),
      Auxiliaries.getPixeledAABB(8-SHAPE_TRACK_HALFWIDTH,0,0,  8+SHAPE_TRACK_HALFWIDTH,16,SHAPE_LAYER_THICKNESS)
    );
    private static final VoxelShape SOUTH_SHAPE = Auxiliaries.getUnionShape(
      Auxiliaries.getPixeledAABB(0,8-SHAPE_TRACK_HALFWIDTH,16-SHAPE_LAYER_THICKNESS, 16, 8+SHAPE_TRACK_HALFWIDTH,16),
      Auxiliaries.getPixeledAABB(8-SHAPE_TRACK_HALFWIDTH,0,16-SHAPE_LAYER_THICKNESS,  8+SHAPE_TRACK_HALFWIDTH,16,16)
    );

    // maps are too slow, 64 objects are ok to pre-allocate.
    // check if thread sync needed.
    private static final VoxelShape[] shape_cache = new VoxelShape[64];

    public static VoxelShape get(int faces)
    {
      if(shape_cache[faces] == null) {
        VoxelShape shape = Shapes.empty();
        if((faces & 0x01)!=0) shape = Shapes.join(shape, DOWN_SHAPE, BooleanOp.OR);
        if((faces & 0x02)!=0) shape = Shapes.join(shape, UP_SHAPE, BooleanOp.OR);
        if((faces & 0x04)!=0) shape = Shapes.join(shape, NORTH_SHAPE, BooleanOp.OR);
        if((faces & 0x08)!=0) shape = Shapes.join(shape, SOUTH_SHAPE, BooleanOp.OR);
        if((faces & 0x10)!=0) shape = Shapes.join(shape, EAST_SHAPE, BooleanOp.OR);
        if((faces & 0x20)!=0) shape = Shapes.join(shape, WEST_SHAPE, BooleanOp.OR);
        shape_cache[faces] = shape;
      }
      return shape_cache[faces];
    }
  }

  public static final class models
  {
    public static final ImmutableMap<Long,String> STATE_WIRE_MAPPING = new ImmutableMap.Builder<Long,String>()
      .put(0x00000000L, "none")
      .put(0x00000001L, "dn")
      .put(0x00000002L, "ds")
      .put(0x00000004L, "de")
      .put(0x00000008L, "dw")
      .put(0x00000010L, "un")
      .put(0x00000020L, "us")
      .put(0x00000040L, "ue")
      .put(0x00000080L, "uw")
      .put(0x00000100L, "nu")
      .put(0x00000200L, "nd")
      .put(0x00000400L, "ne")
      .put(0x00000800L, "nw")
      .put(0x00001000L, "su")
      .put(0x00002000L, "sd")
      .put(0x00004000L, "se")
      .put(0x00008000L, "sw")
      .put(0x00010000L, "eu")
      .put(0x00020000L, "ed")
      .put(0x00040000L, "en")
      .put(0x00080000L, "es")
      .put(0x00100000L, "wu")
      .put(0x00200000L, "wd")
      .put(0x00400000L, "wn")
      .put(0x00800000L, "ws")
      .build();

    public static final ImmutableMap<Long,String> STATE_CONNECT_MAPPING = new ImmutableMap.Builder<Long,String>()
      .put(0x0000000000000000L, "none")
      .put(0x0000000001000000L, "dc")
      .put(0x0000000002000000L, "uc")
      .put(0x0000000004000000L, "nc")
      .put(0x0000000008000000L, "sc")
      .put(0x0000000010000000L, "ec")
      .put(0x0000000020000000L, "wc")
      .build();

    public static final ImmutableMap<Long,String> STATE_CNTWIRE_MAPPING = new ImmutableMap.Builder<Long,String>()
      .put(0x0000000000000000L, "none")
      .put(0x0000000001000000L, "dm")
      .put(0x0000000002000000L, "um")
      .put(0x0000000004000000L, "nm")
      .put(0x0000000008000000L, "sm")
      .put(0x0000000010000000L, "em")
      .put(0x0000000020000000L, "wm")
      .build();
  }
}
