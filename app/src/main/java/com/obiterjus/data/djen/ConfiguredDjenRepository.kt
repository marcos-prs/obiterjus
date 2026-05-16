package com.obiterjus.data.djen

import com.obiterjus.core.config.AppConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.djen.mapper.DjenMapper
import com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper
import com.obiterjus.data.djen.remote.DjenRemoteDataSource
import com.obiterjus.data.djen.remote.DjenRetrofitFactory
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.ProcessosRepository
import java.time.Clock

class ConfiguredDjenRepository(
    private val appConfigRepository: AppConfigRepository,
    private val localPublicacaoRepository: LocalPublicacaoRepository,
    private val localProcessoRepository: ProcessosRepository,
    private val djenMapper: DjenMapper,
    private val publicacaoPrazoMapper: PublicacaoPrazoMapper,
    private val clock: Clock = Clock.systemUTC(),
) : DjenRepository {

    private var cachedRepository: DjenRepositoryImpl? = null
    private var cachedConfigKey: DjenConfigCacheKey? = null

    override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
        val config = appConfigRepository.refresh()

        if (!config.djenEnabled) {
            return disabledResumo()
        }

        return repositoryFor(config).monitorar(params)
    }

    private fun repositoryFor(config: AppConfig): DjenRepositoryImpl {
        val key = DjenConfigCacheKey(
            baseUrl = config.djenBaseUrl,
            timeoutSeconds = config.requestTimeoutSeconds,
            itensPorPagina = config.djenDefaultItemsPerPage,
        )
        if (key == cachedConfigKey) {
            return cachedRepository!!
        }
        val repository = DjenRepositoryImpl(
            remoteDataSource = DjenRemoteDataSource(
                api = DjenRetrofitFactory.createApi(
                    baseUrl = config.djenBaseUrl,
                    timeoutSeconds = config.requestTimeoutSeconds,
                    // sem okHttpClient: endpoint de consulta é público
                ),
                itensPorPagina = config.djenDefaultItemsPerPage,
            ),
            localPublicacaoRepository = localPublicacaoRepository,
            localProcessoRepository = localProcessoRepository,
            djenMapper = djenMapper,
            publicacaoPrazoMapper = publicacaoPrazoMapper,
            clock = clock,
        )
        cachedRepository = repository
        cachedConfigKey = key
        return repository
    }

    private fun disabledResumo() = MonitorarDjenResumo(
        totalRemoto = null,
        totalRecebidas = 0,
        novas = 0,
        atualizadas = 0,
        sigilosas = 0,
        processosNovos = emptyList(),
        processosParaSincronizar = emptyList(),
        paginasConsultadas = 0,
        motivoParada = MonitorarDjenStopReason.UNKNOWN,
        falhas = listOf("DJEN desabilitado pela configuração remota."),
    )

    private data class DjenConfigCacheKey(
        val baseUrl: String,
        val timeoutSeconds: Long,
        val itensPorPagina: Int,
    )
}
