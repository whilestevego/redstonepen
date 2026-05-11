package wile.redstonepen.gametestcommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import wile.redstonepen.blocks.CircuitComponents
import wile.redstonepen.registry.Registries

object RelayTests {
    private val RELAY_POS = BlockPos(1, 1, 1)
    private val INPUT_POS = RELAY_POS.east()

    @JvmStatic fun relayActivatesFromRedstone(helper: GameTestHelper) {
        placePoweredInput(helper, Registries.getBlock("relay")!!)
        helper.succeedWhen { assertRelayState(helper, Registries.getBlock("relay")!!, true, 0, "expected relay to become powered from adjacent redstone input") }
    }

    @JvmStatic fun invertedRelayRegistersPoweredInput(helper: GameTestHelper) {
        placePoweredInput(helper, Registries.getBlock("inverted_relay")!!)
        helper.succeedWhen { assertRelayState(helper, Registries.getBlock("inverted_relay")!!, true, 0, "expected inverted relay to register its powered input") }
    }

    @JvmStatic fun pulseRelayClearsItsPulseAfterTick(helper: GameTestHelper) {
        placePoweredInput(helper, Registries.getBlock("pulse_relay")!!)
        helper.runAtTickTime(1) { assertRelayState(helper, Registries.getBlock("pulse_relay")!!, true, 1, "expected pulse relay to enter its pulsing state") }
        helper.runAtTickTime(4) {
            assertRelayState(helper, Registries.getBlock("pulse_relay")!!, true, 0, "expected pulse relay to clear its pulse after the scheduled tick")
            helper.succeed()
        }
    }

