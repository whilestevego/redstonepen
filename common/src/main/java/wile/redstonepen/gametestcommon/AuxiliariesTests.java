package wile.redstonepen.gametestcommon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import wile.redstonepen.util.Auxiliaries;

public final class AuxiliariesTests
{
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  private AuxiliariesTests() {}

  public static void particlesOnServerLevelDoesNotThrow(GameTestHelper helper)
  {
    Auxiliaries.particles(helper.getLevel(), helper.absolutePos(POS), ParticleTypes.SMOKE);
    helper.succeed();
  }

  public static void particlesVec3OnServerLevelDoesNotThrow(GameTestHelper helper)
  {
    final net.minecraft.world.phys.Vec3 pos = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(POS));
    Auxiliaries.particles(helper.getLevel(), pos, ParticleTypes.SMOKE, 1f);
    helper.succeed();
  }

  public static void getFakePlayerOnServerLevelReturnsPresent(GameTestHelper helper)
  {
    final var result = Auxiliaries.getFakePlayer(helper.getLevel());
    if(result == null) helper.fail("getFakePlayer must not return null Optional");
    helper.succeed();
  }

  public static void playerChatMessageWithMockPlayerDoesNotThrow(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    Auxiliaries.playerChatMessage(player, "test.message");
    helper.succeed();
  }

  public static void serializeNonNullComponentReturnsNonEmpty(GameTestHelper helper)
  {
    final String serialized = Auxiliaries.serializeTextComponent(
      Component.literal("hello"), helper.getLevel().registryAccess());
    if(serialized == null || serialized.isEmpty()) helper.fail("serialize of non-null component must return non-empty string");
    helper.succeed();
  }

  public static void unserializeSimpleTextComponentReturnsComponent(GameTestHelper helper)
  {
    final String json = Auxiliaries.serializeTextComponent(
      Component.literal("hi"), helper.getLevel().registryAccess());
    final Component result = Auxiliaries.unserializeTextComponent(json, helper.getLevel().registryAccess());
    if(result == null) helper.fail("unserialize of valid JSON must return non-null Component");
    helper.succeed();
  }
}
