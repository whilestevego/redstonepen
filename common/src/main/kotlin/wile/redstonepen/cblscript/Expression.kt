package wile.redstonepen.cblscript

/**
 * A node in a CBLScript expression tree.
 *
 * Expressions are immutable after construction. The [calc] method is the sole evaluation entry
 * point; implementations may read from [mem] (variable references, built-in function state) and —
 * for assignment and function nodes — write back to it as a side effect.
 */
abstract class Expression {
    /**
     * Evaluates this node and returns its integer result.
     *
     * @param mem Symbol table for the current tick. Reads resolve missing keys to 0. Stateful nodes
     *   ([AssignExpression], [FuncExpression]) write their results back here.
     */
    abstract fun calc(mem: Map<String, Int>): Int
}

internal class ConstExpression(private val value: Int) : Expression() {
    override fun calc(mem: Map<String, Int>): Int = value
}

/** Reads a named symbol from [mem], returning 0 if the symbol is not yet set. */
internal class VarRefExpression(private val name: String) : Expression() {
    override fun calc(mem: Map<String, Int>): Int = mem.getOrDefault(name, 0)
}

/**
 * Dispatches to a named built-in function.
 *
 * [func] receives a mutable view of [mem] so timer and counter implementations can persist their
 * internal state (clock reference, elapsed ticks, count) across ticks without requiring a separate
 * state store.
 */
internal class FuncExpression(
    private val name: String,
    private val arity: Int,
    private val func: (Array<Expression>, MutableMap<String, Int>) -> Int,
    private val args: List<Expression>,
) : Expression() {
    init {
        if (arity >= 0 && arity != args.size) error("invalid_number_of_arguments")
    }

    @Suppress("UNCHECKED_CAST")
    override fun calc(mem: Map<String, Int>): Int =
        func(args.toTypedArray(), mem as MutableMap<String, Int>)
}

/**
 * Evaluates [value] and writes the result to [mem] under [name] as a side effect, mirroring how
 * assignments work in the source language.
 */
internal class AssignExpression(private val name: String, private val value: Expression) :
    Expression() {
    fun assignmentName(): String = name

    @Suppress("UNCHECKED_CAST")
    override fun calc(mem: Map<String, Int>): Int {
        val res = value.calc(mem)
        if (name.isNotEmpty()) (mem as MutableMap<String, Int>)[name] = res
        return res
    }
}

internal class NegExpression(private val operand: Expression) : Expression() {
    override fun calc(mem: Map<String, Int>): Int = -operand.calc(mem)
}

/**
 * Logical NOT: returns [trueValue] when [operand] is ≤ 0, [falseValue] otherwise.
 *
 * The `<= 0` threshold matches the falsiness convention used by [AndExpression] and [OrExpression]
 * (`> 0` is truthy), so De Morgan's laws hold for arbitrary integer inputs.
 *
 * @param trueValue Value to return for a true result; comes from [SignalPolicy].
 * @param falseValue Value to return for a false result; comes from [SignalPolicy].
 */
internal class NotExpression(
    private val operand: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (operand.calc(mem) <= 0) trueValue else falseValue
}

internal class MulExpression(private val left: Expression, private val right: Expression) :
    Expression() {
    override fun calc(mem: Map<String, Int>): Int = left.calc(mem) * right.calc(mem)
}

internal class DivExpression(private val left: Expression, private val right: Expression) :
    Expression() {
    override fun calc(mem: Map<String, Int>): Int {
        val b = right.calc(mem)
        if (b == 0) throw ArithmeticException("division by zero")
        return left.calc(mem) / b
    }
}

internal class ModExpression(private val left: Expression, private val right: Expression) :
    Expression() {
    override fun calc(mem: Map<String, Int>): Int {
        val b = right.calc(mem)
        if (b == 0) throw ArithmeticException("modulo by zero")
        return left.calc(mem) % b
    }
}

internal class AddExpression(private val left: Expression, private val right: Expression) :
    Expression() {
    override fun calc(mem: Map<String, Int>): Int = left.calc(mem) + right.calc(mem)
}

internal class SubExpression(private val left: Expression, private val right: Expression) :
    Expression() {
    override fun calc(mem: Map<String, Int>): Int = left.calc(mem) - right.calc(mem)
}

/**
 * Logical AND: true (`> 0`) on both sides yields [trueValue]; otherwise [falseValue].
 *
 * @param trueValue Value to return for a true result; comes from [SignalPolicy].
 * @param falseValue Value to return for a false result; comes from [SignalPolicy].
 */
internal class AndExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) > 0 && right.calc(mem) > 0) trueValue else falseValue
}

/**
 * Logical OR: true (`> 0`) on either side yields [trueValue]; otherwise [falseValue].
 *
 * @param trueValue Value to return for a true result; comes from [SignalPolicy].
 * @param falseValue Value to return for a false result; comes from [SignalPolicy].
 */
internal class OrExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) > 0 || right.calc(mem) > 0) trueValue else falseValue
}

/**
 * Logical XOR: exactly one side is true (`> 0`) yields [trueValue]; otherwise [falseValue].
 *
 * @param trueValue Value to return for a true result; comes from [SignalPolicy].
 * @param falseValue Value to return for a false result; comes from [SignalPolicy].
 */
internal class XorExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if ((left.calc(mem) > 0) xor (right.calc(mem) > 0)) trueValue else falseValue
}

internal class NeqExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) != right.calc(mem)) trueValue else falseValue
}

internal class EqExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) == right.calc(mem)) trueValue else falseValue
}

internal class GeExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) >= right.calc(mem)) trueValue else falseValue
}

internal class LeExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) <= right.calc(mem)) trueValue else falseValue
}

internal class GtExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) > right.calc(mem)) trueValue else falseValue
}

internal class LtExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) < right.calc(mem)) trueValue else falseValue
}
