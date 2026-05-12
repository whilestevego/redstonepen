package wile.redstonepen.detail

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import java.util.UUID
import net.minecraft.nbt.CompoundTag
import wile.redstonepen.detail.RcaSync.CommonRca
import wile.redstonepen.detail.RcaSync.RcaData

class RcaSyncDataTest :
    DescribeSpec({
        describe("RcaData") {
            it("with zero UUID is invalid") { RcaData(UUID(0, 0)).isValid() shouldBe false }

            it("with non-zero UUID is valid") { RcaData(UUID.randomUUID()).isValid() shouldBe true }

            it("client inputs round-trip") {
                val data = RcaData(UUID.randomUUID())
                data.client_inputs(0xDEADBEEFCAFEBABEuL.toLong())
                data.client_inputs() shouldBe 0xDEADBEEFCAFEBABEuL.toLong()
            }

            it("server outputs round-trip") {
                val data = RcaData(UUID.randomUUID())
                data.server_outputs(0x0102030405060708L)
                data.server_outputs() shouldBe 0x0102030405060708L
            }

            it("toString contains hex client inputs") {
                val data = RcaData(UUID.randomUUID())
                data.client_inputs(0xABCDL)
                data.toString() shouldContain "000000000000abcd"
            }

            it("toString contains hex server outputs") {
                val data = RcaData(UUID.randomUUID())
                data.server_outputs(0xFF00L)
                data.toString() shouldContain "000000000000ff00"
            }

            it("toString contains UUID") {
                val uid = UUID.randomUUID()
                RcaData(uid).toString() shouldContain uid.toString()
            }

            it("toString shows zero client outputs") {
                val data = RcaData(UUID.randomUUID())
                data.toString() shouldContain "co:0000000000000000"
            }
        }

        describe("CommonRca") {
            it("EMPTY is invalid") { CommonRca.EMPTY.isValid() shouldBe false }

            it("ofPlayer with null returns EMPTY") {
                CommonRca.ofPlayer(null, false) shouldBeSameInstanceAs CommonRca.EMPTY
            }

            it("ofPlayer with unknown UUID and no create returns EMPTY") {
                CommonRca.ofPlayer(UUID.randomUUID(), false) shouldBeSameInstanceAs CommonRca.EMPTY
            }

            it("ofPlayer creates entry when allowCreate is true") {
                val uid = UUID.randomUUID()
                val data = CommonRca.ofPlayer(uid, true)
                data shouldNotBeSameInstanceAs CommonRca.EMPTY
                data.isValid() shouldBe true
            }

            it("ofPlayer returns same instance on subsequent calls") {
                val uid = UUID.randomUUID()
                val first = CommonRca.ofPlayer(uid, true)
                val second = CommonRca.ofPlayer(uid, false)
                second shouldBeSameInstanceAs first
            }

            it("applyRcaUpdate ignores NBT without input key") {
                val nbt = CompoundTag()
                nbt.putLong("x", 42L)
                CommonRca.applyRcaUpdate(UUID.randomUUID(), nbt)
                nbt.contains("x") shouldBe true
                nbt.contains("o") shouldBe false
            }

            it("applyRcaUpdate applies client inputs and writes server outputs") {
                val uid = UUID.randomUUID()
                val rca = CommonRca.ofPlayer(uid, true)
                rca.server_outputs(0xCAFEL)
                val nbt = CompoundTag()
                nbt.putLong("i", 0xDEADL)
                val result = CommonRca.applyRcaUpdate(uid, nbt)
                result shouldBe true
                rca.client_inputs() shouldBe 0xDEADL
                nbt.contains("i") shouldBe false
                nbt.getLong("o") shouldBe 0xCAFEL
            }
        }
    })
