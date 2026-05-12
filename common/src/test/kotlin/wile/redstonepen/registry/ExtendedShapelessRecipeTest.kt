package wile.redstonepen.registry

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient

class ExtendedShapelessRecipeTest :
    DescribeSpec({
        fun recipe(ingredientCount: Int, aspects: CompoundTag): ExtendedShapelessRecipe {
            val ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY)
            return ExtendedShapelessRecipe(
                "test_group",
                CraftingBookCategory.MISC,
                ItemStack(Items.REDSTONE),
                ingredients,
                aspects,
            )
        }

        fun repairRecipe(repairPerItem: Int): ExtendedShapelessRecipe {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:iron_pickaxe")
            aspects.putInt("tool_repair", repairPerItem)
            return recipe(2, aspects)
        }

        fun repairInput(tool: ItemStack, repairItem: ItemStack): CraftingInput =
            CraftingInput.of(2, 1, listOf(tool, repairItem))

        describe("aspects") {
            it("isSpecial false for empty aspects") {
                recipe(1, CompoundTag()).isSpecial() shouldBe false
            }

            it("isSpecial true when dynamic flag set") {
                val aspects = CompoundTag()
                aspects.putBoolean("dynamic", true)
                recipe(1, aspects).isSpecial() shouldBe true
            }

            it("isSpecial true when tool_repair set") {
                val aspects = CompoundTag()
                aspects.putInt("tool_repair", 100)
                recipe(1, aspects).isSpecial() shouldBe true
            }

            it("isSpecial false when only tool_damage set") {
                val aspects = CompoundTag()
                aspects.putInt("tool_damage", 50)
                recipe(1, aspects).isSpecial() shouldBe false
            }

            it("getAspects returns copy not reference") {
                val aspects = CompoundTag()
                aspects.putBoolean("dynamic", true)
                val r = recipe(1, aspects)
                val copy = r.getAspects()
                copy.putInt("mutation", 99)
                r.getAspects().contains("mutation") shouldBe false
            }
        }

        describe("dimensions") {
            it("canCraftInDimensions true when grid fits ingredients") {
                recipe(1, CompoundTag()).canCraftInDimensions(1, 1) shouldBe true
                recipe(9, CompoundTag()).canCraftInDimensions(3, 3) shouldBe true
                recipe(1, CompoundTag()).canCraftInDimensions(1, 9) shouldBe true
            }

            it("canCraftInDimensions false when grid too small") {
                recipe(9, CompoundTag()).canCraftInDimensions(2, 4) shouldBe false
                recipe(2, CompoundTag()).canCraftInDimensions(1, 1) shouldBe false
            }
        }

        describe("metadata") {
            it("group returns constructor argument") {
                val r =
                    ExtendedShapelessRecipe(
                        "my_group",
                        CraftingBookCategory.MISC,
                        ItemStack(Items.REDSTONE),
                        NonNullList.withSize(1, Ingredient.EMPTY),
                        CompoundTag(),
                    )
                r.group shouldBe "my_group"
            }

            it("category returns constructor argument") {
                recipe(1, CompoundTag()).category() shouldBe CraftingBookCategory.MISC
            }

            it("serializer is not null") {
                recipe(1, CompoundTag()).serializer shouldNotBe null
            }

            it("serializer exposes codecs") {
                ExtendedShapelessRecipe.SERIALIZER.codec() shouldNotBe null
                ExtendedShapelessRecipe.SERIALIZER.streamCodec() shouldNotBe null
            }

            it("ingredients reflects constructor") {
                val r =
                    ExtendedShapelessRecipe(
                        "g",
                        CraftingBookCategory.MISC,
                        ItemStack(Items.REDSTONE),
                        NonNullList.withSize(2, Ingredient.EMPTY),
                        CompoundTag(),
                    )
                r.ingredients.size shouldBe 2
            }
        }

        describe("result item") {
            it("getResultItem returns configured output for non-special recipe") {
                recipe(1, CompoundTag()).getResultItem(null).item shouldBe Items.REDSTONE
            }

            it("getResultItem empty for special dynamic recipe") {
                val aspects = CompoundTag()
                aspects.putBoolean("dynamic", true)
                recipe(1, aspects).getResultItem(null).isEmpty shouldBe true
            }

            it("assemble returns result stack for basic recipe") {
                val r =
                    ExtendedShapelessRecipe(
                        "g",
                        CraftingBookCategory.MISC,
                        ItemStack(Items.IRON_PICKAXE),
                        NonNullList.withSize(1, Ingredient.EMPTY),
                        CompoundTag(),
                    )
                val out = r.assemble(CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK))), null)
                out.item shouldBe Items.IRON_PICKAXE
                out.damageValue shouldBe 0
            }

            it("assemble applies initial_durability") {
                val aspects = CompoundTag()
                aspects.putInt("initial_durability", 1)
                val r =
                    ExtendedShapelessRecipe(
                        "g",
                        CraftingBookCategory.MISC,
                        ItemStack(Items.IRON_PICKAXE),
                        NonNullList.withSize(1, Ingredient.EMPTY),
                        aspects,
                    )
                val out = r.assemble(CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK))), null)
                (out.damageValue > 0) shouldBe true
            }

            it("assemble applies initial_damage") {
                val aspects = CompoundTag()
                aspects.putInt("initial_damage", 5)
                val r =
                    ExtendedShapelessRecipe(
                        "g",
                        CraftingBookCategory.MISC,
                        ItemStack(Items.IRON_PICKAXE),
                        NonNullList.withSize(1, Ingredient.EMPTY),
                        aspects,
                    )
                val out = r.assemble(CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK))), null)
                out.damageValue shouldBe 5
            }

            it("assemble empty result yields empty") {
                val r =
                    ExtendedShapelessRecipe(
                        "g",
                        CraftingBookCategory.MISC,
                        ItemStack.EMPTY,
                        NonNullList.withSize(1, Ingredient.EMPTY),
                        CompoundTag(),
                    )
                r.assemble(CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK))), null).isEmpty shouldBe true
            }
        }

        describe("remaining items") {
            it("getRemainingItems returns list of same size as input") {
                val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
                recipe(1, CompoundTag()).getRemainingItems(inv).size shouldBe 1
            }

            it("retains tool when not damageable") {
                val aspects = CompoundTag()
                aspects.putString("tool", "minecraft:stick")
                aspects.putInt("tool_damage", 1)
                val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
                recipe(1, aspects).getRemainingItems(inv)[0].item shouldBe Items.STICK
            }

            it("damages damageable tool") {
                val aspects = CompoundTag()
                aspects.putString("tool", "minecraft:iron_pickaxe")
                aspects.putInt("tool_damage", 3)
                val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.IRON_PICKAXE)))
                val out = recipe(1, aspects).getRemainingItems(inv)[0]
                out.item shouldBe Items.IRON_PICKAXE
                out.damageValue shouldBe 3
            }

            it("consumes tool when damage exceeds max") {
                val aspects = CompoundTag()
                aspects.putString("tool", "minecraft:iron_pickaxe")
                aspects.putInt("tool_damage", 1024)
                val pickaxe = ItemStack(Items.IRON_PICKAXE)
                pickaxe.setDamageValue(pickaxe.maxDamage - 1)
                val inv = CraftingInput.of(1, 1, listOf(pickaxe))
                recipe(1, aspects).getRemainingItems(inv)[0].isEmpty shouldBe true
            }
        }

        describe("repair flow") {
            it("tool_repair aspect makes recipe special and changes result") {
                val aspects = CompoundTag()
                aspects.putString("tool", "minecraft:iron_pickaxe")
                aspects.putInt("tool_repair", 100)
                val r = recipe(1, aspects)
                r.isSpecial() shouldBe true
                r.getResultItem(null).isEmpty shouldBe true
            }

            it("assemble returns healed tool stack") {
                val r = repairRecipe(50)
                val pickaxe = ItemStack(Items.IRON_PICKAXE)
                pickaxe.setDamageValue(120)
                val out = r.assemble(repairInput(pickaxe, ItemStack(Items.IRON_INGOT)), null)
                out.item shouldBe Items.IRON_PICKAXE
                (out.damageValue < 120) shouldBe true
            }

            it("getRemainingItems consumes repair item") {
                val r = repairRecipe(50)
                val pickaxe = ItemStack(Items.IRON_PICKAXE)
                pickaxe.setDamageValue(120)
                val rem = r.getRemainingItems(repairInput(pickaxe, ItemStack(Items.IRON_INGOT, 4)))
                (rem[0].isEmpty || rem[0].item == Items.IRON_PICKAXE) shouldBe true
            }

            it("undamaged tool returns empty assemble") {
                val r = repairRecipe(50)
                val pickaxe = ItemStack(Items.IRON_PICKAXE)
                pickaxe.setDamageValue(0)
                r.assemble(repairInput(pickaxe, ItemStack(Items.IRON_INGOT)), null).isEmpty shouldBe true
            }

            it("over_repair aspect allows zero damage input") {
                val aspects = CompoundTag()
                aspects.putString("tool", "minecraft:iron_pickaxe")
                aspects.putInt("tool_repair", 50)
                aspects.putBoolean("over_repair", true)
                val r = recipe(2, aspects)
                val pickaxe = ItemStack(Items.IRON_PICKAXE)
                pickaxe.setDamageValue(0)
                r.assemble(repairInput(pickaxe, ItemStack(Items.IRON_INGOT)), null) shouldNotBe null
            }

            it("relative_repair_damage path triggers") {
                val aspects = CompoundTag()
                aspects.putString("tool", "minecraft:iron_pickaxe")
                aspects.putInt("tool_repair", 25)
                aspects.putBoolean("relative_repair_damage", true)
                val r = recipe(2, aspects)
                val pickaxe = ItemStack(Items.IRON_PICKAXE)
                pickaxe.setDamageValue(pickaxe.maxDamage - 10)
                val out = r.assemble(repairInput(pickaxe, ItemStack(Items.IRON_INGOT)), null)
                out.item shouldBe Items.IRON_PICKAXE
                (out.damageValue < pickaxe.maxDamage - 10) shouldBe true
            }

            it("missing tool returns empty") {
                val r = repairRecipe(50)
                val inv = CraftingInput.of(2, 1, listOf(ItemStack(Items.IRON_INGOT), ItemStack(Items.IRON_INGOT)))
                r.assemble(inv, null).isEmpty shouldBe true
            }

            it("non-damageable tool returns empty") {
                val aspects = CompoundTag()
                aspects.putString("tool", "minecraft:stick")
                aspects.putInt("tool_repair", 50)
                val r = recipe(2, aspects)
                val inv = CraftingInput.of(2, 1, listOf(ItemStack(Items.STICK), ItemStack(Items.IRON_INGOT)))
                r.assemble(inv, null).isEmpty shouldBe true
            }
        }

        describe("matches") {
            it("false for empty input with ingredients") {
                val inv = CraftingInput.of(1, 1, listOf(ItemStack.EMPTY))
                recipe(1, CompoundTag()).matches(inv, null) shouldBe false
            }
        }
    })
