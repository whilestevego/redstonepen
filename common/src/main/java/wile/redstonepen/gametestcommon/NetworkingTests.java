package wile.redstonepen.gametestcommon;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import wile.redstonepen.blocks.controlbox.ControlBoxBlockEntity;
import wile.redstonepen.net.Networking;
import wile.redstonepen.registry.Registries;

public final class NetworkingTests
{
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  private NetworkingTests() {}

  // --- dispatchServerReceive ---

  public static void dispatchServerReceiveNbtNotifyCallsRegisteredHandler(GameTestHelper helper)
  {
    final boolean[] called = {false};
    final String handlerKey = "test.c2s." + helper.getLevel().getGameTime();
    Networking.PacketNbtNotifyClientToServer.handlers.put(handlerKey, (player, nbt) -> called[0] = true);
    try {
      final CompoundTag payloadNbt = new CompoundTag();
      payloadNbt.putString("hnd", handlerKey);
      payloadNbt.put("nbt", new CompoundTag());
      Networking.dispatchServerReceive(
        makePayload(Networking.PacketNbtNotifyClientToServer.PACKET_ID, payloadNbt),
        helper.getLevel(),
        helper.makeMockPlayer(GameType.SURVIVAL));
      if(!called[0]) helper.fail("expected C2S NbtNotify handler to be called");
    } finally {
      Networking.PacketNbtNotifyClientToServer.handlers.remove(handlerKey);
    }
    helper.succeed();
  }

  public static void dispatchServerReceiveTileNotifyRoutesToBlockEntity(GameTestHelper helper)
  {
    helper.setBlock(POS, Registries.getBlock("control_box").defaultBlockState());
    final ControlBoxBlockEntity te = (ControlBoxBlockEntity) helper.getBlockEntity(POS);
    if(te == null) { helper.fail("expected control box block entity"); return; }

    final CompoundTag innerNbt = new CompoundTag();
    final CompoundTag logicNbt = new CompoundTag();
    logicNbt.putString("code", "b=3");
    innerNbt.put("logic", logicNbt);

    final CompoundTag payloadNbt = new CompoundTag();
    payloadNbt.putLong("pos", helper.absolutePos(POS).asLong());
    payloadNbt.put("nbt", innerNbt);

    Networking.dispatchServerReceive(
      makePayload(Networking.PacketTileNotifyClientToServer.PACKET_ID, payloadNbt),
      helper.getLevel(),
      helper.makeMockPlayer(GameType.SURVIVAL));

    helper.succeed();
  }

  public static void dispatchServerReceiveContainerSyncCallsOnClientPacketReceived(GameTestHelper helper)
  {
    final int testCid = 77;
    final int[] calledWindowId = {-1};

    class TestContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer {
      TestContainer() { super(null, testCid); }
      @Override public void onServerPacketReceived(int wid, CompoundTag nbt) {}
      @Override public void onClientPacketReceived(int wid, Player p, CompoundTag nbt) { calledWindowId[0] = wid; }
      @Override public boolean stillValid(Player p) { return true; }
      @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    }

    final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    player.containerMenu = new TestContainer();

    final CompoundTag payloadNbt = new CompoundTag();
    payloadNbt.putInt("cid", testCid);
    payloadNbt.put("nbt", new CompoundTag());
    Networking.dispatchServerReceive(
      makePayload(Networking.PacketContainerSyncClientToServer.PACKET_ID, payloadNbt),
      helper.getLevel(),
      player);

    if(calledWindowId[0] != testCid)
      helper.fail("expected onClientPacketReceived with cid=" + testCid + " but got " + calledWindowId[0]);
    helper.succeed();
  }

  // --- dispatchClientReceive ---

  public static void dispatchClientReceiveNbtNotifyCallsRegisteredHandler(GameTestHelper helper)
  {
    final boolean[] called = {false};
    final String handlerKey = "test.s2c." + helper.getLevel().getGameTime();
    Networking.PacketNbtNotifyServerToClient.handlers.put(handlerKey, nbt -> called[0] = true);
    try {
      final CompoundTag payloadNbt = new CompoundTag();
      payloadNbt.putString("hnd", handlerKey);
      payloadNbt.put("nbt", new CompoundTag());
      Networking.dispatchClientReceive(
        makePayload(Networking.PacketNbtNotifyServerToClient.PACKET_ID, payloadNbt),
        helper.getLevel(),
        helper.makeMockPlayer(GameType.SURVIVAL).containerMenu);
      if(!called[0]) helper.fail("expected S2C NbtNotify handler to be called");
    } finally {
      Networking.PacketNbtNotifyServerToClient.handlers.remove(handlerKey);
    }
    helper.succeed();
  }

  public static void dispatchClientReceiveTileNotifyCallsOnServerPacketReceived(GameTestHelper helper)
  {
    helper.setBlock(POS, Registries.getBlock("control_box").defaultBlockState());
    final ControlBoxBlockEntity te = (ControlBoxBlockEntity) helper.getBlockEntity(POS);
    if(te == null) { helper.fail("expected control box block entity"); return; }

    final CompoundTag innerNbt = new CompoundTag();
    final CompoundTag logicNbt = new CompoundTag();
    logicNbt.putString("code", "b=9");
    innerNbt.put("logic", logicNbt);

    final CompoundTag payloadNbt = new CompoundTag();
    payloadNbt.putLong("pos", helper.absolutePos(POS).asLong());
    payloadNbt.put("nbt", innerNbt);

    Networking.dispatchClientReceive(
      makePayload(Networking.PacketTileNotifyServerToClient.PACKET_ID, payloadNbt),
      helper.getLevel(),
      helper.makeMockPlayer(GameType.SURVIVAL).containerMenu);

    if(!"b=9".equals(te.getCode()))
      helper.fail("expected code 'b=9' after S2C tile notify, got: " + te.getCode());
    helper.succeed();
  }

  public static void dispatchClientReceiveContainerSyncCallsOnServerPacketReceived(GameTestHelper helper)
  {
    final int testCid = 88;
    final int[] calledWindowId = {-1};

    class TestContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer {
      TestContainer() { super(null, testCid); }
      @Override public void onServerPacketReceived(int wid, CompoundTag nbt) { calledWindowId[0] = wid; }
      @Override public void onClientPacketReceived(int wid, Player p, CompoundTag nbt) {}
      @Override public boolean stillValid(Player p) { return true; }
      @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    }

    final CompoundTag payloadNbt = new CompoundTag();
    payloadNbt.putInt("cid", testCid);
    payloadNbt.put("nbt", new CompoundTag());
    Networking.dispatchClientReceive(
      makePayload(Networking.PacketContainerSyncServerToClient.PACKET_ID, payloadNbt),
      helper.getLevel(),
      new TestContainer());

    if(calledWindowId[0] != testCid)
      helper.fail("expected onServerPacketReceived with cid=" + testCid + " but got " + calledWindowId[0]);
    helper.succeed();
  }

  // ---

  private static Networking.UnifiedPayload makePayload(String packetId, CompoundTag nbt)
  {
    return new Networking.UnifiedPayload(new Networking.UnifiedPayload.UnifiedData(packetId, nbt));
  }
}
