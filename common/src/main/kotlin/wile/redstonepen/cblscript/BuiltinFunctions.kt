package wile.redstonepen.cblscript

private fun timerOn(
    sym: String,
    x: Array<Expression>,
    m: MutableMap<String, Int>,
    policy: SignalPolicy,
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
        return policy.falseValue
    } else if (pt <= 0) {
        return policy.trueValue
    } else {
        val now = m.getOrDefault(".clock", 0)
        var et = m.getOrDefault("$sym.et", 0)
        if (et >= pt) {
            return policy.trueValue
        } else if (et <= 0) {
            m[".$sym.clk"] = now
            m["$sym.et"] = 1
            m["$sym.pt"] = pt
            m[".deadline"] = minOf(m.getOrDefault(".deadline", 20), pt)
            return policy.falseValue
        } else {
            et = minOf(now - m.getOrDefault(".$sym.clk", now), pt)
            m["$sym.et"] = et
            return if (et >= pt) {
                m.remove(".$sym.clk")
                policy.trueValue
            } else {
                m[".deadline"] = minOf(m.getOrDefault(".deadline", 20), pt - et)
                policy.falseValue
            }
        }
    }
}

private fun timerOff(
    sym: String,
    x: Array<Expression>,
    m: MutableMap<String, Int>,
    policy: SignalPolicy,
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
        return policy.trueValue
    } else if (pt <= 0) {
        return policy.trueValue
    } else {
        val now = m.getOrDefault(".clock", 0)
        var et = m.getOrDefault("$sym.et", 0)
        if (et >= pt) {
            return policy.falseValue
        } else if (et <= 0) {
            m[".$sym.clk"] = now
            m["$sym.et"] = 1
            m["$sym.pt"] = pt
            m[".deadline"] = minOf(m.getOrDefault(".deadline", 20), pt)
            return policy.trueValue
        } else {
            et = minOf(now - m.getOrDefault(".$sym.clk", now), pt)
            m["$sym.et"] = et
            return if (et >= pt) {
                m.remove(".$sym.clk")
                policy.falseValue
            } else {
                m[".deadline"] = minOf(m.getOrDefault(".deadline", 20), pt - et)
                policy.trueValue
            }
        }
    }
}

private fun timerPulse(
    sym: String,
    x: Array<Expression>,
    m: MutableMap<String, Int>,
    policy: SignalPolicy,
): Int {
    if (x.size != 2) {
        m.remove(".$sym.clk")
        m.remove("$sym.et")
        m.remove("$sym.pt")
        return 0
    }
    val inp = x[0].calc(m)
    val pt = x[1].calc(m)
    if (pt <= 0) return if (inp > 0) policy.trueValue else policy.falseValue
    var et = m.getOrDefault("$sym.et", 0)
    if (et > 0) {
        if (et >= pt) {
            if (inp <= 0) m["$sym.et"] = 0
            return policy.falseValue
        } else {
            val now = m.getOrDefault(".clock", 0)
            et = minOf(now - m.getOrDefault(".$sym.clk", now), pt)
            m["$sym.et"] = et
            return if (et >= pt) {
                m.remove(".$sym.clk")
                policy.falseValue
            } else {
                m[".deadline"] = minOf(m.getOrDefault(".deadline", 20), pt - et)
                policy.trueValue
            }
        }
    } else if (inp > 0) {
        m[".$sym.clk"] = m.getOrDefault(".clock", 0)
        m["$sym.et"] = 1
        m["$sym.pt"] = pt
        m[".deadline"] = minOf(m.getOrDefault(".deadline", 20), pt)
        return policy.trueValue
    } else {
        return policy.falseValue
    }
}

private fun timerInterval(
    sym: String,
    x: Array<Expression>,
    m: MutableMap<String, Int>,
    policy: SignalPolicy,
): Int {
    if (x.isEmpty() || x.size > 2) {
        m.remove("$sym.clk")
        return 0
    }
    val en = if (x.size < 2) 15 else x[1].calc(m)
    if (en <= 0) {
        m.remove("$sym.clk")
        return 0
    }
    val pt = x[0].calc(m)
    if (pt <= 2) return policy.falseValue
    val now = m.getOrDefault(".clock", 0)
    val clk = m.getOrDefault("$sym.clk", now - pt)
    return if (Math.abs(now - clk) >= pt) {
        m["$sym.clk"] = now
        m[".deadline"] = 1
        policy.trueValue
    } else {
        m[".deadline"] = minOf(m.getOrDefault(".deadline", 20), clk - now + pt)
        policy.falseValue
    }
}

