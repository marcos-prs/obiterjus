package com.obiterjus.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PrazosWorkerScheduler {
    private const val UNIQUE_WORK_NAME = "prazos_daily_check"
    private const val INTERVAL_HOURS = 24L

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<PrazosWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(Constraints.NONE)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }
}
