package wile.redstonepen.registry

import java.util.function.BiFunction
import java.util.function.Supplier
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
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

/** Common game registry handling. */
@Suppress("UNCHECKED_CAST")
object Registries {

    fun interface BlockEntityFactory<T : BlockEntity> {
        fun create(pos: BlockPos, state: BlockState): T
    }

    fun interface MenuFactory<T : AbstractContainerMenu> {
        fun create(containerId: Int, inventory: net.minecraft.world.entity.player.Inventory): T
    }

    private val registeredBlockTagKeys = mutableMapOf<String, TagKey<Block>>()
    private val registeredItemTagKeys = mutableMapOf<String, TagKey<Item>>()

    private val blockSuppliers = mutableListOf<Pair<String, Supplier<Block>>>()
    private val itemSuppliers = mutableListOf<Pair<String, Supplier<Item>>>()
    private val blockEntityTypeSuppliers =
        mutableListOf<Pair<String, Supplier<BlockEntityType<*>>>>()
    private val entityTypeSuppliers = mutableListOf<Pair<String, Supplier<EntityType<*>>>>()
    private val menuTypeSuppliers = mutableListOf<Pair<String, Supplier<MenuType<*>>>>()
    private val recipeSerializersSuppliers =
        mutableListOf<Pair<String, Supplier<RecipeSerializer<*>>>>()

    private val registeredBlocks = linkedMapOf<String, Block>()
    private val registeredItems = linkedMapOf<String, Item>()
    private val registeredBlockEntityTypes = mutableMapOf<String, BlockEntityType<*>>()
    private val registeredEntityTypes = mutableMapOf<String, EntityType<*>>()
    private val registeredMenuTypes = mutableMapOf<String, MenuType<*>>()
    private val registeredRecipeSerializers = mutableMapOf<String, RecipeSerializer<*>>()

    @JvmStatic fun init() {}

