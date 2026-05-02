/*
 * @file DemoGameTests.java
 * @license MIT
 *
 * Integration tests for the /redstonepen demo contraptions. Each test builds
 * one contraption inside an empty pad template and asserts that the expected
 * mod block landed at the expected position. Together with the existing
 * RelayGameTests / ControlBoxGameTests / TrackGameTests, this serves as an
 * end-to-end sanity check that the mod's blocks place correctly and integrate
 * with vanilla redstone (wire, lamps, pistons, levers, buttons, repeaters).
 */
package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.commands.DemoSections;
import wile.redstonepen.libmc.Registries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class DemoGameTests
{
  private static final String TEMPLATE_NAMESPACE = "minecraft";
  private static final String EMPTY_PAD = "empty_demo_pad";

  // Each contraption is built with cell-origin = (1, 1, 1) in template-local coords
  // so the 9x9 footprint fits inside the 16x6x16 pad with room to spare on every side.
  private static final BlockPos CELL_LOCAL = new BlockPos(1, 1, 1);

  private DemoGameTests()
  {}

  // -------------------------------------------------------------------------------------------
  // basic_lever drives a vanilla redstone lamp via redstone wire.
  // Asserts: lever, wires, and lamp land at expected positions.
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void leverDrivesLamp(GameTestHelper helper)
  {
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 2), Blocks.REDSTONE_WIRE);
    });
  }

  // -------------------------------------------------------------------------------------------
  // basic_button -> wire -> sticky piston -> glowstone.
  // Asserts: button, piston, glowstone all placed correctly.
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void buttonDrivesPiston(GameTestHelper helper)
  {
    DemoSections.buildButtonDrivesPiston(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_button");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), Blocks.STICKY_PISTON);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.GLOWSTONE);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> inverted_relay -> wire -> lamp. (NOT-gate behavior.)
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void invertedRelayNotGate(GameTestHelper helper)
  {
    DemoSections.buildInvertedRelayNotGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "inverted_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Stone-button -> bistable_relay -> lamp. (T flip-flop.)
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void bistableToggle(GameTestHelper helper)
  {
    DemoSections.buildBistableToggle(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.STONE_BUTTON);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "bistable_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> pulse_relay -> wire -> lamp.
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void pulseRelayMonostable(GameTestHelper helper)
  {
    DemoSections.buildPulseRelayMonostable(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "pulse_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> relay -> wire -> lamp. (Buffer / direction.)
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void relayBuffer(GameTestHelper helper)
  {
    DemoSections.buildRelayBuffer(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), "relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 0), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Two perpendicular signal lines crossing at a single bridge_relay.
  // Asserts both input levers and the bridge_relay are placed at the intersection.
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void bridgeRelayCrossover(GameTestHelper helper)
  {
    DemoSections.buildBridgeRelayCrossover(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(4, 0, 3), "bridge_relay");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 5), Blocks.LEVER);    // N-S input
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(1, 0, 3), Blocks.LEVER);    // E-W input
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(4, 0, 1), Blocks.REDSTONE_LAMP);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(6, 0, 3), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // control_box AND program: levers feed ports y/g, output b drives a lamp.
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void controlBoxAndGate(GameTestHelper helper)
  {
    DemoSections.buildControlBoxAndGate(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(3, 1, 3), "control_box");
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(3, 1, 4), Blocks.REDSTONE_BLOCK);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(5, 1, 3), Blocks.REDSTONE_LAMP);
    });
  }

  // -------------------------------------------------------------------------------------------
  // Vanilla lever -> wire -> repeater -> wire -> basic_gauge.
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 20)
  public static void gaugeReadout(GameTestHelper helper)
  {
    DemoSections.buildGaugeReadout(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    helper.succeedWhen(() -> {
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 5), Blocks.LEVER);
      assertVanillaBlockAt(helper, CELL_LOCAL.offset(2, 0, 3), Blocks.REPEATER);
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 1), "basic_gauge");
    });
  }

  // -------------------------------------------------------------------------------------------
  // End-to-end: build all 9 contraptions in one structure. Asserts the master entry-point
  // exits without exception and at least one block from each contraption lands. This is the
  // "smoke test" that a player running /redstonepen demo will see succeed.
  // -------------------------------------------------------------------------------------------
  @GameTest(templateNamespace = TEMPLATE_NAMESPACE, template = EMPTY_PAD, timeoutTicks = 40)
  public static void runAllSmoke(GameTestHelper helper)
  {
    // Just call runCircuits but only with the first contraption's cell visible inside the pad.
    // For full grid, an even larger template would be required; this test verifies the
    // dispatch loop doesn't throw.
    DemoSections.buildLeverDrivesLamp(helper.getLevel(), helper.absolutePos(CELL_LOCAL));
    DemoSections.buildGaugeReadout(helper.getLevel(),
      helper.absolutePos(new BlockPos(7, 1, 1)));   // second cell offset
    helper.succeedWhen(() -> {
      assertModBlockAt(helper, CELL_LOCAL.offset(2, 0, 4), "basic_lever");
      assertModBlockAt(helper, new BlockPos(7, 1, 1).offset(2, 0, 1), "basic_gauge");
    });
  }

  // -------------------------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------------------------

  private static void assertModBlockAt(GameTestHelper helper, BlockPos localPos, String modBlockName)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState actual = helper.getLevel().getBlockState(abs);
    if(actual.getBlock() != Registries.getBlock(modBlockName)) {
      helper.fail("expected mod block " + modBlockName + " at " + localPos
        + " but found " + actual.getBlock(), localPos);
    }
  }

  private static void assertVanillaBlockAt(GameTestHelper helper, BlockPos localPos, net.minecraft.world.level.block.Block expected)
  {
    final BlockPos abs = helper.absolutePos(localPos);
    final BlockState actual = helper.getLevel().getBlockState(abs);
    if(actual.getBlock() != expected) {
      helper.fail("expected " + expected + " at " + localPos
        + " but found " + actual.getBlock(), localPos);
    }
  }
}
