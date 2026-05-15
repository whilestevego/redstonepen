package wile.redstonepen.commands

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.RepeaterBlock
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import wile.redstonepen.blocks.CircuitComponents
import wile.redstonepen.blocks.controlbox.ControlBoxBlockEntity
import wile.redstonepen.blocks.track.RedstoneTrackDefs
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.registry.Registries

object DemoSections {

    private val FLAGS = DemoBuilder.FLAGS
    private const val CELL_SIZE = 9
    private const val GRID_COLUMNS = 4
    private const val GRID_SPACING = CELL_SIZE + 1

    @JvmStatic
    fun runCircuits(level: Level, origin: BlockPos) {
        val contraptions: List<(Level, BlockPos) -> Unit> =
            listOf(
                ::buildLeverDrivesLamp,
                ::buildButtonDrivesPiston,
                ::buildInvertedRelayNotGate,
                ::buildBistableToggle,
                ::buildPulseRelayMonostable,
                ::buildRelayBuffer,
                ::buildBridgeRelayCrossover,
                ::buildControlBoxAndGate,
                ::buildGaugeReadout,
                ::buildPenTrackWallClimb,
                ::buildTrafficLight,
                ::buildPulseCounter,
                ::buildSrLatch,
                ::buildPwmDemo,
                ::buildStepSequencer,
                ::buildHoldTimer,
            )
        contraptions.forEachIndexed { i, build ->
            val cell = DemoBuilder.cellOrigin(origin, i, GRID_COLUMNS, GRID_SPACING)
            build(level, cell)
            refreshWireConnections(level, cell)
        }
        buildGridBorders(level, origin, contraptions.size)
    }

    // -----------------------------------------------------------------------------------------------
    // Contraptions
    // -----------------------------------------------------------------------------------------------

    @JvmStatic
    fun buildLeverDrivesLamp(level: Level, cell: BlockPos) {
        platform(level, cell)
        val lever = Registries.getBlock("basic_lever") ?: return
        level.setBlock(
            cell.offset(2, 0, 4),
            lever
                .defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
            FLAGS,
        )
        for (dz in 3 downTo 1) {
            level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        }
        level.setBlock(cell.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "basic_lever", "drives lamp")
    }

