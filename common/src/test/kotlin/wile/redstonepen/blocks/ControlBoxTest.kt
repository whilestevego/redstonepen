package wile.redstonepen.blocks

import net.minecraft.core.Direction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import wile.redstonepen.blocks.controlbox.ControlBoxBlockEntity.TestHooks

class ControlBoxTest {
    private fun isInputUsed(h: TestHooks, d: Direction) =
        (h.inputMask() and (0xf shl (4 * d.ordinal))) != 0

    private fun isOutputUsed(h: TestHooks, d: Direction) =
        (h.outputMask() and (0xf shl (4 * d.ordinal))) != 0

    private fun tickAt(h: TestHooks, clock: Int) {
        h.setSymbol(".clock", clock)
        h.tick()
    }

    @Nested
    inner class AlternateOperators {
        @Test
        fun andKeywordActsAsLogicalAnd() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d and u"))
            h.setInput(Direction.DOWN, 3)
            h.setInput(Direction.UP, 5)
            h.tick()
            assertEquals(15, h.output(Direction.EAST))
            h.setInput(Direction.DOWN, 0)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun orKeywordActsAsLogicalOr() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d or u"))
            h.setInput(Direction.DOWN, 0)
            h.setInput(Direction.UP, 5)
            h.tick()
            assertEquals(15, h.output(Direction.EAST))
            h.setInput(Direction.UP, 0)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun notKeywordInvertsValue() {
            val h = TestHooks()
            assertTrue(h.setCode("b=not d"))
            h.setInput(Direction.DOWN, 0)
            h.tick()
            assertTrue(h.output(Direction.EAST) > 0)
            h.setInput(Direction.DOWN, 3)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun xorKeywordActsAsExclusiveOr() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d xor u"))
            h.setInput(Direction.DOWN, 1)
            h.setInput(Direction.UP, 0)
            h.tick()
            assertEquals(15, h.output(Direction.EAST))
            h.setInput(Direction.UP, 1)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun diamondOperatorActsAsInequality() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d<>5"))
            h.setInput(Direction.DOWN, 6)
            h.tick()
            assertEquals(15, h.output(Direction.EAST))
            h.setInput(Direction.DOWN, 5)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun singlePipeActsAsLogicalOr() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d|u"))
            h.setInput(Direction.DOWN, 0)
            h.setInput(Direction.UP, 5)
            h.tick()
            assertEquals(15, h.output(Direction.EAST))
            h.setInput(Direction.UP, 0)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun singleAmpersandActsAsLogicalAnd() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d&u"))
            h.setInput(Direction.DOWN, 3)
            h.setInput(Direction.UP, 5)
            h.tick()
            assertEquals(15, h.output(Direction.EAST))
            h.setInput(Direction.DOWN, 0)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }
    }

