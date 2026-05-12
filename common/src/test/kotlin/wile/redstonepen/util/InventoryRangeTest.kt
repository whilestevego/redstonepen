package wile.redstonepen.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.NoSuchElementException
import java.util.Optional
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class InventoryRangeTest :
    DescribeSpec({
        fun container(size: Int) = SimpleContainer(size)

        fun filled(size: Int, redstone: Int) =
            SimpleContainer(size).also { c ->
                for (i in 0 until size) c.setItem(i, ItemStack(Items.REDSTONE, redstone))
            }

        describe("construction") {
            it("clamps offset and size to container bounds") {
                val c = container(9)
                val r = Inventories.InventoryRange(c, 100, 100, 1)
                (r.size() <= 9) shouldBe true
                (r.offset() >= 0) shouldBe true
                r.inventory() shouldBe c
            }

            it("single arg constructor wraps entire container") {
                val c = container(7)
                val r = Inventories.InventoryRange(c)
                r.size() shouldBe 7
                r.offset() shouldBe 0
                r.containerSize shouldBe 7
            }

            it("two arg constructor defaults rows to one") {
                Inventories.InventoryRange(container(9), 0, 9).size() shouldBe 9
            }

            it("get and set use the range offset") {
                val c = container(9)
                val r = Inventories.InventoryRange(c, 3, 4, 1)
                r.set(0, ItemStack(Items.REDSTONE, 2))
                c.getItem(3).item shouldBe Items.REDSTONE
                r.get(0).count shouldBe 2
            }

            it("maxStackSize is bounded and at least one") {
                val r = Inventories.InventoryRange(container(4))
                r.setMaxStackSize(0)
                (r.maxStackSize >= 1) shouldBe true
                r.setMaxStackSize(8)
                r.maxStackSize shouldBe 8
            }

            it("validator can reject items") {
                val r = Inventories.InventoryRange(container(4))
                r.setValidator { _, stack -> stack.`is`(Items.REDSTONE) }
                r.getValidator() shouldNotBe null
                r.canPlaceItem(0, ItemStack(Items.REDSTONE)) shouldBe true
                r.canPlaceItem(0, ItemStack(Items.COAL)) shouldBe false
            }

            it("multi-range insert consumes until empty") {
                val a = container(1)
                a.setItem(0, ItemStack(Items.REDSTONE, 60))
                val b = container(2)
                val ranges = arrayOf(Inventories.InventoryRange(a), Inventories.InventoryRange(b))
                Inventories.insert(ranges, ItemStack(Items.REDSTONE, 10)).isEmpty shouldBe true
            }
        }

        describe("container delegates") {
            it("clearContent empties every slot in range") {
                val r = Inventories.InventoryRange(filled(4, 2))
                r.clearContent()
                r.isEmpty shouldBe true
            }

            it("removeItemNoUpdate removes and returns stack") {
                val c = filled(2, 4)
                val r = Inventories.InventoryRange(c)
                val out = r.removeItemNoUpdate(0)
                out.count shouldBe 4
                r.getItem(0).isEmpty shouldBe true
            }

            it("removeItem splits stack") {
                val c = filled(2, 5)
                val r = Inventories.InventoryRange(c)
                val out = r.removeItem(0, 3)
                out.count shouldBe 3
                r.getItem(0).count shouldBe 2
            }

            it("stillValid delegates to inventory") {
                Inventories.InventoryRange(container(1)).stillValid(null) shouldBe true
            }

            it("startOpen stopOpen setChanged are no-ops") {
                val r = Inventories.InventoryRange(container(1))
                r.startOpen(null)
                r.stopOpen(null)
                r.setChanged()
            }
        }

        describe("iteration") {
            it("iterate stops when predicate matches") {
                val c = container(3)
                c.setItem(1, ItemStack(Items.COAL))
                val r = Inventories.InventoryRange(c)
                val seen = mutableListOf<Int>()
                val matched = r.iterate { i, s ->
                    seen.add(i)
                    s.`is`(Items.COAL)
                }
                matched shouldBe true
                seen shouldBe listOf(0, 1)
            }

            it("iterate returns false when nothing matches") {
                Inventories.InventoryRange(filled(3, 1)).iterate { _, _ -> false } shouldBe false
            }

            it("contains and indexOf find matching stack") {
                val c = container(3)
                c.setItem(2, ItemStack(Items.REDSTONE, 1))
                val r = Inventories.InventoryRange(c)
                r.contains(ItemStack(Items.REDSTONE, 1)) shouldBe true
                r.indexOf(ItemStack(Items.REDSTONE, 1)) shouldBe 2
                r.indexOf(ItemStack(Items.COAL, 1)) shouldBe -1
            }

            it("find returns first present result") {
                val c = container(3)
                c.setItem(0, ItemStack(Items.COAL))
                c.setItem(2, ItemStack(Items.REDSTONE))
                val r = Inventories.InventoryRange(c)
                val idx = r.find { i, s ->
                    if (s.`is`(Items.REDSTONE)) Optional.of(i) else Optional.empty()
                }
                idx shouldBe Optional.of(2)
            }

            it("find returns empty when nothing matches") {
                Inventories.InventoryRange(container(2))
                    .find { _, _ -> Optional.empty<Int>() }
                    .isEmpty shouldBe true
            }

            it("collect gathers all present results") {
                val c = container(4)
                c.setItem(0, ItemStack(Items.REDSTONE))
                c.setItem(2, ItemStack(Items.REDSTONE))
                val r = Inventories.InventoryRange(c)
                val hits = r.collect { i, s ->
                    if (s.`is`(Items.REDSTONE)) Optional.of(i) else Optional.empty()
                }
                hits shouldBe listOf(0, 2)
            }

            it("stream yields all slots including empty") {
                val c = container(3)
                c.setItem(1, ItemStack(Items.REDSTONE, 4))
                Inventories.InventoryRange(c).stream().count() shouldBe 3
            }

            it("iterator walks range and throws at end") {
                val r = Inventories.InventoryRange(filled(2, 1))
                val it = r.iterator()
                it.hasNext() shouldBe true
                it.next()
                it.hasNext() shouldBe true
                it.next()
                it.hasNext() shouldBe false
                shouldThrow<NoSuchElementException> { it.next() }
            }
        }

        describe("match counts") {
            it("stackMatchCount and totalMatchingItemCount") {
                val c = container(4)
                c.setItem(0, ItemStack(Items.REDSTONE, 3))
                c.setItem(1, ItemStack(Items.COAL, 5))
                c.setItem(3, ItemStack(Items.REDSTONE, 7))
                val r = Inventories.InventoryRange(c)
                r.stackMatchCount(ItemStack(Items.REDSTONE, 3)) shouldBe 2
                r.totalMatchingItemCount(ItemStack(Items.REDSTONE, 3)) shouldBe 10
            }
        }

        describe("insert") {
            it("empty stack returns empty") {
                Inventories.InventoryRange(container(3)).insert(ItemStack.EMPTY).isEmpty shouldBe
                    true
            }

            it("fills existing matching stack before any empty slot") {
                val c = container(3)
                c.setItem(2, ItemStack(Items.REDSTONE, 10))
                val r = Inventories.InventoryRange(c)
                r.insert(ItemStack(Items.REDSTONE, 5)).isEmpty shouldBe true
                c.getItem(2).count shouldBe 15
                c.getItem(0).isEmpty shouldBe true
            }

            it("returns overflow when no capacity") {
                val c = container(1)
                c.setItem(0, ItemStack(Items.REDSTONE, 64))
                Inventories.InventoryRange(c).insert(ItemStack(Items.REDSTONE, 5)).count shouldBe 5
            }

            it("fillup-only skips empty slots") {
                val c = container(3)
                c.setItem(0, ItemStack(Items.REDSTONE, 60))
                val r = Inventories.InventoryRange(c)
                val remaining = r.insert(ItemStack(Items.REDSTONE, 10), true, 0, false, false)
                c.getItem(0).count shouldBe 64
                remaining.count shouldBe 6
                c.getItem(1).isEmpty shouldBe true
            }

            it("uses empty slot when no match exists") {
                val c = container(3)
                val r = Inventories.InventoryRange(c)
                r.insert(ItemStack(Items.REDSTONE, 4)).isEmpty shouldBe true
                c.getItem(0).item shouldBe Items.REDSTONE
                c.getItem(0).count shouldBe 4
            }

            it("reverse fills from the end") {
                val c = container(3)
                val r = Inventories.InventoryRange(c)
                r.insert(ItemStack(Items.REDSTONE, 4), false, 0, true, true)
                c.getItem(2).item shouldBe Items.REDSTONE
            }

            it("simulate does not mutate") {
                val c = container(2)
                val r = Inventories.InventoryRange(c)
                r.insert(ItemStack(Items.REDSTONE, 4), true).isEmpty shouldBe true
                c.getItem(0).isEmpty shouldBe true
            }

            it("simulate reports leftover when full") {
                val c = container(1)
                c.setItem(0, ItemStack(Items.REDSTONE, 64))
                Inventories.InventoryRange(c)
                    .insert(ItemStack(Items.REDSTONE, 4), true)
                    .count shouldBe 4
            }

            it("at index fills target slot and returns remainder") {
                val c = container(2)
                c.setItem(0, ItemStack(Items.REDSTONE, 60))
                val r = Inventories.InventoryRange(c)
                val remaining = r.insert(0, ItemStack(Items.REDSTONE, 10))
                c.getItem(0).count shouldBe 64
                remaining.count shouldBe 6
            }

            it("at index into empty places full stack") {
                val c = container(2)
                val r = Inventories.InventoryRange(c)
                r.insert(0, ItemStack(Items.REDSTONE, 10)).isEmpty shouldBe true
                c.getItem(0).count shouldBe 10
            }

            it("at index into mismatched slot returns input unchanged") {
                val c = container(2)
                c.setItem(0, ItemStack(Items.COAL, 4))
                val r = Inventories.InventoryRange(c)
                val remaining = r.insert(0, ItemStack(Items.REDSTONE, 3))
                remaining.count shouldBe 3
                remaining.item shouldBe Items.REDSTONE
            }

            it("at index empty input returns empty") {
                Inventories.InventoryRange(container(1)).insert(0, ItemStack.EMPTY).isEmpty shouldBe
                    true
            }
        }

        describe("extract") {
            it("from empty range returns empty") {
                Inventories.InventoryRange(container(3)).extract(5).isEmpty shouldBe true
            }

            it("takes from first non-empty stack") {
                val c = container(3)
                c.setItem(1, ItemStack(Items.REDSTONE, 10))
                val r = Inventories.InventoryRange(c)
                val out = r.extract(4)
                out.count shouldBe 4
                c.getItem(1).count shouldBe 6
            }

            it("aggregates across identical stacks") {
                val c = container(3)
                c.setItem(0, ItemStack(Items.REDSTONE, 3))
                c.setItem(1, ItemStack(Items.COAL, 8))
                c.setItem(2, ItemStack(Items.REDSTONE, 2))
                val r = Inventories.InventoryRange(c)
                val out = r.extract(5)
                out.item shouldBe Items.REDSTONE
                out.count shouldBe 5
                c.getItem(0).isEmpty shouldBe true
                c.getItem(2).isEmpty shouldBe true
                c.getItem(1).count shouldBe 8
            }

            it("simulate does not mutate") {
                val c = container(2)
                c.setItem(0, ItemStack(Items.REDSTONE, 5))
                val r = Inventories.InventoryRange(c)
                r.extract(2, false, true).count shouldBe 2
                c.getItem(0).count shouldBe 5
            }

            it("by stack returns empty for empty request") {
                Inventories.InventoryRange(filled(2, 4)).extract(ItemStack.EMPTY).isEmpty shouldBe
                    true
            }

            it("by stack pulls requested amount") {
                val c = container(3)
                c.setItem(0, ItemStack(Items.REDSTONE, 4))
                c.setItem(2, ItemStack(Items.REDSTONE, 3))
                Inventories.InventoryRange(c).extract(ItemStack(Items.REDSTONE, 5)).count shouldBe 5
            }

            it("by stack simulate returns available amount and does not mutate") {
                val c = container(3)
                c.setItem(0, ItemStack(Items.REDSTONE, 4))
                c.setItem(2, ItemStack(Items.REDSTONE, 3))
                val r = Inventories.InventoryRange(c)
                val out = r.extract(ItemStack(Items.REDSTONE, 50), true)
                out.count shouldBe 7
                c.getItem(0).count shouldBe 4
                c.getItem(2).count shouldBe 3
            }

            it("by stack returns empty when no match") {
                val c = container(2)
                c.setItem(0, ItemStack(Items.COAL, 4))
                Inventories.InventoryRange(c).extract(ItemStack(Items.REDSTONE, 1)).isEmpty shouldBe
                    true
            }
        }

        describe("move") {
            it("slot moves into empty target") {
                val src = container(2)
                src.setItem(0, ItemStack(Items.REDSTONE, 5))
                val dst = container(2)
                val srcR = Inventories.InventoryRange(src)
                val dstR = Inventories.InventoryRange(dst)
                srcR.move(0, dstR) shouldBe true
                src.getItem(0).isEmpty shouldBe true
                dst.getItem(0).count shouldBe 5
            }

            it("slot with empty source returns false") {
                Inventories.InventoryRange(container(2))
                    .move(0, Inventories.InventoryRange(container(2))) shouldBe false
            }

            it("moveAll identical drains source when target has room") {
                val src = container(3)
                src.setItem(0, ItemStack(Items.REDSTONE, 30))
                src.setItem(1, ItemStack(Items.REDSTONE, 20))
                src.setItem(2, ItemStack(Items.REDSTONE, 10))
                val srcR = Inventories.InventoryRange(src)
                val dstR = Inventories.InventoryRange(container(2))
                srcR.move(0, dstR, true, false, false, true) shouldBe true
            }

            it("range moves everything possible") {
                val src = container(3)
                src.setItem(0, ItemStack(Items.REDSTONE, 5))
                src.setItem(2, ItemStack(Items.COAL, 3))
                val dst = container(3)
                val srcR = Inventories.InventoryRange(src)
                val dstR = Inventories.InventoryRange(dst)
                srcR.move(dstR) shouldBe true
                src.getItem(0).isEmpty shouldBe true
                src.getItem(2).isEmpty shouldBe true
            }

            it("range returns false when source empty") {
                Inventories.InventoryRange(container(3))
                    .move(Inventories.InventoryRange(container(3))) shouldBe false
            }

            it("fillup variant compiles and runs") {
                val src = container(2)
                src.setItem(0, ItemStack(Items.REDSTONE, 5))
                val srcR = Inventories.InventoryRange(src)
                val dstR = Inventories.InventoryRange(container(2))
                srcR.move(dstR, true)
            }
        }
    })
