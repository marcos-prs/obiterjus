package com.obiterjus.presentation.autenticacao

import com.obiterjus.R
import com.obiterjus.data.settings.PerfilPreferences
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.data.settings.SyncPreferencesRepository
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.DataJudRepository
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import com.obiterjus.domain.usecase.MonitorarDjenUseCase
import com.obiterjus.domain.usecase.SincronizarProcessosDataJudUseCase
import com.obiterjus.ui.theme.TipoTema
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        val modelo = modelo(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { modelo.estado.collect() }
        advanceUntilIdle()

        modelo.aoEntrar()
        advanceUntilIdle()

        assertEquals("required", modelo.estado.value.mensagemErro)
        assertFalse(modelo.estado.value.carregando)
        assertEquals(0, repositorio.loginEmailChamadas)
    }

    @Test
    fun falhaNoLoginPorEmailMostraErroAmigavel() = runTest {
        val repositorio = RepositorioAuthFake(
            resultadoEmail = Result.failure(IllegalStateException()),
        )
        val modelo = modelo(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { modelo.estado.collect() }
        modelo.aoAlterarEmail("usuario@exemplo.com")
        modelo.aoAlterarSenha("senha")
        advanceUntilIdle()

        modelo.aoEntrar()
        advanceUntilIdle()

        assertEquals("login", modelo.estado.value.mensagemErro)
        assertFalse(modelo.estado.value.carregando)
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
        val modelo = modelo(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { modelo.estado.collect() }
        modelo.aoAlterarEmail(" usuario@exemplo.com ")
        modelo.aoAlterarSenha("senha")
        advanceUntilIdle()

        modelo.aoEntrar()
        advanceUntilIdle()

        assertEquals(null, modelo.estado.value.mensagemErro)
        assertEquals("success", modelo.estado.value.mensagemSucesso)
        assertEquals("usuario@exemplo.com", modelo.estado.value.emailUsuarioAtual)
        assertEquals("usuario@exemplo.com", repositorio.ultimoEmail)
        assertEquals(1, repositorio.loginEmailChamadas)
    }

    private fun modelo(authRepository: RepositorioAuthFake): ModeloAutenticacao =
        ModeloAutenticacao(
            authRepository = authRepository,
            repositorioCadastroOab = RepositorioCadastroOabFake(),
            repositorioSincronizacao = RepositorioSincronizacaoFake(),
            monitorarCnjUseCase = monitorarCnjUseCase(),
            syncPreferencesRepository = SyncPreferencesRepositoryFake(),
            perfilPreferencesRepository = PerfilPreferencesRepositoryFake(),
            textos = TextosFake,
        )

    private fun monitorarCnjUseCase(): MonitorarCnjUseCase =
        MonitorarCnjUseCase(
            monitorarDjenUseCase = MonitorarDjenUseCase(
                object : DjenRepository {
                    override suspend fun monitorar(
                        params: com.obiterjus.domain.model.MonitorarDjenParams,
                    ): com.obiterjus.domain.model.MonitorarDjenResumo =
                        throw AssertionError("Verificacao automatica nao deve rodar nestes testes")
                },
            ),
            sincronizarProcessosDataJudUseCase = SincronizarProcessosDataJudUseCase(
                object : DataJudRepository {
                    override suspend fun sincronizar(
                        params: SincronizarProcessosDataJudParams,
                    ): SincronizarProcessosDataJudResumo =
                        SincronizarProcessosDataJudResumo(
                            solicitados = 0,
                            normalizados = 0,
                            encontrados = 0,
                            naoEncontrados = 0,
                            falhas = 0,
                            movimentosSalvos = 0,
                            resultados = emptyList(),
                        )
                },
            ),
        )

    private object TextosFake : TextosAutenticacao {
        override fun get(resId: Int): String =
            when (resId) {
                R.string.autenticacao_error_required_fields -> "required"
                R.string.autenticacao_error_login_email -> "login"
                R.string.autenticacao_sucesso_login -> "success"
                else -> resId.toString()
            }

        override fun get(resId: Int, vararg args: Any): String = get(resId)
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

        override suspend fun updatePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = Result.success(Unit)

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

    private class RepositorioCadastroOabFake : RepositorioCadastroOab {
        override val cadastro = MutableStateFlow(OabCadastro())
        override val status = MutableStateFlow(SincronizacaoStatus())

        override suspend fun salvarCadastro(
            numero: String,
            uf: String,
            nomeAdvogado: String?,
            tipoInscricao: String?,
            nomeEscritorio: String?,
            areasAtuacao: List<String>?,
            dataInicio: java.time.LocalDate?,
            dataFim: java.time.LocalDate?,
        ) = Unit

        override suspend fun registrarSucesso(executadoEm: Instant, novasPublicacoes: Int) = Unit
        override suspend fun registrarFalha(executadoEm: Instant, mensagem: String) = Unit
    }

    private class SyncPreferencesRepositoryFake : SyncPreferencesRepository {
        override val syncFrequencyHours = MutableStateFlow(6)
        override suspend fun saveSyncFrequencyHours(hours: Int) {
            syncFrequencyHours.value = hours
        }
    }

    private class PerfilPreferencesRepositoryFake : PerfilPreferencesRepository {
        override val preferencias = MutableStateFlow(PerfilPreferences())
        override suspend fun saveIntervaloBuscaDias(dias: Int) = Unit
        override suspend fun saveSincronizacaoAutomatica(ativo: Boolean) = Unit
        override suspend fun saveNotificarPublicacoes(ativo: Boolean) = Unit
        override suspend fun saveNotificarPrazosUrgentes(ativo: Boolean) = Unit
        override suspend fun saveNotificarMovimentacoes(ativo: Boolean) = Unit
        override suspend fun saveTema(tema: TipoTema) = Unit
        override suspend fun saveApenasPorNome(ativo: Boolean) = Unit
    }

    private class RepositorioSincronizacaoFake : com.obiterjus.domain.repository.RepositorioSincronizacao {
        override suspend fun enviarTudo(userId: String): com.obiterjus.domain.model.SincronizacaoNuvemResumo =
            com.obiterjus.domain.model.SincronizacaoNuvemResumo()

        override suspend fun restaurarTudo(userId: String): com.obiterjus.domain.model.SincronizacaoNuvemResumo =
            com.obiterjus.domain.model.SincronizacaoNuvemResumo()

        override suspend fun enviarPerfil(userId: String): Result<Unit> = Result.success(Unit)

        override suspend fun restaurarPerfil(userId: String): Result<Unit> = Result.success(Unit)
    }
}
