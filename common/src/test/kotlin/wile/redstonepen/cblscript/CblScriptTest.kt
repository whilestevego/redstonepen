package wile.redstonepen.cblscript

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class CblScriptTest :
    DescribeSpec({
        fun compile(code: String, policy: SignalPolicy = SignalPolicy.DEFAULT) =
            CBLScript.compile(code, policy = policy)

        fun eval(
            code: String,
            inputs: Map<String, Int> = emptyMap(),
            policy: SignalPolicy = SignalPolicy.DEFAULT,
        ): Map<String, Int> {
            val p = compile(code, policy)
            p.isValid shouldBe true
            return p.evaluate(mutableMapOf(), inputs)
        }

        describe("compile") {
            describe("validity") {
                it("valid single-line assignment compiles with isValid true") {
                    compile("a = 5").isValid shouldBe true
                }

                it("empty code is valid") { compile("").isValid shouldBe true }

                it("comment-only line is valid") {
                    compile("# just a comment").isValid shouldBe true
                }

                it("whitespace-only lines are ignored") {
                    val p = compile("b = 5\n   \nu = 3")
                    val r = p.evaluate(mutableMapOf())
                    assertSoftly {
                        r["b"] shouldBe 5
                        r["u"] shouldBe 3
                    }
                }
            }

            describe("error reporting") {
                it("syntax error produces isValid false with error entry") {
                    val p = compile("a = (1 + 2")
                    assertSoftly {
                        p.isValid shouldBe false
                        p.errors.shouldNotBeEmpty()
                    }
                }

                it("syntax error line offset is correct for second-line error") {
                    val first = "x=5"
                    val p = compile("$first\na=nosuch()")
                    assertSoftly {
                        p.isValid shouldBe false
                        p.errors.shouldNotBeEmpty()
                        p.errors.keys.all { it > first.length } shouldBe true
                    }
                }

                it("undefined function call produces parse error") {
                    val p = compile("a=nofunc(1)")
                    assertSoftly {
                        p.isValid shouldBe false
                        p.errors.shouldNotBeEmpty()
                    }
                }

                it("unterminated parenthesis is invalid") {
                    compile("b=(1+2").isValid shouldBe false
                }

                it("missing rhs is invalid") { compile("b=").isValid shouldBe false }

                it("trailing dot suffix is invalid") { compile("b=d.").isValid shouldBe false }

                it("invalid character after expression is rejected") {
                    val p = compile("b=3 5")
                    assertSoftly {
                        p.isValid shouldBe false
                        p.errors.values.contains("invalid_character") shouldBe true
                    }
                }

                it("unknown function call is invalid") {
                    compile("b=foo(d)").isValid shouldBe false
                }
            }

            describe("symbol tracking") {
                it("referencedSymbols contains input variable") {
                    compile("b=somevar+1").referencedSymbols.contains("somevar") shouldBe true
                }

                it("assignedSymbols contains output variable") {
                    compile("outvar=5").assignedSymbols.contains("outvar") shouldBe true
                }
            }

            describe("symbol suffix validation") {
                it("invalid symbol suffix produces parse error when validation enabled") {
                    compile("b=d.reed").isValid shouldBe false
                }

                it("invalid symbol suffix accepted when validSymbolSuffixes is null") {
                    CBLScript.compile("b=d.reed", validSymbolSuffixes = null).isValid shouldBe true
                }

                it("all valid suffixes parse without error") {
                    for (suffix in listOf(".re", ".fe", ".co", ".co.re", ".co.fe", ".pt", ".et")) {
                        compile("b=d$suffix").isValid shouldBe true
                    }
                }
            }

            describe("custom functions") {
                it("custom FunctionDef is callable in compiled program") {
                    val dbl = FunctionDef("dbl", 1) { x, m -> x[0].calc(m) * 2 }
                    val p = CBLScript.compile("a=dbl(3)", functions = listOf(dbl))
                    assertSoftly {
                        p.isValid shouldBe true
                        p.evaluate(mutableMapOf())["a"] shouldBe 6
                    }
                }
            }
        }

        describe("arithmetic operators") {
            it("addition is commutative") {
                checkAll(Arb.int(-100..100), Arb.int(-100..100)) { a, b ->
                    eval("r = a + b", mapOf("a" to a, "b" to b))["r"] shouldBe a + b
                }
            }

            it("subtraction produces correct difference") {
                checkAll(Arb.int(-100..100), Arb.int(-100..100)) { a, b ->
                    eval("r = a - b", mapOf("a" to a, "b" to b))["r"] shouldBe a - b
                }
            }

            it("multiplication produces correct product") {
                checkAll(Arb.int(-20..20), Arb.int(-20..20)) { a, b ->
                    eval("r = a * b", mapOf("a" to a, "b" to b))["r"] shouldBe a * b
                }
            }

            it("division produces correct quotient for non-zero divisors") {
                checkAll(Arb.int(-50..50), Arb.int(1..20)) { a, b ->
                    eval("r = a / b", mapOf("a" to a, "b" to b))["r"] shouldBe a / b
                }
            }

            it("modulo produces correct remainder for non-zero divisors") {
                checkAll(Arb.int(0..50), Arb.int(1..10)) { a, b ->
                    eval("r = a % b", mapOf("a" to a, "b" to b))["r"] shouldBe a % b
                }
            }

            it("division by zero throws ArithmeticException") {
                val p = compile("r = a / 0")
                shouldThrow<ArithmeticException> { p.evaluate(mutableMapOf("a" to 5)) }
            }

            it("modulo by zero throws ArithmeticException") {
                val p = compile("r = a % 0")
                shouldThrow<ArithmeticException> { p.evaluate(mutableMapOf("a" to 9)) }
            }

            it("operator precedence: multiplication before addition") {
                eval("c = 2 + 3 * 4")["c"] shouldBe 14
            }

            it("parentheses override operator precedence") {
                eval("c = (2 + 3) * 4")["c"] shouldBe 20
            }

            it("unary negation") { eval("r = -a", mapOf("a" to 7))["r"] shouldBe -7 }
        }

        describe("boolean operators") {
            it("and/&&/& all act as logical and") {
                assertSoftly {
                    eval("r = a and b", mapOf("a" to 3, "b" to 5))["r"] shouldBe 1
                    eval("r = a and b", mapOf("a" to 0, "b" to 5))["r"] shouldBe 0
                    eval("r = a && b", mapOf("a" to 3, "b" to 5))["r"] shouldBe 1
                    eval("r = a && b", mapOf("a" to 0, "b" to 5))["r"] shouldBe 0
                    eval("r = a & b", mapOf("a" to 3, "b" to 5))["r"] shouldBe 1
                    eval("r = a & b", mapOf("a" to 0, "b" to 5))["r"] shouldBe 0
                }
            }

            it("or/||/| all act as logical or") {
                assertSoftly {
                    eval("r = a or b", mapOf("a" to 0, "b" to 1))["r"] shouldBe 1
                    eval("r = a or b", mapOf("a" to 0, "b" to 0))["r"] shouldBe 0
                    eval("r = a || b", mapOf("a" to 0, "b" to 5))["r"] shouldBe 1
                    eval("r = a || b", mapOf("a" to 0, "b" to 0))["r"] shouldBe 0
                    eval("r = a | b", mapOf("a" to 0, "b" to 5))["r"] shouldBe 1
                    eval("r = a | b", mapOf("a" to 0, "b" to 0))["r"] shouldBe 0
                }
            }

            it("xor/^ act as logical xor") {
                assertSoftly {
                    eval("r = a xor b", mapOf("a" to 1, "b" to 0))["r"] shouldBe 1
                    eval("r = a xor b", mapOf("a" to 1, "b" to 1))["r"] shouldBe 0
                    eval("r = a ^ b", mapOf("a" to 1, "b" to 0))["r"] shouldBe 1
                    eval("r = a ^ b", mapOf("a" to 1, "b" to 1))["r"] shouldBe 0
                }
            }

            it("not/! act as logical not") {
                assertSoftly {
                    eval("r = not a", mapOf("a" to 0))["r"] shouldBe 1
                    eval("r = not a", mapOf("a" to 1))["r"] shouldBe 0
                    eval("r = !a", mapOf("a" to 0))["r"] shouldBe 1
                    eval("r = !a", mapOf("a" to 5))["r"] shouldBe 0
                }
            }

            it("or is commutative: (a or b) == (b or a)") {
                checkAll(Arb.int(-50..50), Arb.int(-50..50)) { a, b ->
                    val ab = eval("r = a or b", mapOf("a" to a, "b" to b))["r"]
                    val ba = eval("r = b or a", mapOf("a" to a, "b" to b))["r"]
                    ab shouldBe ba
                }
            }

            it("and is commutative: (a and b) == (b and a)") {
                checkAll(Arb.int(-50..50), Arb.int(-50..50)) { a, b ->
                    val ab = eval("r = a and b", mapOf("a" to a, "b" to b))["r"]
                    val ba = eval("r = b and a", mapOf("a" to a, "b" to b))["r"]
                    ab shouldBe ba
                }
            }

            it("De Morgan's: not(a or b) == (not a) and (not b)") {
                checkAll(Arb.int(-50..50), Arb.int(-50..50)) { a, b ->
                    val lhs = eval("r = !(a or b)", mapOf("a" to a, "b" to b))["r"]
                    val rhs = eval("r = (!a) and (!b)", mapOf("a" to a, "b" to b))["r"]
                    lhs shouldBe rhs
                }
            }

            it("De Morgan's: not(a and b) == (not a) or (not b)") {
                checkAll(Arb.int(-50..50), Arb.int(-50..50)) { a, b ->
                    val lhs = eval("r = !(a and b)", mapOf("a" to a, "b" to b))["r"]
                    val rhs = eval("r = (!a) or (!b)", mapOf("a" to a, "b" to b))["r"]
                    lhs shouldBe rhs
                }
            }
        }

        describe("comparison operators") {
            it("== tests equality") {
                assertSoftly {
                    eval("r = a == 5", mapOf("a" to 5))["r"] shouldBe 1
                    eval("r = a == 5", mapOf("a" to 4))["r"] shouldBe 0
                }
            }

            it("!= and <> both test inequality") {
                assertSoftly {
                    eval("r = a != 5", mapOf("a" to 6))["r"] shouldBe 1
                    eval("r = a != 5", mapOf("a" to 5))["r"] shouldBe 0
                    eval("r = a <> 5", mapOf("a" to 6))["r"] shouldBe 1
                    eval("r = a <> 5", mapOf("a" to 5))["r"] shouldBe 0
                }
            }

            it("< > <= >= produce correct truth values") {
                assertSoftly {
                    eval("r = a < 5", mapOf("a" to 3))["r"] shouldBe 1
                    eval("r = a < 5", mapOf("a" to 5))["r"] shouldBe 0
                    eval("r = a > 5", mapOf("a" to 6))["r"] shouldBe 1
                    eval("r = a > 5", mapOf("a" to 5))["r"] shouldBe 0
                    eval("r = a <= 5", mapOf("a" to 5))["r"] shouldBe 1
                    eval("r = a <= 5", mapOf("a" to 6))["r"] shouldBe 0
                    eval("r = a >= 5", mapOf("a" to 5))["r"] shouldBe 1
                    eval("r = a >= 5", mapOf("a" to 4))["r"] shouldBe 0
                }
            }

            it("comparisons use trueValue from SignalPolicy") {
                val p = compile("r = a > 3", SignalPolicy.REDSTONE)
                assertSoftly {
                    p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 15
                    p.evaluate(mutableMapOf("a" to 2))["r"] shouldBe 0
                }
            }

            it("(a > b) == (b < a) for all integer pairs") {
                checkAll(Arb.int(-50..50), Arb.int(-50..50)) { a, b ->
                    val gt = eval("r = a > b", mapOf("a" to a, "b" to b))["r"]
                    val lt = eval("r = b < a", mapOf("a" to a, "b" to b))["r"]
                    gt shouldBe lt
                }
            }

            it("(a >= b) == (b <= a) for all integer pairs") {
                checkAll(Arb.int(-50..50), Arb.int(-50..50)) { a, b ->
                    val ge = eval("r = a >= b", mapOf("a" to a, "b" to b))["r"]
                    val le = eval("r = b <= a", mapOf("a" to a, "b" to b))["r"]
                    ge shouldBe le
                }
            }

            it("exactly one of (a < b), (a == b), (a > b) is true for any two integers") {
                checkAll(Arb.int(-50..50), Arb.int(-50..50)) { a, b ->
                    val lt = eval("r = a < b", mapOf("a" to a, "b" to b))["r"]!!
                    val eq = eval("r = a == b", mapOf("a" to a, "b" to b))["r"]!!
                    val gt = eval("r = a > b", mapOf("a" to a, "b" to b))["r"]!!
                    (lt + eq + gt) shouldBe 1
                }
            }
        }

        describe("evaluate") {
            it("constant assignment returns correct value") { eval("a = 5")["a"] shouldBe 5 }

            it("variable reference reads from inputs") {
                eval("b = a", mapOf("a" to 3))["b"] shouldBe 3
            }

            it("inputs are merged into state at start of each evaluate call") {
                val p = compile("r = a")
                val state = mutableMapOf<String, Int>()
                assertSoftly {
                    p.evaluate(state, mapOf("a" to 5))["r"] shouldBe 5
                    p.evaluate(state, mapOf("a" to 9))["r"] shouldBe 9
                }
            }

            it("multi-line program evaluates all assignments in order") {
                val p = compile("x = 3\ny = x + 2\nz = y * 2")
                val state = mutableMapOf<String, Int>()
                p.evaluate(state)
                assertSoftly {
                    state["x"] shouldBe 3
                    state["y"] shouldBe 5
                    state["z"] shouldBe 10
                }
            }

            it("later assignment sees earlier result in the same tick") {
                val result = compile("foo = 4\nbar = foo + 1").evaluate(mutableMapOf())
                result["bar"] shouldBe 5
            }

            it("duplicate assignment retains only the last value") {
                compile("r = 3\nr = 7").evaluate(mutableMapOf())["r"] shouldBe 7
            }

            it("inline comment after assignment is ignored") {
                eval("b = 5 # comment")["b"] shouldBe 5
            }

            it("state persists between evaluate calls") {
                val p = compile("r = cnt1(1)")
                val state = mutableMapOf<String, Int>()
                repeat(3) { p.evaluate(state) }
                state.getOrDefault("r", 0) shouldBe 3
            }
        }

        describe("outputClamp") {
            it("is applied to assigned symbols") {
                val p =
                    CBLScript.compile(
                        "r = 100",
                        outputClamp = { sym, v -> if (sym == "r") v.coerceIn(0, 15) else v },
                    )
                p.evaluate(mutableMapOf())["r"] shouldBe 15
            }
        }

        describe("SignalPolicy") {
            it("DEFAULT policy returns 1/0 for boolean results") {
                val p = compile("r = a and b")
                assertSoftly {
                    p.evaluate(mutableMapOf("a" to 1, "b" to 1))["r"] shouldBe 1
                    p.evaluate(mutableMapOf("a" to 1, "b" to 0))["r"] shouldBe 0
                }
            }

            it("REDSTONE policy returns 15/0 for boolean results") {
                val p = compile("r = a and b", SignalPolicy.REDSTONE)
                assertSoftly {
                    p.evaluate(mutableMapOf("a" to 1, "b" to 1))["r"] shouldBe 15
                    p.evaluate(mutableMapOf("a" to 1, "b" to 0))["r"] shouldBe 0
                }
            }
        }

        describe("builtin functions") {
            describe("inv") {
                it("complements relative to trueValue under DEFAULT policy") {
                    assertSoftly {
                        eval("r = inv(a)", mapOf("a" to 0))["r"] shouldBe 1
                        eval("r = inv(a)", mapOf("a" to 1))["r"] shouldBe 0
                    }
                }

                it("uses 15 as ceiling under REDSTONE policy") {
                    val p = compile("r = inv(a)", SignalPolicy.REDSTONE)
                    assertSoftly {
                        p.evaluate(mutableMapOf("a" to 0))["r"] shouldBe 15
                        p.evaluate(mutableMapOf("a" to 15))["r"] shouldBe 0
                        p.evaluate(mutableMapOf("a" to 3))["r"] shouldBe 12
                    }
                }

                it("inv(inv(x)) == x for all values in 0..trueValue") {
                    val p = compile("r = inv(inv(a))", SignalPolicy.REDSTONE)
                    checkAll(Arb.int(0..15)) { a ->
                        p.evaluate(mutableMapOf("a" to a))["r"] shouldBe a.coerceIn(0, 15)
                    }
                }
            }

            describe("max / min") {
                it("max returns the largest argument") {
                    assertSoftly {
                        eval("r = max(a, b, c)", mapOf("a" to 3, "b" to 8, "c" to 5))["r"] shouldBe
                            8
                        eval("r = max(a)", mapOf("a" to 7))["r"] shouldBe 7
                        eval("r = max()")["r"] shouldBe 0
                    }
                }

                it("min returns the smallest argument") {
                    assertSoftly {
                        eval("r = min(a, b)", mapOf("a" to 3, "b" to 8))["r"] shouldBe 3
                        eval("r = min(a)", mapOf("a" to 7))["r"] shouldBe 7
                    }
                }

                it("max(a,b) + min(a,b) == a + b") {
                    checkAll(Arb.int(-100..100), Arb.int(-100..100)) { a, b ->
                        val mx = eval("r = max(a, b)", mapOf("a" to a, "b" to b))["r"]!!
                        val mn = eval("r = min(a, b)", mapOf("a" to a, "b" to b))["r"]!!
                        mx + mn shouldBe a + b
                    }
                }

                it("max(a,b) >= a and max(a,b) >= b") {
                    checkAll(Arb.int(-100..100), Arb.int(-100..100)) { a, b ->
                        val mx = eval("r = max(a, b)", mapOf("a" to a, "b" to b))["r"]!!
                        assertSoftly {
                            (mx >= a) shouldBe true
                            (mx >= b) shouldBe true
                        }
                    }
                }
            }

            describe("lim") {
                it("three args clamp the value within the given range") {
                    assertSoftly {
                        eval("r = lim(a, 2, 8)", mapOf("a" to 1))["r"] shouldBe 2
                        eval("r = lim(a, 2, 8)", mapOf("a" to 5))["r"] shouldBe 5
                        eval("r = lim(a, 2, 8)", mapOf("a" to 10))["r"] shouldBe 8
                    }
                }

                it("two args clamp between zero and upper bound") {
                    eval("r = lim(a, 8)", mapOf("a" to 12))["r"] shouldBe 8
                }

                it("one arg clamps to 0..15") {
                    assertSoftly {
                        eval("r = lim(a)", mapOf("a" to 12))["r"] shouldBe 12
                        eval("r = lim(a)", mapOf("a" to -3))["r"] shouldBe 0
                    }
                }

                it("inverted range clamps to the upper bound") {
                    eval("r = lim(a, 8, 3)", mapOf("a" to 5))["r"] shouldBe 3
                }

                it("result is always within lo..hi for well-ordered range") {
                    checkAll(Arb.int(-100..100), Arb.int(-50..50), Arb.int(-50..50)) { v, lo, hi ->
                        val lo2 = minOf(lo, hi)
                        val hi2 = maxOf(lo, hi)
                        val r =
                            eval("r = lim(v, lo, hi)", mapOf("v" to v, "lo" to lo2, "hi" to hi2))[
                                "r"]!!
                        assertSoftly {
                            (r >= lo2) shouldBe true
                            (r <= hi2) shouldBe true
                        }
                    }
                }

                it("result equals v when v is already within lo..hi") {
                    checkAll(Arb.int(0..10), Arb.int(0..5), Arb.int(5..15)) { v, lo, hi ->
                        val inRange = v.coerceIn(lo, hi)
                        eval("r = lim(v, lo, hi)", mapOf("v" to inRange, "lo" to lo, "hi" to hi))[
                            "r"] shouldBe inRange
                    }
                }
            }

            describe("if") {
                it("three args selects the true or false branch") {
                    assertSoftly {
                        eval("r = if(a, 5, 2)", mapOf("a" to 3))["r"] shouldBe 5
                        eval("r = if(a, 5, 2)", mapOf("a" to 0))["r"] shouldBe 2
                    }
                }

                it("two args uses zero as the else branch") {
                    assertSoftly {
                        eval("r = if(a, 7)", mapOf("a" to 0))["r"] shouldBe 0
                        eval("r = if(a, 7)", mapOf("a" to 1))["r"] shouldBe 7
                    }
                }

                it("one arg returns trueValue or falseValue") {
                    assertSoftly {
                        eval("r = if(a)", mapOf("a" to 1))["r"] shouldBe 1
                        eval("r = if(a)", mapOf("a" to 0))["r"] shouldBe 0
                    }
                }
            }

            describe("mean") {
                it("returns integer average of all arguments") {
                    assertSoftly {
                        eval("r = mean(a, b)", mapOf("a" to 3, "b" to 7))["r"] shouldBe 5
                        eval("r = mean(a, b, c)", mapOf("a" to 3, "b" to 6, "c" to 9))["r"] shouldBe
                            6
                        eval("r = mean(a)", mapOf("a" to 6))["r"] shouldBe 6
                    }
                }
            }

            describe("clock / time") {
                it("clock() reads .clock from state") {
                    compile("r = clock()").evaluate(mutableMapOf(".clock" to 7))["r"] shouldBe 7
                }

                it("time() reads .time from state") {
                    compile("r = time()").evaluate(mutableMapOf(".time" to 5))["r"] shouldBe 5
                }
            }

            describe("rnd") {
                it("always returns a value in 0..15") {
                    val p = compile("r = rnd()")
                    repeat(20) {
                        val v = p.evaluate(mutableMapOf())["r"]!!
                        (v in 0..15) shouldBe true
                    }
                }
            }
        }

        describe("timers: ton (timer-on delay)") {
            it("ton1..ton5 each maintain independent state") {
                val p =
                    compile(
                        "a=ton1(inp,3)\nb=ton2(inp,3)\nc=ton3(inp,3)\nd=ton4(inp,3)\ne=ton5(inp,3)",
                        SignalPolicy.REDSTONE,
                    )
                val state = mutableMapOf<String, Int>()
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
                state[".clock"] = 5
                val r = p.evaluate(state, mapOf("inp" to 1, ".clock" to 5))
                assertSoftly {
                    r["a"] shouldBe 15
                    r["b"] shouldBe 15
                    r["c"] shouldBe 15
                    r["d"] shouldBe 15
                    r["e"] shouldBe 15
                }
            }

            it("is false until the delay elapses then true") {
                val p = compile("r = ton1(inp, 3)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                assertSoftly {
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))["r"] shouldBe 0
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 2))["r"] shouldBe 0
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 4))["r"] shouldBe 15
                }
            }

            it("resets when input goes low") {
                val p = compile("r = ton1(inp, 4)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
                p.evaluate(state, mapOf("inp" to 0, ".clock" to 1))["r"] shouldBe 0
            }

            it("with zero period returns true immediately") {
                compile("r = ton1(inp, 0)", SignalPolicy.REDSTONE)
                    .evaluate(mutableMapOf("inp" to 1))["r"] shouldBe 15
            }
        }

        describe("timers: tof (timer-off delay)") {
            it("tof1..tof5 each maintain independent state") {
                val p =
                    compile(
                        "a=tof1(inp,3)\nb=tof2(inp,3)\nc=tof3(inp,3)\nd=tof4(inp,3)\ne=tof5(inp,3)",
                        SignalPolicy.REDSTONE,
                    )
                val state = mutableMapOf(".clock" to 0, "inp" to 1)
                p.evaluate(state)
                state["inp"] = 0
                state[".clock"] = 1
                val mid = p.evaluate(state)
                assertSoftly {
                    mid["a"] shouldBe 15
                    mid["b"] shouldBe 15
                    mid["c"] shouldBe 15
                    mid["d"] shouldBe 15
                    mid["e"] shouldBe 15
                }
            }

            it("stays true until the delay elapses after input goes low") {
                val p = compile("r = tof1(inp, 3)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                assertSoftly {
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))["r"] shouldBe 15
                    p.evaluate(state, mapOf("inp" to 0, ".clock" to 1))["r"] shouldBe 15
                    p.evaluate(state, mapOf("inp" to 0, ".clock" to 5))["r"] shouldBe 0
                }
            }

            it("resets to true when input returns high") {
                val p = compile("r = tof1(inp, 4)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
                p.evaluate(state, mapOf("inp" to 0, ".clock" to 1))
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 2))["r"] shouldBe 15
            }

            it("with zero period returns true immediately") {
                compile("r = tof1(inp, 0)", SignalPolicy.REDSTONE)
                    .evaluate(mutableMapOf("inp" to 0))["r"] shouldBe 15
            }
        }

        describe("timers: tp (pulse)") {
            it("tp1..tp5 each maintain independent state") {
                val p =
                    compile(
                        "a=tp1(inp,3)\nb=tp2(inp,3)\nc=tp3(inp,3)\nd=tp4(inp,3)\ne=tp5(inp,3)",
                        SignalPolicy.REDSTONE,
                    )
                val r = p.evaluate(mutableMapOf(".clock" to 0, "inp" to 1))
                assertSoftly {
                    r["a"] shouldBe 15
                    r["b"] shouldBe 15
                    r["c"] shouldBe 15
                    r["d"] shouldBe 15
                    r["e"] shouldBe 15
                }
            }

            it("output is true for the pulse period then falls") {
                val p = compile("r = tp1(inp, 3)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                assertSoftly {
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))["r"] shouldBe 15
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 2))["r"] shouldBe 15
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 4))["r"] shouldBe 0
                }
            }

            it("requires input to drop before it can retrigger") {
                val p = compile("r = tp1(inp, 3)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
                assertSoftly {
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 5))["r"] shouldBe 0
                    p.evaluate(state, mapOf("inp" to 0, ".clock" to 6))
                    p.evaluate(state, mapOf("inp" to 1, ".clock" to 7))["r"] shouldBe 15
                }
            }
        }

        describe("timers: tiv (interval)") {
            it("tiv1..tiv3 each maintain independent state") {
                val p = compile("a=tiv1(4)\nb=tiv2(4)\nc=tiv3(4)", SignalPolicy.REDSTONE)
                val state = mutableMapOf(".clock" to 0)
                p.evaluate(state)
                state[".clock"] = 5
                val r = p.evaluate(state)
                assertSoftly {
                    r["a"] shouldBe 15
                    r["b"] shouldBe 15
                    r["c"] shouldBe 15
                }
            }

            it("fires once per period") {
                val p = compile("r = tiv1(4)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                assertSoftly {
                    p.evaluate(state, mapOf(".clock" to 0))
                    p.evaluate(state, mapOf(".clock" to 5))["r"] shouldBe 15
                    p.evaluate(state, mapOf(".clock" to 6))["r"] shouldBe 0
                    p.evaluate(state, mapOf(".clock" to 10))["r"] shouldBe 15
                }
            }

            it("returns zero when enable signal is low") {
                compile("r = tiv1(5, 0)").evaluate(mutableMapOf())["r"] shouldBe 0
            }

            it("returns false for period less than 3") {
                compile("r = tiv1(2)").evaluate(mutableMapOf())["r"] shouldBe 0
            }

            it("returns zero with no arguments") {
                val p = compile("r = tiv1()")
                assertSoftly {
                    p.evaluate(mutableMapOf(".clock" to 0))["r"] shouldBe 0
                    p.evaluate(mutableMapOf(".clock" to 100))["r"] shouldBe 0
                }
            }
        }

        describe("counters: cnt1..cnt5") {
            it("cnt1..cnt5 each maintain independent counts") {
                val p = compile("a=cnt1(1)\nb=cnt2(1)\nc=cnt3(1)\nd=cnt4(1)\ne=cnt5(1)")
                val state = mutableMapOf<String, Int>()
                repeat(3) { p.evaluate(state) }
                assertSoftly {
                    state["a"] shouldBe 3
                    state["b"] shouldBe 3
                    state["c"] shouldBe 3
                    state["d"] shouldBe 3
                    state["e"] shouldBe 3
                }
            }

            it("increments on truthy input, holds on zero input") {
                val pUp = compile("r = cnt1(1)")
                val pHold = compile("r = cnt1(0)")
                val stateUp = mutableMapOf<String, Int>()
                val stateHold = mutableMapOf<String, Int>()
                repeat(3) { pUp.evaluate(stateUp) }
                repeat(5) { pHold.evaluate(stateHold) }
                assertSoftly {
                    stateUp.getOrDefault("r", 0) shouldBe 3
                    stateHold.getOrDefault("r", 0) shouldBe 0
                }
            }

            it("decrements on second-arg rising edge") {
                val p = compile("r = cnt1(up, dn)")
                val state = mutableMapOf<String, Int>()
                p.evaluate(state, mapOf("up" to 1, "dn" to 0))
                p.evaluate(state, mapOf("up" to 1, "dn" to 0))
                val before = state.getOrDefault("r", 0)
                p.evaluate(state, mapOf("up" to 0, "dn" to 1))
                state.getOrDefault("r", 0) shouldBe before - 1
            }

            it("three args clamp count between zero and max") {
                val p = compile("r = cnt1(1, 0, 5)")
                val state = mutableMapOf<String, Int>()
                repeat(10) { p.evaluate(state) }
                state.getOrDefault("r", 0) shouldBe 5
            }

            it("four args clamp count between explicit min and max") {
                val p = compile("r = cnt1(up, dn, 2, 8)")
                val state = mutableMapOf<String, Int>()
                repeat(10) { p.evaluate(state, mapOf("up" to 0, "dn" to 1)) }
                repeat(10) { p.evaluate(state, mapOf("up" to 1, "dn" to 0)) }
                assertSoftly { state.getOrDefault("r", 0) shouldBe 8 }
            }

            it("five args reset the counter on a truthy fifth argument") {
                val p = compile("r = cnt1(up, dn, 0, 15, rst)")
                val state = mutableMapOf<String, Int>()
                repeat(3) { p.evaluate(state, mapOf("up" to 1, "dn" to 0, "rst" to 0)) }
                p.evaluate(state, mapOf("up" to 0, "dn" to 0, "rst" to 1))
                state.getOrDefault("r", 0) shouldBe 0
            }
        }

        describe("SignalPolicy: timer return values") {
            it("DEFAULT policy: ton1 returns 0 or 1") {
                val p = compile("r = ton1(inp, 3)", SignalPolicy.DEFAULT)
                val state = mutableMapOf<String, Int>()
                p.evaluate(state, mapOf("inp" to 0, ".clock" to 0))["r"] shouldBe 0
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
                state[".clock"] = 5
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 5))["r"] shouldBe 1
            }

            it("REDSTONE policy: ton1 returns 0 or 15") {
                val p = compile("r = ton1(inp, 3)", SignalPolicy.REDSTONE)
                val state = mutableMapOf<String, Int>()
                p.evaluate(state, mapOf("inp" to 0, ".clock" to 0))["r"] shouldBe 0
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
                state[".clock"] = 5
                p.evaluate(state, mapOf("inp" to 1, ".clock" to 5))["r"] shouldBe 15
            }
        }
    })
