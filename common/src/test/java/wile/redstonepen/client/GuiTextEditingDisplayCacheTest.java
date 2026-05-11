package wile.redstonepen.client;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import wile.redstonepen.McBootstrap;

import static org.junit.jupiter.api.Assertions.*;

class GuiTextEditingDisplayCacheTest
{
  @BeforeAll
  static void bootstrap()
  { McBootstrap.bootstrap(); }

  // --- helpers ---

  private static GuiTextEditing.MultiLineTextBox.DisplayCache twoLineCache()
  {
    // Simulates "hello\nworld": line 0 starts at 0 ("hello"), line 1 starts at 6 ("world").
    final int[] lineStarts = { 0, 6 };
    final GuiTextEditing.MultiLineTextBox.LineInfo[] lines = {
      new GuiTextEditing.MultiLineTextBox.LineInfo(Style.EMPTY, "hello", 0, 0),
      new GuiTextEditing.MultiLineTextBox.LineInfo(Style.EMPTY, "world", 0, 9)
    };
    return new GuiTextEditing.MultiLineTextBox.DisplayCache(
      "hello\nworld", new Guis.Coord2d(0, 0), false, lineStarts, lines, new Rect2i[0]
    );
  }

  // --- DisplayCache.EMPTY ---

  @Test
  void emptyDisplayCacheHasCursorAtEnd()
  { assertTrue(GuiTextEditing.MultiLineTextBox.DisplayCache.EMPTY.cursorAtEnd); }

  @Test
  void emptyDisplayCacheHasCursorAtOrigin()
  {
    final Guis.Coord2d c = GuiTextEditing.MultiLineTextBox.DisplayCache.EMPTY.cursor;
    assertEquals(0, c.x);
    assertEquals(0, c.y);
  }

  // --- changeLine ---

  @Test
  void changeLineReturnsUnchangedPosWhenMovingPastLastLine()
  {
    final GuiTextEditing.MultiLineTextBox.DisplayCache cache = twoLineCache();
    // cursor at position 7 (line 1), moving down 1 — already on last line
    assertEquals(7, cache.changeLine(7, 1));
  }

  @Test
  void changeLineReturnsUnchangedPosWhenMovingBeforeFirstLine()
  {
    final GuiTextEditing.MultiLineTextBox.DisplayCache cache = twoLineCache();
    // cursor at position 2 (line 0), moving up 1 — already on first line
    assertEquals(2, cache.changeLine(2, -1));
  }

  @Test
  void changeLineMovesDownPreservingColumnOffset()
  {
    final GuiTextEditing.MultiLineTextBox.DisplayCache cache = twoLineCache();
    // cursor at position 2 (offset 2 into "hello"), moving down 1 line
    // → lineStarts[1] + min(2, "world".length()) = 6 + 2 = 8
    assertEquals(8, cache.changeLine(2, 1));
  }

  @Test
  void changeLineMovesUpPreservingColumnOffset()
  {
    final GuiTextEditing.MultiLineTextBox.DisplayCache cache = twoLineCache();
    // cursor at position 9 (offset 3 into "world"), moving up 1 line
    // → lineStarts[0] + min(3, "hello".length()) = 0 + 3 = 3
    assertEquals(3, cache.changeLine(9, -1));
  }

  @Test
  void changeLineClampedToLineLength()
  {
    // cursor column offset exceeds target line length — should clamp to end of target line
    final GuiTextEditing.MultiLineTextBox.DisplayCache cache = twoLineCache();
    // cursor at position 4 (offset 4 into "hello"), moving down to "world" (length 5) — no clamp needed here
    // Use a wider offset: position 5 (end of "hello") moving down to "world" (length 5) → 6+5=11
    assertEquals(11, cache.changeLine(5, 1));
  }

  // --- findLineStart ---

  @Test
  void findLineStartForFirstLineReturnsZero()
  {
    assertEquals(0, twoLineCache().findLineStart(0));
  }

  @Test
  void findLineStartForMidFirstLineReturnsZero()
  {
    assertEquals(0, twoLineCache().findLineStart(3));
  }

  @Test
  void findLineStartForSecondLineReturnsItsStart()
  {
    assertEquals(6, twoLineCache().findLineStart(7));
  }

  // --- findLineEnd ---

  @Test
  void findLineEndForFirstLineReturnsLengthOfContents()
  {
    // lineStarts[0]=0, lines[0].contents="hello" (length 5) → 0+5=5
    assertEquals(5, twoLineCache().findLineEnd(2));
  }

  @Test
  void findLineEndForSecondLineReturnsItsEnd()
  {
    // lineStarts[1]=6, lines[1].contents="world" (length 5) → 6+5=11
    assertEquals(11, twoLineCache().findLineEnd(8));
  }
}
