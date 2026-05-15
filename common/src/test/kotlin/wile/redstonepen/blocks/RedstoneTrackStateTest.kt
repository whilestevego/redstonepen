package wile.redstonepen.blocks

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import net.minecraft.core.Direction
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_CON_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_PWR_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_WIR_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections
import wile.redstonepen.blocks.track.TestHooks

class RedstoneTrackStateTest :
    DescribeSpec({
        fun h() = TestHooks()

        describe("mask constants") {
            it("masks don't overlap") {
                assertSoftly {
                    (STATE_FLAG_WIR_MASK and STATE_FLAG_CON_MASK) shouldBe 0L
                    (STATE_FLAG_WIR_MASK and STATE_FLAG_PWR_MASK) shouldBe 0L
                    (STATE_FLAG_CON_MASK and STATE_FLAG_PWR_MASK) shouldBe 0L
                }
            }
        }

        describe("wire flags") {
            it("zero state returns zero") { h().getWireFlags() shouldBe 0 }

            it("ignores upper bits") {
                val th = h()
                th.state = STATE_FLAG_WIR_MASK.inv()
                th.getWireFlags() shouldBe 0
            }

            it("bit 0 true when set") {
                val th = h()
                th.state = 1L
                th.getWireFlag(0) shouldBe true
            }

            it("bit 0 false when clear") { h().getWireFlag(0) shouldBe false }

            it("bit 23 true when set") {
                val th = h()
                th.state = 1L shl 23
                th.getWireFlag(23) shouldBe true
            }

            it("flag count is 24") { h().getWireFlagCount() shouldBe 24 }

            it("does not increment for already-set bit") {
                val th = h()
                th.state = 1L
                th.addWireFlags(1L) shouldBe 0
            }

            withData((0 until 24).toList()) { i -> h().addWireFlags(1L shl i) shouldBe 1 }

            it("addWireFlags all at once sets all 24") {
                val th = h()
                th.addWireFlags(STATE_FLAG_WIR_MASK)
                th.getWireFlags() shouldBe 0x00ffffff
            }
        }

        describe("connection flags") {
            it("zero state returns zero") { h().getConnectionFlags() shouldBe 0 }

            it("only bit 24 set returns one") {
                val th = h()
                th.state = 1L shl 24
                th.getConnectionFlags() shouldBe 1
            }

            withData(0, 1, 2, 3, 4, 5) { i ->
                val th = h()
                th.state = 1L shl (24 + i)
                th.getConnectionFlag(i) shouldBe true
                for (j in 0 until 6) {
                    if (j != i) th.getConnectionFlag(j) shouldBe false
                }
            }

            it("flag count is 6") { h().getConnectionFlagCount() shouldBe 6 }
        }

        describe("side power") {
            withData(Direction.values().toList()) { dir -> h().getSidePower(dir) shouldBe 0 }

            context("round trips at max") {
                withData(Direction.values().toList()) { dir ->
                    val th = h()
                    th.setSidePower(dir, 15)
                    th.getSidePower(dir) shouldBe 15
                }
            }

            context("round trips at zero after max") {
                withData(Direction.values().toList()) { dir ->
                    val th = h()
                    th.setSidePower(dir, 15)
                    th.setSidePower(dir, 0)
                    th.getSidePower(dir) shouldBe 0
                }
            }

            it("does not corrupt adjacent direction") {
                val dirs = Direction.values()
                for (i in dirs.indices) {
                    val th = h()
                    for (d in dirs) th.setSidePower(d, 15)
                    th.setSidePower(dirs[i], 0)
                    assertSoftly {
                        for (j in dirs.indices) {
                            val expected = if (j == i) 0 else 15
                            th.getSidePower(dirs[j]) shouldBe expected
                        }
                    }
                }
            }

            it("truncates to 4 bits") {
                val th = h()
                th.setSidePower(Direction.DOWN, 16)
                th.getSidePower(Direction.DOWN) shouldBe 0
            }
        }

        describe("dust count") {
            it("zero state returns zero") { h().getRedstoneDustCount() shouldBe 0 }

            it("all wire bits set returns 24") {
                val th = h()
                th.state = STATE_FLAG_WIR_MASK
                th.getRedstoneDustCount() shouldBe 24
            }

            it("all connection bits set, zero wire bits returns 6") {
                val th = h()
                th.state = STATE_FLAG_CON_MASK
                th.getRedstoneDustCount() shouldBe 6
            }

            withData((0..24).toList()) { k ->
                val th = h()
                th.state = (1L shl k) - 1
                th.getRedstoneDustCount() shouldBe k
            }
        }

        describe("side power all values") {
            context("round trips for all values 0..15") {
                withData(Direction.values().toList()) { dir ->
                    checkAll(Arb.int(0..15)) { v ->
                        val th = h()
                        th.setSidePower(dir, v)
                        th.getSidePower(dir) shouldBe v
                    }
                }
            }

            it("all six directions hold distinct values simultaneously") {
                val dirs = Direction.values()
                val th = h()
                dirs.forEachIndexed { i, dir -> th.setSidePower(dir, i + 1) }
                assertSoftly {
                    dirs.forEachIndexed { i, dir -> th.getSidePower(dir) shouldBe i + 1 }
                }
            }
        }

        describe("connection flags by direction") {
            it("CONNECTION_BIT_ORDER index matches getConnectionFlag(index)") {
                assertSoftly {
                    connections.CONNECTION_BIT_ORDER.forEachIndexed { i, dir ->
                        val th = h()
                        th.state = 1L shl (24 + i)
                        th.getConnectionFlag(i) shouldBe true
                        connections.CONNECTION_BIT_ORDER_REV[dir] shouldBe i
                    }
                }
            }
        }

        describe("static mappings") {
            it("WIRE_FACE_DIRECTION_MAPPING has 24 entries") {
                connections.WIRE_FACE_DIRECTION_MAPPING.size - 1 shouldBe 24
            }

            it("WIRE_FACE_DIRECTION_MAPPING keys are distinct powers of two") {
                val seen = mutableSetOf<Long>()
                for (key in connections.WIRE_FACE_DIRECTION_MAPPING.keys) {
                    if (key == 0L) continue
                    java.lang.Long.bitCount(key) shouldBe 1
                    seen.add(key) shouldBe true
                }
            }

            it("BULK_FACE_MAPPING has one entry per face plus zero") {
                connections.BULK_FACE_MAPPING.size shouldBe 7
            }

            it("BULK_FACE_MAPPING_REV non-null for all directions") {
                for (dir in Direction.values()) {
                    connections.BULK_FACE_MAPPING_REV[dir] shouldNotBe null
                }
            }

            it("CONNECTION_BIT_ORDER_REV has all six directions") {
                connections.CONNECTION_BIT_ORDER_REV.size shouldBe 6
                for (dir in Direction.values()) {
                    connections.CONNECTION_BIT_ORDER_REV.containsKey(dir) shouldBe true
                }
            }
        }
    })
