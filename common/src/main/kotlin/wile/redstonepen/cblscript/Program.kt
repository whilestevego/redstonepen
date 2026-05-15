package wile.redstonepen.cblscript

internal class CompiledLine(val assignmentSymbol: String, val expression: Expression)

class Program
internal constructor(
    private val entries: List<CompiledLine>,
    val errors: Map<Int, String>,
    val referencedSymbols: Set<String>,
    val assignedSymbols: Set<String>,
    private val outputClamp: (symbol: String, value: Int) -> Int,
) {
    val isValid: Boolean
        get() = errors.isEmpty()

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
        val EMPTY = Program(emptyList(), emptyMap(), emptySet(), emptySet()) { _, v -> v }
    }
}
