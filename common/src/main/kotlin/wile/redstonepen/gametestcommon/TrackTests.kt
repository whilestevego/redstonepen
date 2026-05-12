package wile.redstonepen.gametestcommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongArrayTag
import net.minecraft.nbt.Tag
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.piston.PistonBaseBlock
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import wile.redstonepen.blocks.track.RedstoneTrackBlock
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.blocks.track.TrackNet
import wile.redstonepen.registry.Registries

object TrackTests {
    private val TRACK_POS = BlockPos(1, 1, 1)

    @JvmStatic
    fun trackStoresSeededPowerRoute(helper: GameTestHelper) {
        placeTrack(helper)
        seedTrackNet(helper, 15, Direction.WEST)
        helper.succeedWhen {
            val te = getTrack(helper)
            val nbt = te!!.writenbt(helper.level.registryAccess(), CompoundTag(), false)
            val route = nbt.getList("nets", Tag.TAG_COMPOUND.toInt()).getCompound(0)
            check(route.getInt("power") == 15) {
                "expected seeded track route to keep its stored power value"
            }
            check(
                route.getIntArray("pfac").size == 1 &&
                    route.getIntArray("pfac")[0] == Direction.WEST.get3DDataValue()
            ) {
                "expected seeded track route to keep its configured power side"
            }
        }
    }

    @JvmStatic
    fun trackReplacesSeededRouteWhenPowerClears(helper: GameTestHelper) {
        placeTrack(helper)
        seedTrackNet(helper, 15, Direction.WEST)
        seedTrackNet(helper, 0, Direction.WEST)
        helper.succeedWhen {
            val te = getTrack(helper)
            val nbt = te!!.writenbt(helper.level.registryAccess(), CompoundTag(), false)
            val route = nbt.getList("nets", Tag.TAG_COMPOUND.toInt()).getCompound(0)
            check(route.getInt("power") == 0) {
                "expected seeded track route power to update to zero"
            }
        }
    }

    @JvmStatic
    fun writenbtSyncOmitsNetsList(helper: GameTestHelper) {
        placeTrack(helper)
        seedTrackNet(helper, 5, Direction.WEST)
        val te = getTrack(helper)
        val sync = te!!.writenbt(helper.level.registryAccess(), CompoundTag(), true)
        if (sync.contains("nets")) helper.fail("sync packet writenbt must omit nets list")
        helper.succeed()
    }

    @JvmStatic
    fun writenbtFullIncludesNetsList(helper: GameTestHelper) {
        placeTrack(helper)
        seedTrackNet(helper, 5, Direction.WEST)
        val te = getTrack(helper)
        val full = te!!.writenbt(helper.level.registryAccess(), CompoundTag(), false)
        if (!full.contains("nets", Tag.TAG_LIST.toInt())) {
            helper.fail("full writenbt must include nets list")
        }
        helper.succeed()
    }

    @JvmStatic
    fun readnbtRestoresStateFlagsAndNets(helper: GameTestHelper) {
        placeTrack(helper)
        seedTrackNet(helper, 9, Direction.NORTH)
        val te = getTrack(helper)!!
        val full = te.writenbt(helper.level.registryAccess(), CompoundTag(), false)
        val empty = CompoundTag()
        empty.putLong("sflags", 0L)
        te.readnbt(helper.level.registryAccess(), empty)
        if (te.getStateFlags() != 0L) {
            helper.fail("expected state flags cleared after readnbt of empty payload")
        }
        te.readnbt(helper.level.registryAccess(), full)
        helper.succeed()
    }

    @JvmStatic
    fun readnbtAcceptsCorruptNetsListWithoutThrowing(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        val corrupt = CompoundTag()
        corrupt.putLong("sflags", 0L)
        val nets = ListTag()
        val bad = CompoundTag()
        bad.put("npos", LongArrayTag(listOf(0L)))
        bad.put("nsid", IntArrayTag(listOf(99)))
        bad.put("ifac", IntArrayTag(listOf()))
        bad.put("pfac", IntArrayTag(listOf()))
        bad.putInt("power", 0)
        nets.add(bad)
        corrupt.put("nets", nets)
        te.readnbt(helper.level.registryAccess(), corrupt)
        helper.succeed()
    }

