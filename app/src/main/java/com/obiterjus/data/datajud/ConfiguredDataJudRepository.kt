package com.obiterjus.data.datajud

import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.core.config.AppConfig
import com.obiterjus.data.datajud.remote.DataJudRemoteDataSource
import com.obiterjus.data.datajud.remote.DataJudRetrofitFactory
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoDataJudSyncResultado
import com.obiterjus.domain.model.ProcessoDataJudSyncStatus
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository
import java.time.Clock

class ConfiguredDataJudRepository(
    private val appConfigRepository: AppConfigRepository,
    private val localProcessoRepository: LocalProcessoRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val dataJudRepositoryFactory: (
        AppConfig,
        LocalProcessoRepository,
        Clock,
    ) -> DataJudRepository = { config, processoRepository, now ->
        DataJudRepositoryImpl(
            remoteDataSource = DataJudRemoteDataSource(
                api = DataJudRetrofitFactory.createApi(
                    baseUrl = config.dataJudBaseUrl,
                    timeoutSeconds = config.requestTimeoutSeconds,
                ),
                apiKey = config.dataJudApiKey.trim(),
            ),
            localProcessoRepository = processoRepository,
            clock = now,
        )
    },
) : DataJudRepository {
    override suspend fun sincronizar(
        params: SincronizarProcessosDataJudParams,
    ): SincronizarProcessosDataJudResumo {
        val config = appConfigRepository.refresh()
        val apiKey = config.dataJudApiKey.trim()
        if (!config.dataJudEnabled || apiKey.isBlank()) {
            return salvarComoPendentes(params)
        }

        return dataJudRepositoryFactory(
            config.copy(dataJudApiKey = apiKey),
            localProcessoRepository,
            clock,
        ).sincronizar(params)
    }

    private suspend fun salvarComoPendentes(
        params: SincronizarProcessosDataJudParams,
    ): SincronizarProcessosDataJudResumo {
        val now = clock.instant()
        val resultados = params.processos.map { request ->
            localProcessoRepository.upsertProcesso(
                ProcessoEntity(
                    numeroProcesso = request.numeroProcesso,
                    tribunal = request.tribunal,
                    grau = null,
                    classeCodigo = null,
                    classeNome = null,
                    assuntosJson = null,
                    orgaoJulgadorCodigo = null,
                    orgaoJulgadorNome = null,
                    nivelSigilo = null,
                    dataAjuizamento = null,
                    syncStatus = ProcessoSyncStatus.PENDING,
                    capturadoEm = now,
                    atualizadoEm = now,
                    dataJudTentativasRestantes = 0,
                ),
            )
            ProcessoDataJudSyncResultado(
                numeroProcesso = request.numeroProcesso,
                tribunal = request.tribunal,
                status = ProcessoDataJudSyncStatus.PENDING,
                movimentosSalvos = 0,
                mensagem = null,
            )
        }
        return SincronizarProcessosDataJudResumo(
            solicitados = params.processos.size,
            normalizados = params.processos.size,
            encontrados = 0,
            naoEncontrados = 0,
            falhas = 0,
            movimentosSalvos = 0,
            salvosPendentes = resultados.size,
            resultados = resultados,
        )
    }
}
