package wile.tools.ktanalyzer.engine

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.nio.file.Files

class CollectingMessageCollector : MessageCollector {
    data class Message(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?,
    )

    private val _messages = mutableListOf<Message>()
    val messages: List<Message> get() = _messages

    override fun clear() = _messages.clear()
    override fun hasErrors() = _messages.any { it.severity.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        _messages += Message(severity, message, location)
    }
}

class PsiEngine(private val extraClasspath: List<File> = emptyList()) : AutoCloseable {
    val collector = CollectingMessageCollector()

    fun analyze(sourceFiles: List<File>) {
        if (sourceFiles.isEmpty()) return
        val tmpOut = Files.createTempDirectory("kt-analyzer-out").toFile()
        try {
            val compiler = K2JVMCompiler()
            val args = K2JVMCompilerArguments().apply {
                freeArgs = sourceFiles.map { it.absolutePath }
                destination = tmpOut.absolutePath
                noStdlib = true  // We add stdlib explicitly below via classpath
                noReflect = true
                noJdk = false
                suppressWarnings = false

                // Build explicit classpath: stdlib from our fat JAR + user-supplied extras
                val cp = mutableListOf<String>()
                val stdlib = stdlibJar()
                stdlib?.let { cp.add(it.absolutePath) }
                cp.addAll(extraClasspath.map { it.absolutePath })
                if (cp.isNotEmpty()) {
                    classpath = cp.joinToString(File.pathSeparator)
                }
            }
            compiler.exec(collector, Services.EMPTY, args)
        } finally {
            tmpOut.deleteRecursively()
        }
    }

    override fun close() {}

    companion object {
        private var cachedStdlibJar: File? = null

        /** Extracts kotlin-stdlib classes from the running fat JAR into a temp JAR,
         *  so K2JVMCompiler can resolve Kotlin built-ins during analysis. */
        private fun stdlibJar(): File? {
            cachedStdlibJar?.let { if (it.exists()) return it }

            // Locate our own JAR on the classpath — it contains stdlib classes
            val selfJar = PsiEngine::class.java.protectionDomain?.codeSource?.location?.toURI()
                ?.let { File(it) }?.takeIf { it.isFile && it.extension == "jar" }
                ?: return null

            // Create a filtered JAR containing only stdlib packages
            val tmpJar = File.createTempFile("kt-analyzer-stdlib-", ".jar")
            tmpJar.deleteOnExit()

            java.util.jar.JarOutputStream(tmpJar.outputStream()).use { out ->
                java.util.jar.JarInputStream(selfJar.inputStream()).use { jarIn ->
                    var entry = jarIn.nextJarEntry
                    val stdlibPrefixes = listOf(
                        "kotlin/", "kotlinx/", "META-INF/kotlin-stdlib",
                        "META-INF/proguard/kotlin-stdlib",
                    )
                    val seen = HashSet<String>()
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && stdlibPrefixes.any { name.startsWith(it) } && seen.add(name)) {
                            out.putNextEntry(java.util.jar.JarEntry(name))
                            jarIn.copyTo(out)
                            out.closeEntry()
                        }
                        entry = jarIn.nextJarEntry
                    }
                }
            }

            cachedStdlibJar = tmpJar
            return tmpJar
        }
    }
}
