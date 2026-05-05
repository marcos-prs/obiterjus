package com.obiterjus.data.agenda.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.R
import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.domain.model.ProvedorCalendario
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
        val prazosPendentes = prazoSugeridoDao.getPrazosParaSincronizar(
            provedores = listOf(
                ProvedorCalendario.GOOGLE.codigo,
                ProvedorCalendario.OUTLOOK.codigo,
            ),
        )

        if (prazosPendentes.isEmpty()) {
            return Result.success()
        }

        var hasFailures = false

        for (entity in prazosPendentes) {
            val provedor = ProvedorCalendario.fromCodigo(entity.provedorCalendario) ?: continue
            
            val prazoDomain = PublicacaoPrazo(
                quantidade = entity.quantidade,
                unidade = entity.unidade,
                diasUteis = entity.diasUteis,
                textoOriginal = entity.textoOriginal,
                dataLimiteEstimada = entity.dataLimite,
                isConfirmado = entity.isConfirmado,
                idExternoCalendario = entity.idExternoCalendario,
                provedorCalendario = provedor.codigo
            )

            val syncResult = calendarSyncRepository.syncPrazo(
                prazo = prazoDomain,
                title = applicationContext.getString(
                    R.string.calendar_sync_event_title,
                    entity.publicacaoId,
                ),
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
