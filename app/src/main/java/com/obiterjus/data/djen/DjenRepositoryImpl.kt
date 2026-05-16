package com.obiterjus.data.djen

import android.util.Log
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.data.djen.mapper.DjenMapper
import com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper
import com.obiterjus.data.djen.remote.DjenFetchResult
import com.obiterjus.data.djen.remote.DjenPaginationStopReason
import com.obiterjus.data.djen.remote.DjenRemoteDataSource
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.domain.model.ConfiancaMatch
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.RepositorioProcessos
import com.obiterjus.domain.usecase.matcher.NomeVariacoes
import com.obiterjus.domain.usecase.matcher.PublicationMatcher
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class DjenRepositoryImpl(
    private val remoteDataSource: DjenRemoteDataSource,
    private val localPublicacaoRepository: LocalPublicacaoRepository,
    private val localProcessoRepository: RepositorioProcessos,
    private val djenMapper: DjenMapper,
    private val publicacaoPrazoMapper: PublicacaoPrazoMapper,
    private val clock: Clock = Clock.systemUTC(),
) : DjenRepository {

    override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
        val nomeVariacoes = params.nomeAdvogado
            ?.let { NomeVariacoes.gerar(it) }
            .orEmpty()

        // Queries sequenciais com pequeno intervalo entre elas: a API do CNJ
        // (comunicaapi.pje.jus.br) responde 429 quando recebe rajada de
        // requisicoes em paralelo. Latencia maior, porem confiavel.
        val (remoteResultOab, resultadosPorNome) = try {
            val oab = if (params.apenasPorNome) {
                Log.d(TAG, "Modo apenasPorNome ativo: pulando busca por OAB.")
                vazioFetchResult()
            } else {
                val resultado = remoteDataSource.buscarComunicacoes(
                    numeroOab = params.numeroOab,
                    ufOab = params.ufOab,
                    nomeAdvogado = null,
                    dataInicio = params.dataInicio,
                    dataFim = params.dataFim,
                )
                if (nomeVariacoes.isNotEmpty()) delay(THROTTLE_ENTRE_QUERIES_MS)
                resultado
            }
            val porNome = mutableListOf<DjenFetchResult>()
            nomeVariacoes.forEachIndexed { index, variacao ->
                if (index > 0) delay(THROTTLE_ENTRE_QUERIES_MS)
                porNome += remoteDataSource.buscarComunicacoes(
                    numeroOab = null,
                    ufOab = null,
                    nomeAdvogado = variacao,
                    dataInicio = params.dataInicio,
                    dataFim = params.dataFim,
                )
            }
            Pair(oab, porNome.toList())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return errorResumo(error)
        }

        val capturedAt = clock.instant()

        // Tagueia a origem para garantir ALTA em itens que vieram pela busca de OAB
        // (a API já confirmou o vínculo pela sigla+número), mesmo que o corpo do
        // texto não traga a OAB em formato legível.
        data class DtoTagged(val dto: DjenComunicacaoDto, val veioDaBuscaOab: Boolean)

        val itensOab = remoteResultOab.items.map { DtoTagged(it, veioDaBuscaOab = true) }
        val itensNome = resultadosPorNome.flatMap { result ->
            result.items.map { DtoTagged(it, veioDaBuscaOab = false) }
        }

        // distinctBy preserva o primeiro — itensOab vem antes, então itens que
        // aparecem em ambas as buscas são marcados como veioDaBuscaOab=true.
        val itensUnicos = (itensOab + itensNome).distinctBy { it.dto.id }

        val publicacoes: List<PublicacaoEntity> = itensUnicos.map { tagged ->
            val entity = djenMapper.toPublicacaoEntity(
                dto = tagged.dto,
                capturedAt = capturedAt,
                updatedAt = capturedAt,
            )
            val comPrazo = publicacaoPrazoMapper.comPrazoCalculado(entity)
            val confianca = classificarConfianca(
                texto = comPrazo.textoLimpo,
                veioDaBuscaOab = tagged.veioDaBuscaOab,
                params = params,
            )
            comPrazo.copy(confiancaMatch = confianca)
        }

        Log.d(TAG, "Publicações recebidas da API (${publicacoes.size}):")
        publicacoes.forEach { pub ->
            Log.d(
                TAG,
                "  id=${pub.id} | dataDisponibilizacao=${pub.dataDisponibilizacao} | tribunal=${pub.tribunal} | confianca=${pub.confiancaMatch}",
            )
        }

        val upsertResult = localPublicacaoRepository.upsertPublicacoes(publicacoes)
        Log.d(
            TAG,
            "Upsert: novas=${upsertResult.novas} | atualizadas=${upsertResult.atualizadas} | novasIds=${upsertResult.novasIds}",
        )
        val novasIds = upsertResult.novasIds.toSet()
        val publicacoesNovas = publicacoes.filter { it.id in novasIds }

        val processosParaSincronizar = publicacoesNovas
            .mapNotNull { publicacao -> publicacao.resolverProcessoParaSincronizar() }
            .distinctBy { it.numeroProcesso to it.tribunal?.uppercase() }
            .toList()

        val paginasNome = resultadosPorNome.sumOf(DjenFetchResult::paginasConsultadas)
        val totalRemotoNome = resultadosPorNome.firstNotNullOfOrNull { it.totalRemoto }

        return MonitorarDjenResumo(
            totalRemoto = remoteResultOab.totalRemoto ?: totalRemotoNome,
            totalRecebidas = upsertResult.totalRecebidas,
            novas = upsertResult.novas,
            atualizadas = upsertResult.atualizadas,
            sigilosas = publicacoes.count { it.isSigiloso },
            processosNovos = processosParaSincronizar.map { it.numeroProcesso },
            processosParaSincronizar = processosParaSincronizar,
            paginasConsultadas = remoteResultOab.paginasConsultadas + paginasNome,
            motivoParada = remoteResultOab.motivoParada.toDomain(),
            falhas = emptyList(),
            novasComPrazo = publicacoesNovas.count { it.prazoQuantidade != null },
            novasSigilosas = publicacoesNovas.count { it.isSigiloso },
        )
    }

    private fun classificarConfianca(
        texto: String?,
        veioDaBuscaOab: Boolean,
        params: MonitorarDjenParams,
    ): ConfiancaMatch {
        // Item devolvido via busca por OAB exata é, por definição, ALTA — a API
        // já confirmou o vínculo pela sigla+número. Se veio só via busca por
        // nome, deixa o classificador decidir a partir do corpo do texto.
        if (veioDaBuscaOab) return ConfiancaMatch.ALTA
        return PublicationMatcher.classificar(
            textoPublicacao = texto,
            numeroOab = params.numeroOab,
            ufOab = params.ufOab,
            nomeAdvogado = params.nomeAdvogado.orEmpty(),
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

    private suspend fun PublicacaoEntity.resolverProcessoParaSincronizar(): ProcessoDataJudSyncRequest? {
        val numeroProcesso = NumeroProcessoNormalizer.normalize(numeroProcesso) ?: return null
        if (!deveSincronizarDataJud(numeroProcesso)) return null

        return ProcessoDataJudSyncRequest(
            numeroProcesso = numeroProcesso,
            tribunal = tribunal,
        )
    }

    private suspend fun deveSincronizarDataJud(numeroProcesso: String): Boolean {
        val processoLocal = localProcessoRepository.obterProcesso(numeroProcesso)
        return processoLocal == null || processoLocal.syncStatus.isReenriquecivel(processoLocal.dataJudTentativasRestantes)
    }

    private fun ProcessoSyncStatus.isReenriquecivel(tentativasRestantes: Int): Boolean =
        when (this) {
            ProcessoSyncStatus.PENDING,
            ProcessoSyncStatus.STALE,
            -> true

            ProcessoSyncStatus.NOT_FOUND,
            ProcessoSyncStatus.FAILED,
            -> tentativasRestantes > 0

            ProcessoSyncStatus.SYNCED -> false
        }

    private fun vazioFetchResult(): DjenFetchResult =
        DjenFetchResult(
            items = emptyList(),
            totalRemoto = null,
            paginasConsultadas = 0,
            motivoParada = DjenPaginationStopReason.EMPTY_PAGE,
        )

    companion object {
        private const val TAG = "DjenRepositoryImpl"
        private const val THROTTLE_ENTRE_QUERIES_MS = 600L
    }
}
