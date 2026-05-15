package wile.redstonepen.blocks.controlbox

import wile.redstonepen.cblscript.CBLScript
import wile.redstonepen.cblscript.Program
import wile.redstonepen.cblscript.SignalPolicy

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

    private val state_: MutableMap<String, Int> = HashMap()
    private var program_: Program = Program.EMPTY
    private var code_: String = ""

    fun valid(): Boolean = program_.isValid

    fun usesSymbol(s: String): Boolean = program_.referencedSymbols.contains(s)

    fun errors(): Map<Int, String> = program_.errors

    fun symbol(key: String, value: Int) {
        state_[key.lowercase()] = value
    }

    fun symbol(key: String): Int = state_.getOrDefault(key.lowercase(), 0)

    fun symbols(): MutableMap<String, Int> = state_

    fun clearSymbols() {
        state_.clear()
    }

    fun code(): String = code_

    fun code(new_code: String): Boolean {
        if (code_ == new_code && program_ !== Program.EMPTY) {
            return program_.isValid
        }
        code_ = new_code
        program_ =
            CBLScript.compile(
                code = new_code,
                policy = SignalPolicy.REDSTONE,
                outputClamp = { sym, v ->
                    if (Defs.PORT_NAMES.contains(sym)) v.coerceIn(0, 15) else v
                },
            )
        input_mask = 0
        output_mask = 0
        rca_input_mask = 0
        rca_output_mask = 0
        state_.clear()
        for (i in Defs.PORT_NAMES.indices) {
            val port = Defs.PORT_NAMES[i]
            val bit = 0xf shl (4 * i)
            if (program_.referencedSymbols.any { it == port + ".co.re" || it == port + ".co.fe" })
                state_[port + ".co"] = 0
            when {
                program_.assignedSymbols.contains(port) -> output_mask = output_mask or bit
                CBLScript.STANDARD_SYMBOL_SUFFIXES.any {
                    program_.referencedSymbols.contains(port + it)
                } -> input_mask = input_mask or bit
            }
        }
        for (sym in program_.referencedSymbols) {
            if (!sym.matches(Regex("^d[io][1]?[\\d][\\d]?\$"))) continue
            val ch = sym.substring(2).toInt()
            if (ch > 15) continue
            if (sym[1] == 'i') rca_input_mask = rca_input_mask or (0xfL shl (ch * 4))
            else rca_output_mask = rca_output_mask or (0xfL shl (ch * 4))
        }
        // Ports/channels no longer referenced by the new program lose their mask bits; AND the
        // cached data with the updated masks so stale values from old code don't persist.
        rca_input_data = rca_input_data and rca_input_mask
        rca_output_data = rca_output_data and rca_output_mask
        output_data = output_data and output_mask
        input_data = input_data and input_mask
        return program_.isValid
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
        program_.referencedSymbols.forEach { esym ->
            if (!esym.contains(".")) return@forEach
            val isrising = esym.endsWith(".re")
            if (isrising || esym.endsWith(".fe")) {
                val symref = esym.substring(0, esym.length - 3)
                if (!state_.containsKey(symref) && !Defs.PORT_NAMES.contains(symref)) return@forEach
                val symlast = ".$esym.d"
                val q1 = if (state_.getOrDefault(symref, 0) > 0) 15 else 0
                var edge = false
                if (state_.containsKey(symlast)) {
                    val q0 = if (state_.getOrDefault(symlast, 0) > 0) 15 else 0
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
        // Timer built-ins lower .deadline to the ticks until their next edge; the block entity
        // uses the value to schedule early wakeups.  Initialize to 40 (max interval) so timers
        // only need to reduce it — never increase it.
        symbol(".deadline", 40)
        program_.evaluate(state_)
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
}
