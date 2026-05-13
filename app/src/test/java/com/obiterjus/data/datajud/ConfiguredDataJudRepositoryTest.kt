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
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConfiguredDataJudRepositoryTest {
    @Test
    fun refreshesConfigBeforeDecidingIfSyncShouldRun() = runBlocking {
        val currentConfig = AppConfig(
            dataJudEnabled = false,
            dataJudApiKey = "",
            source = AppConfigSource.CACHED,
        )
        val refreshedConfig = AppConfig(
            dataJudEnabled = true,
            dataJudApiKey = " token-refreshado ",
            dataJudBaseUrl = "https://datajud.exemplo/",
            source = AppConfigSource.REMOTE,
        )
        val appConfigRepository = FakeAppConfigRepository(
            currentConfig = currentConfig,
            refreshedConfig = refreshedConfig,
        )
        var configRecebidaPelaFactory: AppConfig? = null
        val resultadoEsperado = SincronizarProcessosDataJudResumo(
            solicitados = 1,
            normalizados = 1,
            encontrados = 1,
            naoEncontrados = 0,
            falhas = 0,
            movimentosSalvos = 0,
            resultados = emptyList(),
        )

        val repository = ConfiguredDataJudRepository(
            appConfigRepository = appConfigRepository,
            localProcessoRepository = LocalProcessoRepository(
                processoDao = FakeProcessoDao(),
                movimentoDao = FakeMovimentoDao(),
                participanteDao = FakeParticipanteDao(),
            ),
            clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), java.time.ZoneOffset.UTC),
            dataJudRepositoryFactory = { config, _, _ ->
                configRecebidaPelaFactory = config
                object : DataJudRepository {
                    override suspend fun sincronizar(
                        params: SincronizarProcessosDataJudParams,
                    ): SincronizarProcessosDataJudResumo {
                        assertEquals(1, params.processos.size)
                        assertEquals("50110879520258130245", params.processos.first().numeroProcesso)
                        return resultadoEsperado
                    }
                }
            },
        )

        val resposta = repository.sincronizar(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = "50110879520258130245",
                        tribunal = "TJMG",
                    ),
                ),
            ),
        )

        assertEquals(0, appConfigRepository.currentCalls)
        assertEquals(1, appConfigRepository.refreshCalls)
        assertNotNull(configRecebidaPelaFactory)
        assertEquals("token-refreshado", configRecebidaPelaFactory?.dataJudApiKey)
        assertEquals("https://datajud.exemplo/", configRecebidaPelaFactory?.dataJudBaseUrl)
        assertEquals(resultadoEsperado, resposta)
    }

    private class FakeAppConfigRepository(
        private val currentConfig: AppConfig,
        private val refreshedConfig: AppConfig,
    ) : AppConfigRepository {
        var currentCalls = 0
        var refreshCalls = 0

        override val config: Flow<AppConfig> = MutableStateFlow(currentConfig)

        override suspend fun current(): AppConfig {
            currentCalls += 1
            return currentConfig
        }

        override suspend fun refresh(): AppConfig {
            refreshCalls += 1
            return refreshedConfig
        }
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
