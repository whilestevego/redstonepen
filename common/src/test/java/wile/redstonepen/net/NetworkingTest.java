package wile.redstonepen.net;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetworkingTest
{
  @Test
  void unifiedPayloadRoundTripPreservesIdAndNbt()
  {
    final String id = "testpacket";
    final CompoundTag nbt = new CompoundTag();
    nbt.putInt("value", 42);
    nbt.putString("key", "hello");
    final Networking.UnifiedPayload original = new Networking.UnifiedPayload(
      new Networking.UnifiedPayload.UnifiedData(id, nbt));

    final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    Networking.UnifiedPayload.STREAM_CODEC.encode(buf, original);
    buf.resetReaderIndex();
    final Networking.UnifiedPayload decoded = Networking.UnifiedPayload.STREAM_CODEC.decode(buf);

    assertEquals(id, decoded.data().id());
    assertEquals(42, decoded.data().nbt().getInt("value"));
    assertEquals("hello", decoded.data().nbt().getString("key"));
  }

  @Test
  void unifiedPayloadRoundTripWithEmptyNbt()
  {
    final Networking.UnifiedPayload original = new Networking.UnifiedPayload(
      new Networking.UnifiedPayload.UnifiedData("tnc2s", new CompoundTag()));

    final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
    Networking.UnifiedPayload.STREAM_CODEC.encode(buf, original);
    buf.resetReaderIndex();
    final Networking.UnifiedPayload decoded = Networking.UnifiedPayload.STREAM_CODEC.decode(buf);

    assertEquals("tnc2s", decoded.data().id());
    assertTrue(decoded.data().nbt().isEmpty());
  }

  @Test
  void unifiedDataToStringContainsId()
  {
    final Networking.UnifiedPayload payload = new Networking.UnifiedPayload(
      new Networking.UnifiedPayload.UnifiedData("mypacketid", new CompoundTag()));
    assertTrue(payload.data().toString().contains("mypacketid"));
  }
}
