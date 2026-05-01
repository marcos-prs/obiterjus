package com.obiterjus.data.publicacao.local

import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.RepositorioPublicacoes
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PublicacaoFilter(
    val numeroProcesso: String? = null,
    val tribunal: String? = null,
    val tipoComunicacao: String? = null,
    val dataInicio: LocalDate? = null,
    val dataFim: LocalDate? = null,
    val somenteSigilosas: Boolean? = null,
)

class LocalPublicacaoRepository(
    private val publicacaoDao: PublicacaoDao,
) : RepositorioPublicacoes {
    override fun observarPublicacoes(): Flow<List<Publicacao>> =
        observePublicacoes().map { entities ->
            entities.map(PublicacaoEntity::paraDominio)
        }

    override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
        observePorProcesso(numeroProcesso).map { entities ->
            entities.map(PublicacaoEntity::paraDominio)
        }

    fun observePublicacoes(filter: PublicacaoFilter = PublicacaoFilter()): Flow<List<PublicacaoEntity>> =
        publicacaoDao.observePublicacoes(
            numeroProcesso = filter.numeroProcesso,
            tribunal = filter.tribunal,
            tipoComunicacao = filter.tipoComunicacao,
            dataInicio = filter.dataInicio,
            dataFim = filter.dataFim,
            somenteSigilosas = filter.somenteSigilosas,
        )

    fun observePorProcesso(numeroProcesso: String): Flow<List<PublicacaoEntity>> =
        publicacaoDao.observePorProcesso(numeroProcesso)

    suspend fun getPublicacoes(ids: List<Long>): List<PublicacaoEntity> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            publicacaoDao.getByIds(ids)
        }

    suspend fun upsertPublicacoes(publicacoes: List<PublicacaoEntity>): UpsertPublicacoesResult {
        if (publicacoes.isEmpty()) {
            return UpsertPublicacoesResult()
        }

        val ids = publicacoes.map { it.id }
        val existingIds = publicacaoDao.getExistingIds(ids).toSet()
        publicacaoDao.upsertAll(publicacoes)

        val newIds = ids.filterNot(existingIds::contains)
        return UpsertPublicacoesResult(
            totalRecebidas = publicacoes.size,
            novas = newIds.size,
            atualizadas = publicacoes.size - newIds.size,
            novasIds = newIds,
        )
    }

    suspend fun getNumerosProcessoDistintos(): List<String> =
        publicacaoDao.getNumerosProcessoDistintos()
}

data class UpsertPublicacoesResult(
    val totalRecebidas: Int = 0,
    val novas: Int = 0,
    val atualizadas: Int = 0,
    val novasIds: List<Long> = emptyList(),
)

private fun PublicacaoEntity.paraDominio(): Publicacao =
    Publicacao(
        id = id,
        hash = hash,
        numeroProcesso = numeroProcesso,
        participantes = participantesJson.toParticipantes(),
        prazo = prazo(),
        dataDisponibilizacao = dataDisponibilizacao,
        tribunal = tribunal,
        tipoComunicacao = tipoComunicacao,
        nomeOrgao = nomeOrgao,
        textoLimpo = textoLimpo,
        isSigiloso = isSigiloso,
        fonte = fonte,
        capturadoEm = capturadoEm,
        atualizadoEm = atualizadoEm,
    )

private fun PublicacaoEntity.prazo(): PublicacaoPrazo? {
    val quantidade = prazoQuantidade ?: return null
    val unidade = prazoUnidade?.takeIf { it.isNotBlank() } ?: return null
    val texto = prazoTexto?.takeIf { it.isNotBlank() } ?: return null
    return PublicacaoPrazo(
        quantidade = quantidade,
        unidade = unidade,
        diasUteis = prazoDiasUteis,
        textoOriginal = texto,
        dataLimiteEstimada = prazoDataLimite,
    )
}
