package wile.tools.ktanalyzer.engine

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import wile.tools.ktanalyzer.api.LintFinding
import wile.tools.ktanalyzer.api.Severity
import wile.tools.ktanalyzer.config.AnalyzerConfig
import java.io.File

class AnalysisRunner(
    private val engine: PsiEngine,
    private val config: AnalyzerConfig,
) {
    fun analyze(files: List<File>): List<LintFinding> {
        engine.analyze(files)
        return engine.collector.messages
            .mapNotNull { convertMessage(it) }
            .sortedWith(compareBy({ it.filePath }, { it.line }, { it.column }))
    }

    private fun convertMessage(msg: CollectingMessageCollector.Message): LintFinding? {
        val location = msg.location ?: return null

        // Java files are passed for type resolution only; only report Kotlin findings
        if (!location.path.endsWith(".kt")) return null

        // Skip LOGGING/OUTPUT noise
        if (msg.severity == CompilerMessageSeverity.LOGGING ||
            msg.severity == CompilerMessageSeverity.OUTPUT
        ) return null

        val ruleId = derivedRuleId(msg.message)
        if (ruleId in config.suppress) return null

        val severity = config.severityOverrides[ruleId]
            ?: when {
                msg.severity.isError -> Severity.ERROR
                msg.severity == CompilerMessageSeverity.WARNING ||
                    msg.severity == CompilerMessageSeverity.STRONG_WARNING -> Severity.WARNING
                else -> Severity.INFO
            }

        return LintFinding(
            ruleId = ruleId,
            message = msg.message,
            severity = severity,
            filePath = location.path,
            line = location.line,
            column = location.column,
        )
    }

    companion object {
        /** Derives a stable rule ID from the human-readable message text.
         * Strips quoted identifiers first so "Unresolved reference 'Foo'" → UNRESOLVED_REFERENCE
         * rather than UNRESOLVED_REFERENCE_FOO, matching Kotlin's diagnostic factory names. */
        fun derivedRuleId(message: String): String {
            val clause = message
                .replace(Regex("'[^']*'"), "")  // strip quoted identifiers: 'Foo', 'net.Foo'
                .substringBefore(":\n")          // multi-line messages end the label at the colon
                .substringBefore(": ")           // inline detail text starts after ': '
                .substringBefore(". ")           // stop at end of first sentence
                .trim()
            return clause
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .uppercase()
                .trim('_')
                .take(80)
        }
    }
}
