package wile.redstonepen.detail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import wile.redstonepen.detail.RcaSync.CommonRca;
import wile.redstonepen.detail.RcaSync.RcaData;

import java.util.UUID;

class RcaSyncDataTest
{
  // --- RcaData ---

  @Test
  void rcaDataWithZeroUUIDIsInvalid()
  {
    assertFalse(new RcaData(new UUID(0, 0)).isValid());
  }

  @Test
  void rcaDataWithNonZeroUUIDIsValid()
  {
    assertTrue(new RcaData(UUID.randomUUID()).isValid());
  }

  @Test
  void rcaDataClientInputsRoundTrips()
  {
    final RcaData data = new RcaData(UUID.randomUUID());
    data.client_inputs(0xDEADBEEFCAFEBABEL);
    assertEquals(0xDEADBEEFCAFEBABEL, data.client_inputs());
  }

  @Test
  void rcaDataServerOutputsRoundTrips()
  {
    final RcaData data = new RcaData(UUID.randomUUID());
    data.server_outputs(0x0102030405060708L);
    assertEquals(0x0102030405060708L, data.server_outputs());
  }

  @Test
  void rcaDataToStringContainsHexClientInputs()
  {
    final RcaData data = new RcaData(UUID.randomUUID());
    data.client_inputs(0xABCDL);
    // toString uses %016x format
    assertTrue(data.toString().contains("000000000000abcd"),
      "toString must contain hex-encoded client_inputs");
  }

  @Test
  void rcaDataToStringContainsHexServerOutputs()
  {
    final RcaData data = new RcaData(UUID.randomUUID());
    data.server_outputs(0xFF00L);
    assertTrue(data.toString().contains("000000000000ff00"),
      "toString must contain hex-encoded server_outputs");
  }

  // --- CommonRca ---

  @Test
  void commonRcaEmptyIsInvalid()
  {
    assertFalse(CommonRca.EMPTY.isValid());
  }

  @Test
  void commonRcaOfPlayerNullReturnsEmpty()
  {
    assertSame(CommonRca.EMPTY, CommonRca.ofPlayer(null, false));
  }

  @Test
  void commonRcaOfPlayerUnknownUUIDWithoutCreateReturnsEmpty()
  {
    assertSame(CommonRca.EMPTY, CommonRca.ofPlayer(UUID.randomUUID(), false));
  }

  @Test
  void commonRcaOfPlayerCreatesEntryWhenAllowCreate()
  {
    final UUID uid = UUID.randomUUID();
    final RcaData data = CommonRca.ofPlayer(uid, true);
    assertNotSame(CommonRca.EMPTY, data);
    assertTrue(data.isValid());
  }

  @Test
  void commonRcaOfPlayerReturnsSameInstanceOnSubsequentCalls()
  {
    final UUID uid = UUID.randomUUID();
    final RcaData first  = CommonRca.ofPlayer(uid, true);
    final RcaData second = CommonRca.ofPlayer(uid, false);
    // Cache must return the same object — structural equality (Kotlin data class)
    // on the UUID would break this if RcaData were ever converted to a data class.
    assertSame(first, second);
  }

  @Test
  void rcaDataToStringContainsUuid()
  {
    final UUID uid = UUID.randomUUID();
    assertTrue(new RcaData(uid).toString().contains(uid.toString()),
      "toString must include the UUID string");
  }

  @Test
  void rcaDataToStringShowsZeroClientOutputs()
  {
    final RcaData data = new RcaData(UUID.randomUUID());
    // client_outputs_ starts at 0; toString renders it as the "co:" field
    assertTrue(data.toString().contains("co:0000000000000000"),
      "toString must include co: field with zero-padded hex");
  }

  @Test
  void commonRcaApplyRcaUpdateIgnoresNbtWithoutInputKey()
  {
    final CompoundTag nbt = new CompoundTag();
    nbt.putLong("x", 42L);
    CommonRca.applyRcaUpdate(UUID.randomUUID(), nbt);
    // no "i" key — must return false and leave nbt unchanged
    assertTrue(nbt.contains("x"));
    assertFalse(nbt.contains("o"));
  }

  @Test
  void commonRcaApplyRcaUpdateAppliesClientInputsAndWritesServerOutputs()
  {
    final UUID uid = UUID.randomUUID();
    final RcaData rca = CommonRca.ofPlayer(uid, true);
    rca.server_outputs(0xCAFEL);
    final CompoundTag nbt = new CompoundTag();
    nbt.putLong("i", 0xDEADL);
    final boolean result = CommonRca.applyRcaUpdate(uid, nbt);
    assertTrue(result, "applyRcaUpdate must return true when 'i' key is present");
    assertEquals(0xDEADL, rca.client_inputs(), "client_inputs must be updated from nbt 'i'");
    assertFalse(nbt.contains("i"), "'i' key must be removed after processing");
    assertEquals(0xCAFEL, nbt.getLong("o"), "'o' key must reflect server_outputs");
  }
}
