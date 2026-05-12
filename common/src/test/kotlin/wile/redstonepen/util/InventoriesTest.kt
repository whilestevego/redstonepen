package wile.redstonepen.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class InventoriesTest :
    DescribeSpec({
        describe("areItemStacksIdenticalIgnoreDamage") {
            it("treats different durability as equivalent") {
                val a = ItemStack(Items.IRON_PICKAXE)
                val b = ItemStack(Items.IRON_PICKAXE)
                a.setDamageValue(5)
                b.setDamageValue(42)
                Inventories.areItemStacksIdenticalIgnoreDamage(a, b) shouldBe true
            }

            it("ignores components only present on the other stack") {
                val a = ItemStack(Items.IRON_PICKAXE)
                val b = ItemStack(Items.IRON_PICKAXE)
                a.setDamageValue(5)
                b.setDamageValue(42)
                b.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"))
                Inventories.areItemStacksIdenticalIgnoreDamage(a, b) shouldBe true
            }

            it("still respects shared non-damage components") {
                val a = ItemStack(Items.IRON_PICKAXE)
                val b = ItemStack(Items.IRON_PICKAXE)
                a.setDamageValue(5)
                b.setDamageValue(42)
                a.set(DataComponents.CUSTOM_NAME, Component.literal("Left"))
                b.set(DataComponents.CUSTOM_NAME, Component.literal("Right"))
                Inventories.areItemStacksIdenticalIgnoreDamage(a, b) shouldBe false
            }
        }

        describe("areItemStacksIdentical") {
            it("true for same item and components") {
                Inventories.areItemStacksIdentical(ItemStack(Items.REDSTONE, 5), ItemStack(Items.REDSTONE, 5)) shouldBe
                    true
            }

            it("false for different items") {
                Inventories.areItemStacksIdentical(ItemStack(Items.REDSTONE), ItemStack(Items.COAL)) shouldBe false
            }
        }

        describe("areItemStacksDifferent") {
            it("true when items differ") {
                Inventories.areItemStacksDifferent(ItemStack(Items.REDSTONE), ItemStack(Items.COAL)) shouldBe true
            }

            it("false for same item") {
                Inventories.areItemStacksDifferent(ItemStack(Items.REDSTONE, 3), ItemStack(Items.REDSTONE, 3)) shouldBe
                    false
            }
        }

        describe("isItemStackableOn") {
            it("false for empty source stack") {
                Inventories.isItemStackableOn(ItemStack.EMPTY, ItemStack(Items.REDSTONE)) shouldBe false
            }

            it("false for unstackable item") {
                Inventories.isItemStackableOn(ItemStack(Items.IRON_PICKAXE), ItemStack(Items.IRON_PICKAXE)) shouldBe
                    false
            }

            it("true for matching stackable items") {
                Inventories.isItemStackableOn(ItemStack(Items.REDSTONE, 3), ItemStack(Items.REDSTONE, 10)) shouldBe true
            }

            it("false for different items") {
                Inventories.isItemStackableOn(ItemStack(Items.REDSTONE), ItemStack(Items.COAL)) shouldBe false
            }
        }

        describe("copyOf") {
            it("clones contents instead of sharing item stacks") {
                val source = SimpleContainer(2)
                source.setItem(0, ItemStack(Items.REDSTONE, 3))
                val copy = Inventories.copyOf(source) as SimpleContainer
                copy.getItem(0).count = 1
                source.getItem(0).count shouldBe 3
                copy.getItem(0).count shouldBe 1
                copy.getItem(0) shouldNotBeSameInstanceAs source.getItem(0)
            }

            it("preserves container size") {
                val source = SimpleContainer(5)
                val copy = Inventories.copyOf(source) as SimpleContainer
                copy.containerSize shouldBe 5
            }
        }
    })
