package wile.redstonepen.detail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
