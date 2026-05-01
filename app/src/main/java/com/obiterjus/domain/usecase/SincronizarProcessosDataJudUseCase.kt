package com.obiterjus.domain.usecase

import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository

class SincronizarProcessosDataJudUseCase(
    private val repository: DataJudRepository,
) {
    suspend operator fun invoke(
        params: SincronizarProcessosDataJudParams,
    ): SincronizarProcessosDataJudResumo {
        val normalizedRequests = params.processos
            .mapNotNull { request ->
                NumeroProcessoNormalizer.normalize(request.numeroProcesso)?.let { numero ->
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = numero,
                        tribunal = request.tribunal?.trim()?.takeIf { it.isNotEmpty() },
                    )
                }
            }
            .distinctBy { it.numeroProcesso to it.tribunal?.uppercase() }

        require(normalizedRequests.isNotEmpty()) {
            "Informe ao menos um número processual CNJ válido."
        }

        return repository.sincronizar(
            SincronizarProcessosDataJudParams(processos = normalizedRequests),
        )
    }
}
