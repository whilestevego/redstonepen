package wile.redstonepen.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class RsSignalsTest :
    DescribeSpec({
        describe("fromContainer") {
            it("returns zero for null containers") { RsSignals.fromContainer(null) shouldBe 0 }

            it("matches vanilla comparator-style fill level") {
                val container = SimpleContainer(2)
                container.setItem(0, ItemStack(Items.REDSTONE, 32))
                RsSignals.fromContainer(container) shouldBe 4
            }

            it("returns zero for empty container") {
                RsSignals.fromContainer(SimpleContainer(1)) shouldBe 0
            }

            it("returns 15 for full single slot") {
                val container = SimpleContainer(1)
                container.setItem(0, ItemStack(Items.REDSTONE, 64))
                RsSignals.fromContainer(container) shouldBe 15
            }

            it("applies non-empty bonus when fill rounds to zero") {
                val container = SimpleContainer(27)
                container.setItem(0, ItemStack(Items.REDSTONE, 1))
                RsSignals.fromContainer(container) shouldBe 1
            }

            it("returns 15 for full container") {
                val container = SimpleContainer(27)
                for (i in 0 until 27) container.setItem(i, ItemStack(Items.REDSTONE, 64))
                RsSignals.fromContainer(container) shouldBe 15
            }

            it("boundary: signal 1 at 4 items, 2 at 5 items in single slot") {
                val at4 = SimpleContainer(1)
                at4.setItem(0, ItemStack(Items.REDSTONE, 4))
                RsSignals.fromContainer(at4) shouldBe 1

                val at5 = SimpleContainer(1)
                at5.setItem(0, ItemStack(Items.REDSTONE, 5))
                RsSignals.fromContainer(at5) shouldBe 2
            }
        }
    })
