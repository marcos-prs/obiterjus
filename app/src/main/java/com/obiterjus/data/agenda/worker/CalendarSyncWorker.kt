package com.obiterjus.data.agenda.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.domain.repository.CalendarSyncRepository
import com.obiterjus.domain.model.PublicacaoPrazo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val prazoSugeridoDao: PrazoSugeridoDao by inject()
    private val calendarSyncRepository: CalendarSyncRepository by inject()

    override suspend fun doWork(): Result {
        val prazosPendentes = prazoSugeridoDao.getPrazosParaSincronizar()

        if (prazosPendentes.isEmpty()) {
            return Result.success()
        }

        var hasFailures = false

        for (entity in prazosPendentes) {
            val provedor = entity.provedorCalendario ?: continue
            
            val prazoDomain = PublicacaoPrazo(
                quantidade = entity.quantidade,
                unidade = entity.unidade,
                diasUteis = entity.diasUteis,
                textoOriginal = entity.textoOriginal,
                dataLimiteEstimada = entity.dataLimite,
                isConfirmado = entity.isConfirmado,
                idExternoCalendario = entity.idExternoCalendario,
                provedorCalendario = provedor
            )

            // Idealmente buscaria o título/descrição reais da Publicação ligada, mas para MVP 
            // assumimos strings fixas ou passamos info básica
            val syncResult = calendarSyncRepository.syncPrazo(
                prazo = prazoDomain,
                title = "Prazo ObiterJus",
                description = entity.textoOriginal,
                provedor = provedor
            )

            if (syncResult.isSuccess) {
                prazoSugeridoDao.update(
                    entity.copy(idExternoCalendario = syncResult.getOrNull())
                )
            } else {
                hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }
}
