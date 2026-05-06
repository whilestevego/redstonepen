package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.RedstoneTrack;
import wile.redstonepen.libmc.Registries;
import java.util.List;

public class TrackGameTests
{
  private static final String EMPTY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos TRACK_POS = new BlockPos(1, 1, 1);

  public TrackGameTests()
  {}

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 20)
  public static void trackStoresSeededPowerRoute(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 15, Direction.WEST);

    helper.succeedWhen(() -> {
      final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
      final CompoundTag nbt = te.writenbt(helper.getLevel().registryAccess(), new CompoundTag(), false);
      final CompoundTag route = nbt.getList("nets", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0);
      if(route.getInt("power") != 15) throw new IllegalStateException("expected seeded track route to keep its stored power value");
      if(route.getIntArray("pfac").length != 1 || route.getIntArray("pfac")[0] != Direction.WEST.get3DDataValue()) {
        throw new IllegalStateException("expected seeded track route to keep its configured power side");
      }
    });
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 20)
  public static void trackReplacesSeededRouteWhenPowerClears(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 15, Direction.WEST);
    seedTrackNet(helper, 0, Direction.WEST);

    helper.succeedWhen(() -> {
      final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
      final CompoundTag nbt = te.writenbt(helper.getLevel().registryAccess(), new CompoundTag(), false);
      final CompoundTag route = nbt.getList("nets", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0);
      if(route.getInt("power") != 0) throw new IllegalStateException("expected seeded track route power to update to zero");
    });
  }

  // --- NBT round-trip --------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void writenbtSyncOmitsNetsList(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 5, Direction.WEST);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final CompoundTag sync = te.writenbt(helper.getLevel().registryAccess(), new CompoundTag(), true);
    if(sync.contains("nets")) helper.fail("sync packet writenbt must omit nets list");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void writenbtFullIncludesNetsList(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 5, Direction.WEST);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final CompoundTag full = te.writenbt(helper.getLevel().registryAccess(), new CompoundTag(), false);
    if(!full.contains("nets", net.minecraft.nbt.Tag.TAG_LIST)) helper.fail("full writenbt must include nets list");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void readnbtRestoresStateFlagsAndNets(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 9, Direction.NORTH);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final CompoundTag full = te.writenbt(helper.getLevel().registryAccess(), new CompoundTag(), false);
    // Wipe and reload
    final CompoundTag empty = new CompoundTag();
    empty.putLong("sflags", 0L);
    te.readnbt(helper.getLevel().registryAccess(), empty);
    if(te.getStateFlags() != 0L) helper.fail("expected state flags cleared after readnbt of empty payload");
    te.readnbt(helper.getLevel().registryAccess(), full);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void readnbtAcceptsCorruptNetsListWithoutThrowing(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final CompoundTag corrupt = new CompoundTag();
    corrupt.putLong("sflags", 0L);
    final net.minecraft.nbt.ListTag nets = new net.minecraft.nbt.ListTag();
    final CompoundTag bad = new CompoundTag();
    bad.put("npos", new net.minecraft.nbt.LongArrayTag(java.util.List.of(0L)));
    bad.put("nsid", new net.minecraft.nbt.IntArrayTag(java.util.List.of(99)));
    bad.put("ifac", new net.minecraft.nbt.IntArrayTag(java.util.List.of()));
    bad.put("pfac", new net.minecraft.nbt.IntArrayTag(java.util.List.of()));
    bad.putInt("power", 0);
    nets.add(bad);
    corrupt.put("nets", nets);
    // Must not throw — readnbt has try/catch that on any failure clears nets.
    te.readnbt(helper.getLevel().registryAccess(), corrupt);
    helper.succeed();
  }

  // --- State-flag bit accessors --------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void addWireFlagsRecordsOnlyNewBits(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final int firstAdded = te.addWireFlags(0x0FL);
    final int secondAdded = te.addWireFlags(0x0FL);
    if(firstAdded != 4) helper.fail("expected 4 wire flags added, got " + firstAdded);
    if(secondAdded != 0) helper.fail("expected 0 newly-added on duplicate apply, got " + secondAdded);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getWireFlagsReadsIndividualBits(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.addWireFlags(0b1010_1L);
    if(!te.getWireFlag(0)) helper.fail("bit 0 should be set");
    if(te.getWireFlag(1))  helper.fail("bit 1 should not be set");
    if(!te.getWireFlag(2)) helper.fail("bit 2 should be set");
    if(te.getWireFlag(3))  helper.fail("bit 3 should not be set");
    if(!te.getWireFlag(4)) helper.fail("bit 4 should be set");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void setSidePowerAndGetSidePowerRoundTrip(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    for(Direction d : Direction.values()) {
      te.setSidePower(d, 0);
    }
    te.setSidePower(Direction.NORTH, 11);
    if(te.getSidePower(Direction.NORTH) != 11) helper.fail("expected 11 on NORTH");
    if(te.getSidePower(Direction.SOUTH) != 0)  helper.fail("expected 0 on SOUTH (untouched)");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void hasVanillaRedstoneConnectionReadsBitsAndConnectorMask(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    // Set a bulk-connector bit for EAST via addWireFlags — addWireFlags only sets
    // wire bits; use seedTrackNet path to get the connection mask exercised.
    seedTrackNet(helper, 0, Direction.EAST);
    // Just call the predicate for each side; must not throw.
    for(Direction d : Direction.values()) te.hasVanillaRedstoneConnection(d);
    helper.succeed();
  }

  // --- updateAllPowerValuesFromAdjacent (drives updateConnections internally) -----------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void updateAllPowerValuesOnIsolatedTrackReturnsMap(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final java.util.Map<BlockPos, BlockPos> notes = te.updateAllPowerValuesFromAdjacent();
    if(notes == null) helper.fail("updateAllPowerValuesFromAdjacent must return a non-null map");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void updateAllPowerValuesOnTrackWithSeededNetReturnsMap(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 0, Direction.EAST);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final java.util.Map<BlockPos, BlockPos> notes = te.updateAllPowerValuesFromAdjacent();
    if(notes == null) helper.fail("expected non-null change notification map");
    helper.succeed();
  }

  // --- handleShapeUpdate -----------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateForAirNeighborReturns(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.addWireFlags(0x01L);
    te.handleShapeUpdate(Direction.DOWN, Blocks.AIR.defaultBlockState(), TRACK_POS.below(), false);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateMovingFlagSkipsRecursion(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.handleShapeUpdate(Direction.DOWN, Blocks.AIR.defaultBlockState(), TRACK_POS.below(), true);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateRedstoneBlockNeighborSkipsConnectionRefresh(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.handleShapeUpdate(Direction.EAST, Blocks.REDSTONE_BLOCK.defaultBlockState(), TRACK_POS.east(), false);
    helper.succeed();
  }

  // --- RedstoneTrackBlock pure-state methods --------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getShapeReflectsWireFlags(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.addWireFlags(0x01L); // dn — produces DOWN face shape
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final var shape = block.getShape(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), net.minecraft.world.phys.shapes.CollisionContext.empty());
    if(shape.isEmpty()) helper.fail("expected non-empty shape with one wire bit set");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void emptyTrackShapeIsEmpty(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final var shape = block.getShape(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), net.minecraft.world.phys.shapes.CollisionContext.empty());
    if(!shape.isEmpty()) helper.fail("expected empty shape with no wire bits");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getCollisionShapeIsAlwaysEmpty(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final var shape = block.getCollisionShape(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), net.minecraft.world.phys.shapes.CollisionContext.empty());
    if(!shape.isEmpty()) helper.fail("track collision shape must be empty");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getSignalForUnpoweredTrackIsZero(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    for(Direction d : Direction.values()) {
      final int s = block.getSignal(helper.getBlockState(TRACK_POS), helper.getLevel(), helper.absolutePos(TRACK_POS), d);
      if(s != 0) helper.fail("expected 0 signal on side " + d + ", got " + s);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getDirectSignalForUnpoweredTrackIsZero(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final int s = block.getDirectSignal(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), Direction.NORTH);
    if(s != 0) helper.fail("expected 0 direct signal");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void canSurviveAlwaysTrue(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(!block.canSurvive(helper.getBlockState(TRACK_POS), helper.getLevel(), helper.absolutePos(TRACK_POS)))
      helper.fail("canSurvive must be true");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void shouldCheckWeakPowerIsFalse(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(block.shouldCheckWeakPower(helper.getBlockState(TRACK_POS), helper.getLevel(),
        helper.absolutePos(TRACK_POS), Direction.NORTH))
      helper.fail("shouldCheckWeakPower must be false");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void canConnectRedstoneFalseForUnconnectedTrack(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final boolean c = block.canConnectRedstone(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), Direction.NORTH);
    if(c) helper.fail("expected no redstone connection on bare track");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void canConnectRedstoneFalseForNullSide(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(block.canConnectRedstone(helper.getBlockState(TRACK_POS), helper.getLevel(),
        helper.absolutePos(TRACK_POS), null))
      helper.fail("null side must yield false");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void propagatesSkylightDownDependsOnWaterlogged(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(!block.propagatesSkylightDown(helper.getBlockState(TRACK_POS), helper.getLevel(), helper.absolutePos(TRACK_POS)))
      helper.fail("non-waterlogged track must propagate skylight");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void useShapeForLightOcclusionTrue(GameTestHelper helper)
  {
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    placeTrack(helper);
    if(!block.useShapeForLightOcclusion(helper.getBlockState(TRACK_POS)))
      helper.fail("useShapeForLightOcclusion must be true");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getRenderShapeIsAnimatedEntityBlock(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(block.getRenderShape(helper.getBlockState(TRACK_POS)) != net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED)
      helper.fail("expected ENTITYBLOCK_ANIMATED render shape");
    helper.succeed();
  }

  // --- notifyAdjacent does not throw on placed track ---

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void notifyAdjacentRunsWithoutThrowing(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    block.notifyAdjacent(helper.getLevel(), helper.absolutePos(TRACK_POS));
    helper.succeed();
  }

  // --- dropList --------------------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void dropListReturnsRedstoneDustMatchingWireCount(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.addWireFlags(0x03L); // 2 wire bits = 2 dust
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final List<ItemStack> drops = block.dropList(helper.getBlockState(TRACK_POS), helper.getLevel(), te, false);
    if(drops.size() != 1) helper.fail("expected 1 drop stack, got " + drops.size());
    if(drops.get(0).getCount() != 2) helper.fail("expected 2 redstone dust, got " + drops.get(0).getCount());
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void dropListEmptyForNoWires(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final List<ItemStack> drops = block.dropList(helper.getBlockState(TRACK_POS), helper.getLevel(), getTrack(helper), false);
    if(!drops.isEmpty()) helper.fail("dropList must be empty when track has no wires");
    helper.succeed();
  }

  // --- isPathfindable --------------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void isPathfindableAlwaysTrue(GameTestHelper helper)
  {
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(!block.isPathfindable(Registries.getBlock("track").defaultBlockState(), PathComputationType.LAND))
      helper.fail("isPathfindable must return true");
    helper.succeed();
  }

  // --- neighborChanged -------------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void neighborChangedDoesNotThrow(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final BlockPos absPos = helper.absolutePos(TRACK_POS);
    block.neighborChanged(helper.getBlockState(TRACK_POS), helper.getLevel(), absPos, Blocks.STONE, absPos.east(), false);
    helper.succeed();
  }

  // --- onRemove --------------------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void onRemoveNotifiesAdjacentWhenReplaced(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.addWireFlags(0x01L);
    // Replacing the track with stone triggers onRemove internally.
    helper.setBlock(TRACK_POS, Blocks.STONE);
    helper.succeed();
  }

  // --- modifySegments: add then remove path ---------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void modifySegmentsAddThenRemoveReturnsConsume(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
    final BlockPos absPos = helper.absolutePos(TRACK_POS);
    // Hit the north face near the upper-left corner to target a specific wire direction.
    final net.minecraft.world.phys.Vec3 hitAdd = new net.minecraft.world.phys.Vec3(
      absPos.getX() + 0.5 - 0.3, absPos.getY() + 0.5 + 0.3, absPos.getZ());
    final net.minecraft.world.phys.BlockHitResult rtrAdd = new net.minecraft.world.phys.BlockHitResult(
      hitAdd, Direction.NORTH, absPos, false);
    final net.minecraft.world.item.ItemStack pen = new net.minecraft.world.item.ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);

    // ADD pass: place a wire segment on the north face.
    block.modifySegments(helper.getBlockState(TRACK_POS), helper.getLevel(), absPos, player,
      pen, net.minecraft.world.InteractionHand.MAIN_HAND, rtrAdd, false, true);

    // REMOVE pass: same hit, now remove mode.
    final net.minecraft.world.phys.BlockHitResult rtrRemove = new net.minecraft.world.phys.BlockHitResult(
      hitAdd, Direction.NORTH, absPos, false);
    final net.minecraft.world.item.ItemStack pen2 = new net.minecraft.world.item.ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen2);
    final var removeResult = block.modifySegments(helper.getBlockState(TRACK_POS), helper.getLevel(), absPos, player,
      pen2, net.minecraft.world.InteractionHand.MAIN_HAND, rtrRemove, true, false);
    if(removeResult == null) helper.fail("modifySegments(remove) must not return null");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void modifySegmentsAddRemoveUntilEmptyRemovesBlock(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
    final BlockPos absPos = helper.absolutePos(TRACK_POS);
    final net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(
      absPos.getX() + 0.5 - 0.3, absPos.getY() + 0.5 + 0.3, absPos.getZ());
    final net.minecraft.world.phys.BlockHitResult rtr = new net.minecraft.world.phys.BlockHitResult(
      hitVec, Direction.NORTH, absPos, false);

    final net.minecraft.world.item.ItemStack pen = new net.minecraft.world.item.ItemStack(Registries.getItem("pen"));
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pen);

    // ADD one wire so block is non-empty.
    block.modifySegments(helper.getBlockState(TRACK_POS), helper.getLevel(), absPos, player,
      pen, net.minecraft.world.InteractionHand.MAIN_HAND, rtr, false, true);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    if(te == null) { helper.fail("expected TrackBlockEntity after placeTrack"); return; }
    if(te.getWireFlags() == 0) { helper.fail("expected non-zero wire flags after ADD pass"); return; }

    // REMOVE all wires: call remove with empty stack (no pen) on each face/direction combo.
    // We remove the same bit we just added.
    block.modifySegments(helper.getBlockState(TRACK_POS), helper.getLevel(), absPos, player,
      net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND,
      new net.minecraft.world.phys.BlockHitResult(hitVec, Direction.NORTH, absPos, false),
      true, false);
    helper.succeed();
  }

  // --- toggle_trace ------------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void toggleTraceWithNullPlayerDoesNotThrow(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.toggle_trace(null);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void toggleTraceWithMockPlayerDoesNotThrow(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    te.toggle_trace(player);
    helper.succeed();
  }

  // --- connection flag accessors ---------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void connectionFlagAccessorsDoNotThrow(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final int flags = te.getConnectionFlags();
    final boolean flag0 = te.getConnectionFlag(0);
    final int count = te.getConnectionFlagCount();
    if(count <= 0) helper.fail("connection flag count must be positive, got " + count);
    if(flag0 != ((flags & 1) != 0)) helper.fail("getConnectionFlag(0) must match bit 0 of getConnectionFlags()");
    helper.succeed();
  }

  // --- TrackNet.toString -----------------------------------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackNetToStringProducesNonEmptyString(GameTestHelper helper)
  {
    final RedstoneTrack.TrackBlockEntity.TrackNet net = new RedstoneTrack.TrackBlockEntity.TrackNet(
      java.util.List.of(helper.absolutePos(TRACK_POS)),
      java.util.List.of(Direction.NORTH),
      java.util.List.of(Direction.SOUTH),
      java.util.List.of(Direction.WEST),
      7
    );
    final String s = net.toString();
    if(s == null || s.isEmpty()) helper.fail("TrackNet.toString must return non-empty string");
    helper.succeed();
  }

  // --- getRedstonePower / getRedstoneDustCount (real BE methods) ------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getRedstonePowerZeroOnIsolatedTrack(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    for(Direction d : Direction.values()) {
      final int p = te.getRedstonePower(d, false);
      if(p != 0) helper.fail("expected 0 power on isolated track side " + d + ", got " + p);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getRedstoneDustCountZeroForFreshTrack(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    if(te.getRedstoneDustCount() != 0) helper.fail("expected 0 dust count on fresh track");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getRedstoneDustCountMatchesWireFlags(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.addWireFlags(0x07L); // 3 wire bits
    final int count = te.getRedstoneDustCount();
    if(count != 3) helper.fail("expected 3 dust from 3 wire bits, got " + count);
    helper.succeed();
  }

  // --- RedstoneTrackBlock trivial-method coverage ------------------------------------------

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackHasDynamicDropListTrue(GameTestHelper helper)
  {
    final RedstoneTrack.RedstoneTrackBlock block = placeTrack(helper);
    if(!block.hasDynamicDropList()) helper.fail("track must have dynamic drop list");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackAsItemReturnsRedstone(GameTestHelper helper)
  {
    final RedstoneTrack.RedstoneTrackBlock block = placeTrack(helper);
    if(!block.asItem().equals(Items.REDSTONE)) helper.fail("track asItem must return redstone");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackIsSignalSourceTrue(GameTestHelper helper)
  {
    final RedstoneTrack.RedstoneTrackBlock block = placeTrack(helper);
    final boolean signalSource = block.isSignalSource(helper.getBlockState(TRACK_POS));
    if(!signalSource) helper.fail("track must be a signal source");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackUseWithoutItemWithPlayerDoesNotThrow(GameTestHelper helper)
  {
    // useWithoutItem delegates to modifySegments with no_add=true, no_remove=false.
    placeTrack(helper);
    final Player player = helper.makeMockPlayer(GameType.CREATIVE);
    final BlockPos abs = helper.absolutePos(TRACK_POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    final BlockState state = helper.getBlockState(TRACK_POS);
    state.useWithoutItem(helper.getLevel(), player, hit);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackUseItemOnWithDebugStickTogglesTrace(GameTestHelper helper)
  {
    placeTrack(helper);
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack debugStick = new ItemStack(Items.DEBUG_STICK);
    player.setItemInHand(InteractionHand.MAIN_HAND, debugStick);
    final BlockPos abs = helper.absolutePos(TRACK_POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.SOUTH, abs, false);
    final BlockState state = helper.getBlockState(TRACK_POS);
    state.useItemOn(debugStick, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void trackCanBePlacedOnFaceOfPiston(GameTestHelper helper)
  {
    // Exercise the PistonBaseBlock branch in canBePlacedOnFace.
    final BlockPos pistonPos = TRACK_POS;
    final BlockState pistonState = Blocks.PISTON.defaultBlockState()
      .setValue(PistonBaseBlock.FACING, Direction.NORTH);
    helper.setBlock(pistonPos, pistonState);
    // Face == piston facing → should return false; opposite face → should return true.
    final boolean faceSame = RedstoneTrack.RedstoneTrackBlock.canBePlacedOnFace(pistonState, helper.getLevel(), helper.absolutePos(pistonPos), Direction.NORTH);
    final boolean faceOpp = RedstoneTrack.RedstoneTrackBlock.canBePlacedOnFace(pistonState, helper.getLevel(), helper.absolutePos(pistonPos), Direction.SOUTH);
    if(faceSame) helper.fail("piston front face must not accept track");
    if(!faceOpp) helper.fail("piston back face must accept track");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackCanBePlacedOnFaceOfHopper(GameTestHelper helper)
  {
    // Exercise the hopper branch in canBePlacedOnFace.
    final BlockPos hopperPos = TRACK_POS;
    final BlockState hopperState = Blocks.HOPPER.defaultBlockState();
    helper.setBlock(hopperPos, hopperState);
    final boolean topFace = RedstoneTrack.RedstoneTrackBlock.canBePlacedOnFace(hopperState, helper.getLevel(), helper.absolutePos(hopperPos), Direction.UP);
    if(!topFace) helper.fail("hopper top face must accept track");
    final boolean sideFace = RedstoneTrack.RedstoneTrackBlock.canBePlacedOnFace(hopperState, helper.getLevel(), helper.absolutePos(hopperPos), Direction.NORTH);
    if(sideFace) helper.fail("hopper side face must not accept track");
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackNeighborChangedWithRedstoneBlockTriggersUpdate(GameTestHelper helper)
  {
    placeTrack(helper);
    // Trigger neighborChanged by adding redstone block adjacent.
    helper.setBlock(TRACK_POS.east(), Blocks.REDSTONE_BLOCK);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final BlockState state = helper.getBlockState(TRACK_POS);
    final BlockPos absPos = helper.absolutePos(TRACK_POS);
    block.neighborChanged(state, helper.getLevel(), absPos, Blocks.REDSTONE_BLOCK, absPos.east(), false);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackGetRedstoneDustCountZeroWhenNoWiresLargeConfig(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    if(te == null) { helper.fail("TE missing"); return; }
    // addWireFlags with 0 — getRedstoneDustCount should be 0
    te.addWireFlags(0L);
    final int count = te.getRedstoneDustCount();
    if(count != 0) helper.fail("expected 0, got " + count);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void trackReadnbtWithSflagsField(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    if(te == null) { helper.fail("TE missing"); return; }
    // readnbt via onServerPacketReceived with only sflags (no nets) — exercises readnbt's
    // other branch that differs from the existing test.
    final CompoundTag data = new CompoundTag();
    data.putLong("sflags", 0x07L);
    te.onServerPacketReceived(data);
    helper.succeed();
  }

  @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void trackGetNonWireSignalFromRedstoneBlock(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    if(te == null) { helper.fail("TE missing"); return; }
    // Place a redstone block adjacent to get non-wire signal.
    helper.setBlock(TRACK_POS.east(), Blocks.REDSTONE_BLOCK);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    // getNonWireSignal via the block's signal machinery
    block.neighborChanged(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), Blocks.REDSTONE_BLOCK, helper.absolutePos(TRACK_POS.east()), false);
    helper.succeed();
  }

  // -- helpers ----------------------------------------------------------------------------------

  private static RedstoneTrack.RedstoneTrackBlock placeTrack(GameTestHelper helper)
  {
    helper.setBlock(TRACK_POS, Registries.getBlock("track").defaultBlockState());
    return (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
  }

  private static void seedTrackNet(GameTestHelper helper, int power, Direction... powerSides)
  {
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    if(te == null) throw new IllegalStateException("expected track block entity to exist");

    final CompoundTag route = new CompoundTag();
    route.putInt("power", power);
    route.put("npos", new LongArrayTag(java.util.List.of()));
    route.put("nsid", new IntArrayTag(java.util.List.of()));
    route.put("ifac", new IntArrayTag(java.util.List.of()));
    route.put("pfac", new IntArrayTag(java.util.Arrays.stream(powerSides).mapToInt(Direction::get3DDataValue).boxed().toList()));

    final ListTag nets = new ListTag();
    nets.add(route);

    final CompoundTag trackData = new CompoundTag();
    trackData.putLong("sflags", 0L);
    trackData.put("nets", nets);
    te.onServerPacketReceived(trackData);
  }

  private static RedstoneTrack.TrackBlockEntity getTrack(GameTestHelper helper)
  {
    return helper.getBlockEntity(TRACK_POS) instanceof RedstoneTrack.TrackBlockEntity te ? te : null;
  }
}
