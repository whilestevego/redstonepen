package wile.redstonepen.cblscript

abstract class Expression {
    abstract fun calc(mem: Map<String, Int>): Int
}

internal class ConstExpression(private val value: Int) : Expression() {
    override fun calc(mem: Map<String, Int>): Int = value
}

internal class VarRefExpression(private val name: String) : Expression() {
    override fun calc(mem: Map<String, Int>): Int = mem.getOrDefault(name, 0)
}

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

internal class AndExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) > 0 && right.calc(mem) > 0) trueValue else falseValue
}

internal class OrExpression(
    private val left: Expression,
    private val right: Expression,
    private val trueValue: Int,
    private val falseValue: Int,
) : Expression() {
    override fun calc(mem: Map<String, Int>): Int =
        if (left.calc(mem) > 0 || right.calc(mem) > 0) trueValue else falseValue
}

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
