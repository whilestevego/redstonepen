package wile.redstonepen.commands

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.minecraft.core.BlockPos

class DemoBuilderTest :
    DescribeSpec({
        describe("cellOrigin") {
            it("index zero returns grid origin") {
                val origin = BlockPos(10, 64, 20)
                DemoBuilder.cellOrigin(origin, 0, 4, 3) shouldBe origin
            }

            it("advances across columns before advancing row") {
                val origin = BlockPos(0, 0, 0)
                DemoBuilder.cellOrigin(origin, 1, 4, 3) shouldBe BlockPos(3, 0, 0)
                DemoBuilder.cellOrigin(origin, 2, 4, 3) shouldBe BlockPos(6, 0, 0)
                DemoBuilder.cellOrigin(origin, 3, 4, 3) shouldBe BlockPos(9, 0, 0)
                DemoBuilder.cellOrigin(origin, 4, 4, 3) shouldBe BlockPos(0, 0, 3)
                DemoBuilder.cellOrigin(origin, 5, 4, 3) shouldBe BlockPos(3, 0, 3)
            }

            it("preserves y coordinate") {
                DemoBuilder.cellOrigin(BlockPos(0, 100, 0), 7, 3, 5).y shouldBe 100
            }

            it("rejects zero columns") {
                shouldThrow<IllegalArgumentException> {
                    DemoBuilder.cellOrigin(BlockPos.ZERO, 0, 0, 1)
                }
            }

            it("rejects zero spacing") {
                shouldThrow<IllegalArgumentException> {
                    DemoBuilder.cellOrigin(BlockPos.ZERO, 0, 4, 0)
                }
            }

            it("rejects negative index") {
                shouldThrow<IllegalArgumentException> {
                    DemoBuilder.cellOrigin(BlockPos.ZERO, -1, 4, 3)
                }
            }
        }

        describe("regionVolume") {
            it("is inclusive on both ends") {
                DemoBuilder.regionVolume(BlockPos.ZERO, BlockPos.ZERO) shouldBe 1
                DemoBuilder.regionVolume(BlockPos.ZERO, BlockPos(1, 1, 1)) shouldBe 8
                DemoBuilder.regionVolume(BlockPos.ZERO, BlockPos(2, 2, 2)) shouldBe 27
            }

            it("is order independent") {
                val a = BlockPos(5, 5, 5)
                val b = BlockPos(0, 0, 0)
                DemoBuilder.regionVolume(a, b) shouldBe DemoBuilder.regionVolume(b, a)
            }
        }
    })
