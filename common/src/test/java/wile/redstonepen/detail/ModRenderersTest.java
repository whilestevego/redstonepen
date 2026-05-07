package wile.redstonepen.detail;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import wile.redstonepen.McBootstrap;
import wile.redstonepen.blocks.track.TrackBlockEntity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ModRenderersTest
{
  @BeforeAll
  static void bootstrap()
  { McBootstrap.bootstrap(); }

  // Fix 1: pushPose/popPose must be balanced even when the entity throws before any rendering.
  //
  // With the old code, popPose was outside the try block. If getBlockState() threw (which happens
  // before pushPose was ever called), popPose would drain the PoseStack's initial entry and leave
  // the stack empty. mxs.last() on an empty stack throws NoSuchElementException, corrupting
  // every renderer that runs afterward in the same frame.
  @Test
  void poseStackIsBalancedWhenEntityThrowsDuringRender()
  {
    final var ter = new ModRenderers.TrackTer(null);
    final var te  = mock(TrackBlockEntity.class);
    final var mxs = new PoseStack();
    final var buf = mock(MultiBufferSource.class);

    when(te.getStateFlags()).thenReturn(42L);
    when(te.getBlockPos()).thenReturn(new BlockPos(0, 0, 0));
    when(te.getBlockState()).thenThrow(new RuntimeException("simulated render fault"));

    ter.render(te, 0f, mxs, buf, 0, 0);

    // PoseStack must still have its initial entry after a render that threw internally.
    // getLast() throws NoSuchElementException if the deque was drained by an unmatched popPose.
    assertDoesNotThrow(mxs::last, "PoseStack was drained by an unmatched popPose");
  }

  // Fix 2: a broken entity must be isolated — it does not suppress rendering of other entities,
  // and it recovers automatically when its state changes.
  @Test
  void renderErrorIsIsolatedToFailingEntityAndClearsOnStateChange()
  {
    final var ter = new ModRenderers.TrackTer(null);
    final var teA = mock(TrackBlockEntity.class);
    final var teB = mock(TrackBlockEntity.class);
    final var mxs = new PoseStack();
    final var buf = mock(MultiBufferSource.class);

    when(teA.getStateFlags()).thenReturn(1L);
    when(teA.getBlockPos()).thenReturn(new BlockPos(0, 0, 0));
    when(teA.getBlockState()).thenThrow(new RuntimeException("entity A broken"));

    when(teB.getStateFlags()).thenReturn(2L);
    when(teB.getBlockPos()).thenReturn(new BlockPos(1, 0, 0));
    when(teB.getBlockState()).thenThrow(new RuntimeException("entity B broken"));

    // First render: both entities attempt and fail; getBlockState called once each.
    ter.render(teA, 0f, mxs, buf, 0, 0);
    ter.render(teB, 0f, mxs, buf, 0, 0);
    verify(teA, times(1)).getBlockState();
    verify(teB, times(1)).getBlockState();

    // Second render of A with same state: must be skipped entirely.
    // getBlockState must NOT be called again for A.
    ter.render(teA, 0f, mxs, buf, 0, 0);
    verify(teA, times(1)).getBlockState();

    // B is independently isolated: still skipped regardless of what happened to A.
    ter.render(teB, 0f, mxs, buf, 0, 0);
    verify(teB, times(1)).getBlockState();

    // State change on A: renderer must retry.
    when(teA.getStateFlags()).thenReturn(99L);
    ter.render(teA, 0f, mxs, buf, 0, 0);
    verify(teA, times(2)).getBlockState();
  }
}
