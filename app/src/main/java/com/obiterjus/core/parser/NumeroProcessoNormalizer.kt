package com.obiterjus.core.parser

object NumeroProcessoNormalizer {
    private val cnjFormattedRegex = Regex("\\b\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}\\b")
    private val twentyDigitsRegex = Regex("\\b\\d{20}\\b")

    fun digitsOnly(raw: String?): String? =
        raw
            ?.filter(Char::isDigit)
            ?.takeIf { it.isNotEmpty() }

    fun normalize(raw: String?): String? =
        digitsOnly(raw)?.takeIf { it.length == 20 }

    fun format(raw: String?): String? {
        val digits = normalize(raw) ?: return null
        return buildString {
            append(digits.substring(0, 7))
            append('-')
            append(digits.substring(7, 9))
            append('.')
            append(digits.substring(9, 13))
            append('.')
            append(digits.substring(13, 14))
            append('.')
            append(digits.substring(14, 16))
            append('.')
            append(digits.substring(16, 20))
        }
    }

    fun extractAll(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()

        val formatted = cnjFormattedRegex.findAll(text).mapNotNull { normalize(it.value) }
        val compact = twentyDigitsRegex.findAll(text).mapNotNull { normalize(it.value) }

        return (formatted + compact).distinct().toList()
    }
}
