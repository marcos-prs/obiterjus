package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MonitorarCnjResumo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams

class MonitorarCnjUseCase(
    private val monitorarDjenUseCase: MonitorarDjenUseCase,
    private val sincronizarProcessosDataJudUseCase: SincronizarProcessosDataJudUseCase,
) {
    suspend operator fun invoke(params: MonitorarDjenParams): MonitorarCnjResumo {
        val djenResumo = monitorarDjenUseCase(params)
        val dataJudResumo = djenResumo.processosParaSincronizar
            .takeIf { it.isNotEmpty() }
            ?.let { processos ->
                sincronizarProcessosDataJudUseCase(
                    SincronizarProcessosDataJudParams(processos = processos),
                )
            }

        return MonitorarCnjResumo(
            djen = djenResumo,
            dataJud = dataJudResumo,
        )
    }
}
