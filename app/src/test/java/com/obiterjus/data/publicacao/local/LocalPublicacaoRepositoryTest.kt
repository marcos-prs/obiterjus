package com.obiterjus.data.publicacao.local

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPublicacaoRepositoryTest {
    @Test
    fun reusesExistingIdWhenHashAlreadyExistsAndDeduplicatesIncomingBatch() = runBlocking {
        val existente = publicacao(
            id = 10L,
            hash = "hash-a",
            textoLimpo = "publicacao existente",
        )
        val dao = FakePublicacaoDao(
            existingByHash = mapOf("hash-a" to existente),
            existingIds = setOf(10L),
        )
        val repository = LocalPublicacaoRepository(dao)

        val resultado = repository.upsertPublicacoes(
            listOf(
                publicacao(id = 20L, hash = " hash-a ", textoLimpo = "publicacao nova 1"),
                publicacao(id = 21L, hash = "hash-a", textoLimpo = "publicacao nova 2"),
                publicacao(id = 30L, hash = null, textoLimpo = "sem hash"),
            ),
        )

        assertEquals(3, resultado.totalRecebidas)
        assertEquals(1, resultado.novas)
        assertEquals(1, resultado.atualizadas)
        assertEquals(listOf(30L), resultado.novasIds)
        assertEquals(2, dao.upserted.size)
        assertTrue(dao.upserted.any { it.id == 10L && it.hash == "hash-a" })
        assertTrue(dao.upserted.any { it.id == 30L && it.hash == null })
    }

    private class FakePublicacaoDao(
        private val existingByHash: Map<String, PublicacaoEntity>,
        private val existingIds: Set<Long>,
    ) : PublicacaoDao {
        var upserted: List<PublicacaoEntity> = emptyList()

        override suspend fun upsert(publicacao: PublicacaoEntity) {
            upserted = upserted + publicacao
        }

        override suspend fun upsertAll(publicacoes: List<PublicacaoEntity>) {
            upserted = publicacoes
        }

        override suspend fun getExistingIds(ids: List<Long>): List<Long> =
            ids.filter(existingIds::contains)

        override suspend fun getByHashes(hashes: List<String>): List<PublicacaoEntity> =
            hashes.mapNotNull(existingByHash::get)

        override suspend fun getById(id: Long): PublicacaoEntity? =
            upserted.firstOrNull { it.id == id }

        override fun observeById(id: Long): Flow<PublicacaoEntity?> = emptyFlow()

        override suspend fun getByIds(ids: List<Long>): List<PublicacaoEntity> =
            upserted.filter { it.id in ids }

        override fun observePublicacoes(
            numeroProcesso: String?,
            tribunal: String?,
            tipoComunicacao: String?,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
            somenteSigilosas: Boolean?,
        ): Flow<List<PublicacaoEntity>> = emptyFlow()

        override fun observePorProcesso(numeroProcesso: String): Flow<List<PublicacaoEntity>> = emptyFlow()

        override suspend fun getNumerosProcessoDistintos(): List<String> = emptyList()
    }

    private fun publicacao(
        id: Long,
        hash: String?,
        textoLimpo: String,
    ): PublicacaoEntity =
        PublicacaoEntity(
            id = id,
            hash = hash,
            numeroProcesso = "50110879520258130245",
            participantesJson = null,
            prazoQuantidade = null,
            prazoUnidade = null,
            prazoDiasUteis = false,
            prazoTexto = null,
            prazoDataLimite = null,
            dataDisponibilizacao = LocalDate.of(2026, 4, 29),
            tribunal = "TJMG",
            tipoComunicacao = "Intimacao",
            nomeOrgao = "1a Vara",
            idOrgao = null,
            textoRaw = textoLimpo,
            textoLimpo = textoLimpo,
            textoPossuiHtml = false,
            textoPossuiErroTemplate = false,
            isSigiloso = false,
            ativo = true,
            fonte = "DJEN",
            capturadoEm = Instant.parse("2026-04-29T12:00:00Z"),
            atualizadoEm = Instant.parse("2026-04-29T12:00:00Z"),
        )
}
