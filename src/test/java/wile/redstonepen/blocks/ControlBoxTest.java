package wile.redstonepen.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import wile.redstonepen.blocks.ControlBox.ControlBoxBlockEntity.TestHooks;

class ControlBoxTest
{
  @Test
  void rejectsUnknownSignalSuffixes()
  {
    final TestHooks hooks = new TestHooks();

    assertFalse(hooks.setCode("b=d.bad"));
    assertFalse(hooks.errors().isEmpty());
  }

  @Test
  void derivesInputAndOutputMasksFromProgram()
  {
    final TestHooks hooks = new TestHooks();

    assertTrue(hooks.setCode("b=d\nu=15"));
    assertTrue(hooks.valid());
    assertTrue((hooks.inputMask() & 0xf) != 0, "expected input mask for d");
    assertTrue((hooks.outputMask() & (0xf << (4 * Direction.EAST.ordinal()))) != 0, "expected output mask for b");
    assertTrue((hooks.outputMask() & (0xf << (4 * Direction.UP.ordinal()))) != 0, "expected output mask for u");
  }

  @Test
  void tickEvaluatesAssignmentsFromInputs()
  {
    final TestHooks hooks = new TestHooks();

    assertTrue(hooks.setCode("b=d+1"));
    hooks.setInput(Direction.DOWN, 4);
    hooks.tick();

    assertEquals(5, hooks.output(Direction.EAST));
  }

  @Test
  void tivTimerUsesClockSymbolForPulseGeneration()
  {
    final TestHooks hooks = new TestHooks();

    assertTrue(hooks.setCode("b=tiv1(3)"));

    hooks.setSymbol(".clock", 0);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));

    hooks.setSymbol(".clock", 3);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }
}
