package wile.redstonepen.cblscript

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class CblScriptTest :
    StringSpec({

        // --- Compile-time tests ---

        "valid single-line assignment compiles with isValid true" {
            val p = CBLScript.compile("a = 5")
            p.isValid shouldBe true
            p.errors.shouldBeEmpty()
        }

        "syntax error produces isValid false with error entry" {
            val p = CBLScript.compile("a = (1 + 2")
            p.isValid shouldBe false
            p.errors.shouldNotBeEmpty()
        }

        "syntax error line offset is correct for second-line error" {
            val first = "x=5"
            val p = CBLScript.compile("$first\na=nosuch()")
            p.isValid shouldBe false
            p.errors.shouldNotBeEmpty()
            p.errors.keys.all { it > first.length } shouldBe true
        }

        "undefined function call produces parse error" {
            val p = CBLScript.compile("a=nofunc(1)")
            p.isValid shouldBe false
            p.errors.shouldNotBeEmpty()
        }

        "referencedSymbols contains input variable" {
            val p = CBLScript.compile("b=somevar+1")
            p.referencedSymbols.contains("somevar") shouldBe true
        }

        "assignedSymbols contains output variable" {
            val p = CBLScript.compile("outvar=5")
            p.assignedSymbols.contains("outvar") shouldBe true
        }

        "invalid symbol suffix produces parse error when validation enabled" {
            val p = CBLScript.compile("b=d.reed")
            p.isValid shouldBe false
            p.errors.shouldNotBeEmpty()
        }

        "invalid symbol suffix accepted when validSymbolSuffixes is null" {
            val p = CBLScript.compile("b=d.reed", validSymbolSuffixes = null)
            p.isValid shouldBe true
        }

        "custom FunctionDef is callable in compiled program" {
            val dbl = FunctionDef("dbl", 1) { x, m -> x[0].calc(m) * 2 }
            val p = CBLScript.compile("a=dbl(3)", functions = listOf(dbl))
            p.isValid shouldBe true
            val state = mutableMapOf<String, Int>()
            p.evaluate(state)["a"] shouldBe 6
        }

        // --- Evaluate-time tests ---

        "constant assignment returns correct value" {
            val p = CBLScript.compile("a = 5")
            val result = p.evaluate(mutableMapOf())
            result["a"] shouldBe 5
        }

        "variable reference with inputs map" {
            val p = CBLScript.compile("b = a")
            val result = p.evaluate(mutableMapOf(), inputs = mapOf("a" to 3))
            result["b"] shouldBe 3
        }

        "arithmetic precedence: 2 + 3 * 4 = 14" {
            val p = CBLScript.compile("c = 2 + 3 * 4")
            val result = p.evaluate(mutableMapOf())
            result["c"] shouldBe 14
        }

        "boolean and operator" {
            val p = CBLScript.compile("r = a and b")
            p.evaluate(mutableMapOf("a" to 1, "b" to 1), mapOf())["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 1, "b" to 0), mapOf())["r"] shouldBe 0
        }

        "boolean or operator" {
            val p = CBLScript.compile("r = a or b")
            p.evaluate(mutableMapOf("a" to 0, "b" to 1), mapOf())["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 0, "b" to 0), mapOf())["r"] shouldBe 0
        }

        "boolean xor operator" {
            val p = CBLScript.compile("r = a xor b")
            p.evaluate(mutableMapOf("a" to 1, "b" to 0), mapOf())["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 1, "b" to 1), mapOf())["r"] shouldBe 0
        }

        "not operator inverts truthiness" {
            val p = CBLScript.compile("r = !a")
            p.evaluate(mutableMapOf("a" to 0), mapOf())["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 1), mapOf())["r"] shouldBe 0
        }

        "equality operator" {
            val p = CBLScript.compile("r = a == 5")
            p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 4))["r"] shouldBe 0
        }

        "inequality operator" {
            val p = CBLScript.compile("r = a != 5")
            p.evaluate(mutableMapOf("a" to 6))["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 0
        }

        "less than operator" {
            val p = CBLScript.compile("r = a < 5")
            p.evaluate(mutableMapOf("a" to 3))["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 0
        }

        "greater than operator" {
            val p = CBLScript.compile("r = a > 5")
            p.evaluate(mutableMapOf("a" to 6))["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 0
        }

        "less than or equal operator" {
            val p = CBLScript.compile("r = a <= 5")
            p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 6))["r"] shouldBe 0
        }

        "greater than or equal operator" {
            val p = CBLScript.compile("r = a >= 5")
            p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 4))["r"] shouldBe 0
        }

        "if function both branches" {
            val p = CBLScript.compile("r = if(a, 7, 3)")
            p.evaluate(mutableMapOf("a" to 1))["r"] shouldBe 7
            p.evaluate(mutableMapOf("a" to 0))["r"] shouldBe 3
        }

        "lim function clamps value" {
            val p = CBLScript.compile("r = lim(a, 2, 8)")
            p.evaluate(mutableMapOf("a" to 1))["r"] shouldBe 2
            p.evaluate(mutableMapOf("a" to 5))["r"] shouldBe 5
            p.evaluate(mutableMapOf("a" to 10))["r"] shouldBe 8
        }

        "min and max functions" {
            val mn = CBLScript.compile("r = min(a, b)")
            mn.evaluate(mutableMapOf("a" to 3, "b" to 7))["r"] shouldBe 3
            val mx = CBLScript.compile("r = max(a, b)")
            mx.evaluate(mutableMapOf("a" to 3, "b" to 7))["r"] shouldBe 7
        }

        "mean function returns truncated average" {
            val p = CBLScript.compile("r = mean(a, b)")
            p.evaluate(mutableMapOf("a" to 3, "b" to 7))["r"] shouldBe 5
        }

        "inv function with default policy" {
            val p = CBLScript.compile("r = inv(a)")
            p.evaluate(mutableMapOf("a" to 0))["r"] shouldBe 1
            p.evaluate(mutableMapOf("a" to 1))["r"] shouldBe 0
        }

        "state persists between evaluate calls: counter increments" {
            val p = CBLScript.compile("r = cnt1(1)")
            val state = mutableMapOf<String, Int>()
            p.evaluate(state)
            p.evaluate(state)
            p.evaluate(state)
            (state.getOrDefault("r", 0)) shouldBe 3
        }

        "inputs map is merged into state at start of each evaluate" {
            val p = CBLScript.compile("r = a")
            val state = mutableMapOf<String, Int>()
            p.evaluate(state, inputs = mapOf("a" to 5))["r"] shouldBe 5
            p.evaluate(state, inputs = mapOf("a" to 9))["r"] shouldBe 9
        }

        // --- SignalPolicy tests ---

        "DEFAULT policy: ton1 returns 0 when off and 1 when on after period" {
            val p = CBLScript.compile("r = ton1(inp, 3)", policy = SignalPolicy.DEFAULT)
            val state = mutableMapOf<String, Int>()
            p.evaluate(state, mapOf("inp" to 0, ".clock" to 0))["r"] shouldBe 0
            state[".clock"] = 0
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
            state[".clock"] = 5
            val result = p.evaluate(state, mapOf("inp" to 1, ".clock" to 5))["r"]
            result shouldBe 1
        }

        "REDSTONE policy: ton1 returns 0 when off and 15 when on after period" {
            val p = CBLScript.compile("r = ton1(inp, 3)", policy = SignalPolicy.REDSTONE)
            val state = mutableMapOf<String, Int>()
            p.evaluate(state, mapOf("inp" to 0, ".clock" to 0))["r"] shouldBe 0
            state[".clock"] = 0
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))
            state[".clock"] = 5
            val result = p.evaluate(state, mapOf("inp" to 1, ".clock" to 5))["r"]
            result shouldBe 15
        }

        "outputClamp is applied to assigned symbols" {
            val p =
                CBLScript.compile(
                    "r = 100",
                    outputClamp = { sym, v -> if (sym == "r") v.coerceIn(0, 15) else v },
                )
            p.evaluate(mutableMapOf())["r"] shouldBe 15
        }

        // --- Built-in function tests ---

        "cnt1 with single arg increments on each truthy call" {
            val p = CBLScript.compile("r = cnt1(1)")
            val state = mutableMapOf<String, Int>()
            p.evaluate(state)
            p.evaluate(state)
            state.getOrDefault("r", 0) shouldBe 2
        }

        "cnt1 with max arg clamps to max" {
            val p = CBLScript.compile("r = cnt1(1, 0, 0, 5)")
            val state = mutableMapOf<String, Int>()
            repeat(10) { p.evaluate(state) }
            state.getOrDefault("r", 0) shouldBe 5
        }

        "ton1 is false until period elapses" {
            val p = CBLScript.compile("r = ton1(inp, 3)", policy = SignalPolicy.REDSTONE)
            val state = mutableMapOf<String, Int>()
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))["r"] shouldBe 0
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 2))["r"] shouldBe 0
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 4))["r"] shouldBe 15
        }

        "tof1 is true until period elapses after input goes low" {
            val p = CBLScript.compile("r = tof1(inp, 3)", policy = SignalPolicy.REDSTONE)
            val state = mutableMapOf<String, Int>()
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))["r"] shouldBe 15
            p.evaluate(state, mapOf("inp" to 0, ".clock" to 1))["r"] shouldBe 15
            p.evaluate(state, mapOf("inp" to 0, ".clock" to 5))["r"] shouldBe 0
        }

        "tp1 is true for exactly the period then false" {
            val p = CBLScript.compile("r = tp1(inp, 3)", policy = SignalPolicy.REDSTONE)
            val state = mutableMapOf<String, Int>()
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 0))["r"] shouldBe 15
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 2))["r"] shouldBe 15
            p.evaluate(state, mapOf("inp" to 1, ".clock" to 4))["r"] shouldBe 0
        }

        "tiv1 fires every N ticks" {
            val p = CBLScript.compile("r = tiv1(4)", policy = SignalPolicy.REDSTONE)
            val state = mutableMapOf<String, Int>()
            p.evaluate(state, mapOf(".clock" to 0))
            p.evaluate(state, mapOf(".clock" to 5))["r"] shouldBe 15
            p.evaluate(state, mapOf(".clock" to 6))["r"] shouldBe 0
            p.evaluate(state, mapOf(".clock" to 10))["r"] shouldBe 15
        }
    })
