package wile.api.rca

interface RedstoneClientAdapter {
    fun tick()
    fun isOpen(): Boolean
    fun getInputs(): Long
    fun setInputs(value: Long)
    fun isInputsChanged(): Boolean
    fun setInputsChanged(changed: Boolean)
    fun getOutputs(): Long
    fun setOutputs(value: Long)
    fun isOutputsChanged(): Boolean
    fun setOutputsChanged(changed: Boolean)

    fun numChannels(): Int = 16

    fun getInputChannel(channel: Int): Int = ((getInputs() shr (4 * channel)) and 0xf).toInt()

    fun getOutputChannel(channel: Int): Int = ((getOutputs() shr (4 * channel)) and 0xf).toInt()

    fun setInputChannel(channel: Int, value: Int) =
        setInputs((getInputs() and (0xfL.inv() shl (4 * channel))) or ((value and 0xf).toLong() shl (4 * channel)))

    fun setOutputChannel(channel: Int, value: Int) =
        setOutputs((getOutputs() and (0xfL.inv() shl (4 * channel))) or ((value and 0xf).toLong() shl (4 * channel)))
}
