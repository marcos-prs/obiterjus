package com.obiterjus.data.datajud.remote

class DataJudTribunalResolver {
    fun resolve(
        tribunal: String?,
        numeroProcesso: String?,
    ): ResolvedDataJudTribunal? {
        val normalizedTribunal = tribunal?.normalizeTribunal()
        val explicit = normalizedTribunal?.takeIf(::isSupportedTribunal)
        val inferred = explicit ?: numeroProcesso?.let(::inferStateCourtFromCnjNumber)

        return inferred?.let { sigla ->
            ResolvedDataJudTribunal(
                tribunal = sigla,
                indexName = "api_publica_${sigla.lowercase()}",
            )
        }
    }

    private fun isSupportedTribunal(value: String): Boolean =
        value in HIGH_COURTS ||
            value.matches(STATE_COURT_REGEX) ||
            value.matches(REGIONAL_COURT_REGEX)

    private fun inferStateCourtFromCnjNumber(numeroProcesso: String): String? {
        val digits = numeroProcesso.filter(Char::isDigit)
        if (digits.length != 20) return null

        val justiceSegment = digits[13]
        val tribunalCode = digits.substring(14, 16)
        return if (justiceSegment == '8') {
            STATE_COURTS_BY_CODE[tribunalCode]
        } else {
            null
        }
    }

    private fun String.normalizeTribunal(): String =
        uppercase().filter(Char::isLetterOrDigit)

    companion object {
        private val STATE_COURT_REGEX = Regex("TJ[A-Z]{2}")
        private val REGIONAL_COURT_REGEX = Regex("(TRF|TRT)\\d{1,2}|TRE[A-Z]{2}")
        private val HIGH_COURTS = setOf("STF", "STJ", "TST", "TSE", "STM", "CNJ")
        private val STATE_COURTS_BY_CODE = mapOf(
            "01" to "TJAC",
            "02" to "TJAL",
            "03" to "TJAP",
            "04" to "TJAM",
            "05" to "TJBA",
            "06" to "TJCE",
            "07" to "TJDFT",
            "08" to "TJES",
            "09" to "TJGO",
            "10" to "TJMA",
            "11" to "TJMT",
            "12" to "TJMS",
            "13" to "TJMG",
            "14" to "TJPA",
            "15" to "TJPB",
            "16" to "TJPR",
            "17" to "TJPE",
            "18" to "TJPI",
            "19" to "TJRJ",
            "20" to "TJRN",
            "21" to "TJRS",
            "22" to "TJRO",
            "23" to "TJRR",
            "24" to "TJSC",
            "25" to "TJSE",
            "26" to "TJSP",
            "27" to "TJTO",
        )
    }
}

data class ResolvedDataJudTribunal(
    val tribunal: String,
    val indexName: String,
)
