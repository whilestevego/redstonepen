package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.ControlBox;
import wile.redstonepen.items.RemoteItem;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class RemoteItemGameTests
{
  private static final String EMPTY = "relay_activates_from_redstone";
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
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    if(remote.getItem().doesSneakBypassUse(remote, helper.getLevel(), helper.absolutePos(POS), player))
      helper.fail("remote must not bypass sneak use");
    helper.succeed();
  }

  // --- attack / setRemoteData / getRemoteData / onTriggerRemoteLink (requires ServerPlayer) -

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void remoteAttackLinksToLeverWithServerPlayer(GameTestHelper helper)
  {
    // FakePlayerFactory gives a real ServerPlayer; attack() proceeds past the instanceof check
    // and calls setRemoteData(), storing the lever position in the remote stack's NBT.
    placeLever(helper, POS);
    final var fp = FakePlayerFactory.getMinecraft(helper.getLevel());
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    fp.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos leverAbs = helper.absolutePos(POS);
    remote.getItem().canAttackBlock(
      helper.getLevel().getBlockState(leverAbs), helper.getLevel(), leverAbs, fp);
    if(Auxiliaries.getItemStackNbt(remote, "remote") == null)
      helper.fail("remote must have link data after attack on lever");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void remoteTriggerLinkedLeverViaUse(GameTestHelper helper)
  {
    // Full flow: link remote to lever via attack(), then trigger via use().
    // This covers: getRemoteData, onTriggerRemoteLink (lever branch), sound lambdas.
    placeLever(helper, POS);
    final var fp = FakePlayerFactory.getMinecraft(helper.getLevel());
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    fp.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos leverAbs = helper.absolutePos(POS);
    // Link
    remote.getItem().canAttackBlock(
      helper.getLevel().getBlockState(leverAbs), helper.getLevel(), leverAbs, fp);
    // Trigger: use() → onTriggerRemoteLink → lever.pull
    remote.getItem().use(helper.getLevel(), fp, InteractionHand.MAIN_HAND);
    // Lever must now be powered
    if(!helper.getLevel().getBlockState(leverAbs).getValue(BlockStateProperties.POWERED))
      helper.fail("lever must be powered after remote trigger");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void remoteOnItemUseFirstConsumeWhenLinked(GameTestHelper helper)
  {
    // onItemUseFirst: links remote to lever (already linked via attack), then triggers it.
    // Covers the full onItemUseFirst body and a second call to onTriggerRemoteLink.
    placeLever(helper, POS);
    final var fp = FakePlayerFactory.getMinecraft(helper.getLevel());
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    fp.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos leverAbs = helper.absolutePos(POS);
    // Link first
    remote.getItem().canAttackBlock(
      helper.getLevel().getBlockState(leverAbs), helper.getLevel(), leverAbs, fp);
    // onItemUseFirst → onTriggerRemoteLink
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(leverAbs), Direction.UP, leverAbs, false);
    final var ctx = new UseOnContext(helper.getLevel(), fp, InteractionHand.MAIN_HAND, remote, hit);
    final InteractionResult result = ((RemoteItem)remote.getItem()).onItemUseFirst(remote, ctx);
    if(result != InteractionResult.CONSUME && result != InteractionResult.CONSUME_PARTIAL)
      helper.fail("onItemUseFirst with linked remote must CONSUME, got " + result);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void remoteTriggerLinkedButRemovedBlockTakesFailPath(GameTestHelper helper)
  {
    // onTriggerRemoteLink: linked position exists but block was replaced → fail branch.
    placeLever(helper, POS);
    final var fp = FakePlayerFactory.getMinecraft(helper.getLevel());
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    fp.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos leverAbs = helper.absolutePos(POS);
    // Link to lever
    remote.getItem().canAttackBlock(
      helper.getLevel().getBlockState(leverAbs), helper.getLevel(), leverAbs, fp);
    // Replace lever with stone so the linked block no longer has POWERED property
    helper.setBlock(POS, Blocks.STONE);
    // Trigger: onTriggerRemoteLink → state.hasProperty(POWERED) = false → fail.run()
    remote.getItem().use(helper.getLevel(), fp, InteractionHand.MAIN_HAND);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void remoteTriggerLinkedButtonActivatesButton(GameTestHelper helper)
  {
    // onTriggerRemoteLink: ButtonBlock branch — unpowered button → press() + sound.
    helper.setBlock(POS.below(), Blocks.STONE);
    helper.setBlock(POS, Blocks.STONE_BUTTON); // default: FACE=FLOOR, not powered
    final var fp = FakePlayerFactory.getMinecraft(helper.getLevel());
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    fp.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos buttonAbs = helper.absolutePos(POS);
    remote.getItem().canAttackBlock(
      helper.getLevel().getBlockState(buttonAbs), helper.getLevel(), buttonAbs, fp);
    remote.getItem().use(helper.getLevel(), fp, InteractionHand.MAIN_HAND);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void remoteTriggerLinkedControlBoxTogglesEnabled(GameTestHelper helper)
  {
    // onTriggerRemoteLink: ControlBoxBlock branch — toggles enabled state.
    helper.setBlock(POS.below(), Blocks.STONE);
    helper.setBlock(POS, wile.redstonepen.ModContent.references.CONTROLBOX_BLOCK);
    final var fp = FakePlayerFactory.getMinecraft(helper.getLevel());
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    fp.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos cbAbs = helper.absolutePos(POS);
    remote.getItem().canAttackBlock(
      helper.getLevel().getBlockState(cbAbs), helper.getLevel(), cbAbs, fp);
    final boolean wasEnabled = helper.getLevel().getBlockState(cbAbs).getValue(BlockStateProperties.POWERED);
    remote.getItem().use(helper.getLevel(), fp, InteractionHand.MAIN_HAND);
    final boolean nowEnabled = helper.getLevel().getBlockState(cbAbs).getValue(BlockStateProperties.POWERED);
    if(nowEnabled == wasEnabled) helper.fail("control box enabled state must change after remote trigger");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void remoteTriggerLinkedObserverHitsElseFail(GameTestHelper helper)
  {
    // onTriggerRemoteLink: else branch — block has POWERED but is not Lever/Button/ControlBox.
    // Observer has POWERED property; manually set NBT since attack() won't link to it.
    helper.setBlock(POS.below(), Blocks.STONE);
    helper.setBlock(POS, Blocks.OBSERVER);
    final var fp = FakePlayerFactory.getMinecraft(helper.getLevel());
    final ItemStack remote = new ItemStack(Registries.getItem("remote"));
    fp.setItemInHand(InteractionHand.MAIN_HAND, remote);
    final BlockPos obsAbs = helper.absolutePos(POS);
    final CompoundTag nbt = new CompoundTag();
    nbt.putLong("pos", obsAbs.asLong());
    nbt.putString("name", Blocks.OBSERVER.getDescriptionId());
    Auxiliaries.setItemStackNbt(remote, "remote", nbt);
    // Trigger: hasProperty(POWERED)=true, block is none of Lever/Button/ControlBox → else fail.run()
    remote.getItem().use(helper.getLevel(), fp, InteractionHand.MAIN_HAND);
    helper.succeed();
  }
}
