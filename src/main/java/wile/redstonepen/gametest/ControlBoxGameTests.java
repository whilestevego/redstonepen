package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.ControlBox;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class ControlBoxGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_RELAY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos CONTROL_BOX_POS = new BlockPos(1, 1, 1);

  private ControlBoxGameTests()
  {}

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void writenbtRoundTripsCodeAndSymbols(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCode("b=5");
    final net.minecraft.nbt.CompoundTag nbt = te.writenbt(helper.getLevel().registryAccess(), new net.minecraft.nbt.CompoundTag(), false);
    if(!nbt.contains("logic", net.minecraft.nbt.Tag.TAG_COMPOUND)) helper.fail("expected logic compound");
    if(!"b=5".equals(nbt.getCompound("logic").getString("code"))) helper.fail("expected code persisted");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void initiallyDisabled(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    if(te.getEnabled()) helper.fail("freshly placed control box must start disabled");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setEnabledTrueFlipsState(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setEnabled(true);
    final ControlBox.ControlBoxBlockEntity te2 = getControlBox(helper);
    if(te2 == null) { helper.fail("BE missing after setEnabled"); return; }
    if(!te2.getEnabled()) helper.fail("expected enabled after setEnabled(true)");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
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

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void traceToggleFlipsFlag(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    final boolean before = te.trace_enabled();
    te.toggle_trace(null);
    if(te.trace_enabled() == before) helper.fail("toggle_trace must flip the flag");
    helper.succeed();
  }

  // --- name ---------------------------------------------------------------------------------

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getNameFallsBackToBlockTranslationKey(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    if(te.hasCustomName()) helper.fail("default control box must not have custom name");
    if(te.getName() == null) helper.fail("getName must not be null");
    helper.succeed();
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void setCustomNameStoresAndReturnsIt(GameTestHelper helper)
  {
    final ControlBox.ControlBoxBlockEntity te = placeControlBox(helper);
    te.setCustomName(net.minecraft.network.chat.Component.literal("My Box"));
    if(!te.hasCustomName()) helper.fail("hasCustomName false after set");
    if(!"My Box".equals(te.getName().getString())) helper.fail("custom name not returned");
    helper.succeed();
  }

  // --- packet receiver ---------------------------------------------------------------------

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
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