    @JvmStatic fun pulseRelaySetsPoweredFalseWhenInputRemoved(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS.north(), Blocks.STONE)
        placePoweredInput(helper, Registries.getBlock("pulse_relay")!!)
        helper.runAtTickTime(8) { helper.setBlock(INPUT_POS, Blocks.AIR) }
        helper.runAtTickTime(10) {
            assertRelayState(helper, Registries.getBlock("pulse_relay")!!, false, 0, "pulse relay must become POWERED=false when power source removed")
            helper.succeed()
        }
    }

    @JvmStatic fun bistableRelayLatchesOnFirstRisingEdge(helper: GameTestHelper) {
        placePoweredInput(helper, Registries.getBlock("bistable_relay")!!)
        helper.succeedWhen { assertRelayState(helper, Registries.getBlock("bistable_relay")!!, true, 1, "expected bistable relay to latch on after the first rising edge") }
    }

    @JvmStatic fun relayDownFacingRotation0ActivatesFromEast(helper: GameTestHelper) {
        val relayBlock = Registries.getBlock("relay")!!
        helper.setBlock(RELAY_POS.east(), Blocks.REDSTONE_BLOCK)
        helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
            .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
            .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0))
        helper.succeedWhen { assertRelayState(helper, relayBlock, true, 0, "relay with FACING=DOWN ROTATION=0 must activate when input is from east (non-output side)") }
    }

    @JvmStatic fun relayDownFacingRotation0DoesNotActivateFromNorth(helper: GameTestHelper) {
        val relayBlock = Registries.getBlock("relay")!!
        helper.setBlock(RELAY_POS.north(), Blocks.REDSTONE_BLOCK)
        helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
            .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
            .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0))
        helper.succeedWhen { assertRelayState(helper, relayBlock, false, 0, "relay with FACING=DOWN ROTATION=0 must not activate when input is from north (output side)") }
    }

    @JvmStatic fun relayDownFacingRotation1ShiftsOutputToEast(helper: GameTestHelper) {
        val relayBlock = Registries.getBlock("relay")!!
        helper.setBlock(RELAY_POS.east(), Blocks.REDSTONE_BLOCK)
        helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
            .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
            .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 1))
        helper.succeedWhen { assertRelayState(helper, relayBlock, false, 0, "relay with FACING=DOWN ROTATION=1 must not activate when input is from east (now the output side)") }
    }

    @JvmStatic fun relayDownFacingRotation1ActivatesFromNorth(helper: GameTestHelper) {
        val relayBlock = Registries.getBlock("relay")!!
        helper.setBlock(RELAY_POS.north(), Blocks.REDSTONE_BLOCK)
        helper.setBlock(RELAY_POS, relayBlock.defaultBlockState()
            .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
            .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 1))
        helper.succeedWhen { assertRelayState(helper, relayBlock, true, 0, "relay with FACING=DOWN ROTATION=1 must activate when input is from north (non-output side)") }
    }

    @JvmStatic fun downFacingRotation0FrontIsNorth(helper: GameTestHelper) {
        val s = relayState(Direction.DOWN, 0)
        if (CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.NORTH)
            helper.fail("FACING=DOWN ROTATION=0: expected getFrontFacing=NORTH")
        helper.succeed()
    }

    @JvmStatic fun downFacingRotation1FrontIsEast(helper: GameTestHelper) {
        val s = relayState(Direction.DOWN, 1)
        if (CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.EAST)
            helper.fail("FACING=DOWN ROTATION=1: expected getFrontFacing=EAST")
        helper.succeed()
    }

    @JvmStatic fun downFacingRotation2FrontIsSouth(helper: GameTestHelper) {
        val s = relayState(Direction.DOWN, 2)
        if (CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.SOUTH)
            helper.fail("FACING=DOWN ROTATION=2: expected getFrontFacing=SOUTH")
        helper.succeed()
    }

    @JvmStatic fun downFacingRotation3FrontIsWest(helper: GameTestHelper) {
        val s = relayState(Direction.DOWN, 3)
        if (CircuitComponents.DirectedComponentBlock.getFrontFacing(s) != Direction.WEST)
            helper.fail("FACING=DOWN ROTATION=3: expected getFrontFacing=WEST")
        helper.succeed()
    }

    @JvmStatic fun upFacingIsOppositeOfFacingForAllDirections(helper: GameTestHelper) {
        val relay = Registries.getBlock("relay")!!
        for (face in Direction.values()) {
            val s = relay.defaultBlockState()
                .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0)
            val expected = face.opposite
            val actual = CircuitComponents.DirectedComponentBlock.getUpFacing(s)
            if (actual != expected)
                helper.fail("getUpFacing for FACING=$face: expected $expected got $actual")
        }
        helper.succeed()
    }

    @JvmStatic fun downFacingMethodEqualsFacingPropertyForAllDirections(helper: GameTestHelper) {
        val relay = Registries.getBlock("relay")!!
        for (face in Direction.values()) {
            val s = relay.defaultBlockState()
                .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0)
            val actual = CircuitComponents.DirectedComponentBlock.getDownFacing(s)
            if (actual != face)
                helper.fail("getDownFacing for FACING=$face: expected $face got $actual")
        }
        helper.succeed()
    }

    @JvmStatic fun rightFacingIsRotationPlusOneRelativeToFront(helper: GameTestHelper) {
        val relay = Registries.getBlock("relay")!!
        for (face in Direction.values()) {
            for (r in 0..3) {
                val s = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r)
                val sNext = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, (r + 1) and 3)
                val expected = CircuitComponents.DirectedComponentBlock.getFrontFacing(sNext)
                val actual = CircuitComponents.DirectedComponentBlock.getRightFacing(s)
                if (actual != expected)
                    helper.fail("getRightFacing FACING=$face ROTATION=$r: expected $expected got $actual")
            }
        }
        helper.succeed()
    }

    @JvmStatic fun backFacingIsRotationPlusTwoRelativeToFront(helper: GameTestHelper) {
        val relay = Registries.getBlock("relay")!!
        for (face in Direction.values()) {
            for (r in 0..3) {
                val s = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r)
                val sTwo = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, (r + 2) and 3)
                val expected = CircuitComponents.DirectedComponentBlock.getFrontFacing(sTwo)
                val actual = CircuitComponents.DirectedComponentBlock.getBackFacing(s)
                if (actual != expected)
                    helper.fail("getBackFacing FACING=$face ROTATION=$r: expected $expected got $actual")
            }
        }
        helper.succeed()
    }

    @JvmStatic fun leftFacingIsRotationPlusThreeRelativeToFront(helper: GameTestHelper) {
        val relay = Registries.getBlock("relay")!!
        for (face in Direction.values()) {
            for (r in 0..3) {
                val s = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r)
                val sThree = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, (r + 3) and 3)
                val expected = CircuitComponents.DirectedComponentBlock.getFrontFacing(sThree)
                val actual = CircuitComponents.DirectedComponentBlock.getLeftFacing(s)
                if (actual != expected)
                    helper.fail("getLeftFacing FACING=$face ROTATION=$r: expected $expected got $actual")
            }
        }
        helper.succeed()
    }

    @JvmStatic fun allSixMappedDirectionsAreDistinctForEveryState(helper: GameTestHelper) {
        val relay = Registries.getBlock("relay")!!
        for (face in Direction.values()) {
            for (r in 0..3) {
                val s = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r)
                val dirs = mutableSetOf<Direction>()
                dirs.add(CircuitComponents.DirectedComponentBlock.getFrontFacing(s))
                dirs.add(CircuitComponents.DirectedComponentBlock.getBackFacing(s))
                dirs.add(CircuitComponents.DirectedComponentBlock.getLeftFacing(s))
                dirs.add(CircuitComponents.DirectedComponentBlock.getRightFacing(s))
                dirs.add(CircuitComponents.DirectedComponentBlock.getUpFacing(s))
                dirs.add(CircuitComponents.DirectedComponentBlock.getDownFacing(s))
                if (dirs.size != 6)
                    helper.fail("FACING=$face ROTATION=$r: expected 6 distinct facing directions, got ${dirs.size}")
            }
        }
        helper.succeed()
    }

    @JvmStatic fun forwardThenReverseStateMappedFacingIsIdentity(helper: GameTestHelper) {
        val relay = Registries.getBlock("relay")!!
        for (face in Direction.values()) {
            for (r in 0..3) {
                val s = relay.defaultBlockState()
                    .setValue(CircuitComponents.DirectedComponentBlock.FACING, face)
                    .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, r)
                for (worldSide in Direction.values()) {
                    val mapped = CircuitComponents.DirectedComponentBlock.getForwardStateMappedFacing(s, worldSide)
                    val back = CircuitComponents.DirectedComponentBlock.getReverseStateMappedFacing(s, mapped)
                    if (back != worldSide)
                        helper.fail("FACING=$face ROTATION=$r side=$worldSide: reverse(forward(side)) != side, got $back")
                }
            }
        }
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementDownFaceRotation0(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.UP, Vec3(0.0, 0.0, -0.3))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.DOWN)
            helper.fail("expected FACING=DOWN, got ${s.getValue(CircuitComponents.DirectedComponentBlock.FACING)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementDownFaceRotation1(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.UP, Vec3(0.3, 0.0, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
            helper.fail("expected ROTATION=1, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementDownFaceRotation2(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.UP, Vec3(0.0, 0.0, 0.3))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 2)
            helper.fail("expected ROTATION=2, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementDownFaceRotation3(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.UP, Vec3(-0.3, 0.0, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 3)
            helper.fail("expected ROTATION=3, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementNorthFaceRotation0(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.SOUTH, Vec3(0.0, 0.3, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null for NORTH face")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.NORTH)
            helper.fail("expected FACING=NORTH, got ${s.getValue(CircuitComponents.DirectedComponentBlock.FACING)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementNorthFaceRotation1(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.SOUTH, Vec3(0.3, 0.0, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
            helper.fail("expected ROTATION=1, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementNorthFaceRotation2(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.SOUTH, Vec3(0.0, -0.3, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 2)
            helper.fail("expected ROTATION=2, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementEastFaceRotation1(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.WEST, Vec3(0.0, 0.0, 0.3))
        if (s == null) helper.fail("getStateForPlacement returned null for EAST face")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.EAST)
            helper.fail("expected FACING=EAST, got ${s.getValue(CircuitComponents.DirectedComponentBlock.FACING)}")
        if (s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
            helper.fail("expected ROTATION=1, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementEastFaceRotation2(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.WEST, Vec3(0.0, -0.3, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 2)
            helper.fail("expected ROTATION=2, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementSouthFaceRotation1(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.NORTH, Vec3(-0.3, 0.0, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null for SOUTH face")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.SOUTH)
            helper.fail("expected FACING=SOUTH, got ${s.getValue(CircuitComponents.DirectedComponentBlock.FACING)}")
        if (s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
            helper.fail("expected ROTATION=1, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementSouthFaceRotation3(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.NORTH, Vec3(0.3, 0.0, 0.0))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 3)
            helper.fail("expected ROTATION=3, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementWestFaceRotation1(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.EAST, Vec3(0.0, 0.0, -0.3))
        if (s == null) helper.fail("getStateForPlacement returned null for WEST face")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.FACING) != Direction.WEST)
            helper.fail("expected FACING=WEST, got ${s.getValue(CircuitComponents.DirectedComponentBlock.FACING)}")
        if (s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 1)
            helper.fail("expected ROTATION=1, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementWestFaceRotation3(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.STONE)
        val s = relayPlacementState(helper, RELAY_POS, Direction.EAST, Vec3(0.0, 0.0, 0.3))
        if (s == null) helper.fail("getStateForPlacement returned null")
        if (s!!.getValue(CircuitComponents.DirectedComponentBlock.ROTATION) != 3)
            helper.fail("expected ROTATION=3, got ${s.getValue(CircuitComponents.DirectedComponentBlock.ROTATION)}")
        helper.succeed()
    }

    @JvmStatic fun getStateForPlacementReturnsNullWhenNoSurface(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Blocks.AIR)
        val s = relayPlacementState(helper, RELAY_POS, Direction.UP, Vec3.ZERO)
        if (s != null) helper.fail("expected null from getStateForPlacement when surface is not sturdy")
        helper.succeed()
    }

    @JvmStatic fun relayDropsWhenSupportBlockRemoved(helper: GameTestHelper) {
        val supportPos = RELAY_POS
        val relayPos = RELAY_POS.above()
        helper.setBlock(supportPos, Blocks.STONE)
        helper.setBlock(relayPos, Registries.getBlock("relay")!!.defaultBlockState()
            .setValue(CircuitComponents.DirectedComponentBlock.FACING, Direction.DOWN)
            .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, 0))
        helper.setBlock(supportPos, Blocks.AIR)
        helper.succeedWhen {
            if (!helper.getBlockState(relayPos).isAir)
                helper.fail("expected relay to become air after support removed")
        }
    }

    @JvmStatic fun relayDeactivatesWhenPowerRemovedViaTick(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS.north(), Blocks.STONE)
        placePoweredInput(helper, Registries.getBlock("relay")!!)
        helper.runAfterDelay(5) { helper.setBlock(INPUT_POS, Blocks.AIR) }
        helper.succeedWhen { assertRelayState(helper, Registries.getBlock("relay")!!, false, 0, "relay must deactivate after power source removed") }
    }

    @JvmStatic fun bistableRelayDeactivatesOnFallingEdge(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS.north(), Blocks.STONE)
        placePoweredInput(helper, Registries.getBlock("bistable_relay")!!)
        helper.runAfterDelay(5) { helper.setBlock(INPUT_POS, Blocks.AIR) }
        helper.succeedWhen { assertRelayState(helper, Registries.getBlock("bistable_relay")!!, false, 1, "bistable relay must set POWERED=false after power removed while STATE stays latched") }
    }

    @JvmStatic fun invertedRelayDeactivatesWhenPowerRemoved(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS.north(), Blocks.STONE)
        placePoweredInput(helper, Registries.getBlock("inverted_relay")!!)
        helper.runAtTickTime(4) { assertRelayState(helper, Registries.getBlock("inverted_relay")!!, true, 0, "inverted relay must be POWERED=true before power removal") }
        helper.runAtTickTime(5) { helper.setBlock(INPUT_POS, Blocks.AIR) }
        helper.runAtTickTime(6) {
            assertRelayState(helper, Registries.getBlock("inverted_relay")!!, false, 0, "inverted relay must set POWERED=false immediately when power source removed")
            helper.succeed()
        }
    }

    @JvmStatic fun bridgeRelayPlacedAndGetSignalDoesNotThrow(helper: GameTestHelper) {
        helper.setBlock(RELAY_POS, Registries.getBlock("bridge_relay")!!.defaultBlockState())
        val abs = helper.absolutePos(RELAY_POS)
        for (d in Direction.values()) {
            val sig = helper.getBlockState(RELAY_POS).getSignal(helper.getLevel(), abs, d)
            if (sig < 0) helper.fail("getSignal must be non-negative for $d")
        }
        helper.succeed()
    }

    @JvmStatic fun bridgeRelayGetInputPowerFromRedstoneWire(helper: GameTestHelper) {
        val wirePos = RELAY_POS.east()
        helper.setBlock(wirePos.below(), Blocks.STONE)
        helper.setBlock(wirePos, Blocks.REDSTONE_WIRE)
        helper.setBlock(RELAY_POS, Registries.getBlock("bridge_relay")!!.defaultBlockState())
        val sig = helper.getBlockState(RELAY_POS).getSignal(helper.getLevel(), helper.absolutePos(RELAY_POS), Direction.EAST)
        if (sig < 0) helper.fail("getSignal must be non-negative, got $sig")
        helper.succeed()
    }

    @JvmStatic fun bridgeRelayGetSignalFromLeftWireConnection(helper: GameTestHelper) {
        val leftPos = RELAY_POS.north()
        helper.setBlock(leftPos.below(), Blocks.STONE)
        helper.setBlock(leftPos, Blocks.REDSTONE_WIRE)
        helper.setBlock(RELAY_POS, Registries.getBlock("bridge_relay")!!.defaultBlockState())
        val sig = helper.getBlockState(RELAY_POS).getSignal(helper.getLevel(), helper.absolutePos(RELAY_POS), Direction.NORTH)
        if (sig < 0) helper.fail("getSignal must return non-negative value")
        helper.succeed()
    }

    private fun relayState(facing: Direction, rotation: Int): BlockState =
        Registries.getBlock("relay")!!.defaultBlockState()
            .setValue(CircuitComponents.DirectedComponentBlock.FACING, facing)
            .setValue(CircuitComponents.DirectedComponentBlock.ROTATION, rotation)

    private fun relayPlacementState(helper: GameTestHelper, surfacePos: BlockPos, clickedFace: Direction, clickOffset: Vec3): BlockState? {
        val surfaceAbs = helper.absolutePos(surfacePos)
        val placementAbs = surfaceAbs.relative(clickedFace)
        val clickLoc = Vec3.atCenterOf(placementAbs).add(clickOffset)
        val hit = BlockHitResult(clickLoc, clickedFace, surfaceAbs, false)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val relayItem = ItemStack(Registries.getItem("relay")!!)
        player.setItemInHand(InteractionHand.MAIN_HAND, relayItem)
        val uoc = UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, relayItem, hit)
        val bpc = BlockPlaceContext(uoc)
        return Registries.getBlock("relay")!!.getStateForPlacement(bpc)
    }

    private fun placePoweredInput(helper: GameTestHelper, relayBlock: Block) {
        helper.setBlock(INPUT_POS, Blocks.REDSTONE_BLOCK)
        helper.setBlock(RELAY_POS, relayBlock.defaultBlockState())
    }

    private fun assertRelayState(helper: GameTestHelper, relayBlock: Block, powered: Boolean, state: Int, message: String) {
        helper.assertBlockState(
            RELAY_POS,
            { it.`is`(relayBlock) && it.getValue(CircuitComponents.DirectedComponentBlock.POWERED) == powered && it.getValue(CircuitComponents.DirectedComponentBlock.STATE) == state },
            { message }
        )
    }
}