private fun counter(sym: String, x: Array<Expression>, m: MutableMap<String, Int>): Int {
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
        if (x0 > 0 && x1 <= 0) ++q else if (x0 <= 0 && x1 > 0) --q
    }
    q =
        when {
            nargs >= 4 -> q.coerceIn(x[2].calc(m), x[3].calc(m))
            nargs >= 3 -> q.coerceIn(0, x[2].calc(m))
            else -> q.coerceIn(0, 0x7fffffff)
        }
    m[sym] = q
    return q
}

internal fun standardFunctions(policy: SignalPolicy): List<FunctionDef> =
    listOf(
        FunctionDef("inv", 1) { x, m ->
            (policy.trueValue - x[0].calc(m)).coerceIn(0, policy.trueValue)
        },
        FunctionDef("max", -1) { x, m -> x.maxOfOrNull { it.calc(m) } ?: 0 },
        FunctionDef("min", -1) { x, m -> x.minOfOrNull { it.calc(m) } ?: 0 },
        FunctionDef("lim", -1) { x, m ->
            when (x.size) {
                0 -> 0
                1 -> minOf(15, maxOf(0, x[0].calc(m)))
                2 -> minOf(x[1].calc(m), maxOf(0, x[0].calc(m)))
                else -> minOf(x[2].calc(m), maxOf(x[1].calc(m), x[0].calc(m)))
            }
        },
        FunctionDef("if", -1) { x, m ->
            when (x.size) {
                0 -> 0
                1 -> if (x[0].calc(m) > 0) policy.trueValue else policy.falseValue
                2 -> if (x[0].calc(m) > 0) x[1].calc(m) else 0
                else -> if (x[0].calc(m) > 0) x[1].calc(m) else x[2].calc(m)
            }
        },
        FunctionDef("mean", -1) { x, m -> if (x.isEmpty()) 0 else x.sumOf { it.calc(m) } / x.size },
        FunctionDef("rnd", 0) { _, _ -> (Math.random() * 16.0).toInt() },
        FunctionDef("clock", 0) { _, m -> m.getOrDefault(".clock", 0) },
        FunctionDef("time", 0) { _, m -> m.getOrDefault(".time", 0) },
        FunctionDef("tiv1", -1) { x, m -> timerInterval(".tiv1", x, m, policy) },
        FunctionDef("tiv2", -1) { x, m -> timerInterval(".tiv2", x, m, policy) },
        FunctionDef("tiv3", -1) { x, m -> timerInterval(".tiv3", x, m, policy) },
        FunctionDef("cnt1", -1) { x, m -> counter(".cnt1", x, m) },
        FunctionDef("cnt2", -1) { x, m -> counter(".cnt2", x, m) },
        FunctionDef("cnt3", -1) { x, m -> counter(".cnt3", x, m) },
        FunctionDef("cnt4", -1) { x, m -> counter(".cnt4", x, m) },
        FunctionDef("cnt5", -1) { x, m -> counter(".cnt5", x, m) },
        FunctionDef("ton1", 2) { x, m -> timerOn("ton1", x, m, policy) },
        FunctionDef("ton2", 2) { x, m -> timerOn("ton2", x, m, policy) },
        FunctionDef("ton3", 2) { x, m -> timerOn("ton3", x, m, policy) },
        FunctionDef("ton4", 2) { x, m -> timerOn("ton4", x, m, policy) },
        FunctionDef("ton5", 2) { x, m -> timerOn("ton5", x, m, policy) },
        FunctionDef("tof1", 2) { x, m -> timerOff("tof1", x, m, policy) },
        FunctionDef("tof2", 2) { x, m -> timerOff("tof2", x, m, policy) },
        FunctionDef("tof3", 2) { x, m -> timerOff("tof3", x, m, policy) },
        FunctionDef("tof4", 2) { x, m -> timerOff("tof4", x, m, policy) },
        FunctionDef("tof5", 2) { x, m -> timerOff("tof5", x, m, policy) },
        FunctionDef("tp1", 2) { x, m -> timerPulse("tp1", x, m, policy) },
        FunctionDef("tp2", 2) { x, m -> timerPulse("tp2", x, m, policy) },
        FunctionDef("tp3", 2) { x, m -> timerPulse("tp3", x, m, policy) },
        FunctionDef("tp4", 2) { x, m -> timerPulse("tp4", x, m, policy) },
        FunctionDef("tp5", 2) { x, m -> timerPulse("tp5", x, m, policy) },
    )
