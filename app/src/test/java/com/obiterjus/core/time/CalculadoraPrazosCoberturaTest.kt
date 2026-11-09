package com.obiterjus.core.time

import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.CoberturaJudicial
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.data.time.RespostaSaude
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A API tem camada judicial (recesso, emendas, Semana Santa estendida, Dia da
 * Justiça — por portaria de cada tribunal) para 13 dos 92 órgãos cadastrados
 * em 2026. Nos outros 79 a contagem pula apenas fim de semana, feriado legal e
 * a suspensão do art. 220 — e a API ainda responde CONFIAVEL.
 *
 * Prazo errado rotulado como confiável é pior que prazo ausente: o app consulta
 * GET /saude e rebaixa esses tribunais para Incerto por conta própria,
 * inclusive contra servidor anterior ao BLOQUEADO_SEM_CALENDARIO.
 */
class CalculadoraPrazosCoberturaTest {

    private val dataBase = LocalDate.of(2026, 6, 23)
    private val vencimento = LocalDate.of(2026, 7, 15)

    @Test
    fun `tribunal zerado nao sai confiavel nem com CONFIAVEL da api`() = runTest {
        val resultado = calcular(CalculadoraPrazos(fake()), tribunal = "TJBA")

        assertEquals(ResultadoCalculoPrazo.Incerto(vencimento), resultado)
    }

    @Test
    fun `tribunal com detalhe zero nao sai confiavel`() = runTest {
        // STM fora da lista zerados, mas detalhe diz 0 portarias coletadas.
        val fake = fake(
            CoberturaJudicial(
                zerados = listOf("TJAC"),
                detalhe = mapOf("STM" to 0, "TJMG" to 25),
            ),
        )

        val resultado = calcular(CalculadoraPrazos(fake), tribunal = "STM")

        assertEquals(ResultadoCalculoPrazo.Incerto(vencimento), resultado)
    }

    @Test
    fun `tribunal com calendario judicial segue confiavel`() = runTest {
        val resultado = calcular(CalculadoraPrazos(fake()), tribunal = "TJMG")

        assertEquals(ResultadoCalculoPrazo.Confiavel(vencimento), resultado)
    }

    @Test
    fun `saude fora do ar preserva o estado que a api devolveu`() = runTest {
        // Ausência de informação não é motivo para rebaixar, e nunca derruba o
        // cálculo: a instância free do Render hiberna todo dia.
        val fake = FakeCalendarioForense { throw IOException("render hibernando") }

        val resultado = calcular(CalculadoraPrazos(fake), tribunal = "TJBA")

        assertEquals(ResultadoCalculoPrazo.Confiavel(vencimento), resultado)
    }

    @Test
    fun `corpo de saude sem cobertura preserva o estado que a api devolveu`() = runTest {
        val fake = FakeCalendarioForense { RespostaSaude() }

        val resultado = calcular(CalculadoraPrazos(fake), tribunal = "TJBA")

        assertEquals(ResultadoCalculoPrazo.Confiavel(vencimento), resultado)
    }

    @Test
    fun `cobertura e consultada uma vez por dia por ano`() = runTest {
        val fake = fake()
        val relogio = RelogioMutavel(Instant.parse("2026-06-23T09:00:00Z"))
        val calculadora = CalculadoraPrazos(fake, relogio)

        repeat(3) { calcular(calculadora, tribunal = "TJBA") }
        assertEquals(1, fake.chamadasSaude)

        // A cobertura muda quando entra uma coleta nova: no dia seguinte
        // pergunta de novo.
        relogio.agora = relogio.agora.plusSeconds(VINTE_E_CINCO_HORAS)
        calcular(calculadora, tribunal = "TJBA")
        assertEquals(2, fake.chamadasSaude)
    }

    @Test
    fun `ano do vencimento sem cobertura tambem rebaixa`() = runTest {
        // Contagem iniciada em dezembro vence no ano seguinte: o calendário que
        // falta é o de 2027, não o de 2026.
        val fake = FakeCalendarioForense(
            resposta = RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2027-01-15"),
            saude = { ano ->
                if (ano == 2027) {
                    RespostaSaude(CoberturaJudicial(zerados = listOf("TJMG")))
                } else {
                    RespostaSaude(CoberturaJudicial(detalhe = mapOf("TJMG" to 25)))
                }
            },
        )

        val resultado = calcular(
            CalculadoraPrazos(fake),
            tribunal = "TJMG",
            dataBase = LocalDate.of(2026, 12, 20),
        )

        assertEquals(ResultadoCalculoPrazo.Incerto(LocalDate.of(2027, 1, 15)), resultado)
    }

    private suspend fun calcular(
        calculadora: CalculadoraPrazos,
        tribunal: String,
        dataBase: LocalDate = this.dataBase,
    ): ResultadoCalculoPrazo = calculadora.calcularDataLimite(
        dataBase = dataBase,
        quantidade = 15,
        unidade = "dias",
        diasUteis = true,
        tribunal = tribunal,
    )

    private fun fake(
        cobertura: CoberturaJudicial = CoberturaJudicial(
            zerados = listOf("TJBA", "TJAC", "STM"),
            detalhe = mapOf("TJMG" to 25, "TJSP" to 26),
        ),
    ) = FakeCalendarioForense { RespostaSaude(cobertura) }

    private class FakeCalendarioForense(
        private val resposta: RespostaPrazo = RespostaPrazo(
            estado = "CONFIAVEL",
            dataVencimento = "2026-07-15",
        ),
        private val saude: (Int) -> RespostaSaude,
    ) : CalendarioForenseDataSource {
        var chamadasSaude = 0
            private set

        override suspend fun calcularPrazo(pedido: PedidoCalculoPrazo): RespostaPrazo = resposta

        override suspend fun consultarSaude(ano: Int): RespostaSaude {
            chamadasSaude += 1
            return saude(ano)
        }
    }

    private class RelogioMutavel(var agora: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = agora
    }

    private companion object {
        const val VINTE_E_CINCO_HORAS = 25L * 60 * 60
    }
}
