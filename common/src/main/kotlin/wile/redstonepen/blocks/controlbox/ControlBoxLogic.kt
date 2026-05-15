package wile.redstonepen.blocks.controlbox

import java.util.*
import java.util.stream.Collectors
import net.minecraft.util.Mth

internal class ControlBoxLogic {

    var input_mask = 0x00000000
    var input_data = 0x00000000
    var output_mask = 0x00000000
    var output_data = 0x00000000
    var intr_redges = 0x00000000
    var intr_fedges = 0x00000000
    var rca_input_mask = 0L
    var rca_input_data = 0L
    var rca_output_mask = 0L
    var rca_output_data = 0L

    private var symbols_: MutableMap<String, Int> = HashMap()
    private var expressions_: MultiLineMathExpr = MultiLineMathExpr.EMPTY
    private var code_: String = ""

    fun valid(): Boolean = expressions_.invalid_entries.isEmpty()

    fun usesSymbol(s: String): Boolean = expressions_.symbols.contains(s)

    fun errors(): Map<Int, String> =
        expressions_.invalid_entries
            .stream()
            .collect(Collectors.toMap({ e -> e.offset + e.parsed.pe }, { e -> e.parsed.error }))

    fun symbol(key: String, value: Int) {
        symbols_[key.lowercase()] = value
    }

    fun symbol(key: String): Int = symbols_.getOrDefault(key.lowercase(), 0)

    fun symbols(): MutableMap<String, Int> = symbols_

    fun clearSymbols() {
        symbols_.clear()
    }

    fun code(): String = code_

    fun code(new_code: String): Boolean {
        if (code_ == new_code && !expressions_.isEmpty()) {
            return expressions_.invalid_entries.isEmpty()
        }
        code_ = new_code
        expressions_ = MultiLineMathExpr.of(code_, "", functions_)
        input_mask = 0
        output_mask = 0
        rca_input_mask = 0
        rca_output_mask = 0
        symbols_.clear()
        for (i in 0 until Defs.PORT_NAMES.size) {
            val port = Defs.PORT_NAMES[i]
            if (
                expressions_.symbols.contains(port + ".co.re") ||
                    expressions_.symbols.contains(port + ".co.fe")
            ) {
                expressions_.symbols.add(port + ".co")
            }
            if (expressions_.assignments.contains(port)) {
                output_mask = output_mask or (0xf shl (4 * i))
            } else if (
                MultiLineMathExpr.VALID_SYMBOL_SUFFIXES.any { s ->
                    expressions_.symbols.contains(port + s)
                }
            ) {
                input_mask = input_mask or (0xf shl (4 * i))
            }
        }
        expressions_.symbols.forEach { esym ->
            if (esym.matches(Regex("^d[io][1]?[\\d][\\d]?\$"))) {
                val channel = esym.substring(2).toInt()
                if (channel > 15) return@forEach
                if (esym[1] == 'i') {
                    rca_input_mask = rca_input_mask or (0xfL shl (channel * 4))
                } else {
                    rca_output_mask = rca_output_mask or (0xfL shl (channel * 4))
                }
            }
        }
        rca_input_data = rca_input_data and rca_input_mask
        rca_output_data = rca_output_data and rca_output_mask
        output_data = output_data and output_mask
        input_data = input_data and input_mask
        return expressions_.invalid_entries.isEmpty()
    }

