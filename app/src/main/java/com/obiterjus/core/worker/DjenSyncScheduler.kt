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

    /**
     * Garante que a sincronização diária está agendada, sem mexer no horário já ancorado.
     *
     * Deve ser o único ponto de agendamento chamado na inicialização do app: como o
     * WorkManager inicia o processo do app para executar o job das 7h, um UPDATE aqui
     * re-ancoraria o override para o dia seguinte antes de o worker rodar, e a execução
     * pendente seria descartada ("Delaying execution... before schedule").
     */
    fun ensureScheduled(context: Context) {
        enqueue(context, ExistingPeriodicWorkPolicy.KEEP)
    }

    /**
     * Agenda (ou re-ancora) a sincronização diária exatamente às [TARGET_HOUR]h.
     *
     * Um PeriodicWorkRequest sozinho não garante horário fixo: após a primeira execução o
     * WorkManager agenda por "última execução + intervalo", acumulando atraso a cada dia.
     * Por isso usamos [setNextScheduleTimeOverride], que fixa o instante exato da próxima
     * execução — e o [DjenSyncWorker] chama este método novamente ao final de cada execução
     * para re-ancorar a seguinte às 7h.
     *
     * Só deve ser chamado ao final de uma execução do worker ou quando o usuário liga a
     * sincronização automática; na inicialização do app use [ensureScheduled].
     */
    fun schedulePeriodic(context: Context) {
        enqueue(context, ExistingPeriodicWorkPolicy.UPDATE)
    }

    private fun enqueue(context: Context, policy: ExistingPeriodicWorkPolicy) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DjenSyncWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setNextScheduleTimeOverride(System.currentTimeMillis() + delayAteProximoHorario())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(BACKOFF_MINUTES),
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                policy,
                request,
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
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
