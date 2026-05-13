package com.obiterjus.core.texto

fun String.formatarCnj(): String {
    val digitos = filter(Char::isDigit)
    if (digitos.length != 20) return this

    return buildString {
        append(digitos.substring(0, 7))
        append('-')
        append(digitos.substring(7, 9))
        append('.')
        append(digitos.substring(9, 13))
        append('.')
        append(digitos.substring(13, 14))
        append('.')
        append(digitos.substring(14, 16))
        append('.')
        append(digitos.substring(16, 20))
    }
}
