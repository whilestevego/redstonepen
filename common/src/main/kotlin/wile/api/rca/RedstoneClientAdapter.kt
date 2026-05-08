package wile.api.rca

interface RedstoneClientAdapter {

    /**
     * Frequently invoked tick function, transferring cached
     * RAM data from and to the adapter application on the
     * system side.
     *
     * - The method shall manage reading and writing to this
     *    channel, but also opening, closing, and error/exception
     *    handling.
     *
     * - I/O should be implemented nonblocking.
     *
     * - Inputs are updated, and if changed, marked as changed.
     *
     * - If the outputs are uninitialized or marked as changed,
     *   their value shall be written and the change-marker
     *   unset.
     */
    fun tick()

    /**
     * Returns true if the used IPC channels are successfully
     * opened at adapter side of this application. This may not
     * guarantee that a bidirectional communication is possible
     * (depending on the IPC method used), but indicate to the
     * tick() method to close-retry-open, as well as indicate
     * a "issue" marker to the user.
     * Note: A serial port, or a pipe/fifo, or a shared
     *       memory, or UDP socket may be opened successfully,
     *       but is not possible to determine if someone is on
     *       the other side.
     */
    fun isOpen(): Boolean

    /**
     * Returns the RAM stored/cached input data word,
     * holding 16 4bit channels.
     * @return The cached input data word.
     */
    fun getInputs(): Long

    /**
     * Sets the RAM stored/cached input data word,
     * and marks the input word as changed.
     * @param value The new input data word.
     */
    fun setInputs(value: Long)

    /**
     * Returns true if the input data word is marked
     * as changed.
     * @return True if the input is marked changed, false otherwise.
     */
    fun isInputsChanged(): Boolean

    /**
     * Sets/resets the input-changed marker.
     * @param changed New value of the changed-marker.
     */
    fun setInputsChanged(changed: Boolean)

    /**
     * Returns the RAM stored/cached output data word,
     * holding 16 4bit channels.
     * @return The cached output word value.
     */
    fun getOutputs(): Long

    /**
     * Sets the RAM stored/cached output data word,
     * and marks the input word as changed.
     * @param value The new output word value.
     */
    fun setOutputs(value: Long)

    /**
     * Returns true if the output data word is marked
     * as changed.
     * @return True if the changed-marker is set, false otherwise.
     */
    fun isOutputsChanged(): Boolean

    /**
     * Sets/resets the output-changed marker.
     */
    fun setOutputsChanged(changed: Boolean)

    /** RAM cached channel access: Number of available channels. */
    fun numChannels(): Int = 16

    /**
     * RAM cached channel access: Input value for the given channel.
     * @param channel Channel index 0..15
     * @return Input channel value 0..15
     */
    fun getInputChannel(channel: Int): Int = ((getInputs() shr (4 * channel)) and 0xf).toInt()

    /**
     * RAM cached channel access: Output value for the given channel.
     * @param channel Channel index 0..15
     * @return Output channel value 0..15
     */
    fun getOutputChannel(channel: Int): Int = ((getOutputs() shr (4 * channel)) and 0xf).toInt()

    /**
     * RAM cached channel access: Input value assignment for the given channel.
     * @param channel Channel index 0..15
     * @param value Input channel value 0..15
     */
    fun setInputChannel(channel: Int, value: Int) =
        setInputs((getInputs() and (0xfL.inv() shl (4 * channel))) or ((value and 0xf).toLong() shl (4 * channel)))

    /**
     * RAM cached channel access: Output value assignment for the given channel.
     * @param channel Channel index 0..15
     * @param value Output channel value 0..15
     */
    fun setOutputChannel(channel: Int, value: Int) =
        setOutputs((getOutputs() and (0xfL.inv() shl (4 * channel))) or ((value and 0xf).toLong() shl (4 * channel)))
}