    fun tick() {
        for (i in 0 until Defs.PORT_NAMES.size) {
            if ((input_mask and (0xf shl (4 * i))) != 0) {
                symbol(Defs.PORT_NAMES[i], (input_data shr (4 * i)) and 0xf)
            }
        }
        if (rca_input_mask != 0L) {
            for (i in 0 until 16) {
                if ((rca_input_mask and (0xfL shl (4 * i))) != 0L) {
                    symbol("di$i", ((rca_input_data shr (4 * i)) and 0xfL).toInt())
                }
            }
        }
        expressions_.symbols.forEach { esym ->
            if (!esym.contains(".")) return@forEach
            val isrising = esym.endsWith(".re")
            if (isrising || esym.endsWith(".fe")) {
                val syms = symbols()
                val symref = esym.substring(0, esym.length - 3)
                if (!syms.containsKey(symref) && !Defs.PORT_NAMES.contains(symref)) return@forEach
                val symlast = ".$esym.d"
                val q1 = if (syms.getOrDefault(symref, 0) > 0) 15 else 0
                var edge = false
                if (syms.containsKey(symlast)) {
                    val q0 = if (syms.getOrDefault(symlast, 0) > 0) 15 else 0
                    edge = if (isrising) (q0 <= 0 && q1 > 0) else (q0 > 0 && q1 <= 0)
                }
                symbol(esym, if (edge) 15 else 0)
                symbol(symlast, q1)
            }
        }
        for (i in 0 until Defs.PORT_NAMES.size) {
            val port_mask = 0xf shl (4 * i)
            if ((intr_redges and port_mask) != 0) symbol(Defs.PORT_NAMES[i] + ".re", 15)
            if ((intr_fedges and port_mask) != 0) symbol(Defs.PORT_NAMES[i] + ".fe", 15)
        }
        intr_redges = 0
        intr_fedges = 0
        symbol(".deadline", 40)
        val assigned =
            expressions_.recalculate(symbols_) { entry, _ ->
                when (entry.parsed.assignment_symbol) {
                    "r",
                    "b",
                    "y",
                    "g",
                    "u",
                    "d" -> Math.max(0, Math.min(15, entry.last_result))
                    else -> entry.last_result
                }
            }
        assigned.forEach { (k, v) -> symbol(k, v) }
        output_data = 0
        for (i in 0 until Defs.PORT_NAMES.size) {
            output_data = output_data or ((symbol(Defs.PORT_NAMES[i]) and 0xf) shl (4 * i))
        }
        output_data = output_data and output_mask
        if (rca_output_mask != 0L) {
            rca_output_data = 0L
            for (i in 0 until 16) {
                if ((rca_output_mask and (0xfL shl (4 * i))) != 0L) {
                    rca_output_data =
                        rca_output_data or
                            (Math.min(15, Math.max(0, symbol("do$i"))).toLong() shl (4 * i))
                }
            }
        }
        rca_output_data = rca_output_data and rca_output_mask
    }

