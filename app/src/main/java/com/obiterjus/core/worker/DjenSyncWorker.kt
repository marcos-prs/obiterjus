package com.obiterjus.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class DjenSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appConfigRepository: AppConfigRepository,
    private val repositorioCadastroOab: RepositorioCadastroOab,
    private val monitorarCnjUseCase: MonitorarCnjUseCase,
    private val notificationHelper: PublicacaoNotificationHelper,
    private val syncLogDao: SyncLogDao,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val executadoEm = Instant.now()

        return try {
            val cadastro = repositorioCadastroOab.cadastro.first()
            if (!cadastro.isValid) {
                return Result.success()
            }

            val config = appConfigRepository.refresh()
            val status = repositorioCadastroOab.status.first()
            val dataFim = LocalDate.now(ZoneOffset.UTC)
            val dataInicio = status.ultimoSucessoEm
                ?.atOffset(ZoneOffset.UTC)
                ?.toLocalDate()
                ?.minusDays(config.syncLookbackDays)
                ?: cadastro.dataInicio
                ?: dataFim.minusDays(config.syncLookbackDays)

            val resumo = monitorarCnjUseCase(
                MonitorarDjenParams(
                    numeroOab = cadastro.numero,
                    ufOab = cadastro.uf,
                    dataInicio = dataInicio,
                    dataFim = dataFim,
                    modo = MonitorarDjenModo.BACKGROUND,
                ),
            )

            val duracaoMs = Instant.now().toEpochMilli() - executadoEm.toEpochMilli()

            if (resumo.djen.falhas.isEmpty()) {
                repositorioCadastroOab.registrarSucesso(
                    executadoEm = executadoEm,
                    novasPublicacoes = resumo.djen.novas,
                )

                syncLogDao.insert(
                    SyncLogEntity(
                        executadoEm = executadoEm,
                        duracaoMs = duracaoMs,
                        fonte = "DJEN + DataJud",
                        novasPublicacoes = resumo.djen.novas,
                        processosSincronizados = resumo.totalProcessosSincronizados,
                        sucesso = true,
                        mensagemErro = null,
                    ),
                )
                syncLogDao.descartarAntigos()

                // Alertas granulares
                notificationHelper.notificarNovasPublicacoes(resumo.djen.novas)
                notificationHelper.notificarProcessosNovos(resumo.djen.processosNovos.size)
                if (resumo.djen.novasComPrazo > 0) {
                    notificationHelper.notificarPublicacaoComPrazo()
                }
                if (resumo.djen.novasSigilosas > 0) {
                    notificationHelper.notificarPublicacaoSigilosa()
                }

                Result.success()
            } else {
                val mensagem = resumo.djen.falhas.first()
                repositorioCadastroOab.registrarFalha(
                    executadoEm = executadoEm,
                    mensagem = mensagem,
                )
                syncLogDao.insert(
                    SyncLogEntity(
                        executadoEm = executadoEm,
                        duracaoMs = duracaoMs,
                        fonte = "DJEN",
                        novasPublicacoes = 0,
                        processosSincronizados = 0,
                        sucesso = false,
                        mensagemErro = mensagem,
                    ),
                )
                Result.retry()
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            val mensagem = error.message ?: error::class.java.simpleName
            repositorioCadastroOab.registrarFalha(
                executadoEm = executadoEm,
                mensagem = mensagem,
            )
            runCatching {
                syncLogDao.insert(
                    SyncLogEntity(
                        executadoEm = executadoEm,
                        duracaoMs = null,
                        fonte = "DJEN",
                        novasPublicacoes = 0,
                        processosSincronizados = 0,
                        sucesso = false,
                        mensagemErro = mensagem,
                    ),
                )
            }
            Result.retry()
        }
    }
}
