package com.obiterjus.data.djen

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
import java.time.Clock

class ConfiguredDjenRepository(
    private val appConfigRepository: AppConfigRepository,
    private val localPublicacaoRepository: LocalPublicacaoRepository,
    private val djenMapper: DjenMapper,
    private val publicacaoPrazoMapper: PublicacaoPrazoMapper,
    private val clock: Clock = Clock.systemUTC(),
) : DjenRepository {
    override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
        val config = appConfigRepository.refresh()
        if (!config.djenEnabled) {
            return MonitorarDjenResumo(
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
        }

        val repository = DjenRepositoryImpl(
            remoteDataSource = DjenRemoteDataSource(
                api = DjenRetrofitFactory.createApi(
                    baseUrl = config.djenBaseUrl,
                    timeoutSeconds = config.requestTimeoutSeconds,
                ),
                itensPorPagina = config.djenDefaultItemsPerPage,
            ),
            localPublicacaoRepository = localPublicacaoRepository,
            djenMapper = djenMapper,
            publicacaoPrazoMapper = publicacaoPrazoMapper,
            clock = clock,
        )

        return repository.monitorar(params)
    }
}
