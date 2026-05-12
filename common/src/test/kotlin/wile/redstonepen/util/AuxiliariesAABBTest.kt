package wile.redstonepen.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB

class AuxiliariesAABBTest :
    DescribeSpec({
        fun assertAABB(expected: AABB, actual: AABB) {
            val eps = 1e-9
            actual.minX shouldBe (expected.minX plusOrMinus eps)
            actual.minY shouldBe (expected.minY plusOrMinus eps)
            actual.minZ shouldBe (expected.minZ plusOrMinus eps)
            actual.maxX shouldBe (expected.maxX plusOrMinus eps)
            actual.maxY shouldBe (expected.maxY plusOrMinus eps)
            actual.maxZ shouldBe (expected.maxZ plusOrMinus eps)
        }

        describe("getPixeledAABB") {
            it("divides all coordinates by sixteen") {
                assertAABB(
                    AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
                    Auxiliaries.getPixeledAABB(0.0, 0.0, 0.0, 16.0, 16.0, 16.0),
                )
            }

            it("preserves asymmetric bounds") {
                assertAABB(
                    AABB(0.25, 0.5, 0.0, 0.75, 1.0, 0.25),
                    Auxiliaries.getPixeledAABB(4.0, 8.0, 0.0, 12.0, 16.0, 4.0),
                )
            }

            it("handles partial pixels") {
                val bb = Auxiliaries.getPixeledAABB(2.0, 4.0, 6.0, 10.0, 12.0, 14.0)
                val eps = 1e-9
                bb.minX shouldBe (2.0 / 16.0 plusOrMinus eps)
                bb.minY shouldBe (4.0 / 16.0 plusOrMinus eps)
                bb.minZ shouldBe (6.0 / 16.0 plusOrMinus eps)
                bb.maxX shouldBe (10.0 / 16.0 plusOrMinus eps)
                bb.maxY shouldBe (12.0 / 16.0 plusOrMinus eps)
                bb.maxZ shouldBe (14.0 / 16.0 plusOrMinus eps)
            }
        }

        describe("getRotatedAABB") {
            it("NORTH is identity") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.NORTH))
            }

            it("SOUTH mirrors X and Z") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7), Auxiliaries.getRotatedAABB(bb, Direction.SOUTH))
            }

            it("DOWN swaps Y and Z") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.4, 0.3, 0.2, 0.9, 0.8, 0.7), Auxiliaries.getRotatedAABB(bb, Direction.DOWN))
            }

            it("UP inverts all three axes") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8), Auxiliaries.getRotatedAABB(bb, Direction.UP))
            }

            it("WEST swaps X and Z") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.3, 0.2, 0.4, 0.8, 0.7, 0.9), Auxiliaries.getRotatedAABB(bb, Direction.WEST))
            }

            it("EAST swaps X and Z") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.2, 0.2, 0.1, 0.7, 0.7, 0.6), Auxiliaries.getRotatedAABB(bb, Direction.EAST))
            }

            it("all six faces produce valid boxes") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9)
                for (d in Direction.values()) {
                    val out = Auxiliaries.getRotatedAABB(bb, d)
                    out shouldNotBe null
                    (out.maxX > out.minX) shouldBe true
                    (out.maxY > out.minY) shouldBe true
                    (out.maxZ > out.minZ) shouldBe true
                }
            }

            it("array overload preserves length") {
                val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), AABB(0.2, 0.2, 0.2, 0.8, 0.8, 0.8))
                Auxiliaries.getRotatedAABB(inn, Direction.EAST).size shouldBe inn.size
            }

            it("array overload handles empty input") {
                Auxiliaries.getRotatedAABB(emptyArray<AABB>(), Direction.NORTH).size shouldBe 0
            }
        }

        describe("getRotatedAABB horizontal") {
            it("keeps Y axis") {
                val bb = AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9)
                val south = Auxiliaries.getRotatedAABB(bb, Direction.SOUTH, true)
                val eps = 1e-9
                south.minY shouldBe (bb.minY plusOrMinus eps)
                south.maxY shouldBe (bb.maxY plusOrMinus eps)
            }

            it("DOWN and UP are identity") {
                val bb = AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9)
                assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.DOWN, true))
                assertAABB(bb, Auxiliaries.getRotatedAABB(bb, Direction.UP, true))
            }

            it("all six faces produce valid boxes") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9)
                for (d in Direction.values()) {
                    val out = Auxiliaries.getRotatedAABB(bb, d, true)
                    out shouldNotBe null
                    (out.maxX > out.minX) shouldBe true
                    (out.maxY > out.minY) shouldBe true
                    (out.maxZ > out.minZ) shouldBe true
                }
            }

            it("array overload preserves length") {
                val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                Auxiliaries.getRotatedAABB(inn, Direction.SOUTH, true).size shouldBe inn.size
            }
        }

        describe("getYRotatedAABB") {
            it("zero steps is identity") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(bb, Auxiliaries.getYRotatedAABB(bb, 0))
            }

            it("four steps is identity") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(bb, Auxiliaries.getYRotatedAABB(bb, 4))
            }

            it("two steps is 180 degrees") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.4, 0.2, 0.2, 0.9, 0.7, 0.7), Auxiliaries.getYRotatedAABB(bb, 2))
            }

            it("handles negative and modular steps") {
                val bb = AABB(0.1, 0.25, 0.3, 0.7, 0.85, 0.9)
                val step1 = Auxiliaries.getYRotatedAABB(bb, 1)
                assertAABB(step1, Auxiliaries.getYRotatedAABB(bb, 5))
                assertAABB(step1, Auxiliaries.getYRotatedAABB(bb, -3))
            }

            it("all four quarters preserve Y and produce valid boxes") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.9)
                val eps = 1e-9
                for (q in 0 until 4) {
                    val out = Auxiliaries.getYRotatedAABB(bb, q)
                    out shouldNotBe null
                    out.minY shouldBe (bb.minY plusOrMinus eps)
                    out.maxY shouldBe (bb.maxY plusOrMinus eps)
                }
            }

            it("array overload preserves length") {
                val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                Auxiliaries.getYRotatedAABB(inn, 2).size shouldBe inn.size
            }

            it("array overload handles empty input") {
                Auxiliaries.getYRotatedAABB(emptyArray<AABB>(), 1).size shouldBe 0
            }
        }

        describe("getMirroredAABB") {
            it("X axis swaps X coordinates") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.4, 0.2, 0.3, 0.9, 0.7, 0.8), Auxiliaries.getMirroredAABB(bb, Direction.Axis.X))
            }

            it("Y axis swaps Y coordinates") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.1, 0.3, 0.3, 0.6, 0.8, 0.8), Auxiliaries.getMirroredAABB(bb, Direction.Axis.Y))
            }

            it("Z axis swaps Z coordinates") {
                val bb = AABB(0.1, 0.2, 0.3, 0.6, 0.7, 0.8)
                assertAABB(AABB(0.1, 0.2, 0.2, 0.6, 0.7, 0.7), Auxiliaries.getMirroredAABB(bb, Direction.Axis.Z))
            }

            it("array overload preserves length") {
                val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0), AABB(0.2, 0.2, 0.2, 0.8, 0.8, 0.8))
                Auxiliaries.getMirroredAABB(inn, Direction.Axis.X).size shouldBe inn.size
            }

            it("array overload handles empty input") {
                Auxiliaries.getMirroredAABB(emptyArray<AABB>(), Direction.Axis.X).size shouldBe 0
            }
        }

        describe("getMappedAABB") {
            it("applies mapper") {
                val inn = arrayOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                assertAABB(AABB(2.0, 0.0, 0.0, 3.0, 1.0, 1.0), Auxiliaries.getMappedAABB(inn) { b -> b.move(2.0, 0.0, 0.0) }[0])
            }

            it("handles empty input") {
                Auxiliaries.getMappedAABB(emptyArray<AABB>()) { b -> b }.size shouldBe 0
            }
        }

        describe("getUnionShape") {
            it("single AABB is non-empty") {
                Auxiliaries.getUnionShape(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)).isEmpty shouldBe false
            }

            it("array of arrays is non-empty") {
                val s =
                    Auxiliaries.getUnionShape(
                        arrayOf(AABB(0.0, 0.0, 0.0, 0.5, 1.0, 1.0)),
                        arrayOf(AABB(0.5, 0.0, 0.0, 1.0, 1.0, 1.0)),
                    )
                s.isEmpty shouldBe false
            }

            it("array of arrays handles empty input") {
                Auxiliaries.getUnionShape(*Array(0) { emptyArray<AABB>() }).isEmpty shouldBe true
            }
        }
    })
