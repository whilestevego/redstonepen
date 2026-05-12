package wile.redstonepen.client

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import wile.redstonepen.client.Guis.Coord2d

class GuisCoord2dTest :
    DescribeSpec({
        it("constructor stores x and y") {
            val c = Coord2d(3, 7)
            c.x shouldBe 3
            c.y shouldBe 7
        }

        it("of factory creates equivalent coord") {
            val a = Coord2d(5, 9)
            val b = Coord2d.of(5, 9)
            b.x shouldBe a.x
            b.y shouldBe a.y
        }

        it("ORIGIN is zero zero") {
            Coord2d.ORIGIN.x shouldBe 0
            Coord2d.ORIGIN.y shouldBe 0
        }

        it("toString formats as [x,y]") { Coord2d(3, 4).toString() shouldBe "[3,4]" }

        it("toString for zero zero is [0,0]") { Coord2d.ORIGIN.toString() shouldBe "[0,0]" }

        it("toString handles negative values") { Coord2d(-1, -2).toString() shouldBe "[-1,-2]" }
    })
