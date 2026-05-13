package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo

class RessincronizarProcessoUseCase(
    private val sincronizar: SincronizarProcessosDataJudUseCase,
) {
    suspend operator fun invoke(numeroProcesso: String): SincronizarProcessosDataJudResumo =
        sincronizar(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest(numeroProcesso = numeroProcesso),
                ),
            ),
        )
}
