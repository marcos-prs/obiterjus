package com.obiterjus.data.agenda.remote

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

sealed interface GoogleCalendarAuthorizationResult {
    data class Authorized(val accessToken: String) : GoogleCalendarAuthorizationResult

    data class NeedsResolution(val pendingIntent: PendingIntent) : GoogleCalendarAuthorizationResult
}

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
                Result.failure(IllegalStateException("Google Calendar não retornou access token"))
            } else {
                tokenRepository.saveAccessToken(accessToken)
                Result.success(GoogleCalendarAuthorizationResult.Authorized(accessToken))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun completeAuthorization(data: Intent?): Result<String> = try {
        val authorizationClient = Identity.getAuthorizationClient(context)
        if (data == null) {
            return Result.failure(IllegalStateException("Autorização do Google Calendar cancelada"))
        }
        val result = authorizationClient.getAuthorizationResultFromIntent(data)
        val accessToken = result.accessToken?.trim().orEmpty()
        if (accessToken.isBlank()) {
            Result.failure(IllegalStateException("Google Calendar não retornou access token"))
        } else {
            tokenRepository.saveAccessToken(accessToken)
            Result.success(accessToken)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun revokeAccess(): Result<Unit> = try {
        val authorizationClient = Identity.getAuthorizationClient(context)
        tokenRepository.clearAccessToken()
        authorizationClient.revokeAccess(
            com.google.android.gms.auth.api.identity.RevokeAccessRequest.builder().build(),
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val GOOGLE_CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
    }
}
