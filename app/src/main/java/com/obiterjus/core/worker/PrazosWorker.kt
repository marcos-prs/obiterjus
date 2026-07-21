package com.obiterjus.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.domain.repository.PublicacoesRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val executadoEm = Instant.now()
        return try {
            val hoje = LocalDate.now(ZoneOffset.UTC)
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
        const val ALERTA_DIAS_ANTECEDENCIA = 2L
        const val FONTE_PRAZOS_WORKER = "PrazosWorker"
    }
}
