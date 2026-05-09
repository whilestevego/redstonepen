package wile.redstonepen.registry

import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.util.Tuple
import net.minecraft.world.entity.EntityType
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import wile.redstonepen.ModConstants
import wile.redstonepen.util.Auxiliaries
import java.util.function.BiFunction
import java.util.function.Supplier

/** Common game registry handling. */
@Suppress("UNCHECKED_CAST")
object Registries {

    fun interface BlockEntityFactory<T : BlockEntity> {
        fun create(pos: BlockPos, state: BlockState): T
    }

    fun interface MenuFactory<T : AbstractContainerMenu> {
        fun create(containerId: Int, inventory: net.minecraft.world.entity.player.Inventory): T
    }

    private val registeredBlockTagKeys = HashMap<String, TagKey<Block>>()
    private val registeredItemTagKeys = HashMap<String, TagKey<Item>>()

    private val blockSuppliers = ArrayList<Tuple<String, Supplier<Block>>>()
    private val itemSuppliers = ArrayList<Tuple<String, Supplier<Item>>>()
    private val blockEntityTypeSuppliers = ArrayList<Tuple<String, Supplier<BlockEntityType<*>>>>()
    private val entityTypeSuppliers = ArrayList<Tuple<String, Supplier<EntityType<*>>>>()
    private val menuTypeSuppliers = ArrayList<Tuple<String, Supplier<MenuType<*>>>>()
    private val recipeSerializersSuppliers = ArrayList<Tuple<String, Supplier<RecipeSerializer<*>>>>()

    private val registeredBlocks = LinkedHashMap<String, Block>()
    private val registeredItems = LinkedHashMap<String, Item>()
    private val registeredBlockEntityTypes = HashMap<String, BlockEntityType<*>>()
    private val registeredEntityTypes = HashMap<String, EntityType<*>>()
    private val registeredMenuTypes = HashMap<String, MenuType<*>>()
    private val registeredRecipeSerializers = HashMap<String, RecipeSerializer<*>>()

    @JvmStatic fun init() {}

