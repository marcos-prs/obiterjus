package com.obiterjus.core.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object DjenSyncScheduler {
    private const val UNIQUE_WORK_NAME = "djen_periodic_sync"
    private const val UNIQUE_WORK_IMEDIATA = "djen_sync_push"
    private const val INTERVAL_HOURS = 24L
    private const val TARGET_HOUR = 7
    private const val BACKOFF_MINUTES = 30L

    /** Marca a execução como disparada por push, para a fonte da Auditoria. */
    const val KEY_ORIGEM_PUSH = "origem_push"

    /**
     * Dispara a sincronização agora, em resposta a um push do servidor.
     *
     * `setExpedited` é o ponto: job expedito é isento da constraint FLEXIBILITY
     * (carregando + ocioso + bateria ok) que segura o periódico — justamente a
     * que faz a execução das 7h escorregar até o usuário abrir o app. O
     * fallback `RUN_AS_NON_EXPEDITED_WORK_REQUEST` cobre a cota esgotada: a
     * execução vira comum em vez de ser descartada.
     *
     * KEEP evita empilhar execução se chegarem dois pushes seguidos.
     */
    fun agendarAgora(context: Context) {
        val request = OneTimeWorkRequestBuilder<DjenSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(KEY_ORIGEM_PUSH to true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(BACKOFF_MINUTES))
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK_IMEDIATA, ExistingWorkPolicy.KEEP, request)
    }

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
