package wile.api.rca

import wile.redstonepen.util.Auxiliaries
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

/** File Memory Mapped Redstone Client Adapter implementation. */
class FmmRedstoneClientAdapter {

    /**
     * Memory Mapping auxiliary class.
     * Wraps the common processes of using MappedByteBuffer/RandomAccessFile
     * in java. Under Linux the native `mmap()` functionality will be implicitly
     * used, Windows provides the similar functionality via the `CreateFileMapping()`/
     * `OpenFileMapping()` WINAPI functions (a bit more is needed, but in essence
     * these are the key functions).
     */
    private class FileMemMap(
        private val mapFilePath: String,
        private val isWrite: Boolean,
        private val reopenDelay: Int,
        private val mapSize: Int,
    ) {
        private val mapData: ByteArray = ByteArray(mapSize) { '0'.code.toByte() }
        private var file: RandomAccessFile? = null
        private var channel: FileChannel? = null
        private var buffer: MappedByteBuffer? = null

        fun path(): String = mapFilePath
        fun size(): Int = mapSize
        fun data(): ByteArray = mapData
        fun get(index: Int): Byte = if (index < 0 || index >= size()) 0 else mapData[index]
        fun set(index: Int, value: Byte) { if (index in 0 until size()) mapData[index] = value }
        fun closed(): Boolean = buffer == null || file == null

        fun open(initialize: Boolean): Boolean {
            if (mapSize <= 0 || mapSize > 4096) return false
            close()
            return try {
                if (initialize) {
                    val p = Path.of(mapFilePath)
                    if (!Files.exists(p) || Files.size(p) < mapSize) {
                        Files.write(p, mapData)
                    }
                }
                val raf = RandomAccessFile(File(mapFilePath), if (isWrite) "rw" else "r")
                val ch = raf.channel
                if (ch.size() < mapSize) {
                    ch.close(); raf.close(); return false
                }
                val mode = if (isWrite) FileChannel.MapMode.READ_WRITE else FileChannel.MapMode.READ_ONLY
                val buf = ch.map(mode, 0, mapSize.toLong())
                if (isWrite) buf.clear().put(mapData)
                file = raf; channel = ch; buffer = buf
                true
            } catch (_: Throwable) {
                close(); false
            }
        }

        fun close() {
            try { channel?.close() } catch (_: Throwable) {}
            try { file?.close() } catch (_: Throwable) {}
            channel = null; file = null; buffer = null
        }

        fun remove() { try { File(mapFilePath).delete() } catch (_: Throwable) {} }

        fun tick(): Boolean = try {
            if (isWrite) buffer!!.clear().put(mapData) else buffer!!.clear().get(mapData)
            true
        } catch (_: Throwable) {
            close(); false
        }
    }

    /** Memory Mapped IPC adapter implementation. */
    class Adapter(private val isSystemSide: Boolean) : RedstoneClientAdapter {
        private val reopenDelay = if (isSystemSide) 0 else ERROR_RELOAD_DELAY
        private val inFile  = FileMemMap(ipcIoPath(isSystemSide),  false, reopenDelay, MAP_SIZE)
        private val outFile = FileMemMap(ipcIoPath(!isSystemSide), true,  reopenDelay, MAP_SIZE)
        private var inputDataWord: Long = 0
        private var outputDataWord: Long = 0
        private var inputDataChanged = false
        private var outputDataChanged = true
        private var ipcOpen = false

        /** @see RedstoneClientAdapter */
        override fun getInputs(): Long = inputDataWord

        /** @see RedstoneClientAdapter */
        override fun setInputs(value: Long) { if (value != inputDataWord) { inputDataWord = value; inputDataChanged = true } }

        /** @see RedstoneClientAdapter */
        override fun isInputsChanged(): Boolean = inputDataChanged

        /** @see RedstoneClientAdapter */
        override fun setInputsChanged(changed: Boolean) { inputDataChanged = changed }

        /** @see RedstoneClientAdapter */
        override fun getOutputs(): Long = outputDataWord

