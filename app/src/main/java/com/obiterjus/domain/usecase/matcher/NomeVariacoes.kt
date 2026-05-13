package com.obiterjus.domain.usecase.matcher

import java.text.Normalizer

/**
 * Gera variações ortográficas do nome do advogado para ampliar a consulta no
 * DJEN. Compensa cadastros do CNJ que armazenam o nome sem acentuação.
 *
 * Por que NÃO geramos "primeiro + último": para um sobrenome comum como
 * "Souza", isso devolve milhares de publicações de homônimos do país inteiro
 * (3349 num teste real), causando rate-limit na API e enchendo o banco de
 * lixo. Se a publicação do usuário existir com o nome abreviado, a busca por
 * OAB já a captura — então a variação curta é redundante para os casos reais
 * e estritamente prejudicial para nomes comuns.
 */
object NomeVariacoes {

    fun gerar(nome: String): List<String> {
        val canonico = nome.trim()
        if (canonico.isBlank()) return emptyList()

        val variacoes = mutableListOf(canonico)
        val semAcento = removerAcentos(canonico)
        if (semAcento != canonico) variacoes += semAcento

        return variacoes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    private fun removerAcentos(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
}