    companion object {
        private fun counter_function(
            sym: String,
            x: Array<MathExpr.Expr>,
            m: MutableMap<String, Int>,
        ): Int {
            val nargs = x.size
            if (nargs <= 0) return 0
            var q = m.getOrDefault(sym, 0)
            if (nargs >= 5 && x[4].calc(m) > 0) {
                q = 0
            } else if (nargs == 1) {
                if (x[0].calc(m) > 0) ++q
            } else {
                val x0 = x[0].calc(m)
                val x1 = x[1].calc(m)
                if (x0 > 0 && x1 <= 0) {
                    ++q
                } else if (x0 <= 0 && x1 > 0) {
                    --q
                }
            }
            q =
                if (nargs >= 4) {
                    Mth.clamp(q, x[2].calc(m), x[3].calc(m))
                } else if (nargs >= 3) {
                    Mth.clamp(q, 0, x[2].calc(m))
                } else {
                    Mth.clamp(q, 0, 0x7fffffff)
                }
            m[sym] = q
            return q
        }

        private fun timer_on_function(
            sym: String,
            x: Array<MathExpr.Expr>,
            m: MutableMap<String, Int>,
        ): Int {
            if (x.size != 2) {
                m.remove(".$sym.clk")
                m.remove("$sym.et")
                m.remove("$sym.pt")
                return 0
            }
            val inp = x[0].calc(m)
            val pt = x[1].calc(m)
            if (inp <= 0) {
                m.remove(".$sym.clk")
                m["$sym.et"] = 0
                return MathExpr.Expr.bool_false
            } else if (pt <= 0) {
                return MathExpr.Expr.bool_true
            } else {
                val now = m.getOrDefault(".clock", 0)
                var et = m.getOrDefault("$sym.et", 0)
                if (et >= pt) {
                    return MathExpr.Expr.bool_true
                } else if (et <= 0) {
                    m[".$sym.clk"] = now
                    m["$sym.et"] = 1
                    m["$sym.pt"] = pt
                    m[".deadline"] = Math.min(m.getOrDefault(".deadline", 20), pt)
                    return MathExpr.Expr.bool_false
                } else {
                    et = Math.min(now - m.getOrDefault(".$sym.clk", now), pt)
                    m["$sym.et"] = et
                    if (et >= pt) {
                        m.remove(".$sym.clk")
                        return MathExpr.Expr.bool_true
                    } else {
                        m[".deadline"] = Math.min(m.getOrDefault(".deadline", 20), pt - et)
                        return MathExpr.Expr.bool_false
                    }
                }
            }
        }

        private fun timer_off_function(
            sym: String,
            x: Array<MathExpr.Expr>,
            m: MutableMap<String, Int>,
        ): Int {
            if (x.size != 2) {
                m.remove(".$sym.clk")
                m.remove("$sym.et")
                m.remove("$sym.pt")
                return 0
            }
            val inp = x[0].calc(m)
            val pt = x[1].calc(m)
            if (inp > 0) {
                m.remove(".$sym.clk")
                m["$sym.et"] = 0
                return MathExpr.Expr.bool_true
            } else if (pt <= 0) {
                return MathExpr.Expr.bool_true
            } else {
                val now = m.getOrDefault(".clock", 0)
                var et = m.getOrDefault("$sym.et", 0)
                if (et >= pt) {
                    return MathExpr.Expr.bool_false
                } else if (et <= 0) {
                    m[".$sym.clk"] = now
                    m["$sym.et"] = 1
                    m["$sym.pt"] = pt
                    m[".deadline"] = Math.min(m.getOrDefault(".deadline", 20), pt)
                    return MathExpr.Expr.bool_true
                } else {
                    et = Math.min(now - m.getOrDefault(".$sym.clk", now), pt)
                    m["$sym.et"] = et
                    if (et >= pt) {
                        m.remove(".$sym.clk")
                        return MathExpr.Expr.bool_false
                    } else {
                        m[".deadline"] = Math.min(m.getOrDefault(".deadline", 20), pt - et)
                        return MathExpr.Expr.bool_true
                    }
                }
            }
        }

        private fun timer_pulse_function(
            sym: String,
            x: Array<MathExpr.Expr>,
            m: MutableMap<String, Int>,
        ): Int {
            if (x.size != 2) {
                m.remove(".$sym.clk")
                m.remove("$sym.et")
                m.remove("$sym.pt")
                return 0
            }
            val inp = x[0].calc(m)
            val pt = x[1].calc(m)
            if (pt <= 0) {
                return if (inp > 0) MathExpr.Expr.bool_true else MathExpr.Expr.bool_false
            }
            var et = m.getOrDefault("$sym.et", 0)
            if (et > 0) {
                if (et >= pt) {
                    if (inp <= 0) m["$sym.et"] = 0
                    return MathExpr.Expr.bool_false
                } else {
                    val now = m.getOrDefault(".clock", 0)
                    et = Math.min(now - m.getOrDefault(".$sym.clk", now), pt)
                    m["$sym.et"] = et
                    if (et >= pt) {
                        m.remove(".$sym.clk")
                        return MathExpr.Expr.bool_false
                    } else {
                        m[".deadline"] = Math.min(m.getOrDefault(".deadline", 20), pt - et)
                        return MathExpr.Expr.bool_true
                    }
                }
            } else if (inp > 0) {
                m[".$sym.clk"] = m.getOrDefault(".clock", 0)
                m["$sym.et"] = 1
                m["$sym.pt"] = pt
                m[".deadline"] = Math.min(m.getOrDefault(".deadline", 20), pt)
                return MathExpr.Expr.bool_true
            } else {
                return MathExpr.Expr.bool_false
            }
        }

        private fun timer_interval_function(
            sym: String,
            x: Array<MathExpr.Expr>,
            m: MutableMap<String, Int>,
        ): Int {
            if (x.size < 1 || x.size > 2) {
                m.remove("$sym.clk")
                return 0
            }
            val en = if (x.size < 2) 15 else x[1].calc(m)
            if (en <= 0) {
                m.remove("$sym.clk")
                return 0
            }
            val pt = x[0].calc(m)
            if (pt <= 2) return MathExpr.Expr.bool_false
            val now = m.getOrDefault(".clock", 0)
            val clk = m.getOrDefault("$sym.clk", now - pt)
            if (Math.abs(now - clk) >= pt) {
                m["$sym.clk"] = now
                m[".deadline"] = 1
                return MathExpr.Expr.bool_true
            } else {
                m[".deadline"] = Math.min(m.getOrDefault(".deadline", 20), clk - now + pt)
                return MathExpr.Expr.bool_false
            }
        }

        private fun makeFunctions(): List<MathExpr.ExprFuncDef> =
            listOf(
                MathExpr.ExprFuncDef(
                    "inv",
                    1,
                    { x, m -> Math.min(15, Math.max(0, 15 - x[0].calc(m))) },
                ),
                MathExpr.ExprFuncDef("max", -1, { x, m -> x.maxOfOrNull { it.calc(m) } ?: 0 }),
                MathExpr.ExprFuncDef("min", -1, { x, m -> x.minOfOrNull { it.calc(m) } ?: 0 }),
                MathExpr.ExprFuncDef(
                    "lim",
                    -1,
                    { x, m ->
                        when (x.size) {
                            0 -> 0
                            1 -> Math.min(15, Math.max(0, x[0].calc(m)))
                            2 -> Math.min(x[1].calc(m), Math.max(0, x[0].calc(m)))
                            else -> Math.min(x[2].calc(m), Math.max(x[1].calc(m), x[0].calc(m)))
                        }
                    },
                ),
                MathExpr.ExprFuncDef(
                    "if",
                    -1,
                    { x, m ->
                        when (x.size) {
                            0 -> 0
                            1 -> if (x[0].calc(m) > 0) 15 else 0
                            2 -> if (x[0].calc(m) > 0) x[1].calc(m) else 0
                            else -> if (x[0].calc(m) > 0) x[1].calc(m) else x[2].calc(m)
                        }
                    },
                ),
                MathExpr.ExprFuncDef(
                    "mean",
                    -1,
                    { x, m -> if (x.isEmpty()) 0 else x.sumOf { it.calc(m) } / x.size },
                ),
                MathExpr.ExprFuncDef("rnd", 0, { _, _ -> (Math.random() * 16.0).toInt() }),
                MathExpr.ExprFuncDef("clock", 0, { _, m -> m.getOrDefault(".clock", 0) }),
                MathExpr.ExprFuncDef("time", 0, { _, m -> m.getOrDefault(".time", 0) }),
                MathExpr.ExprFuncDef(
                    "tiv1",
                    -1,
                    { x, m -> timer_interval_function(".tiv1", x, m) },
                ),
                MathExpr.ExprFuncDef(
                    "tiv2",
                    -1,
                    { x, m -> timer_interval_function(".tiv2", x, m) },
                ),
                MathExpr.ExprFuncDef(
                    "tiv3",
                    -1,
                    { x, m -> timer_interval_function(".tiv3", x, m) },
                ),
                MathExpr.ExprFuncDef("cnt1", -1, { x, m -> counter_function(".cnt1", x, m) }),
                MathExpr.ExprFuncDef("cnt2", -1, { x, m -> counter_function(".cnt2", x, m) }),
                MathExpr.ExprFuncDef("cnt3", -1, { x, m -> counter_function(".cnt3", x, m) }),
                MathExpr.ExprFuncDef("cnt4", -1, { x, m -> counter_function(".cnt4", x, m) }),
                MathExpr.ExprFuncDef("cnt5", -1, { x, m -> counter_function(".cnt5", x, m) }),
                MathExpr.ExprFuncDef("ton1", 2, { x, m -> timer_on_function("ton1", x, m) }),
                MathExpr.ExprFuncDef("ton2", 2, { x, m -> timer_on_function("ton2", x, m) }),
                MathExpr.ExprFuncDef("ton3", 2, { x, m -> timer_on_function("ton3", x, m) }),
                MathExpr.ExprFuncDef("ton4", 2, { x, m -> timer_on_function("ton4", x, m) }),
                MathExpr.ExprFuncDef("ton5", 2, { x, m -> timer_on_function("ton5", x, m) }),
                MathExpr.ExprFuncDef("tof1", 2, { x, m -> timer_off_function("tof1", x, m) }),
                MathExpr.ExprFuncDef("tof2", 2, { x, m -> timer_off_function("tof2", x, m) }),
                MathExpr.ExprFuncDef("tof3", 2, { x, m -> timer_off_function("tof3", x, m) }),
                MathExpr.ExprFuncDef("tof4", 2, { x, m -> timer_off_function("tof4", x, m) }),
                MathExpr.ExprFuncDef("tof5", 2, { x, m -> timer_off_function("tof5", x, m) }),
                MathExpr.ExprFuncDef("tp1", 2, { x, m -> timer_pulse_function("tp1", x, m) }),
                MathExpr.ExprFuncDef("tp2", 2, { x, m -> timer_pulse_function("tp2", x, m) }),
                MathExpr.ExprFuncDef("tp3", 2, { x, m -> timer_pulse_function("tp3", x, m) }),
                MathExpr.ExprFuncDef("tp4", 2, { x, m -> timer_pulse_function("tp4", x, m) }),
                MathExpr.ExprFuncDef("tp5", 2, { x, m -> timer_pulse_function("tp5", x, m) }),
            )

        private val functions_: List<MathExpr.ExprFuncDef> = makeFunctions()
    }

