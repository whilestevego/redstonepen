package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.blocks.CircuitComponents;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class RelayGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_RELAY_TEMPLATE = "relay_activates_from_redstone";
  private static final BlockPos RELAY_POS = new BlockPos(1, 1, 1);
  private static final BlockPos INPUT_POS = RELAY_POS.east();

  private RelayGameTests()
  {}

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayActivatesFromRedstone(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("relay"), true, 0,
      "expected relay to become powered from adjacent redstone input"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void invertedRelayRegistersPoweredInput(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("inverted_relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("inverted_relay"), true, 0,
      "expected inverted relay to register its powered input"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
  public static void pulseRelayClearsItsPulseAfterTick(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("pulse_relay"));

    helper.runAtTickTime(1, () -> assertRelayState(helper, Registries.getBlock("pulse_relay"), true, 1,
      "expected pulse relay to enter its pulsing state"));
    helper.runAtTickTime(6, () -> assertRelayState(helper, Registries.getBlock("pulse_relay"), true, 0,
      "expected pulse relay to clear its pulse after the scheduled tick"));
    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("pulse_relay"), true, 0,
      "expected pulse relay to settle back to an unlatched state"));
  }

  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void bistableRelayLatchesOnFirstRisingEdge(GameTestHelper helper)
  {
    placePoweredInput(helper, Registries.getBlock("bistable_relay"));

    helper.succeedWhen(() -> assertRelayState(helper, Registries.getBlock("bistable_relay"), true, 1,
      "expected bistable relay to latch on after the first rising edge"));
  }

  private static void placePoweredInput(GameTestHelper helper, Block relayBlock)
  {
    helper.setBlock(INPUT_POS, Blocks.REDSTONE_BLOCK);
    helper.setBlock(RELAY_POS, relayBlock.defaultBlockState());
  }

  private static void assertRelayState(GameTestHelper helper, Block relayBlock, boolean powered, int state, String message)
  {
    helper.assertBlockState(
      RELAY_POS,
      blockState -> blockState.is(relayBlock)
        && blockState.getValue(CircuitComponents.DirectedComponentBlock.POWERED) == powered
        && blockState.getValue(CircuitComponents.DirectedComponentBlock.STATE) == state,
      () -> message
    );
  }
}
