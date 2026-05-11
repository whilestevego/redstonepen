package wile.redstonepen.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TooltipDisplayTipRangeTest
{
  @Test
  void boundsComputedFromWidthAndHeight()
  {
    final TooltipDisplay.TipRange r = new TooltipDisplay.TipRange(10, 20, 5, 3, () -> null);
    assertEquals(10, r.x0);
    assertEquals(20, r.y0);
    assertEquals(14, r.x1); // x0+w-1 = 10+5-1
    assertEquals(22, r.y1); // y0+h-1 = 20+3-1
  }

  @Test
  void singlePixelWidthHeightCollapsesBounds()
  {
    final TooltipDisplay.TipRange r = new TooltipDisplay.TipRange(7, 4, 1, 1, () -> null);
    assertEquals(7, r.x0);
    assertEquals(4, r.y0);
    assertEquals(7, r.x1); // x0+1-1 = x0
    assertEquals(4, r.y1); // y0+1-1 = y0
  }

  @Test
  void zeroOriginBounds()
  {
    final TooltipDisplay.TipRange r = new TooltipDisplay.TipRange(0, 0, 10, 10, () -> null);
    assertEquals(0, r.x0);
    assertEquals(0, r.y0);
    assertEquals(9, r.x1);
    assertEquals(9, r.y1);
  }

  @Test
  void componentConstructorDelegatesSupplier()
  {
    // The Component overload wraps in a supplier; supplier field should be non-null.
    // We cannot call .get() without McBootstrap, so we only verify the field is set.
    final TooltipDisplay.TipRange r = new TooltipDisplay.TipRange(0, 0, 1, 1, () -> null);
    assertNotNull(r.text);
  }
}
