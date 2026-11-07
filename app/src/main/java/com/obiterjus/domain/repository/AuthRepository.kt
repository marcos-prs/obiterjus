package com.obiterjus.domain.repository

import com.obiterjus.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<AuthUser?>
    suspend fun linkWithGoogle(idToken: String): Result<AuthUser>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun signOut()

    /**
     * Se a conta tem provedor de e-mail/senha. Conta criada só com Google não
     * tem senha para reautenticar, e o Firebase exige reautenticação recente
     * antes de apagar a conta.
     */
    suspend fun temProvedorSenha(): Boolean

    suspend fun reautenticarComSenha(senha: String): Result<Unit>

    /**
     * Apaga a conta no provedor de autenticação. Não apaga dado algum na nuvem
     * nem no aparelho — isso é tarefa de
     * [com.obiterjus.domain.usecase.ExcluirContaUseCase].
     */
    suspend fun excluirConta(): Result<Unit>

    /** ID token do usuário autenticado, usado como Bearer em APIs próprias (Obiter-Minuta). */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? = null
}
