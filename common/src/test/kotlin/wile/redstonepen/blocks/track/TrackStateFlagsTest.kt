package wile.redstonepen.blocks.track

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import net.minecraft.core.Direction
import wile.redstonepen.blocks.track.RedstoneTrackDefs.Connections
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_CON_MASK
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_CON_POS
import wile.redstonepen.blocks.track.RedstoneTrackDefs.STATE_FLAG_WIR_MASK

class TrackStateFlagsTest :
    DescribeSpec({
        describe("EMPTY") {
            it("has raw value zero") { TrackStateFlags.EMPTY.raw shouldBe 0L }

            it("wireFlags is zero") { TrackStateFlags.EMPTY.wireFlags shouldBe 0 }

            it("connectionFlags is zero") { TrackStateFlags.EMPTY.connectionFlags shouldBe 0 }

            it("all side powers are zero") {
                assertSoftly {
                    for (side in Direction.entries) {
                        TrackStateFlags.EMPTY.sidePower(side) shouldBe 0
                    }
                }
            }
        }

        describe("wireFlag / wireFlags") {
            it("bit 0 is false on EMPTY") { TrackStateFlags.EMPTY.wireFlag(0) shouldBe false }

            it("bit 0 is true when set") { TrackStateFlags(1L).wireFlag(0) shouldBe true }

            it("bit 23 is true when set") { TrackStateFlags(1L shl 23).wireFlag(23) shouldBe true }

            it("bit 23 is false when not set") { TrackStateFlags(0L).wireFlag(23) shouldBe false }

            it("wireFlags masks upper bits") {
                TrackStateFlags(STATE_FLAG_WIR_MASK.inv()).wireFlags shouldBe 0
            }

            it("wireFlags returns all 24 bits when all wire bits set") {
                TrackStateFlags(STATE_FLAG_WIR_MASK).wireFlags shouldBe STATE_FLAG_WIR_MASK.toInt()
            }
        }

        describe("connectionFlag / connectionFlags") {
            it("index 0 is false on EMPTY") {
                TrackStateFlags.EMPTY.connectionFlag(0) shouldBe false
            }

            it("index 0 is true when bit 24 set") {
                TrackStateFlags(1L shl STATE_FLAG_CON_POS).connectionFlag(0) shouldBe true
            }

            it("index 5 is true when bit 29 set") {
                TrackStateFlags(1L shl (STATE_FLAG_CON_POS + 5)).connectionFlag(5) shouldBe true
            }

            it("connectionFlags shifts bits down to a 6-bit int") {
                val allCon = TrackStateFlags(STATE_FLAG_CON_MASK)
                allCon.connectionFlags shouldBe 0x3f
            }

            it("connectionFlags ignores wire and power bits") {
                TrackStateFlags(STATE_FLAG_WIR_MASK or (STATE_FLAG_CON_MASK.inv()))
                    .connectionFlags shouldBe 0
            }
        }

        describe("sidePower") {
            it("returns 0 for every side on EMPTY") {
                for (side in Direction.entries) {
                    TrackStateFlags.EMPTY.sidePower(side) shouldBe 0
                }
            }

            it("returns 15 for a side set to max power") {
                for (side in Direction.entries) {
                    val flags = TrackStateFlags.EMPTY.withSidePower(side, 15)
                    flags.sidePower(side) shouldBe 15
                }
            }

            it("does not bleed into adjacent side nibbles") {
                val order = Connections.CONNECTION_BIT_ORDER
                for (i in order.indices) {
                    val side = order[i]
                    val flags = TrackStateFlags.EMPTY.withSidePower(side, 15)
                    for (other in order.indices) {
                        if (other != i) {
                            flags.sidePower(order[other]) shouldBe 0
                        }
                    }
                }
            }
        }

        describe("withSidePower") {
            it("round-trips all levels 0..15 for each side") {
                for (side in Direction.entries) {
                    for (p in 0..15) {
                        TrackStateFlags.EMPTY.withSidePower(side, p).sidePower(side) shouldBe p
                    }
                }
            }

            it("truncates bits above 15 (only low 4 bits stored)") {
                val flags = TrackStateFlags.EMPTY.withSidePower(Direction.DOWN, 0x1f)
                flags.sidePower(Direction.DOWN) shouldBe 0xf
            }

            it("overwriting a side does not disturb other sides") {
                val base =
                    TrackStateFlags.EMPTY.withSidePower(Direction.DOWN, 7)
                        .withSidePower(Direction.NORTH, 3)
                val updated = base.withSidePower(Direction.DOWN, 12)
                assertSoftly {
                    updated.sidePower(Direction.DOWN) shouldBe 12
                    updated.sidePower(Direction.NORTH) shouldBe 3
                    updated.sidePower(Direction.UP) shouldBe 0
                }
            }

            it("round-trips any Int value masked to 0..15 via property test") {
                checkAll<Int> { raw ->
                    val p = raw and 0xf
                    TrackStateFlags.EMPTY.withSidePower(Direction.EAST, p)
                        .sidePower(Direction.EAST) shouldBe p
                }
            }
        }

        describe("withAddedWireFlags") {
            it("adding zero flags changes nothing and returns count 0") {
                val (result, count) = TrackStateFlags.EMPTY.withAddedWireFlags(0L)
                assertSoftly {
                    result.raw shouldBe 0L
                    count shouldBe 0
                }
            }

            it("adding a single new bit sets it and returns count 1") {
                val (result, count) = TrackStateFlags.EMPTY.withAddedWireFlags(0x1L)
                assertSoftly {
                    result.wireFlag(0) shouldBe true
                    count shouldBe 1
                }
            }

            it("adding an already-set bit is idempotent with count 0") {
                val base = TrackStateFlags(0x1L)
                val (result, count) = base.withAddedWireFlags(0x1L)
                assertSoftly {
                    result.raw shouldBe base.raw
                    count shouldBe 0
                }
            }

            it("ignores bits outside STATE_FLAG_WIR_MASK") {
                val nonWireBit = 1L shl 30
                val (result, count) = TrackStateFlags.EMPTY.withAddedWireFlags(nonWireBit)
                assertSoftly {
                    result.raw shouldBe 0L
                    count shouldBe 0
                }
            }

            it("count equals the number of newly set bits") {
                val (_, count) = TrackStateFlags.EMPTY.withAddedWireFlags(0x7L)
                count shouldBe 3
            }
        }

        describe("withBitsCleared / withBitsSet / hasBits") {
            it("withBitsCleared removes targeted bits") {
                val flags = TrackStateFlags(0xffL)
                flags.withBitsCleared(0x0fL).raw shouldBe 0xf0L
            }

            it("withBitsCleared does not disturb unmasked bits") {
                val flags = TrackStateFlags(0xffL)
                flags.withBitsCleared(0x0fL).raw shouldBe 0xf0L
            }

            it("withBitsSet adds targeted bits") {
                TrackStateFlags.EMPTY.withBitsSet(0x5L).raw shouldBe 0x5L
            }

            it("withBitsSet does not clear existing bits") {
                TrackStateFlags(0xaL).withBitsSet(0x5L).raw shouldBe 0xfL
            }

            it("hasBits returns true when at least one bit in mask matches") {
                TrackStateFlags(0x3L).hasBits(0x2L) shouldBe true
            }

            it("hasBits returns false when no bit in mask matches") {
                TrackStateFlags(0x1L).hasBits(0x2L) shouldBe false
            }

            it("hasBits returns false on EMPTY for any mask") {
                TrackStateFlags.EMPTY.hasBits(0xffffL) shouldBe false
            }
        }
    })
