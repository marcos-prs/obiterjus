package com.obiterjus.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.djen.DjenSyncExecutor
import com.obiterjus.data.push.PushRegistroRepository
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
    private val pushRegistro: PushRegistroRepository,
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Distingue a execução disparada por push da execução periódica. Serve só
     * para o rótulo da Auditoria: é por ele que se sabe, sem adb, qual dos dois
     * mecanismos acordou o app.
     */
    private val origemPush: Boolean
        get() = inputData.getBoolean(DjenSyncScheduler.KEY_ORIGEM_PUSH, false)

    /** Execução avulsa que repete uma diária que falhou. */
    private val origemRetentativa: Boolean
        get() = inputData.getBoolean(DjenSyncScheduler.KEY_RETENTATIVA, false)

    override suspend fun doWork(): Result {
        val iniciadoEm = Instant.now()
        return try {
            val preferencias = perfilPreferencesRepository.preferencias.first()
            if (!preferencias.sincronizacaoAutomatica) {
                return concluir()
            }

            if (sincronizadoRecentemente()) {
                return concluir()
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
            concluir()
        } catch (error: Exception) {
            if (error is CancellationException) {
                registrarInterrupcao(syncLogDao, FONTE_AUDITORIA, iniciadoEm)
                throw error
            }
            when (decidirAposFalha(origemPush, origemRetentativa, runAttemptCount)) {
                AposFalha.RETENTATIVA_AVULSA -> {
                    // Nunca `Result.retry()` na periódica: o WorkManager troca o
                    // override das 7h por "hora da falha + backoff" (é o que faz
                    // `WorkerWrapper.reschedule`), e as 7h só voltariam depois de
                    // um sucesso. Se a nova tentativa ficasse retida pelo sistema,
                    // o dia seguinte ficava sem execução às 7h — o que houve de
                    // 19/11 18:17 para 20/11. `failure()` em trabalho periódico
                    // não encerra nada: segue para o próximo período, como o
                    // sucesso, e preserva o override re-ancorado acima.
                    reancorarProximaExecucao()
                    DjenSyncScheduler.agendarRetentativa(applicationContext)
                    Result.failure()
                }
                AposFalha.NOVA_TENTATIVA -> Result.retry()
                AposFalha.DESISTIR -> {
                    reancorarProximaExecucao()
                    Result.failure()
                }
            }
        }
    }

    /**
     * Fecha toda execução que terminou bem.
     *
     * A inscrição no push é refeita aqui, e não só na abertura do app: se a
     * abertura não alcançou o Firebase (sem rede naquele instante), ou se o flag
     * mudou com o processo já de pé, nada mais conferia a inscrição até o app
     * abrir do zero de novo. Aqui ela se acerta sozinha a cada dia. É idempotente
     * e falha em silêncio, então não muda o resultado da sincronização.
     */
    private suspend fun concluir(): Result {
        pushRegistro.sincronizar()
        reancorarProximaExecucao()
        return Result.success()
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
        val JANELA_ANTI_DUPLICIDADE: Duration = Duration.ofMinutes(30)

        /** Mesmo rótulo usado pelo executor em modo BACKGROUND, para a tela de Auditoria. */
        const val FONTE_AUDITORIA = "DJEN + DataJud · Diária"
    }
}

/** O que o [DjenSyncWorker] faz depois de uma execução que falhou. */
internal enum class AposFalha {
    /** Periódica: mantém as 7h do dia seguinte e repete numa execução avulsa. */
    RETENTATIVA_AVULSA,

    /** Execução avulsa (push ou retentativa): `Result.retry()`, com backoff. */
    NOVA_TENTATIVA,

    /** Acabaram as tentativas. */
    DESISTIR,
}

/**
 * Quantas vezes uma busca que falhou é repetida, sem contar a primeira
 * execução. Igual para o push e para a diária.
 */
internal const val MAX_RETENTATIVAS = 3

/**
 * Decide o destino de uma execução que falhou. Fica fora do worker para ser
 * testável sem WorkManager.
 *
 * A periódica nunca usa `Result.retry()` — ver o comentário no `catch` de
 * [DjenSyncWorker.doWork]. Ela delega a repetição a uma execução avulsa, e
 * conta como a primeira tentativa: por isso a avulsa soma 1 ao seu
 * `runAttemptCount`. Assim a diária faz no total as mesmas 1 + [MAX_RETENTATIVAS]
 * execuções que fazia antes, e que o push faz.
 */
internal fun decidirAposFalha(
    origemPush: Boolean,
    origemRetentativa: Boolean,
    runAttemptCount: Int,
): AposFalha {
    if (!origemPush && !origemRetentativa) return AposFalha.RETENTATIVA_AVULSA

    val retentativasFeitas = if (origemRetentativa) runAttemptCount + 1 else runAttemptCount
    return if (retentativasFeitas < MAX_RETENTATIVAS) AposFalha.NOVA_TENTATIVA else AposFalha.DESISTIR
}
