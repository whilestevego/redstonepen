package wile.redstonepen.blocks

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import wile.redstonepen.blocks.track.TrackNet

class TrackNetTest :
    DescribeSpec({
        describe("TrackNet") {
            it("stores all construction parameters") {
                val positions = listOf(BlockPos(1, 2, 3))
                val nbSides = listOf(Direction.DOWN)
                val intSides = listOf(Direction.UP)
                val pwrSides = listOf(Direction.NORTH)
                val net = TrackNet(positions, nbSides, intSides, pwrSides, 7)
                assertSoftly {
                    net.neighbour_positions shouldBe positions
                    net.neighbour_sides shouldBe nbSides
                    net.internal_sides shouldBe intSides
                    net.power_sides shouldBe pwrSides
                    net.power shouldBe 7
                }
            }

            it("default power is zero") {
                val net = TrackNet(emptyList(), emptyList(), emptyList(), emptyList())
                net.power shouldBe 0
            }

            it("power can be mutated") {
                val net = TrackNet(emptyList(), emptyList(), emptyList(), emptyList(), 5)
                net.power = 10
                net.power shouldBe 10
            }

            it("toString contains NET block and power value") {
                val net = TrackNet(emptyList(), emptyList(), emptyList(), emptyList(), 4)
                val s = net.toString()
                assertSoftly {
                    s shouldContain "NET{"
                    s shouldContain "p:4"
                }
            }

            it("empty lists produce valid toString") {
                val net = TrackNet(emptyList(), emptyList(), emptyList(), emptyList())
                val s = net.toString()
                s shouldContain "NET{"
            }
        }
    })
