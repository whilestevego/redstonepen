package wile.redstonepen.detail

import com.mojang.blaze3d.vertex.PoseStack
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import wile.redstonepen.McBootstrap
import wile.redstonepen.blocks.track.TrackBlockEntity

class ModRenderersTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() = McBootstrap.bootstrap()
    }

    @Test fun poseStackIsBalancedWhenEntityThrowsDuringRender() {
        val ter = ModRenderers.TrackTer(null)
        val te  = mockk<TrackBlockEntity>()
        val mxs = PoseStack()
        val buf = mockk<MultiBufferSource>()

        every { te.getStateFlags() } returns 42L
        every { te.blockPos } returns BlockPos(0, 0, 0)
        every { te.blockState } throws RuntimeException("simulated render fault")

        ter.render(te, 0f, mxs, buf, 0, 0)

        assertDoesNotThrow({ mxs.last() }, "PoseStack was drained by an unmatched popPose")
    }

    @Test fun renderErrorIsIsolatedToFailingEntityAndClearsOnStateChange() {
        val ter = ModRenderers.TrackTer(null)
        val teA = mockk<TrackBlockEntity>()
        val teB = mockk<TrackBlockEntity>()
        val mxs = PoseStack()
        val buf = mockk<MultiBufferSource>()

        every { teA.getStateFlags() } returns 1L
        every { teA.blockPos } returns BlockPos(0, 0, 0)
        every { teA.blockState } throws RuntimeException("entity A broken")

        every { teB.getStateFlags() } returns 2L
        every { teB.blockPos } returns BlockPos(1, 0, 0)
        every { teB.blockState } throws RuntimeException("entity B broken")

        ter.render(teA, 0f, mxs, buf, 0, 0)
        ter.render(teB, 0f, mxs, buf, 0, 0)
        verify(exactly = 1) { teA.blockState }
        verify(exactly = 1) { teB.blockState }

        ter.render(teA, 0f, mxs, buf, 0, 0)
        verify(exactly = 1) { teA.blockState }

        ter.render(teB, 0f, mxs, buf, 0, 0)
        verify(exactly = 1) { teB.blockState }

        every { teA.getStateFlags() } returns 99L
        ter.render(teA, 0f, mxs, buf, 0, 0)
        verify(exactly = 2) { teA.blockState }
    }
}
