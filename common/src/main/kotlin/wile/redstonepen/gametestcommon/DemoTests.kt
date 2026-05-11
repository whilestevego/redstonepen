package wile.redstonepen.gametestcommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.WallSignBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.RedstoneSide
import wile.redstonepen.blocks.CircuitComponents
import wile.redstonepen.blocks.track.RedstoneTrackDefs
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.commands.DemoBuilder
import wile.redstonepen.commands.DemoSections
import wile.redstonepen.registry.Registries

object DemoTests {
    private val CELL_LOCAL = BlockPos(1, 1, 1)

    @JvmStatic fun leverDrivesLamp(helper: GameTestHelper) {
        DemoSections.buildLeverDrivesLamp(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 2), Blocks.REDSTONE_WIRE)
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2))
        }
    }

    @JvmStatic fun buttonDrivesPiston(helper: GameTestHelper) {
        DemoSections.buildButtonDrivesPiston(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_button")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), Blocks.STICKY_PISTON)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.GLOWSTONE)
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 3))
        }
    }

    @JvmStatic fun invertedRelayNotGate(helper: GameTestHelper) {
        DemoSections.buildInvertedRelayNotGate(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER)
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "inverted_relay")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2))
        }
    }

    @JvmStatic fun bistableToggle(helper: GameTestHelper) {
        DemoSections.buildBistableToggle(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.STONE_BUTTON)
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "bistable_relay")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2))
        }
    }

    @JvmStatic fun pulseRelayMonostable(helper: GameTestHelper) {
        DemoSections.buildPulseRelayMonostable(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER)
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "pulse_relay")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2))
        }
    }

    @JvmStatic fun relayBuffer(helper: GameTestHelper) {
        DemoSections.buildRelayBuffer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER)
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "relay")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 2))
        }
    }

    @JvmStatic fun bridgeRelayCrossover(helper: GameTestHelper) {
        DemoSections.buildBridgeRelayCrossover(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 0, 3), "bridge_relay")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 5), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 1), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 4))
        }
    }

    @JvmStatic fun controlBoxAndGate(helper: GameTestHelper) {
        DemoSections.buildControlBoxAndGate(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3))
        }
    }

    @JvmStatic fun gaugeReadout(helper: GameTestHelper) {
        DemoSections.buildGaugeReadout(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), Blocks.REPEATER)
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), "basic_gauge")
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 4))
        }
    }

    @JvmStatic fun runAllSmoke(helper: GameTestHelper) {
        DemoSections.buildLeverDrivesLamp(helper.level, helper.absolutePos(CELL_LOCAL))
        DemoSections.buildGaugeReadout(helper.level, helper.absolutePos(BlockPos(7, 1, 1)))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever")
            assertModBlockAt(helper, BlockPos(7, 1, 1).offset(2, 0, 1), "basic_gauge")
        }
    }

    @JvmStatic fun penTrackWallClimb(helper: GameTestHelper) {
        DemoSections.buildPenTrackWallClimb(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 6), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 4), Blocks.STONE)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 1, 4), Blocks.STONE)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 2, 4), Blocks.STONE)
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 0, 5), "track")
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 1, 5), "track")
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 2, 5), "track")
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 4), "track")
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 3), "track")
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 2), "track")
            assertModBlockAt(helper, CELL_LOCAL.offset(4, 3, 1), "relay")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 3, 0), Blocks.REDSTONE_LAMP)
        }
    }

    @JvmStatic fun clearRegionFillsWithAir(helper: GameTestHelper) {
        val lo = CELL_LOCAL
        val hi = CELL_LOCAL.offset(2, 2, 2)
        helper.setBlock(lo, Blocks.STONE)
        helper.setBlock(hi, Blocks.STONE)
        DemoBuilder.clearRegion(helper.level, helper.absolutePos(lo), helper.absolutePos(hi))
        helper.succeedWhen {
            if (!helper.getBlockState(lo).isAir) helper.fail("clearRegion lo corner must be air")
            if (!helper.getBlockState(hi).isAir) helper.fail("clearRegion hi corner must be air")
        }
    }

    @JvmStatic fun placeWallSignPlacesSignBlock(helper: GameTestHelper) {
        val signPos = CELL_LOCAL
        DemoBuilder.placeWallSign(helper.level, helper.absolutePos(signPos), Direction.NORTH, "hello", "world")
        helper.succeedWhen {
            val state = helper.level.getBlockState(helper.absolutePos(signPos))
            if (state.block !is WallSignBlock)
                helper.fail("expected wall sign block at signPos, found ${state.block}")
        }
    }

    @JvmStatic fun leverDrivesLampLightsOnPull(helper: GameTestHelper) {
        DemoSections.buildLeverDrivesLamp(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { toggleLeverPowered(helper, CELL_LOCAL.offset(2, 0, 4), true) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun relayBufferLightsLampOnPull(helper: GameTestHelper) {
        DemoSections.buildRelayBuffer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { helper.pullLever(CELL_LOCAL.offset(2, 0, 5)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun invertedRelayLampDarkensOnPull(helper: GameTestHelper) {
        DemoSections.buildInvertedRelayNotGate(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(4) { helper.pullLever(CELL_LOCAL.offset(2, 0, 5)) }
        helper.runAtTickTime(20) {
            helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 0), BlockStateProperties.LIT, false)
            helper.succeed()
        }
    }

    @JvmStatic fun bridgeRelayNorthSouthLineLights(helper: GameTestHelper) {
        DemoSections.buildBridgeRelayCrossover(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { helper.pullLever(CELL_LOCAL.offset(4, 0, 5)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(4, 0, 1), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun bridgeRelayEastWestLineLights(helper: GameTestHelper) {
        DemoSections.buildBridgeRelayCrossover(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { helper.pullLever(CELL_LOCAL.offset(3, 0, 3)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun pulseRelayProducesObservablePulse(helper: GameTestHelper) {
        DemoSections.buildPulseRelayMonostable(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(5) { helper.pullLever(CELL_LOCAL.offset(2, 0, 5)) }
        helper.runAtTickTime(6) { helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 3), CircuitComponents.DirectedComponentBlock.STATE, 1) }
        helper.runAtTickTime(15) {
            helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 3), CircuitComponents.DirectedComponentBlock.STATE, 0)
            helper.succeed()
        }
    }

    @JvmStatic fun bistableRelayTogglesStateOnButtonPress(helper: GameTestHelper) {
        DemoSections.buildBistableToggle(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) {
            val buttonAbs = helper.absolutePos(CELL_LOCAL.offset(2, 0, 5))
            val buttonState = helper.level.getBlockState(buttonAbs)
            helper.level.setBlock(buttonAbs, buttonState.setValue(BlockStateProperties.POWERED, true), 3)
            helper.level.updateNeighborsAt(buttonAbs, buttonState.block)
        }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 3), CircuitComponents.DirectedComponentBlock.STATE, 1) }
    }

    @JvmStatic fun buttonPressExtendsPiston(helper: GameTestHelper) {
        DemoSections.buildButtonDrivesPiston(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) {
            val buttonAbs = helper.absolutePos(CELL_LOCAL.offset(2, 0, 4))
            val buttonState = helper.level.getBlockState(buttonAbs)
            helper.level.setBlock(buttonAbs, buttonState.setValue(BlockStateProperties.POWERED, true), 3)
            helper.level.updateNeighborsAt(buttonAbs, buttonState.block)
        }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(2, 0, 1), BlockStateProperties.EXTENDED, true) }
    }

    @JvmStatic fun penTrackWallClimbLightsLamp(helper: GameTestHelper) {
        DemoSections.buildPenTrackWallClimb(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(4, 3, 0), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun penTrackWallClimbTracksArePoweredImmediately(helper: GameTestHelper) {
        DemoSections.buildPenTrackWallClimb(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            val trackPositions = arrayOf(
                CELL_LOCAL.offset(4, 0, 5), CELL_LOCAL.offset(4, 1, 5), CELL_LOCAL.offset(4, 2, 5),
                CELL_LOCAL.offset(4, 3, 4), CELL_LOCAL.offset(4, 3, 3), CELL_LOCAL.offset(4, 3, 2)
            )
            for (local in trackPositions) {
                val te = helper.level.getBlockEntity(helper.absolutePos(local))
                if (te !is TrackBlockEntity) { helper.fail("expected TrackBlockEntity at $local", local); return@succeedWhen }
                if (te.getWireFlags() == 0) { helper.fail("track at $local has no wire flags", local) }
                if ((te.getStateFlags() and RedstoneTrackDefs.STATE_FLAG_PWR_MASK) == 0L)
                    helper.fail("track at $local has no power (state=0x${te.getStateFlags().toString(16)})", local)
            }
        }
    }

    @JvmStatic fun gaugeReadoutPowerRisesOnPull(helper: GameTestHelper) {
        DemoSections.buildGaugeReadout(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { helper.pullLever(CELL_LOCAL.offset(2, 0, 5)) }
        helper.succeedWhen {
            val gaugePos = helper.absolutePos(CELL_LOCAL.offset(2, 0, 1))
            val gauge = helper.level.getBlockState(gaugePos)
            val power = gauge.getValue(BlockStateProperties.POWER)
            if (power < 1)
                helper.fail("expected basic_gauge POWER >= 1 after lever pull, got $power", CELL_LOCAL.offset(2, 0, 1))
        }
    }

    @JvmStatic fun controlBoxAndOneInputKeepsLampDark(helper: GameTestHelper) {
        DemoSections.buildControlBoxAndGate(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(4) { helper.pullLever(CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAtTickTime(50) {
            helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false)
            helper.succeed()
        }
    }

    @JvmStatic fun controlBoxAndBothInputsLightLamp(helper: GameTestHelper) {
        DemoSections.buildControlBoxAndGate(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(4) {
            helper.pullLever(CELL_LOCAL.offset(3, 0, 5))
            helper.pullLever(CELL_LOCAL.offset(1, 0, 3))
        }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun runCircuitsBuildsAllContraptions(helper: GameTestHelper) {
        DemoSections.runCircuits(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeed()
    }

    @JvmStatic fun trafficLightPlacesBlocksCorrectly(helper: GameTestHelper) {
        DemoSections.buildTrafficLight(helper.level, helper.absolutePos(CELL_LOCAL))
        DemoSections.refreshWireConnections(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(3, 0, 2))
            assertWireConnected(helper, CELL_LOCAL.offset(2, 0, 3))
            assertWireConnected(helper, CELL_LOCAL.offset(3, 0, 4))
        }
    }

    @JvmStatic fun pulseCounterPlacesBlocksCorrectly(helper: GameTestHelper) {
        DemoSections.buildPulseCounter(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3))
        }
    }

    @JvmStatic fun srLatchPlacesBlocksCorrectly(helper: GameTestHelper) {
        DemoSections.buildSrLatch(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.STONE_BUTTON)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3))
        }
    }

    @JvmStatic fun pwmDemoPlacesBlocksCorrectly(helper: GameTestHelper) {
        DemoSections.buildPwmDemo(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3))
        }
    }

    @JvmStatic fun stepSequencerPlacesBlocksCorrectly(helper: GameTestHelper) {
        DemoSections.buildStepSequencer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 1), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3))
        }
    }

    @JvmStatic fun trafficLightExactlyOneLampLit(helper: GameTestHelper) {
        DemoSections.buildTrafficLight(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            val rLit = helper.getBlockState(CELL_LOCAL.offset(3, 0, 1)).getValue(BlockStateProperties.LIT)
            val yLit = helper.getBlockState(CELL_LOCAL.offset(3, 0, 5)).getValue(BlockStateProperties.LIT)
            val gLit = helper.getBlockState(CELL_LOCAL.offset(1, 0, 3)).getValue(BlockStateProperties.LIT)
            val litCount = (if (rLit) 1 else 0) + (if (yLit) 1 else 0) + (if (gLit) 1 else 0)
            if (litCount != 1) helper.fail("expected exactly one traffic lamp lit, got $litCount")
        }
    }

    @JvmStatic fun pulseCounterFirstPressLightsNorthLamp(helper: GameTestHelper) {
        DemoSections.buildPulseCounter(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun pulseCounterSecondPressFillsEastLamp(helper: GameTestHelper) {
        DemoSections.buildPulseCounter(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2)  { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(7)  { releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(15) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.succeedWhen {
            helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true)
            helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true)
        }
    }

    @JvmStatic fun pulseCounterFourthPressWrapsToZero(helper: GameTestHelper) {
        DemoSections.buildPulseCounter(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2)  { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(7)  { releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(15) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(20) { releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(30) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(35) { releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(45) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(50) { releaseButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(70) {
            helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, false)
            helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false)
            helper.assertBlockProperty(CELL_LOCAL.offset(1, 0, 3), BlockStateProperties.LIT, false)
            helper.succeed()
        }
    }

    @JvmStatic fun srLatchSetsAndHolds(helper: GameTestHelper) {
        DemoSections.buildSrLatch(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { pressButton(helper, CELL_LOCAL.offset(3, 0, 1)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun srLatchResetsAfterSet(helper: GameTestHelper) {
        DemoSections.buildSrLatch(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2)  { pressButton(helper, CELL_LOCAL.offset(3, 0, 1)) }
        helper.runAfterDelay(15) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(30) {
            helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false)
            helper.succeed()
        }
    }

    @JvmStatic fun pwmZeroDutyLampStaysDark(helper: GameTestHelper) {
        DemoSections.buildPwmDemo(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(20) {
            helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false)
            helper.succeed()
        }
    }

    @JvmStatic fun pwmFullDutyLightsLamp(helper: GameTestHelper) {
        DemoSections.buildPwmDemo(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { helper.pullLever(CELL_LOCAL.offset(1, 0, 3)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun stepSequencerStartsAtStepZero(helper: GameTestHelper) {
        DemoSections.buildStepSequencer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true)
            helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, false)
            helper.assertBlockProperty(CELL_LOCAL.offset(1, 0, 3), BlockStateProperties.LIT, false)
        }
    }

    @JvmStatic fun stepSequencerAdvancesToStepOne(helper: GameTestHelper) {
        DemoSections.buildStepSequencer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun stepSequencerWrapsAroundToStepZero(helper: GameTestHelper) {
        DemoSections.buildStepSequencer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2)  { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(15) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.runAfterDelay(30) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(3, 0, 1), BlockStateProperties.LIT, true) }
    }

    @JvmStatic fun holdTimerPlacesBlocksCorrectly(helper: GameTestHelper) {
        DemoSections.buildHoldTimer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.succeedWhen {
            assertModBlockAt(helper, CELL_LOCAL.offset(3, 0, 3), "control_box")
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 0, 5), Blocks.STONE_BUTTON)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER)
            assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP)
            assertWireConnected(helper, CELL_LOCAL.offset(4, 0, 3))
        }
    }

    @JvmStatic fun holdTimerLightsLampAfterPress(helper: GameTestHelper) {
        DemoSections.buildHoldTimer(helper.level, helper.absolutePos(CELL_LOCAL))
        helper.runAfterDelay(2) { helper.pullLever(CELL_LOCAL.offset(1, 0, 3)) }
        helper.runAfterDelay(5) { pressButton(helper, CELL_LOCAL.offset(3, 0, 5)) }
        helper.succeedWhen { helper.assertBlockProperty(CELL_LOCAL.offset(6, 0, 3), BlockStateProperties.LIT, true) }
    }

    private fun toggleLeverPowered(helper: GameTestHelper, localPos: BlockPos, powered: Boolean) {
        val abs = helper.absolutePos(localPos)
        val state = helper.level.getBlockState(abs)
        helper.level.setBlock(abs, state.setValue(BlockStateProperties.POWERED, powered), 3)
        helper.level.updateNeighborsAt(abs, state.block)
    }

    private fun pressButton(helper: GameTestHelper, localPos: BlockPos) {
        val abs = helper.absolutePos(localPos)
        val state = helper.level.getBlockState(abs)
        helper.level.setBlock(abs, state.setValue(BlockStateProperties.POWERED, true), 3)
        helper.level.updateNeighborsAt(abs, state.block)
    }

    private fun releaseButton(helper: GameTestHelper, localPos: BlockPos) {
        val abs = helper.absolutePos(localPos)
        val state = helper.level.getBlockState(abs)
        if (state.hasProperty(BlockStateProperties.POWERED) && state.getValue(BlockStateProperties.POWERED)) {
            helper.level.setBlock(abs, state.setValue(BlockStateProperties.POWERED, false), 3)
            helper.level.updateNeighborsAt(abs, state.block)
        }
    }

    private fun assertModBlockAt(helper: GameTestHelper, localPos: BlockPos, modBlockName: String) {
        val abs = helper.absolutePos(localPos)
        val actual = helper.level.getBlockState(abs)
        if (actual.block != Registries.getBlock(modBlockName)!!)
            helper.fail("expected mod block $modBlockName at $localPos but found ${actual.block}", localPos)
    }

    private fun assertVanillaBlockAt(helper: GameTestHelper, localPos: BlockPos, expected: Block) {
        val abs = helper.absolutePos(localPos)
        val actual = helper.level.getBlockState(abs)
        if (actual.block != expected)
            helper.fail("expected $expected at $localPos but found ${actual.block}", localPos)
    }

    private fun assertWireConnected(helper: GameTestHelper, local: BlockPos) {
        val state = helper.getBlockState(local)
        val connected = state.getValue(BlockStateProperties.NORTH_REDSTONE) != RedstoneSide.NONE
            || state.getValue(BlockStateProperties.SOUTH_REDSTONE) != RedstoneSide.NONE
            || state.getValue(BlockStateProperties.EAST_REDSTONE) != RedstoneSide.NONE
            || state.getValue(BlockStateProperties.WEST_REDSTONE) != RedstoneSide.NONE
        if (!connected) helper.fail("redstone wire at $local has no connections (dot state)", local)
    }
}
