package wile.redstonepen.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.entity.BlockEntityType
import wile.redstonepen.ModContent
import wile.redstonepen.blocks.controlbox.ControlBoxUiContainer
import wile.redstonepen.blocks.controlbox.client.ControlBoxGui
import wile.redstonepen.blocks.track.TrackBlockEntity
import wile.redstonepen.client.Overlay
import wile.redstonepen.detail.ModRenderers
import wile.redstonepen.detail.RcaSync
import wile.redstonepen.net.NetworkingClient
import wile.redstonepen.platform.PlatformServices
import wile.redstonepen.registry.Registries

@Environment(EnvType.CLIENT)
class ModRedstonePenClient : ClientModInitializer {
    companion object {
        init {
            ModelLoadingPlugin.register { ctx ->
                ctx.addModels(ModRenderers.TrackTer.registerModels())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onInitializeClient() {
        NetworkingClient.clientInit()
        Overlay.register()
        MenuScreens.register(
            Registries.getMenuTypeOfBlock("control_box") as MenuType<ControlBoxUiContainer>,
            ::ControlBoxGui,
        )
        BlockEntityRenderers.register(
            Registries.getBlockEntityTypeOfBlock("track") as BlockEntityType<TrackBlockEntity>
        ) { ctx ->
            ModRenderers.TrackTer(ctx)
        }
        PlatformServices.getRendering()
            .setRenderLayer(ModContent.References.TRACK_BLOCK, RenderType.cutout())
        PlatformServices.getRendering()
            .setRenderLayer(ModContent.References.BASIC_GAUGE_BLOCK, RenderType.translucent())
        Overlay.on_config(0.75, 0x00ffaa00, 0x55333333, 0x55333333, 0x55444444)
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register { context, _ ->
            val matrixStack = context.matrixStack() ?: return@register true
            Overlay.TextOverlayGui.INSTANCE.onRenderWorldOverlay(
                matrixStack,
                context.tickCounter().realtimeDeltaTicks.toDouble(),
            )
            true
        }
        if (RcaSync.ClientRca.init()) {
            ClientTickEvents.END_CLIENT_TICK.register { mc ->
                val level = mc.level ?: return@register
                if (level.gameTime and 0x1L != 0L) return@register
                RcaSync.ClientRca.tick()
            }
        }
    }
}
