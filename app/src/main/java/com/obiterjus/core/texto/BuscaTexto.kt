package com.obiterjus.core.texto

import java.util.Locale

fun String?.correspondeAoTermoBusca(termoBusca: String): Boolean {
    val termo = termoBusca.normalizarBusca()
    if (termo.isBlank()) return true

    val valor = orEmpty().normalizarBusca()
    return valor.contains(termo) || valor.compactarBusca().contains(termo.compactarBusca())
}

fun Iterable<String?>.correspondeAoTermoBusca(termoBusca: String): Boolean =
    any { valor -> valor.correspondeAoTermoBusca(termoBusca) }

fun String.normalizarBusca(): String =
    trim().lowercase(Locale.ROOT)

private fun String.compactarBusca(): String =
    filter(Char::isLetterOrDigit).lowercase(Locale.ROOT)
