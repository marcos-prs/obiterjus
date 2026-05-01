package com.obiterjus.data.datajud

import com.obiterjus.domain.model.ProcessoDataJudSyncResultado
import com.obiterjus.domain.model.ProcessoDataJudSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository

class DisabledDataJudRepository(
    private val reason: String = "DataJud ainda nao configurado: informe DATAJUD_API_KEY.",
) : DataJudRepository {
    override suspend fun sincronizar(
        params: SincronizarProcessosDataJudParams,
    ): SincronizarProcessosDataJudResumo {
        val resultados = params.processos.map { request ->
            ProcessoDataJudSyncResultado(
                numeroProcesso = request.numeroProcesso,
                tribunal = request.tribunal,
                status = ProcessoDataJudSyncStatus.FAILED,
                movimentosSalvos = 0,
                mensagem = reason,
            )
        }

        return SincronizarProcessosDataJudResumo(
            solicitados = params.processos.size,
            normalizados = params.processos.size,
            encontrados = 0,
            naoEncontrados = 0,
            falhas = resultados.size,
            movimentosSalvos = 0,
            resultados = resultados,
        )
    }
}
