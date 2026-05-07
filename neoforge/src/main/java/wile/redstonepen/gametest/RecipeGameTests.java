package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.ModConstants;
import wile.redstonepen.gametestcommon.RecipeTests;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class RecipeGameTests
{
  private static final String RELAY_TEMPLATE = "relay_activates_from_redstone";

  public RecipeGameTests() {}

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void extendedShapelessRecipeSerializerRoundTrip(GameTestHelper helper)
  { RecipeTests.extendedShapelessRecipeSerializerRoundTrip(helper); }
}
