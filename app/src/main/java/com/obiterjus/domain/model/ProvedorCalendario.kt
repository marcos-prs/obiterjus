package com.obiterjus.domain.model

enum class ProvedorCalendario(val codigo: String) {
    GOOGLE("GOOGLE"),
    OUTLOOK("OUTLOOK"),
    LOCAL("LOCAL");

    companion object {
        fun fromCodigo(codigo: String?): ProvedorCalendario? =
            entries.firstOrNull { it.codigo.equals(codigo, ignoreCase = true) }
    }
}

