package com.obiterjus.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.djen.DjenSyncExecutor
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.usecase.RecalcularPrazosPendentesUC
import java.time.Duration
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
    private val recalcularPrazosPendentes: RecalcularPrazosPendentesUC,
    private val repositorioCadastroOab: CadastroOabRepository,
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Distingue a execução disparada por push da execução periódica. Serve só
     * para o rótulo da Auditoria: é por ele que se sabe, sem adb, qual dos dois
     * mecanismos acordou o app.
     */
    private val origemPush: Boolean
        get() = inputData.getBoolean(DjenSyncScheduler.KEY_ORIGEM_PUSH, false)

    override suspend fun doWork(): Result {
        val iniciadoEm = Instant.now()
        return try {
            val preferencias = perfilPreferencesRepository.preferencias.first()
            if (!preferencias.sincronizacaoAutomatica) {
                reancorarProximaExecucao()
                return Result.success()
            }

            if (sincronizadoRecentemente()) {
                reancorarProximaExecucao()
                return Result.success()
            }

            val modo = if (origemPush) MonitorarDjenModo.PUSH else MonitorarDjenModo.BACKGROUND
            val resumo = djenSyncExecutor.executar(modo)

            // Fecha o que a API de cálculo recusou nesta e em execuções anteriores.
            // Roda antes das notificações para que o alerta de prazo enxergue as
            // datas recém-resolvidas. Falha aqui não invalida a sincronização.
            runCatching { recalcularPrazosPendentes() }
                .onFailure { erro -> Log.w(TAG, "Recálculo de prazos pendentes falhou", erro) }

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

    /**
     * Evita a busca dupla quando o push e o periódico caem na mesma janela —
     * às 7h os dois querem rodar, e a segunda execução só gastaria rede e cota
     * do DJEN para encontrar `novas = 0`.
     *
     * A janela é curta de propósito. Publicação entra ao longo da manhã, então
     * uma janela larga suprimiria execução legítima: quem sincronizou às 6h30
     * ainda precisa da busca das 7h.
     */
    private suspend fun sincronizadoRecentemente(): Boolean {
        val ultimoSucessoEm = repositorioCadastroOab.status.first().ultimoSucessoEm ?: return false
        return Duration.between(ultimoSucessoEm, Instant.now()) < JANELA_ANTI_DUPLICIDADE
    }

    private companion object {
        const val TAG = "DjenSyncWorker"
        const val MAX_TENTATIVAS = 3
        val JANELA_ANTI_DUPLICIDADE: Duration = Duration.ofMinutes(30)

        /** Mesmo rótulo usado pelo executor em modo BACKGROUND, para a tela de Auditoria. */
        const val FONTE_AUDITORIA = "DJEN + DataJud · Diária"
    }
}
