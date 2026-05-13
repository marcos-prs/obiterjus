package com.obiterjus.domain.model

import com.obiterjus.R

enum class GeneroTribunal(val rotuloRes: Int) {
    SUPERIOR_SUPREMO(R.string.genero_tribunal_superior_supremo),
    TRIBUNAL_REGIONAL_FEDERAL(R.string.genero_tribunal_regional_federal),
    TRIBUNAL_REGIONAL_TRABALHO(R.string.genero_tribunal_regional_trabalho),
    TRIBUNAL_JUSTICA(R.string.genero_tribunal_justica),
    TRIBUNAL_REGIONAL_ELEITORAL(R.string.genero_tribunal_regional_eleitoral),
    OUTROS(R.string.genero_tribunal_outros);

    companion object {
        fun classificar(tribunal: String?): GeneroTribunal {
            val sigla = tribunal
                ?.trim()
                ?.uppercase()
                ?.substringBefore(' ')
                ?.substringBefore('-')
                .orEmpty()

            return when {
                sigla in especiesSuperiores -> SUPERIOR_SUPREMO
                sigla.startsWith("TJ") -> TRIBUNAL_JUSTICA
                sigla.startsWith("TRF") -> TRIBUNAL_REGIONAL_FEDERAL
                sigla.startsWith("TRT") -> TRIBUNAL_REGIONAL_TRABALHO
                sigla.startsWith("TRE") -> TRIBUNAL_REGIONAL_ELEITORAL
                else -> OUTROS
            }
        }

        val especiesSuperiores = listOf("STF", "STJ", "TST", "TSE", "STM")
        val especiesTRF = (1..6).map { "TRF$it" }
        val especiesTRT = (1..24).map { "TRT$it" }
        val especiesTJ = listOf(
            "TJAC", "TJAL", "TJAM", "TJAP", "TJBA", "TJCE", "TJDF", "TJES", "TJGO",
            "TJMA", "TJMG", "TJMS", "TJMT", "TJPA", "TJPB", "TJPR", "TJPE", "TJPI",
            "TJRJ", "TJRN", "TJRS", "TJRO", "TJRR", "TJSC", "TJSP", "TJSE", "TJTO"
        )
        val especiesTRE = listOf(
            "TRE-AC", "TRE-AL", "TRE-AP", "TRE-AM", "TRE-BA", "TRE-CE", "TRE-DF", "TRE-ES", "TRE-GO",
            "TRE-MA", "TRE-MT", "TRE-MS", "TRE-MG", "TRE-PA", "TRE-PB", "TRE-PR", "TRE-PE", "TRE-PI",
            "TRE-RJ", "TRE-RN", "TRE-RS", "TRE-RO", "TRE-RR", "TRE-SC", "TRE-SP", "TRE-SE", "TRE-TO"
        )
        val especiesOutros = listOf("CNJ", "CJF", "CSJT", "ENAMAT")

        fun obterEspecies(genero: GeneroTribunal): List<String> = when (genero) {
            SUPERIOR_SUPREMO -> especiesSuperiores
            TRIBUNAL_REGIONAL_FEDERAL -> especiesTRF
            TRIBUNAL_REGIONAL_TRABALHO -> especiesTRT
            TRIBUNAL_JUSTICA -> especiesTJ
            TRIBUNAL_REGIONAL_ELEITORAL -> especiesTRE
            OUTROS -> especiesOutros
        }
    }
}
