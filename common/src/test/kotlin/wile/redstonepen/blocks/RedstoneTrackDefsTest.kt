package wile.redstonepen.blocks

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.minecraft.core.Direction
import wile.redstonepen.blocks.track.RedstoneTrackDefs
import wile.redstonepen.blocks.track.RedstoneTrackDefs.Connections

class RedstoneTrackDefsTest :
    DescribeSpec({
        describe("shape") {
            it("get returns non-null for every face mask") {
                for (faces in 0 until 64) {
                    RedstoneTrackDefs.Shape.get(faces) shouldNotBe null
                }
            }

            it("get is cached for repeated calls") {
                RedstoneTrackDefs.Shape.get(0x3F) shouldBe RedstoneTrackDefs.Shape.get(0x3F)
            }

            it("no faces yields empty shape") {
                RedstoneTrackDefs.Shape.get(0).isEmpty shouldBe true
            }

            it("any single face yields non-empty shape") {
                for (bit in 0 until 6) {
                    RedstoneTrackDefs.Shape.get(1 shl bit).isEmpty shouldBe false
                }
            }
        }

        describe("models") {
            it("wire mapping has 25 entries") {
                RedstoneTrackDefs.Models.STATE_WIRE_MAPPING.size shouldBe 25
            }

            it("connect mapping has 7 entries") {
                RedstoneTrackDefs.Models.STATE_CONNECT_MAPPING.size shouldBe 7
            }
        }

        describe("connections") {
            it("bit order has six directions") {
                assertSoftly {
                    Connections.CONNECTION_BIT_ORDER.size shouldBe 6
                    Connections.CONNECTION_BIT_ORDER_REV.size shouldBe 6
                }
            }

            it("bulk connector bit matches forward mapping") {
                assertSoftly {
                    for (d in Direction.values()) {
                        val bit = Connections.getBulkConnectorBit(d)
                        Connections.BULK_FACE_MAPPING[bit] shouldBe d
                    }
                }
            }

            it("getWireBit yields known values") {
                assertSoftly {
                    Connections.getWireBit(Direction.DOWN, Direction.NORTH) shouldBe 0x00000001L
                    Connections.getWireBit(Direction.WEST, Direction.SOUTH) shouldBe 0x00800000L
                }
            }

            it("getWireBit returns zero for same-direction pair") {
                Connections.getWireBit(Direction.DOWN, Direction.DOWN) shouldBe 0L
            }

            it("getWireBit side and direction round trip") {
                val bit = Connections.getWireBit(Direction.NORTH, Direction.EAST)
                val t = Connections.getWireBitSideAndDirection(bit)
                assertSoftly {
                    t.a shouldBe Direction.NORTH
                    t.b shouldBe Direction.EAST
                }
            }

            it("getWireBitSideAndDirection defaults to DOWN/DOWN for unknown bit") {
                val t = Connections.getWireBitSideAndDirection(0xFFFFFFFFL)
                assertSoftly {
                    t.a shouldBe Direction.DOWN
                    t.b shouldBe Direction.DOWN
                }
            }

            it("getVanillaWireConnectionDirections returns all set sides") {
                val dirs = Connections.getVanillaWireConnectionDirections(0x0FL)
                assertSoftly {
                    dirs.size shouldBe 4
                    dirs shouldContain Direction.NORTH
                    dirs shouldContain Direction.SOUTH
                    dirs shouldContain Direction.EAST
                    dirs shouldContain Direction.WEST
                }
            }

            it("getVanillaWireConnectionDirections returns empty for zero mask") {
                Connections.getVanillaWireConnectionDirections(0L).shouldBeEmpty()
            }

            it("hasVanillaWireConnection per side") {
                assertSoftly {
                    Connections.hasVanillaWireConnection(0x01, Direction.NORTH) shouldBe true
                    Connections.hasVanillaWireConnection(0x02, Direction.SOUTH) shouldBe true
                    Connections.hasVanillaWireConnection(0x04, Direction.EAST) shouldBe true
                    Connections.hasVanillaWireConnection(0x08, Direction.WEST) shouldBe true
                    Connections.hasVanillaWireConnection(0x01, Direction.SOUTH) shouldBe false
                    Connections.hasVanillaWireConnection(0L, Direction.UP) shouldBe false
                }
            }

            it("hasBulkConnection detects face bit") {
                val mask = Connections.getBulkConnectorBit(Direction.EAST)
                assertSoftly {
                    Connections.hasBulkConnection(mask, Direction.EAST) shouldBe true
                    Connections.hasBulkConnection(mask, Direction.WEST) shouldBe false
                }
            }

            it("hasRedstoneConnection is true for all directions when all bits set") {
                val full = 0xFFFFFFFFL
                assertSoftly {
                    for (d in Direction.values()) {
                        Connections.hasRedstoneConnection(full, d) shouldBe true
                    }
                }
            }

            it("hasRedstoneConnection is false for all directions when zero") {
                assertSoftly {
                    for (d in Direction.values()) {
                        Connections.hasRedstoneConnection(0L, d) shouldBe false
                    }
                }
            }

            it("getWireElementsOnFace covers 4 bits per face") {
                assertSoftly {
                    for (d in Direction.values()) {
                        val mask = Connections.getWireElementsOnFace(d)
                        java.lang.Long.bitCount(mask) shouldBe 4
                    }
                }
            }

            it("getAllElementsOnFace covers wires and connector") {
                assertSoftly {
                    for (d in Direction.values()) {
                        val mask = Connections.getAllElementsOnFace(d)
                        java.lang.Long.bitCount(mask) shouldBe 5
                    }
                }
            }

            it("REDSTONE_UPDATE_DIRECTIONS contains all six faces") {
                RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS.size shouldBe 6
            }

            it("getWireElementsOnFace masks are disjoint across all faces") {
                val masks = Direction.values().map { Connections.getWireElementsOnFace(it) }
                assertSoftly {
                    for (i in masks.indices) {
                        for (j in masks.indices) {
                            if (i != j) (masks[i] and masks[j]) shouldBe 0L
                        }
                    }
                }
            }

            it("getAllElementsOnFace masks are disjoint across all faces") {
                val masks = Direction.values().map { Connections.getAllElementsOnFace(it) }
                assertSoftly {
                    for (i in masks.indices) {
                        for (j in masks.indices) {
                            if (i != j) (masks[i] and masks[j]) shouldBe 0L
                        }
                    }
                }
            }
        }
    })