        /** @see RedstoneClientAdapter */
        override fun setOutputs(value: Long) { if (value != outputDataWord) { outputDataWord = value; outputDataChanged = true } }

        /** @see RedstoneClientAdapter */
        override fun isOutputsChanged(): Boolean = outputDataChanged

        /** @see RedstoneClientAdapter */
        override fun setOutputsChanged(changed: Boolean) { outputDataChanged = changed }

        /** @see RedstoneClientAdapter */
        override fun isOpen(): Boolean = ipcOpen

        /** Timed/client tick related cyclic IPC I/O method. @see RedstoneClientAdapter */
        override fun tick() {
            if (!ipcOpen) {
                ipcOpen = if (isSystemSide) {
                    inFile.open(true) && outFile.open(true)
                } else if (inFile.closed() || outFile.closed()) {
                    inFile.open(false) && outFile.open(false)
                } else false
            } else {
                if (inFile.tick()) {
                    var v = 0L
                    for (i in 0 until 16) v = (v shl 4) or hex2nibble(inFile.get(i)).toLong()
                    if (v != inputDataWord) { inputDataWord = v; inputDataChanged = true }
                }
                if (outputDataChanged) {
                    var v = outputDataWord
                    for (i in 15 downTo 0) {
                        outFile.set(i, nibble2hex((v and 0xf).toInt()))
                        v = v ushr 4
                    }
                    outputDataChanged = !outFile.tick()
                }
                ipcOpen = !inFile.closed() && !outFile.closed()
            }
        }

        companion object {
            private const val ERROR_RELOAD_DELAY = 20
            private const val MAP_SIZE = 16
            private var singletonInstance: Adapter? = null
            private var featureEnabled = true

            private fun hex2nibble(b: Byte): Int = when {
                b >= 'A'.code.toByte() && b <= 'F'.code.toByte() -> b - 'A'.code.toByte() + 10
                b >= 'a'.code.toByte() && b <= 'f'.code.toByte() -> b - 'a'.code.toByte() + 10
                b >= '0'.code.toByte() && b <= '9'.code.toByte() -> b - '0'.code.toByte()
                else -> 0
            }

            private fun nibble2hex(n: Int): Byte = when {
                n <= 0x0 -> '0'.code.toByte()
                n <= 0x9 -> ('0'.code + n).toByte()
                n <= 0xf -> ('a'.code + (n - 10)).toByte()
                else -> '0'.code.toByte()
            }

            /**
             * Returns the IPC map file path ("[prefix].i.mmap" or "[prefix].o.mmap") from
             * the reference perspective of the MC mod side.
             *
             * @param mcSideOutput True if output for the mod side and input for system side, or vice versa.
             * @return System formatted IPC file path string representation.
             */
            @JvmStatic
            fun ipcIoPath(mcSideOutput: Boolean): String =
                Auxiliaries.getGameDirectory().resolve("redstonepen." + (if (mcSideOutput) 'o' else 'i') + ".mmap").toString()

            /**
             * Returns true if the feature is enabled. Initialized after
             * the first invocation of `instance()`.
             * @return bool
             */
            @JvmStatic
            fun available(): Boolean = featureEnabled && singletonInstance != null

            /**
             * Mod side singleton instance getter. Returns `null`
             * on error or if the preconditions to allow enabling
             * this feature are not met.
             *
             * If you use this class for writing a system-side
             * adapter program, call the constructor directly.
             *
             * @return Adapter
             */
            @JvmStatic
            fun instance(): Adapter? {
                if (!featureEnabled) return null
                if (singletonInstance != null) return singletonInstance
                return try {
                    if (!Files.exists(Path.of(ipcIoPath(false))) ||
                        !Files.exists(Path.of(ipcIoPath(true)))) {
                        featureEnabled = false; null
                    } else {
                        Adapter(false).also { singletonInstance = it }
                    }
                } catch (_: Exception) {
                    featureEnabled = false; null
                }
            }
        }
    }
}
