package wile.redstonepen.cblscript

data class FunctionDef(
    val name: String,
    val arity: Int,
    val impl: (args: Array<Expression>, state: MutableMap<String, Int>) -> Int,
)
