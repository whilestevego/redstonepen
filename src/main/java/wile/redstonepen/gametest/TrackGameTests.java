package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
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
