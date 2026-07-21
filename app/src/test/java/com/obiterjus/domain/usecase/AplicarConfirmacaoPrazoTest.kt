package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.PublicacaoPrazo
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AplicarConfirmacaoPrazoTest {

    private val prazoDaEntity = PublicacaoPrazo(
        quantidade = 5,
        unidade = "dias",
        diasUteis = false,
        textoOriginal = "prazo de 5 dias recalculado no sync",
        dataLimiteEstimada = LocalDate.of(2026, 7, 20),
    )

    @Test
    fun `sem linha sugerida mantem o prazo da publicacao`() {
        assertEquals(prazoDaEntity, prazoDaEntity.aplicarConfirmacao(null))
    }

    @Test
    fun `linha sugerida sobrepoe dados do prazo alem da confirmacao`() {
        val manual = PrazoSugeridoEntity(
            id = 1L,
            publicacaoId = 7L,
            quantidade = 30,
            unidade = "dias",
            diasUteis = true,
            textoOriginal = "Prazo cadastrado manualmente",
            dataLimite = LocalDate.of(2026, 8, 12),
            isConfirmado = true,
            idExternoCalendario = "evt-1",
            provedorCalendario = ProvedorCalendario.GOOGLE.codigo,
        )

        val resultado = prazoDaEntity.aplicarConfirmacao(manual)

        assertEquals(30, resultado.quantidade)
        assertTrue(resultado.diasUteis)
        assertEquals("Prazo cadastrado manualmente", resultado.textoOriginal)
        assertEquals(LocalDate.of(2026, 8, 12), resultado.dataLimiteEstimada)
        assertTrue(resultado.isConfirmado)
        assertEquals("evt-1", resultado.idExternoCalendario)
        assertEquals(ProvedorCalendario.GOOGLE.codigo, resultado.provedorCalendario)
    }

    @Test
    fun `linha sem data limite preserva a data estimada da publicacao`() {
        val sugerido = PrazoSugeridoEntity(
            id = 1L,
            publicacaoId = 7L,
            quantidade = 5,
            unidade = "dias",
            diasUteis = false,
            textoOriginal = "prazo de 5 dias",
            dataLimite = null,
            isConfirmado = true,
        )

        val resultado = prazoDaEntity.aplicarConfirmacao(sugerido)

        assertEquals(LocalDate.of(2026, 7, 20), resultado.dataLimiteEstimada)
        assertTrue(resultado.isConfirmado)
    }
}
