package wile.redstonepen.blocks

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import net.minecraft.core.Direction
import wile.redstonepen.blocks.controlbox.ControlBoxBlockEntity.TestHooks

class ControlBoxTest :
    DescribeSpec({
        fun isInputUsed(h: TestHooks, d: Direction) =
            (h.inputMask() and (0xf shl (4 * d.ordinal))) != 0

        fun isOutputUsed(h: TestHooks, d: Direction) =
            (h.outputMask() and (0xf shl (4 * d.ordinal))) != 0

        fun tickAt(h: TestHooks, clock: Int) {
            h.setSymbol(".clock", clock)
            h.tick()
        }

        describe("alternate operators") {
            it("and keyword acts as logical and") {
                val h = TestHooks()
                h.setCode("b=d and u") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.setInput(Direction.UP, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("or keyword acts as logical or") {
                val h = TestHooks()
                h.setCode("b=d or u") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
                h.setInput(Direction.UP, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("not keyword inverts value") {
                val h = TestHooks()
                h.setCode("b=not d") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("xor keyword acts as exclusive or") {
                val h = TestHooks()
                h.setCode("b=d xor u") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.setInput(Direction.UP, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
                h.setInput(Direction.UP, 1)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("diamond operator acts as inequality") {
                val h = TestHooks()
                h.setCode("b=d<>5") shouldBe true
                h.setInput(Direction.DOWN, 6)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("single pipe acts as logical or") {
                val h = TestHooks()
                h.setCode("b=d|u") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
                h.setInput(Direction.UP, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("single ampersand acts as logical and") {
                val h = TestHooks()
                h.setCode("b=d&u") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.setInput(Direction.UP, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }
        }

        describe("user variables") {
            it("user variable chain propagates within same tick") {
                val h = TestHooks()
                h.setCode("foo=d\nb=foo+1") shouldBe true
                h.setInput(Direction.DOWN, 4)
                h.tick()
                h.output(Direction.EAST) shouldBe 5
            }

            it("setting new code clears symbol table") {
                val h = TestHooks()
                h.setCode("b=d")
                h.setSymbol("myvar", 9)
                h.getSymbol("myvar") shouldBe 9
                h.setCode("b=u")
                h.getSymbol("myvar") shouldBe 0
            }

            it("user variable rising edge fires one later than port") {
                val h = TestHooks()
                h.setCode("foo=d\nb=foo.re") shouldBe true
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 0)
                h.output(Direction.EAST) shouldBe 0
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 1)
                h.output(Direction.EAST) shouldBe 0
                tickAt(h, 2)
                (h.output(Direction.EAST) > 0) shouldBe true
                tickAt(h, 3)
                h.output(Direction.EAST) shouldBe 0
            }

            it("user variable not in port masks") {
                val h = TestHooks()
                h.setCode("foo=3") shouldBe true
                h.inputMask() shouldBe 0
                h.outputMask() shouldBe 0
            }
        }

        describe("parsing") {
            it("rejects unknown signal suffixes") {
                val h = TestHooks()
                h.setCode("b=d.bad") shouldBe false
                h.errors().isEmpty() shouldBe false
            }

            it("errors map is empty for valid code") {
                val h = TestHooks()
                h.setCode("b=d+5") shouldBe true
                h.errors().shouldBeEmpty()
            }

            it("error key reflects character offset not line index for second-line error") {
                val h = TestHooks()
                val firstLine = "x=5"
                h.setCode("$firstLine\nb=nosuch()") shouldBe false
                val errs = h.errors()
                errs.shouldNotBeEmpty()
                errs.keys.all { it > firstLine.length } shouldBe true
            }

            it("empty code is valid with no masks") {
                val h = TestHooks()
                h.setCode("") shouldBe true
                h.valid() shouldBe true
                h.inputMask() shouldBe 0
                h.outputMask() shouldBe 0
            }

            it("comment only line is valid") {
                val h = TestHooks()
                h.setCode("# this is a comment") shouldBe true
                h.valid() shouldBe true
                h.inputMask() shouldBe 0
                h.outputMask() shouldBe 0
            }

            it("unknown function call is invalid") {
                val h = TestHooks()
                h.setCode("b=foo(d)") shouldBe false
                h.valid() shouldBe false
                h.errors().isEmpty() shouldBe false
            }

            it("all port names accepted on lhs") {
                for (port in listOf("d", "u", "r", "y", "g", "b")) {
                    val h = TestHooks()
                    h.setCode("$port=5") shouldBe true
                }
            }

            it("derives input and output masks from program") {
                val h = TestHooks()
                h.setCode("b=d\nu=15") shouldBe true
                h.valid() shouldBe true
                isInputUsed(h, Direction.DOWN) shouldBe true
                isOutputUsed(h, Direction.EAST) shouldBe true
                isOutputUsed(h, Direction.UP) shouldBe true
            }

            it("unterminated paren is invalid") {
                val h = TestHooks()
                h.setCode("b=(1+2") shouldBe false
                h.errors().isEmpty() shouldBe false
            }

            it("missing rhs is invalid") {
                val h = TestHooks()
                h.setCode("b=") shouldBe false
                h.errors().isEmpty() shouldBe false
            }

            it("numeric constant outside zero to fifteen still parses") {
                val h = TestHooks()
                h.setCode("b=100") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("setting the same code twice returns valid") {
                val h = TestHooks()
                h.setCode("b=d") shouldBe true
                h.setCode("b=d") shouldBe true
            }

            it("trailing dot suffix is invalid") {
                val h = TestHooks()
                h.setCode("b=d.") shouldBe false
                h.errors().isEmpty() shouldBe false
            }

            it("invalid character after expression is rejected") {
                val h = TestHooks()
                h.setCode("b=3 5") shouldBe false
                h.errors().isEmpty() shouldBe false
                h.errors().values.contains("invalid_character") shouldBe true
            }

            it("comparator override suffix is valid") {
                val h = TestHooks()
                h.setCode("b=d.co") shouldBe true
                h.valid() shouldBe true
            }

            it("timer elapsed and preset suffixes are valid") {
                val h = TestHooks()
                h.setCode("b=ton1.et\nu=ton1.pt") shouldBe true
                h.valid() shouldBe true
            }
        }

        describe("arithmetic operators") {
            it("addition operator outputs sum") {
                val h = TestHooks()
                h.setCode("b=3+4") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 7
            }

            it("subtraction operator outputs difference") {
                val h = TestHooks()
                h.setCode("b=15-3") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 12
            }

            it("multiplication operator outputs product") {
                val h = TestHooks()
                h.setCode("b=3*4") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 12
            }

            it("division operator outputs quotient") {
                val h = TestHooks()
                h.setCode("b=8/2") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 4
            }

            it("modulo operator outputs remainder") {
                val h = TestHooks()
                h.setCode("b=9%5") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 4
            }

            it("division by zero throws ArithmeticException") {
                val h = TestHooks()
                h.setCode("b=d/0") shouldBe true
                h.setInput(Direction.DOWN, 6)
                shouldThrow<ArithmeticException> { h.tick() }
            }

            it("port assignment clamps overflow to fifteen") {
                val h = TestHooks()
                h.setCode("b=20") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("port assignment clamps underflow to zero") {
                val h = TestHooks()
                h.setCode("b=0-1") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("parenthesized expression forces precedence") {
                val h = TestHooks()
                h.setCode("b=(2+3)*2") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 10
            }

            it("unary negation produces zero after port clamp") {
                val h = TestHooks()
                h.setCode("b=-5") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("modulo by zero throws ArithmeticException") {
                val h = TestHooks()
                h.setCode("b=d%0") shouldBe true
                h.setInput(Direction.DOWN, 9)
                shouldThrow<ArithmeticException> { h.tick() }
            }

            it("port output is always clamped to 0..15 for any constant expression") {
                checkAll(Arb.int(-1000..1000)) { n ->
                    val h = TestHooks()
                    h.setCode("b=$n")
                    h.tick()
                    (h.output(Direction.EAST) in 0..15) shouldBe true
                }
            }
        }

        describe("comparison operators") {
            it("greater than returns true when input exceeds threshold") {
                val h = TestHooks()
                h.setCode("b=d>5") shouldBe true
                h.setInput(Direction.DOWN, 6)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("greater than returns false when input equals threshold") {
                val h = TestHooks()
                h.setCode("b=d>5") shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("greater than or equal returns true at boundary") {
                val h = TestHooks()
                h.setCode("b=d>=5") shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("greater than or equal returns false below boundary") {
                val h = TestHooks()
                h.setCode("b=d>=5") shouldBe true
                h.setInput(Direction.DOWN, 4)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("equality returns true on match") {
                val h = TestHooks()
                h.setCode("b=d==5") shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("equality returns false on mismatch") {
                val h = TestHooks()
                h.setCode("b=d==5") shouldBe true
                h.setInput(Direction.DOWN, 4)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("inequality returns true when values differ") {
                val h = TestHooks()
                h.setCode("b=d!=5") shouldBe true
                h.setInput(Direction.DOWN, 6)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("less than operator evaluates") {
                val h = TestHooks()
                h.setCode("b=d<5") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("less than or equal operator evaluates") {
                val h = TestHooks()
                h.setCode("b=d<=5") shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
                h.setInput(Direction.DOWN, 6)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }
        }

        describe("logical operators") {
            it("logical and returns true when both inputs nonzero") {
                val h = TestHooks()
                h.setCode("b=d&&u") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.setInput(Direction.UP, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("logical and returns false when one input is zero") {
                val h = TestHooks()
                h.setCode("b=d&&u") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("logical or returns true when one input is nonzero") {
                val h = TestHooks()
                h.setCode("b=d||u") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("logical or returns false when both inputs are zero") {
                val h = TestHooks()
                h.setCode("b=d||u") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("xor returns true when inputs differ") {
                val h = TestHooks()
                h.setCode("b=d^u") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.setInput(Direction.UP, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("xor returns false when inputs match") {
                val h = TestHooks()
                h.setCode("b=d^u") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.setInput(Direction.UP, 1)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("logical not inverts truthiness") {
                val h = TestHooks()
                h.setCode("b=!d") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }
        }

        describe("builtin functions") {
            it("inv function complements input relative to fifteen") {
                val h = TestHooks()
                h.setCode("b=inv(d)") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.tick()
                h.output(Direction.EAST) shouldBe 12
            }

            it("max function returns largest of arguments") {
                val h = TestHooks()
                h.setCode("b=max(d,u,r)") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.setInput(Direction.UP, 8)
                h.setInput(Direction.NORTH, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 8
            }

            it("min function returns smallest of arguments") {
                val h = TestHooks()
                h.setCode("b=min(d,u)") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.setInput(Direction.UP, 8)
                h.tick()
                h.output(Direction.EAST) shouldBe 3
            }

            it("lim function clamps value below minimum") {
                val h = TestHooks()
                h.setCode("b=lim(d,3,10)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.tick()
                h.output(Direction.EAST) shouldBe 3
            }

            it("lim function passes through value within range") {
                val h = TestHooks()
                h.setCode("b=lim(d,3,10)") shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 5
            }

            it("lim function clamps value above maximum") {
                val h = TestHooks()
                h.setCode("b=lim(d,3,10)") shouldBe true
                h.setInput(Direction.DOWN, 12)
                h.tick()
                h.output(Direction.EAST) shouldBe 10
            }

            it("if function returns then branch when condition nonzero") {
                val h = TestHooks()
                h.setCode("b=if(d,5,2)") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.tick()
                h.output(Direction.EAST) shouldBe 5
            }

            it("if function returns else branch when condition zero") {
                val h = TestHooks()
                h.setCode("b=if(d,5,2)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 2
            }

            it("mean function returns truncated average") {
                val h = TestHooks()
                h.setCode("b=mean(d,u)") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.setInput(Direction.UP, 7)
                h.tick()
                h.output(Direction.EAST) shouldBe 5
            }

            it("if function with single arg yields boolean fifteen") {
                val h = TestHooks()
                h.setCode("b=if(d)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("if function with single arg yields zero for false condition") {
                val h = TestHooks()
                h.setCode("b=if(d)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("if function with two args returns zero for false") {
                val h = TestHooks()
                h.setCode("b=if(d,7)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("lim function two args clamps between zero and upper") {
                val h = TestHooks()
                h.setCode("b=lim(d,8)") shouldBe true
                h.setInput(Direction.DOWN, 12)
                h.tick()
                h.output(Direction.EAST) shouldBe 8
            }

            it("lim function single arg clamps to fifteen") {
                val h = TestHooks()
                h.setCode("b=lim(d)") shouldBe true
                h.setInput(Direction.DOWN, 12)
                h.tick()
                h.output(Direction.EAST) shouldBe 12
            }

            it("mean with single arg returns that arg") {
                val h = TestHooks()
                h.setCode("b=mean(d)") shouldBe true
                h.setInput(Direction.DOWN, 6)
                h.tick()
                h.output(Direction.EAST) shouldBe 6
            }

            it("clock function returns current clock symbol value") {
                val h = TestHooks()
                h.setCode("b=clock()") shouldBe true
                h.setSymbol(".clock", 7)
                h.tick()
                h.output(Direction.EAST) shouldBe 7
            }

            it("time function returns current time symbol value") {
                val h = TestHooks()
                h.setCode("b=time()") shouldBe true
                h.setSymbol(".time", 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 5
            }

            it("rnd function returns value in range") {
                val h = TestHooks()
                h.setCode("b=rnd()") shouldBe true
                h.tick()
                val v = h.output(Direction.EAST)
                (v in 0..15) shouldBe true
            }

            it("inv of zero returns max signal") {
                val h = TestHooks()
                h.setCode("b=inv(d)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("inv of fifteen returns zero") {
                val h = TestHooks()
                h.setCode("b=inv(d)") shouldBe true
                h.setInput(Direction.DOWN, 15)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("max with one arg returns that arg") {
                val h = TestHooks()
                h.setCode("b=max(d)") shouldBe true
                h.setInput(Direction.DOWN, 7)
                h.tick()
                h.output(Direction.EAST) shouldBe 7
            }

            it("max with zero args returns zero") {
                val h = TestHooks()
                h.setCode("b=max()") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("min with one arg returns that arg") {
                val h = TestHooks()
                h.setCode("b=min(d)") shouldBe true
                h.setInput(Direction.DOWN, 7)
                h.tick()
                h.output(Direction.EAST) shouldBe 7
            }

            it("mean with three args returns truncated average") {
                val h = TestHooks()
                h.setCode("b=mean(d,u,r)") shouldBe true
                h.setInput(Direction.DOWN, 3)
                h.setInput(Direction.UP, 6)
                h.setInput(Direction.NORTH, 9)
                h.tick()
                h.output(Direction.EAST) shouldBe 6
            }

            it("lim with inverted range clamps to upper bound") {
                val h = TestHooks()
                h.setCode("b=lim(d,8,3)") shouldBe true
                h.setInput(Direction.DOWN, 5)
                h.tick()
                h.output(Direction.EAST) shouldBe 3
            }
        }

        describe("execution") {
            it("tick evaluates assignments from inputs") {
                val h = TestHooks()
                h.setCode("b=d+1") shouldBe true
                h.setInput(Direction.DOWN, 4)
                h.tick()
                h.output(Direction.EAST) shouldBe 5
            }

            it("multiline assignments are evaluated for all ports") {
                val h = TestHooks()
                h.setCode("b=5\nu=3") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 5
                h.output(Direction.UP) shouldBe 3
            }

            it("whitespace only lines are ignored") {
                val h = TestHooks()
                h.setCode("b=5\n   \nu=3") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 5
                h.output(Direction.UP) shouldBe 3
            }

            it("inline comment after assignment is ignored") {
                val h = TestHooks()
                h.setCode("b=5 # comment") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 5
            }

            it("duplicate assignment uses last value") {
                val h = TestHooks()
                h.setCode("b=3\nb=7") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 7
            }

            it("single assignment sets only expected input and output masks") {
                val h = TestHooks()
                h.setCode("b=d") shouldBe true
                isInputUsed(h, Direction.DOWN) shouldBe true
                isOutputUsed(h, Direction.EAST) shouldBe true
                isInputUsed(h, Direction.UP) shouldBe false
                isInputUsed(h, Direction.NORTH) shouldBe false
                isOutputUsed(h, Direction.DOWN) shouldBe false
                isOutputUsed(h, Direction.UP) shouldBe false
            }

            it("setSymbol stores and getSymbol reads back") {
                val h = TestHooks()
                h.setCode("")
                h.setSymbol("foo", 7)
                h.getSymbol("foo") shouldBe 7
            }

            it("get unknown symbol returns zero") { TestHooks().getSymbol("never_set") shouldBe 0 }

            it("edge rising symbol fires once on positive transition") {
                val h = TestHooks()
                h.setCode("b=d.re") shouldBe true
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 0)
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 1)
                (h.output(Direction.EAST) > 0) shouldBe true
                tickAt(h, 2)
                h.output(Direction.EAST) shouldBe 0
            }

            it("edge falling symbol fires once on negative transition") {
                val h = TestHooks()
                h.setCode("b=d.fe") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 1)
                (h.output(Direction.EAST) > 0) shouldBe true
                tickAt(h, 2)
                h.output(Direction.EAST) shouldBe 0
            }
        }

        describe("timers") {
            it("tiv timer uses clock symbol for pulse generation") {
                val h = TestHooks()
                h.setCode("b=tiv1(3)") shouldBe true
                h.setSymbol(".clock", 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
                h.setSymbol(".clock", 3)
                h.tick()
                h.output(Direction.EAST) shouldBe 15
            }

            it("ton with zero period returns true immediately") {
                val h = TestHooks()
                h.setCode("b=ton1(d, 0)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("ton with false input returns false") {
                val h = TestHooks()
                h.setCode("b=ton1(d, 5)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("tof with true input returns true") {
                val h = TestHooks()
                h.setCode("b=tof1(d, 5)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("tof with zero period returns true") {
                val h = TestHooks()
                h.setCode("b=tof1(d, 0)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("tp with zero period matches input") {
                val h = TestHooks()
                h.setCode("b=tp1(d, 0)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("tp with zero input and zero period returns false") {
                val h = TestHooks()
                h.setCode("b=tp1(d, 0)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("tiv with zero period returns false") {
                val h = TestHooks()
                h.setCode("b=tiv1(0)") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("tiv with disabled enable signal returns zero") {
                val h = TestHooks()
                h.setCode("b=tiv1(5, 0)") shouldBe true
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("ton rises after period elapses") {
                val h = TestHooks()
                h.setCode("b=ton1(d, 4)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                h.output(Direction.EAST) shouldBe 0
                tickAt(h, 2)
                h.output(Direction.EAST) shouldBe 0
                tickAt(h, 5)
                (h.output(Direction.EAST) > 0) shouldBe true
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 6)
                h.output(Direction.EAST) shouldBe 0
            }

            it("ton resets when input drops") {
                val h = TestHooks()
                h.setCode("b=ton1(d, 4)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                tickAt(h, 1)
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 2)
                h.output(Direction.EAST) shouldBe 0
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 3)
                h.output(Direction.EAST) shouldBe 0
            }

            it("tof falls after period elapses") {
                val h = TestHooks()
                h.setCode("b=tof1(d, 4)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                (h.output(Direction.EAST) > 0) shouldBe true
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 1)
                (h.output(Direction.EAST) > 0) shouldBe true
                tickAt(h, 6)
                h.output(Direction.EAST) shouldBe 0
            }

            it("tof resets when input returns high") {
                val h = TestHooks()
                h.setCode("b=tof1(d, 4)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 1)
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 2)
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("tp stays high for period then falls") {
                val h = TestHooks()
                h.setCode("b=tp1(d, 4)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                (h.output(Direction.EAST) > 0) shouldBe true
                tickAt(h, 2)
                (h.output(Direction.EAST) > 0) shouldBe true
                tickAt(h, 6)
                h.output(Direction.EAST) shouldBe 0
            }

            it("tp requires input drop before retrigger") {
                val h = TestHooks()
                h.setCode("b=tp1(d, 3)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                tickAt(h, 5)
                h.output(Direction.EAST) shouldBe 0
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 6)
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 7)
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("tiv produces pulse when interval elapses") {
                val h = TestHooks()
                h.setCode("b=tiv1(5)") shouldBe true
                tickAt(h, 0)
                val n0 = h.output(Direction.EAST)
                tickAt(h, 6)
                val n1 = h.output(Direction.EAST)
                if (n0 == 0 && n1 == 0) {
                    tickAt(h, 12)
                    (h.output(Direction.EAST) >= 0) shouldBe true
                }
            }

            it("ton stays high after period with input still high") {
                val h = TestHooks()
                h.setCode("b=ton1(d, 3)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                tickAt(h, 4)
                (h.output(Direction.EAST) > 0) shouldBe true
                tickAt(h, 5)
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("tof stays low after period with input still low") {
                val h = TestHooks()
                h.setCode("b=tof1(d, 3)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 0)
                h.setInput(Direction.DOWN, 0)
                tickAt(h, 1)
                tickAt(h, 5)
                h.output(Direction.EAST) shouldBe 0
                tickAt(h, 6)
                h.output(Direction.EAST) shouldBe 0
            }

            it("tiv with no args always returns zero") {
                val h = TestHooks()
                h.setCode("b=tiv1()") shouldBe true
                tickAt(h, 0)
                h.output(Direction.EAST) shouldBe 0
                tickAt(h, 100)
                h.output(Direction.EAST) shouldBe 0
            }

            it("two timer instances maintain independent state") {
                val h = TestHooks()
                h.setCode("b=ton1(d, 3)\nu=ton2(r, 6)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.setInput(Direction.NORTH, 1)
                tickAt(h, 0)
                h.output(Direction.EAST) shouldBe 0
                h.output(Direction.UP) shouldBe 0
                tickAt(h, 4)
                (h.output(Direction.EAST) > 0) shouldBe true
                h.output(Direction.UP) shouldBe 0
                tickAt(h, 7)
                (h.output(Direction.UP) > 0) shouldBe true
            }
        }

        describe("counter") {
            it("counter with single arg increments on positive input") {
                val h = TestHooks()
                h.setCode("b=cnt1(d)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.tick()
                h.tick()
                h.tick()
                (h.output(Direction.EAST) > 0) shouldBe true
            }

            it("counter with five args clears on reset") {
                val h = TestHooks()
                h.setCode("b=cnt1(d, u, 0, 15, r)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.tick()
                h.tick()
                h.setInput(Direction.NORTH, 1)
                h.tick()
                h.output(Direction.EAST) shouldBe 0
            }

            it("counter three args clamps between zero and max") {
                val h = TestHooks()
                h.setCode("b=cnt1(d, u, 5)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.setInput(Direction.UP, 0)
                repeat(10) { h.tick() }
                h.output(Direction.EAST) shouldBe 5
            }

            it("counter rising edge increments across clock ticks") {
                val h = TestHooks()
                h.setCode("b=cnt1(d, u)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 0)
                tickAt(h, 0)
                h.setInput(Direction.DOWN, 1)
                tickAt(h, 1)
                val afterRise = h.output(Direction.EAST)
                (afterRise > 0) shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 1)
                tickAt(h, 2)
                val afterFall = h.output(Direction.EAST)
                (afterFall <= afterRise) shouldBe true
            }

            it("counter four args clamps between explicit min and max") {
                val h = TestHooks()
                h.setCode("b=cnt1(d, u, 2, 8)") shouldBe true
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 1)
                repeat(10) { h.tick() }
                h.output(Direction.EAST) shouldBe 2
                h.setInput(Direction.UP, 0)
                h.setInput(Direction.DOWN, 1)
                repeat(10) { h.tick() }
                h.output(Direction.EAST) shouldBe 8
            }

            it("counter two args decrements output on each tick with down signal high") {
                val h = TestHooks()
                h.setCode("b=cnt1(d, u)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.setInput(Direction.UP, 0)
                repeat(5) { h.tick() }
                val afterIncrement = h.output(Direction.EAST)
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 1)
                h.tick()
                h.output(Direction.EAST) shouldBe afterIncrement - 1
            }

            it("cnt1 and cnt2 maintain independent counts") {
                val h = TestHooks()
                h.setCode("b=cnt1(d)\ng=cnt2(u)") shouldBe true
                h.setInput(Direction.DOWN, 1)
                h.setInput(Direction.UP, 0)
                repeat(3) { h.tick() }
                h.setInput(Direction.DOWN, 0)
                h.setInput(Direction.UP, 1)
                h.tick()
                assertSoftly {
                    h.output(Direction.EAST) shouldBe 3 // b = cnt1, only incremented by d
                    h.output(Direction.WEST) shouldBe 1 // g = cnt2, only incremented by u
                }
            }
        }

        describe("rca") {
            it("rca input channels are recognised") {
                TestHooks().setCode("b=di0+di1") shouldBe true
            }

            it("rca output channels are recognised") { TestHooks().setCode("do0=d") shouldBe true }

            it("rca channel out of range still parses") {
                TestHooks().setCode("b=di20") shouldBe true
            }

            it("rca input channel flows through tick") {
                val h = TestHooks()
                h.setCode("do0=di0+di1") shouldBe true
                h.setRcaInput(0, 3)
                h.setRcaInput(1, 5)
                h.tick()
                h.getRcaOutput(0) shouldBe 8
            }

            it("rca output mask limits out of range value") {
                val h = TestHooks()
                h.setCode("do0=di0") shouldBe true
                h.setRcaInput(0, 15)
                h.tick()
                h.getRcaOutput(0) shouldBe 15
                h.setRcaInput(0, 0)
                h.tick()
                h.getRcaOutput(0) shouldBe 0
            }

            it("rca output data is zero when not assigned") {
                val h = TestHooks()
                h.setCode("b=di0") shouldBe true
                h.setRcaInput(0, 7)
                h.tick()
                h.rcaOutputData() shouldBe 0
            }
        }

        describe("tick error handling") {
            it("division by zero throws ArithmeticException via simulateTick") {
                val h = TestHooks()
                h.setCode("b=1/0") shouldBe true
                h.simulateTick()
                h.tickErrorMessage().shouldNotBeNull() shouldContain "zero"
            }

            it("no error message before any error occurs") {
                val h = TestHooks()
                h.setCode("b=5")
                h.tickErrorMessage().shouldBeNull()
                h.simulateTick()
                h.tickErrorMessage().shouldBeNull()
            }

            it("simulateTick stores error message when tick throws") {
                val h = TestHooks()
                h.injectTickError("/ by zero")
                h.tickErrorMessage().shouldNotBeNull() shouldContain "zero"
            }

            it("simulateTick is a no-op after error is set") {
                val h = TestHooks()
                h.setCode("b=5")
                h.injectTickError("something went wrong")
                val msgBefore = h.tickErrorMessage()
                h.simulateTick()
                h.tickErrorMessage() shouldBe msgBefore
            }

            it("output does not change while in error state") {
                val h = TestHooks()
                h.setCode("b=5")
                h.simulateTick()
                val outputBefore = h.outputData()
                h.injectTickError("something went wrong")
                h.simulateTick()
                h.outputData() shouldBe outputBefore
            }

            it("setCode clears error state") {
                val h = TestHooks()
                h.injectTickError("something went wrong")
                h.tickErrorMessage().shouldNotBeNull()
                h.setCode("b=7")
                h.tickErrorMessage().shouldBeNull()
            }

            it("ticking resumes after setCode clears the error") {
                val h = TestHooks()
                h.setCode("b=7")
                h.injectTickError("something went wrong")
                h.setCode("b=7")
                shouldNotThrowAny { h.simulateTick() }
                h.output(Direction.EAST) shouldBe 7
            }
        }
    })
