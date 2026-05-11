package wile.redstonepen.gametestcommon

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.world.level.GameType
import wile.redstonepen.util.Auxiliaries

object AuxiliariesTests {
    private val POS = BlockPos(1, 1, 1)

    @JvmStatic
    fun particlesOnServerLevelDoesNotThrow(helper: GameTestHelper) {
        Auxiliaries.particles(helper.level, helper.absolutePos(POS), ParticleTypes.SMOKE)
        helper.succeed()
    }

    @JvmStatic
    fun particlesVec3OnServerLevelDoesNotThrow(helper: GameTestHelper) {
        val pos = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(POS))
        Auxiliaries.particles(helper.level, pos, ParticleTypes.SMOKE, 1f)
        helper.succeed()
    }

    @JvmStatic
    fun getFakePlayerOnServerLevelReturnsPresent(helper: GameTestHelper) {
        val result = Auxiliaries.getFakePlayer(helper.level)
        if (result == null) helper.fail("getFakePlayer must not return null Optional")
        helper.succeed()
    }

    @JvmStatic
    fun playerChatMessageWithMockPlayerDoesNotThrow(helper: GameTestHelper) {
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        Auxiliaries.playerChatMessage(player, "test.message")
        helper.succeed()
    }

    @JvmStatic
    fun serializeNonNullComponentReturnsNonEmpty(helper: GameTestHelper) {
        val serialized =
            Auxiliaries.serializeTextComponent(
                Component.literal("hello"),
                helper.level.registryAccess(),
            )
        if (serialized == null || serialized.isEmpty()) {
            helper.fail("serialize of non-null component must return non-empty string")
        }
        helper.succeed()
    }

    @JvmStatic
    fun unserializeSimpleTextComponentReturnsComponent(helper: GameTestHelper) {
        val json =
            Auxiliaries.serializeTextComponent(
                Component.literal("hi"),
                helper.level.registryAccess(),
            )
        val result = Auxiliaries.unserializeTextComponent(json, helper.level.registryAccess())
        if (result == null) helper.fail("unserialize of valid JSON must return non-null Component")
        helper.succeed()
    }
}
