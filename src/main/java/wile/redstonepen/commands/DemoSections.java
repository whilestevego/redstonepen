/*
 * @file DemoSections.java
 * @license MIT
 *
 * Implementations of each /redstonepen demo section.
 */
package wile.redstonepen.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.blocks.ControlBox;
import wile.redstonepen.libmc.Registries;

import java.util.ArrayList;
import java.util.List;

public final class DemoSections
{
  private DemoSections() {}

  // Block names that are DirectedComponentBlock instances.
  public static final List<String> DIRECTED_BLOCKS = List.of(
    "relay", "inverted_relay", "pulse_relay", "bistable_relay", "bridge_relay",
    "control_box", "basic_lever", "basic_button", "basic_pulse_button"
  );

  public static final int CELL_SPACING = 3;        // 3 blocks between adjacent grid cells
  public static final int BLOCK_GROUP_SPACING = 4; // gap between block sections (z axis)
  public static final int GRID_COLUMNS = 4;        // ROTATION goes across columns
  public static final int GRID_ROWS = 6;           // FACING goes down rows

  /**
   * Returns the visual states of a directional block: every FACING × ROTATION combination,
   * with POWERED=false and WATERLOGGED=false. 24 states total.
   * Sorted by FACING.get3DDataValue() * 4 + ROTATION for stable layout.
   */
  public static List<BlockState> directionalStates(Block block)
  {
    final List<BlockState> states = new ArrayList<>();
    for(BlockState s : block.getStateDefinition().getPossibleStates()) {
      if(!s.hasProperty(BlockStateProperties.FACING)) continue;
      if(!s.hasProperty(CircuitComponents.DirectedComponentBlock.ROTATION)) continue;
      if(s.hasProperty(BlockStateProperties.POWERED) && s.getValue(BlockStateProperties.POWERED)) continue;
      if(s.hasProperty(BlockStateProperties.WATERLOGGED) && s.getValue(BlockStateProperties.WATERLOGGED)) continue;
      if(s.hasProperty(CircuitComponents.DirectedComponentBlock.STATE)
        && s.getValue(CircuitComponents.DirectedComponentBlock.STATE) != 0) continue;
      states.add(s);
    }
    states.sort((a, b) -> {
      int af = a.getValue(BlockStateProperties.FACING).get3DDataValue() * 4
        + a.getValue(CircuitComponents.DirectedComponentBlock.ROTATION);
      int bf = b.getValue(BlockStateProperties.FACING).get3DDataValue() * 4
        + b.getValue(CircuitComponents.DirectedComponentBlock.ROTATION);
      return Integer.compare(af, bf);
    });
    return states;
  }

  /**
   * Builds the full block gallery starting at {@code origin}. Each directional block gets
   * a 6×4 grid (FACING × ROTATION). Returns the {@code z}-extent consumed so callers can
   * lay out subsequent sections beyond it.
   */
  public static int runGallery(Level level, BlockPos origin)
  {
    int zCursor = 0;
    for(String name : DIRECTED_BLOCKS) {
      final Block block = Registries.getBlock(name);
      if(block == null || block == Blocks.AIR) continue;
      final List<BlockState> states = directionalStates(block);
      final BlockPos sectionOrigin = origin.offset(0, 0, zCursor);
      DemoBuilder.placeStandingSign(level, sectionOrigin.offset(-2, 1, 0), 0, name);
      for(int i = 0; i < states.size(); ++i) {
        final BlockPos cell = DemoBuilder.cellOrigin(sectionOrigin, i, GRID_COLUMNS, CELL_SPACING);
        DemoBuilder.placeAttached(level, cell, states.get(i));
      }
      zCursor += GRID_ROWS * CELL_SPACING + BLOCK_GROUP_SPACING;
    }
    return zCursor;
  }

  /**
   * Places a control_box pre-loaded with a demo program. The program reads inputs from
   * adjacent levers/buttons (down, up, right, yellow=south, green=west, blue=east) and
   * produces deterministic outputs on the same ports.
   */
  public static void runControlBox(Level level, BlockPos origin)
  {
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null || controlBox == Blocks.AIR) return;
    final BlockState state = controlBox.defaultBlockState()
      .setValue(BlockStateProperties.FACING, Direction.DOWN)
      .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0);
    DemoBuilder.placeAttached(level, origin, state);
    if(level.getBlockEntity(origin) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(CONTROL_BOX_DEMO_PROGRAM);
      cbe.setChanged();
    }
    DemoBuilder.placeStandingSign(level, origin.offset(-2, 1, 0), 0, "ControlBox", "demo program");
  }

  /**
   * Lays a small redstone-track wire pattern: a "+" of redstone tracks on a stone platform,
   * with a redstone block at one end providing power.
   */
  public static void runTrack(Level level, BlockPos origin)
  {
    // 5×5 stone platform under the cross
    for(int dx = -2; dx <= 2; ++dx) {
      for(int dz = -2; dz <= 2; ++dz) {
        level.setBlock(origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), DemoBuilder.FLAGS);
      }
    }
    final Block track = Registries.getBlock("track");
    if(track == null || track == Blocks.AIR) return;
    // Place track segments: vanilla redstone wire is its own placement; the mod uses a
    // pen-driven track. For demo, place vanilla redstone wire so the platform is visible
    // and pen-applied tracks can be added by the user.
    for(int d = -2; d <= 2; ++d) {
      level.setBlock(origin.offset(d, 0, 0), Blocks.REDSTONE_WIRE.defaultBlockState(), DemoBuilder.FLAGS);
      level.setBlock(origin.offset(0, 0, d), Blocks.REDSTONE_WIRE.defaultBlockState(), DemoBuilder.FLAGS);
    }
    level.setBlock(origin.offset(3, 0, 0), Blocks.REDSTONE_BLOCK.defaultBlockState(), DemoBuilder.FLAGS);
    DemoBuilder.placeStandingSign(level, origin.offset(-2, 1, -3), 0, "Track demo", "stone + wire");
  }

  /**
   * Runs every section in sequence, laid out along the z-axis from {@code origin}.
   */
  public static void runAll(Level level, BlockPos origin)
  {
    int z = 0;
    z += runGallery(level, origin.offset(0, 0, z));
    z += BLOCK_GROUP_SPACING;
    runControlBox(level, origin.offset(0, 0, z));
    z += BLOCK_GROUP_SPACING + 4;
    runTrack(level, origin.offset(0, 0, z));
  }

  // Demo program for the control box. Each output port mirrors the corresponding input
  // through a different transformation, making the runtime behavior visible at a glance.
  private static final String CONTROL_BOX_DEMO_PROGRAM = String.join("\n",
    "# 6-input demo: each output reflects its input differently",
    "b = max(d, u)",
    "g = inv(d)",
    "y = if(d, 15, 0)",
    "r = lim(d + u, 0, 15)"
  );
}