    class MultiLineMathExpr {

        companion object {
            @JvmField val EMPTY = MultiLineMathExpr()

            @JvmField
            val VALID_SYMBOL_SUFFIXES =
                arrayOf("", ".re", ".fe", ".co", ".co.re", ".co.fe", ".pt", ".et")

            @JvmStatic fun of(code: String): MultiLineMathExpr = of(code, "", emptyList())

            @JvmStatic
            fun of(code: String, assignment_variable: String): MultiLineMathExpr =
                of(code, assignment_variable, emptyList())

            @JvmStatic
            fun of(
                code: String,
                assignment_variable: String,
                functions: Collection<MathExpr.ExprFuncDef>,
            ): MultiLineMathExpr {
                if (code.trim().isEmpty()) return EMPTY
                val entries: MutableList<Entry> = ArrayList()
                val lines = code.replace("[\\r\\n\\s]+\$", "").split(Regex("[\\r]?[\\n]"))
                val parse_errors: MutableList<Entry> = ArrayList()
                val symbols: MutableSet<String> = HashSet()
                val assignments: MutableSet<String> = HashSet()
                var line_index = 0
                var offset = 0
                for (line in lines) {
                    if (line.trim().isNotEmpty()) {
                        val entry =
                            Entry(
                                line_index,
                                offset,
                                MathExpr.ParsedLine.of(line, assignment_variable, functions),
                            )
                        if (entry.parsed.error.isNotEmpty()) {
                            parse_errors.add(entry)
                        } else {
                            val invalid_symbol =
                                entry.parsed.symbols.firstOrNull { sym ->
                                    val pos = sym.indexOf('.')
                                    if (pos < 0) {
                                        false
                                    } else if (pos >= sym.length - 1) {
                                        true
                                    } else {
                                        !VALID_SYMBOL_SUFFIXES.contains(sym.substring(pos))
                                    }
                                }
                            if (invalid_symbol != null) {
                                entry.parsed.error = "parse_error"
                                entry.parsed.pe = entry.parsed.line.indexOf(invalid_symbol)
                                if (entry.parsed.pe < 0) {
                                    entry.parsed.pe = entry.parsed.line.length - 1
                                }
                                parse_errors.add(entry)
                            } else {
                                symbols.addAll(entry.parsed.symbols)
                                if (entry.parsed.expression.type == MathExpr.ExprType.ASSIGN) {
                                    entries.add(entry)
                                    assignments.add(entry.parsed.assignment_symbol)
                                }
                            }
                        }
                    }
                    ++line_index
                    offset += 1 + line.length
                }
                return if (entries.isEmpty() && parse_errors.isEmpty()) {
                    EMPTY
                } else {
                    MultiLineMathExpr(entries, parse_errors, symbols, assignments)
                }
            }
        }

