package com.obiterjus.domain.repository

import com.obiterjus.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    suspend fun signInAnonymously(): Result<AuthUser>
    suspend fun linkWithGoogle(idToken: String): Result<AuthUser>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun signOut()

    /** ID token do usuário autenticado, usado como Bearer em APIs próprias (Obiter-Minuta). */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? = null
}
