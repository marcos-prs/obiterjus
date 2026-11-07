package com.obiterjus.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.domain.repository.PublicacoesRepository
import com.obiterjus.domain.usecase.RecalcularPrazosPendentesUC
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Worker diário que varre a agenda de prazos e emite alertas para os
 * prazos que vencem em até [ALERTA_DIAS_ANTECEDENCIA] dias.
 *
 * Complementa o [DjenSyncWorker], que cobre a descoberta de publicações e processos.
 * Este worker é agendado uma vez por dia sem requisito de rede.
 */
class PrazosWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repositorioPublicacoes: PublicacoesRepository,
    private val notificationHelper: PublicacaoNotificationHelper,
    private val syncLogDao: SyncLogDao,
    private val clock: Clock,
    private val recalcularPrazosPendentes: RecalcularPrazosPendentesUC,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val executadoEm = Instant.now()
        return try {
            // Este worker não exige rede; sem ela o recálculo simplesmente não
            // resolve nada e as pendências ficam para o próximo ciclo. Precede a
            // contagem para que o alerta enxergue as datas resolvidas agora.
            runCatching { recalcularPrazosPendentes() }
                .onFailure { erro -> Log.w(TAG, "Recálculo de prazos pendentes falhou", erro) }

            // O resto do app conta prazo no fuso do aparelho. Em UTC, entre
            // 21h e meia-noite de Brasília a data já é a do dia seguinte e o
            // alerta deslizava um dia.
            val hoje = LocalDate.now(clock)
            val limiteAlerta = hoje.plusDays(ALERTA_DIAS_ANTECEDENCIA)

            val publicacoes = repositorioPublicacoes.observarPublicacoes().first()

            val prazosVencendo = publicacoes.count { publicacao ->
                val dataLimite = publicacao.prazo?.dataLimiteEstimada ?: return@count false
                !dataLimite.isBefore(hoje) && !dataLimite.isAfter(limiteAlerta)
            }

            notificationHelper.notificarPrazosVencendo(prazosVencendo)
            syncLogDao.insert(
                SyncLogEntity(
                    executadoEm = executadoEm,
                    duracaoMs = Instant.now().toEpochMilli() - executadoEm.toEpochMilli(),
                    fonte = FONTE_PRAZOS_WORKER,
                    novasPublicacoes = prazosVencendo,
                    processosSincronizados = 0,
                    sucesso = true,
                    mensagemErro = null,
                ),
            )

            Result.success()
        } catch (error: Exception) {
            if (error is CancellationException) {
                registrarInterrupcao(syncLogDao, FONTE_PRAZOS_WORKER, executadoEm)
                throw error
            }
            runCatching {
                syncLogDao.insert(
                    SyncLogEntity(
                        executadoEm = executadoEm,
                        duracaoMs = null,
                        fonte = FONTE_PRAZOS_WORKER,
                        novasPublicacoes = 0,
                        processosSincronizados = 0,
                        sucesso = false,
                        mensagemErro = error.message ?: error::class.java.simpleName,
                    ),
                )
            }
            Result.failure()
        }
    }

    private companion object {
        const val TAG = "PrazosWorker"
        const val ALERTA_DIAS_ANTECEDENCIA = 2L
        const val FONTE_PRAZOS_WORKER = "PrazosWorker"
    }
}