    @JvmStatic fun instantiateAll() {
        registeredBlocks.clear()
        blockSuppliers.forEach { reg ->
            registeredBlocks[reg.a] = reg.b.get()
            Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, reg.a), registeredBlocks[reg.a]!!)
        }
        registeredItems.clear()
        itemSuppliers.forEach { reg ->
            registeredItems[reg.a] = reg.b.get()
            Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, reg.a), registeredItems[reg.a]!!)
        }
        registeredBlockEntityTypes.clear()
        blockEntityTypeSuppliers.forEach { reg ->
            registeredBlockEntityTypes[reg.a] = reg.b.get()
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, reg.a), registeredBlockEntityTypes[reg.a]!!)
        }
        registeredEntityTypes.clear()
        entityTypeSuppliers.forEach { reg ->
            registeredEntityTypes[reg.a] = reg.b.get()
            Registry.register(BuiltInRegistries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, reg.a), registeredEntityTypes[reg.a]!!)
        }
        registeredMenuTypes.clear()
        menuTypeSuppliers.forEach { reg ->
            registeredMenuTypes[reg.a] = reg.b.get()
            Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, reg.a), registeredMenuTypes[reg.a]!!)
        }
        registeredRecipeSerializers.clear()
        recipeSerializersSuppliers.forEach { reg ->
            registeredRecipeSerializers[reg.a] = reg.b.get()
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, reg.a), registeredRecipeSerializers[reg.a]!!)
        }
    }

    @JvmStatic fun getBlock(blockName: String): Block? = registeredBlocks[blockName]
    @JvmStatic fun getItem(name: String): Item? = registeredItems[name]
    @JvmStatic fun getEntityType(name: String): EntityType<*>? = registeredEntityTypes[name]
    @JvmStatic fun getBlockEntityType(blockName: String): BlockEntityType<*>? = registeredBlockEntityTypes[blockName]
    @JvmStatic fun getMenuType(name: String): MenuType<*>? = registeredMenuTypes[name]
    @JvmStatic fun getRecipeSerializer(name: String): RecipeSerializer<*>? = registeredRecipeSerializers[name]

    @JvmStatic fun getBlockEntityTypeOfBlock(blockName: String): BlockEntityType<*>? = getBlockEntityType("tet_$blockName")
    @JvmStatic fun getBlockEntityTypeOfBlock(block: Block): BlockEntityType<*>? = getBlockEntityTypeOfBlock(BuiltInRegistries.BLOCK.getKey(block)!!.path)

    @JvmStatic fun getMenuTypeOfBlock(name: String): MenuType<*>? = getMenuType("ct_$name")
    @JvmStatic fun getMenuTypeOfBlock(block: Block): MenuType<*>? = getMenuTypeOfBlock(BuiltInRegistries.BLOCK.getKey(block)!!.path)

    @JvmStatic fun getBlockTagKey(name: String): TagKey<Block>? = registeredBlockTagKeys[name]
    @JvmStatic fun getItemTagKey(name: String): TagKey<Item>? = registeredItemTagKeys[name]

    @JvmStatic fun getRegisteredBlocks(): List<Block> = registeredBlocks.values.toList()
    @JvmStatic fun getRegisteredItems(): List<Item> = registeredItems.values.toList()
    @JvmStatic fun getRegisteredBlockEntityTypes(): List<BlockEntityType<*>> = registeredBlockEntityTypes.values.toList()
    @JvmStatic fun getRegisteredEntityTypes(): List<EntityType<*>> = registeredEntityTypes.values.toList()

    @JvmStatic fun <T : Item> addItem(registryName: String, supplier: Supplier<T>) {
        itemSuppliers.add(Tuple(registryName, supplier as Supplier<Item>))
    }

    @JvmStatic fun <T : Block> addBlock(registryName: String, blockSupplier: Supplier<T>) {
        blockSuppliers.add(Tuple(registryName, blockSupplier as Supplier<Block>))
        itemSuppliers.add(Tuple(registryName, Supplier { BlockItem(registeredBlocks[registryName]!!, Item.Properties()) }))
    }

    @JvmStatic fun <TB : Block, TI : Item> addBlock(registryName: String, blockSupplier: Supplier<TB>, itemSupplier: Supplier<TI>) {
        blockSuppliers.add(Tuple(registryName, blockSupplier as Supplier<Block>))
        itemSuppliers.add(Tuple(registryName, itemSupplier as Supplier<Item>))
    }

    @JvmStatic fun <T : BlockEntity> addBlockEntityType(registryName: String, ctor: BlockEntityFactory<T>, vararg blockNames: String) {
        blockEntityTypeSuppliers.add(Tuple(registryName, Supplier {
            val blocks = blockNames.mapNotNull { s ->
                registeredBlocks[s] ?: run {
                    Auxiliaries.logError("registered_blocks does not encompass '$s'")
                    null
                }
            }.toTypedArray()
            BlockEntityType.Builder.of(ctor::create, *blocks).build(null)
        }))
    }

    @JvmStatic fun addEntityType(registryName: String, supplier: Supplier<EntityType<*>>) {
        entityTypeSuppliers.add(Tuple(registryName, supplier))
    }

    @JvmStatic fun <T : AbstractContainerMenu> addMenuType(registryName: String, supplier: MenuFactory<T>) {
        menuTypeSuppliers.add(Tuple(registryName, Supplier { MenuType(supplier::create, FeatureFlagSet.of()) as MenuType<*> }))
    }

    @JvmStatic fun addRecipeSerializer(registryName: String, serializerSupplier: Supplier<out RecipeSerializer<*>>) {
        recipeSerializersSuppliers.add(Tuple(registryName, serializerSupplier as Supplier<RecipeSerializer<*>>))
    }

    @JvmStatic fun <TB : Block> addBlock(registryName: String, blockSupplier: Supplier<TB>, itemBuilder: BiFunction<Block, Item.Properties, Item>) {
        @Suppress("UNCHECKED_CAST")
        addBlock(registryName, blockSupplier, Supplier { itemBuilder.apply(registeredBlocks[registryName]!!, Item.Properties()) } as Supplier<Item>)
    }

    @JvmStatic fun addBlock(registryName: String, blockSupplier: Supplier<out Block>, blockEntityCtor: BlockEntityFactory<*>) {
        addBlock(registryName, blockSupplier)
        addBlockEntityType("tet_$registryName", blockEntityCtor as BlockEntityFactory<BlockEntity>, registryName)
    }

    @JvmStatic fun <T : AbstractContainerMenu> addBlock(registryName: String, blockSupplier: Supplier<out Block>, itemBuilder: BiFunction<Block, Item.Properties, Item>, blockEntityCtor: BlockEntityFactory<*>, menuTypeSupplier: MenuFactory<T>) {
        addBlock(registryName, blockSupplier, itemBuilder)
        addBlockEntityType("tet_$registryName", blockEntityCtor as BlockEntityFactory<BlockEntity>, registryName)
        addMenuType("ct_$registryName", menuTypeSupplier)
    }

    @JvmStatic fun <T : AbstractContainerMenu> addBlock(registryName: String, blockSupplier: Supplier<out Block>, blockEntityCtor: BlockEntityFactory<*>, menuTypeSupplier: MenuFactory<T>) {
        addBlock(registryName, blockSupplier, blockEntityCtor)
        addMenuType("ct_$registryName", menuTypeSupplier)
    }
}
