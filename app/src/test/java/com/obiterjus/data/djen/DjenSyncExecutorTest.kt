package com.obiterjus.data.djen

import com.obiterjus.core.config.AppConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.repository.DataJudRepository
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import com.obiterjus.domain.usecase.MonitorarDjenUseCase
import com.obiterjus.domain.usecase.SincronizarProcessosDataJudUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DjenSyncExecutorTest {
    private val clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun syncManualUsaModoManualERegistraHistorico() = runBlocking {
        val djenRepository = FakeDjenRepository(
            response = MonitorarDjenResumo(
                totalRemoto = 1,
                totalRecebidas = 1,
                novas = 1,
                atualizadas = 0,
                sigilosas = 0,
                processosNovos = emptyList(),
                processosParaSincronizar = emptyList(),
                paginasConsultadas = 1,
                motivoParada = MonitorarDjenStopReason.PARTIAL_PAGE,
                falhas = emptyList(),
            ),
        )
        val cadastroRepository = FakeRepositorioCadastroOab()
        val logDao = FakeSyncLogDao()
        val executor = DjenSyncExecutorImpl(
            appConfigRepository = FakeAppConfigRepository(),
            repositorioCadastroOab = cadastroRepository,
            monitorarCnjUseCase = monitorarCnjUseCase(djenRepository),
            syncLogDao = logDao,
            clock = clock,
            perfilPreferencesRepository = FakePerfilPreferencesRepository(),
        )

        val resumo = executor.executar(MonitorarDjenModo.MANUAL)

        assertEquals(MonitorarDjenModo.MANUAL, djenRepository.lastParams?.modo)
        assertEquals(LocalDate.of(2026, 4, 21), djenRepository.lastParams?.dataInicio)
        assertEquals(LocalDate.of(2026, 5, 6), djenRepository.lastParams?.dataFim)
        assertEquals(1, resumo.djen.novas)
        assertEquals(1, logDao.logs.size)
        assertEquals(true, logDao.logs.single().sucesso)
        assertEquals(1, cadastroRepository.status.value.novasPublicacoesUltimaExecucao)
        assertEquals(clock.instant(), cadastroRepository.status.value.ultimoSucessoEm)
    }

    @Test
    fun syncManualRegistraFalhaQuandoDjenRetornaErro() = runBlocking {
        val djenRepository = FakeDjenRepository(
            response = MonitorarDjenResumo(
                totalRemoto = 0,
                totalRecebidas = 0,
                novas = 0,
                atualizadas = 0,
                sigilosas = 0,
                processosNovos = emptyList(),
                processosParaSincronizar = emptyList(),
                paginasConsultadas = 1,
                motivoParada = MonitorarDjenStopReason.EMPTY_PAGE,
                falhas = listOf("DJEN indisponivel"),
            ),
        )
        val cadastroRepository = FakeRepositorioCadastroOab()
        val logDao = FakeSyncLogDao()
        val executor = DjenSyncExecutorImpl(
            appConfigRepository = FakeAppConfigRepository(),
            repositorioCadastroOab = cadastroRepository,
            monitorarCnjUseCase = monitorarCnjUseCase(djenRepository),
            syncLogDao = logDao,
            clock = clock,
            perfilPreferencesRepository = FakePerfilPreferencesRepository(),
        )

        val erro = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                executor.executar(MonitorarDjenModo.MANUAL)
            }
        }

        assertEquals("DJEN indisponivel", erro.message)
        assertEquals(MonitorarDjenModo.MANUAL, djenRepository.lastParams?.modo)
        assertEquals(1, logDao.logs.size)
        assertFalse(logDao.logs.single().sucesso)
        assertEquals("DJEN indisponivel", logDao.logs.single().mensagemErro)
        assertEquals("DJEN indisponivel", cadastroRepository.status.value.ultimaFalha)
    }

    @Test
    fun syncManualCalculaJanelaNaZonaLocalDoApp() = runBlocking {
        val relogioBrasil = Clock.fixed(
            Instant.parse("2026-05-07T02:30:00Z"),
            ZoneId.of("America/Sao_Paulo"),
        )
        val djenRepository = FakeDjenRepository(
            response = MonitorarDjenResumo(
                totalRemoto = 0,
                totalRecebidas = 0,
                novas = 0,
                atualizadas = 0,
                sigilosas = 0,
                processosNovos = emptyList(),
                processosParaSincronizar = emptyList(),
                paginasConsultadas = 1,
                motivoParada = MonitorarDjenStopReason.EMPTY_PAGE,
                falhas = emptyList(),
            ),
        )
        val executor = DjenSyncExecutorImpl(
            appConfigRepository = FakeAppConfigRepository(),
            repositorioCadastroOab = FakeRepositorioCadastroOab(),
            monitorarCnjUseCase = monitorarCnjUseCase(djenRepository),
            syncLogDao = FakeSyncLogDao(),
            clock = relogioBrasil,
            perfilPreferencesRepository = FakePerfilPreferencesRepository(),
        )

        executor.executar(MonitorarDjenModo.MANUAL)

        assertEquals(LocalDate.of(2026, 5, 6), djenRepository.lastParams?.dataFim)
        assertEquals(LocalDate.of(2026, 4, 21), djenRepository.lastParams?.dataInicio)
    }

    @Test
    fun syncManualIgnoraDatasLegadasDoCadastroOab() = runBlocking {
        val djenRepository = FakeDjenRepository(
            response = MonitorarDjenResumo(
                totalRemoto = 0,
                totalRecebidas = 0,
                novas = 0,
                atualizadas = 0,
                sigilosas = 0,
                processosNovos = emptyList(),
                processosParaSincronizar = emptyList(),
                paginasConsultadas = 1,
                motivoParada = MonitorarDjenStopReason.EMPTY_PAGE,
                falhas = emptyList(),
            ),
        )
        val executor = DjenSyncExecutorImpl(
            appConfigRepository = FakeAppConfigRepository(),
            repositorioCadastroOab = FakeRepositorioCadastroOab(
                initialCadastro = OabCadastro(
                    numero = "12345",
                    uf = "MG",
                    nomeAdvogado = "Advogada Teste",
                    dataInicio = LocalDate.of(2026, 4, 1),
                    dataFim = LocalDate.of(2026, 4, 30),
                ),
            ),
            monitorarCnjUseCase = monitorarCnjUseCase(djenRepository),
            syncLogDao = FakeSyncLogDao(),
            clock = clock,
            perfilPreferencesRepository = FakePerfilPreferencesRepository(),
        )

        executor.executar(MonitorarDjenModo.MANUAL)

        assertEquals(LocalDate.of(2026, 4, 21), djenRepository.lastParams?.dataInicio)
        assertEquals(LocalDate.of(2026, 5, 6), djenRepository.lastParams?.dataFim)
    }

    private fun monitorarCnjUseCase(djenRepository: DjenRepository): MonitorarCnjUseCase =
        MonitorarCnjUseCase(
            monitorarDjenUseCase = MonitorarDjenUseCase(djenRepository),
            sincronizarProcessosDataJudUseCase = SincronizarProcessosDataJudUseCase(
                object : DataJudRepository {
                    override suspend fun sincronizar(
                        params: SincronizarProcessosDataJudParams,
                    ): SincronizarProcessosDataJudResumo =
                        SincronizarProcessosDataJudResumo(
                            solicitados = params.processos.size,
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

    private class FakeAppConfigRepository : AppConfigRepository {
        private val configState = MutableStateFlow(
            AppConfig(
                syncLookbackDays = 15,
                djenEnabled = true,
                dataJudEnabled = true,
            ),
        )

        override val config: Flow<AppConfig> = configState

        override suspend fun current(): AppConfig = configState.value

        override suspend fun refresh(): AppConfig = configState.value
    }

    private class FakeRepositorioCadastroOab(
        initialCadastro: OabCadastro =
            OabCadastro(
                numero = "12345",
                uf = "MG",
                nomeAdvogado = "Advogada Teste",
                dataInicio = null,
            ),
    ) : RepositorioCadastroOab {
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
            cadastro.value = cadastro.value.copy(
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
            status.value = status.value.copy(
                ultimaExecucaoEm = executadoEm,
                ultimoSucessoEm = executadoEm,
                ultimaFalha = null,
                novasPublicacoesUltimaExecucao = novasPublicacoes,
            )
        }

        override suspend fun registrarFalha(
            executadoEm: Instant,
            mensagem: String,
        ) {
            status.value = status.value.copy(
                ultimaExecucaoEm = executadoEm,
                ultimaFalha = mensagem,
                novasPublicacoesUltimaExecucao = 0,
            )
        }
    }

    private class FakeSyncLogDao : SyncLogDao {
        val logs = mutableListOf<SyncLogEntity>()
        var descartarChamadas = 0

        override suspend fun insert(log: SyncLogEntity) {
            logs += log
        }

        override fun observeRecentes(limite: Int): Flow<List<SyncLogEntity>> = emptyFlow()

        override suspend fun descartarAntigos(manter: Int) {
            descartarChamadas += 1
        }
    }

    private class FakeDjenRepository(
        private val response: MonitorarDjenResumo,
    ) : DjenRepository {
        var lastParams: MonitorarDjenParams? = null

        override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
            lastParams = params
            return response
        }
    }

    private class FakePerfilPreferencesRepository : PerfilPreferencesRepository {
        override val preferencias: Flow<com.obiterjus.data.settings.PerfilPreferences> = MutableStateFlow(com.obiterjus.data.settings.PerfilPreferences(intervaloBuscaDias = 15))

        override suspend fun saveIntervaloBuscaDias(dias: Int) {}
        override suspend fun saveSincronizacaoAutomatica(ativo: Boolean) {}
        override suspend fun saveNotificarPublicacoes(ativo: Boolean) {}
        override suspend fun saveNotificarPrazosUrgentes(ativo: Boolean) {}
        override suspend fun saveNotificarMovimentacoes(ativo: Boolean) {}
        override suspend fun saveTema(tema: com.obiterjus.ui.theme.TipoTema) {}
        override suspend fun saveApenasPorNome(ativo: Boolean) {}
    }
}
