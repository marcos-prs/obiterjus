package com.obiterjus.domain.model

/**
 * Extensões para o modelo de Publicação.
 */

val Publicacao.tipoAto: TipoAto
    get() = TipoAto.classificar(tipoComunicacao, textoLimpo)

/**
 * Retorna o nome das partes formatado para exibição rápida.
 */
val Publicacao.resumoPartes: String
    get() = participantes.joinToString(" x ") { it.nome }
