package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class RemoteItemGameTests
{
  private static final String NS = "minecraft";
  private static final String EMPTY = "relay_activates_from_redstone";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  private RemoteItemGameTests() {}

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void remoteBarNotVisible(GameTestHelper helper)
  {
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    if(remote.getItem().isBarVisible(remote)) helper.fail("remote item must hide damage bar");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void remoteSneakDoesNotBypassUse(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    if(remote.getItem().doesSneakBypassUse(remote, helper.getLevel(), helper.absolutePos(POS), player))
      helper.fail("remote must not bypass-use on sneak");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void remoteHasHighDestroySpeed(GameTestHelper helper)
  {
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    final float speed = remote.getItem().getDestroySpeed(remote, Blocks.STONE.defaultBlockState());
    if(speed < 100f) helper.fail("remote destroy speed must be high (got " + speed + ")");
    helper.succeed();
  }
}
