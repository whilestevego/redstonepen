package wile.redstonepen.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import wile.redstonepen.ModConstants;
import wile.redstonepen.libmc.ExtendedShapelessRecipe;

public class RecipeGameTests
{
  private static final String NS = "minecraft";
  private static final String RELAY_TEMPLATE = "redstonepen:relay_activates_from_redstone";

  public RecipeGameTests() {}

  @GameTest(template = RELAY_TEMPLATE, timeoutTicks = 5)
  public static void extendedShapelessRecipeSerializerRoundTrip(GameTestHelper helper)
  {
    final NonNullList<Ingredient> ingredients = NonNullList.of(
      Ingredient.EMPTY, Ingredient.of(Items.REDSTONE));
    final CompoundTag aspects = new CompoundTag();
    aspects.putInt("initial_durability", 10);
    final ExtendedShapelessRecipe recipe = new ExtendedShapelessRecipe(
      "test_group", CraftingBookCategory.MISC, new ItemStack(Items.STICK), ingredients, aspects);

    final var ra = helper.getLevel().registryAccess();
    final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), ra);

    ExtendedShapelessRecipe.Serializer.STREAM_CODEC.encode(buf, recipe);
    final ExtendedShapelessRecipe decoded = ExtendedShapelessRecipe.Serializer.STREAM_CODEC.decode(buf);

    if(!decoded.getGroup().equals("test_group"))
      helper.fail("group must round-trip through fromNetwork/toNetwork, got: " + decoded.getGroup());
    if(decoded.getIngredients().size() != 1)
      helper.fail("ingredient count must round-trip, got: " + decoded.getIngredients().size());
    helper.succeed();
  }
}
