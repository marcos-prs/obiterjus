package com.obiterjus.data.datajud

import com.obiterjus.core.config.AppConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.core.config.AppConfigSource
import com.obiterjus.data.datajud.local.MovimentoDao
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoDao
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.ProcessoDataJudSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DisabledDataJudRepositoryTest {
    @Test
    fun returnsFailureSummaryWithoutCallingNetwork() = runBlocking {
        val repository = ConfiguredDataJudRepository(
            appConfigRepository = FakeAppConfigRepository(
                AppConfig(
                    dataJudEnabled = false,
                    dataJudApiKey = "",
                    source = AppConfigSource.FALLBACK,
                ),
            ),
            localProcessoRepository = LocalProcessoRepository(
                processoDao = FakeProcessoDao(),
                movimentoDao = FakeMovimentoDao(),
                participanteDao = FakeParticipanteDao(),
            ),
        )

        val resumo = repository.sincronizar(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = "50110879520258130245",
                        tribunal = "TJMG",
                    ),
                ),
            ),
        )

        assertEquals(1, resumo.solicitados)
        assertEquals(0, resumo.encontrados)
        assertEquals(0, resumo.falhas)
        assertEquals(1, resumo.salvosPendentes)
        assertEquals(ProcessoDataJudSyncStatus.PENDING, resumo.resultados.first().status)
    }

    private class FakeAppConfigRepository(
        private val appConfig: AppConfig,
    ) : AppConfigRepository {
        override val config: Flow<AppConfig> = MutableStateFlow(appConfig)

        override suspend fun current(): AppConfig = appConfig

        override suspend fun refresh(): AppConfig = appConfig
    }

    private class FakeProcessoDao : ProcessoDao {
        override suspend fun upsert(processo: ProcessoEntity) = Unit
        override suspend fun upsertAll(processos: List<ProcessoEntity>) = Unit
        override fun observeAll(): Flow<List<ProcessoEntity>> = emptyFlow()
        override fun observeByNumero(numeroProcesso: String): Flow<ProcessoEntity?> = emptyFlow()
        override suspend fun getByNumero(numeroProcesso: String): ProcessoEntity? = null
        override suspend fun getByNumeros(numerosProcesso: List<String>): List<ProcessoEntity> = emptyList()
        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) = Unit
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
