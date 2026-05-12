package wile.redstonepen.blocks

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.minecraft.core.Direction
import wile.redstonepen.blocks.track.RedstoneTrackDefs
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections

class RedstoneTrackDefsTest :
    DescribeSpec({
        describe("shape") {
            it("get returns non-null for every face mask") {
                for (faces in 0 until 64) {
                    RedstoneTrackDefs.shape.get(faces) shouldNotBe null
                }
            }

            it("get is cached for repeated calls") {
                RedstoneTrackDefs.shape.get(0x3F) shouldBe RedstoneTrackDefs.shape.get(0x3F)
            }

            it("no faces yields empty shape") { RedstoneTrackDefs.shape.get(0).isEmpty shouldBe true }

            it("any single face yields non-empty shape") {
                for (bit in 0 until 6) {
                    RedstoneTrackDefs.shape.get(1 shl bit).isEmpty shouldBe false
                }
            }
        }

        describe("models") {
            it("wire mapping has 25 entries") {
                RedstoneTrackDefs.models.STATE_WIRE_MAPPING.size shouldBe 25
            }

            it("connect mapping has 7 entries") {
                RedstoneTrackDefs.models.STATE_CONNECT_MAPPING.size shouldBe 7
            }
        }

        describe("connections") {
            it("bit order has six directions") {
                connections.CONNECTION_BIT_ORDER.size shouldBe 6
                connections.CONNECTION_BIT_ORDER_REV.size shouldBe 6
            }

            it("bulk connector bit matches forward mapping") {
                for (d in Direction.values()) {
                    val bit = connections.getBulkConnectorBit(d)
                    connections.BULK_FACE_MAPPING[bit] shouldBe d
                }
            }

            it("getWireBit yields known values") {
                connections.getWireBit(Direction.DOWN, Direction.NORTH) shouldBe 0x00000001L
                connections.getWireBit(Direction.WEST, Direction.SOUTH) shouldBe 0x00800000L
            }

            it("getWireBit returns zero for same-direction pair") {
                connections.getWireBit(Direction.DOWN, Direction.DOWN) shouldBe 0L
            }

            it("getWireBit side and direction round trip") {
                val bit = connections.getWireBit(Direction.NORTH, Direction.EAST)
                val t = connections.getWireBitSideAndDirection(bit)
                t.a shouldBe Direction.NORTH
                t.b shouldBe Direction.EAST
            }

            it("getWireBitSideAndDirection defaults to DOWN/DOWN for unknown bit") {
                val t = connections.getWireBitSideAndDirection(0xFFFFFFFFL)
                t.a shouldBe Direction.DOWN
                t.b shouldBe Direction.DOWN
            }

            it("getVanillaWireConnectionDirections returns all set sides") {
                val dirs = connections.getVanillaWireConnectionDirections(0x0FL)
                dirs.size shouldBe 4
                dirs shouldContain Direction.NORTH
                dirs shouldContain Direction.SOUTH
                dirs shouldContain Direction.EAST
                dirs shouldContain Direction.WEST
            }

            it("getVanillaWireConnectionDirections returns empty for zero mask") {
                connections.getVanillaWireConnectionDirections(0L).shouldBeEmpty()
            }

            it("hasVanillaWireConnection per side") {
                connections.hasVanillaWireConnection(0x01, Direction.NORTH) shouldBe true
                connections.hasVanillaWireConnection(0x02, Direction.SOUTH) shouldBe true
                connections.hasVanillaWireConnection(0x04, Direction.EAST) shouldBe true
                connections.hasVanillaWireConnection(0x08, Direction.WEST) shouldBe true
                connections.hasVanillaWireConnection(0x01, Direction.SOUTH) shouldBe false
                connections.hasVanillaWireConnection(0L, Direction.UP) shouldBe false
            }

            it("hasBulkConnection detects face bit") {
                val mask = connections.getBulkConnectorBit(Direction.EAST)
                connections.hasBulkConnection(mask, Direction.EAST) shouldBe true
                connections.hasBulkConnection(mask, Direction.WEST) shouldBe false
            }

            it("hasRedstoneConnection is true for all directions when all bits set") {
                val full = 0xFFFFFFFFL
                for (d in Direction.values()) {
                    connections.hasRedstoneConnection(full, d) shouldBe true
                }
            }

            it("hasRedstoneConnection is false for all directions when zero") {
                for (d in Direction.values()) {
                    connections.hasRedstoneConnection(0L, d) shouldBe false
                }
            }

            it("getWireElementsOnFace covers 4 bits per face") {
                for (d in Direction.values()) {
                    val mask = connections.getWireElementsOnFace(d)
                    java.lang.Long.bitCount(mask) shouldBe 4
                }
            }

            it("getAllElementsOnFace covers wires and connector") {
                for (d in Direction.values()) {
                    val mask = connections.getAllElementsOnFace(d)
                    java.lang.Long.bitCount(mask) shouldBe 5
                }
            }

            it("REDSTONE_UPDATE_DIRECTIONS contains all six faces") {
                RedstoneTrackDefs.REDSTONE_UPDATE_DIRECTIONS.size shouldBe 6
            }
        }
    })
