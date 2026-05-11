package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import wile.redstonepen.ModConstants
import wile.redstonepen.gametestcommon.RecipeTests

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
object RecipeGameTests {
    private const val RELAY_TEMPLATE = "relay_activates_from_redstone"

    @JvmStatic @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun extendedShapelessRecipeSerializerRoundTrip(helper: GameTestHelper) =
        RecipeTests.extendedShapelessRecipeSerializerRoundTrip(helper)
}