    @JvmStatic
    fun instantiateAll() {
        registeredBlocks.clear()
        blockSuppliers.forEach { (name, supplier) ->
            registeredBlocks[name] = supplier.get()
            Registry.register(
                BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, name),
                registeredBlocks[name]!!,
            )
        }
        registeredItems.clear()
        itemSuppliers.forEach { (name, supplier) ->
            registeredItems[name] = supplier.get()
            Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, name),
                registeredItems[name]!!,
            )
        }
        registeredBlockEntityTypes.clear()
        blockEntityTypeSuppliers.forEach { (name, supplier) ->
            registeredBlockEntityTypes[name] = supplier.get()
            Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, name),
                registeredBlockEntityTypes[name]!!,
            )
        }
        registeredEntityTypes.clear()
        entityTypeSuppliers.forEach { (name, supplier) ->
            registeredEntityTypes[name] = supplier.get()
            Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, name),
                registeredEntityTypes[name]!!,
            )
        }
        registeredMenuTypes.clear()
        menuTypeSuppliers.forEach { (name, supplier) ->
            registeredMenuTypes[name] = supplier.get()
            Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, name),
                registeredMenuTypes[name]!!,
            )
        }
        registeredRecipeSerializers.clear()
        recipeSerializersSuppliers.forEach { (name, supplier) ->
            registeredRecipeSerializers[name] = supplier.get()
            Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, name),
                registeredRecipeSerializers[name]!!,
            )
        }
    }

    @JvmStatic fun getBlock(blockName: String): Block? = registeredBlocks[blockName]

    @JvmStatic fun getItem(name: String): Item? = registeredItems[name]

    @JvmStatic fun getEntityType(name: String): EntityType<*>? = registeredEntityTypes[name]

    @JvmStatic
    fun getBlockEntityType(blockName: String): BlockEntityType<*>? =
        registeredBlockEntityTypes[blockName]

    @JvmStatic fun getMenuType(name: String): MenuType<*>? = registeredMenuTypes[name]

    @JvmStatic
    fun getRecipeSerializer(name: String): RecipeSerializer<*>? = registeredRecipeSerializers[name]

    @JvmStatic
    fun getBlockEntityTypeOfBlock(blockName: String): BlockEntityType<*>? =
        getBlockEntityType("tet_$blockName")

    @JvmStatic
    fun getBlockEntityTypeOfBlock(block: Block): BlockEntityType<*>? =
        getBlockEntityTypeOfBlock(BuiltInRegistries.BLOCK.getKey(block)!!.path)

    @JvmStatic fun getMenuTypeOfBlock(name: String): MenuType<*>? = getMenuType("ct_$name")

    @JvmStatic
    fun getMenuTypeOfBlock(block: Block): MenuType<*>? =
        getMenuTypeOfBlock(BuiltInRegistries.BLOCK.getKey(block)!!.path)

    @JvmStatic fun getBlockTagKey(name: String): TagKey<Block>? = registeredBlockTagKeys[name]

    @JvmStatic fun getItemTagKey(name: String): TagKey<Item>? = registeredItemTagKeys[name]

    @JvmStatic fun getRegisteredBlocks(): List<Block> = registeredBlocks.values.toList()

    @JvmStatic fun getRegisteredItems(): List<Item> = registeredItems.values.toList()

    @JvmStatic
    fun getRegisteredBlockEntityTypes(): List<BlockEntityType<*>> =
        registeredBlockEntityTypes.values.toList()

    @JvmStatic
    fun getRegisteredEntityTypes(): List<EntityType<*>> = registeredEntityTypes.values.toList()

    @JvmStatic
    fun <T : Item> addItem(registryName: String, supplier: Supplier<T>) {
        itemSuppliers.add(Pair(registryName, supplier as Supplier<Item>))
    }

    @JvmStatic
    fun <T : Block> addBlock(registryName: String, blockSupplier: Supplier<T>) {
        blockSuppliers.add(Pair(registryName, blockSupplier as Supplier<Block>))
        itemSuppliers.add(
            Pair(
                registryName,
                Supplier { BlockItem(registeredBlocks[registryName]!!, Item.Properties()) },
            )
        )
    }

    @JvmStatic
    fun <TB : Block, TI : Item> addBlock(
        registryName: String,
        blockSupplier: Supplier<TB>,
        itemSupplier: Supplier<TI>,
    ) {
        blockSuppliers.add(Pair(registryName, blockSupplier as Supplier<Block>))
        itemSuppliers.add(Pair(registryName, itemSupplier as Supplier<Item>))
    }

    @JvmStatic
    fun <T : BlockEntity> addBlockEntityType(
        registryName: String,
        ctor: BlockEntityFactory<T>,
        vararg blockNames: String,
    ) {
        blockEntityTypeSuppliers.add(
            Pair(
                registryName,
                Supplier {
                    val blocks =
                        blockNames
                            .mapNotNull { s ->
                                registeredBlocks[s]
                                    ?: run {
                                        Auxiliaries.logError(
                                            "registered_blocks does not encompass '$s'"
                                        )
                                        null
                                    }
                            }
                            .toTypedArray()
                    @Suppress("SpreadOperator")
                    BlockEntityType.Builder.of(ctor::create, *blocks).build(null)
                },
            )
        )
    }

    @JvmStatic
    fun addEntityType(registryName: String, supplier: Supplier<EntityType<*>>) {
        entityTypeSuppliers.add(Pair(registryName, supplier))
    }

    @JvmStatic
    fun <T : AbstractContainerMenu> addMenuType(registryName: String, supplier: MenuFactory<T>) {
        menuTypeSuppliers.add(
            Pair(
                registryName,
                Supplier { MenuType(supplier::create, FeatureFlagSet.of()) as MenuType<*> },
            )
        )
    }

    @JvmStatic
    fun addRecipeSerializer(
        registryName: String,
        serializerSupplier: Supplier<out RecipeSerializer<*>>,
    ) {
        recipeSerializersSuppliers.add(
            Pair(registryName, serializerSupplier as Supplier<RecipeSerializer<*>>)
        )
    }

    @JvmStatic
    fun <TB : Block> addBlock(
        registryName: String,
        blockSupplier: Supplier<TB>,
        itemBuilder: BiFunction<Block, Item.Properties, Item>,
    ) {
        @Suppress("UNCHECKED_CAST")
        addBlock(
            registryName,
            blockSupplier,
            Supplier { itemBuilder.apply(registeredBlocks[registryName]!!, Item.Properties()) }
                as Supplier<Item>,
        )
    }

    @JvmStatic
    fun addBlock(
        registryName: String,
        blockSupplier: Supplier<out Block>,
        blockEntityCtor: BlockEntityFactory<*>,
    ) {
        addBlock(registryName, blockSupplier)
        addBlockEntityType(
            "tet_$registryName",
            blockEntityCtor as BlockEntityFactory<BlockEntity>,
            registryName,
        )
    }

    @JvmStatic
    fun <T : AbstractContainerMenu> addBlock(
        registryName: String,
        blockSupplier: Supplier<out Block>,
        itemBuilder: BiFunction<Block, Item.Properties, Item>,
        blockEntityCtor: BlockEntityFactory<*>,
        menuTypeSupplier: MenuFactory<T>,
    ) {
        addBlock(registryName, blockSupplier, itemBuilder)
        addBlockEntityType(
            "tet_$registryName",
            blockEntityCtor as BlockEntityFactory<BlockEntity>,
            registryName,
        )
        addMenuType("ct_$registryName", menuTypeSupplier)
    }

    @JvmStatic
    fun <T : AbstractContainerMenu> addBlock(
        registryName: String,
        blockSupplier: Supplier<out Block>,
        blockEntityCtor: BlockEntityFactory<*>,
        menuTypeSupplier: MenuFactory<T>,
    ) {
        addBlock(registryName, blockSupplier, blockEntityCtor)
        addMenuType("ct_$registryName", menuTypeSupplier)
    }
}
