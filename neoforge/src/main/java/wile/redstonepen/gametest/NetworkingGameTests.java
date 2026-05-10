package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import wile.redstonepen.ModConstants;
import wile.redstonepen.gametestcommon.NetworkingTests;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class NetworkingGameTests
{
  private static final String TEMPLATE = "relay_activates_from_redstone";

  public NetworkingGameTests() {}

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dispatchServerReceiveNbtNotifyCallsRegisteredHandler(GameTestHelper helper)
  { NetworkingTests.dispatchServerReceiveNbtNotifyCallsRegisteredHandler(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dispatchServerReceiveTileNotifyRoutesToBlockEntity(GameTestHelper helper)
  { NetworkingTests.dispatchServerReceiveTileNotifyRoutesToBlockEntity(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dispatchServerReceiveContainerSyncCallsOnClientPacketReceived(GameTestHelper helper)
  { NetworkingTests.dispatchServerReceiveContainerSyncCallsOnClientPacketReceived(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dispatchClientReceiveNbtNotifyCallsRegisteredHandler(GameTestHelper helper)
  { NetworkingTests.dispatchClientReceiveNbtNotifyCallsRegisteredHandler(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dispatchClientReceiveTileNotifyCallsOnServerPacketReceived(GameTestHelper helper)
  { NetworkingTests.dispatchClientReceiveTileNotifyCallsOnServerPacketReceived(helper); }

  @GameTest(template = TEMPLATE, timeoutTicks = 5)
  public static void dispatchClientReceiveContainerSyncCallsOnServerPacketReceived(GameTestHelper helper)
  { NetworkingTests.dispatchClientReceiveContainerSyncCallsOnServerPacketReceived(helper); }
}
