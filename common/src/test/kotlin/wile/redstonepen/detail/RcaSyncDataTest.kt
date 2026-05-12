package wile.redstonepen.detail

import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import wile.redstonepen.detail.RcaSync.CommonRca
import wile.redstonepen.detail.RcaSync.RcaData
import java.util.UUID

class RcaSyncDataTest {
    @Test fun rcaDataWithZeroUUIDIsInvalid() {
        assertFalse(RcaData(UUID(0, 0)).isValid())
    }

    @Test fun rcaDataWithNonZeroUUIDIsValid() {
        assertTrue(RcaData(UUID.randomUUID()).isValid())
    }

    @Test fun rcaDataClientInputsRoundTrips() {
        val data = RcaData(UUID.randomUUID())
        data.client_inputs(0xDEADBEEFCAFEBABEuL.toLong())
        assertEquals(0xDEADBEEFCAFEBABEuL.toLong(), data.client_inputs())
    }

    @Test fun rcaDataServerOutputsRoundTrips() {
        val data = RcaData(UUID.randomUUID())
        data.server_outputs(0x0102030405060708L)
        assertEquals(0x0102030405060708L, data.server_outputs())
    }

    @Test fun rcaDataToStringContainsHexClientInputs() {
        val data = RcaData(UUID.randomUUID())
        data.client_inputs(0xABCDL)
        assertTrue(data.toString().contains("000000000000abcd"))
    }

    @Test fun rcaDataToStringContainsHexServerOutputs() {
        val data = RcaData(UUID.randomUUID())
        data.server_outputs(0xFF00L)
        assertTrue(data.toString().contains("000000000000ff00"))
    }

    @Test fun commonRcaEmptyIsInvalid() {
        assertFalse(CommonRca.EMPTY.isValid())
    }

    @Test fun commonRcaOfPlayerNullReturnsEmpty() {
        assertSame(CommonRca.EMPTY, CommonRca.ofPlayer(null, false))
    }

    @Test fun commonRcaOfPlayerUnknownUUIDWithoutCreateReturnsEmpty() {
        assertSame(CommonRca.EMPTY, CommonRca.ofPlayer(UUID.randomUUID(), false))
    }

    @Test fun commonRcaOfPlayerCreatesEntryWhenAllowCreate() {
        val uid = UUID.randomUUID()
        val data = CommonRca.ofPlayer(uid, true)
        assertNotSame(CommonRca.EMPTY, data)
        assertTrue(data.isValid())
    }

    @Test fun commonRcaOfPlayerReturnsSameInstanceOnSubsequentCalls() {
        val uid = UUID.randomUUID()
        val first = CommonRca.ofPlayer(uid, true)
        val second = CommonRca.ofPlayer(uid, false)
        assertSame(first, second)
    }

    @Test fun rcaDataToStringContainsUuid() {
        val uid = UUID.randomUUID()
        assertTrue(RcaData(uid).toString().contains(uid.toString()))
    }

    @Test fun rcaDataToStringShowsZeroClientOutputs() {
        val data = RcaData(UUID.randomUUID())
        assertTrue(data.toString().contains("co:0000000000000000"))
    }

    @Test fun commonRcaApplyRcaUpdateIgnoresNbtWithoutInputKey() {
        val nbt = CompoundTag()
        nbt.putLong("x", 42L)
        CommonRca.applyRcaUpdate(UUID.randomUUID(), nbt)
        assertTrue(nbt.contains("x"))
        assertFalse(nbt.contains("o"))
    }

    @Test fun commonRcaApplyRcaUpdateAppliesClientInputsAndWritesServerOutputs() {
        val uid = UUID.randomUUID()
        val rca = CommonRca.ofPlayer(uid, true)
        rca.server_outputs(0xCAFEL)
        val nbt = CompoundTag()
        nbt.putLong("i", 0xDEADL)
        val result = CommonRca.applyRcaUpdate(uid, nbt)
        assertTrue(result)
        assertEquals(0xDEADL, rca.client_inputs())
        assertFalse(nbt.contains("i"))
        assertEquals(0xCAFEL, nbt.getLong("o"))
    }
}
