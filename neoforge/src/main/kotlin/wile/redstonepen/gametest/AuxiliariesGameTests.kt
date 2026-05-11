package wile.redstonepen.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import wile.redstonepen.ModConstants
import wile.redstonepen.gametestcommon.AuxiliariesTests

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
object AuxiliariesGameTests {
    private const val RELAY_TEMPLATE = "relay_activates_from_redstone"

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun particlesOnServerLevelDoesNotThrow(helper: GameTestHelper) =
        AuxiliariesTests.particlesOnServerLevelDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun particlesVec3OnServerLevelDoesNotThrow(helper: GameTestHelper) =
        AuxiliariesTests.particlesVec3OnServerLevelDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun getFakePlayerOnServerLevelReturnsPresent(helper: GameTestHelper) =
        AuxiliariesTests.getFakePlayerOnServerLevelReturnsPresent(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun playerChatMessageWithMockPlayerDoesNotThrow(helper: GameTestHelper) =
        AuxiliariesTests.playerChatMessageWithMockPlayerDoesNotThrow(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun serializeNonNullComponentReturnsNonEmpty(helper: GameTestHelper) =
        AuxiliariesTests.serializeNonNullComponentReturnsNonEmpty(helper)

    @JvmStatic
    @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
    fun unserializeSimpleTextComponentReturnsComponent(helper: GameTestHelper) =
        AuxiliariesTests.unserializeSimpleTextComponentReturnsComponent(helper)
}
