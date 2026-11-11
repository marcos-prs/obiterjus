package com.obiterjus.presentation.autenticacao

import com.obiterjus.R
import com.obiterjus.data.auth.CredencialGoogleProvider
import com.obiterjus.data.auth.ResultadoCredencialGoogle
import com.obiterjus.data.settings.PerfilPreferences
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.AgendadorSincronizacao
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.DataJudRepository
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.ProcessosRepository
import com.obiterjus.domain.usecase.AplicarPreferenciaSincronizacaoUC
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutenticacaoViewModelTest {
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
        val repositorio = AuthRepositoryFake()
        val viewModel = viewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        advanceUntilIdle()

        viewModel.aoEntrar()
        advanceUntilIdle()

        assertEquals("required", viewModel.estado.value.mensagemErro)
        assertFalse(viewModel.estado.value.carregando)
        assertEquals(0, repositorio.loginEmailChamadas)
    }

    @Test
    fun falhaNoLoginPorEmailMostraErroAmigavel() = runTest {
        val repositorio = AuthRepositoryFake(
            resultadoEmail = Result.failure(IllegalStateException()),
        )
        val viewModel = viewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        viewModel.aoAlterarEmail("usuario@exemplo.com")
        viewModel.aoAlterarSenha("senha")
        advanceUntilIdle()

        viewModel.aoEntrar()
        advanceUntilIdle()

        assertEquals("login", viewModel.estado.value.mensagemErro)
        assertFalse(viewModel.estado.value.carregando)
        assertEquals(1, repositorio.loginEmailChamadas)
    }

    @Test
    fun loginPorEmailComSucessoAtualizaUsuario() = runTest {
        val usuarioLogado = AuthUser(
            uid = "usuario-logado",
            email = "usuario@exemplo.com",
            isAnonymous = false,
        )
        val repositorio = AuthRepositoryFake(
            resultadoEmail = Result.success(usuarioLogado),
        )
        val viewModel = viewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        viewModel.aoAlterarEmail(" usuario@exemplo.com ")
        viewModel.aoAlterarSenha("senha")
        advanceUntilIdle()

        viewModel.aoEntrar()
        advanceUntilIdle()

        assertEquals(null, viewModel.estado.value.mensagemErro)
        assertEquals("success", viewModel.estado.value.mensagemSucesso)
        assertEquals("usuario@exemplo.com", viewModel.estado.value.emailUsuarioAtual)
        assertEquals("usuario@exemplo.com", repositorio.ultimoEmail)
        assertEquals(1, repositorio.loginEmailChamadas)
    }

    @Test
    fun loginComGoogleComSucessoAutentica() = runTest {
        val usuarioGoogle = AuthUser(
            uid = "usuario-google",
            email = "advogado@gmail.com",
            isAnonymous = false,
        )
        val repositorio = AuthRepositoryFake(resultadoGoogle = Result.success(usuarioGoogle))
        val viewModel = viewModel(
            repositorio,
            CredencialGoogleFake(ResultadoCredencialGoogle.Sucesso("id-token")),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        advanceUntilIdle()

        viewModel.aoEntrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        assertEquals(null, viewModel.estado.value.mensagemErro)
        assertEquals("success", viewModel.estado.value.mensagemSucesso)
        assertEquals("advogado@gmail.com", viewModel.estado.value.emailUsuarioAtual)
        assertEquals("id-token", repositorio.ultimoIdTokenGoogle)
        assertFalse(viewModel.estado.value.carregando)
    }

    @Test
    fun cancelarOSeletorDoGoogleNaoRendeErro() = runTest {
        val repositorio = AuthRepositoryFake()
        val viewModel = viewModel(repositorio, CredencialGoogleFake(ResultadoCredencialGoogle.Cancelado))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        advanceUntilIdle()

        viewModel.aoEntrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        assertEquals(null, viewModel.estado.value.mensagemErro)
        assertEquals(null, viewModel.estado.value.mensagemSucesso)
        assertFalse(viewModel.estado.value.carregando)
        assertEquals(null, repositorio.ultimoIdTokenGoogle)
    }

    @Test
    fun aparelhoSemContaGoogleMostraMensagemPropria() = runTest {
        val repositorio = AuthRepositoryFake()
        val viewModel = viewModel(repositorio, CredencialGoogleFake(ResultadoCredencialGoogle.SemConta))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        advanceUntilIdle()

        viewModel.aoEntrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        assertEquals("google-sem-conta", viewModel.estado.value.mensagemErro)
        assertFalse(viewModel.estado.value.carregando)
        assertEquals(null, repositorio.ultimoIdTokenGoogle)
    }

    @Test
    fun falhaDoFirebaseComGoogleMostraErroAmigavel() = runTest {
        val repositorio = AuthRepositoryFake(
            resultadoGoogle = Result.failure(IllegalStateException()),
        )
        val viewModel = viewModel(
            repositorio,
            CredencialGoogleFake(ResultadoCredencialGoogle.Sucesso("id-token")),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        advanceUntilIdle()

        viewModel.aoEntrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        assertEquals("google-falha", viewModel.estado.value.mensagemErro)
        assertEquals(null, viewModel.estado.value.mensagemSucesso)
        assertFalse(viewModel.estado.value.carregando)
    }

    @Test
    fun cadastroComGoogleCumpreEtapaContaESegueParaOab() = runTest {
        val repositorio = AuthRepositoryFake(
            resultadoGoogle = Result.success(
                AuthUser(uid = "usuario-google", email = "ana@gmail.com", isAnonymous = false),
            ),
        )
        val viewModel = viewModel(
            repositorio,
            CredencialGoogleFake(
                ResultadoCredencialGoogle.Sucesso(
                    idToken = "id-token",
                    nomeExibicao = "Ana Advogada",
                    email = "ana@gmail.com",
                ),
            ),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        viewModel.aoSelecionarModo(ModoAutenticacao.CADASTRAR)
        viewModel.aoAlternarTermos(true)
        viewModel.aoAlternarPrivacidade(true)
        advanceUntilIdle()

        viewModel.aoCadastrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.contaGoogle)
        assertEquals(EtapaCadastro.OAB, estado.etapaCadastro)
        assertEquals("Ana Advogada", estado.nomeCompleto)
        assertEquals("ana@gmail.com", estado.email)
        assertEquals("id-token", repositorio.ultimoIdTokenGoogle)
        // Sucesso aqui tiraria o advogado da tela e abandonaria o cadastro.
        assertEquals(null, estado.mensagemSucesso)
        assertFalse(estado.carregando)
    }

    @Test
    fun cadastroComGoogleSemAceitesNaoPedeCredencial() = runTest {
        val repositorio = AuthRepositoryFake()
        val credencial = CredencialGoogleFake(ResultadoCredencialGoogle.Sucesso("id-token"))
        val viewModel = viewModel(repositorio, credencial)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        viewModel.aoSelecionarModo(ModoAutenticacao.CADASTRAR)
        advanceUntilIdle()

        viewModel.aoCadastrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        assertEquals("politicas", viewModel.estado.value.mensagemErro)
        assertFalse(viewModel.estado.value.contaGoogle)
        assertEquals(0, credencial.chamadas)
        assertEquals(null, repositorio.ultimoIdTokenGoogle)
    }

    @Test
    fun cadastroComGoogleAdiantaOabDaContaQueJaExistia() = runTest {
        val repositorio = AuthRepositoryFake(
            resultadoGoogle = Result.success(
                AuthUser(uid = "usuario-google", email = "ana@gmail.com", isAnonymous = false),
            ),
        )
        val cadastroOab = CadastroOabRepositoryFake()
        cadastroOab.cadastro.value = OabCadastro(
            numero = "123456",
            uf = "SP",
            nomeAdvogado = "Ana da Silva",
            tipoInscricao = "SOCIO",
            nomeEscritorio = "Silva Advocacia",
            areasAtuacao = listOf("Cível"),
        )
        val viewModel = viewModel(
            repositorio,
            CredencialGoogleFake(ResultadoCredencialGoogle.Sucesso("id-token")),
            repositorioCadastroOab = cadastroOab,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        viewModel.aoSelecionarModo(ModoAutenticacao.CADASTRAR)
        viewModel.aoAlternarTermos(true)
        viewModel.aoAlternarPrivacidade(true)
        advanceUntilIdle()

        viewModel.aoCadastrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals("SP", estado.uf)
        assertEquals("123456", estado.numeroOab)
        assertEquals("Ana da Silva", estado.nomeCompleto)
        assertEquals(TipoInscricaoCadastro.SOCIO, estado.tipoInscricao)
        assertEquals("Silva Advocacia", estado.nomeEscritorio)
        assertEquals(setOf("Cível"), estado.areasAtuacao)
    }

    @Test
    fun cadastroComGoogleFinalizaSemCriarContaPorSenha() = runTest {
        val repositorio = AuthRepositoryFake(
            resultadoGoogle = Result.success(
                AuthUser(uid = "usuario-google", email = "ana@gmail.com", isAnonymous = false),
            ),
        )
        val sincronizacao = SincronizacaoRepositoryFake()
        val viewModel = viewModel(
            repositorio,
            CredencialGoogleFake(ResultadoCredencialGoogle.Sucesso("id-token")),
            repositorioSincronizacao = sincronizacao,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        viewModel.aoSelecionarModo(ModoAutenticacao.CADASTRAR)
        viewModel.aoAlternarTermos(true)
        viewModel.aoAlternarPrivacidade(true)
        advanceUntilIdle()

        viewModel.aoCadastrarComGoogle(contextoDaTela = null)
        advanceUntilIdle()

        // Pula direto ao resumo: a verificação automática não é assunto deste teste.
        viewModel.aoSelecionarEtapa(EtapaCadastro.RESUMO.ordinal)
        viewModel.aoAvancarCadastro()
        advanceUntilIdle()

        assertEquals("cadastro", viewModel.estado.value.mensagemSucesso)
        assertEquals(0, repositorio.cadastroEmailChamadas)
        assertEquals(1, sincronizacao.enviarPerfilChamadas)
    }

    @Test
    fun etapaDePreferenciasAgendaASincronizacaoDiaria() = runTest {
        val agendador = AgendadorSincronizacaoFake()
        val viewModel = viewModel(AuthRepositoryFake(), agendador = agendador)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        advanceUntilIdle()

        viewModel.aoSelecionarEtapa(EtapaCadastro.PREFERENCIAS.ordinal)
        viewModel.aoAvancarCadastro()
        advanceUntilIdle()

        // Antes do AplicarPreferenciaSincronizacaoUC esta etapa gravava a
        // preferência e seguia adiante, sem agendar nada.
        assertEquals(1, agendador.agendamentos)
        assertEquals(0, agendador.cancelamentos)
    }

    @Test
    fun etapaDePreferenciasComSincronizacaoDesligadaCancelaOAgendamento() = runTest {
        val agendador = AgendadorSincronizacaoFake()
        val viewModel = viewModel(AuthRepositoryFake(), agendador = agendador)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect() }
        advanceUntilIdle()

        viewModel.aoAlternarSincronizacaoAutomatica(false)
        viewModel.aoSelecionarEtapa(EtapaCadastro.PREFERENCIAS.ordinal)
        viewModel.aoAvancarCadastro()
        advanceUntilIdle()

        assertEquals(0, agendador.agendamentos)
        assertEquals(1, agendador.cancelamentos)
    }

    private fun viewModel(
        authRepository: AuthRepositoryFake,
        credencialGoogle: CredencialGoogleProvider = CredencialGoogleFake(ResultadoCredencialGoogle.Cancelado),
        repositorioCadastroOab: CadastroOabRepositoryFake = CadastroOabRepositoryFake(),
        repositorioSincronizacao: SincronizacaoRepositoryFake = SincronizacaoRepositoryFake(),
        perfilPreferences: PerfilPreferencesRepositoryFake = PerfilPreferencesRepositoryFake(),
        agendador: AgendadorSincronizacaoFake = AgendadorSincronizacaoFake(),
    ): AutenticacaoViewModel =
        AutenticacaoViewModel(
            authRepository = authRepository,
            repositorioCadastroOab = repositorioCadastroOab,
            repositorioSincronizacao = repositorioSincronizacao,
            monitorarCnjUseCase = monitorarCnjUseCase(),
            perfilPreferencesRepository = perfilPreferences,
            aplicarPreferenciaSincronizacao = AplicarPreferenciaSincronizacaoUC(
                perfilPreferencesRepository = perfilPreferences,
                agendador = agendador,
            ),
            sessaoLocal = SessaoLocalRepositoryFake(),
            credencialGoogle = credencialGoogle,
            textos = TextosFake,
        )

    private class AgendadorSincronizacaoFake : AgendadorSincronizacao {
        var garantias = 0
        var agendamentos = 0
        var cancelamentos = 0

        override fun garantirDiaria() {
            garantias++
        }

        override fun agendarDiaria() {
            agendamentos++
        }

        override fun cancelarDiaria() {
            cancelamentos++
        }
    }

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
            repositorioProcessos = object : ProcessosRepository {
                override fun observarProcessos(): Flow<List<ProcessoMonitorado>> =
                    kotlinx.coroutines.flow.flowOf(emptyList())
                override fun observarMovimentos(numeroProcesso: String): Flow<List<MovimentoProcesso>> = kotlinx.coroutines.flow.emptyFlow()
                override fun observarParticipantes(numeroProcesso: String): Flow<List<ParticipanteProcesso>> = kotlinx.coroutines.flow.emptyFlow()
                override suspend fun obterProcesso(numeroProcesso: String): ProcessoMonitorado? = null
                override suspend fun salvarProcesso(processo: ProcessoMonitorado) = Unit
                override suspend fun excluirProcesso(numeroProcesso: String) = Unit
            },
        )

    private object TextosFake : TextosAutenticacao {
        override fun get(resId: Int): String =
            when (resId) {
                R.string.autenticacao_error_required_fields -> "required"
                R.string.autenticacao_error_login_email -> "login"
                R.string.autenticacao_sucesso_login -> "success"
                R.string.autenticacao_error_login_google -> "google-falha"
                R.string.autenticacao_error_google_sem_conta -> "google-sem-conta"
                R.string.autenticacao_error_politicas -> "politicas"
                R.string.autenticacao_sucesso_cadastro -> "cadastro"
                else -> resId.toString()
            }

        override fun get(resId: Int, vararg args: Any): String = get(resId)
    }

    private class AuthRepositoryFake(
        private val resultadoEmail: Result<AuthUser> = Result.success(usuarioAnonimo),
        private val resultadoGoogle: Result<AuthUser> = Result.success(usuarioAnonimo),
    ) : AuthRepository {
        private val usuarioAtual = MutableStateFlow<AuthUser?>(usuarioAnonimo)
        var loginEmailChamadas = 0
            private set
        var ultimoEmail: String? = null
            private set
        var ultimoIdTokenGoogle: String? = null
            private set

        override val currentUser: Flow<AuthUser?> = usuarioAtual

        override suspend fun linkWithGoogle(idToken: String): Result<AuthUser> {
            ultimoIdTokenGoogle = idToken
            resultadoGoogle.getOrNull()?.let { usuarioAtual.value = it }
            return resultadoGoogle
        }

        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
            loginEmailChamadas += 1
            ultimoEmail = email
            resultadoEmail.getOrNull()?.let { usuarioAtual.value = it }
            return resultadoEmail
        }

        var cadastroEmailChamadas = 0
            private set

        override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> {
            cadastroEmailChamadas += 1
            return signInWithEmail(email, password)
        }

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun updatePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun signOut() {
            usuarioAtual.value = null
        }

        override suspend fun temProvedorSenha(): Boolean = true

        override suspend fun reautenticarComSenha(senha: String): Result<Unit> = Result.success(Unit)

        override suspend fun excluirConta(): Result<Unit> = Result.success(Unit)

        private companion object {
            val usuarioAnonimo = AuthUser(
                uid = "usuario-anonimo",
                email = null,
                isAnonymous = true,
            )
        }
    }

    private class CredencialGoogleFake(
        private val resultado: ResultadoCredencialGoogle,
    ) : CredencialGoogleProvider {
        var chamadas = 0
            private set

        var limpezas = 0
            private set

        override suspend fun obterIdToken(contextoDaTela: android.content.Context?): ResultadoCredencialGoogle {
            chamadas += 1
            return resultado
        }

        override suspend fun limparEstadoDeCredencial() {
            limpezas += 1
        }
    }

    private class CadastroOabRepositoryFake : CadastroOabRepository {
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


    private class PerfilPreferencesRepositoryFake : PerfilPreferencesRepository {
        override val preferencias = MutableStateFlow(PerfilPreferences())
        override suspend fun saveIntervaloBuscaDias(dias: Int) = Unit
        override suspend fun saveSincronizacaoAutomatica(ativo: Boolean) = Unit
        override suspend fun saveNotificarPublicacoes(ativo: Boolean) = Unit
        override suspend fun saveNotificarPrazosUrgentes(ativo: Boolean) = Unit
        override suspend fun saveNotificarMovimentacoes(ativo: Boolean) = Unit
        override suspend fun saveTema(tema: TipoTema) = Unit
        override suspend fun saveApenasPorNome(ativo: Boolean) = Unit

        override suspend fun saveSincronizacaoNuvemAutorizada(ativo: Boolean) = Unit
        override suspend fun savePushPreChecagemAutorizada(ativo: Boolean) = Unit
    }

    private class SessaoLocalRepositoryFake : com.obiterjus.data.settings.SessaoLocalRepository {
        override suspend fun prepararPara(uid: String) = Unit

        override suspend fun limparTudo() = Unit
    }

    private class SincronizacaoRepositoryFake : com.obiterjus.domain.repository.SincronizacaoRepository {
        override suspend fun nuvemAutorizada(): Boolean = false

        override suspend fun enviarTudo(userId: String): com.obiterjus.domain.model.SincronizacaoNuvemResumo =
            com.obiterjus.domain.model.SincronizacaoNuvemResumo()

        override suspend fun restaurarTudo(userId: String): com.obiterjus.domain.model.SincronizacaoNuvemResumo =
            com.obiterjus.domain.model.SincronizacaoNuvemResumo()

        var enviarPerfilChamadas = 0
            private set

        override suspend fun enviarPerfil(userId: String): Result<Unit> {
            enviarPerfilChamadas += 1
            return Result.success(Unit)
        }

        override suspend fun restaurarPerfil(userId: String): Result<Unit> = Result.success(Unit)

        override suspend fun excluirTudo(userId: String): Result<Unit> = Result.success(Unit)
    }
}
