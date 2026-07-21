package com.obiterjus.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.djen.DjenSyncExecutor
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.model.MonitorarDjenModo
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class DjenSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val djenSyncExecutor: DjenSyncExecutor,
    private val notificationHelper: PublicacaoNotificationHelper,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val syncLogDao: SyncLogDao,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val iniciadoEm = Instant.now()
        return try {
            val preferencias = perfilPreferencesRepository.preferencias.first()
            if (!preferencias.sincronizacaoAutomatica) {
                reancorarProximaExecucao()
                return Result.success()
            }

            val resumo = djenSyncExecutor.executar(MonitorarDjenModo.BACKGROUND)

            notificationHelper.notificarNovasPublicacoes(resumo.djen.novas)
            notificationHelper.notificarProcessosNovos(resumo.djen.processosNovos.size)
            if (resumo.djen.novasComPrazo > 0) {
                notificationHelper.notificarPublicacaoComPrazo()
            }
            if (resumo.djen.novasSigilosas > 0) {
                notificationHelper.notificarPublicacaoSigilosa()
            }
            reancorarProximaExecucao()
            Result.success()
        } catch (error: Exception) {
            if (error is CancellationException) {
                registrarInterrupcao(syncLogDao, FONTE_AUDITORIA, iniciadoEm)
                throw error
            }
            if (runAttemptCount < MAX_TENTATIVAS) {
                Result.retry()
            } else {
                reancorarProximaExecucao()
                Result.failure()
            }
        }
    }

    /**
     * O override de horário vale apenas para a execução seguinte; sem re-ancorar aqui,
     * o WorkManager voltaria a agendar por "última execução + 24h" e o horário das 7h
     * derivaria um pouco a cada dia.
     */
    private fun reancorarProximaExecucao() {
        DjenSyncScheduler.schedulePeriodic(applicationContext)
    }

    private companion object {
        const val MAX_TENTATIVAS = 3

        /** Mesmo rótulo usado pelo executor em modo BACKGROUND, para a tela de Auditoria. */
        const val FONTE_AUDITORIA = "DJEN + DataJud · Diária"
    }
}
