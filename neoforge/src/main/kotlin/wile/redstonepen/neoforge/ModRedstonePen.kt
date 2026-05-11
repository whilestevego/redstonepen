package wile.redstonepen.neoforge

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.RegisterEvent
import wile.redstonepen.ModConstants
import wile.redstonepen.ModContent
import wile.redstonepen.blocks.controlbox.ControlBoxUiContainer
import wile.redstonepen.blocks.controlbox.client.ControlBoxGui
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.client.Overlay
import wile.redstonepen.commands.DemoCommand
import wile.redstonepen.detail.ModRenderers
import wile.redstonepen.detail.RcaSync
import wile.redstonepen.net.Networking
import wile.redstonepen.registry.Registries
import wile.redstonepen.util.Auxiliaries

@Mod("redstonepen")
class ModRedstonePen(bus: IEventBus) {
    init {
        Auxiliaries.init()
        Auxiliaries.logGitVersion()
        Registries.init()
        ModContent.init()
        bus.addListener(LiveCycleEvents::onConstruct)
        bus.addListener(LiveCycleEvents::onRegister)
        bus.addListener(LiveCycleEvents::onRegisterNetwork)
        NeoForge.EVENT_BUS.addListener { e: RegisterCommandsEvent -> DemoCommand.register(e.dispatcher) }
        CREATIVE_MODE_TABS.register(bus)
    }

    companion object {
        @JvmField val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ModConstants.MODID)

        // java.util.function.Supplier explicit cast resolves ambiguity with DeferredRegister.register(String, Function<ResourceLocation, I>) overload
        @JvmField val CREATIVE_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> =
            CREATIVE_MODE_TABS.register("tab_${ModConstants.MODID}", java.util.function.Supplier {
                CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tabredstonepen"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon { ItemStack(Registries.getItem("pen")) }
                    .displayItems { _, output ->
                        Registries.getRegisteredItems().forEach {
                            if (it !is BlockItem || it.block != ModContent.References.TRACK_BLOCK) output.accept(it)
                        }
                    }
                    .build()
            })
    }

    private class LiveCycleEvents {
        companion object {
            @JvmStatic fun onConstruct(event: FMLConstructModEvent) {
                RcaSync.CommonRca.init()
            }

            @JvmStatic fun onRegister(event: RegisterEvent) {
                if (event.registry.key().location().toString() != "minecraft:block") return
                Registries.instantiateAll()
                ModContent.initReferences()
            }

            @JvmStatic fun onRegisterNetwork(event: RegisterPayloadHandlersEvent) {
                val registrar: PayloadRegistrar = event.registrar("v1")
                NetworkingPlatformNeoForge.setRegistrar(registrar)
                Networking.init()
            }
        }
    }

    @EventBusSubscriber(modid = ModConstants.MODID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    class ClientEvents {
        companion object {
            @JvmStatic @SubscribeEvent @Suppress("UNCHECKED_CAST")
            fun onClientSetup(event: FMLClientSetupEvent) {
                Networking.OverlayTextMessage.setHandler(Overlay.TextOverlayGui::show)
                Overlay.on_config(0.75, 0x00ffaa00, 0x55333333, 0x55333333, 0x55444444)
                BlockEntityRenderers.register(
                    Registries.getBlockEntityTypeOfBlock("track") as BlockEntityType<TrackBlockEntity>
                ) { ctx -> ModRenderers.TrackTer(ctx) }
                if (RcaSync.ClientRca.init()) {
                    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST) { _: PlayerTickEvent.Post ->
                        RcaSync.ClientRca.tick()
                    }
                }
            }

            @JvmStatic @SubscribeEvent @Suppress("UNCHECKED_CAST")
            fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {
                event.register(
                    Registries.getMenuTypeOfBlock("control_box") as MenuType<ControlBoxUiContainer>,
                    ::ControlBoxGui
                )
            }

            @JvmStatic @SubscribeEvent
            fun onRegisterModels(event: ModelEvent.RegisterAdditional) {
                ModRenderers.TrackTer.registerModels()
                    .map { net.minecraft.client.resources.model.ModelResourceLocation(it, "standalone") }
                    .forEach(event::register)
            }
        }
    }

    @EventBusSubscriber(modid = ModConstants.MODID, value = [Dist.CLIENT])
    class ClientGameEvents {
        companion object {
            @JvmStatic @SubscribeEvent @OnlyIn(Dist.CLIENT)
            fun onRenderGui(event: net.neoforged.neoforge.client.event.RenderGuiEvent.Post) {
                Overlay.TextOverlayGui.INSTANCE.onRenderGui(event.guiGraphics)
            }

            @JvmStatic @SubscribeEvent @OnlyIn(Dist.CLIENT)
            fun onRenderWorldOverlay(event: RenderLevelStageEvent) {
                if (event.stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) {
                    Overlay.TextOverlayGui.INSTANCE.onRenderWorldOverlay(event.poseStack, event.renderTick.toDouble())
                }
            }
        }
    }
}
