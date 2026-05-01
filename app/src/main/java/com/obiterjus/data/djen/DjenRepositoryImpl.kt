package com.obiterjus.data.djen

import com.obiterjus.data.djen.mapper.DjenMapper
import com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper
import com.obiterjus.data.djen.remote.DjenRemoteDataSource
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.repository.DjenRepository
import java.time.Clock
import kotlinx.coroutines.CancellationException

class DjenRepositoryImpl(
    private val remoteDataSource: DjenRemoteDataSource,
    private val localPublicacaoRepository: LocalPublicacaoRepository,
    private val djenMapper: DjenMapper,
    private val publicacaoPrazoMapper: PublicacaoPrazoMapper,
    private val clock: Clock = Clock.systemUTC(),
) : DjenRepository {
    override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
        val remoteResult = try {
            remoteDataSource.buscarComunicacoes(
                numeroOab = params.numeroOab,
                ufOab = params.ufOab,
                dataInicio = params.dataInicio,
                dataFim = params.dataFim,
            )
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            return MonitorarDjenResumo(
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
        }
        val capturedAt = clock.instant()
        val publicacoes = mutableListOf<com.obiterjus.data.publicacao.local.PublicacaoEntity>()
        for (dto in remoteResult.items) {
            val entity = djenMapper.toPublicacaoEntity(
                dto = dto,
                capturedAt = capturedAt,
                updatedAt = capturedAt,
            )
            publicacoes.add(publicacaoPrazoMapper.comPrazoCalculado(entity))
        }
        val upsertResult = localPublicacaoRepository.upsertPublicacoes(publicacoes)
        val novasIds = upsertResult.novasIds.toSet()
        val processosNovos = publicacoes
            .asSequence()
            .filter { it.id in novasIds }
            .mapNotNull { it.numeroProcesso }
            .distinct()
            .toList()
        val processosParaSincronizar = publicacoes
            .asSequence()
            .filter { it.id in novasIds }
            .mapNotNull { publicacao ->
                publicacao.numeroProcesso?.let { numeroProcesso ->
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = numeroProcesso,
                        tribunal = publicacao.tribunal,
                    )
                }
            }
            .distinctBy { it.numeroProcesso to it.tribunal?.uppercase() }
            .toList()
        val publicacoesNovas = publicacoes.filter { it.id in novasIds }

        return MonitorarDjenResumo(
            totalRemoto = remoteResult.totalRemoto,
            totalRecebidas = upsertResult.totalRecebidas,
            novas = upsertResult.novas,
            atualizadas = upsertResult.atualizadas,
            sigilosas = publicacoes.count { it.isSigiloso },
            processosNovos = processosNovos,
            processosParaSincronizar = processosParaSincronizar,
            paginasConsultadas = remoteResult.paginasConsultadas,
            motivoParada = remoteResult.motivoParada.paraMotivoParadaDominio(),
            falhas = emptyList(),
            novasComPrazo = publicacoesNovas.count { it.prazoQuantidade != null },
            novasSigilosas = publicacoesNovas.count { it.isSigiloso },
        )
    }

    private fun com.obiterjus.data.djen.remote.DjenPaginationStopReason.paraMotivoParadaDominio(): MonitorarDjenStopReason =
        when (this) {
            com.obiterjus.data.djen.remote.DjenPaginationStopReason.EMPTY_PAGE -> MonitorarDjenStopReason.EMPTY_PAGE
            com.obiterjus.data.djen.remote.DjenPaginationStopReason.PARTIAL_PAGE -> MonitorarDjenStopReason.PARTIAL_PAGE
            com.obiterjus.data.djen.remote.DjenPaginationStopReason.COUNT_CONSUMED -> MonitorarDjenStopReason.COUNT_CONSUMED
            com.obiterjus.data.djen.remote.DjenPaginationStopReason.REPEATED_PAGE -> MonitorarDjenStopReason.REPEATED_PAGE
            com.obiterjus.data.djen.remote.DjenPaginationStopReason.PAGE_LIMIT -> MonitorarDjenStopReason.PAGE_LIMIT
        }
}
