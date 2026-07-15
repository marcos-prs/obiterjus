package com.obiterjus.domain.usecase

import com.obiterjus.core.parser.DjenPrazoExtractor
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

class CalcularPrazoRegraUCTest {

    private val decisaoNaoConhecimento =
        "Ante o exposto, não conheço do Agravo em Recurso Especial. Publique-se. Intimem-se."

    @Test
    fun decisaoDenegatoriaGeraSugestaoViaRuleset() = runBlocking {
        val fake = FakeCalendarioForense(
            RespostaPrazo(
                estado = "CONFIAVEL",
                dataVencimento = "2026-07-15",
                prazo = 15,
                unidade = "dias_uteis",
            ),
        )
        val uc = calcularPrazoRegraUC(fake)

        val prazo = uc.invoke(
            texto = decisaoNaoConhecimento,
            tipoComunicacao = "Intimação",
            dataDisponibilizacao = LocalDate.of(2026, 6, 24),
            tribunal = "STJ",
        )

        // O número de dias veio do ruleset da API (artigo 1070), não do app.
        assertEquals("ruleset", fake.ultimoPedido?.origem)
        assertEquals("1070", fake.ultimoPedido?.artigo)
        assertEquals(15, prazo?.quantidade)
        assertEquals(true, prazo?.diasUteis)
        assertEquals(LocalDate.of(2026, 7, 15), prazo?.dataLimiteEstimada)
        // Inferência jurídica nunca sai CONFIAVEL nem confirmada
        assertEquals(ConfiancaCalculo.INCERTO, prazo?.confiancaCalculo)
        assertEquals(false, prazo?.isConfirmado)
    }

    @Test
    fun sugestaoNaoSeAplicaAoPenal() = runBlocking {
        val fake = FakeCalendarioForense(
            RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-07-15", prazo = 15),
        )
        val uc = calcularPrazoRegraUC(fake)

        val prazo = uc.invoke(
            texto = decisaoNaoConhecimento,
            tipoComunicacao = "Intimação",
            dataDisponibilizacao = LocalDate.of(2026, 6, 24),
            tribunal = "STJ",
            natureza = NaturezaProcesso.PENAL,
        )

        assertNull(prazo)
    }

    @Test
    fun apiIndisponivelNaoGeraSugestao() = runBlocking {
        val fake = FakeCalendarioForense(RespostaPrazo(estado = "INDISPONIVEL"))
        val uc = calcularPrazoRegraUC(fake)

        val prazo = uc.invoke(
            texto = decisaoNaoConhecimento,
            tipoComunicacao = "Intimação",
            dataDisponibilizacao = LocalDate.of(2026, 6, 24),
            tribunal = "STJ",
        )

        // Sem número resolvido pela API não há sugestão — o app não conhece
        // os prazos legais; recalcula no próximo sync.
        assertNull(prazo)
    }

    private fun calcularPrazoRegraUC(fake: FakeCalendarioForense): CalcularPrazoRegraUC {
        val calculadora = CalculadoraPrazos(fake)
        return CalcularPrazoRegraUC(calculadora, DjenPrazoExtractor(calculadora))
    }

    private class FakeCalendarioForense(
        private val resposta: RespostaPrazo,
    ) : CalendarioForenseDataSource {
        var ultimoPedido: PedidoCalculoPrazo? = null

        override suspend fun calcularPrazo(pedido: PedidoCalculoPrazo): RespostaPrazo {
            ultimoPedido = pedido
            return resposta
        }
    }
}
