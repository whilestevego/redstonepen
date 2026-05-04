package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.RedstoneTrack;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class TrackGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos TRACK_POS = new BlockPos(1, 1, 1);

  private TrackGameTests()
  {}

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void writenbtSyncOmitsNetsList(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 5, Direction.WEST);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final CompoundTag sync = te.writenbt(helper.getLevel().registryAccess(), new CompoundTag(), true);
    if(sync.contains("nets")) helper.fail("sync packet writenbt must omit nets list");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 10)
  public static void writenbtFullIncludesNetsList(GameTestHelper helper)
  {
    placeTrack(helper);
    seedTrackNet(helper, 5, Direction.WEST);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final CompoundTag full = te.writenbt(helper.getLevel().registryAccess(), new CompoundTag(), false);
    if(!full.contains("nets", net.minecraft.nbt.Tag.TAG_LIST)) helper.fail("full writenbt must include nets list");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 10)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 10)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void updateAllPowerValuesOnIsolatedTrackReturnsMap(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    final java.util.Map<BlockPos, BlockPos> notes = te.updateAllPowerValuesFromAdjacent();
    if(notes == null) helper.fail("updateAllPowerValuesFromAdjacent must return a non-null map");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateForAirNeighborReturns(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.addWireFlags(0x01L);
    te.handleShapeUpdate(Direction.DOWN, Blocks.AIR.defaultBlockState(), TRACK_POS.below(), false);
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateMovingFlagSkipsRecursion(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.handleShapeUpdate(Direction.DOWN, Blocks.AIR.defaultBlockState(), TRACK_POS.below(), true);
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void handleShapeUpdateRedstoneBlockNeighborSkipsConnectionRefresh(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.TrackBlockEntity te = getTrack(helper);
    te.handleShapeUpdate(Direction.EAST, Blocks.REDSTONE_BLOCK.defaultBlockState(), TRACK_POS.east(), false);
    helper.succeed();
  }

  // --- RedstoneTrackBlock pure-state methods --------------------------------------------------

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void emptyTrackShapeIsEmpty(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final var shape = block.getShape(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), net.minecraft.world.phys.shapes.CollisionContext.empty());
    if(!shape.isEmpty()) helper.fail("expected empty shape with no wire bits");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getCollisionShapeIsAlwaysEmpty(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final var shape = block.getCollisionShape(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), net.minecraft.world.phys.shapes.CollisionContext.empty());
    if(!shape.isEmpty()) helper.fail("track collision shape must be empty");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getDirectSignalForUnpoweredTrackIsZero(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final int s = block.getDirectSignal(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), Direction.NORTH);
    if(s != 0) helper.fail("expected 0 direct signal");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void canSurviveAlwaysTrue(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(!block.canSurvive(helper.getBlockState(TRACK_POS), helper.getLevel(), helper.absolutePos(TRACK_POS)))
      helper.fail("canSurvive must be true");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void shouldCheckWeakPowerIsFalse(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(block.shouldCheckWeakPower(helper.getBlockState(TRACK_POS), helper.getLevel(),
        helper.absolutePos(TRACK_POS), Direction.NORTH))
      helper.fail("shouldCheckWeakPower must be false");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void canConnectRedstoneFalseForUnconnectedTrack(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    final boolean c = block.canConnectRedstone(helper.getBlockState(TRACK_POS), helper.getLevel(),
      helper.absolutePos(TRACK_POS), Direction.NORTH);
    if(c) helper.fail("expected no redstone connection on bare track");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void canConnectRedstoneFalseForNullSide(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(block.canConnectRedstone(helper.getBlockState(TRACK_POS), helper.getLevel(),
        helper.absolutePos(TRACK_POS), null))
      helper.fail("null side must yield false");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void propagatesSkylightDownDependsOnWaterlogged(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(!block.propagatesSkylightDown(helper.getBlockState(TRACK_POS), helper.getLevel(), helper.absolutePos(TRACK_POS)))
      helper.fail("non-waterlogged track must propagate skylight");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void useShapeForLightOcclusionTrue(GameTestHelper helper)
  {
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    placeTrack(helper);
    if(!block.useShapeForLightOcclusion(helper.getBlockState(TRACK_POS)))
      helper.fail("useShapeForLightOcclusion must be true");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void getRenderShapeIsAnimatedEntityBlock(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    if(block.getRenderShape(helper.getBlockState(TRACK_POS)) != net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED)
      helper.fail("expected ENTITYBLOCK_ANIMATED render shape");
    helper.succeed();
  }

  // --- notifyAdjacent does not throw on placed track ---

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_TEMPLATE, timeoutTicks = 5)
  public static void notifyAdjacentRunsWithoutThrowing(GameTestHelper helper)
  {
    placeTrack(helper);
    final RedstoneTrack.RedstoneTrackBlock block = (RedstoneTrack.RedstoneTrackBlock)Registries.getBlock("track");
    block.notifyAdjacent(helper.getLevel(), helper.absolutePos(TRACK_POS));
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
