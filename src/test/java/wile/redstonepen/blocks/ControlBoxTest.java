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

  // --- Parsing validation ---

  @Test
  void emptyCodeIsValidWithNoMasks()
  {
    final TestHooks hooks = new TestHooks();

    assertTrue(hooks.setCode(""));
    assertTrue(hooks.valid());
    assertEquals(0, hooks.inputMask());
    assertEquals(0, hooks.outputMask());
  }

  @Test
  void commentOnlyLineIsValid()
  {
    final TestHooks hooks = new TestHooks();

    assertTrue(hooks.setCode("# this is a comment"));
    assertTrue(hooks.valid());
    assertEquals(0, hooks.inputMask());
    assertEquals(0, hooks.outputMask());
  }

  @Test
  void unknownFunctionCallIsInvalid()
  {
    final TestHooks hooks = new TestHooks();

    assertFalse(hooks.setCode("b=foo(d)"));
    assertFalse(hooks.valid());
    assertFalse(hooks.errors().isEmpty());
  }

  @Test
  void allPortNamesAcceptedOnLhs()
  {
    for(String port : new String[]{"d", "u", "r", "y", "g", "b"}) {
      final TestHooks hooks = new TestHooks();
      assertTrue(hooks.setCode(port + "=5"), "port '" + port + "' should be a valid LHS assignment");
    }
  }

  // --- Arithmetic operators ---

  @Test
  void additionOperatorOutputsSum()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=3+4"));
    hooks.tick();
    assertEquals(7, hooks.output(Direction.EAST));
  }

  @Test
  void subtractionOperatorOutputsDifference()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=15-3"));
    hooks.tick();
    assertEquals(12, hooks.output(Direction.EAST));
  }

  @Test
  void multiplicationOperatorOutputsProduct()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=3*4"));
    hooks.tick();
    assertEquals(12, hooks.output(Direction.EAST));
  }

  @Test
  void divisionOperatorOutputsQuotient()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=8/2"));
    hooks.tick();
    assertEquals(4, hooks.output(Direction.EAST));
  }

  @Test
  void moduloOperatorOutputsRemainder()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=9%5"));
    hooks.tick();
    assertEquals(4, hooks.output(Direction.EAST));
  }

  @Test
  void divisionByZeroReturnsZero()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d/0"));
    hooks.setInput(Direction.DOWN, 6);
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  // --- Comparison operators ---

  @Test
  void greaterThanReturnsTrueWhenInputExceedsThreshold()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d>5"));
    hooks.setInput(Direction.DOWN, 6);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  @Test
  void greaterThanReturnsFalseWhenInputEqualsThreshold()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d>5"));
    hooks.setInput(Direction.DOWN, 5);
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  @Test
  void greaterThanOrEqualReturnsTrueAtBoundary()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d>=5"));
    hooks.setInput(Direction.DOWN, 5);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  @Test
  void greaterThanOrEqualReturnsFalseBelowBoundary()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d>=5"));
    hooks.setInput(Direction.DOWN, 4);
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  @Test
  void equalityReturnsTrueOnMatch()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d==5"));
    hooks.setInput(Direction.DOWN, 5);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  @Test
  void equalityReturnsFalseOnMismatch()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d==5"));
    hooks.setInput(Direction.DOWN, 4);
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  @Test
  void inequalityReturnsTrueWhenValuesDiffer()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d!=5"));
    hooks.setInput(Direction.DOWN, 6);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  // --- Logical operators ---

  @Test
  void logicalAndReturnsTrueWhenBothInputsNonzero()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d&&u"));
    hooks.setInput(Direction.DOWN, 3);
    hooks.setInput(Direction.UP, 5);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  @Test
  void logicalAndReturnsFalseWhenOneInputIsZero()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d&&u"));
    hooks.setInput(Direction.DOWN, 0);
    hooks.setInput(Direction.UP, 5);
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  @Test
  void logicalOrReturnsTrueWhenOneInputIsNonzero()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d||u"));
    hooks.setInput(Direction.DOWN, 0);
    hooks.setInput(Direction.UP, 5);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  @Test
  void logicalOrReturnsFalseWhenBothInputsAreZero()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d||u"));
    hooks.setInput(Direction.DOWN, 0);
    hooks.setInput(Direction.UP, 0);
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  @Test
  void xorReturnsTrueWhenInputsDiffer()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d^u"));
    hooks.setInput(Direction.DOWN, 1);
    hooks.setInput(Direction.UP, 0);
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  @Test
  void xorReturnsFalseWhenInputsMatch()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d^u"));
    hooks.setInput(Direction.DOWN, 1);
    hooks.setInput(Direction.UP, 1);
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  // --- Built-in functions ---

  @Test
  void invFunctionComplementsInputRelativeToFifteen()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=inv(d)"));
    hooks.setInput(Direction.DOWN, 3);
    hooks.tick();
    assertEquals(12, hooks.output(Direction.EAST)); // 15 - 3
  }

  @Test
  void maxFunctionReturnsLargestOfArguments()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=max(d,u,r)"));
    hooks.setInput(Direction.DOWN, 3);
    hooks.setInput(Direction.UP, 8);
    hooks.setInput(Direction.NORTH, 5);
    hooks.tick();
    assertEquals(8, hooks.output(Direction.EAST));
  }

  @Test
  void minFunctionReturnsSmallestOfArguments()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=min(d,u)"));
    hooks.setInput(Direction.DOWN, 3);
    hooks.setInput(Direction.UP, 8);
    hooks.tick();
    assertEquals(3, hooks.output(Direction.EAST));
  }

  @Test
  void limFunctionClampsValueBelowMinimum()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=lim(d,3,10)"));
    hooks.setInput(Direction.DOWN, 1);
    hooks.tick();
    assertEquals(3, hooks.output(Direction.EAST));
  }

  @Test
  void limFunctionPassesThroughValueWithinRange()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=lim(d,3,10)"));
    hooks.setInput(Direction.DOWN, 5);
    hooks.tick();
    assertEquals(5, hooks.output(Direction.EAST));
  }

  @Test
  void limFunctionClampsValueAboveMaximum()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=lim(d,3,10)"));
    hooks.setInput(Direction.DOWN, 12);
    hooks.tick();
    assertEquals(10, hooks.output(Direction.EAST));
  }

  @Test
  void ifFunctionReturnsThenBranchWhenConditionNonzero()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=if(d,5,2)"));
    hooks.setInput(Direction.DOWN, 3);
    hooks.tick();
    assertEquals(5, hooks.output(Direction.EAST));
  }

  @Test
  void ifFunctionReturnsElseBranchWhenConditionZero()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=if(d,5,2)"));
    hooks.setInput(Direction.DOWN, 0);
    hooks.tick();
    assertEquals(2, hooks.output(Direction.EAST));
  }

  @Test
  void meanFunctionReturnsTruncatedAverage()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=mean(d,u)"));
    hooks.setInput(Direction.DOWN, 3);
    hooks.setInput(Direction.UP, 7);
    hooks.tick();
    assertEquals(5, hooks.output(Direction.EAST)); // (3+7)/2 = 5
  }

  // --- Output clamping ---

  @Test
  void portAssignmentClampsOverflowToFifteen()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=20"));
    hooks.tick();
    assertEquals(15, hooks.output(Direction.EAST));
  }

  @Test
  void portAssignmentClampsUnderflowToZero()
  {
    final TestHooks hooks = new TestHooks();
    // Unary negation: b = -(1)
    assertTrue(hooks.setCode("b=0-1"));
    hooks.tick();
    assertEquals(0, hooks.output(Direction.EAST));
  }

  // --- Multi-line programs ---

  @Test
  void multilineAssignmentsAreEvaluatedForAllPorts()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=5\nu=3"));
    hooks.tick();
    assertEquals(5, hooks.output(Direction.EAST));
    assertEquals(3, hooks.output(Direction.UP));
  }

  @Test
  void whitespaceOnlyLinesAreIgnored()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=5\n   \nu=3"));
    hooks.tick();
    assertEquals(5, hooks.output(Direction.EAST));
    assertEquals(3, hooks.output(Direction.UP));
  }

  // --- I/O mask isolation ---

  @Test
  void singleAssignmentSetsOnlyExpectedInputAndOutputMasks()
  {
    final TestHooks hooks = new TestHooks();
    assertTrue(hooks.setCode("b=d"));

    // d (DOWN, ordinal 0) should be in input mask
    assertTrue((hooks.inputMask() & (0xf << (4 * Direction.DOWN.ordinal()))) != 0, "expected input mask for d");
    // b (EAST, ordinal 5) should be in output mask
    assertTrue((hooks.outputMask() & (0xf << (4 * Direction.EAST.ordinal()))) != 0, "expected output mask for b");

    // All other directions must have zero mask
    assertEquals(0, hooks.inputMask()  & (0xf << (4 * Direction.UP.ordinal())),    "unexpected input mask for u");
    assertEquals(0, hooks.inputMask()  & (0xf << (4 * Direction.NORTH.ordinal())), "unexpected input mask for r");
    assertEquals(0, hooks.outputMask() & (0xf << (4 * Direction.DOWN.ordinal())),  "unexpected output mask for d");
    assertEquals(0, hooks.outputMask() & (0xf << (4 * Direction.UP.ordinal())),    "unexpected output mask for u");
  }
}
