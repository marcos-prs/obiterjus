package com.obiterjus.domain.model

enum class ConfiancaMatch {
    ALTA,
    MEDIA,
    BAIXA;

    companion object {
        val padrao: ConfiancaMatch = ALTA
    }
}
