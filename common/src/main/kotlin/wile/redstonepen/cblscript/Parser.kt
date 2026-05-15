package wile.redstonepen.cblscript

/**
 * The result of parsing a single source line.
 *
 * A line is either a comment (all fields empty/default), an assignment (`sym = expr`), or an error.
 * The caller inspects [error] first; a non-empty string means parsing failed and only [errorPos] is
 * meaningful.
 *
 * @property assignmentSymbol The symbol being assigned, lowercased. Empty for comment lines and
 *   error lines.
 * @property expression The compiled expression tree for the right-hand side. A no-op constant when
 *   [error] is non-empty.
 * @property symbols Every symbol name referenced anywhere in this line, including the assignment
 *   target. Used by [CBLScript] to build [Program.referencedSymbols].
 * @property error Non-empty error key (e.g. `"parse_error"`, `"unknown_function"`) when the line
 *   could not be parsed; empty string on success.
 * @property errorPos Byte offset within the original line string where the error was detected, or
 *   -1 on success.
 */
internal class ParsedLine(
    val assignmentSymbol: String,
    val expression: Expression,
    val symbols: Set<String>,
    val error: String,
    val errorPos: Int,
)

/**
 * Parses individual lines of CBLScript source into [ParsedLine] trees.
 *
 * One [Parser] instance is reused across all lines of a single compilation unit so that the
 * function map and symbol-suffix set are resolved once and shared.
 *
 * @param validSymbolSuffixes Allowed dot-suffixes for symbol names (e.g. `".re"`, `".fe"`). A
 *   symbol whose suffix is not in this set is treated as a parse error. Pass `null` to disable
 *   suffix validation entirely.
 * @param functions Map of function name → [FunctionDef] for all callable built-ins and
 *   user-supplied functions.
 * @param policy Signal domain configuration; controls which integer values [NotExpression] and the
 *   comparison nodes emit for true and false results.
 */
internal class Parser(
    private val validSymbolSuffixes: Set<String>?,
    private val functions: Map<String, FunctionDef>,
    private val policy: SignalPolicy = SignalPolicy.DEFAULT,
) {
    /**
     * Parses [lineStr] and returns the resulting [ParsedLine].
     *
     * Comment lines (starting with optional whitespace then `#`) return an empty [ParsedLine] with
     * no error. Assignment lines are parsed as `identifier = expression`; bare expressions without
     * an explicit left-hand side use [defaultAssignmentVariable] as the target symbol.
     *
     * @param lineStr A single line of source text without a trailing newline.
     * @param defaultAssignmentVariable Symbol name to assign to when the line has no explicit
     *   `identifier =` prefix.
     */
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

/**
 * Recursive-descent parser for a single CBLScript line.
 *
 * Grammar (simplified):
 * ```
 * line       ::= assignment
 * assignment ::= [identifier '='] expr
 * expr       ::= or
 * or         ::= xor (('or' | '||' | '|') xor)*
 * xor        ::= and (('xor' | '^') and)*
 * and        ::= rel (('and' | '&&' | '&') rel)*
 * rel        ::= add (('==' | '!=' | '<>' | '>=' | '<=' | '>' | '<') add)*
 * add        ::= mul (('+' | '-') mul)*
 * mul        ::= factor (('*' | '/' | '%') factor)*
 * factor     ::= ['+' | '-' | '!'] factor
 *              | '(' expr ')'
 *              | number
 *              | identifier ['(' arglist ')']
 *              | 'not' expr
 * ```
 *
 * The parser advances through [line] one character at a time using [adv]. Whitespace and `#`
 * comments are consumed transparently inside each `adv(Char)` and `adv(String)` overload before the
 * match is attempted, so the grammar rules above do not need to account for spacing.
 *
 * All symbol names collected during parsing are accumulated in [symbols] and surfaced through
 * [ParsedLine.symbols] so the compiler can build the referenced-symbol set without re-walking the
 * expression tree.
 */
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

    /**
     * Advances [pos] by one and updates [c].
     *
     * Sets [c] to `' '` (space) when [pos] reaches or exceeds [line.length], treating end-of-input
     * as trailing whitespace so callers that loop on whitespace terminate naturally.
     */
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

    /**
     * Skips whitespace, then consumes [match] and advances past it.
     *
     * The whitespace skip is a side effect regardless of whether [match] is found. Callers must not
     * rely on [pos] being unchanged when this returns `false`.
     *
     * @return `true` if [match] was the current character and was consumed; `false` otherwise.
     */
    private fun adv(match: Char): Boolean {
        while (pos < line.length && (c == ' ' || c == '\t' || c == '#')) adv()
        if (c != match) return false
        adv()
        return true
    }

    /**
     * Skips whitespace, then consumes [match] (case-insensitive) and advances past it.
     *
     * Like [adv] for characters, the whitespace skip is unconditional.
     *
     * @return `true` if the current position starts with [match] and it was consumed; `false`
     *   otherwise.
     */
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
