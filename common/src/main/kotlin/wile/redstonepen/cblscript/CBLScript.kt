package wile.redstonepen.cblscript

object CBLScript {

    val STANDARD_SYMBOL_SUFFIXES: Set<String> =
        setOf("", ".re", ".fe", ".co", ".co.re", ".co.fe", ".pt", ".et")

    /**
     * Compiles [code] into an immutable [Program].
     *
     * @param code Source text; lines separated by `\n` or `\r\n`.
     * @param policy Signal domain configuration. Defaults to generic integers (true=1, false=0).
     *   Pass [SignalPolicy.REDSTONE] for Control Box use.
     * @param functions Additional named functions available in [code]. The standard built-ins
     *   (ton1–ton5, cnt1–cnt5, etc.) are always included; caller functions win on name collision.
     * @param validSymbolSuffixes Dot-suffixes allowed on symbol names. An unknown suffix is treated
     *   as a parse error. Pass `null` to disable suffix validation.
     * @return A [Program]; check [Program.isValid] before evaluating.
     */
    fun compile(
        code: String,
        policy: SignalPolicy = SignalPolicy.DEFAULT,
        functions: List<FunctionDef> = emptyList(),
        validSymbolSuffixes: Set<String>? = STANDARD_SYMBOL_SUFFIXES,
        outputClamp: (symbol: String, value: Int) -> Int = { _, v -> v },
    ): Program {
        val trimmed = code.replace(Regex("[\\r\\n\\s]+$"), "")
        if (trimmed.trim().isEmpty()) return Program.EMPTY

        val allFunctions =
            buildMap<String, FunctionDef> {
                for (f in standardFunctions(policy)) put(f.name.lowercase(), f)
                for (f in functions) put(f.name.lowercase(), f)
            }

        val parser = Parser(validSymbolSuffixes, allFunctions, policy)
        val lines = trimmed.split(Regex("[\\r]?[\\n]"))

        val entries = mutableListOf<CompiledLine>()
        val errors = mutableMapOf<Int, String>()
        val referencedSymbols = mutableSetOf<String>()
        val assignedSymbols = mutableSetOf<String>()

        var offset = 0
        for (line in lines) {
            if (line.trim().isNotEmpty()) {
                val parsed = parser.parseLine(line)
                if (parsed.error.isNotEmpty()) {
                    errors[offset + parsed.errorPos.coerceAtLeast(0)] = parsed.error
                } else {
                    val invalidSuffix = validSymbolSuffixes?.let { suffixes ->
                        parsed.symbols.firstOrNull { sym ->
                            val dot = sym.indexOf('.')
                            if (dot < 0) false
                            else if (dot >= sym.length - 1) true
                            else !suffixes.contains(sym.substring(dot))
                        }
                    }
                    if (invalidSuffix != null) {
                        val errPos =
                            line.indexOf(invalidSuffix).let { if (it < 0) line.length - 1 else it }
                        errors[offset + errPos] = "parse_error"
                    } else if (parsed.assignmentSymbol.isNotEmpty()) {
                        referencedSymbols.addAll(parsed.symbols)
                        assignedSymbols.add(parsed.assignmentSymbol)
                        entries.add(CompiledLine(parsed.assignmentSymbol, parsed.expression))
                    }
                }
            }
            offset += 1 + line.length
        }

        return if (entries.isEmpty() && errors.isEmpty()) {
            Program.EMPTY
        } else {
            Program(entries, errors, referencedSymbols, assignedSymbols, outputClamp)
        }
    }
}
