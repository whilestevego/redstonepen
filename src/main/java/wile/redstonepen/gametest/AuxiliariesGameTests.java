package wile.redstonepen.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.libmc.Auxiliaries;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class AuxiliariesGameTests
{
  private static final String NS = "minecraft";
  private static final String EMPTY = "relay_activates_from_redstone";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  private AuxiliariesGameTests() {}

  // --- particles -----------------------------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void particlesOnServerLevelDoesNotThrow(GameTestHelper helper)
  {
    Auxiliaries.particles(helper.getLevel(), helper.absolutePos(POS), ParticleTypes.SMOKE);
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void particlesVec3OnServerLevelDoesNotThrow(GameTestHelper helper)
  {
    final net.minecraft.world.phys.Vec3 pos = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(POS));
    Auxiliaries.particles(helper.getLevel(), pos, ParticleTypes.SMOKE, 1f);
    helper.succeed();
  }

  // --- getFakePlayer -------------------------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void getFakePlayerOnServerLevelReturnsPresent(GameTestHelper helper)
  {
    final var result = Auxiliaries.getFakePlayer(helper.getLevel());
    if(result == null) helper.fail("getFakePlayer must not return null Optional");
    helper.succeed();
  }

  // --- playerChatMessage ---------------------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void playerChatMessageWithMockPlayerDoesNotThrow(GameTestHelper helper)
  {
    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    Auxiliaries.playerChatMessage(player, "test.message");
    helper.succeed();
  }

  // --- text component helpers ----------------------------------------------------------------------

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void serializeNonNullComponentReturnsNonEmpty(GameTestHelper helper)
  {
    final String serialized = Auxiliaries.serializeTextComponent(
      Component.literal("hello"), helper.getLevel().registryAccess());
    if(serialized == null || serialized.isEmpty()) helper.fail("serialize of non-null component must return non-empty string");
    helper.succeed();
  }

  @GameTest(templateNamespace = NS, template = EMPTY, timeoutTicks = 5)
  public static void unserializeSimpleTextComponentReturnsComponent(GameTestHelper helper)
  {
    final String json = Auxiliaries.serializeTextComponent(
      Component.literal("hi"), helper.getLevel().registryAccess());
    final Component result = Auxiliaries.unserializeTextComponent(json, helper.getLevel().registryAccess());
    if(result == null) helper.fail("unserialize of valid JSON must return non-null Component");
    helper.succeed();
  }
}
