/*
 * @file DemoSections.java
 * @license MIT
 *
 * Working redstone contraptions that exercise mod blocks together with vanilla
 * redstone components. Each contraption is a small, labeled circuit that can be
 * triggered by the player and observed in-world.
 */
package wile.redstonepen.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.blocks.ControlBox;
import wile.redstonepen.libmc.Registries;

public final class DemoSections
{
  private DemoSections() {}

  private static final int FLAGS = DemoBuilder.FLAGS;
  private static final int CELL_SIZE = 9;     // each contraption gets a 9x9 footprint
  private static final int GRID_COLUMNS = 3;

  /**
   * Builds the showcase: 9 working redstone contraptions in a 3x3 grid, each
   * exercising a different mod block in interaction with vanilla redstone.
   */
  public static void runCircuits(Level level, BlockPos origin)
  {
    final Contraption[] contraptions = {
      DemoSections::buildLeverDrivesLamp,
      DemoSections::buildButtonDrivesPiston,
      DemoSections::buildInvertedRelayNotGate,
      DemoSections::buildBistableToggle,
      DemoSections::buildPulseRelayMonostable,
      DemoSections::buildRelayBuffer,
      DemoSections::buildBridgeRelayCrossover,
      DemoSections::buildControlBoxAndGate,
      DemoSections::buildGaugeReadout
    };
    for(int i = 0; i < contraptions.length; ++i) {
      final BlockPos cell = DemoBuilder.cellOrigin(origin, i, GRID_COLUMNS, CELL_SIZE);
      contraptions[i].build(level, cell);
    }
  }

  public static void runAll(Level level, BlockPos origin)
  {
    runCircuits(level, origin);
  }

  // -----------------------------------------------------------------------------------------------
  // Contraptions — each occupies a CELL_SIZE × CELL_SIZE footprint with `cell` as the SW corner.
  // Convention: stone platform at y-1, components at y0, signs at y+1. North-facing signs label
  // the contraption from the south edge so the player reads them when approaching.
  // -----------------------------------------------------------------------------------------------

  @FunctionalInterface
  private interface Contraption { void build(Level level, BlockPos cell); }

