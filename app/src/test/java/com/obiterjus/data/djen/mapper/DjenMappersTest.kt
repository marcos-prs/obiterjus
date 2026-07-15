package com.obiterjus.data.djen.mapper

import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.publicacao.local.toParticipantes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate

class DjenMappersTest {

    private val djenMapper = DjenMapper()

    @Test
    fun mapsDjenDtoToAuditablePublicacaoEntity() = runBlocking {
        val capturedAt = Instant.parse("2026-04-29T12:00:00Z")
        val dto = DjenComunicacaoDto(
            id = 42L,
            hash = " hash ",
            dataDisponibilizacaoIso = "2026-04-29",
            siglaTribunal = " TJMG ",
            tipoComunicacao = " Intimacao ",
            nomeOrgao = " 1a Vara ",
            idOrgao = 7L,
            texto = "<p>Processo 5011087-95.2025.8.13.0245</p><p>Destinatário: Maria Silva</p><p>Advogado: Joao Souza OAB/MG 12345</p><p>Intime-se no prazo de 15 dias.</p>",
            numeroProcesso = null,
            ativo = null,
        )

        val entity = djenMapper.toPublicacaoEntity(dto, capturedAt, capturedAt)

        assertEquals(42L, entity.id)
        assertEquals("hash", entity.hash)
        assertEquals("50110879520258130245", entity.numeroProcesso)
        assertEquals(LocalDate.of(2026, 4, 29), entity.dataDisponibilizacao)
        assertEquals("TJMG", entity.tribunal)
        assertEquals("Intimacao", entity.tipoComunicacao)
        assertEquals("1a Vara", entity.nomeOrgao)
        assertEquals(
            "Processo 5011087-95.2025.8.13.0245\nDestinatário: Maria Silva\nAdvogado: Joao Souza OAB/MG 12345\nIntime-se no prazo de 15 dias.",
            entity.textoLimpo,
        )
        assertEquals(
            listOf("Destinatário", "Advogado"),
            entity.participantesJson.toParticipantes().map { it.tipo },
        )
        assertEquals("Maria Silva", entity.participantesJson.toParticipantes().first().nome)
        assertTrue(entity.textoPossuiHtml)
        assertFalse(entity.textoPossuiErroTemplate)
        assertFalse(entity.isSigiloso)
        assertTrue(entity.ativo)
        // O prazo não é calculado no mapper: isso acontece uma única vez, no
        // PublicacaoPrazoMapper, com o tribunal já normalizado.
        assertNull(entity.prazoQuantidade)
        assertNull(entity.prazoDataLimite)
        assertEquals("DJEN", entity.fonte)
    }

    @Test
    fun prefereDestinatariosEstruturadosEComplementaComRegex() = runBlocking {
        val capturedAt = Instant.parse("2026-04-29T12:00:00Z")
        val dto = DjenComunicacaoDto(
            id = 44L,
            texto = "<p>Destinatário: Antônio Araújo</p><p>Advogado: Joao Souza OAB/MG 54321</p>",
            destinatarios = listOf(
                com.obiterjus.data.djen.remote.dto.DjenDestinatarioDto(nome = "Antonio Araujo", polo = "A"),
            ),
            destinatarioAdvogados = listOf(
                com.obiterjus.data.djen.remote.dto.DjenDestinatarioAdvogadoDto(
                    advogado = com.obiterjus.data.djen.remote.dto.DjenAdvogadoDto(
                        nome = "Marcos Paulo",
                        numeroOab = "123456",
                        ufOab = "MG",
                    ),
                ),
            ),
        )

        val entity = djenMapper.toPublicacaoEntity(dto, capturedAt, capturedAt)
        val participantes = entity.participantesJson.toParticipantes()

        // Antonio estruturado (com polo) vence o "Destinatário" homônimo do texto;
        // Joao Souza (só no texto) complementa a lista.
        assertEquals(3, participantes.size)
        val antonio = participantes.first { it.nome == "Antonio Araujo" }
        assertEquals("Polo Ativo", antonio.tipo)
        val marcos = participantes.first { it.nome == "Marcos Paulo" }
        assertEquals("Advogado", marcos.tipo)
        assertEquals("OAB/MG 123456", marcos.documento)
        assertEquals(1, participantes.count { it.nome.contains("Joao Souza") })
    }

    @Test
    fun marksSecretPublicationFromCleanedText() = runBlocking {
        val capturedAt = Instant.parse("2026-04-29T12:00:00Z")
        val dto = DjenComunicacaoDto(
            id = 43L,
            texto = "Processo sob sigilo",
        )

        val entity = djenMapper.toPublicacaoEntity(dto, capturedAt, capturedAt)

        assertTrue(entity.isSigiloso)
    }
}
