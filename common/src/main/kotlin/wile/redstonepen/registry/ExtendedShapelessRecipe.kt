@file:Suppress("DEPRECATION")

package wile.redstonepen.registry

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.HashMap
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.Mth
import net.minecraft.util.Tuple
import net.minecraft.world.entity.player.StackedContents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import wile.redstonepen.util.Auxiliaries

class ExtendedShapelessRecipe(
    private val group: String,
    private val category: CraftingBookCategory,
    private val result: ItemStack,
    private val ingredients: NonNullList<Ingredient>,
    private val aspects: CompoundTag,
) : CraftingRecipe {

    fun interface IRepairableToolItem {
        fun onShapelessRecipeRepaired(
            toolStack: ItemStack,
            previousDamage: Int,
            repairedDamage: Int,
        ): ItemStack
    }

    override fun getSerializer(): RecipeSerializer<*> = SERIALIZER

    override fun getGroup(): String = group

    override fun category(): CraftingBookCategory = category

    fun getAspects(): CompoundTag = aspects.copy()

    override fun isSpecial(): Boolean = isRepair() || aspects.getBoolean("dynamic")

    override fun getResultItem(ra: HolderLookup.Provider): ItemStack =
        if (isSpecial()) ItemStack.EMPTY else result

    override fun getIngredients(): NonNullList<Ingredient> = ingredients

    override fun canCraftInDimensions(i: Int, j: Int): Boolean = i * j >= ingredients.size

    override fun getRemainingItems(inv: CraftingInput): NonNullList<ItemStack> {
        if (isRepair()) {
            val remaining = getRepaired(inv).b
            for (i in 0 until remaining.size) {
                val remStack = remaining[i]
                val invStack = inv.getItem(i)
                if (invStack.isEmpty) continue
                if (!remStack.isEmpty && !inv.getItem(i).`is`(remStack.item)) continue
                remaining[i] = ItemStack.EMPTY
                if (!remStack.isEmpty) remStack.grow(1)
                invStack.count = remStack.count
            }
            return remaining
        } else {
            val toolName = aspects.getString("tool")
            val toolDamage = getToolDamage()
            val remaining = NonNullList.withSize(inv.size(), ItemStack.EMPTY)
            for (i in 0 until remaining.size) {
                val stack = inv.getItem(i)
                if (Auxiliaries.getResourceLocation(stack.item).toString() == toolName) {
                    if (!stack.isDamageableItem) {
                        remaining[i] = stack
                    } else {
                        val rstack = stack.copy()
                        rstack.damageValue = rstack.damageValue + toolDamage
                        if (rstack.damageValue < rstack.maxDamage) remaining[i] = rstack
                    }
                } else if (stack.item.hasCraftingRemainingItem()) {
                    remaining[i] =
                        ItemStack(requireNotNull(stack.item.craftingRemainingItem), stack.count)
                }
            }
            return remaining
        }
    }

    override fun matches(input: CraftingInput, world: Level): Boolean {
        val stacked = StackedContents()
        var i = 0
        for (j in 0 until input.size()) {
            val ingr = input.getItem(j)
            if (ingr.isEmpty) continue
            stacked.accountStack(ingr, 1)
            ++i
        }
        return (i == ingredients.size) && stacked.canCraft(this, null)
    }

    override fun assemble(inv: CraftingInput, ra: HolderLookup.Provider): ItemStack {
        if (isRepair()) return getRepaired(inv).a
        val rstack = result.copy()
        if (rstack.isEmpty) return ItemStack.EMPTY
        if (aspects.getInt("initial_durability") > 0) {
            val dmg = maxOf(0, rstack.maxDamage - aspects.getInt("initial_durability"))
            if (dmg > 0) rstack.damageValue = dmg
        } else if (aspects.getInt("initial_damage") > 0) {
            val dmg = minOf(aspects.getInt("initial_damage"), rstack.maxDamage)
            if (dmg > 0) rstack.damageValue = dmg
        }
        return rstack
    }

    private fun getToolDamage(): Int =
        when {
            aspects.contains("tool_repair") -> -Mth.clamp(aspects.getInt("tool_repair"), 0, 4096)
            aspects.contains("tool_damage") -> Mth.clamp(aspects.getInt("tool_damage"), 1, 1024)
            else -> 0
        }

    private fun isRepair(): Boolean = getToolDamage() < 0

    private fun getRepaired(inv: CraftingInput): Tuple<ItemStack, NonNullList<ItemStack>> {
        val toolName = aspects.getString("tool")
        val repairItems = HashMap<Item, Int>()
        val remaining = NonNullList.withSize(inv.size(), ItemStack.EMPTY)
        var toolItem = ItemStack.EMPTY
        for (i in 0 until inv.size()) {
            val stack = inv.getItem(i)
            when {
                stack.isEmpty -> continue
                Auxiliaries.getResourceLocation(stack.item).toString() == toolName ->
                    toolItem = stack.copy()
                else -> {
                    remaining[i] = stack.copy()
                    repairItems[stack.item] = stack.count + (repairItems[stack.item] ?: 0)
                }
            }
        }
        if (toolItem.isEmpty) return Tuple(ItemStack.EMPTY, remaining)
        if (!toolItem.isDamageableItem) {
            Auxiliaries.logWarn(
                "Repairing '${Auxiliaries.getResourceLocation(toolItem.item)}' can't work, the item is not damageable."
            )
            return Tuple(ItemStack.EMPTY, remaining)
        }
        val dmg = toolItem.damageValue
        if ((dmg <= 0) && (!aspects.getBoolean("over_repair"))) {
            return Tuple(ItemStack.EMPTY, remaining)
        }
        val minRepairItemCount = repairItems.values.minOrNull() ?: 0
        if (minRepairItemCount <= 0) return Tuple(ItemStack.EMPTY, remaining)
        val singleRepairDur =
            if (aspects.getBoolean("relative_repair_damage")) {
                maxOf(1, -getToolDamage() * toolItem.maxDamage / 100)
            } else {
                maxOf(1, -getToolDamage())
            }
        var numRepairs = dmg / singleRepairDur
        if (numRepairs * singleRepairDur < dmg) ++numRepairs
        numRepairs = minOf(numRepairs, minRepairItemCount)
        for (ki in repairItems.keys) repairItems[ki] = numRepairs
        toolItem.damageValue = maxOf(dmg - (singleRepairDur * numRepairs), 0)
        for (i in 0 until remaining.size) {
            val stack = inv.getItem(i)
            if (stack.isEmpty) continue
            if (Auxiliaries.getResourceLocation(stack.item).toString() == toolName) continue
            remaining[i] =
                if (stack.item.hasCraftingRemainingItem()) {
                    ItemStack(requireNotNull(stack.item.craftingRemainingItem), stack.count)
                } else {
                    stack.copy()
                }
        }
        for (i in 0 until remaining.size) {
            val stack = remaining[i]
            val item = stack.item
            if (!repairItems.containsKey(item)) continue
            val n = repairItems[item]!!
            if (stack.count >= n) {
                stack.shrink(n)
                repairItems.remove(item)
            } else {
                repairItems[item] = n - stack.count
                remaining[i] = ItemStack.EMPTY
            }
        }
        if (toolItem.item is IRepairableToolItem) {
            toolItem =
                (toolItem.item as IRepairableToolItem).onShapelessRecipeRepaired(
                    toolItem,
                    dmg,
                    toolItem.damageValue,
                )
        }
        return Tuple(toolItem, remaining)
    }

    companion object {
        @JvmField val SERIALIZER: Serializer = Serializer()
    }

    class Serializer : RecipeSerializer<ExtendedShapelessRecipe> {
        override fun codec(): MapCodec<ExtendedShapelessRecipe> = CODEC

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ExtendedShapelessRecipe> =
            STREAM_CODEC

        companion object {
            @Suppress("UNCHECKED_CAST")
            private val CODEC: MapCodec<ExtendedShapelessRecipe> =
                RecordCodecBuilder.mapCodec { instance ->
                    instance
                        .group(
                            Codec.STRING.optionalFieldOf("group", "").forGetter { it.group },
                            CraftingBookCategory.CODEC.fieldOf("category")
                                .orElse(CraftingBookCategory.MISC)
                                .forGetter { it.category },
                            ItemStack.CODEC.fieldOf("result").forGetter { it.result },
                            Ingredient.CODEC_NONEMPTY.listOf()
                                .fieldOf("ingredients")
                                .flatXmap(
                                    { list ->
                                        val ingredients = list.filter { !it.isEmpty }.toTypedArray()
                                        when {
                                            ingredients.isEmpty() ->
                                                DataResult.error { "no ingredients" }
                                            ingredients.size > 9 ->
                                                DataResult.error { "too many ingredients" }
                                            else ->
                                                DataResult.success(
                                                    @Suppress("SpreadOperator")
                                                    NonNullList.of(Ingredient.EMPTY, *ingredients)
                                                )
                                        }
                                    },
                                    { v -> DataResult.success(v) },
                                )
                                .forGetter { it.ingredients },
                            CompoundTag.CODEC.optionalFieldOf("aspects", CompoundTag()).forGetter {
                                it.aspects
                            },
                        )
                        .apply(instance) { g, cat, res, ing, asp ->
                            ExtendedShapelessRecipe(g, cat, res, ing, asp)
                        }
                }

            @JvmField
            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ExtendedShapelessRecipe> =
                StreamCodec.of(::toNetwork, ::fromNetwork)

            private fun fromNetwork(buf: RegistryFriendlyByteBuf): ExtendedShapelessRecipe {
                val group = buf.readUtf()
                val cat = buf.readEnum(CraftingBookCategory::class.java)
                val size = buf.readVarInt()
                val ingredients = NonNullList.withSize(size, Ingredient.EMPTY)
                ingredients.replaceAll { Ingredient.CONTENTS_STREAM_CODEC.decode(buf) }
                val stack = ItemStack.STREAM_CODEC.decode(buf)
                val aspects = requireNotNull(buf.readNbt())
                return ExtendedShapelessRecipe(group, cat, stack, ingredients, aspects)
            }

            private fun toNetwork(buf: RegistryFriendlyByteBuf, recipe: ExtendedShapelessRecipe) {
                buf.writeUtf(recipe.group)
                buf.writeEnum(recipe.category)
                buf.writeVarInt(recipe.ingredients.size)
                for (ingredient in recipe.ingredients) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient)
                }
                ItemStack.STREAM_CODEC.encode(buf, recipe.result)
                buf.writeNbt(recipe.getAspects())
            }
        }
    }
}
