package com.obiterjus.data.datajud

import com.obiterjus.data.datajud.local.MovimentoDao
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.datajud.remote.DataJudApi
import com.obiterjus.data.datajud.remote.DataJudRemoteDataSource
import com.obiterjus.data.datajud.remote.dto.DataJudSearchResponseDto
import com.obiterjus.data.datajud.remote.dto.DataJudTotalDto
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoDao
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DataJudRepositoryImplTeimosinhaTest {
    @Test
    fun notFoundDecrementaTentativasRestantes() = runBlocking {
        val processoDao = FakeProcessoDao(
            processos = mapOf(
                "50110879520258130245" to processo(
                    numeroProcesso = "50110879520258130245",
                    status = ProcessoSyncStatus.NOT_FOUND,
                    tentativasRestantes = 2,
                ),
            ),
        )
        val repository = DataJudRepositoryImpl(
            remoteDataSource = remoteDataSource(),
            localProcessoRepository = LocalProcessoRepository(
                processoDao = processoDao,
                movimentoDao = FakeMovimentoDao(),
                participanteDao = FakeParticipanteDao(),
            ),
            clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC),
        )

        repository.sincronizar(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = "50110879520258130245",
                        tribunal = "TJMG",
                    ),
                ),
            ),
        )

        assertEquals(1, processoDao.saved["50110879520258130245"]?.dataJudTentativasRestantes)
        assertEquals(ProcessoSyncStatus.NOT_FOUND, processoDao.saved["50110879520258130245"]?.syncStatus)
    }

    @Test
    fun failureDecrementaTentativasRestantesAteZero() = runBlocking {
        val processoDao = FakeProcessoDao(
            processos = mapOf(
                "50110879520258130245" to processo(
                    numeroProcesso = "50110879520258130245",
                    status = ProcessoSyncStatus.FAILED,
                    tentativasRestantes = 1,
                ),
            ),
        )
        val repository = DataJudRepositoryImpl(
            remoteDataSource = remoteDataSource(shouldThrow = true),
            localProcessoRepository = LocalProcessoRepository(
                processoDao = processoDao,
                movimentoDao = FakeMovimentoDao(),
                participanteDao = FakeParticipanteDao(),
            ),
            clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC),
        )

        repository.sincronizar(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = "50110879520258130245",
                        tribunal = "TJMG",
                    ),
                ),
            ),
        )

        assertEquals(0, processoDao.saved["50110879520258130245"]?.dataJudTentativasRestantes)
        assertEquals(ProcessoSyncStatus.FAILED, processoDao.saved["50110879520258130245"]?.syncStatus)
    }

    private fun remoteDataSource(
        shouldThrow: Boolean = false,
    ): DataJudRemoteDataSource =
        DataJudRemoteDataSource(
            api = object : DataJudApi {
                override suspend fun buscarProcesso(
                    endpoint: String,
                    authorization: String,
                    request: com.obiterjus.data.datajud.remote.dto.DataJudSearchRequestDto,
                ): DataJudSearchResponseDto {
                    if (shouldThrow) throw RuntimeException("boom")
                    return DataJudSearchResponseDto(
                        hits = com.obiterjus.data.datajud.remote.dto.DataJudHitsDto(
                            total = DataJudTotalDto(value = 0),
                            hits = emptyList(),
                        ),
                    )
                }
            },
            apiKey = "token",
        )

    private fun processo(
        numeroProcesso: String,
        status: ProcessoSyncStatus,
        tentativasRestantes: Int,
    ): ProcessoEntity =
        ProcessoEntity(
            numeroProcesso = numeroProcesso,
            tribunal = "TJMG",
            grau = null,
            classeCodigo = null,
            classeNome = null,
            assuntosJson = null,
            orgaoJulgadorCodigo = null,
            orgaoJulgadorNome = null,
            nivelSigilo = null,
            dataAjuizamento = null,
            syncStatus = status,
            capturadoEm = Instant.EPOCH,
            atualizadoEm = Instant.EPOCH,
            dataJudTentativasRestantes = tentativasRestantes,
        )

    private class FakeProcessoDao(
        val processos: Map<String, ProcessoEntity>,
    ) : ProcessoDao {
        val saved = processos.toMutableMap()

        override suspend fun upsert(processo: ProcessoEntity) {
            saved[processo.numeroProcesso] = processo
        }

        override suspend fun upsertAll(processos: List<ProcessoEntity>) {
            processos.forEach { saved[it.numeroProcesso] = it }
        }

        override fun observeAll(): Flow<List<ProcessoEntity>> = emptyFlow()
        override fun observeByNumero(numeroProcesso: String): Flow<ProcessoEntity?> = emptyFlow()
        override suspend fun getByNumero(numeroProcesso: String): ProcessoEntity? = saved[numeroProcesso]
        override suspend fun getByNumeros(numerosProcesso: List<String>): List<ProcessoEntity> =
            numerosProcesso.mapNotNull(saved::get)

        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) {
            saved.remove(numeroProcesso)
        }
    }

    private class FakeMovimentoDao : MovimentoDao() {
        override suspend fun upsertAll(movimentos: List<MovimentoEntity>) = Unit
        override fun observeByProcesso(numeroProcesso: String): Flow<List<MovimentoEntity>> = emptyFlow()
        override suspend fun getByProcesso(numeroProcesso: String): List<MovimentoEntity> = emptyList()
        override suspend fun getByIds(ids: List<String>): List<MovimentoEntity> = emptyList()
        override suspend fun replaceForProcesso(numeroProcesso: String, movimentos: List<MovimentoEntity>) = Unit
        override suspend fun deleteByProcesso(numeroProcesso: String) = Unit
    }

    private class FakeParticipanteDao : ParticipanteDao {
        override suspend fun upsertAll(participantes: List<ParticipanteEntity>) = Unit
        override fun observeByNumeroProcesso(numeroProcesso: String): Flow<List<ParticipanteEntity>> = emptyFlow()
        override fun observeAll(): Flow<List<ParticipanteEntity>> = emptyFlow()
        override suspend fun getByNumeroProcesso(numeroProcesso: String): List<ParticipanteEntity> = emptyList()
        override suspend fun getAll(): List<ParticipanteEntity> = emptyList()
        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) = Unit
    }
}
