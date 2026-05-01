package wile.redstonepen.libmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.NonNullList;
import org.junit.jupiter.api.Test;

class ExtendedShapelessRecipeTest
{
  private static ExtendedShapelessRecipe recipe(int ingredientCount, CompoundTag aspects)
  {
    final NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
    return new ExtendedShapelessRecipe("test_group", CraftingBookCategory.MISC,
      new ItemStack(Items.REDSTONE), ingredients, aspects);
  }

  // --- isSpecial / aspects ---

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

  // --- getAspects copy semantics ---

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

  // --- canCraftInDimensions ---

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

  // --- getGroup ---

  @Test
  void getGroupReturnsConstructorArgument()
  {
    final NonNullList<Ingredient> ingredients = NonNullList.withSize(1, Ingredient.EMPTY);
    final ExtendedShapelessRecipe r = new ExtendedShapelessRecipe(
      "my_group", CraftingBookCategory.MISC, new ItemStack(Items.REDSTONE),
      ingredients, new CompoundTag());
    assertEquals("my_group", r.getGroup());
  }
}