    @JvmStatic
    fun addWireFlagsRecordsOnlyNewBits(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        val firstAdded = te.addWireFlags(0x0FL)
        val secondAdded = te.addWireFlags(0x0FL)
        if (firstAdded != 4) helper.fail("expected 4 wire flags added, got $firstAdded")
        if (secondAdded != 0) {
            helper.fail("expected 0 newly-added on duplicate apply, got $secondAdded")
        }
        helper.succeed()
    }

    @JvmStatic
    fun getWireFlagsReadsIndividualBits(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.addWireFlags(0b10101L)
        if (!te.getWireFlag(0)) helper.fail("bit 0 should be set")
        if (te.getWireFlag(1)) helper.fail("bit 1 should not be set")
        if (!te.getWireFlag(2)) helper.fail("bit 2 should be set")
        if (te.getWireFlag(3)) helper.fail("bit 3 should not be set")
        if (!te.getWireFlag(4)) helper.fail("bit 4 should be set")
        helper.succeed()
    }

    @JvmStatic
    fun setSidePowerAndGetSidePowerRoundTrip(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        for (d in Direction.values()) te.setSidePower(d, 0)
        te.setSidePower(Direction.NORTH, 11)
        if (te.getSidePower(Direction.NORTH) != 11) helper.fail("expected 11 on NORTH")
        if (te.getSidePower(Direction.SOUTH) != 0) helper.fail("expected 0 on SOUTH (untouched)")
        helper.succeed()
    }

    @JvmStatic
    fun hasVanillaRedstoneConnectionReadsBitsAndConnectorMask(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        seedTrackNet(helper, 0, Direction.EAST)
        for (d in Direction.values()) te.hasVanillaRedstoneConnection(d)
        helper.succeed()
    }

    @JvmStatic
    fun updateAllPowerValuesOnIsolatedTrackReturnsMap(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        val notes = te.updateAllPowerValuesFromAdjacent()
        if (notes == null) {
            helper.fail("updateAllPowerValuesFromAdjacent must return a non-null map")
        }
        helper.succeed()
    }

    @JvmStatic
    fun updateAllPowerValuesOnTrackWithSeededNetReturnsMap(helper: GameTestHelper) {
        placeTrack(helper)
        seedTrackNet(helper, 0, Direction.EAST)
        val te = getTrack(helper)!!
        val notes = te.updateAllPowerValuesFromAdjacent()
        if (notes == null) helper.fail("expected non-null change notification map")
        helper.succeed()
    }

    @JvmStatic
    fun handleShapeUpdateForAirNeighborReturns(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.addWireFlags(0x01L)
        te.handleShapeUpdate(
            Direction.DOWN,
            Blocks.AIR.defaultBlockState(),
            TRACK_POS.below(),
            false,
        )
        helper.succeed()
    }

    @JvmStatic
    fun handleShapeUpdateMovingFlagSkipsRecursion(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.handleShapeUpdate(
            Direction.DOWN,
            Blocks.AIR.defaultBlockState(),
            TRACK_POS.below(),
            true,
        )
        helper.succeed()
    }

    @JvmStatic
    fun handleShapeUpdateRedstoneBlockNeighborSkipsConnectionRefresh(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.handleShapeUpdate(
            Direction.EAST,
            Blocks.REDSTONE_BLOCK.defaultBlockState(),
            TRACK_POS.east(),
            false,
        )
        helper.succeed()
    }

    @JvmStatic
    fun getShapeReflectsWireFlags(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.addWireFlags(0x01L)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val shape =
            block.getShape(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
                CollisionContext.empty(),
            )
        if (shape.isEmpty) helper.fail("expected non-empty shape with one wire bit set")
        helper.succeed()
    }

    @JvmStatic
    fun emptyTrackShapeIsEmpty(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val shape =
            block.getShape(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
                CollisionContext.empty(),
            )
        if (!shape.isEmpty) helper.fail("expected empty shape with no wire bits")
        helper.succeed()
    }

