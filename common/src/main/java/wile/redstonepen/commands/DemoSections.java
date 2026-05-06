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
import wile.redstonepen.blocks.RedstoneTrack;
import wile.redstonepen.libmc.Registries;

public final class DemoSections
{
  private DemoSections() {}

  private static final int FLAGS = DemoBuilder.FLAGS;
  private static final int CELL_SIZE = 9;                  // each contraption gets a 9×9 footprint
  private static final int GRID_COLUMNS = 4;               // 16 contraptions in a 4×4 grid
  private static final int GRID_SPACING = CELL_SIZE + 1;   // 1-block log border between cells

  /**
   * Builds the showcase: working redstone contraptions in a grid, each
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
      DemoSections::buildGaugeReadout,
      DemoSections::buildPenTrackWallClimb,
      DemoSections::buildTrafficLight,
      DemoSections::buildPulseCounter,
      DemoSections::buildSrLatch,
      DemoSections::buildPwmDemo,
      DemoSections::buildStepSequencer,
      DemoSections::buildHoldTimer
    };
    for(int i = 0; i < contraptions.length; ++i) {
      final BlockPos cell = DemoBuilder.cellOrigin(origin, i, GRID_COLUMNS, GRID_SPACING);
      contraptions[i].build(level, cell);
      refreshWireConnections(level, cell);
    }
    buildGridBorders(level, origin, contraptions.length);
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

  /**
   * Pen-track 3D route: a single signal climbs a 3-block tower on its south face,
   * crosses the tower top horizontally for 3 blocks, and drops south-side onto the
   * input face of a relay; the relay then drives a vanilla redstone lamp. The route
   * uses pen-track multi-face segments at every corner — a vanilla redstone wire
   * could not span this path without a staircase of full blocks per vertical step.
   */
  public static void buildPenTrackWallClimb(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block track = Registries.getBlock("track");
    final Block relay = Registries.getBlock("relay");
    if(track == null || relay == null) return;

    // Tower: 3-tall pillar at z=4 plus a horizontal top extension at y=2 going north.
    final BlockState stone = Blocks.STONE.defaultBlockState();
    level.setBlock(cell.offset(4, 0, 4), stone, FLAGS);
    level.setBlock(cell.offset(4, 1, 4), stone, FLAGS);
    level.setBlock(cell.offset(4, 2, 4), stone, FLAGS);
    level.setBlock(cell.offset(4, 2, 3), stone, FLAGS);
    level.setBlock(cell.offset(4, 2, 2), stone, FLAGS);
    level.setBlock(cell.offset(4, 2, 1), stone, FLAGS); // support for terminal relay

    DemoBuilder.placeAttached(level, cell.offset(4, 3, 1),
      relay.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    level.setBlock(cell.offset(4, 3, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);

    // Lever drives the wire from south on the platform. Start powered so tracks are
    // visible immediately; updateNeighborsAt below propagates that power through the net.
    level.setBlock(cell.offset(4, 0, 6),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.NORTH)
        .setValue(LeverBlock.POWERED, true), FLAGS);

    // 1) Floor track + start of climb on south face of tower base.
    placePenTrack(level, cell.offset(4, 0, 5),
      wireBit(Direction.DOWN, Direction.NORTH)
        | wireBit(Direction.DOWN, Direction.SOUTH)
        | wireBit(Direction.NORTH, Direction.DOWN)
        | wireBit(Direction.NORTH, Direction.UP));
    // 2) Vertical climb segment on south face of tower middle.
    placePenTrack(level, cell.offset(4, 1, 5),
      wireBit(Direction.NORTH, Direction.DOWN)
        | wireBit(Direction.NORTH, Direction.UP));
    // 3) Top of climb on south face of tower top.
    placePenTrack(level, cell.offset(4, 2, 5),
      wireBit(Direction.NORTH, Direction.DOWN)
        | wireBit(Direction.NORTH, Direction.UP));
    // 4-6) Horizontal run across the tower top (north of tower).
    placePenTrack(level, cell.offset(4, 3, 4),
      wireBit(Direction.DOWN, Direction.SOUTH)
        | wireBit(Direction.DOWN, Direction.NORTH));
    placePenTrack(level, cell.offset(4, 3, 3),
      wireBit(Direction.DOWN, Direction.SOUTH)
        | wireBit(Direction.DOWN, Direction.NORTH));
    placePenTrack(level, cell.offset(4, 3, 2),
      wireBit(Direction.DOWN, Direction.SOUTH)
        | wireBit(Direction.DOWN, Direction.NORTH));

    sign(level, cell.offset(4, 1, 8), "pen_track 3D", "lever to relay");
    level.updateNeighborsAt(cell.offset(4, 0, 6), Blocks.LEVER);
  }

  private static long wireBit(Direction face, Direction wireDirection)
  {
    return RedstoneTrack.defs.connections.getWireBit(face, wireDirection);
  }

  private static void placePenTrack(Level level, BlockPos pos, long wireFlags)
  {
    level.setBlock(pos, Registries.getBlock("track").defaultBlockState(), FLAGS);
    if(level.getBlockEntity(pos) instanceof RedstoneTrack.TrackBlockEntity te) {
      te.addWireFlags(wireFlags);
      te.handleShapeUpdate(Direction.DOWN, level.getBlockState(pos.below()), pos.below(), false);
      te.sync(true);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------------------------------

  // After all blocks in a cell are placed, call neighborChanged on every redstone wire so each
  // wire recalculates all four directional connections from the final world state. Without this,
  // bulk setBlock placement never calls neighborChanged on the wires themselves — only on their
  // neighbors — leaving the dot (all-NONE) visual until something interacts with the circuit.
  private static void refreshWireConnections(Level level, BlockPos cell)
  {
    for(int dx = 0; dx < CELL_SIZE; ++dx) {
      for(int dz = 0; dz < CELL_SIZE; ++dz) {
        final BlockPos pos = cell.offset(dx, 0, dz);
        if(level.getBlockState(pos).is(Blocks.REDSTONE_WIRE)) {
          level.neighborChanged(pos, Blocks.REDSTONE_WIRE, pos);
        }
      }
    }
  }

  private static void buildGridBorders(Level level, BlockPos origin, int numContraptions)
  {
    final int rows = (numContraptions + GRID_COLUMNS - 1) / GRID_COLUMNS;
    final BlockState log = Blocks.OAK_LOG.defaultBlockState();
    final BlockState air = Blocks.AIR.defaultBlockState();
    final int totalX = GRID_COLUMNS * GRID_SPACING - 1;
    final int totalZ = rows * GRID_SPACING - 1;

    for(int c = 0; c < GRID_COLUMNS - 1; ++c) {
      final int bx = c * GRID_SPACING + CELL_SIZE;
      for(int dz = 0; dz < totalZ; ++dz) {
        level.setBlock(origin.offset(bx, -1, dz), log, FLAGS);
        for(int dy = 0; dy < 4; ++dy) level.setBlock(origin.offset(bx, dy, dz), air, FLAGS);
      }
    }

    for(int r = 0; r < rows - 1; ++r) {
      final int bz = r * GRID_SPACING + CELL_SIZE;
      for(int dx = 0; dx < totalX; ++dx) {
        level.setBlock(origin.offset(dx, -1, bz), log, FLAGS);
        for(int dy = 0; dy < 4; ++dy) level.setBlock(origin.offset(dx, dy, bz), air, FLAGS);
      }
    }
  }

  private static void platform(Level level, BlockPos cell)
  {
    final BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
    final BlockState air = Blocks.AIR.defaultBlockState();
    for(int dx = 0; dx < CELL_SIZE; ++dx) {
      for(int dz = 0; dz < CELL_SIZE; ++dz) {
        level.setBlock(cell.offset(dx, -1, dz), gold, FLAGS);
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

  /**
   * Traffic light: CLOCK() drives a three-phase cycle (red/green/yellow) with no inputs.
   * Port r (NORTH) = red lamp, g (WEST) = green lamp, y (SOUTH) = yellow lamp.
   */
  public static void buildTrafficLight(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null) return;
    final BlockPos cbPos = cell.offset(3, 0, 3);
    DemoBuilder.placeAttached(level, cbPos,
      controlBox.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    if(level.getBlockEntity(cbPos) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(TRAFFIC_LIGHT_PROGRAM);
      cbe.setEnabled(true);
      cbe.setChanged();
    }
    // Red lamp — north output (port r)
    level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // Green lamp — west output (port g)
    level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(1, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // Yellow lamp — south output (port y)
    level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(3, 0, 5), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "traffic_light", "CLOCK() demo");
  }

  /**
   * Pulse counter: each button press fills one more lamp out of three — press 1→north,
   * press 2→north+east, press 3→all three, press 4→wraps back to zero.
   * Port y (SOUTH) = button, r (NORTH) = lamp 1, b (EAST) = lamp 2, g (WEST) = lamp 3.
   */
  public static void buildPulseCounter(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null) return;
    final BlockPos cbPos = cell.offset(3, 0, 3);
    DemoBuilder.placeAttached(level, cbPos,
      controlBox.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    if(level.getBlockEntity(cbPos) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(PULSE_COUNTER_PROGRAM);
      cbe.setEnabled(true);
      cbe.setChanged();
    }
    // Button — south input (port y)
    level.setBlock(cell.offset(3, 0, 5),
      Blocks.STONE_BUTTON.defaultBlockState()
        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Lamp 1 — north output (port r)
    level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // Lamp 2 — east output (port b)
    level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // Lamp 3 — west output (port g)
    level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(1, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "pulse_counter", "3-lamp fill");
  }

  /**
   * SR latch: two buttons latch a persistent state via software variables.
   * Port r (NORTH) = set button, y (SOUTH) = reset button, b (EAST) = lamp output.
   */
  public static void buildSrLatch(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null) return;
    final BlockPos cbPos = cell.offset(3, 0, 3);
    DemoBuilder.placeAttached(level, cbPos,
      controlBox.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    if(level.getBlockEntity(cbPos) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(SR_LATCH_PROGRAM);
      cbe.setEnabled(true);
      cbe.setChanged();
    }
    // Set button — north input (port r)
    level.setBlock(cell.offset(3, 0, 1),
      Blocks.STONE_BUTTON.defaultBlockState()
        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), FLAGS);
    level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Reset button — south input (port y)
    level.setBlock(cell.offset(3, 0, 5),
      Blocks.STONE_BUTTON.defaultBlockState()
        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Lamp — east output (port b)
    level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "sr_latch", "state persistence");
  }

  /**
   * Pulse-width modulator: lever on west sets duty cycle (0-15); output east flickers
   * proportionally. Port g (WEST) = lever input, b (EAST) = lamp output.
   */
  public static void buildPwmDemo(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null) return;
    final BlockPos cbPos = cell.offset(3, 0, 3);
    DemoBuilder.placeAttached(level, cbPos,
      controlBox.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    if(level.getBlockEntity(cbPos) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(PWM_PROGRAM);
      cbe.setEnabled(true);
      cbe.setChanged();
    }
    // Lever — west input (port g); signal level sets duty cycle
    level.setBlock(cell.offset(1, 0, 3),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.EAST), FLAGS);
    level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Lamp — east output (port b)
    level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "pwm", "duty cycle demo");
  }

  /**
   * Step sequencer: each button press advances through 3 steps, activating one output at a time.
   * Port y (SOUTH) = button, r (NORTH) = step-0 lamp, b (EAST) = step-1 lamp, g (WEST) = step-2 lamp.
   */
  public static void buildStepSequencer(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null) return;
    final BlockPos cbPos = cell.offset(3, 0, 3);
    DemoBuilder.placeAttached(level, cbPos,
      controlBox.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    if(level.getBlockEntity(cbPos) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(STEP_SEQUENCER_PROGRAM);
      cbe.setEnabled(true);
      cbe.setChanged();
    }
    // Advance button — south input (port y)
    level.setBlock(cell.offset(3, 0, 5),
      Blocks.STONE_BUTTON.defaultBlockState()
        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Step-0 lamp — north output (port r)
    level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // Step-1 lamp — east output (port b)
    level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    // Step-2 lamp — west output (port g)
    level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(1, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "step_sequencer", "CNT state machine");
  }

  /**
   * Hold timer: pressing the button triggers a timed output; the lever sets the duration
   * (g=0 → ~2 ticks, g=15 → ~62 ticks). Port y (SOUTH) = trigger button, g (WEST) = duration
   * lever, b (EAST) = lamp output.
   */
  public static void buildHoldTimer(Level level, BlockPos cell)
  {
    platform(level, cell);
    final Block controlBox = Registries.getBlock("control_box");
    if(controlBox == null) return;
    final BlockPos cbPos = cell.offset(3, 0, 3);
    DemoBuilder.placeAttached(level, cbPos,
      controlBox.defaultBlockState()
        .setValue(BlockStateProperties.FACING, Direction.DOWN)
        .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0));
    if(level.getBlockEntity(cbPos) instanceof ControlBox.ControlBoxBlockEntity cbe) {
      cbe.setCode(HOLD_TIMER_PROGRAM);
      cbe.setEnabled(true);
      cbe.setChanged();
    }
    // Trigger button — south input (port y)
    level.setBlock(cell.offset(3, 0, 5),
      Blocks.STONE_BUTTON.defaultBlockState()
        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), FLAGS);
    level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Duration lever — west input (port g)
    level.setBlock(cell.offset(1, 0, 3),
      Blocks.LEVER.defaultBlockState()
        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
        .setValue(LeverBlock.FACING, Direction.EAST), FLAGS);
    level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    // Lamp — east output (port b)
    level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS);
    level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS);
    sign(level, cell.offset(4, 1, 6), "hold_timer", "TON demo");
  }

  // ControlBox AND program. Port mapping: d=DOWN, u=UP, r=NORTH, y=SOUTH, g=WEST, b=EAST.
  // Output b = high only when both south and west inputs are high. `if` chosen over `*` to
  // produce a clean 0/15 output regardless of partial input levels.
  private static final String CONTROL_BOX_AND_PROGRAM = String.join("\n",
    "# AND of south and west inputs",
    "b = if(y, if(g, 15, 0), 0)"
  );

  // Traffic light: 90-tick cycle split into red (40t), green (40t), yellow (10t).
  private static final String TRAFFIC_LIGHT_PROGRAM = String.join("\n",
    "# Autonomous traffic light: red 40t, green 40t, yellow 10t",
    "phase = CLOCK() % 90",
    "r = if(phase < 40, 15, 0)",
    "y = if(phase >= 80, 15, 0)",
    "g = if(phase >= 40, if(phase < 80, 15, 0), 0)"
  );

  // Pulse counter: each press (y.re) fills one more of three lamps before wrapping.
  // count % 4 cycles 0→1→2→3→0; r lights at count>=1, b at count>=2, g at count>=3.
  private static final String PULSE_COUNTER_PROGRAM = String.join("\n",
    "# Each press fills one more lamp; wraps every 4 presses",
    "count = cnt1(y.re) % 4",
    "r = if(count > 0, 15, 0)",
    "b = if(count > 1, 15, 0)",
    "g = if(count > 2, 15, 0)"
  );

  // SR latch: r.re sets state to 15, y.re clears it; state persists between ticks.
  private static final String SR_LATCH_PROGRAM = String.join("\n",
    "# Set-reset memory latch",
    "state = if(r.re, 15, if(y.re, 0, state))",
    "b = state"
  );

  // PWM: output b is on for the first g*2 ticks of every 32-tick clock period.
  // At g=0 always off, g=8 ~50% duty, g=15 on 30/32 ticks.
  private static final String PWM_PROGRAM = String.join("\n",
    "# Duty cycle = west lever position (0-15) out of 32 ticks",
    "b = if(CLOCK() % 32 < g * 2, 15, 0)"
  );

  // Step sequencer: each button press advances cnt1 by 1; modulo 3 cycles r -> b -> g.
  private static final String STEP_SEQUENCER_PROGRAM = String.join("\n",
    "# Three-step sequencer; button advances to next output",
    "step = cnt1(y.re) % 3",
    "r = if(step == 0, 15, 0)",
    "b = if(step == 1, 15, 0)",
    "g = if(step == 2, 15, 0)"
  );

  // Hold timer: button (y) triggers a countdown from g*4+2 ticks; lamp on while timer > 0.
  // At g=0 the timer is 2 ticks; at g=15 it is 62 ticks.
  private static final String HOLD_TIMER_PROGRAM = String.join("\n",
    "# Button starts timed output; lever sets duration (2-62 ticks)",
    "timer = if(y.re, g * 4 + 2, if(timer > 0, timer - 1, 0))",
    "b = if(timer > 0, 15, 0)"
  );
}