        class Entry(val line_index: Int, val offset: Int, val parsed: MathExpr.ParsedLine) {
            var last_result: Int = 0
        }

        constructor() : this(emptyList(), emptyList(), emptySet(), emptySet())

        constructor(
            lines: List<Entry>,
            parse_error_entries: List<Entry>,
            syms: Set<String>,
            assigns: Set<String>,
        ) {
            this.entries = lines
            this.invalid_entries = parse_error_entries
            this.symbols = syms.toMutableSet()
            this.assignments = assigns
        }

        val entries: List<Entry>
        val invalid_entries: List<Entry>
        val symbols: MutableSet<String>
        val assignments: Set<String>

        fun isEmpty(): Boolean = entries.isEmpty()

        fun recalculate(
            mem: MutableMap<String, Int>,
            assignment_post_processor: (Entry, Map<String, Int>) -> Int,
        ): Map<String, Int> {
            val assigned: MutableMap<String, Int> = HashMap()
            for (entry in entries) {
                entry.last_result = entry.parsed.expression.calc(mem)
                if (entry.parsed.assignment_symbol.isNotEmpty()) {
                    assigned[entry.parsed.assignment_symbol] = assignment_post_processor(entry, mem)
                }
            }
            return assigned
        }
    }

    class MathExpr {

        enum class ExprType {
            VOID,
            CONST,
            VARREF,
            FUNC,
            NEG,
            NOT,
            MPY,
            DIV,
            MOD,
            ADD,
            SUB,
            AND,
            OR,
            XOR,
            NEQ,
            EQ,
            LE,
            GE,
            LT,
            GT,
            ASSIGN,
        }

        open class Expr protected constructor(val type: ExprType, val name: String) {
            companion object {
                @JvmField val EMPTY = Expr(ExprType.VOID, "<EMPTY>")

                const val bool_true: Int = 15

                const val bool_false: Int = 0

                @JvmStatic fun assignment_sanitize(x: Int): Int = x
            }