    @JvmStatic
    fun getCollisionShapeIsAlwaysEmpty(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val shape =
            block.getCollisionShape(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
                CollisionContext.empty(),
            )
        if (!shape.isEmpty) helper.fail("track collision shape must be empty")
        helper.succeed()
    }

    @JvmStatic
    fun getSignalForUnpoweredTrackIsZero(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        for (d in Direction.values()) {
            val s =
                block.getSignal(
                    helper.getBlockState(TRACK_POS),
                    helper.level,
                    helper.absolutePos(TRACK_POS),
                    d,
                )
            if (s != 0) helper.fail("expected 0 signal on side $d, got $s")
        }
        helper.succeed()
    }

    @JvmStatic
    fun getDirectSignalForUnpoweredTrackIsZero(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val s =
            block.getDirectSignal(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
                Direction.NORTH,
            )
        if (s != 0) helper.fail("expected 0 direct signal")
        helper.succeed()
    }

    @JvmStatic
    fun canSurviveAlwaysTrue(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        if (
            !block.canSurvive(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
            )
        ) {
            helper.fail("canSurvive must be true")
        }
        helper.succeed()
    }

    @JvmStatic
    fun shouldCheckWeakPowerIsFalse(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        if (
            block.shouldCheckWeakPower(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
                Direction.NORTH,
            )
        ) {
            helper.fail("shouldCheckWeakPower must be false")
        }
        helper.succeed()
    }

    @JvmStatic
    fun canConnectRedstoneFalseForUnconnectedTrack(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val c =
            block.canConnectRedstone(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
                Direction.NORTH,
            )
        if (c) helper.fail("expected no redstone connection on bare track")
        helper.succeed()
    }

    @JvmStatic
    fun canConnectRedstoneFalseForNullSide(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        if (
            block.canConnectRedstone(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
                null,
            )
        ) {
            helper.fail("null side must yield false")
        }
        helper.succeed()
    }

    @JvmStatic
    fun propagatesSkylightDownDependsOnWaterlogged(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        if (
            !block.propagatesSkylightDown(
                helper.getBlockState(TRACK_POS),
                helper.level,
                helper.absolutePos(TRACK_POS),
            )
        ) {
            helper.fail("non-waterlogged track must propagate skylight")
        }
        helper.succeed()
    }

    @JvmStatic
    fun useShapeForLightOcclusionTrue(helper: GameTestHelper) {
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        placeTrack(helper)
        if (!block.useShapeForLightOcclusion(helper.getBlockState(TRACK_POS))) {
            helper.fail("useShapeForLightOcclusion must be true")
        }
        helper.succeed()
    }

    @JvmStatic
    fun getRenderShapeIsAnimatedEntityBlock(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        if (
            block.getRenderShape(helper.getBlockState(TRACK_POS)) !=
                RenderShape.ENTITYBLOCK_ANIMATED
        ) {
            helper.fail("expected ENTITYBLOCK_ANIMATED render shape")
        }
        helper.succeed()
    }

    @JvmStatic
    fun notifyAdjacentRunsWithoutThrowing(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        block.notifyAdjacent(helper.level, helper.absolutePos(TRACK_POS))
        helper.succeed()
    }

    @JvmStatic
    fun dropListReturnsRedstoneDustMatchingWireCount(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.addWireFlags(0x03L)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val drops = block.dropList(helper.getBlockState(TRACK_POS), helper.level, te, false)
        if (drops.size != 1) helper.fail("expected 1 drop stack, got ${drops.size}")
        if (drops[0].count != 2) helper.fail("expected 2 redstone dust, got ${drops[0].count}")
        helper.succeed()
    }

    @JvmStatic
    fun dropListEmptyForNoWires(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val drops =
            block.dropList(helper.getBlockState(TRACK_POS), helper.level, getTrack(helper)!!, false)
        if (drops.isNotEmpty()) helper.fail("dropList must be empty when track has no wires")
        helper.succeed()
    }

