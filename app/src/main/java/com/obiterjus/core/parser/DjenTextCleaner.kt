package com.obiterjus.core.parser

data class DjenCleanText(
    val raw: String?,
    val clean: String,
    val hasHtml: Boolean,
    val hasTemplateError: Boolean,
)

object DjenTextCleaner {
    private val tagRegex = Regex("<[^>]+>")
    private val multilineWhitespaceRegex = Regex("[ \\t\\x0B\\f\\r]+")
    private val repeatedBlankLinesRegex = Regex("\\n{3,}")
    private val templateErrorRegex = Regex(
        pattern = "(erro|falha).{0,32}(template|modelo)|template.{0,32}(erro|falha)",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    fun clean(raw: String?): DjenCleanText {
        val original = raw.orEmpty()
        val hasHtml = tagRegex.containsMatchIn(original)
        val withoutTags = if (hasHtml) original.htmlToText() else original
        val clean = withoutTags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .lineSequence()
            .map { line -> line.replace(multilineWhitespaceRegex, " ").trim() }
            .joinToString("\n")
            .replace(repeatedBlankLinesRegex, "\n\n")
            .trim()

        return DjenCleanText(
            raw = raw,
            clean = clean,
            hasHtml = hasHtml,
            hasTemplateError = templateErrorRegex.containsMatchIn(clean),
        )
    }

    private fun String.htmlToText(): String =
        replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n")
            .replace(Regex("(?i)</div\\s*>"), "\n")
            .replace(tagRegex, "")
}
