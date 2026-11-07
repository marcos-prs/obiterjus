package com.obiterjus.core.parser

import java.text.Normalizer

object SigiloDetector {
    fun isSigiloso(textoLimpo: String?, nivelSigilo: Int? = null): Boolean {
        if ((nivelSigilo ?: 0) > 0) return true

        val normalizedText = textoLimpo.normalizedForSearch()
        return MARCADORES.any(normalizedText::contains)
    }

    /**
     * Passou a ser porta de saída de dado — decide o que não vai para a nuvem
     * nem para o serviço de minutas. Falso positivo aqui apenas retém dado no
     * aparelho; falso negativo publica o que corre em segredo.
     */
    private val MARCADORES = listOf(
        "processo sob sigilo",
        "processo sigiloso",
        "segredo de justica",
        "sob sigilo judicial",
        "conforme legislacao aplicavel",
    )

    private fun String?.normalizedForSearch(): String =
        Normalizer.normalize(this.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
}