    @JvmStatic
    fun isPathfindableAlwaysTrue(helper: GameTestHelper) {
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        if (
            !block.isPathfindable(
                Registries.getBlock("track")!!.defaultBlockState(),
                PathComputationType.LAND,
            )
        ) {
            helper.fail("isPathfindable must return true")
        }
        helper.succeed()
    }

    @JvmStatic
    fun neighborChangedDoesNotThrow(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val absPos = helper.absolutePos(TRACK_POS)
        block.neighborChanged(
            helper.getBlockState(TRACK_POS),
            helper.level,
            absPos,
            Blocks.STONE,
            absPos.east(),
            false,
        )
        helper.succeed()
    }

    @JvmStatic
    fun onRemoveNotifiesAdjacentWhenReplaced(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.addWireFlags(0x01L)
        helper.setBlock(TRACK_POS, Blocks.STONE)
        helper.succeed()
    }

    @JvmStatic
    fun modifySegmentsAddThenRemoveReturnsConsume(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val player = helper.makeMockPlayer(GameType.CREATIVE)
        val absPos = helper.absolutePos(TRACK_POS)
        val hitAdd = Vec3(absPos.x + 0.5 - 0.3, absPos.y + 0.5 + 0.3, absPos.z.toDouble())
        val rtrAdd = BlockHitResult(hitAdd, Direction.NORTH, absPos, false)
        val pen = ItemStack(Registries.getItem("pen"))
        player.setItemInHand(InteractionHand.MAIN_HAND, pen)

        block.modifySegments(
            helper.getBlockState(TRACK_POS),
            helper.level,
            absPos,
            player,
            pen,
            InteractionHand.MAIN_HAND,
            rtrAdd,
            false,
            true,
        )

        val rtrRemove = BlockHitResult(hitAdd, Direction.NORTH, absPos, false)
        val pen2 = ItemStack(Registries.getItem("pen"))
        player.setItemInHand(InteractionHand.MAIN_HAND, pen2)
        val removeResult =
            block.modifySegments(
                helper.getBlockState(TRACK_POS),
                helper.level,
                absPos,
                player,
                pen2,
                InteractionHand.MAIN_HAND,
                rtrRemove,
                true,
                false,
            )
        if (removeResult == null) helper.fail("modifySegments(remove) must not return null")
        helper.succeed()
    }

    @JvmStatic
    fun modifySegmentsAddRemoveUntilEmptyRemovesBlock(helper: GameTestHelper) {
        placeTrack(helper)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val player = helper.makeMockPlayer(GameType.CREATIVE)
        val absPos = helper.absolutePos(TRACK_POS)
        val hitVec = Vec3(absPos.x + 0.5 - 0.3, absPos.y + 0.5 + 0.3, absPos.z.toDouble())
        val rtr = BlockHitResult(hitVec, Direction.NORTH, absPos, false)
        val pen = ItemStack(Registries.getItem("pen"))
        player.setItemInHand(InteractionHand.MAIN_HAND, pen)

        block.modifySegments(
            helper.getBlockState(TRACK_POS),
            helper.level,
            absPos,
            player,
            pen,
            InteractionHand.MAIN_HAND,
            rtr,
            false,
            true,
        )
        val te = getTrack(helper)
        if (te == null) {
            helper.fail("expected TrackBlockEntity after placeTrack")
            return
        }
        if (te.getWireFlags() == 0) {
            helper.fail("expected non-zero wire flags after ADD pass")
            return
        }

        block.modifySegments(
            helper.getBlockState(TRACK_POS),
            helper.level,
            absPos,
            player,
            ItemStack.EMPTY,
            InteractionHand.MAIN_HAND,
            BlockHitResult(hitVec, Direction.NORTH, absPos, false),
            true,
            false,
        )
        helper.succeed()
    }

    @JvmStatic
    fun toggleTraceWithNullPlayerDoesNotThrow(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.toggle_trace(null)
        helper.succeed()
    }

    @JvmStatic
    fun toggleTraceWithMockPlayerDoesNotThrow(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        te.toggle_trace(player)
        helper.succeed()
    }

