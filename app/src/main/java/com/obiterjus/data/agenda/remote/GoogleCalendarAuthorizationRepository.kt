package com.obiterjus.data.agenda.remote

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

sealed interface GoogleCalendarAuthorizationResult {
    data class Authorized(val accessToken: String) : GoogleCalendarAuthorizationResult

    data class NeedsResolution(val pendingIntent: PendingIntent) : GoogleCalendarAuthorizationResult
}

/**
 * Falha de autorização já traduzida para algo acionável.
 *
 * A versão anterior devolvia a exceção crua do Play Services e a tela mostrava
 * sempre a mesma frase genérica, então um erro de configuração do Google Cloud
 * (que exige mexer no console) ficava indistinguível de "sem internet".
 */
class FalhaAutorizacaoCalendar(
    message: String,
    val codigo: Int?,
    cause: Throwable?,
) : Exception(message, cause)

class GoogleCalendarAuthorizationRepository(
    private val context: Context,
    private val tokenRepository: GoogleCalendarTokenRepository,
) {
    suspend fun authorize(activity: Activity): Result<GoogleCalendarAuthorizationResult> = try {
        val authorizationClient = Identity.getAuthorizationClient(activity)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GOOGLE_CALENDAR_SCOPE)))
            .build()
        val result = authorizationClient.authorize(request).await()
        val pendingIntent = result.pendingIntent
        if (result.hasResolution() && pendingIntent != null) {
            Result.success(GoogleCalendarAuthorizationResult.NeedsResolution(pendingIntent))
        } else {
            val accessToken = result.accessToken?.trim().orEmpty()
            if (accessToken.isBlank()) {
                Result.failure(semToken())
            } else {
                tokenRepository.saveAccessToken(accessToken)
                Result.success(GoogleCalendarAuthorizationResult.Authorized(accessToken))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(traduzir("authorize", e))
    }

    suspend fun completeAuthorization(data: Intent?): Result<String> = try {
        val authorizationClient = Identity.getAuthorizationClient(context)
        if (data == null) {
            return Result.failure(
                FalhaAutorizacaoCalendar("Autorização do Google Agenda cancelada.", null, null),
            )
        }
        val result = authorizationClient.getAuthorizationResultFromIntent(data)
        val accessToken = result.accessToken?.trim().orEmpty()
        if (accessToken.isBlank()) {
            Result.failure(semToken())
        } else {
            tokenRepository.saveAccessToken(accessToken)
            Result.success(accessToken)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(traduzir("completeAuthorization", e))
    }

    suspend fun revokeAccess(): Result<Unit> = try {
        val authorizationClient = Identity.getAuthorizationClient(context)
        tokenRepository.clearAccessToken()
        authorizationClient.revokeAccess(
            com.google.android.gms.auth.api.identity.RevokeAccessRequest.builder().build(),
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(traduzir("revokeAccess", e))
    }

    private fun semToken() = FalhaAutorizacaoCalendar(
        "O Google não devolveu um token de acesso para a Agenda.",
        null,
        null,
    )

    /**
     * O Play Services devolve tudo como [ApiException] com um código numérico; sem
     * traduzir, o log e a tela não dizem se o problema é do aparelho ou do projeto
     * no Google Cloud.
     */
    private fun traduzir(operacao: String, e: Exception): Throwable {
        val codigo = (e as? ApiException)?.statusCode
        val descricao = when (codigo) {
            CommonStatusCodes.DEVELOPER_ERROR ->
                "Configuração do Google Cloud incompleta: não existe cliente OAuth Android " +
                    "para com.obiterjus com a assinatura deste build (SHA-1), ou a tela de " +
                    "consentimento não libera o escopo do Google Agenda para esta conta."
            CommonStatusCodes.SIGN_IN_REQUIRED ->
                "Entre com uma Conta Google no aparelho para usar a Agenda."
            CommonStatusCodes.NETWORK_ERROR ->
                "Sem conexão para falar com o Google Agenda."
            CommonStatusCodes.API_NOT_CONNECTED ->
                "O Google Play Services deste aparelho não oferece a autorização da Agenda."
            CommonStatusCodes.CANCELED ->
                "Autorização do Google Agenda cancelada."
            CommonStatusCodes.INTERNAL_ERROR ->
                "O Google Play Services falhou ao autorizar a Agenda."
            else -> e.message ?: "Falha ao autorizar o Google Agenda."
        }
        val detalhe = codigo?.let { " (código $it)" }.orEmpty()
        Log.w(TAG, "$operacao falhou$detalhe: $descricao", e)
        return FalhaAutorizacaoCalendar(descricao + detalhe, codigo, e)
    }

    companion object {
        const val GOOGLE_CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
        private const val TAG = "CalendarAuth"
    }
}
