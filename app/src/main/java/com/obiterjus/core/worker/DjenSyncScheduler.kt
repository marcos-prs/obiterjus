package com.obiterjus.core.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object DjenSyncScheduler {
    private const val UNIQUE_WORK_NAME = "djen_periodic_sync"
    private const val INTERVAL_HOURS = 24L
    private const val TARGET_HOUR = 7
    private const val BACKOFF_MINUTES = 30L

    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DjenSyncWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(delayAteProximoHorario(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(BACKOFF_MINUTES),
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
    }

    private fun delayAteProximoHorario(): Long {
        val agora = LocalDateTime.now()
        var alvo = agora.withHour(TARGET_HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!agora.isBefore(alvo)) {
            alvo = alvo.plusDays(1)
        }
        return Duration.between(agora, alvo).toMillis()
    }
}
