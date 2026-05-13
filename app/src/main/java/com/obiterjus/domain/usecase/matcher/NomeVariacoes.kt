package com.obiterjus.domain.usecase.matcher

import java.text.Normalizer

/**
 * Gera variações ortográficas/estruturais de um nome para ampliar a consulta no
 * DJEN. A ideia é compensar cadastros incompletos no CNJ: às vezes o nome do
 * advogado aparece sem acento, sem nome do meio, ou só com primeiro + último.
 *
 * Sempre inclui a forma canônica como primeira variante e deduplica preservando
 * a ordem (canônica primeiro = melhor sinal).
 */
object NomeVariacoes {

    fun gerar(nome: String): List<String> {
        val canonico = nome.trim()
        if (canonico.isBlank()) return emptyList()

        val stopwords = setOf("de", "da", "do", "das", "dos", "e")
        val tokens = canonico.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val tokensSignificativos = tokens.filter { it.lowercase() !in stopwords }

        val variacoes = mutableListOf<String>()
        variacoes += canonico

        val semAcento = removerAcentos(canonico)
        if (semAcento != canonico) variacoes += semAcento

        if (tokensSignificativos.size >= 2) {
            val primeiroUltimo = "${tokensSignificativos.first()} ${tokensSignificativos.last()}"
            variacoes += primeiroUltimo
            val semAcentoPU = removerAcentos(primeiroUltimo)
            if (semAcentoPU != primeiroUltimo) variacoes += semAcentoPU
        }

        return variacoes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    private fun removerAcentos(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
}
