package wile.redstonepen.cblscript

data class SignalPolicy(val trueValue: Int, val falseValue: Int) {
    companion object {
        val DEFAULT = SignalPolicy(trueValue = 1, falseValue = 0)
        val REDSTONE = SignalPolicy(trueValue = 15, falseValue = 0)
    }
}
