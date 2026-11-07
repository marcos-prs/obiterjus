package com.obiterjus.data.agenda.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

/**
 * Drena os prazos confirmados que ainda não viraram evento no calendário.
 *
 * O [CalendarSyncWorker] existia e tinha até uma consulta própria no DAO, mas
 * nunca era enfileirado — um prazo cuja sincronização falhasse ficava pendente
 * para sempre.
 */
object CalendarSyncScheduler {
    private const val UNIQUE_WORK_NAME = "calendar_sync_pendentes"
    private const val BACKOFF_MINUTES = 5L

    fun agendar(context: Context) {
        val request = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(BACKOFF_MINUTES))
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