            open fun calc(mem: Map<String, Int>): Int = 0

            override fun toString(): String = "{VOID}"
        }

        abstract class ExprOp(type: ExprType, args: List<Expr>) : Expr(type, type.toString()) {
            val arguments: List<Expr> = args

            abstract override fun calc(mem: Map<String, Int>): Int

            override fun toString(): String =
                name + "{" + arguments.joinToString(",") { it.toString() } + "}"
        }

        class ExprConst(private val value: Int) : Expr(ExprType.CONST, "<CONST>") {
            override fun calc(mem: Map<String, Int>): Int = value

            override fun toString(): String = "CONST{$value}"
        }

        class ExprVarRef(ref: String) : Expr(ExprType.VARREF, ref) {
            override fun calc(mem: Map<String, Int>): Int = mem.getOrDefault(name, 0)

            override fun toString(): String = "SYM{'$name'}"
        }

        class ExprFunc(
            name: String,
            nargs: Int,
            private val func: (Array<Expr>, MutableMap<String, Int>) -> Int,
            args: List<Expr>,
        ) : Expr(ExprType.FUNC, name) {
            val arguments: List<Expr> = args
            val num_arguments: Int = nargs

            init {
                if (nargs >= 0 && nargs != args.size) {
                    error("invalid_number_of_arguments")
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun calc(mem: Map<String, Int>): Int =
                func(arguments.toTypedArray(), mem as MutableMap<String, Int>)

            fun nargs(): Int = num_arguments

            override fun toString(): String =
                "FN{'$name'(${arguments.joinToString(",") { it.toString() }})}"
        }

        class ExprNeg(args: List<Expr>) : ExprOp(ExprType.NEG, args) {
            override fun calc(mem: Map<String, Int>): Int = -arguments[0].calc(mem)
        }

        class ExprNot(args: List<Expr>) : ExprOp(ExprType.NOT, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) == 0) bool_true else bool_false
        }

        class ExprMpy(args: List<Expr>) : ExprOp(ExprType.MPY, args) {
            override fun calc(mem: Map<String, Int>): Int =
                arguments[0].calc(mem) * arguments[1].calc(mem)
        }

        class ExprDiv(args: List<Expr>) : ExprOp(ExprType.DIV, args) {
            override fun calc(mem: Map<String, Int>): Int {
                val b = arguments[1].calc(mem)
                if (b == 0) throw ArithmeticException("division by zero")
                return arguments[0].calc(mem) / b
            }
        }

        class ExprMod(args: List<Expr>) : ExprOp(ExprType.MOD, args) {
            override fun calc(mem: Map<String, Int>): Int {
                val b = arguments[1].calc(mem)
                if (b == 0) throw ArithmeticException("modulo by zero")
                return arguments[0].calc(mem) % b
            }
        }

        class ExprAdd(args: List<Expr>) : ExprOp(ExprType.ADD, args) {
            override fun calc(mem: Map<String, Int>): Int =
                arguments[0].calc(mem) + arguments[1].calc(mem)
        }

        class ExprSub(args: List<Expr>) : ExprOp(ExprType.SUB, args) {
            override fun calc(mem: Map<String, Int>): Int =
                arguments[0].calc(mem) - arguments[1].calc(mem)
        }

