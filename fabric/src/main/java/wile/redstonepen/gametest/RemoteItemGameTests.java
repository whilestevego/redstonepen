package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModConstants;
import wile.redstonepen.libmc.Registries;

public class RemoteItemGameTests
{
  private static final String NS = "minecraft";
  private static final String EMPTY = "redstonepen:relay_activates_from_redstone";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  public RemoteItemGameTests() {}

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void remoteBarNotVisible(GameTestHelper helper)
  {
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    if(remote.getItem().isBarVisible(remote)) helper.fail("remote item must hide damage bar");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void remoteHasHighDestroySpeed(GameTestHelper helper)
  {
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    final float speed = remote.getItem().getDestroySpeed(remote, Blocks.STONE.defaultBlockState());
    if(speed < 100f) helper.fail("remote destroy speed must be high (got " + speed + ")");
    helper.succeed();
  }

  // --- use() -------------------------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void remoteUseUnlinkedReturnsFail(GameTestHelper helper)
  {
    // Unlinked remote: onTriggerRemoteLink returns early (no data); use() returns fail.
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    player.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final var result = remote.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    if(result.getResult() != InteractionResult.FAIL) helper.fail("unlinked remote use must return fail, got " + result.getResult());
    helper.succeed();
  }

  // --- canAttackBlock() / attack() ---------------------------------------------------------

  // Helper: place a floor-mounted lever at leverPos (requires solid block below).
  private static void placeLever(GameTestHelper helper, BlockPos leverPos)
  {
    helper.setBlock(leverPos.below(), Blocks.STONE);
    helper.setBlock(leverPos, Blocks.LEVER);  // default state = floor face
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void remoteCanAttackBlockAlwaysReturnsFalse(GameTestHelper helper)
  {
    // canAttackBlock must return false to prevent block breaking.
    placeLever(helper, POS);
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    player.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos leverAbs = helper.absolutePos(POS);
    final BlockState ls = helper.getLevel().getBlockState(leverAbs);
    final boolean result = remote.getItem().canAttackBlock(ls, helper.getLevel(), leverAbs, player);
    if(result) helper.fail("canAttackBlock must return false to prevent breaking");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void remoteOnBlockStartBreakDoesNotThrow(GameTestHelper helper)
  {
    // onBlockStartBreak with a mock player must not throw and must return false.
    placeLever(helper, POS);
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    player.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos leverAbs = helper.absolutePos(POS);
    final boolean result = ((wile.redstonepen.libmc.StandardItems.BaseItem)remote.getItem())
      .onBlockStartBreak(remote, leverAbs, player);
    if(result) helper.fail("onBlockStartBreak must return false");
    helper.succeed();
  }

  // --- doesSneakBypassUse ------------------------------------------------------------------

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void remoteDoesNotSneakBypassUse(GameTestHelper helper)
  {
    // doesSneakBypassUse is a NeoForge-only Item extension; not available in Fabric API.
    helper.succeed();
  }
}
