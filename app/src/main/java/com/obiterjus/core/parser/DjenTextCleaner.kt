package com.obiterjus.core.parser

data class DjenCleanText(
    val raw: String?,
    val clean: String,
    val hasHtml: Boolean,
    val hasTemplateError: Boolean,
)

object DjenTextCleaner {
    private val tagRegex = Regex("<[^>]+>")
    private val multilineWhitespaceRegex = Regex("[ \\t\\x0B\\f]+")
    private val repeatedBlankLinesRegex = Regex("\\n{2,}")
    private val templateErrorRegex = Regex(
        pattern = "(erro|falha).{0,32}(template|modelo|interpreta)|(template|interpreta).{0,32}(erro|falha)",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    
    private val headerKeywordsRegex = Regex(
        "(?i)(?<=[^\\n])\\s+(?=(PROCESSO|CLASSE|ASSUNTO|RELATOR|CPF|CNPJ|ADVOGADO|POLO ATIVO|POLO PASSIVO|OAB|Atenção):)",
    )

    fun clean(raw: String?): DjenCleanText {
        val original = raw.orEmpty()
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            
        val hasHtml = tagRegex.containsMatchIn(original)
        val withoutTags = if (hasHtml) original.htmlToText() else original
        
        val withInjectedBreaks = withoutTags.replace(headerKeywordsRegex, "\n")

        val clean = withInjectedBreaks
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .lineSequence()
            .map { line -> line.replace(multilineWhitespaceRegex, " ").trim() }
            .joinToString("\n")
            .replace(repeatedBlankLinesRegex, "\n")
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
            .replace(Regex("(?i)</?(p|div|tr|li|h[1-6])\\s*>"), "\n")
            .replace(tagRegex, "")
}
