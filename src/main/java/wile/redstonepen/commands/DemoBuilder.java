/*
 * @file DemoBuilder.java
 * @license MIT
 *
 * Stateless placement helpers for the /redstonepen demo command.
 */
package wile.redstonepen.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class DemoBuilder
{
  private DemoBuilder() {}

  public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

  public static BlockPos cellOrigin(BlockPos gridOrigin, int cellIndex, int columns, int spacing)
  {
    if(columns <= 0) throw new IllegalArgumentException("columns must be > 0");
    if(spacing <= 0) throw new IllegalArgumentException("spacing must be > 0");
    if(cellIndex < 0) throw new IllegalArgumentException("cellIndex must be >= 0");
    final int col = cellIndex % columns;
    final int row = cellIndex / columns;
    return gridOrigin.offset(col * spacing, 0, row * spacing);
  }

  public static void placeAttached(Level level, BlockPos pos, BlockState state)
  {
    final Direction mount = state.hasProperty(BlockStateProperties.FACING)
      ? state.getValue(BlockStateProperties.FACING)
      : Direction.DOWN;
    final BlockPos supportPos = pos.relative(mount);
    level.setBlock(supportPos, Blocks.STONE.defaultBlockState(), FLAGS);
    level.setBlock(pos, state, FLAGS);
  }

  public static void clearRegion(Level level, BlockPos min, BlockPos max)
  {
    final int x0 = Math.min(min.getX(), max.getX()), x1 = Math.max(min.getX(), max.getX());
    final int y0 = Math.min(min.getY(), max.getY()), y1 = Math.max(min.getY(), max.getY());
    final int z0 = Math.min(min.getZ(), max.getZ()), z1 = Math.max(min.getZ(), max.getZ());
    final BlockState air = Blocks.AIR.defaultBlockState();
    final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
    for(int x = x0; x <= x1; ++x) {
      for(int y = y0; y <= y1; ++y) {
        for(int z = z0; z <= z1; ++z) {
          m.set(x, y, z);
          level.setBlock(m, air, FLAGS);
        }
      }
    }
  }

  public static int regionVolume(BlockPos min, BlockPos max)
  {
    final int dx = Math.abs(max.getX() - min.getX()) + 1;
    final int dy = Math.abs(max.getY() - min.getY()) + 1;
    final int dz = Math.abs(max.getZ() - min.getZ()) + 1;
    return dx * dy * dz;
  }

  public static void placeWallSign(Level level, BlockPos pos, Direction facing, String... lines)
  {
    final BlockState sign = Blocks.OAK_WALL_SIGN.defaultBlockState()
      .setValue(WallSignBlock.FACING, facing);
    level.setBlock(pos.relative(facing.getOpposite()), Blocks.STONE.defaultBlockState(), FLAGS);
    level.setBlock(pos, sign, FLAGS);
    if(level.getBlockEntity(pos) instanceof SignBlockEntity be) {
      final Component[] msgs = new Component[4];
      for(int i = 0; i < 4; ++i) {
        msgs[i] = Component.literal(i < lines.length && lines[i] != null ? truncate(lines[i]) : "");
      }
      be.updateText(text -> text
        .setMessage(0, msgs[0])
        .setMessage(1, msgs[1])
        .setMessage(2, msgs[2])
        .setMessage(3, msgs[3]),
        true);
      be.setChanged();
    }
  }

  public static void placeStandingSign(Level level, BlockPos pos, int rotation, String... lines)
  {
    final BlockState sign = Blocks.OAK_SIGN.defaultBlockState()
      .setValue(BlockStateProperties.ROTATION_16, rotation & 0xf);
    level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), FLAGS);
    level.setBlock(pos, sign, FLAGS);
    if(level.getBlockEntity(pos) instanceof SignBlockEntity be) {
      final Component[] msgs = new Component[4];
      for(int i = 0; i < 4; ++i) {
        msgs[i] = Component.literal(i < lines.length && lines[i] != null ? truncate(lines[i]) : "");
      }
      be.updateText(text -> text
        .setMessage(0, msgs[0])
        .setMessage(1, msgs[1])
        .setMessage(2, msgs[2])
        .setMessage(3, msgs[3]),
        true);
      be.setChanged();
    }
  }

  private static String truncate(String s)
  {
    return s.length() <= 15 ? s : s.substring(0, 15);
  }
}