    @JvmStatic
    fun buildButtonDrivesPiston(level: Level, cell: BlockPos) {
        platform(level, cell)
        val button = Registries.getBlock("basic_button") ?: return
        level.setBlock(
            cell.offset(2, 0, 4),
            button
                .defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
            FLAGS,
        )
        for (dz in 3 downTo 2) {
            level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        }
        level.setBlock(
            cell.offset(2, 0, 1),
            Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 0), Blocks.GLOWSTONE.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "basic_button", "pulses piston")
    }

    @JvmStatic
    fun buildInvertedRelayNotGate(level: Level, cell: BlockPos) {
        platform(level, cell)
        val relay = Registries.getBlock("inverted_relay") ?: return
        level.setBlock(
            cell.offset(2, 0, 5),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        placeFloorRelay(level, cell, relay)
        wireAndLamp(level, cell)
        sign(level, cell.offset(4, 1, 6), "inverted_relay", "NOT gate")
    }

    @JvmStatic
    fun buildBistableToggle(level: Level, cell: BlockPos) {
        platform(level, cell)
        val relay = Registries.getBlock("bistable_relay") ?: return
        level.setBlock(
            cell.offset(2, 0, 5),
            Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.FLOOR)
                .setValue(ButtonBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        placeFloorRelay(level, cell, relay)
        wireAndLamp(level, cell)
        sign(level, cell.offset(4, 1, 6), "bistable_relay", "T flip-flop")
    }

    @JvmStatic
    fun buildPulseRelayMonostable(level: Level, cell: BlockPos) {
        platform(level, cell)
        val relay = Registries.getBlock("pulse_relay") ?: return
        level.setBlock(
            cell.offset(2, 0, 5),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        placeFloorRelay(level, cell, relay)
        wireAndLamp(level, cell)
        sign(level, cell.offset(4, 1, 6), "pulse_relay", "edge pulse")
    }

    @JvmStatic
    fun buildRelayBuffer(level: Level, cell: BlockPos) {
        platform(level, cell)
        val relay = Registries.getBlock("relay") ?: return
        level.setBlock(
            cell.offset(2, 0, 5),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        placeFloorRelay(level, cell, relay)
        wireAndLamp(level, cell)
        sign(level, cell.offset(4, 1, 6), "relay", "buffer / direction")
    }

    @JvmStatic
    fun buildBridgeRelayCrossover(level: Level, cell: BlockPos) {
        platform(level, cell)
        val relay = Registries.getBlock("bridge_relay") ?: return
        level.setBlock(
            cell.offset(4, 0, 5),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(4, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        level.setBlock(
            cell.offset(3, 0, 3),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.EAST),
            FLAGS,
        )
        level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        DemoBuilder.placeAttached(
            level,
            cell.offset(4, 0, 3),
            relay
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.DOWN)
                .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0),
        )
        sign(level, cell.offset(7, 1, 6), "bridge_relay", "crossover")
    }

    @JvmStatic
    fun buildControlBoxAndGate(level: Level, cell: BlockPos) {
        platform(level, cell)
        placeControlBox(level, cell, CONTROL_BOX_AND_PROGRAM)
        level.setBlock(
            cell.offset(3, 0, 5),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(
            cell.offset(1, 0, 3),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.EAST),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(7, 1, 6), "control_box", "AND program")
    }

    @JvmStatic
    fun buildGaugeReadout(level: Level, cell: BlockPos) {
        platform(level, cell)
        val gauge = Registries.getBlock("basic_gauge") ?: return
        level.setBlock(
            cell.offset(2, 0, 5),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(
            cell.offset(2, 0, 3),
            Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.SOUTH),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(2, 0, 1), gauge.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "basic_gauge", "signal readout")
    }

    @JvmStatic
    fun buildPenTrackWallClimb(level: Level, cell: BlockPos) {
        platform(level, cell)
        val track = Registries.getBlock("track")
        val relay = Registries.getBlock("relay")
        if (track == null || relay == null) return

        val stone = Blocks.STONE.defaultBlockState()
        level.setBlock(cell.offset(4, 0, 4), stone, FLAGS)
        level.setBlock(cell.offset(4, 1, 4), stone, FLAGS)
        level.setBlock(cell.offset(4, 2, 4), stone, FLAGS)
        level.setBlock(cell.offset(4, 2, 3), stone, FLAGS)
        level.setBlock(cell.offset(4, 2, 2), stone, FLAGS)
        level.setBlock(cell.offset(4, 2, 1), stone, FLAGS)

        DemoBuilder.placeAttached(
            level,
            cell.offset(4, 3, 1),
            relay
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.DOWN)
                .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0),
        )
        level.setBlock(cell.offset(4, 3, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)

        level.setBlock(
            cell.offset(4, 0, 6),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.NORTH)
                .setValue(LeverBlock.POWERED, true),
            FLAGS,
        )

        placePenTrack(
            level,
            cell.offset(4, 0, 5),
            wireBit(Direction.DOWN, Direction.NORTH) or
                wireBit(Direction.DOWN, Direction.SOUTH) or
                wireBit(Direction.NORTH, Direction.DOWN) or
                wireBit(Direction.NORTH, Direction.UP),
        )
        placePenTrack(
            level,
            cell.offset(4, 1, 5),
            wireBit(Direction.NORTH, Direction.DOWN) or wireBit(Direction.NORTH, Direction.UP),
        )
        placePenTrack(
            level,
            cell.offset(4, 2, 5),
            wireBit(Direction.NORTH, Direction.DOWN) or wireBit(Direction.NORTH, Direction.UP),
        )
        placePenTrack(
            level,
            cell.offset(4, 3, 4),
            wireBit(Direction.DOWN, Direction.SOUTH) or wireBit(Direction.DOWN, Direction.NORTH),
        )
        placePenTrack(
            level,
            cell.offset(4, 3, 3),
            wireBit(Direction.DOWN, Direction.SOUTH) or wireBit(Direction.DOWN, Direction.NORTH),
        )
        placePenTrack(
            level,
            cell.offset(4, 3, 2),
            wireBit(Direction.DOWN, Direction.SOUTH) or wireBit(Direction.DOWN, Direction.NORTH),
        )

        sign(level, cell.offset(4, 1, 8), "pen_track 3D", "lever to relay")
        level.updateNeighborsAt(cell.offset(4, 0, 6), Blocks.LEVER)
    }

    @JvmStatic
    fun buildTrafficLight(level: Level, cell: BlockPos) {
        platform(level, cell)
        placeControlBox(level, cell, TRAFFIC_LIGHT_PROGRAM)
        level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(1, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(3, 0, 5), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "traffic_light", "CLOCK() demo")
    }

    @JvmStatic
    fun buildPulseCounter(level: Level, cell: BlockPos) {
        platform(level, cell)
        placeControlBox(level, cell, PULSE_COUNTER_PROGRAM)
        level.setBlock(
            cell.offset(3, 0, 5),
            Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(1, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "pulse_counter", "3-lamp fill")
    }

    @JvmStatic
    fun buildSrLatch(level: Level, cell: BlockPos) {
        platform(level, cell)
        placeControlBox(level, cell, SR_LATCH_PROGRAM)
        level.setBlock(
            cell.offset(3, 0, 1),
            Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH),
            FLAGS,
        )
        level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(
            cell.offset(3, 0, 5),
            Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "sr_latch", "state persistence")
    }

    @JvmStatic
    fun buildPwmDemo(level: Level, cell: BlockPos) {
        platform(level, cell)
        placeControlBox(level, cell, PWM_PROGRAM)
        level.setBlock(
            cell.offset(1, 0, 3),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.EAST),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "pwm", "duty cycle demo")
    }

    @JvmStatic
    fun buildStepSequencer(level: Level, cell: BlockPos) {
        platform(level, cell)
        placeControlBox(level, cell, STEP_SEQUENCER_PROGRAM)
        level.setBlock(
            cell.offset(3, 0, 5),
            Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(3, 0, 2), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(3, 0, 1), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(1, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "step_sequencer", "CNT state machine")
    }

    @JvmStatic
    fun buildHoldTimer(level: Level, cell: BlockPos) {
        platform(level, cell)
        placeControlBox(level, cell, HOLD_TIMER_PROGRAM)
        level.setBlock(
            cell.offset(3, 0, 5),
            Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH),
            FLAGS,
        )
        level.setBlock(cell.offset(3, 0, 4), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(
            cell.offset(1, 0, 3),
            Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.EAST),
            FLAGS,
        )
        level.setBlock(cell.offset(2, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(4, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(5, 0, 3), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        level.setBlock(cell.offset(6, 0, 3), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
        sign(level, cell.offset(4, 1, 6), "hold_timer", "TON demo")
    }

    // -----------------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------------

    @JvmStatic
    fun refreshWireConnections(level: Level, cell: BlockPos) {
        for (dx in 0 until CELL_SIZE) {
            for (dz in 0 until CELL_SIZE) {
                val pos = cell.offset(dx, 0, dz)
                val state = level.getBlockState(pos)
                if (!state.`is`(Blocks.REDSTONE_WIRE)) continue
                var updated = state
                for (dir in Direction.Plane.HORIZONTAL) {
                    val neighborPos = pos.relative(dir)
                    updated =
                        updated.updateShape(
                            dir,
                            level.getBlockState(neighborPos),
                            level,
                            pos,
                            neighborPos,
                        )
                }
                if (updated !== state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS)
            }
        }
    }

    private fun buildGridBorders(level: Level, origin: BlockPos, numContraptions: Int) {
        val rows = (numContraptions + GRID_COLUMNS - 1) / GRID_COLUMNS
        val log = Blocks.OAK_LOG.defaultBlockState()
        val air = Blocks.AIR.defaultBlockState()
        val totalX = GRID_COLUMNS * GRID_SPACING - 1
        val totalZ = rows * GRID_SPACING - 1
        for (c in 0 until GRID_COLUMNS - 1) {
            val bx = c * GRID_SPACING + CELL_SIZE
            for (dz in 0 until totalZ) {
                level.setBlock(origin.offset(bx, -1, dz), log, FLAGS)
                for (dy in 0 until 4) level.setBlock(origin.offset(bx, dy, dz), air, FLAGS)
            }
        }
        for (r in 0 until rows - 1) {
            val bz = r * GRID_SPACING + CELL_SIZE
            for (dx in 0 until totalX) {
                level.setBlock(origin.offset(dx, -1, bz), log, FLAGS)
                for (dy in 0 until 4) level.setBlock(origin.offset(dx, dy, bz), air, FLAGS)
            }
        }
    }

    private fun platform(level: Level, cell: BlockPos) {
        val gold = Blocks.GOLD_BLOCK.defaultBlockState()
        val air = Blocks.AIR.defaultBlockState()
        for (dx in 0 until CELL_SIZE) {
            for (dz in 0 until CELL_SIZE) {
                level.setBlock(cell.offset(dx, -1, dz), gold, FLAGS)
                for (dy in 0 until 4) level.setBlock(cell.offset(dx, dy, dz), air, FLAGS)
            }
        }
    }

    private fun sign(level: Level, pos: BlockPos, line1: String, line2: String) {
        DemoBuilder.placeStandingSign(level, pos, 8, line1, line2)
    }

    private fun placeControlBox(level: Level, cell: BlockPos, program: String) {
        val controlBox = Registries.getBlock("control_box") ?: return
        val cbPos = cell.offset(3, 0, 3)
        DemoBuilder.placeAttached(
            level,
            cbPos,
            controlBox
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.DOWN)
                .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0),
        )
        (level.getBlockEntity(cbPos) as? ControlBoxBlockEntity)?.also { cbe ->
            cbe.setCode(program)
            cbe.setEnabled(true)
            cbe.setChanged()
        }
    }

    private fun placeFloorRelay(
        level: Level,
        cell: BlockPos,
        relay: net.minecraft.world.level.block.Block,
    ) {
        DemoBuilder.placeAttached(
            level,
            cell.offset(2, 0, 3),
            relay
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.DOWN)
                .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0),
        )
    }

    private fun wireAndLamp(level: Level, cell: BlockPos) {
        for (dz in 2 downTo 1) {
            level.setBlock(cell.offset(2, 0, dz), Blocks.REDSTONE_WIRE.defaultBlockState(), FLAGS)
        }
        level.setBlock(cell.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), FLAGS)
    }

    private fun wireBit(face: Direction, wireDirection: Direction): Long =
        RedstoneTrackDefs.connections.getWireBit(face, wireDirection)

    private fun placePenTrack(level: Level, pos: BlockPos, wireFlags: Long) {
        level.setBlock(pos, Registries.requireBlock("track").defaultBlockState(), FLAGS)
        (level.getBlockEntity(pos) as? TrackBlockEntity)?.also { te ->
            te.addWireFlags(wireFlags)
            te.handleShapeUpdate(
                Direction.DOWN,
                level.getBlockState(pos.below()),
                pos.below(),
                false,
            )
            te.sync(true)
        }
    }

    // ControlBox programs
    private val CONTROL_BOX_AND_PROGRAM =
        listOf("# AND of south and west inputs", "b = if(y, if(g, 15, 0), 0)").joinToString("\n")

    private val TRAFFIC_LIGHT_PROGRAM =
        listOf(
                "# Autonomous traffic light: red 40t, green 40t, yellow 10t",
                "phase = CLOCK() % 90",
                "r = if(phase < 40, 15, 0)",
                "y = if(phase >= 80, 15, 0)",
                "g = if(phase >= 40, if(phase < 80, 15, 0), 0)",
            )
            .joinToString("\n")

    private val PULSE_COUNTER_PROGRAM =
        listOf(
                "# Each press fills one more lamp; wraps every 4 presses",
                "count = cnt1(y.re) % 4",
                "r = if(count > 0, 15, 0)",
                "b = if(count > 1, 15, 0)",
                "g = if(count > 2, 15, 0)",
            )
            .joinToString("\n")

    private val SR_LATCH_PROGRAM =
        listOf("# Set-reset memory latch", "state = if(r.re, 15, if(y.re, 0, state))", "b = state")
            .joinToString("\n")

    private val PWM_PROGRAM =
        listOf(
                "# Duty cycle = west lever position (0-15) out of 32 ticks",
                "b = if(CLOCK() % 32 < g * 2, 15, 0)",
            )
            .joinToString("\n")

    private val STEP_SEQUENCER_PROGRAM =
        listOf(
                "# Three-step sequencer; button advances to next output",
                "step = cnt1(y.re) % 3",
                "r = if(step == 0, 15, 0)",
                "b = if(step == 1, 15, 0)",
                "g = if(step == 2, 15, 0)",
            )
            .joinToString("\n")

    private val HOLD_TIMER_PROGRAM =
        listOf(
                "# Button starts timed output; lever sets duration (2-62 ticks)",
                "timer = if(y.re, g * 4 + 2, if(timer > 0, timer - 1, 0))",
                "b = if(timer > 0, 15, 0)",
            )
            .joinToString("\n")
}
