package wile.redstonepen.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import wile.redstonepen.client.Guis.Coord2d;

class GuisCoord2dTest
{
  @Test
  void constructorStoresXAndY()
  {
    final Coord2d c = new Coord2d(3, 7);
    assertEquals(3, c.x);
    assertEquals(7, c.y);
  }

  @Test
  void ofFactoryCreatesEquivalentCoord()
  {
    final Coord2d a = new Coord2d(5, 9);
    final Coord2d b = Coord2d.of(5, 9);
    assertEquals(a.x, b.x);
    assertEquals(a.y, b.y);
  }

  @Test
  void originIsZeroZero()
  {
    assertEquals(0, Coord2d.ORIGIN.x);
    assertEquals(0, Coord2d.ORIGIN.y);
  }

  @Test
  void toStringFormatsAsBracketedXCommaY()
  {
    assertEquals("[3,4]", new Coord2d(3, 4).toString());
  }

  @Test
  void toStringZeroZero()
  {
    assertEquals("[0,0]", Coord2d.ORIGIN.toString());
  }

  @Test
  void toStringNegativeValues()
  {
    assertEquals("[-1,-2]", new Coord2d(-1, -2).toString());
  }
}
