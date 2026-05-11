package wile.redstonepen.client;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wile.redstonepen.McBootstrap;

import static org.junit.jupiter.api.Assertions.*;

class OverlayTextOverlayGuiTest
{
  @BeforeAll
  static void bootstrap()
  { McBootstrap.bootstrap(); }

  @BeforeEach
  void reset()
  { Overlay.TextOverlayGui.hide(); }

  @Test
  void hideZerosDeadlineAndClearsText()
  {
    Overlay.TextOverlayGui.show("hello", 5000);
    Overlay.TextOverlayGui.hide();
    assertEquals(0, Overlay.TextOverlayGui.deadline());
    assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text());
  }

  @Test
  void showStringUpdatesTextToMatchInput()
  {
    Overlay.TextOverlayGui.show("ping", 1000);
    assertEquals("ping", Overlay.TextOverlayGui.text().getString());
  }

  @Test
  void showNullStringFallsBackToEmptyText()
  {
    Overlay.TextOverlayGui.show((String) null, 1000);
    assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text());
  }

  @Test
  void showEmptyStringFallsBackToEmptyText()
  {
    Overlay.TextOverlayGui.show("", 1000);
    assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text());
  }

  @Test
  void showStringSetsDeadlineInFuture()
  {
    final long before = System.currentTimeMillis();
    Overlay.TextOverlayGui.show("x", 500);
    assertTrue(Overlay.TextOverlayGui.deadline() >= before + 500);
  }

  @Test
  void showComponentUpdatesTextToMatchInput()
  {
    Overlay.TextOverlayGui.show(Component.literal("world"), 1000);
    assertEquals("world", Overlay.TextOverlayGui.text().getString());
  }

  @Test
  void showNullComponentFallsBackToEmptyText()
  {
    Overlay.TextOverlayGui.show((Component) null, 1000);
    assertEquals(Overlay.TextOverlayGui.EMPTY_TEXT, Overlay.TextOverlayGui.text());
  }

  @Test
  void showComponentSetsDeadlineInFuture()
  {
    final long before = System.currentTimeMillis();
    Overlay.TextOverlayGui.show(Component.literal("x"), 1000);
    assertTrue(Overlay.TextOverlayGui.deadline() >= before + 1000);
  }
}
