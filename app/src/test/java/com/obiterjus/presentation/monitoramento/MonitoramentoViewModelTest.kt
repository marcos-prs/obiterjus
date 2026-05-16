package com.obiterjus.presentation.monitoramento

import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.SincronizacaoNuvemResumo
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.DataJudRepository
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.ProcessosRepository
import com.obiterjus.domain.repository.PublicacoesRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import com.obiterjus.domain.usecase.ExportarRelatorioUC
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import com.obiterjus.domain.usecase.MonitorarDjenUseCase
import com.obiterjus.domain.usecase.SincronizarProcessosDataJudUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoramentoViewModelTest {
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
    fun invalidDateShowsErrorWithoutCallingUseCase() = runTest {
        val djenRepository = FakeDjenRepository()
        val viewModel = MonitoramentoViewModel(
            monitorarCnjUseCase = monitorarCnjUseCase(djenRepository),
            authRepository = FakeAuthRepository(),
            repositorioSincronizacao = FakeSincronizacaoRepository(),
            repositorioCadastroOab = FakeCadastroOabRepository(),
            exportarRelatorioUC = ExportarRelatorioUC(FakePublicacoesRepository()),
        )

        viewModel.onNumeroOabChange("12345")
        viewModel.onUfOabChange("MG")
        viewModel.onDataInicioChange("sem-data")
        viewModel.onDataFimChange("2026-04-29")
        viewModel.sincronizar()

        assertEquals(MonitoramentoUiError.InvalidDate, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, djenRepository.calls)
    }

    @Test
    fun ufInputKeepsOnlyFirstTwoLettersUppercased() {
        val viewModel = MonitoramentoViewModel(
            monitorarCnjUseCase = monitorarCnjUseCase(FakeDjenRepository()),
            authRepository = FakeAuthRepository(),
            repositorioSincronizacao = FakeSincronizacaoRepository(),
            repositorioCadastroOab = FakeCadastroOabRepository(),
            exportarRelatorioUC = ExportarRelatorioUC(FakePublicacoesRepository()),
        )

        viewModel.onUfOabChange(" m-g ")

        assertEquals("MG", viewModel.uiState.value.ufOab)
    }

    @Test
    fun savedStartDateIsLoadedAndSavedEndDateIsIgnoredWhenLoadingCadastro() = runTest {
        val viewModel = MonitoramentoViewModel(
            monitorarCnjUseCase = monitorarCnjUseCase(FakeDjenRepository()),
            authRepository = FakeAuthRepository(),
            repositorioSincronizacao = FakeSincronizacaoRepository(),
            repositorioCadastroOab = FakeCadastroOabRepository(
                initialCadastro = OabCadastro(
                    numero = "12345",
                    uf = "MG",
                    nomeAdvogado = "Advogada Teste",
                    dataInicio = LocalDate.of(2026, 4, 1),
                    dataFim = LocalDate.of(2026, 4, 30),
                ),
            ),
            exportarRelatorioUC = ExportarRelatorioUC(FakePublicacoesRepository()),
            clock = Clock.fixed(
                Instant.parse("2026-05-07T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo"),
            ),
            ioDispatcher = dispatcher,
        )

        dispatcher.scheduler.runCurrent()

        assertEquals("01/04/2026", viewModel.uiState.value.dataInicio)
        assertEquals("07/05/2026", viewModel.uiState.value.dataFim)
    }

    @Test
    fun syncUsesTodayAsEndDateEvenIfUiStateHasStaleEndDate() = runTest {
        val djenRepository = FakeDjenRepository()
        val cadastroRepository = FakeCadastroOabRepository()
        val viewModel = MonitoramentoViewModel(
            monitorarCnjUseCase = monitorarCnjUseCase(djenRepository),
            authRepository = FakeAuthRepository(),
            repositorioSincronizacao = FakeSincronizacaoRepository(),
            repositorioCadastroOab = cadastroRepository,
            exportarRelatorioUC = ExportarRelatorioUC(FakePublicacoesRepository()),
            clock = Clock.fixed(
                Instant.parse("2026-05-07T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo"),
            ),
            ioDispatcher = dispatcher,
        )

        viewModel.onNumeroOabChange("12345")
        viewModel.onUfOabChange("MG")
        viewModel.onDataInicioChange("01/04/2026")
        viewModel.onDataFimChange("30/04/2026")
        viewModel.sincronizar()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("07/05/2026", viewModel.uiState.value.dataFim)
        assertEquals(LocalDate.of(2026, 4, 1), djenRepository.lastParams?.dataInicio)
        assertEquals(LocalDate.of(2026, 5, 7), djenRepository.lastParams?.dataFim)
        assertEquals(LocalDate.of(2026, 4, 1), cadastroRepository.cadastro.value.dataInicio)
        assertEquals(null, cadastroRepository.cadastro.value.dataFim)
    }

    private fun monitorarCnjUseCase(
        djenRepository: DjenRepository,
    ): MonitorarCnjUseCase =
        MonitorarCnjUseCase(
            monitorarDjenUseCase = MonitorarDjenUseCase(djenRepository),
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

    private class FakeDjenRepository : DjenRepository {
        var calls = 0
        var lastParams: MonitorarDjenParams? = null

        override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
            calls += 1
            lastParams = params
            return MonitorarDjenResumo(
                totalRemoto = 0,
                totalRecebidas = 0,
                novas = 0,
                atualizadas = 0,
                sigilosas = 0,
                processosNovos = emptyList(),
                processosParaSincronizar = emptyList(),
                paginasConsultadas = 0,
                motivoParada = MonitorarDjenStopReason.EMPTY_PAGE,
                falhas = emptyList(),
            )
        }
    }

    private class FakeAuthRepository : AuthRepository {
        private val user = AuthUser(
            uid = "usuario-teste",
            email = null,
            isAnonymous = true,
        )

        override val currentUser: Flow<AuthUser?> = MutableStateFlow(user)

        override suspend fun signInAnonymously(): Result<AuthUser> = Result.success(user)

        override suspend fun linkWithGoogle(idToken: String): Result<AuthUser> = Result.success(user)

        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
            Result.success(user.copy(email = email, isAnonymous = false))

        override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> =
            Result.success(user.copy(email = email, isAnonymous = false))

        override suspend fun updatePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun signOut() = Unit
    }

    private class FakeSincronizacaoRepository : SincronizacaoRepository {
        override suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo =
            SincronizacaoNuvemResumo()

        override suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo =
            SincronizacaoNuvemResumo()

        override suspend fun enviarPerfil(userId: String): Result<Unit> = Result.success(Unit)

        override suspend fun restaurarPerfil(userId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCadastroOabRepository(
        initialCadastro: OabCadastro = OabCadastro(),
    ) : CadastroOabRepository {
        override val cadastro = MutableStateFlow(initialCadastro)
        override val status = MutableStateFlow(SincronizacaoStatus())

        override suspend fun salvarCadastro(
            numero: String,
            uf: String,
            nomeAdvogado: String?,
            tipoInscricao: String?,
            nomeEscritorio: String?,
            areasAtuacao: List<String>?,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
        ) {
            cadastro.value = OabCadastro(
                numero = numero,
                uf = uf,
                nomeAdvogado = nomeAdvogado.orEmpty(),
                tipoInscricao = tipoInscricao.orEmpty(),
                nomeEscritorio = nomeEscritorio.orEmpty(),
                areasAtuacao = areasAtuacao.orEmpty(),
                dataInicio = dataInicio,
                dataFim = dataFim,
            )
        }

        override suspend fun registrarSucesso(
            executadoEm: Instant,
            novasPublicacoes: Int,
        ) {
            status.value = SincronizacaoStatus(
                ultimaExecucaoEm = executadoEm,
                ultimoSucessoEm = executadoEm,
                novasPublicacoesUltimaExecucao = novasPublicacoes,
            )
        }

        override suspend fun registrarFalha(
            executadoEm: Instant,
            mensagem: String,
        ) {
            status.value = SincronizacaoStatus(
                ultimaExecucaoEm = executadoEm,
                ultimaFalha = mensagem,
            )
        }
    }


    private class FakePublicacoesRepository : PublicacoesRepository {
        private val publicacoes = MutableStateFlow(emptyList<Publicacao>())

        override fun observarPublicacoes(): Flow<List<Publicacao>> = publicacoes

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            publicacoes

        override fun observarPublicacao(id: Long): Flow<Publicacao?> =
            publicacoes.map { itens -> itens.firstOrNull { it.id == id } }
    }
}
