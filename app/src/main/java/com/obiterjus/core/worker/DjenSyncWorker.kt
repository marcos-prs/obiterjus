package com.obiterjus.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.data.djen.DjenSyncExecutor
import com.obiterjus.domain.model.MonitorarDjenModo
import kotlinx.coroutines.CancellationException

class DjenSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val djenSyncExecutor: DjenSyncExecutor,
    private val notificationHelper: PublicacaoNotificationHelper,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val resumo = djenSyncExecutor.executar(MonitorarDjenModo.BACKGROUND)

            notificationHelper.notificarNovasPublicacoes(resumo.djen.novas)
            notificationHelper.notificarProcessosNovos(resumo.djen.processosNovos.size)
            if (resumo.djen.novasComPrazo > 0) {
                notificationHelper.notificarPublicacaoComPrazo()
            }
            if (resumo.djen.novasSigilosas > 0) {
                notificationHelper.notificarPublicacaoSigilosa()
            }
            Result.success()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Result.retry()
        }
    }
}
