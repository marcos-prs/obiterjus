package com.obiterjus.core.parser

import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.NaturezaProcesso
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DjenPrazoExtractorTest {

    @Test
    fun extractsCalendarDayDeadlineViaApi() = runBlocking {
        val extractor = DjenPrazoExtractor(
            CalculadoraPrazos(
                FakeCalendarioForenseDataSource(
                    RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-15"),
                ),
            ),
        )

        val prazo = extractor.extract(
            texto = "Intime-se a parte para manifestar-se no prazo de 15 dias.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
            tribunal = "TJMG",
        )

        assertEquals(15, prazo?.quantidade)
        assertEquals("dias", prazo?.unidade)
        // Sem qualificador no cível → dias úteis (CPC art. 219)
        assertEquals(true, prazo?.diasUteis)
        assertEquals(LocalDate.of(2026, 5, 15), prazo?.dataLimiteEstimada)
        assertEquals(ConfiancaCalculo.CONFIAVEL, prazo?.confiancaCalculo)
    }

    @Test
    fun processoPenalSemQualificadorContaDiasCorridos() = runBlocking {
        val extractor = DjenPrazoExtractor(
            CalculadoraPrazos(
                FakeCalendarioForenseDataSource(
                    RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-15"),
                ),
            ),
        )

        val prazo = extractor.extract(
            texto = "Intime-se a parte para manifestar-se no prazo de 15 dias.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
            tribunal = "TJMG",
            natureza = NaturezaProcesso.PENAL,
        )

        // Sem qualificador no penal → dias corridos (CPP art. 798)
        assertEquals(false, prazo?.diasUteis)
    }

    @Test
    fun corridosExplicitoVencePadraoCivel() = runBlocking {
        val extractor = DjenPrazoExtractor(
            CalculadoraPrazos(
                FakeCalendarioForenseDataSource(
                    RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-15"),
                ),
            ),
        )

        val prazo = extractor.extract(
            texto = "Manifeste-se no prazo de 10 dias corridos.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
            tribunal = "TJMG",
        )

        assertEquals(false, prazo?.diasUteis)
    }

    @Test
    fun extractsBusinessDayDeadline() = runBlocking {
        val extractor = DjenPrazoExtractor(
            CalculadoraPrazos(
                FakeCalendarioForenseDataSource(
                    RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-08"),
                ),
            ),
        )

        val prazo = extractor.extract(
            texto = "Fica intimado para regularizar em 5 dias úteis.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
            tribunal = "TJMG",
        )

        assertEquals(5, prazo?.quantidade)
        assertEquals(true, prazo?.diasUteis)
        assertEquals(LocalDate.of(2026, 5, 8), prazo?.dataLimiteEstimada)
    }

    @Test
    fun semTribunalPrazoFicaPendenteSemDataLocal() = runBlocking {
        val extractor = DjenPrazoExtractor(
            CalculadoraPrazos(
                FakeCalendarioForenseDataSource(
                    RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-15"),
                ),
            ),
        )

        val prazo = extractor.extract(
            texto = "Intime-se a parte para manifestar-se no prazo de 15 dias.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
        )

        // Extração textual funciona, mas sem tribunal não há cálculo — e o app
        // nunca calcula localmente.
        assertEquals(15, prazo?.quantidade)
        assertNull(prazo?.dataLimiteEstimada)
        assertEquals(ConfiancaCalculo.PENDENTE, prazo?.confiancaCalculo)
    }

    @Test
    fun apiIndisponivelPrazoFicaPendente() = runBlocking {
        val extractor = DjenPrazoExtractor(
            CalculadoraPrazos(FakeCalendarioForenseDataSource(RespostaPrazo(estado = "INDISPONIVEL"))),
        )

        val prazo = extractor.extract(
            texto = "Intime-se a parte para manifestar-se no prazo de 15 dias.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
            tribunal = "TJMG",
        )

        assertEquals(15, prazo?.quantidade)
        assertNull(prazo?.dataLimiteEstimada)
        assertEquals(ConfiancaCalculo.PENDENTE, prazo?.confiancaCalculo)
    }

    @Test
    fun bloqueioDaApiViraIncerto() = runBlocking {
        val extractor = DjenPrazoExtractor(
            CalculadoraPrazos(
                FakeCalendarioForenseDataSource(RespostaPrazo(estado = "BLOQUEADO_AMBIGUIDADE")),
            ),
        )

        val prazo = extractor.extract(
            texto = "Intime-se a parte para manifestar-se no prazo de 15 dias.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
            tribunal = "TJMG",
        )

        assertEquals(ConfiancaCalculo.INCERTO, prazo?.confiancaCalculo)
    }

    private class FakeCalendarioForenseDataSource(
        private val resposta: RespostaPrazo,
    ) : CalendarioForenseDataSource {
        override suspend fun calcularPrazo(pedido: PedidoCalculoPrazo): RespostaPrazo = resposta
    }
}
