package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MonitorarCnjResumo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.repository.RepositorioProcessos
import kotlinx.coroutines.flow.first

class MonitorarCnjUseCase(
    private val monitorarDjenUseCase: MonitorarDjenUseCase,
    private val sincronizarProcessosDataJudUseCase: SincronizarProcessosDataJudUseCase,
    private val repositorioProcessos: RepositorioProcessos,
) {
    suspend operator fun invoke(params: MonitorarDjenParams): MonitorarCnjResumo {
        val djenResumo = monitorarDjenUseCase(params)
        val processosLocais = repositorioProcessos.observarProcessos().first()
        val processosReiterar = processosLocais
            .asSequence()
            .filter { it.syncStatus.deveTeimosinha(it.dataJudTentativasRestantes) }
            .map { processo ->
                ProcessoDataJudSyncRequest(
                    numeroProcesso = processo.numeroProcesso,
                    tribunal = processo.tribunal,
                )
            }
            .toList()

        val processosParaSincronizar = (djenResumo.processosParaSincronizar + processosReiterar)
            .distinctBy { it.numeroProcesso to it.tribunal?.uppercase() }

        val dataJudResumo = processosParaSincronizar
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

private fun ProcessoSyncStatus.deveTeimosinha(tentativasRestantes: Int): Boolean =
    when (this) {
        ProcessoSyncStatus.PENDING,
        ProcessoSyncStatus.STALE,
        -> true

        ProcessoSyncStatus.NOT_FOUND,
        ProcessoSyncStatus.FAILED,
        -> tentativasRestantes > 0

        ProcessoSyncStatus.SYNCED -> false
    }
