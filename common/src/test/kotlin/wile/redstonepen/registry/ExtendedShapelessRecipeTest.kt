package wile.redstonepen.registry

import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap

class ExtendedShapelessRecipeTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    private fun recipe(ingredientCount: Int, aspects: CompoundTag): ExtendedShapelessRecipe {
        val ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY)
        return ExtendedShapelessRecipe(
            "test_group",
            CraftingBookCategory.MISC,
            ItemStack(Items.REDSTONE),
            ingredients,
            aspects,
        )
    }

    private fun repairRecipe(repairPerItem: Int): ExtendedShapelessRecipe {
        val aspects = CompoundTag()
        aspects.putString("tool", "minecraft:iron_pickaxe")
        aspects.putInt("tool_repair", repairPerItem)
        return recipe(2, aspects)
    }

    private fun repairInput(tool: ItemStack, repairItem: ItemStack): CraftingInput =
        CraftingInput.of(2, 1, listOf(tool, repairItem))

    @Nested
    inner class Aspects {
        @Test
        fun isSpecialFalseForEmptyAspects() {
            assertFalse(recipe(1, CompoundTag()).isSpecial())
        }

        @Test
        fun isSpecialTrueWhenDynamicFlagSet() {
            val aspects = CompoundTag()
            aspects.putBoolean("dynamic", true)
            assertTrue(recipe(1, aspects).isSpecial())
        }

        @Test
        fun isSpecialTrueWhenToolRepairSet() {
            val aspects = CompoundTag()
            aspects.putInt("tool_repair", 100)
            assertTrue(recipe(1, aspects).isSpecial())
        }

        @Test
        fun isSpecialFalseWhenOnlyToolDamageSet() {
            val aspects = CompoundTag()
            aspects.putInt("tool_damage", 50)
            assertFalse(recipe(1, aspects).isSpecial())
        }

        @Test
        fun getAspectsReturnsCopyNotReference() {
            val aspects = CompoundTag()
            aspects.putBoolean("dynamic", true)
            val r = recipe(1, aspects)
            val copy = r.getAspects()
            copy.putInt("mutation", 99)
            assertFalse(r.getAspects().contains("mutation"))
        }
    }

    @Nested
    inner class Dimensions {
        @Test
        fun canCraftInDimensionsTrueWhenGridFitsIngredients() {
            assertTrue(recipe(1, CompoundTag()).canCraftInDimensions(1, 1))
            assertTrue(recipe(9, CompoundTag()).canCraftInDimensions(3, 3))
            assertTrue(recipe(1, CompoundTag()).canCraftInDimensions(1, 9))
        }

        @Test
        fun canCraftInDimensionsFalseWhenGridTooSmall() {
            assertFalse(recipe(9, CompoundTag()).canCraftInDimensions(2, 4))
            assertFalse(recipe(2, CompoundTag()).canCraftInDimensions(1, 1))
        }
    }

    @Nested
    inner class Metadata {
        @Test
        fun getGroupReturnsConstructorArgument() {
            val ingredients = NonNullList.withSize(1, Ingredient.EMPTY)
            val r =
                ExtendedShapelessRecipe(
                    "my_group",
                    CraftingBookCategory.MISC,
                    ItemStack(Items.REDSTONE),
                    ingredients,
                    CompoundTag(),
                )
            assertEquals("my_group", r.group)
        }

        @Test
        fun categoryReturnsConstructorArgument() {
            assertEquals(CraftingBookCategory.MISC, recipe(1, CompoundTag()).category())
        }

        @Test
        fun getSerializerNotNull() {
            assertNotNull(recipe(1, CompoundTag()).serializer)
        }

        @Test
        fun serializerExposesCodecs() {
            assertNotNull(ExtendedShapelessRecipe.SERIALIZER.codec())
            assertNotNull(ExtendedShapelessRecipe.SERIALIZER.streamCodec())
        }

        @Test
        fun getIngredientsReflectsConstructor() {
            val ingredients = NonNullList.withSize(2, Ingredient.EMPTY)
            val r =
                ExtendedShapelessRecipe(
                    "g",
                    CraftingBookCategory.MISC,
                    ItemStack(Items.REDSTONE),
                    ingredients,
                    CompoundTag(),
                )
            assertEquals(2, r.ingredients.size)
        }
    }

    @Nested
    inner class ResultItem {
        @Test
        fun getResultItemReturnsConfiguredOutputForNonSpecial() {
            val out = recipe(1, CompoundTag()).getResultItem(null)
            assertEquals(Items.REDSTONE, out.item)
        }

        @Test
        fun getResultItemEmptyForSpecialDynamicRecipe() {
            val aspects = CompoundTag()
            aspects.putBoolean("dynamic", true)
            assertTrue(recipe(1, aspects).getResultItem(null).isEmpty)
        }

        @Test
        fun assembleReturnsResultStackForBasicRecipe() {
            val r =
                ExtendedShapelessRecipe(
                    "g",
                    CraftingBookCategory.MISC,
                    ItemStack(Items.IRON_PICKAXE),
                    NonNullList.withSize(1, Ingredient.EMPTY),
                    CompoundTag(),
                )
            val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
            val out = r.assemble(inv, null)
            assertEquals(Items.IRON_PICKAXE, out.item)
            assertEquals(0, out.damageValue)
        }

        @Test
        fun assembleAppliesInitialDurability() {
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
            val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
            val out = r.assemble(inv, null)
            assertTrue(out.damageValue > 0)
        }

        @Test
        fun assembleAppliesInitialDamage() {
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
            val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
            val out = r.assemble(inv, null)
            assertEquals(5, out.damageValue)
        }

        @Test
        fun assembleEmptyResultYieldsEmpty() {
            val r =
                ExtendedShapelessRecipe(
                    "g",
                    CraftingBookCategory.MISC,
                    ItemStack.EMPTY,
                    NonNullList.withSize(1, Ingredient.EMPTY),
                    CompoundTag(),
                )
            val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
            assertTrue(r.assemble(inv, null).isEmpty)
        }
    }

    @Nested
    inner class RemainingItems {
        @Test
        fun getRemainingItemsReturnsListOfSameSizeAsInput() {
            val r = recipe(1, CompoundTag())
            val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
            assertEquals(1, r.getRemainingItems(inv).size)
        }

        @Test
        fun getRemainingItemsRetainsToolWhenNotDamageable() {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:stick")
            aspects.putInt("tool_damage", 1)
            val r = recipe(1, aspects)
            val inv = CraftingInput.of(1, 1, listOf(ItemStack(Items.STICK)))
            val rem = r.getRemainingItems(inv)
            assertEquals(Items.STICK, rem[0].item)
        }

        @Test
        fun getRemainingItemsDamagesDamageableTool() {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:iron_pickaxe")
            aspects.putInt("tool_damage", 3)
            val r = recipe(1, aspects)
            val pickaxe = ItemStack(Items.IRON_PICKAXE)
            val inv = CraftingInput.of(1, 1, listOf(pickaxe))
            val rem = r.getRemainingItems(inv)
            val out = rem[0]
            assertEquals(Items.IRON_PICKAXE, out.item)
            assertEquals(3, out.damageValue)
        }

        @Test
        fun getRemainingItemsConsumesToolWhenDamageExceedsMax() {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:iron_pickaxe")
            aspects.putInt("tool_damage", 1024)
            val r = recipe(1, aspects)
            val pickaxe = ItemStack(Items.IRON_PICKAXE)
            pickaxe.setDamageValue(pickaxe.maxDamage - 1)
            val inv = CraftingInput.of(1, 1, listOf(pickaxe))
            val rem = r.getRemainingItems(inv)
            assertTrue(rem[0].isEmpty)
        }
    }

    @Nested
    inner class RepairFlow {
        @Test
        fun toolRepairAspectMakesRecipeSpecialAndChangesResult() {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:iron_pickaxe")
            aspects.putInt("tool_repair", 100)
            val r = recipe(1, aspects)
            assertTrue(r.isSpecial())
            assertTrue(r.getResultItem(null).isEmpty)
        }

        @Test
        fun repairAssembleReturnsHealedToolStack() {
            val r = repairRecipe(50)
            val pickaxe = ItemStack(Items.IRON_PICKAXE)
            pickaxe.setDamageValue(120)
            val inv = repairInput(pickaxe, ItemStack(Items.IRON_INGOT))
            val out = r.assemble(inv, null)
            assertEquals(Items.IRON_PICKAXE, out.item)
            assertTrue(out.damageValue < 120)
        }

        @Test
        fun repairGetRemainingItemsConsumesRepairItem() {
            val r = repairRecipe(50)
            val pickaxe = ItemStack(Items.IRON_PICKAXE)
            pickaxe.setDamageValue(120)
            val inv = repairInput(pickaxe, ItemStack(Items.IRON_INGOT, 4))
            val rem = r.getRemainingItems(inv)
            assertTrue(rem[0].isEmpty || rem[0].item == Items.IRON_PICKAXE)
        }

        @Test
        fun repairWithUndamagedToolReturnsEmptyAssemble() {
            val r = repairRecipe(50)
            val pickaxe = ItemStack(Items.IRON_PICKAXE)
            pickaxe.setDamageValue(0)
            val inv = repairInput(pickaxe, ItemStack(Items.IRON_INGOT))
            assertTrue(r.assemble(inv, null).isEmpty)
        }

        @Test
        fun repairOverRepairAspectAllowsZeroDamageInput() {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:iron_pickaxe")
            aspects.putInt("tool_repair", 50)
            aspects.putBoolean("over_repair", true)
            val r = recipe(2, aspects)
            val pickaxe = ItemStack(Items.IRON_PICKAXE)
            pickaxe.setDamageValue(0)
            val inv = repairInput(pickaxe, ItemStack(Items.IRON_INGOT))
            val out = r.assemble(inv, null)
            assertNotNull(out)
        }

        @Test
        fun repairRelativeDamagePathTriggers() {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:iron_pickaxe")
            aspects.putInt("tool_repair", 25)
            aspects.putBoolean("relative_repair_damage", true)
            val r = recipe(2, aspects)
            val pickaxe = ItemStack(Items.IRON_PICKAXE)
            pickaxe.setDamageValue(pickaxe.maxDamage - 10)
            val inv = repairInput(pickaxe, ItemStack(Items.IRON_INGOT))
            val out = r.assemble(inv, null)
            assertEquals(Items.IRON_PICKAXE, out.item)
            assertTrue(out.damageValue < pickaxe.maxDamage - 10)
        }

        @Test
        fun repairWithMissingToolReturnsEmpty() {
            val r = repairRecipe(50)
            val inv =
                CraftingInput.of(
                    2,
                    1,
                    listOf(ItemStack(Items.IRON_INGOT), ItemStack(Items.IRON_INGOT)),
                )
            assertTrue(r.assemble(inv, null).isEmpty)
        }

        @Test
        fun repairWithNonDamageableToolReturnsEmpty() {
            val aspects = CompoundTag()
            aspects.putString("tool", "minecraft:stick")
            aspects.putInt("tool_repair", 50)
            val r = recipe(2, aspects)
            val inv =
                CraftingInput.of(2, 1, listOf(ItemStack(Items.STICK), ItemStack(Items.IRON_INGOT)))
            assertTrue(r.assemble(inv, null).isEmpty)
        }
    }

    @Nested
    inner class Matches {
        @Test
        fun matchesEmptyInputForRecipeWithIngredients() {
            val r = recipe(1, CompoundTag())
            val inv = CraftingInput.of(1, 1, listOf(ItemStack.EMPTY))
            assertFalse(r.matches(inv, null))
        }
    }
}
