package com.obiterjus.data.datajud

import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.datajud.remote.DataJudRemoteDataSource
import com.obiterjus.data.datajud.remote.DataJudRetrofitFactory
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository
import java.time.Clock

class ConfiguredDataJudRepository(
    private val appConfigRepository: AppConfigRepository,
    private val localProcessoRepository: LocalProcessoRepository,
    private val clock: Clock = Clock.systemUTC(),
) : DataJudRepository {
    override suspend fun sincronizar(
        params: SincronizarProcessosDataJudParams,
    ): SincronizarProcessosDataJudResumo {
        val config = appConfigRepository.current()
        val apiKey = config.dataJudApiKey.trim()
        if (!config.dataJudEnabled || apiKey.isBlank()) {
            return DisabledDataJudRepository().sincronizar(params)
        }

        return DataJudRepositoryImpl(
            remoteDataSource = DataJudRemoteDataSource(
                api = DataJudRetrofitFactory.createApi(
                    baseUrl = config.dataJudBaseUrl,
                    timeoutSeconds = config.requestTimeoutSeconds,
                ),
                apiKey = apiKey,
            ),
            localProcessoRepository = localProcessoRepository,
            clock = clock,
        ).sincronizar(params)
    }
}
