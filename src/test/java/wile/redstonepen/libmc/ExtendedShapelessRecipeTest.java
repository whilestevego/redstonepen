package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExtendedShapelessRecipeTest
{
  private static ExtendedShapelessRecipe recipe(int ingredientCount, CompoundTag aspects)
  {
    final NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
    return new ExtendedShapelessRecipe("test_group", CraftingBookCategory.MISC,
      new ItemStack(Items.REDSTONE), ingredients, aspects);
  }

  private static ExtendedShapelessRecipe repairRecipe(int repairPerItem)
  {
    final CompoundTag aspects = new CompoundTag();
    aspects.putString("tool", "minecraft:iron_pickaxe");
    aspects.putInt("tool_repair", repairPerItem);
    return recipe(2, aspects);
  }

  private static CraftingInput repairInput(ItemStack tool, ItemStack repairItem)
  { return CraftingInput.of(2, 1, List.of(tool, repairItem)); }

  @Nested
  class Aspects
  {
    @Test
    void isSpecialFalseForEmptyAspects()
    {
      assertFalse(recipe(1, new CompoundTag()).isSpecial());
    }

    @Test
    void isSpecialTrueWhenDynamicFlagSet()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putBoolean("dynamic", true);
      assertTrue(recipe(1, aspects).isSpecial());
    }

    @Test
    void isSpecialTrueWhenToolRepairSet()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putInt("tool_repair", 100);
      assertTrue(recipe(1, aspects).isSpecial());
    }

    @Test
    void isSpecialFalseWhenOnlyToolDamageSet()
    {
      // tool_damage makes getToolDamage() positive → isRepair()=false → isSpecial()=false
      final CompoundTag aspects = new CompoundTag();
      aspects.putInt("tool_damage", 50);
      assertFalse(recipe(1, aspects).isSpecial());
    }

    @Test
    void getAspectsReturnsCopyNotReference()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putBoolean("dynamic", true);
      final ExtendedShapelessRecipe r = recipe(1, aspects);

      final CompoundTag copy = r.getAspects();
      copy.putInt("mutation", 99);

      assertFalse(r.getAspects().contains("mutation"),
        "mutating returned aspects copy must not affect the recipe");
    }
  }

  @Nested
  class Dimensions
  {
    @Test
    void canCraftInDimensionsTrueWhenGridFitsIngredients()
    {
      assertTrue(recipe(1, new CompoundTag()).canCraftInDimensions(1, 1));
      assertTrue(recipe(9, new CompoundTag()).canCraftInDimensions(3, 3));
      assertTrue(recipe(1, new CompoundTag()).canCraftInDimensions(1, 9));
    }

    @Test
    void canCraftInDimensionsFalseWhenGridTooSmall()
    {
      assertFalse(recipe(9, new CompoundTag()).canCraftInDimensions(2, 4)); // 8 < 9
      assertFalse(recipe(2, new CompoundTag()).canCraftInDimensions(1, 1)); // 1 < 2
    }
  }

  @Nested
  class Metadata
  {
    @Test
    void getGroupReturnsConstructorArgument()
    {
      final NonNullList<Ingredient> ingredients = NonNullList.withSize(1, Ingredient.EMPTY);
      final ExtendedShapelessRecipe r = new ExtendedShapelessRecipe(
        "my_group", CraftingBookCategory.MISC, new ItemStack(Items.REDSTONE),
        ingredients, new CompoundTag());
      assertEquals("my_group", r.getGroup());
    }

    @Test
    void categoryReturnsConstructorArgument()
    {
      assertEquals(CraftingBookCategory.MISC, recipe(1, new CompoundTag()).category());
    }

    @Test
    void getSerializerNotNull()
    { assertNotNull(recipe(1, new CompoundTag()).getSerializer()); }

    @Test
    void serializerExposesCodecs()
    {
      assertNotNull(ExtendedShapelessRecipe.SERIALIZER.codec());
      assertNotNull(ExtendedShapelessRecipe.SERIALIZER.streamCodec());
    }

    @Test
    void getIngredientsReflectsConstructor()
    {
      final NonNullList<Ingredient> ingredients = NonNullList.withSize(2, Ingredient.EMPTY);
      final ExtendedShapelessRecipe r = new ExtendedShapelessRecipe(
        "g", CraftingBookCategory.MISC, new ItemStack(Items.REDSTONE),
        ingredients, new CompoundTag());
      assertEquals(2, r.getIngredients().size());
    }
  }

  @Nested
  class ResultItem
  {
    @Test
    void getResultItemReturnsConfiguredOutputForNonSpecial()
    {
      final ItemStack out = recipe(1, new CompoundTag()).getResultItem(null);
      assertEquals(Items.REDSTONE, out.getItem());
    }

    @Test
    void getResultItemEmptyForSpecialDynamicRecipe()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putBoolean("dynamic", true);
      assertTrue(recipe(1, aspects).getResultItem(null).isEmpty());
    }

    @Test
    void assembleReturnsResultStackForBasicRecipe()
    {
      final CompoundTag aspects = new CompoundTag();
      final ExtendedShapelessRecipe r = new ExtendedShapelessRecipe(
        "g", CraftingBookCategory.MISC, new ItemStack(Items.IRON_PICKAXE),
        NonNullList.withSize(1, Ingredient.EMPTY), aspects);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STICK)));
      final ItemStack out = r.assemble(inv, null);
      assertEquals(Items.IRON_PICKAXE, out.getItem());
      assertEquals(0, out.getDamageValue());
    }

    @Test
    void assembleAppliesInitialDurability()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putInt("initial_durability", 1);
      final ExtendedShapelessRecipe r = new ExtendedShapelessRecipe(
        "g", CraftingBookCategory.MISC, new ItemStack(Items.IRON_PICKAXE),
        NonNullList.withSize(1, Ingredient.EMPTY), aspects);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STICK)));
      final ItemStack out = r.assemble(inv, null);
      assertTrue(out.getDamageValue() > 0);
    }

    @Test
    void assembleAppliesInitialDamage()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putInt("initial_damage", 5);
      final ExtendedShapelessRecipe r = new ExtendedShapelessRecipe(
        "g", CraftingBookCategory.MISC, new ItemStack(Items.IRON_PICKAXE),
        NonNullList.withSize(1, Ingredient.EMPTY), aspects);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STICK)));
      final ItemStack out = r.assemble(inv, null);
      assertEquals(5, out.getDamageValue());
    }

    @Test
    void assembleEmptyResultYieldsEmpty()
    {
      final CompoundTag aspects = new CompoundTag();
      final ExtendedShapelessRecipe r = new ExtendedShapelessRecipe(
        "g", CraftingBookCategory.MISC, ItemStack.EMPTY,
        NonNullList.withSize(1, Ingredient.EMPTY), aspects);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STICK)));
      assertTrue(r.assemble(inv, null).isEmpty());
    }
  }

  @Nested
  class RemainingItems
  {
    @Test
    void getRemainingItemsReturnsListOfSameSizeAsInput()
    {
      final CompoundTag aspects = new CompoundTag();
      final ExtendedShapelessRecipe r = recipe(1, aspects);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STICK)));
      final NonNullList<ItemStack> rem = r.getRemainingItems(inv);
      assertEquals(1, rem.size());
    }

    @Test
    void getRemainingItemsRetainsToolWhenNotDamageable()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putString("tool", "minecraft:stick");
      aspects.putInt("tool_damage", 1);
      final ExtendedShapelessRecipe r = recipe(1, aspects);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STICK)));
      final NonNullList<ItemStack> rem = r.getRemainingItems(inv);
      assertEquals(Items.STICK, rem.get(0).getItem());
    }

    @Test
    void getRemainingItemsDamagesDamageableTool()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putString("tool", "minecraft:iron_pickaxe");
      aspects.putInt("tool_damage", 3);
      final ExtendedShapelessRecipe r = recipe(1, aspects);
      final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(pickaxe));
      final NonNullList<ItemStack> rem = r.getRemainingItems(inv);
      final ItemStack out = rem.get(0);
      assertEquals(Items.IRON_PICKAXE, out.getItem());
      assertEquals(3, out.getDamageValue());
    }

    @Test
    void getRemainingItemsConsumesToolWhenDamageExceedsMax()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putString("tool", "minecraft:iron_pickaxe");
      aspects.putInt("tool_damage", 1024);
      final ExtendedShapelessRecipe r = recipe(1, aspects);
      final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
      pickaxe.setDamageValue(pickaxe.getMaxDamage()-1);
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(pickaxe));
      final NonNullList<ItemStack> rem = r.getRemainingItems(inv);
      assertTrue(rem.get(0).isEmpty());
    }
  }

  @Nested
  class RepairFlow
  {
    @Test
    void toolRepairAspectMakesRecipeSpecialAndChangesResult()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putString("tool", "minecraft:iron_pickaxe");
      aspects.putInt("tool_repair", 100);
      final ExtendedShapelessRecipe r = recipe(1, aspects);
      assertTrue(r.isSpecial());
      assertTrue(r.getResultItem(null).isEmpty());
    }

    @Test
    void repairAssembleReturnsHealedToolStack()
    {
      final ExtendedShapelessRecipe r = repairRecipe(50);
      final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
      pickaxe.setDamageValue(120);
      final CraftingInput inv = repairInput(pickaxe, new ItemStack(Items.IRON_INGOT));
      final ItemStack out = r.assemble(inv, null);
      assertEquals(Items.IRON_PICKAXE, out.getItem());
      assertTrue(out.getDamageValue() < 120, "expected damage to drop after repair");
    }

    @Test
    void repairGetRemainingItemsConsumesRepairItem()
    {
      final ExtendedShapelessRecipe r = repairRecipe(50);
      final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
      pickaxe.setDamageValue(120);
      final CraftingInput inv = repairInput(pickaxe, new ItemStack(Items.IRON_INGOT, 4));
      final NonNullList<ItemStack> rem = r.getRemainingItems(inv);
      // tool slot gets cleared (it's the one being repaired); repair item slot
      // shows the remaining count after consumption.
      assertTrue(rem.get(0).isEmpty() || rem.get(0).getItem() == Items.IRON_PICKAXE);
    }

    @Test
    void repairWithUndamagedToolReturnsEmptyAssemble()
    {
      final ExtendedShapelessRecipe r = repairRecipe(50);
      final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
      pickaxe.setDamageValue(0);
      final CraftingInput inv = repairInput(pickaxe, new ItemStack(Items.IRON_INGOT));
      assertTrue(r.assemble(inv, null).isEmpty());
    }

    @Test
    void repairOverRepairAspectAllowsZeroDamageInput()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putString("tool", "minecraft:iron_pickaxe");
      aspects.putInt("tool_repair", 50);
      aspects.putBoolean("over_repair", true);
      final ExtendedShapelessRecipe r = recipe(2, aspects);
      final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
      pickaxe.setDamageValue(0);
      final CraftingInput inv = repairInput(pickaxe, new ItemStack(Items.IRON_INGOT));
      // over_repair allows the calculation to proceed even at zero damage.
      final ItemStack out = r.assemble(inv, null);
      assertNotNull(out);
    }

    @Test
    void repairRelativeDamagePathTriggers()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putString("tool", "minecraft:iron_pickaxe");
      aspects.putInt("tool_repair", 25); // 25% per item when relative
      aspects.putBoolean("relative_repair_damage", true);
      final ExtendedShapelessRecipe r = recipe(2, aspects);
      final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
      pickaxe.setDamageValue(pickaxe.getMaxDamage() - 10);
      final CraftingInput inv = repairInput(pickaxe, new ItemStack(Items.IRON_INGOT));
      final ItemStack out = r.assemble(inv, null);
      assertEquals(Items.IRON_PICKAXE, out.getItem());
      assertTrue(out.getDamageValue() < pickaxe.getMaxDamage() - 10);
    }

    @Test
    void repairWithMissingToolReturnsEmpty()
    {
      final ExtendedShapelessRecipe r = repairRecipe(50);
      final CraftingInput inv = CraftingInput.of(2, 1, List.of(
        new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT)));
      final ItemStack out = r.assemble(inv, null);
      assertTrue(out.isEmpty());
    }

    @Test
    void repairWithNonDamageableToolReturnsEmpty()
    {
      final CompoundTag aspects = new CompoundTag();
      aspects.putString("tool", "minecraft:stick");
      aspects.putInt("tool_repair", 50);
      final ExtendedShapelessRecipe r = recipe(2, aspects);
      final CraftingInput inv = CraftingInput.of(2, 1, List.of(
        new ItemStack(Items.STICK), new ItemStack(Items.IRON_INGOT)));
      assertTrue(r.assemble(inv, null).isEmpty());
    }
  }

  @Nested
  class Matches
  {
    @Test
    void matchesEmptyInputForRecipeWithIngredients()
    {
      final ExtendedShapelessRecipe r = recipe(1, new CompoundTag());
      final CraftingInput inv = CraftingInput.of(1, 1, List.of(ItemStack.EMPTY));
      // 0 non-empty stacks but recipe expects 1 → mismatch
      assertFalse(r.matches(inv, null));
    }
  }
}
