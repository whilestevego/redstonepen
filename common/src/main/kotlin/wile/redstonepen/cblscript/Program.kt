package wile.redstonepen.cblscript

/**
 * A single compiled assignment: the symbol being written and the expression tree that produces its
 * value.
 */
internal class CompiledLine(val assignmentSymbol: String, val expression: Expression)

/**
 * A compiled, immutable CBLScript program.
 *
 * Callers own the [MutableMap] state and persist it between [evaluate] calls so that stateful
 * built-ins (counters, timers) survive across ticks.
 *
 * @property errors Map of source-offset → error key for every line that failed to parse. Empty when
 *   the program is valid.
 * @property referencedSymbols Every symbol name read by any expression in the program, including
 *   dot-suffixed variants (e.g. `d.re`). Used by the caller to decide which input ports to sample.
 * @property assignedSymbols Every symbol written by an assignment statement. Used by the caller to
 *   decide which output ports to drive.
 */
class Program
internal constructor(
    private val entries: List<CompiledLine>,
    val errors: Map<Int, String>,
    val referencedSymbols: Set<String>,
    val assignedSymbols: Set<String>,
    private val outputClamp: (symbol: String, value: Int) -> Int,
) {
    /** True when [errors] is empty and the program can be safely evaluated. */
    val isValid: Boolean
        get() = errors.isEmpty()

    /**
     * Evaluates all assignment statements against [state] and returns the newly assigned values.
     *
     * [inputs] are merged into [state] before evaluation begins, allowing the caller to inject
     * per-tick signal values without pre-populating the map. Each assigned symbol's result is run
     * through [outputClamp] before being written back to [state], so clamped values are visible to
     * later lines in the same tick.
     *
     * @param state Persistent symbol table; modified in place. Pass the same map across ticks so
     *   stateful built-ins (timers, counters) retain their internal values.
     * @param inputs Per-tick overrides merged into [state] at the start of this call.
     * @return Only the symbols assigned during this call, after clamping. Does not include
     *   unmodified entries already in [state].
     */
    fun evaluate(
        state: MutableMap<String, Int>,
        inputs: Map<String, Int> = emptyMap(),
    ): Map<String, Int> {
        state.putAll(inputs)
        val assigned = mutableMapOf<String, Int>()
        for (line in entries) {
            val raw = line.expression.calc(state)
            val clamped = outputClamp(line.assignmentSymbol, raw)
            assigned[line.assignmentSymbol] = clamped
            state[line.assignmentSymbol] = clamped
        }
        return assigned
    }

    companion object {
        /**
         * Sentinel returned for blank or comment-only source; always valid, evaluates to nothing.
         */
        val EMPTY = Program(emptyList(), emptyMap(), emptySet(), emptySet()) { _, v -> v }
    }
}
