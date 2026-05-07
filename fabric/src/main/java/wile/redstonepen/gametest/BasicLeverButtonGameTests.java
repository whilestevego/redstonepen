package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.gametestcommon.BasicLeverButtonTests;

public class BasicLeverButtonGameTests
{
  private static final String EMPTY = "redstonepen:relay_activates_from_redstone";

  public BasicLeverButtonGameTests() {}

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void leverUseWithoutItemTogglesPoweredFalseToTrue(GameTestHelper helper)
  { BasicLeverButtonTests.leverUseWithoutItemTogglesPoweredFalseToTrue(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void leverUseTwiceReturnsToUnpowered(GameTestHelper helper)
  { BasicLeverButtonTests.leverUseTwiceReturnsToUnpowered(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void buttonUseWithoutItemPressesAndPowers(GameTestHelper helper)
  { BasicLeverButtonTests.buttonUseWithoutItemPressesAndPowers(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void buttonUseOnPoweredButtonReturnsConsume(GameTestHelper helper)
  { BasicLeverButtonTests.buttonUseOnPoweredButtonReturnsConsume(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 30)
  public static void pulseButtonRevertsAfterShortInterval(GameTestHelper helper)
  { BasicLeverButtonTests.pulseButtonRevertsAfterShortInterval(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void gaugeReadsZeroWhenNoSignal(GameTestHelper helper)
  { BasicLeverButtonTests.gaugeReadsZeroWhenNoSignal(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 10)
  public static void gaugeReadsSignalFromAdjacentRedstoneBlock(GameTestHelper helper)
  { BasicLeverButtonTests.gaugeReadsSignalFromAdjacentRedstoneBlock(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void gaugeShouldCheckWeakPowerReturnsFalse(GameTestHelper helper)
  { BasicLeverButtonTests.gaugeShouldCheckWeakPowerReturnsFalse(helper); }

  @GameTest(template = EMPTY, timeoutTicks = 5)
  public static void gaugeGetStateForPlacementReturnsNonNull(GameTestHelper helper)
  { BasicLeverButtonTests.gaugeGetStateForPlacementReturnsNonNull(helper); }
}
