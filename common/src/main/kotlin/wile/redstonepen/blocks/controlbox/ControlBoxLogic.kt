package wile.redstonepen.blocks.controlbox

import wile.redstonepen.cblscript.CBLScript
import wile.redstonepen.cblscript.Program
import wile.redstonepen.cblscript.SignalPolicy

private val RCA_SYMBOL_REGEX = Regex("""^d[io][1]?[\d][\d]?$""")

internal class ControlBoxLogic {

    var input_mask = 0x00000000
    var input_data = 0x00000000
    var output_mask = 0x00000000
    var output_data = 0x00000000
    var risingEdgeInterrupts = 0x00000000
    var fallingEdgeInterrupts = 0x00000000
    var rca_input_mask = 0L
    var rca_input_data = 0L
    var rca_output_mask = 0L
    var rca_output_data = 0L

    private val state: MutableMap<String, Int> = HashMap()
    private var program: Program = Program.EMPTY
    private var codeText: String = ""

    fun valid(): Boolean = program.isValid

    fun usesSymbol(s: String): Boolean = program.referencedSymbols.contains(s)

    fun errors(): Map<Int, String> = program.errors

    fun symbol(key: String, value: Int) {
        state[key.lowercase()] = value
    }

    fun symbol(key: String): Int = state.getOrDefault(key.lowercase(), 0)

    fun symbols(): MutableMap<String, Int> = state

    fun clearSymbols() {
        state.clear()
    }

    fun code(): String = codeText

    fun code(new_code: String): Boolean {
        if (codeText == new_code && program !== Program.EMPTY) {
            return program.isValid
        }
        codeText = new_code
        program =
            CBLScript.compile(
                code = new_code,
                policy = SignalPolicy.REDSTONE,
                outputClamp = { sym, v ->
                    if (PortNames.ALL.contains(sym)) v.coerceIn(0, 15) else v
                },
            )
        input_mask = 0
        output_mask = 0
        rca_input_mask = 0
        rca_output_mask = 0
        state.clear()
        for (i in PortNames.ALL.indices) {
            val port = PortNames.ALL[i]
            val bit = 0xf shl (4 * i)
            if (program.referencedSymbols.any { it == "${port}.co.re" || it == "${port}.co.fe" })
                state["${port}.co"] = 0
            when {
                program.assignedSymbols.contains(port) -> output_mask = output_mask or bit
                CBLScript.STANDARD_SYMBOL_SUFFIXES.any {
                    program.referencedSymbols.contains(port + it)
                } -> input_mask = input_mask or bit
            }
        }
        computeRcaMasks()
        clearStaleIoData()
        return program.isValid
    }

    private fun computeRcaMasks() {
        for (sym in program.referencedSymbols) {
            if (!sym.matches(RCA_SYMBOL_REGEX)) continue
            val ch = sym.substring(2).toInt()
            if (ch > 15) continue
            val isInput = sym[1] == 'i'
            if (isInput) rca_input_mask = rca_input_mask or (0xfL shl (ch * 4))
            else rca_output_mask = rca_output_mask or (0xfL shl (ch * 4))
        }
    }

    // Ports/channels no longer referenced by the new program lose their mask bits; AND the
    // cached data with the updated masks so stale values from old code don't persist.
    private fun clearStaleIoData() {
        rca_input_data = rca_input_data and rca_input_mask
        rca_output_data = rca_output_data and rca_output_mask
        output_data = output_data and output_mask
        input_data = input_data and input_mask
    }

    fun tick() {
        for (i in 0 until PortNames.ALL.size) {
            if ((input_mask and (0xf shl (4 * i))) != 0) {
                symbol(PortNames.ALL[i], (input_data shr (4 * i)) and 0xf)
            }
        }
        if (rca_input_mask != 0L) {
            for (i in 0 until 16) {
                if ((rca_input_mask and (0xfL shl (4 * i))) != 0L) {
                    symbol("di$i", ((rca_input_data shr (4 * i)) and 0xfL).toInt())
                }
            }
        }
        program.referencedSymbols.forEach { esym ->
            if (!esym.contains(".")) return@forEach
            val isrising = esym.endsWith(".re")
            if (isrising || esym.endsWith(".fe")) {
                val symref = esym.substring(0, esym.length - 3)
                if (!state.containsKey(symref) && !PortNames.ALL.contains(symref)) return@forEach
                val symlast = ".$esym.d"
                val q1 = if (state.getOrDefault(symref, 0) > 0) 15 else 0
                var edge = false
                if (state.containsKey(symlast)) {
                    val q0 = if (state.getOrDefault(symlast, 0) > 0) 15 else 0
                    edge = if (isrising) (q0 <= 0 && q1 > 0) else (q0 > 0 && q1 <= 0)
                }
                symbol(esym, if (edge) 15 else 0)
                symbol(symlast, q1)
            }
        }
        for (i in 0 until PortNames.ALL.size) {
            val port_mask = 0xf shl (4 * i)
            if ((risingEdgeInterrupts and port_mask) != 0) symbol(PortNames.ALL[i] + ".re", 15)
            if ((fallingEdgeInterrupts and port_mask) != 0) symbol(PortNames.ALL[i] + ".fe", 15)
        }
        risingEdgeInterrupts = 0
        fallingEdgeInterrupts = 0
        // Timer built-ins lower .deadline to the ticks until their next edge; the block entity
        // uses the value to schedule early wakeups.  Initialize to 40 (max interval) so timers
        // only need to reduce it — never increase it.
        symbol(".deadline", 40)
        program.evaluate(state)
        output_data = 0
        for (i in 0 until PortNames.ALL.size) {
            output_data = output_data or ((symbol(PortNames.ALL[i]) and 0xf) shl (4 * i))
        }
        output_data = output_data and output_mask
        if (rca_output_mask != 0L) {
            rca_output_data = 0L
            for (i in 0 until 16) {
                if ((rca_output_mask and (0xfL shl (4 * i))) != 0L) {
                    rca_output_data =
                        rca_output_data or
                            (minOf(15, maxOf(0, symbol("do$i"))).toLong() shl (4 * i))
                }
            }
        }
        rca_output_data = rca_output_data and rca_output_mask
    }
}
