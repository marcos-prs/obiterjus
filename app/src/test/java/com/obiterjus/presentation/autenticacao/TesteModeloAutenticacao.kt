package com.obiterjus.presentation.autenticacao

import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.SincronizacaoNuvemResumo
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.RepositorioSincronizacao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TesteModeloAutenticacao {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emailVazioMostraErroSemChamarRepositorio() = runTest {
        val repositorio = RepositorioAuthFake()
        val sincronizacao = RepositorioSincronizacaoFake()
        val modelo = ModeloAutenticacao(repositorio, sincronizacao, dispatcher)
        advanceUntilIdle()

        modelo.signInEmail("", "")

        assertEquals(ErroAutenticacao.CamposObrigatorios, modelo.estado.value.erro)
        assertFalse(modelo.estado.value.isLoading)
        assertEquals(0, repositorio.loginEmailChamadas)
    }

    @Test
    fun falhaNoLoginPorEmailMostraErroAmigavel() = runTest {
        val repositorio = RepositorioAuthFake(
            resultadoEmail = Result.failure(IllegalStateException()),
        )
        val sincronizacao = RepositorioSincronizacaoFake()
        val modelo = ModeloAutenticacao(repositorio, sincronizacao, dispatcher)
        advanceUntilIdle()

        modelo.signInEmail("usuario@exemplo.com", "senha")
        advanceUntilIdle()

        assertEquals(ErroAutenticacao.EntrarEmail, modelo.estado.value.erro)
        assertFalse(modelo.estado.value.isLoading)
        assertEquals(1, repositorio.loginEmailChamadas)
    }

    @Test
    fun loginPorEmailComSucessoAtualizaUsuario() = runTest {
        val usuarioLogado = AuthUser(
            uid = "usuario-logado",
            email = "usuario@exemplo.com",
            isAnonymous = false,
        )
        val repositorio = RepositorioAuthFake(
            resultadoEmail = Result.success(usuarioLogado),
        )
        val sincronizacao = RepositorioSincronizacaoFake()
        val modelo = ModeloAutenticacao(repositorio, sincronizacao, dispatcher)
        advanceUntilIdle()

        modelo.signInEmail(" usuario@exemplo.com ", "senha")
        advanceUntilIdle()

        assertEquals(null, modelo.estado.value.erro)
        assertEquals(usuarioLogado, modelo.estado.value.usuario)
        assertEquals("usuario@exemplo.com", repositorio.ultimoEmail)
        assertEquals(1, sincronizacao.restaurarChamadas)
        assertEquals(EstadoSincronizacaoNuvem.Concluida(sincronizacao.resumo), modelo.estado.value.sincronizacao)
    }

    private class RepositorioAuthFake(
        private val resultadoEmail: Result<AuthUser> = Result.success(usuarioAnonimo),
    ) : AuthRepository {
        private val usuarioAtual = MutableStateFlow<AuthUser?>(usuarioAnonimo)
        var loginEmailChamadas = 0
            private set
        var ultimoEmail: String? = null
            private set

        override val currentUser: Flow<AuthUser?> = usuarioAtual

        override suspend fun signInAnonymously(): Result<AuthUser> {
            usuarioAtual.value = usuarioAnonimo
            return Result.success(usuarioAnonimo)
        }

        override suspend fun linkWithGoogle(idToken: String): Result<AuthUser> =
            Result.success(usuarioAnonimo)

        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
            loginEmailChamadas += 1
            ultimoEmail = email
            resultadoEmail.getOrNull()?.let { usuarioAtual.value = it }
            return resultadoEmail
        }

        override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> =
            signInWithEmail(email, password)

        override suspend fun signOut() {
            usuarioAtual.value = null
        }

        private companion object {
            val usuarioAnonimo = AuthUser(
                uid = "usuario-anonimo",
                email = null,
                isAnonymous = true,
            )
        }
    }

    private class RepositorioSincronizacaoFake(
        val resumo: SincronizacaoNuvemResumo = SincronizacaoNuvemResumo(
            processos = 1,
            publicacoes = 2,
            movimentos = 3,
        ),
    ) : RepositorioSincronizacao {
        var enviarChamadas = 0
            private set
        var restaurarChamadas = 0
            private set

        override suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo {
            enviarChamadas += 1
            return resumo
        }

        override suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo {
            restaurarChamadas += 1
            return resumo
        }
    }
}
