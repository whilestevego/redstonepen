package wile.redstonepen.client

import com.mojang.blaze3d.platform.Window
import com.mojang.blaze3d.systems.RenderSystem
import java.util.function.Consumer
import java.util.function.Function
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import wile.redstonepen.util.Auxiliaries

@Suppress("DEPRECATION")
object Guis {

    @Environment(EnvType.CLIENT)
    open class ContainerGui<T : AbstractContainerMenu>(
        menu: T,
        playerInv: Inventory,
        title: Component,
        bgImage: String,
        width: Int,
        height: Int,
    ) : AbstractContainerScreen<T>(menu, playerInv, title) {

        protected val bgImage: ResourceLocation =
            ResourceLocation.fromNamespaceAndPath(Auxiliaries.modid(), bgImage)
        protected val player: Player = playerInv.player
        protected val tooltip: TooltipDisplay = TooltipDisplay()

        init {
            imageWidth = width
            imageHeight = height
        }

        constructor(
            menu: T,
            playerInv: Inventory,
            title: Component,
            bgImage: String,
        ) : this(menu, playerInv, title, bgImage, 0, 0)

        override fun init() {
            super.init()
        }

        override fun render(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            renderBackground(gg, mouseX, mouseY, partialTicks)
            super.render(gg, mouseX, mouseY, partialTicks)
            if (!tooltip.render(gg, this, mouseX, mouseY)) renderTooltip(gg, mouseX, mouseY)
        }

        override fun renderLabels(gg: GuiGraphics, x: Int, y: Int) {}

        override fun renderBg(gg: GuiGraphics, partialTicks: Float, mouseX: Int, mouseY: Int) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.setShaderTexture(0, bgImage)
            gg.blit(bgImage, leftPos, topPos, 0, 0, imageWidth, imageHeight)
            renderBgWidgets(gg, partialTicks, mouseX, mouseY)
            RenderSystem.disableBlend()
        }

        fun getBackgroundImage(): ResourceLocation = bgImage

        fun getGuiLeft(): Int = leftPos

        fun getGuiTop(): Int = topPos

        protected open fun renderBgWidgets(
            gg: GuiGraphics,
            partialTicks: Float,
            mouseX: Int,
            mouseY: Int,
        ) {}

