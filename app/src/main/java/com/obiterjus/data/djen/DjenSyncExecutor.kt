package com.obiterjus.data.djen

import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.domain.model.MonitorarCnjResumo
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import android.util.Log
import com.obiterjus.data.settings.PerfilPreferencesRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

interface DjenSyncExecutor {
    suspend fun executar(modo: MonitorarDjenModo): MonitorarCnjResumo
}

class DjenSyncExecutorImpl(
    private val appConfigRepository: AppConfigRepository,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val repositorioCadastroOab: CadastroOabRepository,
    private val monitorarCnjUseCase: MonitorarCnjUseCase,
    private val syncLogDao: SyncLogDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) : DjenSyncExecutor {
    override suspend fun executar(modo: MonitorarDjenModo): MonitorarCnjResumo {
        val executadoEm = clock.instant()

        return try {
            val cadastro = repositorioCadastroOab.cadastro.first()
            if (!cadastro.isValid) {
                val mensagem = "Cadastro OAB invalido."
                registrarFalha(
                    executadoEm = executadoEm,
                    duracaoMs = clock.instant().toEpochMilli() - executadoEm.toEpochMilli(),
                    mensagem = mensagem,
                    modo = modo,
                )
                throw DjenSyncExecutionException(mensagem)
            }

            val config = appConfigRepository.refresh()
            val perfil = perfilPreferencesRepository.preferencias.first()
            val status = repositorioCadastroOab.status.first()
            val dataFim = LocalDate.now(clock)

            // Usa a janela configurada pelo usuário de forma absoluta, ignorando o teto do Remote Config.
            // O app deve SEMPRE obedecer à configuração do usuário.
            val lookbackDias = perfil.intervaloBuscaDias.toLong()

            val ultimoSucessoEm = status.ultimoSucessoEm
            val dataInicio = ultimoSucessoEm
                ?.atZone(clock.zone)
                ?.toLocalDate()
                ?.minusDays(lookbackDias)
                ?: dataFim.minusDays(lookbackDias)

            Log.d(TAG, "--- DjenSync: início da execução ---")
            Log.d(TAG, "OAB: ${cadastro.numero}/${cadastro.uf}")
            Log.d(TAG, "ultimoSucessoEm (raw): $ultimoSucessoEm")
            Log.d(TAG, "CONFIGURAÇÃO: intervaloBuscaDias (usuário): ${perfil.intervaloBuscaDias}")
            Log.d(TAG, "CONFIGURAÇÃO: syncLookbackDays (ignorado, era Remote Config): ${config.syncLookbackDays}")
            Log.d(TAG, "CONFIGURAÇÃO: lookbackDias EFETIVO: $lookbackDias")
            Log.d(TAG, "CONFIGURAÇÃO: apenasPorNome: ${perfil.apenasPorNome}")
            Log.d(TAG, "dataInicio enviada para busca: $dataInicio")
            Log.d(TAG, "dataFim enviada:    $dataFim")
            Log.d(TAG, "modo: $modo")

            val resumo = monitorarCnjUseCase(
                MonitorarDjenParams(
                    numeroOab = cadastro.numero,
                    ufOab = cadastro.uf,
                    nomeAdvogado = cadastro.nomeAdvogado,
                    dataInicio = dataInicio,
                    dataFim = dataFim,
                    modo = modo,
                    apenasPorNome = perfil.apenasPorNome,
                ),
            )

            val duracaoMs = clock.instant().toEpochMilli() - executadoEm.toEpochMilli()

            Log.d(TAG, "--- DjenSync: resultado ---")
            Log.d(TAG, "totalRemoto: ${resumo.djen.totalRemoto}")
            Log.d(TAG, "totalRecebidas: ${resumo.djen.totalRecebidas}")
            Log.d(TAG, "novas: ${resumo.djen.novas}")
            Log.d(TAG, "atualizadas: ${resumo.djen.atualizadas}")
            Log.d(TAG, "paginasConsultadas: ${resumo.djen.paginasConsultadas}")
            Log.d(TAG, "motivoParada: ${resumo.djen.motivoParada}")
            Log.d(TAG, "falhas: ${resumo.djen.falhas}")

            if (resumo.djen.falhas.isEmpty()) {
                registrarSucesso(
                    executadoEm = executadoEm,
                    duracaoMs = duracaoMs,
                    resumo = resumo,
                    modo = modo,
                )
                resumo
            } else {
                val mensagem = resumo.djen.falhas.first()
                Log.w(TAG, "DjenSync falhou: $mensagem")
                registrarFalha(
                    executadoEm = executadoEm,
                    duracaoMs = duracaoMs,
                    mensagem = mensagem,
                    modo = modo,
                )
                throw DjenSyncExecutionException(mensagem)
            }
        } catch (error: DjenSyncExecutionException) {
            throw error
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            registrarFalha(
                executadoEm = executadoEm,
                duracaoMs = clock.instant().toEpochMilli() - executadoEm.toEpochMilli(),
                mensagem = error.message ?: error::class.java.simpleName,
                modo = modo,
            )
            throw error
        }
    }

    private suspend fun registrarSucesso(
        executadoEm: Instant,
        duracaoMs: Long,
        resumo: MonitorarCnjResumo,
        modo: MonitorarDjenModo,
    ) {
        repositorioCadastroOab.registrarSucesso(
            executadoEm = executadoEm,
            novasPublicacoes = resumo.djen.novas,
        )
        syncLogDao.insert(
            SyncLogEntity(
                executadoEm = executadoEm,
                duracaoMs = duracaoMs,
                fonte = fonteAuditoria(modo),
                novasPublicacoes = resumo.djen.novas,
                processosSincronizados = resumo.totalProcessosSincronizados,
                sucesso = true,
                mensagemErro = null,
            ),
        )
        syncLogDao.descartarAntigos()
    }

    private suspend fun registrarFalha(
        executadoEm: Instant,
        duracaoMs: Long?,
        mensagem: String,
        modo: MonitorarDjenModo,
    ) {
        repositorioCadastroOab.registrarFalha(
            executadoEm = executadoEm,
            mensagem = mensagem,
        )
        syncLogDao.insert(
            SyncLogEntity(
                executadoEm = executadoEm,
                duracaoMs = duracaoMs,
                fonte = fonteAuditoria(modo),
                novasPublicacoes = 0,
                processosSincronizados = 0,
                sucesso = false,
                mensagemErro = mensagem,
            ),
        )
    }

    private fun fonteAuditoria(modo: MonitorarDjenModo): String =
        when (modo) {
            MonitorarDjenModo.BACKGROUND -> "DJEN + DataJud · Diária"
            MonitorarDjenModo.MANUAL -> "DJEN + DataJud · Manual"
        }

    private class DjenSyncExecutionException(
        message: String,
    ) : IllegalStateException(message)

    companion object {
        private const val TAG = "DjenSyncExecutor"
    }
}
