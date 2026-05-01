package com.obiterjus.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInAnonymously(): Result<AuthUser> = executarComResultado {
        val result = auth.signInAnonymously().await()
        result.user?.toAuthUser() ?: throw IllegalStateException("Usuário nulo após login anônimo")
    }

    override suspend fun linkWithGoogle(idToken: String): Result<AuthUser> = executarComResultado {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val currentUser = auth.currentUser
        val result = if (currentUser != null && currentUser.isAnonymous) {
            currentUser.linkWithCredential(credential).await()
        } else {
            auth.signInWithCredential(credential).await()
        }
        result.user?.toAuthUser() ?: throw IllegalStateException("Usuário nulo após login com Google")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> = executarComResultado {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.toAuthUser() ?: throw IllegalStateException("Usuário nulo após login com e-mail")
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> = executarComResultado {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.toAuthUser() ?: throw IllegalStateException("Usuário nulo após cadastro com e-mail")
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        isAnonymous = isAnonymous,
    )

    private suspend inline fun <T> executarComResultado(
        crossinline bloco: suspend () -> T,
    ): Result<T> =
        try {
            Result.success(bloco())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
}