        protected fun renderItemTemplate(gg: GuiGraphics, stack: ItemStack, x: Int, y: Int) {
            val x0 = getGuiLeft()
            val y0 = getGuiTop()
            RenderSystem.disableColorLogicOp()
            RenderSystem.enableDepthTest()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderColor(0.8f, 0.8f, 0.8f, 0.4f)
            RenderSystem.enableBlend()
            gg.renderItem(stack, x0 + x, y0 + y)
            RenderSystem.colorMask(true, true, true, true)
            RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 0.4f)
            RenderSystem.setShaderTexture(0, bgImage)
            gg.blit(bgImage, x0 + x, y0 + y, x, y, 16, 16)
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()
        }
    }

    @Environment(EnvType.CLIENT)
    class Coord2d(@JvmField val x: Int, @JvmField val y: Int) {
        override fun toString(): String = "[$x,$y]"

        companion object {
            @JvmField val ORIGIN: Coord2d = Coord2d(0, 0)

            @JvmStatic fun of(x: Int, y: Int): Coord2d = Coord2d(x, y)
        }
    }

    @Environment(EnvType.CLIENT)
    open class UiWidget(x: Int, y: Int, width: Int, height: Int, title: Component) :
        net.minecraft.client.gui.components.AbstractWidget(x, y, width, height, title) {

        private val mc: Minecraft = Minecraft.getInstance()
        private var tooltipProvider: Function<UiWidget, Component> = NO_TOOLTIP
        private var parent: Screen? = null

        open fun init(parent: Screen): UiWidget {
            this.parent = parent
            x += if (parent is ContainerGui<*>) parent.getGuiLeft() else 0
            y += if (parent is ContainerGui<*>) parent.getGuiTop() else 0
            return this
        }

        open fun init(parent: Screen, position: Coord2d): UiWidget {
            this.parent = parent
            x = position.x + if (parent is ContainerGui<*>) parent.getGuiLeft() else 0
            y = position.y + if (parent is ContainerGui<*>) parent.getGuiTop() else 0
            return this
        }

        fun tooltip(tip: Function<UiWidget, Component>): UiWidget {
            tooltipProvider = tip
            return this
        }

        fun tooltip(tip: Component): UiWidget {
            tooltipProvider = Function { tip }
            return this
        }

        override fun getWidth(): Int = width

        override fun getHeight(): Int = height

        fun getMousePosition(): Coord2d {
            val win: Window = mc.window
            return Coord2d.of(
                Mth.clamp(
                    (mc.mouseHandler.xpos() * win.guiScaledWidth / win.screenWidth).toInt() - x,
                    -1,
                    width + 1,
                ),
                Mth.clamp(
                    (mc.mouseHandler.ypos() * win.guiScaledHeight / win.screenHeight).toInt() - y,
                    -1,
                    height + 1,
                ),
            )
        }

        protected fun screenCoordinates(xy: Coord2d, reverse: Boolean): Coord2d =
            if (reverse) {
                Coord2d.of(xy.x + x, xy.y + y)
            } else {
                Coord2d.of(xy.x - x, xy.y - y)
            }

        fun show(): UiWidget {
            visible = true
            return this
        }

        fun hide(): UiWidget {
            visible = false
            return this
        }

        override fun updateWidgetNarration(neo: NarrationElementOutput) {}

        override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            if (isHovered) renderToolTip(gg, mouseX, mouseY)
            tooltip = null
        }

        @Suppress("all")
        fun renderToolTip(gg: GuiGraphics, mouseX: Int, mouseY: Int) {
            if (!visible || !active || tooltipProvider === NO_TOOLTIP) return
            val tip = tooltipProvider.apply(this)
            if (tip.string.trim().isEmpty()) return
            gg.renderTooltip(mc.font, listOf(tip.visualOrderText), mouseX, mouseY)
        }

        companion object {
            @JvmField val EMPTY_TEXT: Component = Component.literal("")
            val NO_TOOLTIP: Function<UiWidget, Component> = Function { EMPTY_TEXT }
        }
    }

    @Environment(EnvType.CLIENT)
    class CheckBox(
        private val atlas: ResourceLocation,
        width: Int,
        height: Int,
        atlasTexturePositionOff: Coord2d,
        atlasTexturePositionOn: Coord2d,
    ) : UiWidget(0, 0, width, height, EMPTY_TEXT) {

        private val texturePositionOff: Coord2d = atlasTexturePositionOff
        private val texturePositionOn: Coord2d = atlasTexturePositionOn
        private var isChecked: Boolean = false
        private var onClickAction: Consumer<CheckBox> = Consumer {}

        fun checked(): Boolean = isChecked

        fun checked(on: Boolean): CheckBox {
            isChecked = on
            return this
        }

        fun onclick(action: Consumer<CheckBox>): CheckBox {
            onClickAction = action
            return this
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            isChecked = !isChecked
            onClickAction.accept(this)
        }

        override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader)
            RenderSystem.setShaderTexture(0, atlas)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            val pos = if (isChecked) texturePositionOn else texturePositionOff
            gg.blit(atlas, x, y, pos.x, pos.y, width, height)
            if (isHovered) renderToolTip(gg, mouseX, mouseY)
        }
    }

    @Environment(EnvType.CLIENT)
    class ImageButton(
        private val atlas: ResourceLocation,
        width: Int,
        height: Int,
        atlasTexturePosition: Coord2d,
    ) : UiWidget(0, 0, width, height, Component.empty()) {

        private val texturePosition: Coord2d = atlasTexturePosition
        private var onClickAction: Consumer<ImageButton> = Consumer {}

        fun onclick(action: Consumer<ImageButton>): ImageButton {
            onClickAction = action
            return this
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            onClickAction.accept(this)
        }

        override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader)
            RenderSystem.setShaderTexture(0, atlas)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            gg.blit(atlas, x, y, texturePosition.x, texturePosition.y, width, height)
            if (isHovered) renderToolTip(gg, mouseX, mouseY)
        }
    }

    @Environment(EnvType.CLIENT)
    class Image(
        private val atlas: ResourceLocation,
        width: Int,
        height: Int,
        atlasTexturePosition: Coord2d,
    ) : UiWidget(0, 0, width, height, Component.empty()) {

        private val texturePosition: Coord2d = atlasTexturePosition

        override fun onClick(mouseX: Double, mouseY: Double) {}

        override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader)
            RenderSystem.setShaderTexture(0, atlas)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            gg.blit(atlas, x, y, texturePosition.x, texturePosition.y, width, height)
            if (isHovered) renderToolTip(gg, mouseX, mouseY)
        }
    }

    @Environment(EnvType.CLIENT)
    class TextBox(x: Int, y: Int, width: Int, height: Int, title: Component, font: Font) :
        net.minecraft.client.gui.components.EditBox(font, x, y, width, height, title) {

        init {
            isBordered = false
        }

        fun withMaxLength(len: Int): TextBox {
            super.setMaxLength(len)
            return this
        }

        fun withBordered(b: Boolean): TextBox {
            super.setBordered(b)
            return this
        }

        fun withValue(s: String): TextBox {
            super.setValue(s)
            return this
        }

        fun withEditable(e: Boolean): TextBox {
            super.setEditable(e)
            return this
        }

        fun withResponder(r: Consumer<String>): TextBox {
            super.setResponder(r)
            return this
        }
    }
}
