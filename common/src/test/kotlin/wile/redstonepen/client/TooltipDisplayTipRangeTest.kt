package wile.redstonepen.client

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TooltipDisplayTipRangeTest :
    DescribeSpec({
        describe("TipRange") {
            it("computes bounds from width and height") {
                val r = TooltipDisplay.TipRange(10, 20, 5, 3) { null }
                r.x0 shouldBe 10
                r.y0 shouldBe 20
                r.x1 shouldBe 14
                r.y1 shouldBe 22
            }

            it("single pixel width and height collapse bounds to origin") {
                val r = TooltipDisplay.TipRange(7, 4, 1, 1) { null }
                r.x0 shouldBe 7
                r.y0 shouldBe 4
                r.x1 shouldBe 7
                r.y1 shouldBe 4
            }

            it("zero origin gives correct bounds") {
                val r = TooltipDisplay.TipRange(0, 0, 10, 10) { null }
                r.x0 shouldBe 0
                r.y0 shouldBe 0
                r.x1 shouldBe 9
                r.y1 shouldBe 9
            }

            it("constructor delegates supplier") {
                val r = TooltipDisplay.TipRange(0, 0, 1, 1) { null }
                r.text shouldNotBe null
            }
        }
    })