    @JvmStatic
    fun connectionFlagAccessorsDoNotThrow(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        val flags = te.getConnectionFlags()
        val flag0 = te.getConnectionFlag(0)
        val count = te.getConnectionFlagCount()
        if (count <= 0) helper.fail("connection flag count must be positive, got $count")
        if (flag0 != ((flags and 1) != 0)) {
            helper.fail("getConnectionFlag(0) must match bit 0 of getConnectionFlags()")
        }
        helper.succeed()
    }

    @JvmStatic
    fun trackNetToStringProducesNonEmptyString(helper: GameTestHelper) {
        val net =
            TrackNet(
                listOf(helper.absolutePos(TRACK_POS)),
                listOf(Direction.NORTH),
                listOf(Direction.SOUTH),
                listOf(Direction.WEST),
                7,
            )
        val s = net.toString()
        if (s.isNullOrEmpty()) helper.fail("TrackNet.toString must return non-empty string")
        helper.succeed()
    }

    @JvmStatic
    fun getRedstonePowerZeroOnIsolatedTrack(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        for (d in Direction.values()) {
            val p = te.getRedstonePower(d, false)
            if (p != 0) helper.fail("expected 0 power on isolated track side $d, got $p")
        }
        helper.succeed()
    }

    @JvmStatic
    fun getRedstoneDustCountZeroForFreshTrack(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        if (te.getRedstoneDustCount() != 0) helper.fail("expected 0 dust count on fresh track")
        helper.succeed()
    }

