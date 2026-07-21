package com.obiterjus.core.worker

import android.os.Build
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import java.time.Instant
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Registra no histórico de auditoria uma execução interrompida pelo sistema
 * (Doze, timeout de 10min do JobScheduler, perda de rede etc.).
 *
 * Sem este registro a interrupção é invisível: a CancellationException precisa ser
 * relançada para o WorkManager reexecutar o worker, e nenhum código de log roda.
 * Executa em [NonCancellable] porque o worker já está cancelado neste ponto.
 */
internal suspend fun ListenableWorker.registrarInterrupcao(
    syncLogDao: SyncLogDao,
    fonte: String,
    iniciadoEm: Instant,
) {
    withContext(NonCancellable) {
        runCatching {
            syncLogDao.insert(
                SyncLogEntity(
                    executadoEm = iniciadoEm,
                    duracaoMs = Instant.now().toEpochMilli() - iniciadoEm.toEpochMilli(),
                    fonte = fonte,
                    novasPublicacoes = 0,
                    processosSincronizados = 0,
                    sucesso = false,
                    mensagemErro = descreverInterrupcao(),
                ),
            )
        }
    }
}

private fun ListenableWorker.descreverInterrupcao(): String {
    val motivo = if (Build.VERSION.SDK_INT >= 31) {
        when (stopReason) {
            WorkInfo.STOP_REASON_TIMEOUT -> "tempo máximo de execução excedido"
            WorkInfo.STOP_REASON_DEVICE_STATE -> "estado do dispositivo (desligamento/economia)"
            WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "perda de conexão de rede"
            WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE -> "dispositivo saiu do estado ocioso"
            WorkInfo.STOP_REASON_QUOTA -> "cota de execução em segundo plano esgotada"
            WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION -> "restrição de segundo plano"
            WorkInfo.STOP_REASON_APP_STANDBY -> "app em standby"
            WorkInfo.STOP_REASON_USER -> "interrompida pelo usuário"
            WorkInfo.STOP_REASON_PREEMPT -> "preempção por tarefa mais prioritária"
            WorkInfo.STOP_REASON_NOT_STOPPED -> "cancelamento interno"
            else -> "código ${stopReason}"
        }
    } else {
        "motivo indisponível nesta versão do Android"
    }
    return "Interrompida pelo sistema: $motivo. Reexecução automática pelo WorkManager."
}
