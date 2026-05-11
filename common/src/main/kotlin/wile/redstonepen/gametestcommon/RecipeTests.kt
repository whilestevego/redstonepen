package wile.redstonepen.gametestcommon

import io.netty.buffer.Unpooled
import net.minecraft.core.NonNullList
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.Ingredient
import wile.redstonepen.registry.ExtendedShapelessRecipe

object RecipeTests {

    @JvmStatic
    fun extendedShapelessRecipeSerializerRoundTrip(helper: GameTestHelper) {
        val ingredients = NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.REDSTONE))
        val aspects = CompoundTag()
        aspects.putInt("initial_durability", 10)
        val recipe =
            ExtendedShapelessRecipe(
                "test_group",
                CraftingBookCategory.MISC,
                ItemStack(Items.STICK),
                ingredients,
                aspects,
            )

        val ra = helper.level.registryAccess()
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), ra)

        ExtendedShapelessRecipe.Serializer.STREAM_CODEC.encode(buf, recipe)
        val decoded = ExtendedShapelessRecipe.Serializer.STREAM_CODEC.decode(buf)

        if (decoded.group != "test_group") {
            helper.fail(
                "group must round-trip through fromNetwork/toNetwork, got: ${decoded.group}"
            )
        }
        if (decoded.ingredients.size != 1) {
            helper.fail("ingredient count must round-trip, got: ${decoded.ingredients.size}")
        }
        helper.succeed()
    }
}
