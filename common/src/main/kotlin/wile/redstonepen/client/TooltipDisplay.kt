package wile.redstonepen.client

import java.util.Optional
import java.util.function.Supplier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.inventory.AbstractContainerMenu
import wile.redstonepen.util.Auxiliaries

@Environment(EnvType.CLIENT)
class TooltipDisplay {

    class TipRange(x: Int, y: Int, w: Int, h: Int, text: Supplier<Component?>) {
        @JvmField val x0: Int = x

        @JvmField val y0: Int = y

        @JvmField val x1: Int = x0 + w - 1

        @JvmField val y1: Int = y0 + h - 1

        @JvmField val text: Supplier<Component?> = text

        constructor(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            text: Component,
        ) : this(x, y, w, h, Supplier { text })
    }

    private var ranges: List<TipRange> = ArrayList()
    private var delay: Long = defaultDelay
    private var maxDeviation: Int = defaultMaxDeviation
    private var xLast: Int = 0
    private var yLast: Int = 0
    private var t: Long = System.currentTimeMillis()
    private val font = Minecraft.getInstance().font

    fun init(ranges: List<TipRange>, delayMs: Long, maxDeviationXy: Int): TooltipDisplay {
        this.ranges = ranges
        this.delay = delayMs
        this.maxDeviation = maxDeviationXy
        t = System.currentTimeMillis()
        xLast = 0
        yLast = 0
        return this
    }

    fun init(ranges: List<TipRange>): TooltipDisplay =
        init(ranges, defaultDelay, defaultMaxDeviation)

    fun init(vararg ranges: TipRange): TooltipDisplay =
        init(ranges.toList(), defaultDelay, defaultMaxDeviation)

    fun delay(ms: Int): TooltipDisplay {
        delay = if (ms <= 0) defaultDelay else ms.toLong()
        return this
    }

    fun resetTimer() {
        t = System.currentTimeMillis()
    }

    @Suppress("UnusedParameter", "TooGenericExceptionCaught")
    fun <T : AbstractContainerMenu> render(
        gg: GuiGraphics,
        gui: AbstractContainerScreen<T>,
        x: Int,
        y: Int,
    ): Boolean {
        if (hadRenderException) return false
        if (Math.abs(x - xLast) > maxDeviation || Math.abs(y - yLast) > maxDeviation) {
            xLast = x
            yLast = y
            resetTimer()
            return false
        } else if (Math.abs(System.currentTimeMillis() - t) < delay) {
            return false
        } else if (
            ranges.stream().noneMatch { tip ->
                val outsideX = x < tip.x0 || x > tip.x1
                val outsideY = y < tip.y0 || y > tip.y1
                val outside = outsideX || outsideY
                if (outside) return@noneMatch false
                val tipComponent = tip.text.get() ?: return@noneMatch false
                if (tipComponent.string.isEmpty()) return@noneMatch false
                try {
                    val lines = Auxiliaries.wrapText(tipComponent, 80)
                    gg.renderTooltip(font, lines, Optional.empty(), x, y)
                } catch (ex: Exception) {
                    hadRenderException = true
                    Auxiliaries.logError(
                        "Tooltip rendering disabled due to exception: '${ex.message}'"
                    )
                    return@noneMatch false
                }
                true
            }
        ) {
            resetTimer()
            return false
        } else {
            return true
        }
    }

    companion object {
        private var defaultDelay: Long = 450
        private var defaultMaxDeviation: Int = 1
        private var hadRenderException: Boolean = false

        @JvmStatic
        fun config(delay: Int, maxDeviation: Int) {
            defaultDelay = Mth.clamp(delay, 500, 5000).toLong()
            defaultMaxDeviation = Mth.clamp(maxDeviation, 1, 5)
        }
    }
}
