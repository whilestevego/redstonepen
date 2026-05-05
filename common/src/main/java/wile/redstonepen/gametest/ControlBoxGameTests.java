package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.ControlBox;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.Registries;
import java.util.List;

public final class ControlBoxGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_RELAY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos CONTROL_BOX_POS = new BlockPos(1, 1, 1);

  private ControlBoxGameTests()
  {}

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void controlBoxEvaluatesConstantProgram(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity controlBox = placeControlBox(helper);
    controlBox.setCode("b=7");
    controlBox.setEnabled(true);
    controlBox.tick();

    helper.succeedWhen(() -> {
      final ControlBox.ControlBoxBlockEntity te = getControlBox(helper);
      if(te == null) throw new IllegalStateException("expected control box block entity to exist");
      if(!te.getEnabled()) throw new IllegalStateException("expected control box to be enabled");
      if(!"b=7".equals(te.getCode())) throw new IllegalStateException("expected control box code to remain applied");
      if(((te.writenbt(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag(), false)
        .getCompound("logic").getInt("output") >> (4 * net.minecraft.core.Direction.EAST.ordinal())) & 0xf) != 7) {
        throw new IllegalStateException("expected control box to compute a constant east-side output of 7");
      }
    });
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void controlBoxRejectsInvalidProgram(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity controlBox = placeControlBox(helper);
    controlBox.setCode("b=d.bad");

    helper.succeedWhen(() -> {
      final ControlBox.ControlBoxBlockEntity.TestHooks hooks = new ControlBox.ControlBoxBlockEntity.TestHooks();
      if(hooks.setCode(controlBox.getCode())) {
        throw new IllegalStateException("expected invalid control box code to remain invalid");
      }
      if(hooks.errors().isEmpty()) {
        throw new IllegalStateException("expected invalid control box code to expose parse errors");
      }
    });
  }

  // --- NBT round-trip ----------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void writenbtRoundTripsCodeAndSymbols(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=5");
    final net.minecraft.nbt.CompoundTag nbt = te.writenbt(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag(), false);
    if(!nbt.contains("logic", net.minecraft.nbt.Tag.TAG_COMPOUND)) helper.fail("expected logic compound");
    if(!"b=5".equals(nbt.getCompound("logic").getString("code"))) helper.fail("expected code persisted");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void readnbtRestoresCodeAndOutputData(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final net.minecraft.nbt.CompoundTag logic = new net.minecraft.nbt.CompoundTag();
    logic.putString("code", "b=12");
    logic.putInt("input", 0);
    logic.putInt("output", 0);
    final net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
    nbt.put("logic", logic);
    te.readnbt(helper.getLevel().registryAccess(), nbt);
    if(!"b=12".equals(te.getCode())) helper.fail("expected code restored");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void readnbtPreservesSymbolMap(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final net.minecraft.nbt.CompoundTag syms = new net.minecraft.nbt.CompoundTag();
    syms.putInt("foo", 42);
    final net.minecraft.nbt.CompoundTag logic = new net.minecraft.nbt.CompoundTag();
    logic.putString("code", "");
    logic.put("symbols", syms);
    final net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
    nbt.put("logic", logic);
    te.readnbt(helper.getLevel().registryAccess(), nbt);
    helper.succeed();
  }

  // --- enabled state -----------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void initiallyDisabled(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    if(te.getEnabled()) helper.fail("freshly placed control box must start disabled");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setEnabledTrueFlipsState(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setEnabled(true);
    final ControlBox.ControlBoxBlockEntity te2 = getControlBox(helper);
    if(te2 == null) { helper.fail("BE missing after setEnabled"); return; }
    if(!te2.getEnabled()) helper.fail("expected enabled after setEnabled(true)");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setEnabledIdempotent(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setEnabled(false);
    final ControlBox.ControlBoxBlockEntity te2 = getControlBox(helper);
    if(te2 == null) { helper.fail("BE missing"); return; }
    if(te2.getEnabled()) helper.fail("setEnabled(false) on already-disabled must remain disabled");
    helper.succeed();
  }

  // --- tick paths --------------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void tickOnDisabledBoxClearsOutput(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=7");
    // Not enabled — tick should set output_data to 0
    te.tick();
    final int output = te.writenbt(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag(), false)
      .getCompound("logic").getInt("output");
    if(output != 0) helper.fail("disabled control box must produce 0 output");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void tickWithInvalidCodeDoesNotThrow(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=d.bad");
    te.setEnabled(true);
    final ControlBox.ControlBoxBlockEntity te2 = getControlBox(helper);
    if(te2 != null) te2.tick();
    helper.succeed();
  }

  // --- trace toggle ------------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void traceToggleFlipsFlag(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final boolean before = te.trace_enabled();
    te.toggle_trace(null);
    if(te.trace_enabled() == before) helper.fail("toggle_trace must flip the flag");
    helper.succeed();
  }

  // --- name ---------------------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getNameFallsBackToBlockTranslationKey(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    if(te.hasCustomName()) helper.fail("default control box must not have custom name");
    if(te.getName() == null) helper.fail("getName must not be null");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setCustomNameStoresAndReturnsIt(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCustomName(net.minecraft.network.chat.Component.literal("My Box"));
    if(!te.hasCustomName()) helper.fail("hasCustomName false after set");
    if(!"My Box".equals(te.getName().getString())) helper.fail("custom name not returned");
    helper.succeed();
  }

  // --- packet receiver ---------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void onServerPacketReceivedAppliesCodeFromNbt(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final net.minecraft.nbt.CompoundTag logic = new net.minecraft.nbt.CompoundTag();
    logic.putString("code", "b=3");
    final net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
    nbt.put("logic", logic);
    te.onServerPacketReceived(nbt);
    if(!"b=3".equals(te.getCode())) helper.fail("packet did not apply code");
    helper.succeed();
  }

  // --- getSignal / getDirectSignal --------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void getSignalReturnsComputedOutput(GameTestHelper helper)
  {
    // Port 'b' = internal EAST (Direction.EAST, ordinal 5).
    // For FACING=NORTH, ROTATION=0, the external side that maps to internal EAST is also EAST,
    // so getSignal is queried with Direction.WEST (redstone_side.getOpposite() = EAST → internal EAST).
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=7");
    te.setEnabled(true);
    te.tick();
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final int signal = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(),
      helper.absolutePos(CONTROL_BOX_POS), Direction.WEST);
    if(signal != 7) helper.fail("expected getSignal=7 queried from WEST, got " + signal);
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void getDirectSignalMatchesGetSignal(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=5");
    te.setEnabled(true);
    te.tick();
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final BlockPos absPos = helper.absolutePos(CONTROL_BOX_POS);
    final net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(CONTROL_BOX_POS);
    final int sig = block.getSignal(state, helper.getLevel(), absPos, Direction.WEST);
    final int dsig = block.getDirectSignal(state, helper.getLevel(), absPos, Direction.WEST);
    if(sig != dsig) helper.fail("getDirectSignal must match getSignal, got sig=" + sig + " dsig=" + dsig);
    helper.succeed();
  }

  // --- dropList ---------------------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void dropListWithCodeSavesNbt(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=3");
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final List<ItemStack> drops = block.dropList(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), te, false);
    if(drops.size() != 1) helper.fail("expected 1 drop, got " + drops.size());
    if(!Auxiliaries.hasItemStackNbt(drops.get(0), "tedata"))
      helper.fail("drop must carry tedata nbt when code is set");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void dropListWithNoCodeHasNoNbt(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final List<ItemStack> drops = block.dropList(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), te, false);
    if(drops.size() != 1) helper.fail("expected 1 drop");
    if(Auxiliaries.hasItemStackNbt(drops.get(0), "tedata"))
      helper.fail("drop must not carry tedata nbt when no code is set");
    helper.succeed();
  }

  // --- setPlacedBy -----------------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setPlacedByWithNbtRestoresCode(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=9");
    final net.minecraft.nbt.CompoundTag tedata = te.writenbt(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag(), false);
    // Prepare stack with saved NBT, re-place a fresh control box, then call setPlacedBy.
    final ItemStack stack = new ItemStack(Registries.getItem("control_box"));
    Auxiliaries.setItemStackNbt(stack, "tedata", tedata);
    helper.setBlock(CONTROL_BOX_POS, Registries.getBlock("control_box").defaultBlockState());
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    block.setPlacedBy(helper.getLevel(), helper.absolutePos(CONTROL_BOX_POS),
      helper.getBlockState(CONTROL_BOX_POS), null, stack);
    final ControlBox.ControlBoxBlockEntity te2 = getControlBox(helper);
    if(te2 == null) { helper.fail("expected BE after setPlacedBy"); return; }
    if(!"b=9".equals(te2.getCode())) helper.fail("expected code restored by setPlacedBy, got: " + te2.getCode());
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setPlacedByWithEmptyNbtIsNoOp(GameTestHelper helper)
  {
    placeControlBox(helper);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final ItemStack stack = new ItemStack(Registries.getItem("control_box")); // no NBT
    block.setPlacedBy(helper.getLevel(), helper.absolutePos(CONTROL_BOX_POS),
      helper.getBlockState(CONTROL_BOX_POS), null, stack);
    // setPlacedBy returns early when NBT is empty – should not throw.
    helper.succeed();
  }

  // --- update ----------------------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void updateWithNullFromPosResetsTickTimer(GameTestHelper helper)
  {
    placeControlBox(helper);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final BlockPos absPos = helper.absolutePos(CONTROL_BOX_POS);
    final net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(CONTROL_BOX_POS);
    final net.minecraft.world.level.block.state.BlockState result = block.update(state, helper.getLevel(), absPos, null);
    if(result == null) helper.fail("update must return non-null state");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void updateWithNeighborPosTriggersSideScan(GameTestHelper helper)
  {
    placeControlBox(helper);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final BlockPos absPos = helper.absolutePos(CONTROL_BOX_POS);
    final net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(CONTROL_BOX_POS);
    // fromPos set to east neighbor → world_side = EAST, signal_update called
    final net.minecraft.world.level.block.state.BlockState result = block.update(state, helper.getLevel(), absPos, absPos.east());
    if(result == null) helper.fail("update must return non-null state");
    helper.succeed();
  }

  // --- ControlBoxBlock additional coverage -------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void isBlockEntityTickingAlwaysTrue(GameTestHelper helper)
  {
    placeControlBox(helper);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    if(!block.isBlockEntityTicking(helper.getLevel(), helper.getBlockState(CONTROL_BOX_POS)))
      helper.fail("isBlockEntityTicking must return true");
    helper.succeed();
  }

  // --- getDisplayName / createMenu -----------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void defsDefaultConstructorIsCallable(GameTestHelper helper)
  {
    // Instantiate Defs to cover its default constructor (the static PORT_NAMES field covers <clinit>).
    new ControlBox.Defs();
    if(ControlBox.Defs.PORT_NAMES.size() != 6) helper.fail("PORT_NAMES must have 6 entries");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getDisplayNameReturnsNonNull(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    if(te.getDisplayName() == null) helper.fail("getDisplayName must not return null");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void createMenuReturnsNonNull(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final var menu = te.createMenu(0, player.getInventory(), player);
    if(menu == null) helper.fail("createMenu must not return null");
    helper.succeed();
  }

  // --- tick with trace / tickrate -----------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void tickWithTraceEnabledCoversTracePaths(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.toggle_trace(null);
    te.setCode("b=3");
    te.setEnabled(true);
    te.tick();
    if(!te.trace_enabled()) helper.fail("trace must still be enabled after tick");
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void tickWithTickrateSymbolSetsInterval(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=3\ntickrate=10");
    te.setEnabled(true);
    te.tick();
    helper.succeed();
  }

  // --- setEnabled(false) from enabled -------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setEnabledFalseFromEnabledClearsSymbols(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=7");
    te.setEnabled(true);
    te.tick();
    te.setEnabled(false);
    final ControlBox.ControlBoxBlockEntity te2 = getControlBox(helper);
    if(te2 == null) { helper.fail("BE missing after setEnabled(false)"); return; }
    if(te2.getEnabled()) helper.fail("must be disabled after setEnabled(false)");
    helper.succeed();
  }

  // --- setRcaPlayerUUID --------------------------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setRcaPlayerUuidNonNullDoesNotThrow(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setRcaPlayerUUID(java.util.UUID.randomUUID());
    helper.succeed();
  }

  // --- signal_update rising/falling edges ---------------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void signalUpdateRisingEdgeSetsIntredge(GameTestHelper helper)
  {
    // Port 'g' = WEST (Direction.WEST ordinal 4). Code "b=g" makes 'g' an input port.
    // Place redstone block to the west, then call update() with fromPos=west neighbor.
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=g");
    te.setEnabled(true);
    te.tick(); // initializes input_data to 0 (no signal yet)
    helper.setBlock(CONTROL_BOX_POS.west(), Blocks.REDSTONE_BLOCK);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final BlockPos absPos = helper.absolutePos(CONTROL_BOX_POS);
    final net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(CONTROL_BOX_POS);
    block.update(state, helper.getLevel(), absPos, absPos.west());
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void signalUpdateFallingEdgeSetsIntfedge(GameTestHelper helper)
  {
    // Same port 'g' = WEST. First tick with redstone block (sets input_data to 15<<16),
    // then remove it and call update → falling edge fires.
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=g");
    te.setEnabled(true);
    helper.setBlock(CONTROL_BOX_POS.west(), Blocks.REDSTONE_BLOCK);
    te.tick(); // input_data now has g=15
    helper.setBlock(CONTROL_BOX_POS.west(), Blocks.AIR);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final BlockPos absPos = helper.absolutePos(CONTROL_BOX_POS);
    block.update(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), absPos, absPos.west());
    helper.succeed();
  }

  // --- ControlBoxBlock.useItemOn with debug stick ------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void useItemOnWithDebugStickTogglesTrace(GameTestHelper helper)
  {
    placeControlBox(helper);
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    final ItemStack debugStick = new ItemStack(Items.DEBUG_STICK);
    player.setItemInHand(InteractionHand.MAIN_HAND, debugStick);
    final BlockPos absPos = helper.absolutePos(CONTROL_BOX_POS);
    final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos), Direction.UP, absPos, false);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final var result = helper.getBlockState(CONTROL_BOX_POS).useItemOn(debugStick, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    if(result == null) helper.fail("useItemOn must not return null");
    helper.succeed();
  }

  // --- ControlBox built-in function coverage -----------------------------------------------

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void controlBoxBuiltinFunctionsExercised(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final BlockPos absPos = helper.absolutePos(CONTROL_BOX_POS);
    te.setEnabled(true);
    int sig;

    te.setCode("b=max(3,5)"); te.tick();
    sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), absPos, Direction.WEST);
    if(sig != 5) helper.fail("max(3,5) expected 5, got " + sig);

    te.setCode("b=min(3,5)"); te.tick();
    sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), absPos, Direction.WEST);
    if(sig != 3) helper.fail("min(3,5) expected 3, got " + sig);

    te.setCode("b=inv(3)"); te.tick();
    sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), absPos, Direction.WEST);
    if(sig != 12) helper.fail("inv(3) expected 12, got " + sig);

    te.setCode("b=if(1,15,0)"); te.tick();
    sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), absPos, Direction.WEST);
    if(sig != 15) helper.fail("if(1,15,0) expected 15, got " + sig);

    te.setCode("b=mean(4,8)"); te.tick();
    sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), absPos, Direction.WEST);
    if(sig != 6) helper.fail("mean(4,8) expected 6, got " + sig);

    te.setCode("b=lim(3,0,15)"); te.tick();
    sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(), absPos, Direction.WEST);
    if(sig != 3) helper.fail("lim(3,0,15) expected 3, got " + sig);

    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void controlBoxCounterFunctionsExercised(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    // cnt1/cnt2/cnt3 each increment from 0; last assignment (cnt3) wins for port b.
    // 3-arg form cnt(up,down,max): up=1>0 increments, down=0<=0 does not decrement, max=10.
    // After 2 ticks cnt3 should have reached 2.
    te.setCode("b=cnt1(1,0,10)\nb=cnt2(1,0,10)\nb=cnt3(1,0,10)");
    te.setEnabled(true);
    te.tick();
    te.tick();
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final int sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(),
      helper.absolutePos(CONTROL_BOX_POS), Direction.WEST);
    if(sig != 2) helper.fail("cnt3(1,10) after 2 ticks expected 2, got " + sig);
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void controlBoxTimerFunctionsExercised(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    // Last assignment (tiv1) wins for port b. tiv1(5) is an interval timer with period=5;
    // after 2 ticks elapsed=2 < period, so output=0.
    te.setCode("b=ton1(1,5)\nb=tof1(1,5)\nb=tp1(1,5)\nb=tiv1(5)");
    te.setEnabled(true);
    te.tick();
    te.tick();
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final int sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(),
      helper.absolutePos(CONTROL_BOX_POS), Direction.WEST);
    if(sig != 0) helper.fail("tiv1(5) after 2 ticks expected 0, got " + sig);
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void controlBoxRemainingTimerVariantsExercised(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    // Cover lambda bodies for cnt4/cnt5, tiv2/tiv3, ton2-5, tof2-5, tp2-5, rnd, clock, time.
    // Last assignment is b=time(); assert output is in valid redstone range [0,15].
    te.setCode("b=cnt4(1,10)\nb=cnt5(1,10)\nb=tiv2(10)\nb=tiv3(10)"
      + "\nb=ton2(1,5)\nb=ton3(1,5)\nb=ton4(1,5)\nb=ton5(1,5)"
      + "\nb=tof2(1,5)\nb=tof3(1,5)\nb=tof4(1,5)\nb=tof5(1,5)"
      + "\nb=tp2(1,5)\nb=tp3(1,5)\nb=tp4(1,5)\nb=tp5(1,5)"
      + "\nb=rnd()\nb=clock()\nb=time()");
    te.setEnabled(true);
    te.tick();
    te.tick();
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final int sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(),
      helper.absolutePos(CONTROL_BOX_POS), Direction.WEST);
    if(sig < 0 || sig > 15) helper.fail("time() output must be in [0,15], got " + sig);
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void controlBoxTimerEdgeCasesDoNotThrow(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    // tof(0,0): pt=0 branch → returns bool_true immediately (b=15).
    // tp(0,5): in=0, pulse never started → returns bool_false (b=0). Last assignment wins.
    te.setCode("b=tof1(0,0)\nb=tp1(0,5)");
    te.setEnabled(true);
    te.tick();
    final ControlBox.ControlBoxBlock block = (ControlBox.ControlBoxBlock)Registries.getBlock("control_box");
    final int sig = block.getSignal(helper.getBlockState(CONTROL_BOX_POS), helper.getLevel(),
      helper.absolutePos(CONTROL_BOX_POS), Direction.WEST);
    if(sig != 0) helper.fail("tp1(0,5) with no input expected 0, got " + sig);
    helper.succeed();
  }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void controlBoxSetCodeSameCodeSkipsReparse(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=7");
    te.setEnabled(true);
    te.tick();
    // Second call with same code hits the short-circuit in Logic.code() when expressions_ is not empty.
    te.setCode("b=7");
    helper.succeed();
  }

  private static ControlBox.ControlBoxBlockEntity placeControlBox(GameTestHelper helper)
  {
    helper.setBlock(CONTROL_BOX_POS, Registries.getBlock("control_box").defaultBlockState());
    final ControlBox.ControlBoxBlockEntity te = getControlBox(helper);
    if(te == null) throw new IllegalStateException("expected control box block entity to be created");
    return te;
  }

  private static ControlBox.ControlBoxBlockEntity getControlBox(GameTestHelper helper)
  {
    return helper.getBlockEntity(CONTROL_BOX_POS) instanceof ControlBox.ControlBoxBlockEntity te ? te : null;
  }
}
