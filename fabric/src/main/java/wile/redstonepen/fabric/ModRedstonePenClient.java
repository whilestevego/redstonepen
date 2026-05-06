package wile.redstonepen.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import wile.redstonepen.ModContent;
import wile.redstonepen.blocks.ControlBox;
import wile.redstonepen.blocks.RedstoneTrack;
import wile.redstonepen.libmc.NetworkingClient;
import wile.redstonepen.libmc.Overlay;
import wile.redstonepen.libmc.PlatformServices;
import wile.redstonepen.libmc.Registries;

@Environment(EnvType.CLIENT)
public class ModRedstonePenClient implements ClientModInitializer
{
  static {
    ModelLoadingPlugin.register(ctx ->
      ctx.addModels(wile.redstonepen.detail.ModRenderers.TrackTer.registerModels()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onInitializeClient()
  {
    NetworkingClient.clientInit();
    Overlay.register();
    MenuScreens.register(
      (MenuType<ControlBox.ControlBoxUiContainer>) Registries.getMenuTypeOfBlock("control_box"),
      ControlBox.ControlBoxGui::new);
    BlockEntityRenderers.register(
      (BlockEntityType<RedstoneTrack.TrackBlockEntity>) Registries.getBlockEntityTypeOfBlock("track"),
      wile.redstonepen.detail.ModRenderers.TrackTer::new);
    PlatformServices.getRendering().setRenderLayer(ModContent.references.TRACK_BLOCK, RenderType.cutout());
    PlatformServices.getRendering().setRenderLayer(ModContent.references.BASIC_GAUGE_BLOCK, RenderType.translucent());
    Overlay.on_config(0.75, 0x00ffaa00, 0x55333333, 0x55333333, 0x55444444);
    WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, ignored) -> {
      Overlay.TextOverlayGui.INSTANCE.onRenderWorldOverlay(context.matrixStack(), context.tickCounter().getRealtimeDeltaTicks());
      return true;
    });
    if(wile.redstonepen.detail.RcaSync.ClientRca.init()) {
      ClientTickEvents.END_CLIENT_TICK.register(mc -> {
        if((mc.level == null) || (mc.level.getGameTime() & 0x1) != 0) return;
        wile.redstonepen.detail.RcaSync.ClientRca.tick();
      });
    }
  }
}
