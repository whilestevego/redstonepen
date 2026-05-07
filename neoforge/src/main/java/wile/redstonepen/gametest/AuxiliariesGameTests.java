package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.ModConstants;
import wile.redstonepen.gametestcommon.AuxiliariesTests;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class AuxiliariesGameTests
{
  private static final String RELAY_TEMPLATE = "relay_activates_from_redstone";

  public AuxiliariesGameTests() {}

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void particlesOnServerLevelDoesNotThrow(GameTestHelper helper)
  { AuxiliariesTests.particlesOnServerLevelDoesNotThrow(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void particlesVec3OnServerLevelDoesNotThrow(GameTestHelper helper)
  { AuxiliariesTests.particlesVec3OnServerLevelDoesNotThrow(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getFakePlayerOnServerLevelReturnsPresent(GameTestHelper helper)
  { AuxiliariesTests.getFakePlayerOnServerLevelReturnsPresent(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void playerChatMessageWithMockPlayerDoesNotThrow(GameTestHelper helper)
  { AuxiliariesTests.playerChatMessageWithMockPlayerDoesNotThrow(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void serializeNonNullComponentReturnsNonEmpty(GameTestHelper helper)
  { AuxiliariesTests.serializeNonNullComponentReturnsNonEmpty(helper); }

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void unserializeSimpleTextComponentReturnsComponent(GameTestHelper helper)
  { AuxiliariesTests.unserializeSimpleTextComponentReturnsComponent(helper); }
}
