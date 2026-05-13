package com.obiterjus.domain.model

import com.obiterjus.R

/**
 * Classificação simplificada de atos jurídicos para fins de interface e filtros.
 */
enum class TipoAto(val rotuloRes: Int) {
    SENTENCA(R.string.ato_sentenca),
    DECISAO(R.string.ato_decisao),
    DESPACHO(R.string.ato_despacho),
    OUTROS(R.string.ato_outros);

    companion object {
        fun classificar(tipoComunicacao: String?, texto: String?): TipoAto {
            val combo = "${tipoComunicacao.orEmpty()} ${texto.orEmpty()}".lowercase()
            return when {
                combo.contains("senten") || combo.contains("extin") -> SENTENCA
                combo.contains("decis") || combo.contains("liminar") || combo.contains("tutela") -> DECISAO
                combo.contains("despacho") || combo.contains("vistas") || combo.contains("digam") -> DESPACHO
                else -> OUTROS
            }
        }
    }
}