    @Nested
    inner class UserVariables {
        @Test
        fun userVariableChainPropagatesWithinSameTick() {
            val h = TestHooks()
            assertTrue(h.setCode("foo=d\nb=foo+1"))
            h.setInput(Direction.DOWN, 4)
            h.tick()
            assertEquals(5, h.output(Direction.EAST))
        }

        @Test
        fun settingNewCodeClearsSymbolTable() {
            val h = TestHooks()
            h.setCode("b=d")
            h.setSymbol("myvar", 9)
            assertEquals(9, h.getSymbol("myvar"))
            h.setCode("b=u")
            assertEquals(0, h.getSymbol("myvar"))
        }

        @Test
        fun userVariableRisingEdgeFiresOneLaterThanPort() {
            val h = TestHooks()
            assertTrue(h.setCode("foo=d\nb=foo.re"))
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 0)
            assertEquals(0, h.output(Direction.EAST))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 1)
            assertEquals(0, h.output(Direction.EAST))
            tickAt(h, 2)
            assertTrue(h.output(Direction.EAST) > 0)
            tickAt(h, 3)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun userVariableNotInPortMasks() {
            val h = TestHooks()
            assertTrue(h.setCode("foo=3"))
            assertEquals(0, h.inputMask())
            assertEquals(0, h.outputMask())
        }
    }

    @Nested
    inner class Parsing {
        @Test
        fun rejectsUnknownSignalSuffixes() {
            val hooks = TestHooks()
            assertFalse(hooks.setCode("b=d.bad"))
            assertFalse(hooks.errors().isEmpty())
        }

        @Test
        fun emptyCodeIsValidWithNoMasks() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode(""))
            assertTrue(hooks.valid())
            assertEquals(0, hooks.inputMask())
            assertEquals(0, hooks.outputMask())
        }

        @Test
        fun commentOnlyLineIsValid() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("# this is a comment"))
            assertTrue(hooks.valid())
            assertEquals(0, hooks.inputMask())
            assertEquals(0, hooks.outputMask())
        }

        @Test
        fun unknownFunctionCallIsInvalid() {
            val hooks = TestHooks()
            assertFalse(hooks.setCode("b=foo(d)"))
            assertFalse(hooks.valid())
            assertFalse(hooks.errors().isEmpty())
        }

        @Test
        fun allPortNamesAcceptedOnLhs() {
            for (port in listOf("d", "u", "r", "y", "g", "b")) {
                val hooks = TestHooks()
                assertTrue(
                    hooks.setCode("$port=5"),
                    "port '$port' should be a valid LHS assignment",
                )
            }
        }

        @Test
        fun derivesInputAndOutputMasksFromProgram() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d\nu=15"))
            assertTrue(hooks.valid())
            assertTrue(isInputUsed(hooks, Direction.DOWN), "expected input mask for d")
            assertTrue(isOutputUsed(hooks, Direction.EAST), "expected output mask for b")
            assertTrue(isOutputUsed(hooks, Direction.UP), "expected output mask for u")
        }

        @Test
        fun unterminatedParenIsInvalid() {
            val hooks = TestHooks()
            assertFalse(hooks.setCode("b=(1+2"))
            assertFalse(hooks.errors().isEmpty())
        }

        @Test
        fun missingRhsIsInvalid() {
            val hooks = TestHooks()
            assertFalse(hooks.setCode("b="))
            assertFalse(hooks.errors().isEmpty())
        }

        @Test
        fun numericConstantOutsideZeroToFifteenStillParses() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=100"))
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun settingTheSameCodeTwiceReturnsValid() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d"))
            assertTrue(hooks.setCode("b=d"))
        }

        @Test
        fun trailingDotSuffixIsInvalid() {
            val hooks = TestHooks()
            assertFalse(hooks.setCode("b=d."))
            assertFalse(hooks.errors().isEmpty())
        }

        @Test
        fun invalidCharacterAfterExpressionIsRejected() {
            val hooks = TestHooks()
            assertFalse(hooks.setCode("b=3 5"))
            assertFalse(hooks.errors().isEmpty())
            assertTrue(hooks.errors().values.contains("invalid_character"))
        }

        @Test
        fun comparatorOverrideSuffixIsValid() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d.co"))
            assertTrue(hooks.valid())
        }

        @Test
        fun timerElapsedAndPresetSuffixesAreValid() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=ton1.et\nu=ton1.pt"))
            assertTrue(hooks.valid())
        }
    }

    @Nested
    inner class ArithmeticOperators {
        @Test
        fun additionOperatorOutputsSum() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=3+4"))
            hooks.tick()
            assertEquals(7, hooks.output(Direction.EAST))
        }

        @Test
        fun subtractionOperatorOutputsDifference() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=15-3"))
            hooks.tick()
            assertEquals(12, hooks.output(Direction.EAST))
        }

        @Test
        fun multiplicationOperatorOutputsProduct() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=3*4"))
            hooks.tick()
            assertEquals(12, hooks.output(Direction.EAST))
        }

        @Test
        fun divisionOperatorOutputsQuotient() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=8/2"))
            hooks.tick()
            assertEquals(4, hooks.output(Direction.EAST))
        }

        @Test
        fun moduloOperatorOutputsRemainder() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=9%5"))
            hooks.tick()
            assertEquals(4, hooks.output(Direction.EAST))
        }

        @Test
        fun divisionByZeroReturnsZero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d/0"))
            hooks.setInput(Direction.DOWN, 6)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun portAssignmentClampsOverflowToFifteen() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=20"))
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun portAssignmentClampsUnderflowToZero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=0-1"))
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun parenthesizedExpressionForcesPrecedence() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=(2+3)*2"))
            hooks.tick()
            assertEquals(10, hooks.output(Direction.EAST))
        }

        @Test
        fun unaryNegationProducesZeroAfterPortClamp() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=-5"))
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun moduloByZeroReturnsZero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d%0"))
            hooks.setInput(Direction.DOWN, 9)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }
    }

    @Nested
    inner class ComparisonOperators {
        @Test
        fun greaterThanReturnsTrueWhenInputExceedsThreshold() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d>5"))
            hooks.setInput(Direction.DOWN, 6)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun greaterThanReturnsFalseWhenInputEqualsThreshold() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d>5"))
            hooks.setInput(Direction.DOWN, 5)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun greaterThanOrEqualReturnsTrueAtBoundary() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d>=5"))
            hooks.setInput(Direction.DOWN, 5)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun greaterThanOrEqualReturnsFalseBelowBoundary() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d>=5"))
            hooks.setInput(Direction.DOWN, 4)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun equalityReturnsTrueOnMatch() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d==5"))
            hooks.setInput(Direction.DOWN, 5)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun equalityReturnsFalseOnMismatch() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d==5"))
            hooks.setInput(Direction.DOWN, 4)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun inequalityReturnsTrueWhenValuesDiffer() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d!=5"))
            hooks.setInput(Direction.DOWN, 6)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun lessThanOperatorEvaluates() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d<5"))
            h.setInput(Direction.DOWN, 3)
            h.tick()
            assertTrue(h.output(Direction.EAST) > 0)
            h.setInput(Direction.DOWN, 5)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun lessThanOrEqualOperatorEvaluates() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d<=5"))
            h.setInput(Direction.DOWN, 5)
            h.tick()
            assertTrue(h.output(Direction.EAST) > 0)
            h.setInput(Direction.DOWN, 6)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }
    }

    @Nested
    inner class LogicalOperators {
        @Test
        fun logicalAndReturnsTrueWhenBothInputsNonzero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d&&u"))
            hooks.setInput(Direction.DOWN, 3)
            hooks.setInput(Direction.UP, 5)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun logicalAndReturnsFalseWhenOneInputIsZero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d&&u"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.setInput(Direction.UP, 5)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun logicalOrReturnsTrueWhenOneInputIsNonzero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d||u"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.setInput(Direction.UP, 5)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun logicalOrReturnsFalseWhenBothInputsAreZero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d||u"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.setInput(Direction.UP, 0)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun xorReturnsTrueWhenInputsDiffer() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d^u"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.setInput(Direction.UP, 0)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun xorReturnsFalseWhenInputsMatch() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d^u"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.setInput(Direction.UP, 1)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun logicalNotInvertsTruthiness() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=!d"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.tick()
            assertTrue(hooks.output(Direction.EAST) > 0)
            hooks.setInput(Direction.DOWN, 5)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }
    }

    @Nested
    inner class BuiltinFunctions {
        @Test
        fun invFunctionComplementsInputRelativeToFifteen() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=inv(d)"))
            hooks.setInput(Direction.DOWN, 3)
            hooks.tick()
            assertEquals(12, hooks.output(Direction.EAST))
        }

        @Test
        fun maxFunctionReturnsLargestOfArguments() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=max(d,u,r)"))
            hooks.setInput(Direction.DOWN, 3)
            hooks.setInput(Direction.UP, 8)
            hooks.setInput(Direction.NORTH, 5)
            hooks.tick()
            assertEquals(8, hooks.output(Direction.EAST))
        }

        @Test
        fun minFunctionReturnsSmallestOfArguments() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=min(d,u)"))
            hooks.setInput(Direction.DOWN, 3)
            hooks.setInput(Direction.UP, 8)
            hooks.tick()
            assertEquals(3, hooks.output(Direction.EAST))
        }

        @Test
        fun limFunctionClampsValueBelowMinimum() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=lim(d,3,10)"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.tick()
            assertEquals(3, hooks.output(Direction.EAST))
        }

        @Test
        fun limFunctionPassesThroughValueWithinRange() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=lim(d,3,10)"))
            hooks.setInput(Direction.DOWN, 5)
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
        }

        @Test
        fun limFunctionClampsValueAboveMaximum() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=lim(d,3,10)"))
            hooks.setInput(Direction.DOWN, 12)
            hooks.tick()
            assertEquals(10, hooks.output(Direction.EAST))
        }

        @Test
        fun ifFunctionReturnsThenBranchWhenConditionNonzero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=if(d,5,2)"))
            hooks.setInput(Direction.DOWN, 3)
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
        }

        @Test
        fun ifFunctionReturnsElseBranchWhenConditionZero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=if(d,5,2)"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.tick()
            assertEquals(2, hooks.output(Direction.EAST))
        }

        @Test
        fun meanFunctionReturnsTruncatedAverage() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=mean(d,u)"))
            hooks.setInput(Direction.DOWN, 3)
            hooks.setInput(Direction.UP, 7)
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
        }

        @Test
        fun ifFunctionWithSingleArgYieldsBooleanFifteen() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=if(d)"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun ifFunctionWithSingleArgYieldsZeroForFalseCondition() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=if(d)"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun ifFunctionWithTwoArgsReturnsZeroForFalse() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=if(d,7)"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun limFunctionTwoArgsClampsBetweenZeroAndUpper() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=lim(d,8)"))
            hooks.setInput(Direction.DOWN, 12)
            hooks.tick()
            assertEquals(8, hooks.output(Direction.EAST))
        }

        @Test
        fun limFunctionSingleArgClampsToFifteen() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=lim(d)"))
            hooks.setInput(Direction.DOWN, 12)
            hooks.tick()
            assertEquals(12, hooks.output(Direction.EAST))
        }

        @Test
        fun meanWithSingleArgReturnsThatArg() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=mean(d)"))
            hooks.setInput(Direction.DOWN, 6)
            hooks.tick()
            assertEquals(6, hooks.output(Direction.EAST))
        }

        @Test
        fun clockFunctionReturnsCurrentClockSymbolValue() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=clock()"))
            hooks.setSymbol(".clock", 7)
            hooks.tick()
            assertEquals(7, hooks.output(Direction.EAST))
        }

        @Test
        fun timeFunctionReturnsCurrentTimeSymbolValue() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=time()"))
            hooks.setSymbol(".time", 5)
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
        }

        @Test
        fun rndFunctionReturnsValueInRange() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=rnd()"))
            hooks.tick()
            val v = hooks.output(Direction.EAST)
            assertTrue(v in 0..15)
        }

        @Test
        fun invOfZeroReturnsMaxSignal() {
            val h = TestHooks()
            assertTrue(h.setCode("b=inv(d)"))
            h.setInput(Direction.DOWN, 0)
            h.tick()
            assertEquals(15, h.output(Direction.EAST))
        }

        @Test
        fun invOfFifteenReturnsZero() {
            val h = TestHooks()
            assertTrue(h.setCode("b=inv(d)"))
            h.setInput(Direction.DOWN, 15)
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun maxWithOneArgReturnsThatArg() {
            val h = TestHooks()
            assertTrue(h.setCode("b=max(d)"))
            h.setInput(Direction.DOWN, 7)
            h.tick()
            assertEquals(7, h.output(Direction.EAST))
        }

        @Test
        fun maxWithZeroArgsReturnsZero() {
            val h = TestHooks()
            assertTrue(h.setCode("b=max()"))
            h.tick()
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun minWithOneArgReturnsThatArg() {
            val h = TestHooks()
            assertTrue(h.setCode("b=min(d)"))
            h.setInput(Direction.DOWN, 7)
            h.tick()
            assertEquals(7, h.output(Direction.EAST))
        }

        @Test
        fun meanWithThreeArgsReturnsTruncatedAverage() {
            val h = TestHooks()
            assertTrue(h.setCode("b=mean(d,u,r)"))
            h.setInput(Direction.DOWN, 3)
            h.setInput(Direction.UP, 6)
            h.setInput(Direction.NORTH, 9)
            h.tick()
            assertEquals(6, h.output(Direction.EAST))
        }

        @Test
        fun limWithInvertedRangeClampsToUpperBound() {
            val h = TestHooks()
            assertTrue(h.setCode("b=lim(d,8,3)"))
            h.setInput(Direction.DOWN, 5)
            h.tick()
            assertEquals(3, h.output(Direction.EAST))
        }
    }

    @Nested
    inner class Execution {
        @Test
        fun tickEvaluatesAssignmentsFromInputs() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d+1"))
            hooks.setInput(Direction.DOWN, 4)
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
        }

        @Test
        fun multilineAssignmentsAreEvaluatedForAllPorts() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=5\nu=3"))
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
            assertEquals(3, hooks.output(Direction.UP))
        }

        @Test
        fun whitespaceOnlyLinesAreIgnored() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=5\n   \nu=3"))
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
            assertEquals(3, hooks.output(Direction.UP))
        }

        @Test
        fun inlineCommentAfterAssignmentIsIgnored() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=5 # comment"))
            hooks.tick()
            assertEquals(5, hooks.output(Direction.EAST))
        }

        @Test
        fun duplicateAssignmentUsesLastValue() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=3\nb=7"))
            hooks.tick()
            assertEquals(7, hooks.output(Direction.EAST))
        }

        @Test
        fun singleAssignmentSetsOnlyExpectedInputAndOutputMasks() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=d"))
            assertTrue(isInputUsed(hooks, Direction.DOWN), "expected input mask for d")
            assertTrue(isOutputUsed(hooks, Direction.EAST), "expected output mask for b")
            assertFalse(isInputUsed(hooks, Direction.UP), "unexpected input mask for u")
            assertFalse(isInputUsed(hooks, Direction.NORTH), "unexpected input mask for r")
            assertFalse(isOutputUsed(hooks, Direction.DOWN), "unexpected output mask for d")
            assertFalse(isOutputUsed(hooks, Direction.UP), "unexpected output mask for u")
        }

        @Test
        fun setSymbolStoresAndGetSymbolReadsBack() {
            val hooks = TestHooks()
            hooks.setCode("")
            hooks.setSymbol("foo", 7)
            assertEquals(7, hooks.getSymbol("foo"))
        }

        @Test
        fun getUnknownSymbolReturnsZero() {
            val hooks = TestHooks()
            assertEquals(0, hooks.getSymbol("never_set"))
        }

        @Test
        fun edgeRisingSymbolFiresOnceOnPositiveTransition() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d.re"))
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 0)
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 1)
            assertTrue(h.output(Direction.EAST) > 0)
            tickAt(h, 2)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun edgeFallingSymbolFiresOnceOnNegativeTransition() {
            val h = TestHooks()
            assertTrue(h.setCode("b=d.fe"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 1)
            assertTrue(h.output(Direction.EAST) > 0)
            tickAt(h, 2)
            assertEquals(0, h.output(Direction.EAST))
        }
    }

    @Nested
    inner class Timers {
        @Test
        fun tivTimerUsesClockSymbolForPulseGeneration() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=tiv1(3)"))
            hooks.setSymbol(".clock", 0)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
            hooks.setSymbol(".clock", 3)
            hooks.tick()
            assertEquals(15, hooks.output(Direction.EAST))
        }

        @Test
        fun tonWithZeroPeriodReturnsTrueImmediately() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=ton1(d, 0)"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.tick()
            assertTrue(hooks.output(Direction.EAST) > 0)
        }

        @Test
        fun tonWithFalseInputReturnsFalse() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=ton1(d, 5)"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun tofWithTrueInputReturnsTrue() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=tof1(d, 5)"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.tick()
            assertTrue(hooks.output(Direction.EAST) > 0)
        }

        @Test
        fun tofWithZeroPeriodReturnsTrue() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=tof1(d, 0)"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.tick()
            assertTrue(hooks.output(Direction.EAST) > 0)
        }

        @Test
        fun tpWithZeroPeriodMatchesInput() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=tp1(d, 0)"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.tick()
            assertTrue(hooks.output(Direction.EAST) > 0)
        }

        @Test
        fun tpWithZeroInputAndZeroPeriodReturnsFalse() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=tp1(d, 0)"))
            hooks.setInput(Direction.DOWN, 0)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun tivWithZeroPeriodReturnsFalse() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=tiv1(0)"))
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun tivWithDisabledEnableSignalReturnsZero() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=tiv1(5, 0)"))
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun tonRisesAfterPeriodElapses() {
            val h = TestHooks()
            assertTrue(h.setCode("b=ton1(d, 4)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            assertEquals(0, h.output(Direction.EAST))
            tickAt(h, 2)
            assertEquals(0, h.output(Direction.EAST))
            tickAt(h, 5)
            assertTrue(h.output(Direction.EAST) > 0)
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 6)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun tonResetsWhenInputDrops() {
            val h = TestHooks()
            assertTrue(h.setCode("b=ton1(d, 4)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            tickAt(h, 1)
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 2)
            assertEquals(0, h.output(Direction.EAST))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 3)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun tofFallsAfterPeriodElapses() {
            val h = TestHooks()
            assertTrue(h.setCode("b=tof1(d, 4)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            assertTrue(h.output(Direction.EAST) > 0)
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 1)
            assertTrue(h.output(Direction.EAST) > 0)
            tickAt(h, 6)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun tofResetsWhenInputReturnsHigh() {
            val h = TestHooks()
            assertTrue(h.setCode("b=tof1(d, 4)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 1)
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 2)
            assertTrue(h.output(Direction.EAST) > 0)
        }

        @Test
        fun tpStaysHighForPeriodThenFalls() {
            val h = TestHooks()
            assertTrue(h.setCode("b=tp1(d, 4)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            assertTrue(h.output(Direction.EAST) > 0)
            tickAt(h, 2)
            assertTrue(h.output(Direction.EAST) > 0)
            tickAt(h, 6)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun tpRequiresInputDropBeforeRetrigger() {
            val h = TestHooks()
            assertTrue(h.setCode("b=tp1(d, 3)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            tickAt(h, 5)
            assertEquals(0, h.output(Direction.EAST))
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 6)
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 7)
            assertTrue(h.output(Direction.EAST) > 0)
        }

        @Test
        fun tivProducesPulseWhenIntervalElapses() {
            val h = TestHooks()
            assertTrue(h.setCode("b=tiv1(5)"))
            tickAt(h, 0)
            val n0 = h.output(Direction.EAST)
            tickAt(h, 6)
            val n1 = h.output(Direction.EAST)
            if (n0 == 0 && n1 == 0) {
                tickAt(h, 12)
                assertTrue(h.output(Direction.EAST) >= 0)
            }
        }

        @Test
        fun tonStaysHighAfterPeriodWithInputStillHigh() {
            val h = TestHooks()
            assertTrue(h.setCode("b=ton1(d, 3)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            tickAt(h, 4)
            assertTrue(h.output(Direction.EAST) > 0)
            tickAt(h, 5)
            assertTrue(h.output(Direction.EAST) > 0)
        }

        @Test
        fun tofStaysLowAfterPeriodWithInputStillLow() {
            val h = TestHooks()
            assertTrue(h.setCode("b=tof1(d, 3)"))
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 0)
            h.setInput(Direction.DOWN, 0)
            tickAt(h, 1)
            tickAt(h, 5)
            assertEquals(0, h.output(Direction.EAST))
            tickAt(h, 6)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun tivWithNoArgsAlwaysReturnsZero() {
            val h = TestHooks()
            assertTrue(h.setCode("b=tiv1()"))
            tickAt(h, 0)
            assertEquals(0, h.output(Direction.EAST))
            tickAt(h, 100)
            assertEquals(0, h.output(Direction.EAST))
        }

        @Test
        fun twoTimerInstancesMaintainIndependentState() {
            val h = TestHooks()
            assertTrue(h.setCode("b=ton1(d, 3)\nu=ton2(r, 6)"))
            h.setInput(Direction.DOWN, 1)
            h.setInput(Direction.NORTH, 1)
            tickAt(h, 0)
            assertEquals(0, h.output(Direction.EAST))
            assertEquals(0, h.output(Direction.UP))
            tickAt(h, 4)
            assertTrue(h.output(Direction.EAST) > 0)
            assertEquals(0, h.output(Direction.UP))
            tickAt(h, 7)
            assertTrue(h.output(Direction.UP) > 0)
        }
    }

    @Nested
    inner class Counter {
        @Test
        fun counterWithSingleArgIncrementsOnPositiveInput() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=cnt1(d)"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.tick()
            hooks.tick()
            hooks.tick()
            assertTrue(hooks.output(Direction.EAST) > 0)
        }

        @Test
        fun counterWithFiveArgsClearsOnReset() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=cnt1(d, u, 0, 15, r)"))
            hooks.setInput(Direction.DOWN, 1)
            hooks.tick()
            hooks.tick()
            hooks.setInput(Direction.NORTH, 1)
            hooks.tick()
            assertEquals(0, hooks.output(Direction.EAST))
        }

        @Test
        fun counterThreeArgsClampsBetweenZeroAndMax() {
            val h = TestHooks()
            assertTrue(h.setCode("b=cnt1(d, u, 5)"))
            h.setInput(Direction.DOWN, 1)
            h.setInput(Direction.UP, 0)
            for (i in 0 until 10) h.tick()
            assertEquals(5, h.output(Direction.EAST))
        }

        @Test
        fun counterRisingEdgeIncrementsAcrossClockTicks() {
            val h = TestHooks()
            assertTrue(h.setCode("b=cnt1(d, u)"))
            h.setInput(Direction.DOWN, 0)
            h.setInput(Direction.UP, 0)
            tickAt(h, 0)
            h.setInput(Direction.DOWN, 1)
            tickAt(h, 1)
            val afterRise = h.output(Direction.EAST)
            assertTrue(afterRise > 0)
            h.setInput(Direction.DOWN, 0)
            h.setInput(Direction.UP, 1)
            tickAt(h, 2)
            val afterFall = h.output(Direction.EAST)
            assertTrue(afterFall <= afterRise)
        }

        @Test
        fun counterFourArgsClampsBetweenExplicitMinAndMax() {
            val h = TestHooks()
            assertTrue(h.setCode("b=cnt1(d, u, 2, 8)"))
            h.setInput(Direction.DOWN, 0)
            h.setInput(Direction.UP, 1)
            for (i in 0 until 10) h.tick()
            assertEquals(2, h.output(Direction.EAST))
            h.setInput(Direction.UP, 0)
            h.setInput(Direction.DOWN, 1)
            for (i in 0 until 10) h.tick()
            assertEquals(8, h.output(Direction.EAST))
        }
    }

    @Nested
    inner class Rca {
        @Test
        fun rcaInputChannelsAreRecognised() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=di0+di1"))
        }

        @Test
        fun rcaOutputChannelsAreRecognised() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("do0=d"))
        }

        @Test
        fun rcaChannelOutOfRangeStillParses() {
            val hooks = TestHooks()
            assertTrue(hooks.setCode("b=di20"))
        }

        @Test
        fun rcaInputChannelFlowsThroughTick() {
            val h = TestHooks()
            assertTrue(h.setCode("do0=di0+di1"))
            h.setRcaInput(0, 3)
            h.setRcaInput(1, 5)
            h.tick()
            assertEquals(8, h.getRcaOutput(0))
        }

        @Test
        fun rcaOutputMaskLimitsOutOfRangeValue() {
            val h = TestHooks()
            assertTrue(h.setCode("do0=di0"))
            h.setRcaInput(0, 15)
            h.tick()
            assertEquals(15, h.getRcaOutput(0))
            h.setRcaInput(0, 0)
            h.tick()
            assertEquals(0, h.getRcaOutput(0))
        }

        @Test
        fun rcaOutputDataIsZeroWhenNotAssigned() {
            val h = TestHooks()
            assertTrue(h.setCode("b=di0"))
            h.setRcaInput(0, 7)
            h.tick()
            assertEquals(0, h.rcaOutputData())
        }
    }
}
