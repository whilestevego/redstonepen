package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.gametestcommon.RecipeTests;

public class RecipeGameTests
{
  private static final String RELAY_TEMPLATE = "redstonepen:relay_activates_from_redstone";

  public RecipeGameTests() {}

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void extendedShapelessRecipeSerializerRoundTrip(GameTestHelper helper)
  { RecipeTests.extendedShapelessRecipeSerializerRoundTrip(helper); }
}
