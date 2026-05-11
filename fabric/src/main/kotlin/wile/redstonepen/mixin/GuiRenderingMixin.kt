package wile.redstonepen.mixin

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import wile.redstonepen.client.Overlay

@Environment(EnvType.CLIENT)
@Mixin(net.minecraft.client.gui.Gui::class)
abstract class GuiRenderingMixin {
    @Inject(at = [At(value = "TAIL")], method = ["render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"])
    private fun render(gg: net.minecraft.client.gui.GuiGraphics, partialTicks: net.minecraft.client.DeltaTracker, info: CallbackInfo) {
        if (Overlay.TextOverlayGui.deadline() < System.currentTimeMillis()) return
        if (Overlay.TextOverlayGui.text() === Overlay.TextOverlayGui.EMPTY_TEXT) return
        Overlay.TextOverlayGui.INSTANCE.onRenderGui(gg)
    }
}
