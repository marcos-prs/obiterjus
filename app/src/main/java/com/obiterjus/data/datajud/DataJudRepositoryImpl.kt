package com.obiterjus.data.datajud

import android.util.Log
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.data.datajud.mapper.toMovimentoEntities
import com.obiterjus.data.datajud.mapper.toParticipanteEntities
import com.obiterjus.data.datajud.mapper.toProcessoEntity
import com.obiterjus.data.datajud.remote.DataJudRemoteDataSource
import com.obiterjus.data.datajud.remote.UnknownDataJudTribunalException
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoDataJudSyncResultado
import com.obiterjus.domain.model.ProcessoDataJudSyncStatus
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException

class DataJudRepositoryImpl(
    private val remoteDataSource: DataJudRemoteDataSource,
    private val localProcessoRepository: LocalProcessoRepository,
    private val clock: Clock = Clock.systemUTC(),
) : DataJudRepository {
    override suspend fun sincronizar(
        params: SincronizarProcessosDataJudParams,
    ): SincronizarProcessosDataJudResumo {
        val requests = params.processos
            .mapNotNull { request ->
                NumeroProcessoNormalizer.normalize(request.numeroProcesso)?.let { numero ->
                    request.copy(numeroProcesso = numero, tribunal = request.tribunal?.trim())
                }
            }
            .distinctBy { it.numeroProcesso to it.tribunal?.uppercase() }

        val resultados = mutableListOf<ProcessoDataJudSyncResultado>()
        var movimentosSalvos = 0

        for (request in requests) {
            val syncedAt = clock.instant()
            try {
                Log.d(TAG, "DataJud: buscando processo=${request.numeroProcesso} tribunal=${request.tribunal}")
                val remoteResult = remoteDataSource.buscarProcesso(
                    numeroProcesso = request.numeroProcesso,
                    tribunal = request.tribunal,
                )
                Log.d(TAG, "  → indexName=${remoteResult.indexName} | processo=${if (remoteResult.processo != null) "FOUND" else "NOT_FOUND"}")
                val processoDto = remoteResult.processo
                if (processoDto == null) {
                    localProcessoRepository.upsertProcesso(
                        processoStatusEntity(
                            numeroProcesso = request.numeroProcesso,
                            tribunal = remoteResult.tribunal,
                            status = ProcessoSyncStatus.NOT_FOUND,
                            syncedAt = syncedAt,
                        ),
                    )
                    resultados += ProcessoDataJudSyncResultado(
                        numeroProcesso = request.numeroProcesso,
                        tribunal = remoteResult.tribunal,
                        status = ProcessoDataJudSyncStatus.NOT_FOUND,
                        movimentosSalvos = 0,
                        mensagem = null,
                    )
                } else {
                    val processo = processoDto.toProcessoEntity(
                        fallbackNumeroProcesso = request.numeroProcesso,
                        fallbackTribunal = remoteResult.tribunal,
                        syncedAt = syncedAt,
                        syncStatus = ProcessoSyncStatus.SYNCED,
                    )
                    val movimentos = processoDto.toMovimentoEntities(processo.numeroProcesso)
                    val participantes = processoDto.toParticipanteEntities(processo.numeroProcesso)
                    Log.d(TAG, "  → FOUND: classe=${processo.classeNome} | movimentos=${movimentos.size} | participantes=${participantes.size} | sigilo=${processo.nivelSigilo}")

                    localProcessoRepository.upsertProcesso(processo)
                    localProcessoRepository.replaceMovimentos(processo.numeroProcesso, movimentos)
                    localProcessoRepository.replaceParticipantes(processo.numeroProcesso, participantes)
                    movimentosSalvos += movimentos.size

                    resultados += ProcessoDataJudSyncResultado(
                        numeroProcesso = processo.numeroProcesso,
                        tribunal = processo.tribunal,
                        status = ProcessoDataJudSyncStatus.FOUND,
                        movimentosSalvos = movimentos.size,
                        mensagem = null,
                    )
                }
            } catch (error: UnknownDataJudTribunalException) {
                Log.e(TAG, "  → FAILED (tribunal desconhecido): ${error.message}")
                localProcessoRepository.upsertProcesso(
                    processoStatusEntity(
                        numeroProcesso = request.numeroProcesso,
                        tribunal = request.tribunal,
                        status = ProcessoSyncStatus.FAILED,
                        syncedAt = syncedAt,
                    ),
                )
                resultados += ProcessoDataJudSyncResultado(
                    numeroProcesso = request.numeroProcesso,
                    tribunal = request.tribunal,
                    status = ProcessoDataJudSyncStatus.FAILED,
                    movimentosSalvos = 0,
                    mensagem = error.message,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e(TAG, "  → FAILED (excecao): ${error::class.java.simpleName}: ${error.message}")
                localProcessoRepository.upsertProcesso(
                    processoStatusEntity(
                        numeroProcesso = request.numeroProcesso,
                        tribunal = request.tribunal,
                        status = ProcessoSyncStatus.FAILED,
                        syncedAt = syncedAt,
                    ),
                )
                resultados += ProcessoDataJudSyncResultado(
                    numeroProcesso = request.numeroProcesso,
                    tribunal = request.tribunal,
                    status = ProcessoDataJudSyncStatus.FAILED,
                    movimentosSalvos = 0,
                    mensagem = error.message,
                )
            }
        }

        return SincronizarProcessosDataJudResumo(
            solicitados = params.processos.size,
            normalizados = requests.size,
            encontrados = resultados.count { it.status == ProcessoDataJudSyncStatus.FOUND },
            naoEncontrados = resultados.count { it.status == ProcessoDataJudSyncStatus.NOT_FOUND },
            falhas = resultados.count { it.status == ProcessoDataJudSyncStatus.FAILED },
            movimentosSalvos = movimentosSalvos,
            resultados = resultados,
        )
    }

    private fun processoStatusEntity(
        numeroProcesso: String,
        tribunal: String?,
        status: ProcessoSyncStatus,
        syncedAt: java.time.Instant,
    ): ProcessoEntity =
        ProcessoEntity(
            numeroProcesso = numeroProcesso,
            tribunal = tribunal,
            grau = null,
            classeCodigo = null,
            classeNome = null,
            assuntosJson = null,
            orgaoJulgadorCodigo = null,
            orgaoJulgadorNome = null,
            nivelSigilo = null,
            dataAjuizamento = null,
            syncStatus = status,
            capturadoEm = syncedAt,
            atualizadoEm = syncedAt,
            dataJudTentativasRestantes = when (status) {
                ProcessoSyncStatus.NOT_FOUND, ProcessoSyncStatus.FAILED -> 2
                else -> 0
            },
        )
    companion object {
        private const val TAG = "DataJudRepository"
    }
}
