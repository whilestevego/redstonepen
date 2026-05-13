package wile.redstonepen.detail

import com.mojang.blaze3d.vertex.PoseStack
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import wile.redstonepen.blocks.track.TrackBlockEntity

class ModRenderersTest :
    DescribeSpec({
        describe("TrackTer render") {
            it("pose stack is balanced when entity throws during render") {
                val ter = ModRenderers.TrackTer(null)
                val te = mockk<TrackBlockEntity>()
                val mxs = PoseStack()
                val buf = mockk<MultiBufferSource>()

                every { te.getStateFlags() } returns 42L
                every { te.blockPos } returns BlockPos(0, 0, 0)
                every { te.blockState } throws RuntimeException("simulated render fault")

                ter.render(te, 0f, mxs, buf, 0, 0)

                mxs.last()
            }

            it("render error is isolated to failing entity and clears on state change") {
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
    })