  /** basic_lever drives a vanilla redstone lamp via redstone wire. */
  public static void buildLeverDrivesLamp(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block lever = Registries.getBlock("basic_lever");
    if(lever == null) return;
    // basic_lever extends vanilla LeverBlock: ATTACH_FACE + HORIZONTAL_FACING + POWERED.
    level.setBlock(cell.offset(2, 0, 4),
      lever.defaultBlockState()
        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), FLAGS);
    // Wire NORTH to lamp
    for(int dz = 3; dz >= 1; --dz) {
      level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    }
    level.setBlock(cell.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "basic_lever", "drives lamp");
  }

  /** basic_button pulses a sticky piston that pushes a glowstone block. */
  public static void buildButtonDrivesPiston(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block button = Registries.getBlock("basic_button");
    if(button == null) return;
    // basic_button extends vanilla ButtonBlock: same property set as a lever.
    level.setBlock(cell.offset(2, 0, 4),
      button.defaultBlockState()
        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), FLAGS);
    // Wire NORTH to piston
    for(int dz = 3; dz >= 2; --dz) {
      level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    }
    // Sticky piston facing NORTH at (2,0,1), pushable glowstone in front of it
    level.setBlock(cell.offset(2, 0, 1),
      Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(2, 0, 0), Blocks.GLOWSTONE.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "basic_button", "pulses piston");
  }

  /** Inverted relay: vanilla lever ON gives lamp OFF, lever OFF gives lamp ON. */
  public static void buildInvertedRelayNotGate(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block relay = Registries.getBlock("inverted_relay");
    if(relay == null) return;
    // Vanilla lever as input on the south end
    level.setBlock(cell.offset(2, 0, 5),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.NORTH), FLAGS);
    // Wire from lever NORTH into relay
    level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // inverted_relay at (2,0,3): FACING=DOWN ROTATION=0 → input from south, output NORTH
    DemoBuilder.placeAttached(level, cell.offset(2, 0, 3),
      relay.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    // Wire from relay NORTH to lamp
    for(int dz = 2; dz >= 1; --dz) {
      level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    }
    level.setBlock(cell.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "inverted_relay", "NOT gate");
  }

  /** bistable_relay toggles state on each rising edge from a vanilla button. */
  public static void buildBistableToggle(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block relay = Registries.getBlock("bistable_relay");
    if(relay == null) return;
    level.setBlock(cell.offset(2, 0, 5),
      Blocks.STONE_BUTTON.defaultBlockState()
        .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
        .setValue(ButtonBlock.FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    DemoBuilder.placeAttached(level, cell.offset(2, 0, 3),
      relay.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    for(int dz = 2; dz >= 1; --dz) {
      level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    }
    level.setBlock(cell.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "bistable_relay", "T flip-flop");
  }

  /** pulse_relay turns a held lever into a single brief lamp flash on rising edge. */
  public static void buildPulseRelayMonostable(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block relay = Registries.getBlock("pulse_relay");
    if(relay == null) return;
    level.setBlock(cell.offset(2, 0, 5),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    DemoBuilder.placeAttached(level, cell.offset(2, 0, 3),
      relay.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    for(int dz = 2; dz >= 1; --dz) {
      level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    }
    level.setBlock(cell.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "pulse_relay", "edge pulse");
  }

  /** Plain relay: passes signal from input to output (buffer / signal direction). */
  public static void buildRelayBuffer(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block relay = Registries.getBlock("relay");
    if(relay == null) return;
    level.setBlock(cell.offset(2, 0, 5),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    DemoBuilder.placeAttached(level, cell.offset(2, 0, 3),
      relay.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    for(int dz = 2; dz >= 1; --dz) {
      level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    }
    level.setBlock(cell.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "relay", "buffer / direction");
  }

  /** Bridge relay: two perpendicular signal lines crossing at a single block. */
  public static void buildBridgeRelayCrossover(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block relay = Registries.getBlock("bridge_relay");
    if(relay == null) return;
    // North-south signal: lever at south, lamp at north
    level.setBlock(cell.offset(4, 0, 5),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(4, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(4, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(4, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // East-west cross-axis: lever placed directly adjacent to the bridge's WEST face
    // so the bridge sees a true signal source (lever) on its cross-axis input. A wire
    // chain on the input side would not work reliably because vanilla wire's
    // `shouldSignal` flag suppresses inter-wire signal queries during a wire's own
    // power-strength update — and the bridge's cross-axis read recurses through that
    // path. With the lever directly adjacent, the bridge reads `lever.getSignal=15`
    // through the non-wire branch of `getInputPower` and propagates cleanly.
    level.setBlock(cell.offset(3, 0, 3),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.EAST), FLAGS);
    level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // Bridge relay sits at the intersection
    DemoBuilder.placeAttached(level, cell.offset(4, 0, 3),
      relay.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    sign(level, cell.offset(7, 1, 6), "bridge_relay", "crossover");
  }

  /** ControlBox AND gate: two vanilla levers feeding ports d and u, output b drives lamp. */
  public static void buildControlBoxAndGate(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null) return;
    // Control box mounted on the platform (FACING=DOWN ROTATION=0). Port mapping:
    // d=DOWN, u=UP, r=NORTH, y=SOUTH, g=WEST, b=EAST.
    final BlockPos cbPos = cell.offset(3, 0, 3);
    DemoBuilder.placeAttached(level, cbPos,
      controlBox.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    if(level.getBlockEntity(cbPos) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(CONTROL_BOX_AND_PROGRAM);
      cbe.setEnabled(true); // Without this, the program never executes.
      cbe.setChanged();
    }
    // South-input lever feeds port y via a wire run.
    level.setBlock(cell.offset(3, 0, 5),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // West-input lever feeds port g via a wire run.
    level.setBlock(cell.offset(1, 0, 3),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.EAST), FLAGS);
    level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Output port b drives the east-side wire and lamp.
    level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(7, 1, 6), "control_box", "AND program");
  }

  /** Vanilla lever drives a basic_gauge through a wire — gauge displays the signal level. */
  public static void buildGaugeReadout(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block gauge = Registries.getBlock("basic_gauge");
    if(gauge == null) return;
    level.setBlock(cell.offset(2, 0, 5),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.NORTH), FLAGS);
    // Wire NORTH; insert a repeater so the gauge sees a clean 15-level signal
    level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(2, 0, 3),
      Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.SOUTH), FLAGS);
    level.setBlock(cell.offset(2, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Gauge has no facing — just place on top of stone at (2,0,1).
    level.setBlock(cell.offset(2, 0, 1), gauge.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "basic_gauge", "signal readout");
  }

  // -----------------------------------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------------------------------

  private static void platform(Level level, BlockPos cell)
  {
    final BlockState stone = Blocks.STONE.defaultBlockState();
    final BlockState air = Blocks.AIR.defaultBlockState();
    for(int dx = 0; dx < CELL_SIZE; ++dx) {
      for(int dz = 0; dz < CELL_SIZE; ++dz) {
        level.setBlock(cell.offset(dx, -1, dz), stone, FLAGS);
        for(int dy = 0; dy < 4; ++dy) {
          level.setBlock(cell.offset(dx, dy, dz), air, FLAGS);
        }
      }
    }
  }

  private static void sign(Level level, BlockPos pos, String line1, String line2)
  {
    DemoBuilder.placeStandingSign(level, pos, 8, line1, line2);
  }

  // ControlBox AND program. Port mapping: d=DOWN, u=UP, r=NORTH, y=SOUTH, g=WEST, b=EAST.
  // Output b = high only when both south and west inputs are high. `if` chosen over `*` to
  // produce a clean 0/15 output regardless of partial input levels.
  private static final String CONTROL_BOX_AND_PROGRAM = String.join("\n",
    "# AND of south and west inputs",
    "b = if(y, if(g, 15, 0), 0)"
  );
}
