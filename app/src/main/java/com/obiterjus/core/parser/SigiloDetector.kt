package com.obiterjus.core.parser

import java.text.Normalizer

object SigiloDetector {
    fun isSigiloso(textoLimpo: String?, nivelSigilo: Int? = null): Boolean {
        if ((nivelSigilo ?: 0) > 0) return true

        val normalizedText = textoLimpo.normalizedForSearch()
        return normalizedText.contains("processo sob sigilo") ||
            normalizedText.contains("conforme legislacao aplicavel")
    }

    private fun String?.normalizedForSearch(): String =
        Normalizer.normalize(this.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
}