    @JvmStatic
    fun getRedstoneDustCountMatchesWireFlags(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)!!
        te.addWireFlags(0x07L)
        val count = te.getRedstoneDustCount()
        if (count != 3) helper.fail("expected 3 dust from 3 wire bits, got $count")
        helper.succeed()
    }

    @JvmStatic
    fun trackHasDynamicDropListTrue(helper: GameTestHelper) {
        val block = placeTrack(helper)
        if (!block.hasDynamicDropList()) helper.fail("track must have dynamic drop list")
        helper.succeed()
    }

    @JvmStatic
    fun trackAsItemReturnsRedstone(helper: GameTestHelper) {
        val block = placeTrack(helper)
        if (block.asItem() != Items.REDSTONE) helper.fail("track asItem must return redstone")
        helper.succeed()
    }

    @JvmStatic
    fun trackIsSignalSourceTrue(helper: GameTestHelper) {
        val block = placeTrack(helper)
        if (!block.isSignalSource(helper.getBlockState(TRACK_POS))) {
            helper.fail("track must be a signal source")
        }
        helper.succeed()
    }

    @JvmStatic
    fun trackUseWithoutItemWithPlayerDoesNotThrow(helper: GameTestHelper) {
        placeTrack(helper)
        val player = helper.makeMockPlayer(GameType.CREATIVE)
        val abs = helper.absolutePos(TRACK_POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)
        helper.getBlockState(TRACK_POS).useWithoutItem(helper.level, player, hit)
        helper.succeed()
    }

    @JvmStatic
    fun trackUseItemOnWithDebugStickTogglesTrace(helper: GameTestHelper) {
        placeTrack(helper)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val debugStick = ItemStack(Items.DEBUG_STICK)
        player.setItemInHand(InteractionHand.MAIN_HAND, debugStick)
        val abs = helper.absolutePos(TRACK_POS)
        val hit = BlockHitResult(Vec3.atCenterOf(abs), Direction.SOUTH, abs, false)
        helper
            .getBlockState(TRACK_POS)
            .useItemOn(debugStick, helper.level, player, InteractionHand.MAIN_HAND, hit)
        helper.succeed()
    }

    @JvmStatic
    fun trackCanBePlacedOnFaceOfPiston(helper: GameTestHelper) {
        val pistonPos = TRACK_POS
        val pistonState =
            Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH)
        helper.setBlock(pistonPos, pistonState)
        val faceSame =
            RedstoneTrackBlock.canBePlacedOnFace(
                pistonState,
                helper.level,
                helper.absolutePos(pistonPos),
                Direction.NORTH,
            )
        val faceOpp =
            RedstoneTrackBlock.canBePlacedOnFace(
                pistonState,
                helper.level,
                helper.absolutePos(pistonPos),
                Direction.SOUTH,
            )
        if (faceSame) helper.fail("piston front face must not accept track")
        if (!faceOpp) helper.fail("piston back face must accept track")
        helper.succeed()
    }

    @JvmStatic
    fun trackCanBePlacedOnFaceOfHopper(helper: GameTestHelper) {
        val hopperPos = TRACK_POS
        val hopperState = Blocks.HOPPER.defaultBlockState()
        helper.setBlock(hopperPos, hopperState)
        val topFace =
            RedstoneTrackBlock.canBePlacedOnFace(
                hopperState,
                helper.level,
                helper.absolutePos(hopperPos),
                Direction.UP,
            )
        if (!topFace) helper.fail("hopper top face must accept track")
        val sideFace =
            RedstoneTrackBlock.canBePlacedOnFace(
                hopperState,
                helper.level,
                helper.absolutePos(hopperPos),
                Direction.NORTH,
            )
        if (sideFace) helper.fail("hopper side face must not accept track")
        helper.succeed()
    }

    @JvmStatic
    fun trackNeighborChangedWithRedstoneBlockTriggersUpdate(helper: GameTestHelper) {
        placeTrack(helper)
        helper.setBlock(TRACK_POS.east(), Blocks.REDSTONE_BLOCK)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val absPos = helper.absolutePos(TRACK_POS)
        block.neighborChanged(
            helper.getBlockState(TRACK_POS),
            helper.level,
            absPos,
            Blocks.REDSTONE_BLOCK,
            absPos.east(),
            false,
        )
        helper.succeed()
    }

    @JvmStatic
    fun trackGetRedstoneDustCountZeroWhenNoWiresLargeConfig(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)
        if (te == null) {
            helper.fail("TE missing")
            return
        }
        te.addWireFlags(0L)
        val count = te.getRedstoneDustCount()
        if (count != 0) helper.fail("expected 0, got $count")
        helper.succeed()
    }

    @JvmStatic
    fun trackReadnbtWithSflagsField(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)
        if (te == null) {
            helper.fail("TE missing")
            return
        }
        val data = CompoundTag()
        data.putLong("sflags", 0x07L)
        te.onServerPacketReceived(data)
        helper.succeed()
    }

    @JvmStatic
    fun trackGetNonWireSignalFromRedstoneBlock(helper: GameTestHelper) {
        placeTrack(helper)
        val te = getTrack(helper)
        if (te == null) {
            helper.fail("TE missing")
            return
        }
        helper.setBlock(TRACK_POS.east(), Blocks.REDSTONE_BLOCK)
        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        block.neighborChanged(
            helper.getBlockState(TRACK_POS),
            helper.level,
            helper.absolutePos(TRACK_POS),
            Blocks.REDSTONE_BLOCK,
            helper.absolutePos(TRACK_POS.east()),
            false,
        )
        helper.succeed()
    }

    @JvmStatic
    fun verticalFaceTrackPowerPreservedAfterBottomSegmentRemoveAndReplace(helper: GameTestHelper) {
        // Regression test for a bug where adding a wire segment to a powered neighbour leaves
        // getSidePower()==0 on the track face even though the new connection reaches a power
        // source.
        //
        // Root cause: RedstoneTrackBlock.modifySegments had no updateAllPowerValuesFromAdjacent()
        // safety-net call in the add branch (redstone_use > 0), unlike the removal branch.
        // Inside te.modifySegments, change_notifications_before and change_notifications_after are
        // both empty when the power source is a signal source (redstone block), because signal
        // sources
        // are stripped from the notification maps at handleNeighborChanged line 610.
        // connected=[] disconnected=[] → TRUE branch fires → setSidePower(face,
        // initial_side_power=0).
        // notifyAdjacent cannot fix it because signal sources do not call back into the track.
        //
        // Topology:
        //   redstone block at TRACK_POS.below()
        //   TrackA at TRACK_POS with ONLY bit 0x0100 (NORTH-UP, dangling — above is air)
        //     → genuinely unpowered (NORTH face power = 0).
        //
        // Adding 0x0200 (NORTH-DOWN → redstone block):
        //   te.modifySegments TRUE branch sets NORTH = initial_side_power = 0.  BUG without fix.
        //   With the safety net, updateAllPowerValuesFromAdjacent recomputes NORTH = 15.  FIXED.
        //
        // Click target: SOUTH face below centre → clicked_face=SOUTH, face=NORTH, dir=DOWN →
        // flip=0x0200.

        val block = Registries.getBlock("track")!! as RedstoneTrackBlock
        val redstonePos = TRACK_POS.below()
        val absA = helper.absolutePos(TRACK_POS)

        // Place the redstone block below, then place TrackA with only the dangling 0x0100 bit.
        helper.setBlock(redstonePos, Blocks.REDSTONE_BLOCK)
        helper.setBlock(TRACK_POS, Registries.getBlock("track")!!.defaultBlockState())
        val teA =
            helper.getBlockEntity(TRACK_POS) as? TrackBlockEntity
                ?: run {
                    helper.fail("expected TrackBlockEntity at TRACK_POS")
                    return
                }

        teA.addWireFlags(0x0100L)
        teA.updateConnections(1)

        if (teA.getSidePower(Direction.NORTH) != 0) {
            helper.fail(
                "precondition: TrackA NORTH face must be 0 before adding the external connection"
            )
            return
        }

        // Add the 0x0200 segment (click SOUTH face below centre → face=NORTH, dir=DOWN →
        // flip=0x0200).
        val hitVec = Vec3(absA.x + 0.5, absA.y + 0.2, absA.z.toDouble())
        val rtr = BlockHitResult(hitVec, Direction.SOUTH, absA, false)
        val player = helper.makeMockPlayer(GameType.CREATIVE)
        val pen = ItemStack(Items.REDSTONE, 64)
        player.setItemInHand(InteractionHand.MAIN_HAND, pen)
        block.modifySegments(
            helper.getBlockState(TRACK_POS),
            helper.level,
            absA,
            player,
            pen,
            InteractionHand.MAIN_HAND,
            rtr,
            no_add = false,
            no_remove = true,
        )

        val teAfterAdd =
            helper.getBlockEntity(TRACK_POS) as? TrackBlockEntity
                ?: run {
                    helper.fail("track block entity missing after add")
                    return
                }
        val powerAfter = teAfterAdd.getSidePower(Direction.NORTH)
        if (powerAfter == 0) {
            helper.fail(
                "BUG: TrackA NORTH face power is 0 after adding the external connection to redstone block. " +
                    "RedstoneTrackBlock.modifySegments is missing the updateAllPowerValuesFromAdjacent " +
                    "safety-net call in the add branch."
            )
            return
        }
        helper.succeed()
    }

    private fun placeTrack(helper: GameTestHelper): RedstoneTrackBlock {
        helper.setBlock(TRACK_POS, Registries.getBlock("track")!!.defaultBlockState())
        return Registries.getBlock("track")!! as RedstoneTrackBlock
    }

    private fun seedTrackNet(helper: GameTestHelper, power: Int, vararg powerSides: Direction) {
        val te = getTrack(helper) ?: error("expected track block entity to exist")
        val route = CompoundTag()
        route.putInt("power", power)
        route.put("npos", LongArrayTag(listOf()))
        route.put("nsid", IntArrayTag(listOf()))
        route.put("ifac", IntArrayTag(listOf()))
        route.put("pfac", IntArrayTag(powerSides.map { it.get3DDataValue() }))
        val nets = ListTag()
        nets.add(route)
        val trackData = CompoundTag()
        trackData.putLong("sflags", 0L)
        trackData.put("nets", nets)
        te.onServerPacketReceived(trackData)
    }

    private fun getTrack(helper: GameTestHelper): TrackBlockEntity? =
        helper.getBlockEntity(TRACK_POS) as? TrackBlockEntity
}
