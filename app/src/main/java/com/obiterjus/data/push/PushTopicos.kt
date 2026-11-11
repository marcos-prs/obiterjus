package com.obiterjus.data.push

import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Nomes dos tópicos do FCM em que o aparelho se inscreve.
 *
 * Tópico é como o servidor endereça o aparelho sem guardar credencial de
 * entrega: o `firebase-messaging` 26 depreciou `getToken()`, e o modelo atual é
 * o app se registrar e se inscrever por conta própria.
 *
 * Nome de tópico aceita só `[a-zA-Z0-9-_.~%]`, então o deslocamento de fuso vai
 * com `m`/`p` no lugar do sinal.
 */
object PushTopicos {

    /**
     * Tópico da Fase 1: todo aparelho naquele horário e fuso. O servidor empurra
     * para ele no momento certo sem saber nada de quem está escutando.
     */
    fun acordaCego(horaLocal: Int, offsetMinutos: Int): String =
        "sync-diaria-h%02d-utc%s".format(Locale.ROOT, horaLocal, offsetTexto(offsetMinutos))

    /**
     * Tópico da Fase 2: um por OAB, derivado por HMAC para o número não
     * transitar nos metadados do Google.
     *
     * O sal vive no APK, então não é segredo contra quem extrai o pacote — não
     * é essa a função dele. A função é não entregar a OAB do advogado a um
     * terceiro que só precisa rotear uma mensagem vazia.
     */
    fun porOab(numeroOab: String, ufOab: String, sal: String): String {
        val material = "${ufOab.trim().uppercase(Locale.ROOT)}|${numeroOab.trim()}"
        return "oab-" + hmacHex(material, sal).take(TAMANHO_HASH)
    }

    private fun offsetTexto(offsetMinutos: Int): String {
        val sinal = if (offsetMinutos < 0) "m" else "p"
        return sinal + kotlin.math.abs(offsetMinutos)
    }

    private fun hmacHex(material: String, sal: String): String {
        val mac = Mac.getInstance(ALGORITMO).apply {
            init(SecretKeySpec(sal.toByteArray(Charsets.UTF_8), ALGORITMO))
        }
        return mac.doFinal(material.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private const val ALGORITMO = "HmacSHA256"
    private const val TAMANHO_HASH = 24
}
