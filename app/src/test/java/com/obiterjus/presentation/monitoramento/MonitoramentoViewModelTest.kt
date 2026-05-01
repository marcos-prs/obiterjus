package com.obiterjus.presentation.monitoramento

import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.SincronizacaoNuvemResumo
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.DataJudRepository
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.repository.RepositorioPublicacoes
import com.obiterjus.data.settings.SyncPreferencesRepository
import com.obiterjus.domain.repository.RepositorioSincronizacao
import com.obiterjus.domain.usecase.ExportarRelatorioUC
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import com.obiterjus.domain.usecase.MonitorarDjenUseCase
import com.obiterjus.domain.usecase.SincronizarProcessosDataJudUseCase
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
            repositorioSincronizacao = FakeRepositorioSincronizacao(),
            repositorioCadastroOab = FakeRepositorioCadastroOab(),
            syncPreferencesRepository = FakeSyncPreferencesRepository(),
            exportarRelatorioUC = ExportarRelatorioUC(FakeRepositorioPublicacoes()),
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
            repositorioSincronizacao = FakeRepositorioSincronizacao(),
            repositorioCadastroOab = FakeRepositorioCadastroOab(),
            syncPreferencesRepository = FakeSyncPreferencesRepository(),
            exportarRelatorioUC = ExportarRelatorioUC(FakeRepositorioPublicacoes()),
        )

        viewModel.onUfOabChange(" m-g ")

        assertEquals("MG", viewModel.uiState.value.ufOab)
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
        )

    private class FakeDjenRepository : DjenRepository {
        var calls = 0

        override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
            calls += 1
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

        override suspend fun signOut() = Unit
    }

    private class FakeRepositorioSincronizacao : RepositorioSincronizacao {
        override suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo =
            SincronizacaoNuvemResumo()

        override suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo =
            SincronizacaoNuvemResumo()
    }

    private class FakeRepositorioCadastroOab : RepositorioCadastroOab {
        override val cadastro = MutableStateFlow(OabCadastro())
        override val status = MutableStateFlow(SincronizacaoStatus())

        override suspend fun salvarCadastro(
            numero: String,
            uf: String,
            nomeAdvogado: String,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
        ) {
            cadastro.value = OabCadastro(
                numero = numero,
                uf = uf,
                nomeAdvogado = nomeAdvogado,
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

    private class FakeSyncPreferencesRepository : SyncPreferencesRepository {
        private val _syncFrequencyHours = MutableStateFlow(24)
        override val syncFrequencyHours: Flow<Int> = _syncFrequencyHours

        override suspend fun saveSyncFrequencyHours(hours: Int) {
            _syncFrequencyHours.value = hours
        }
    }

    private class FakeRepositorioPublicacoes : RepositorioPublicacoes {
        private val publicacoes = MutableStateFlow(emptyList<Publicacao>())

        override fun observarPublicacoes(): Flow<List<Publicacao>> = publicacoes

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            publicacoes
    }
}