        class ExprAnd(args: List<Expr>) : ExprOp(ExprType.AND, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) > 0 && arguments[1].calc(mem) > 0) {
                    bool_true
                } else {
                    bool_false
                }
        }

        class ExprOr(args: List<Expr>) : ExprOp(ExprType.OR, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) > 0 || arguments[1].calc(mem) > 0) {
                    bool_true
                } else {
                    bool_false
                }
        }

        class ExprXor(args: List<Expr>) : ExprOp(ExprType.XOR, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if ((arguments[0].calc(mem) > 0) xor (arguments[1].calc(mem) > 0)) {
                    bool_true
                } else {
                    bool_false
                }
        }

        class ExprNeq(args: List<Expr>) : ExprOp(ExprType.NEQ, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) != arguments[1].calc(mem)) bool_true else bool_false
        }

        class ExprEq(args: List<Expr>) : ExprOp(ExprType.EQ, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) == arguments[1].calc(mem)) bool_true else bool_false
        }

        class ExprGe(args: List<Expr>) : ExprOp(ExprType.GE, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) >= arguments[1].calc(mem)) bool_true else bool_false
        }

        class ExprLe(args: List<Expr>) : ExprOp(ExprType.LE, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) <= arguments[1].calc(mem)) bool_true else bool_false
        }

        class ExprGt(args: List<Expr>) : ExprOp(ExprType.GT, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) > arguments[1].calc(mem)) bool_true else bool_false
        }

        class ExprLt(args: List<Expr>) : ExprOp(ExprType.LT, args) {
            override fun calc(mem: Map<String, Int>): Int =
                if (arguments[0].calc(mem) < arguments[1].calc(mem)) bool_true else bool_false
        }

        class ExprAssign(ref: String, private val value: Expr) : Expr(ExprType.ASSIGN, ref) {
            @Suppress("UNCHECKED_CAST")
            override fun calc(mem: Map<String, Int>): Int {
                val res = value.calc(mem)
                if (name.isNotEmpty()) (mem as MutableMap<String, Int>)[name] = res
                return res
            }

            override fun toString(): String = "ASSIGN{'$name',$value}"
        }

        class ExprFuncDef(
            val name: String,
            val nargs: Int,
            val func: (Array<Expr>, MutableMap<String, Int>) -> Int,
        )

        class ParsedLine
        private constructor(
            lineStr: String,
            defaultAssignmentVariable: String,
            funcDefs: Collection<ExprFuncDef>,
        ) {

            val line: String = lineStr
            val symbols: MutableSet<String> = HashSet()
            val functions: MutableMap<String, ExprFuncDef> = HashMap()
            val expression: Expr
            val assignment_symbol: String
            var error: String
            var pe: Int = -1
            var c: Char = ' '

            init {
                funcDefs.forEach { def -> functions[def.name.lowercase()] = def }
                var exp: Expr = Expr.EMPTY
                var err = ""
                var assign = ""
                if (!lineStr.matches(Regex("^[\\s]*#.*"))) {
                    try {
                        adv()
                        exp = expr_assign(defaultAssignmentVariable)
                        if (pe < lineStr.length) {
                            exp = Expr.EMPTY
                            err = "invalid_character"
                        } else {
                            assign = exp.name.lowercase()
                        }
                    } catch (_e: Exception) {
                        err = "parse_error"
                    }
                }
                expression = exp
                assignment_symbol = assign
                error = err
            }

            override fun toString(): String =
                "ParsedLine{" +
                    " line:\"" +
                    line.replace("\"", "\\\"") +
                    "\"," +
                    (if (error.isEmpty()) {
                        ""
                    } else {
                        " error:\"" + error.replace("\"", "\\\"") + " @pos=$pe\","
                    }) +
                    " sym:\"" +
                    symbols.joinToString(",") +
                    "\"," +
                    " expr:\"$expression\" " +
                    "}"

            companion object {
                @JvmStatic fun of(line: String): ParsedLine = of(line, "")

                @JvmStatic
                fun of(line: String, defaultAssignmentVariable: String): ParsedLine =
                    of(line, defaultAssignmentVariable, emptyList())

                @JvmStatic
                fun of(
                    line: String,
                    defaultAssignmentVariable: String,
                    functions: Collection<ExprFuncDef>,
                ): ParsedLine = ParsedLine(line, defaultAssignmentVariable, functions)
            }

            private fun adv() {
                if (++pe >= line.length) {
                    c = ' '
                } else if (c == '\n' || c == '\r' || c == '#') {
                    pe = line.length
                    c = ' '
                } else {
                    val ci = line[pe].code
                    if (ci > 127) {
                        error("invalid_character")
                    } else {
                        c = line[pe].lowercaseChar()
                    }
                }
            }

            private fun adv(match: Char): Boolean {
                while (c == ' ' || c == '\t' || c == '#') adv()
                if (c != match) return false
                adv()
                return true
            }

            private fun adv(match: String): Boolean {
                while (c == ' ' || c == '\t' || c == '#') adv()
                if (
                    !line.regionMatches(
                        ignoreCase = true,
                        thisOffset = pe,
                        other = match,
                        otherOffset = 0,
                        length = match.length,
                    )
                ) {
                    return false
                }
                pe += match.length - 1
                adv()
                return true
            }

            private fun expr_assign(default_assignment_variable: String): Expr {
                var ref = default_assignment_variable
                if (line.matches(Regex("^[\\s]*[a-zA-Z][\\w.]*[\\s]*[=][^=].*"))) {
                    ref = const_literal()
                    if (!adv('=')) error("expected_assignment")
                    if (functions.containsKey(ref.lowercase())) {
                        error("symbol_readonly")
                    }
                }
                symbols.add(ref)
                return ExprAssign(ref, expr())
            }

            private fun expr(): Expr = expr_or()

            private fun expr_or(): Expr {
                var x = expr_xor()
                while (true) {
                    x =
                        if (adv("or")) {
                            ExprOr(listOf(x, expr_xor()))
                        } else if (adv("||")) {
                            ExprOr(listOf(x, expr_xor()))
                        } else if (adv('|')) {
                            ExprOr(listOf(x, expr_xor()))
                        } else {
                            return x
                        }
                }
            }

            private fun expr_xor(): Expr {
                var x = expr_and()
                while (true) {
                    x =
                        if (adv("xor")) {
                            ExprXor(listOf(x, expr_and()))
                        } else if (adv('^')) {
                            ExprXor(listOf(x, expr_and()))
                        } else {
                            return x
                        }
                }
            }

            private fun expr_and(): Expr {
                var x = expr_rel()
                while (true) {
                    x =
                        if (adv("and")) {
                            ExprAnd(listOf(x, expr_rel()))
                        } else if (adv("&&")) {
                            ExprAnd(listOf(x, expr_rel()))
                        } else if (adv('&')) {
                            ExprAnd(listOf(x, expr_rel()))
                        } else {
                            return x
                        }
                }
            }

            private fun expr_rel(): Expr {
                var x = arith_add()
                while (true) {
                    if (adv("!=")) x = ExprNeq(listOf(x, arith_add()))
                    if (adv("<>")) x = ExprNeq(listOf(x, arith_add()))
                    if (adv("==")) x = ExprEq(listOf(x, arith_add()))
                    if (adv(">=")) x = ExprGe(listOf(x, arith_add()))
                    if (adv("<=")) x = ExprLe(listOf(x, arith_add()))
                    if (adv('>')) x = ExprGt(listOf(x, arith_add()))
                    if (adv('<')) x = ExprLt(listOf(x, arith_add())) else return x
                }
            }

            private fun arith_add(): Expr {
                var x = arith_mpy()
                while (true) {
                    x =
                        if (adv('+')) {
                            ExprAdd(listOf(x, arith_mpy()))
                        } else if (adv('-')) {
                            ExprSub(listOf(x, arith_mpy()))
                        } else {
                            return x
                        }
                }
            }

            private fun arith_mpy(): Expr {
                var x = arith_fact()
                while (true) {
                    x =
                        if (adv('*')) {
                            ExprMpy(listOf(x, arith_fact()))
                        } else if (adv('/')) {
                            ExprDiv(listOf(x, arith_fact()))
                        } else if (adv('%')) {
                            ExprMod(listOf(x, arith_fact()))
                        } else {
                            return x
                        }
                }
            }

            private fun arith_fact(): Expr {
                if (adv('+')) adv()
                if (adv('-') && !adv('-')) return ExprNeg(listOf(arith_fact()))
                if (adv('!')) return ExprNot(listOf(arith_fact()))
                if (adv('(')) {
                    val e = expr()
                    if (!adv(')')) error("missing_closing_parenthesis")
                    return e
                } else if (c in '0'..'9') {
                    return ExprConst(const_number())
                } else if (c in 'a'..'z') {
                    val sym = const_literal()
                    if (sym == "not") {
                        return ExprNot(listOf(expr()))
                    } else if (adv('(')) {
                        val args: MutableList<Expr> = ArrayList()
                        if (!adv(')')) {
                            args.add(expr())
                            while (adv(',')) args.add(expr())
                            if (!adv(')')) {
                                error("missing_closing_function_parenthesis")
                            }
                        }
                        val fn = functions[sym] ?: error("unknown_function")
                        return ExprFunc(fn.name, fn.nargs, fn.func, args)
                    } else {
                        if (functions.containsKey(sym)) {
                            error("missing_function_arguments")
                        }
                        symbols.add(sym)
                        return ExprVarRef(sym)
                    }
                }
                error("unexpected_character")
            }

            private fun isIdentChar(): Boolean =
                c in 'a'..'z' || c in '0'..'9' || c == '.' || c == '_'

            private fun const_literal(): String {
                if (c < 'a' || c > 'z') return ""
                val p0 = pe
                while (isIdentChar()) adv()
                return line.substring(p0, pe).lowercase()
            }

            private fun const_number(): Int {
                val p0 = pe
                while (c in '0'..'9') adv()
                return line.substring(p0, pe).toInt()
            }
        }
    }
}
