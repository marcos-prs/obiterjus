package com.obiterjus.data.publicacao.local

import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.PublicacoesRepository
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
) : PublicacoesRepository {
    override fun observarPublicacoes(): Flow<List<Publicacao>> =
        observePublicacoes().map { entities ->
            entities.map(PublicacaoEntity::paraDominio)
        }

    override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
        observePorProcesso(numeroProcesso).map { entities ->
            entities.map(PublicacaoEntity::paraDominio)
        }

    override fun observarPublicacao(id: Long): Flow<Publicacao?> =
        publicacaoDao.observeById(id).map { it?.paraDominio() }

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

        val publicacoesNormalizadas = publicacoes
            .map { it.normalizarHash() }
            .deduplicarConflitosHash()
        val hashes = publicacoesNormalizadas
            .mapNotNull { it.hash }
            .distinct()
        val publicacoesExistentesPorHash = if (hashes.isEmpty()) {
            emptyMap()
        } else {
            publicacaoDao.getByHashes(hashes)
                .associateBy { it.hash.orEmpty() }
        }
        val publicacoesParaSalvar = publicacoesNormalizadas.map { publicacao ->
            val hash = publicacao.hash?.trim()?.takeIf { it.isNotEmpty() }
            if (hash == null) {
                publicacao
            } else {
                publicacoesExistentesPorHash[hash]?.let { existente ->
                    publicacao.copy(id = existente.id, hash = hash)
                } ?: publicacao.copy(hash = hash)
            }
        }
        val ids = publicacoesParaSalvar.map { it.id }
        val existingIds = publicacaoDao.getExistingIds(ids).toSet()
        publicacaoDao.upsertAll(publicacoesParaSalvar)

        val datasAfetadas = publicacoesParaSalvar
            .mapNotNull { it.dataDisponibilizacao }
            .toSet()
        reconciliarDuplicatas(datasAfetadas)

        val newIds = ids.filterNot(existingIds::contains)
        return UpsertPublicacoesResult(
            totalRecebidas = publicacoes.size,
            novas = newIds.size,
            atualizadas = publicacoesParaSalvar.size - newIds.size,
            novasIds = newIds,
        )
    }

    /**
     * Para cada data afetada, agrupa publicações por chave semântica (mesmo
     * conteúdo no mesmo dia) e marca todas, exceto a primeira a chegar, como
     * duplicatas. Não dedup entre dias diferentes — texto idêntico em datas
     * distintas pode ser retificação/republicação com sentido jurídico próprio.
     */
    private suspend fun reconciliarDuplicatas(datas: Set<LocalDate>) {
        if (datas.isEmpty()) return
        for (data in datas) {
            val entidadesDoDia = publicacaoDao.getByDataDisponibilizacao(data)
            if (entidadesDoDia.size <= 1) {
                // Mesmo com 0/1 entrada, garante que o estado fique limpo.
                entidadesDoDia.forEach { entidade ->
                    if (entidade.duplicataDe != null || entidade.totalDuplicatas != 0) {
                        publicacaoDao.atualizarStatusDuplicata(entidade.id, null, 0)
                    }
                }
                continue
            }

            val grupos = entidadesDoDia.groupBy { it.chaveDeduplicacao() }
            for (grupo in grupos.values) {
                val ordenadas = grupo.sortedWith(
                    compareBy<PublicacaoEntity> { it.capturadoEm }.thenBy { it.id },
                )
                val canonica = ordenadas.first()
                val duplicatas = ordenadas.drop(1)
                val totalDuplicatas = duplicatas.size

                if (canonica.duplicataDe != null || canonica.totalDuplicatas != totalDuplicatas) {
                    publicacaoDao.atualizarStatusDuplicata(
                        id = canonica.id,
                        duplicataDe = null,
                        totalDuplicatas = totalDuplicatas,
                    )
                }
                for (duplicata in duplicatas) {
                    if (duplicata.duplicataDe != canonica.id || duplicata.totalDuplicatas != 0) {
                        publicacaoDao.atualizarStatusDuplicata(
                            id = duplicata.id,
                            duplicataDe = canonica.id,
                            totalDuplicatas = 0,
                        )
                    }
                }
            }
        }
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

private fun PublicacaoEntity.normalizarHash(): PublicacaoEntity =
    copy(hash = hash?.trim()?.takeIf { it.isNotEmpty() })

private fun List<PublicacaoEntity>.deduplicarConflitosHash(): List<PublicacaoEntity> {
    val comparator = compareByDescending<PublicacaoEntity> { it.atualizadoEm }
        .thenByDescending { it.capturadoEm }
        .thenByDescending { it.id }

    return groupBy { entity ->
        entity.hash ?: "__sem_hash__:${entity.id}"
    }.values.mapNotNull { entidades ->
        entidades.maxWithOrNull(comparator)
    }
}

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
        confiancaMatch = confiancaMatch,
        duplicataDe = duplicataDe,
        totalDuplicatas = totalDuplicatas,
    )

internal fun PublicacaoEntity.chaveDeduplicacao(): String {
    val texto = textoLimpo?.normalizarParaDedup().orEmpty()
    return listOf(
        numeroProcesso.orEmpty(),
        tribunal.orEmpty().uppercase(),
        idOrgao?.toString().orEmpty(),
        tipoComunicacao.orEmpty().lowercase(),
        texto,
    ).joinToString("||")
}

private fun String.normalizarParaDedup(): String =
    trim().lowercase().replace(WHITESPACE_REGEX, " ")

private val WHITESPACE_REGEX = Regex("\\s+")

private fun PublicacaoEntity.prazo(): PublicacaoPrazo? {
    val quantidade = prazoQuantidade ?: return null
    val unidade = prazoUnidade?.takeIf { it.isNotBlank() } ?: return null
    val texto = prazoTexto?.takeIf { it.isNotBlank() } ?: return null
    val confianca = prazoConfianca?.let { runCatching { ConfiancaCalculo.valueOf(it) }.getOrNull() }
        ?: ConfiancaCalculo.ESTIMADO
    return PublicacaoPrazo(
        quantidade = quantidade,
        unidade = unidade,
        diasUteis = prazoDiasUteis,
        textoOriginal = texto,
        dataLimiteEstimada = prazoDataLimite,
        confiancaCalculo = confianca,
    )
}
