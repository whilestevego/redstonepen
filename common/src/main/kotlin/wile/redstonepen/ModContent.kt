package wile.redstonepen

import net.minecraft.world.entity.EntityType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import wile.redstonepen.blocks.CircuitComponents
import wile.redstonepen.blocks.StandardBlocks
import wile.redstonepen.blocks.basic.BasicButton
import wile.redstonepen.blocks.basic.BasicButtonConfig
import wile.redstonepen.blocks.basic.BasicGauge
import wile.redstonepen.blocks.basic.BasicLever
import wile.redstonepen.blocks.basic.BasicLeverConfig
import wile.redstonepen.blocks.controlbox.ControlBoxBlock
import wile.redstonepen.blocks.controlbox.ControlBoxBlockEntity
import wile.redstonepen.blocks.controlbox.ControlBoxUiContainer
import wile.redstonepen.blocks.track.RedstoneTrackBlock
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.items.RedstonePenItem
import wile.redstonepen.items.RemoteItem
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries

object ModContent {

    @JvmStatic
    fun init() {
        initBlocks()
        initItems()
        Registries.addRecipeSerializer("crafting_extended_shapeless") {
            wile.redstonepen.registry.ExtendedShapelessRecipe.SERIALIZER
        }
    }

    @JvmStatic
    fun initBlocks() {
        val never = BlockBehaviour.StateArgumentPredicate<EntityType<*>> { _, _, _, _ -> false }
        val directedItem =
            java.util.function.BiFunction<Block, Item.Properties, Item> { b, p ->
                CircuitComponents.DirectedComponentBlockItem(b, p)
            }

        Registries.addBlock(
            "track",
            {
                RedstoneTrackBlock(
                    StandardBlocks.CFG_DEFAULT,
                    BlockBehaviour.Properties.of()
                        .noCollission()
                        .instabreak()
                        .dynamicShape()
                        .randomTicks(),
                )
            },
            Registries.BlockEntityFactory { pos, state -> TrackBlockEntity(pos, state) },
        )
        Registries.addBlock(
            "control_box",
            {
                ControlBoxBlock(
                    StandardBlocks.CFG_CUTOUT,
                    BlockBehaviour.Properties.of().noCollission().instabreak(),
                    arrayOf(
                        Auxiliaries.getPixeledAABB(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
                        Auxiliaries.getPixeledAABB(3.0, 1.0, 3.0, 13.0, 3.9, 13.0),
                    ),
                )
            },
            directedItem,
            ::ControlBoxBlockEntity,
            ::ControlBoxUiContainer,
        )
        Registries.addBlock("relay", { CircuitComponents.RelayBlock() }, directedItem)
        Registries.addBlock(
            "inverted_relay",
            { CircuitComponents.InvertedRelayBlock() },
            directedItem,
        )
        Registries.addBlock("pulse_relay", { CircuitComponents.PulseRelayBlock() }, directedItem)
        Registries.addBlock(
            "bistable_relay",
            { CircuitComponents.BistableRelayBlock() },
            directedItem,
        )
        Registries.addBlock("bridge_relay", { CircuitComponents.BridgeRelayBlock() }, directedItem)
        Registries.addBlock(
            "basic_gauge",
            {
                BasicGauge.BasicGaugeBlock(
                    StandardBlocks.CFG_TRANSLUCENT,
                    BlockBehaviour.Properties.of()
                        .isValidSpawn(never)
                        .strength(0.3f)
                        .sound(SoundType.COPPER)
                        .noCollission()
                        .lightLevel { 3 },
                )
            },
        )
        Registries.addBlock(
            "basic_lever",
            {
                BasicLever.BasicLeverBlock(
                    BasicLeverConfig(0.8f, 0.9f),
                    BlockBehaviour.Properties.of()
                        .noCollission()
                        .isValidSpawn(never)
                        .strength(0.3f)
                        .sound(SoundType.METAL)
                        .pushReaction(PushReaction.DESTROY),
                )
            },
        )
        Registries.addBlock(
            "basic_button",
            {
                BasicButton.BasicButtonBlock(
                    BasicButtonConfig(0.8f, 0.9f, 20),
                    BlockBehaviour.Properties.of()
                        .noCollission()
                        .isValidSpawn(never)
                        .strength(0.3f)
                        .sound(SoundType.METAL)
                        .pushReaction(PushReaction.DESTROY),
                )
            },
        )
        Registries.addBlock(
            "basic_pulse_button",
            {
                BasicButton.BasicButtonBlock(
                    BasicButtonConfig(0.8f, 0.9f, 2),
                    BlockBehaviour.Properties.of()
                        .noCollission()
                        .isValidSpawn(never)
                        .strength(0.3f)
                        .sound(SoundType.METAL)
                        .pushReaction(PushReaction.DESTROY),
                )
            },
        )
    }

    @JvmStatic
    fun initItems() {
        Registries.addItem("pen") { RedstonePenItem(Item.Properties().stacksTo(0).durability(256)) }
        Registries.addItem("quill") { RedstonePenItem(Item.Properties().stacksTo(1).durability(0)) }
        Registries.addItem("remote") { RemoteItem(Item.Properties().stacksTo(1).durability(1)) }
    }

    @JvmStatic
    fun initReferences() {
        References.TRACK_BLOCK = Registries.getBlock("track") as RedstoneTrackBlock
        References.BRIDGE_RELAY_BLOCK =
            Registries.getBlock("bridge_relay") as CircuitComponents.BridgeRelayBlock
        References.CONTROLBOX_BLOCK = Registries.getBlock("control_box") as ControlBoxBlock
        References.BASIC_GAUGE_BLOCK =
            Registries.getBlock("basic_gauge") as BasicGauge.BasicGaugeBlock
    }

    fun getMenuTypeOfBlock(block_name: String): MenuType<*>? =
        Registries.getMenuTypeOfBlock(block_name)

    fun getMenuTypeOfBlock(block: Block): MenuType<*>? = Registries.getMenuTypeOfBlock(block)

    fun getBlockEntityTypeOfBlock(block_name: String): BlockEntityType<*>? =
        Registries.getBlockEntityTypeOfBlock(block_name)

    fun getBlockEntityTypeOfBlock(block: Block): BlockEntityType<*>? =
        Registries.getBlockEntityTypeOfBlock(block)

    object References {
        lateinit var TRACK_BLOCK: RedstoneTrackBlock
        lateinit var BRIDGE_RELAY_BLOCK: CircuitComponents.BridgeRelayBlock
        lateinit var CONTROLBOX_BLOCK: ControlBoxBlock
        lateinit var BASIC_GAUGE_BLOCK: BasicGauge.BasicGaugeBlock
    }
}
