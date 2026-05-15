package wile.redstonepen.client

import com.mojang.blaze3d.platform.Window
import java.util.Optional
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Font
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.util.Tuple
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import org.jetbrains.annotations.Nullable
import wile.redstonepen.ModConstants
import wile.redstonepen.net.Networking

object Overlay {

    @JvmStatic
    @Environment(EnvType.CLIENT)
    fun register() {
        Networking.OverlayTextMessage.setHandler(TextOverlayGui::show)
    }

    @JvmStatic
    fun show(player: ServerPlayer, message: Component) {
        Networking.OverlayTextMessage.sendToPlayer(player, message, 3000)
    }

    @JvmStatic
    fun show(player: ServerPlayer, message: Component, delay: Int) {
        Networking.OverlayTextMessage.sendToPlayer(player, message, delay)
    }

    @JvmStatic fun show(state: BlockState, pos: BlockPos) = show(state, pos, 100)

    @JvmStatic
    fun show(state: BlockState, pos: BlockPos, displayTimeoutMs: Int) {
        net.minecraft.client.Minecraft.getInstance().execute {
            TextOverlayGui.show(state, pos, displayTimeoutMs)
        }
    }

    private var overlay_y_: Double = 0.75
    private var text_color_: Int = 0x00ffaa00
    private var border_color_: Int = 0xaa333333.toInt()
    private var background_color1_: Int = 0xaa333333.toInt()
    private var background_color2_: Int = 0xaa444444.toInt()

    @JvmStatic
    fun on_config(overlayY: Double) {
        on_config(overlayY, 0x00ffaa00, 0xaa333333.toInt(), 0xaa333333.toInt(), 0xaa444444.toInt())
    }

    @JvmStatic
    fun on_config(
        overlayY: Double,
        textColor: Int,
        borderColor: Int,
        backgroundColor1: Int,
        backgroundColor2: Int,
    ) {
        overlay_y_ = overlayY
        text_color_ = textColor
        border_color_ = borderColor
        background_color1_ = backgroundColor1
        background_color2_ = backgroundColor2
    }

    @Environment(EnvType.CLIENT)
    class TextOverlayGui :
        net.minecraft.client.gui.screens.Screen(Component.literal(ModConstants.MODID + "Overlay")) {

        @Environment(EnvType.CLIENT)
        fun onRenderGui(gg: net.minecraft.client.gui.GuiGraphics) {
            if (deadline() < System.currentTimeMillis()) return
            if (text() === EMPTY_TEXT) return
            val txt = text().string
            if (txt.isEmpty()) return
            val mc = net.minecraft.client.Minecraft.getInstance()
            val win: Window = mc.window
            val fr: Font = mc.font
            val cx = win.guiScaledWidth / 2
            val cy = (win.guiScaledHeight * overlay_y_).toInt()
            val w = fr.width(txt)
            val h = fr.lineHeight
            gg.fillGradient(
                cx - (w / 2) - 3,
                cy - 2,
                cx + (w / 2) + 2,
                cy + h + 2,
                0xaa333333.toInt(),
                0xaa444444.toInt(),
            )
            gg.hLine(cx - (w / 2) - 3, cx + (w / 2) + 2, cy - 2, 0xaa333333.toInt())
            gg.hLine(cx - (w / 2) - 3, cx + (w / 2) + 2, cy + h + 2, 0xaa333333.toInt())
            gg.vLine(cx - (w / 2) - 3, cy - 2, cy + h + 2, 0xaa333333.toInt())
            gg.vLine(cx + (w / 2) + 2, cy - 2, cy + h + 2, 0xaa333333.toInt())
            gg.drawCenteredString(fr, text(), cx, cy + 1, 0x00ffaa00)
        }

        @Environment(EnvType.CLIENT)
        fun onRenderWorldOverlay(mxs: com.mojang.blaze3d.vertex.PoseStack, partialTick: Double) {
            val sp = state_pos()
            if (sp.isEmpty) return
            val mc = net.minecraft.client.Minecraft.getInstance()
            val world = mc.level
            val player = mc.player
            if (player == null || world == null) return
            val state = sp.get().a
            val pos = sp.get().b

            @Suppress("DEPRECATION")
            val light =
                if (world.hasChunkAt(pos)) {
                    net.minecraft.client.renderer.LightTexture.pack(
                        world.getBrightness(LightLayer.BLOCK, pos),
                        world.getBrightness(LightLayer.SKY, pos),
                    )
                } else {
                    net.minecraft.client.renderer.LightTexture.pack(15, 15)
                }
            val buffer = mc.renderBuffers().bufferSource()
            val px = Mth.lerp(partialTick, player.xo, player.x)
            val py = Mth.lerp(partialTick, player.yo, player.y)
            val pz = Mth.lerp(partialTick, player.zo, player.z)
            mxs.pushPose()
            mxs.translate((pos.x - px), (pos.y - py - player.getEyeHeight()), (pos.z - pz))
            mc.blockRenderer.renderSingleBlock(
                state,
                mxs,
                buffer,
                light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            )
            mxs.popPose()
        }

        companion object {
            @JvmField val INSTANCE: TextOverlayGui = TextOverlayGui()

            @JvmField val EMPTY_TEXT: Component = Component.literal("")
            val EMPTY_STATE: BlockState? = null
            private var text_deadline_: Long = 0
            private var text_: Component = EMPTY_TEXT
            private var state_deadline_: Long = 0

            @Nullable private var state_: BlockState? = EMPTY_STATE
            private var pos_: BlockPos = BlockPos.ZERO

            @JvmStatic @Synchronized fun text(): Component = text_

            @JvmStatic @Synchronized fun deadline(): Long = text_deadline_

            @JvmStatic
            @Synchronized
            fun hide() {
                text_deadline_ = 0
                text_ = EMPTY_TEXT
            }

            @JvmStatic
            @Synchronized
            fun show(s: Component?, displayTimeoutMs: Int) {
                text_ = s?.copy() ?: EMPTY_TEXT
                text_deadline_ = System.currentTimeMillis() + displayTimeoutMs
            }

            @JvmStatic
            @Synchronized
            fun show(s: String?, displayTimeoutMs: Int) {
                text_ = if (s.isNullOrEmpty()) EMPTY_TEXT else Component.literal(s)
                text_deadline_ = System.currentTimeMillis() + displayTimeoutMs
            }

            @JvmStatic
            @Synchronized
            fun show(state: BlockState, pos: BlockPos, displayTimeoutMs: Int) {
                pos_ = BlockPos(pos)
                state_ = state
                state_deadline_ = System.currentTimeMillis() + displayTimeoutMs
            }

            @JvmStatic
            @Synchronized
            private fun state_pos(): Optional<Tuple<BlockState, BlockPos>> {
                val s = state_
                return if (state_deadline_ < System.currentTimeMillis() || s == null) {
                    Optional.empty()
                } else {
                    Optional.of(Tuple(s, pos_))
                }
            }
        }
    }
}
