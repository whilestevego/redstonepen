package wile.redstonepen.cblscript

internal class ParsedLine(
    val assignmentSymbol: String,
    val expression: Expression,
    val symbols: Set<String>,
    val error: String,
    val errorPos: Int,
)

internal class Parser(
    private val validSymbolSuffixes: Set<String>?,
    private val functions: Map<String, FunctionDef>,
    private val policy: SignalPolicy = SignalPolicy.DEFAULT,
) {
    fun parseLine(lineStr: String, defaultAssignmentVariable: String = ""): ParsedLine {
        return LineParser(
                lineStr,
                defaultAssignmentVariable,
                functions,
                validSymbolSuffixes,
                policy,
            )
            .parse()
    }
}

private class LineParser(
    private val line: String,
    private val defaultAssignmentVariable: String,
    private val functions: Map<String, FunctionDef>,
    private val validSymbolSuffixes: Set<String>?,
    private val policy: SignalPolicy = SignalPolicy.DEFAULT,
) {
    private val tv = policy.trueValue
    private val fv = policy.falseValue
    private val symbols: MutableSet<String> = HashSet()
    private var pos: Int = -1
    private var c: Char = ' '

    fun parse(): ParsedLine {
        if (line.matches(Regex("^[\\s]*#.*"))) {
            return ParsedLine("", emptyExpression(), emptySet(), "", -1)
        }
        return try {
            adv()
            val expr = parseAssignment()
            if (pos < line.length) {
                ParsedLine("", emptyExpression(), emptySet(), "invalid_character", pos)
            } else {
                val assignSym =
                    if (expr is AssignExpression) expr.assignmentName().lowercase() else ""
                ParsedLine(assignSym, expr, symbols.toSet(), "", -1)
            }
        } catch (_: Exception) {
            ParsedLine("", emptyExpression(), emptySet(), "parse_error", pos)
        }
    }

    private fun emptyExpression(): Expression = ConstExpression(0)

    private fun adv() {
        if (++pos >= line.length) {
            c = ' '
        } else if (c == '\n' || c == '\r' || c == '#') {
            pos = line.length
            c = ' '
        } else {
            val ci = line[pos].code
            if (ci > 127) error("invalid_character")
            c = line[pos].lowercaseChar()
        }
    }

    private fun adv(match: Char): Boolean {
        while (pos < line.length && (c == ' ' || c == '\t' || c == '#')) adv()
        if (c != match) return false
        adv()
        return true
    }

    private fun adv(match: String): Boolean {
        while (pos < line.length && (c == ' ' || c == '\t' || c == '#')) adv()
        if (
            !line.regionMatches(
                ignoreCase = true,
                thisOffset = pos,
                other = match,
                otherOffset = 0,
                length = match.length,
            )
        ) {
            return false
        }
        pos += match.length - 1
        adv()
        return true
    }

    private fun parseAssignment(): Expression {
        var ref = defaultAssignmentVariable
        if (line.matches(Regex("^[\\s]*[a-zA-Z][\\w.]*[\\s]*[=][^=].*"))) {
            ref = readIdentifier()
            if (!adv('=')) error("expected_assignment")
            if (functions.containsKey(ref.lowercase())) error("symbol_readonly")
        }
        symbols.add(ref)
        return AssignExpression(ref, parseExpr())
    }

    private fun parseExpr(): Expression = parseOr()

    private fun parseOr(): Expression {
        var x = parseXor()
        while (true) {
            x =
                when {
                    adv("or") -> OrExpression(x, parseXor(), tv, fv)
                    adv("||") -> OrExpression(x, parseXor(), tv, fv)
                    adv('|') -> OrExpression(x, parseXor(), tv, fv)
                    else -> return x
                }
        }
    }

    private fun parseXor(): Expression {
        var x = parseAnd()
        while (true) {
            x =
                when {
                    adv("xor") -> XorExpression(x, parseAnd(), tv, fv)
                    adv('^') -> XorExpression(x, parseAnd(), tv, fv)
                    else -> return x
                }
        }
    }

    private fun parseAnd(): Expression {
        var x = parseRel()
        while (true) {
            x =
                when {
                    adv("and") -> AndExpression(x, parseRel(), tv, fv)
                    adv("&&") -> AndExpression(x, parseRel(), tv, fv)
                    adv('&') -> AndExpression(x, parseRel(), tv, fv)
                    else -> return x
                }
        }
    }

    private fun parseRel(): Expression {
        var x = parseAdd()
        while (true) {
            if (adv("!=")) x = NeqExpression(x, parseAdd(), tv, fv)
            else if (adv("<>")) x = NeqExpression(x, parseAdd(), tv, fv)
            else if (adv("==")) x = EqExpression(x, parseAdd(), tv, fv)
            else if (adv(">=")) x = GeExpression(x, parseAdd(), tv, fv)
            else if (adv("<=")) x = LeExpression(x, parseAdd(), tv, fv)
            else if (adv('>')) x = GtExpression(x, parseAdd(), tv, fv)
            else if (adv('<')) x = LtExpression(x, parseAdd(), tv, fv) else return x
        }
    }

    private fun parseAdd(): Expression {
        var x = parseMul()
        while (true) {
            x =
                when {
                    adv('+') -> AddExpression(x, parseMul())
                    adv('-') -> SubExpression(x, parseMul())
                    else -> return x
                }
        }
    }

    private fun parseMul(): Expression {
        var x = parseFactor()
        while (true) {
            x =
                when {
                    adv('*') -> MulExpression(x, parseFactor())
                    adv('/') -> DivExpression(x, parseFactor())
                    adv('%') -> ModExpression(x, parseFactor())
                    else -> return x
                }
        }
    }

    private fun parseFactor(): Expression {
        if (adv('+')) adv()
        if (adv('-') && !adv('-')) return NegExpression(parseFactor())
        if (adv('!')) return NotExpression(parseFactor(), tv, fv)
        if (adv('(')) {
            val e = parseExpr()
            if (!adv(')')) error("missing_closing_parenthesis")
            return e
        } else if (c in '0'..'9') {
            return ConstExpression(readNumber())
        } else if (c in 'a'..'z') {
            val sym = readIdentifier()
            if (sym == "not") return NotExpression(parseExpr(), tv, fv)
            if (adv('(')) {
                val args = mutableListOf<Expression>()
                if (!adv(')')) {
                    args.add(parseExpr())
                    while (adv(',')) args.add(parseExpr())
                    if (!adv(')')) error("missing_closing_function_parenthesis")
                }
                val fn = functions[sym] ?: error("unknown_function")
                return FuncExpression(fn.name, fn.arity, fn.impl, args)
            } else {
                if (functions.containsKey(sym)) error("missing_function_arguments")
                symbols.add(sym)
                return VarRefExpression(sym)
            }
        }
        error("unexpected_character")
    }

    private fun isIdentChar(): Boolean = c in 'a'..'z' || c in '0'..'9' || c == '.' || c == '_'

    private fun readIdentifier(): String {
        if (c < 'a' || c > 'z') return ""
        val p0 = pos
        while (isIdentChar()) adv()
        return line.substring(p0, pos).lowercase()
    }

    private fun readNumber(): Int {
        val p0 = pos
        while (c in '0'..'9') adv()
        return line.substring(p0, pos).toInt()
    }
}
