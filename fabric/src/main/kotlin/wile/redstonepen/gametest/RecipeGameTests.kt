package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import wile.redstonepen.gametestcommon.RecipeTests

class RecipeGameTests {
    companion object {
        private const val RELAY_TEMPLATE = "redstonepen:relay_activates_from_redstone"

        @JvmStatic @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
        fun extendedShapelessRecipeSerializerRoundTrip(helper: GameTestHelper) = RecipeTests.extendedShapelessRecipeSerializerRoundTrip(helper)
    }
}
