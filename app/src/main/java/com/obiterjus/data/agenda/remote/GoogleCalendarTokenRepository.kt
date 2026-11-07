package com.obiterjus.data.agenda.remote

import android.content.Context
import com.obiterjus.core.security.CofreLocal

/**
 * Access token do Google Calendar.
 *
 * Fica no [CofreLocal] em vez do DataStore por dois motivos: é credencial e
 * precisa estar cifrada em repouso, e a leitura tem de ser síncrona — o
 * interceptor do OkHttp não pode suspender. A versão anterior usava um
 * StateFlow hidratado por corrotina, e as primeiras chamadas depois de um cold
 * start saíam sem cabeçalho, tomavam 401 e apagavam um token que era válido.
 */
interface GoogleCalendarTokenRepository {
    fun accessToken(): String?

    fun saveAccessToken(token: String)

    fun clearAccessToken()
}

class CofreGoogleCalendarTokenRepository(
    context: Context,
    private val cofre: CofreLocal = CofreLocal(context),
) : GoogleCalendarTokenRepository {

    override fun accessToken(): String? =
        cofre.ler(CofreLocal.SEGREDO_TOKEN_CALENDAR)?.takeIf { it.isNotBlank() }

    override fun saveAccessToken(token: String) {
        cofre.guardar(CofreLocal.SEGREDO_TOKEN_CALENDAR, token.trim())
    }

    override fun clearAccessToken() {
        cofre.guardar(CofreLocal.SEGREDO_TOKEN_CALENDAR, null)
    }
}
