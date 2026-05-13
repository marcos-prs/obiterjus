package com.obiterjus.data.djen

import android.util.Log
import com.obiterjus.data.djen.mapper.DjenMapper
import com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper
import com.obiterjus.data.djen.remote.DjenPaginationStopReason
import com.obiterjus.data.djen.remote.DjenRemoteDataSource
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.repository.DjenRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DjenRepositoryImpl(
    private val remoteDataSource: DjenRemoteDataSource,
    private val localPublicacaoRepository: LocalPublicacaoRepository,
    private val djenMapper: DjenMapper,
    private val publicacaoPrazoMapper: PublicacaoPrazoMapper,
    private val clock: Clock = Clock.systemUTC(),
) : DjenRepository {

    override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
        val (remoteResultOab, remoteResultName) = try {
            coroutineScope {
                val oabJob = async {
                    remoteDataSource.buscarComunicacoes(
                        numeroOab = params.numeroOab,
                        ufOab = params.ufOab,
                        nomeAdvogado = null,
                        dataInicio = params.dataInicio,
                        dataFim = params.dataFim,
                    )
                }
                val nameJob = async {
                    if (!params.nomeAdvogado.isNullOrBlank()) {
                        remoteDataSource.buscarComunicacoes(
                            numeroOab = null,
                            ufOab = null,
                            nomeAdvogado = params.nomeAdvogado,
                            dataInicio = params.dataInicio,
                            dataFim = params.dataFim,
                        )
                    } else null
                }
                Pair(oabJob.await(), nameJob.await())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return errorResumo(error)
        }

        val capturedAt = clock.instant()

        val allItems = (remoteResultOab.items + (remoteResultName?.items ?: emptyList()))
            .distinctBy { it.id }

        val publicacoes: List<PublicacaoEntity> = allItems.map { dto ->
            val entity = djenMapper.toPublicacaoEntity(
                dto = dto,
                capturedAt = capturedAt,
                updatedAt = capturedAt,
            )
            publicacaoPrazoMapper.comPrazoCalculado(entity)
        }

        Log.d(TAG, "Publicações recebidas da API (${publicacoes.size}):")
        publicacoes.forEach { pub ->
            Log.d(TAG, "  id=${pub.id} | dataDisponibilizacao=${pub.dataDisponibilizacao} | tribunal=${pub.tribunal}")
        }

        val upsertResult = localPublicacaoRepository.upsertPublicacoes(publicacoes)
        Log.d(TAG, "Upsert: novas=${upsertResult.novas} | atualizadas=${upsertResult.atualizadas} | novasIds=${upsertResult.novasIds}")
        val novasIds = upsertResult.novasIds.toSet()
        val publicacoesNovas = publicacoes.filter { it.id in novasIds }

        val processosParaSincronizar = publicacoesNovas
            .asSequence()
            .mapNotNull { publicacao ->
                publicacao.numeroProcesso?.let { numero ->
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = numero,
                        tribunal = publicacao.tribunal,
                    )
                }
            }
            .distinctBy { it.numeroProcesso to it.tribunal?.uppercase() }
            .toList()

        return MonitorarDjenResumo(
            totalRemoto = remoteResultOab.totalRemoto ?: remoteResultName?.totalRemoto,
            totalRecebidas = upsertResult.totalRecebidas,
            novas = upsertResult.novas,
            atualizadas = upsertResult.atualizadas,
            sigilosas = publicacoes.count { it.isSigiloso },
            processosNovos = processosParaSincronizar.map { it.numeroProcesso },
            processosParaSincronizar = processosParaSincronizar,
            paginasConsultadas = remoteResultOab.paginasConsultadas + (remoteResultName?.paginasConsultadas ?: 0),
            motivoParada = remoteResultOab.motivoParada.toDomain(),
            falhas = emptyList(),
            novasComPrazo = publicacoesNovas.count { it.prazoQuantidade != null },
            novasSigilosas = publicacoesNovas.count { it.isSigiloso },
        )
    }

    private fun errorResumo(error: Exception) = MonitorarDjenResumo(
        totalRemoto = null,
        totalRecebidas = 0,
        novas = 0,
        atualizadas = 0,
        sigilosas = 0,
        processosNovos = emptyList(),
        processosParaSincronizar = emptyList(),
        paginasConsultadas = 0,
        motivoParada = MonitorarDjenStopReason.UNKNOWN,
        falhas = listOf(error.message ?: error::class.java.simpleName),
    )

    private fun DjenPaginationStopReason.toDomain(): MonitorarDjenStopReason =
        when (this) {
            DjenPaginationStopReason.EMPTY_PAGE -> MonitorarDjenStopReason.EMPTY_PAGE
            DjenPaginationStopReason.PARTIAL_PAGE -> MonitorarDjenStopReason.PARTIAL_PAGE
            DjenPaginationStopReason.COUNT_CONSUMED -> MonitorarDjenStopReason.COUNT_CONSUMED
            DjenPaginationStopReason.REPEATED_PAGE -> MonitorarDjenStopReason.REPEATED_PAGE
            DjenPaginationStopReason.PAGE_LIMIT -> MonitorarDjenStopReason.PAGE_LIMIT
        }

    companion object {
        private const val TAG = "DjenRepositoryImpl"
    }
}