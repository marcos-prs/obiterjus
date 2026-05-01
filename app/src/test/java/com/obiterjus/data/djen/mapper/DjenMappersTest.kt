package com.obiterjus.data.djen.mapper

import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.publicacao.local.toParticipantes
import com.obiterjus.data.time.BrasilApiDataSource
import com.obiterjus.data.time.FeriadoDto
import com.obiterjus.data.time.FeriadoRepository
import com.obiterjus.domain.usecase.CalcularPrazoRegraUC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import java.time.Instant
import java.time.LocalDate

class DjenMappersTest {

    private val feriadoRepository = FeriadoRepository(
        brasilApiDataSource = object : BrasilApiDataSource {
            override suspend fun getFeriadosNacionais(ano: Int): Response<List<FeriadoDto>> =
                Response.success(emptyList())
        }
    )
    private val calculadoraPrazos = CalculadoraPrazos(feriadoRepository)
    private val djenPrazoExtractor = DjenPrazoExtractor(calculadoraPrazos)
    private val calcularPrazoRegraUC = CalcularPrazoRegraUC(calculadoraPrazos, djenPrazoExtractor)
    private val djenMapper = DjenMapper(calcularPrazoRegraUC)

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
        assertEquals(15, entity.prazoQuantidade)
        assertEquals("dias", entity.prazoUnidade)
        assertFalse(entity.prazoDiasUteis)
        assertEquals(LocalDate.of(2026, 5, 14), entity.prazoDataLimite)
        assertEquals("DJEN", entity.fonte)
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
