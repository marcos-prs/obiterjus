package com.obiterjus.data.djen

import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.djen.mapper.DjenMapper
import com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper
import com.obiterjus.data.djen.remote.DjenApi
import com.obiterjus.data.djen.remote.DjenRemoteDataSource
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.repository.ProcessosRepository
import com.obiterjus.domain.usecase.CalcularPrazoRegraUC
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DjenRepositoryImplTest {

    private val calculadoraPrazos = CalculadoraPrazos(FakeCalendarioForenseDataSource())
    private val djenPrazoExtractor = DjenPrazoExtractor(calculadoraPrazos)
    private val calcularPrazoRegraUC = CalcularPrazoRegraUC(calculadoraPrazos, djenPrazoExtractor)
    private val djenMapper = DjenMapper()
    private val publicacaoPrazoMapper = PublicacaoPrazoMapper(calcularPrazoRegraUC)

    @Test
    fun returnsNewProcessesWithTribunalForDataJudSync() = runBlocking {
        val repository = DjenRepositoryImpl(
            remoteDataSource = DjenRemoteDataSource(
                api = FakeDjenApi(
                    response = DjenResponseDto(
                        count = 1,
                        items = listOf(
                            DjenComunicacaoDto(
                                id = 1L,
                                siglaTribunal = "TJMG",
                                numeroProcesso = "5011087-95.2025.8.13.0245",
                                texto = "Intime-se. OAB/MG 12345.",
                            ),
                        ),
                    ),
                ),
                itensPorPagina = 100,
            ),
            localPublicacaoRepository = LocalPublicacaoRepository(FakePublicacaoDao(existingIds = emptyList())),
            localProcessoRepository = FakeProcessoRepository(),
            djenMapper = djenMapper,
            publicacaoPrazoMapper = publicacaoPrazoMapper,
            clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC),
        )

        val resumo = repository.monitorar(params())

        assertEquals(listOf("50110879520258130245"), resumo.processosNovos)
        assertEquals(1, resumo.processosParaSincronizar.size)
        assertEquals("50110879520258130245", resumo.processosParaSincronizar.first().numeroProcesso)
        assertEquals("TJMG", resumo.processosParaSincronizar.first().tribunal)
    }

    @Test
    fun skipsDataJudForAlreadySyncedProcess() = runBlocking {
        val repository = DjenRepositoryImpl(
            remoteDataSource = DjenRemoteDataSource(
                api = FakeDjenApi(
                    response = DjenResponseDto(
                        count = 1,
                        items = listOf(
                            DjenComunicacaoDto(
                                id = 1L,
                                siglaTribunal = "TJMG",
                                numeroProcesso = "5011087-95.2025.8.13.0245",
                                texto = "Intime-se. OAB/MG 12345.",
                            ),
                        ),
                    ),
                ),
                itensPorPagina = 100,
            ),
            localPublicacaoRepository = LocalPublicacaoRepository(FakePublicacaoDao(existingIds = emptyList())),
            localProcessoRepository = FakeProcessoRepository(
                processos = mapOf(
                    "50110879520258130245" to processoLocal(
                        numeroProcesso = "50110879520258130245",
                        status = ProcessoSyncStatus.SYNCED,
                    ),
                ),
            ),
            djenMapper = djenMapper,
            publicacaoPrazoMapper = publicacaoPrazoMapper,
            clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC),
        )

        val resumo = repository.monitorar(params())

        assertTrue(resumo.processosNovos.isEmpty())
        assertTrue(resumo.processosParaSincronizar.isEmpty())
    }

    @Test
    fun publicacaoDeOutroTribunalDisparaResincronizacaoMesmoComProcessoSynced() = runBlocking {
        // Processo registrado como TJMG/SYNCED passa a publicar pelo STJ
        // (migração de instância via AREsp): o DataJud deve ser re-consultado
        // no índice do STJ, apesar do SYNCED.
        val repository = DjenRepositoryImpl(
            remoteDataSource = DjenRemoteDataSource(
                api = FakeDjenApi(
                    response = DjenResponseDto(
                        count = 1,
                        items = listOf(
                            DjenComunicacaoDto(
                                id = 2L,
                                siglaTribunal = "STJ",
                                numeroProcesso = "5011087-95.2025.8.13.0245",
                                texto = "Processo distribuído pelo sistema automático. OAB/MG 12345.",
                            ),
                        ),
                    ),
                ),
                itensPorPagina = 100,
            ),
            localPublicacaoRepository = LocalPublicacaoRepository(FakePublicacaoDao(existingIds = emptyList())),
            localProcessoRepository = FakeProcessoRepository(
                processos = mapOf(
                    "50110879520258130245" to processoLocal(
                        numeroProcesso = "50110879520258130245",
                        status = ProcessoSyncStatus.SYNCED,
                    ),
                ),
            ),
            djenMapper = djenMapper,
            publicacaoPrazoMapper = publicacaoPrazoMapper,
            clock = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneOffset.UTC),
        )

        val resumo = repository.monitorar(params())

        assertEquals(1, resumo.processosParaSincronizar.size)
        assertEquals("STJ", resumo.processosParaSincronizar.first().tribunal)
        assertEquals("50110879520258130245", resumo.processosParaSincronizar.first().numeroProcesso)
    }

    private fun params(): MonitorarDjenParams =
        MonitorarDjenParams(
            numeroOab = "12345",
            ufOab = "MG",
            dataInicio = LocalDate.of(2026, 4, 1),
            dataFim = LocalDate.of(2026, 4, 29),
            modo = MonitorarDjenModo.MANUAL,
        )

    private fun processoLocal(
        numeroProcesso: String,
        status: ProcessoSyncStatus,
    ): ProcessoMonitorado =
        ProcessoMonitorado(
            numeroProcesso = numeroProcesso,
            tribunal = "TJMG",
            grau = null,
            classeCodigo = null,
            classeNome = null,
            assuntos = emptyList(),
            orgaoJulgadorCodigo = null,
            orgaoJulgadorNome = null,
            nivelSigilo = null,
            dataAjuizamento = null,
            syncStatus = status,
            capturadoEm = Instant.EPOCH,
            atualizadoEm = Instant.EPOCH,
        )

    private class FakeDjenApi(
        private val response: DjenResponseDto,
    ) : DjenApi {
        override suspend fun buscarComunicacoes(
            numeroOab: String?,
            ufOab: String?,
            nomeAdvogado: String?,
            dataDisponibilizacaoInicio: String,
            dataDisponibilizacaoFim: String,
            pagina: Int,
            itensPorPagina: Int,
        ): DjenResponseDto = response

        override suspend fun baixarCertidao(hash: String): ResponseBody =
            ByteArray(0).toResponseBody()
    }

    private class FakeCalendarioForenseDataSource : CalendarioForenseDataSource {
        override suspend fun calcularPrazo(pedido: PedidoCalculoPrazo): RespostaPrazo =
            RespostaPrazo(estado = "INDISPONIVEL")
    }

    private class FakePublicacaoDao(
        private val existingIds: List<Long>,
    ) : PublicacaoDao {
        val saved = mutableListOf<PublicacaoEntity>()

        override suspend fun upsert(publicacao: PublicacaoEntity) {
            saved += publicacao
        }

        override suspend fun upsertAll(publicacoes: List<PublicacaoEntity>) {
            saved += publicacoes
        }

        override suspend fun getExistingIds(ids: List<Long>): List<Long> = existingIds

        override suspend fun getByHashes(hashes: List<String>): List<PublicacaoEntity> = emptyList()

        override suspend fun getById(id: Long): PublicacaoEntity? =
            saved.firstOrNull { it.id == id }

        override fun observeById(id: Long): Flow<PublicacaoEntity?> = emptyFlow()

        override suspend fun getByIds(ids: List<Long>): List<PublicacaoEntity> =
            saved.filter { it.id in ids }

        override fun observePublicacoes(
            numeroProcesso: String?,
            tribunal: String?,
            tipoComunicacao: String?,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
            somenteSigilosas: Boolean?,
        ): Flow<List<PublicacaoEntity>> = emptyFlow()

        override fun observePorProcesso(numeroProcesso: String): Flow<List<PublicacaoEntity>> = emptyFlow()

        override suspend fun getNumerosProcessoDistintos(): List<String> = emptyList()

        override suspend fun getByDataDisponibilizacao(data: LocalDate): List<PublicacaoEntity> =
            saved.filter { it.dataDisponibilizacao == data }

        override suspend fun atualizarStatusDuplicata(
            id: Long,
            duplicataDe: Long?,
            totalDuplicatas: Int,
        ) {
            val idx = saved.indexOfFirst { it.id == id }
            if (idx >= 0) {
                saved[idx] = saved[idx].copy(
                    duplicataDe = duplicataDe,
                    totalDuplicatas = totalDuplicatas,
                )
            }
        }
    }

    private class FakeProcessoRepository(
        private val processos: Map<String, ProcessoMonitorado> = emptyMap(),
    ) : ProcessosRepository {
        override fun observarProcessos(): Flow<List<ProcessoMonitorado>> =
            kotlinx.coroutines.flow.flowOf(emptyList())

        override fun observarMovimentos(numeroProcesso: String): Flow<List<MovimentoProcesso>> = emptyFlow()

        override fun observarParticipantes(numeroProcesso: String): Flow<List<ParticipanteProcesso>> = emptyFlow()

        override suspend fun obterProcesso(numeroProcesso: String): ProcessoMonitorado? =
            processos[numeroProcesso]

        override suspend fun salvarProcesso(processo: ProcessoMonitorado) = Unit

        override suspend fun excluirProcesso(numeroProcesso: String) = Unit
    }
}
