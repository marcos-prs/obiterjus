package com.obiterjus.data.time

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A API CalendárioForense (FastAPI) valida o corpo e exige os campos com
 * default do DTO (origem, classe, termo_inicial, multiplicador, natureza).
 * Sem encodeDefaults na config do Json, o app envia um corpo incompleto e
 * toda chamada falha com HTTP 422 — regressão coberta por este teste.
 */
class PedidoCalculoPrazoSerializationTest {

    private val json: Json = CalendarioForenseRetrofitFactory.json

    @Test
    fun `serializa campos default exigidos pela API`() {
        val corpo = json.encodeToString(
            PedidoCalculoPrazo.serializer(),
            PedidoCalculoPrazo(
                tribunal = "TJMG",
                dataDisponibilizacao = "2026-07-01",
                prazo = 15,
                unidade = "dias_uteis",
            ),
        )

        val objeto = json.parseToJsonElement(corpo).jsonObject
        assertEquals("TJMG", objeto.getValue("tribunal").toString().trim('"'))
        assertEquals("2026-07-01", objeto.getValue("data_disponibilizacao").toString().trim('"'))
        assertTrue("origem ausente", objeto.containsKey("origem"))
        assertTrue("unidade ausente", objeto.containsKey("unidade"))
        assertTrue("classe ausente", objeto.containsKey("classe"))
        assertTrue("termo_inicial ausente", objeto.containsKey("termo_inicial"))
        assertTrue("multiplicador ausente", objeto.containsKey("multiplicador"))
        assertTrue("natureza ausente", objeto.containsKey("natureza"))
    }

    @Test
    fun `omite artigo nulo`() {
        val corpo = json.encodeToString(
            PedidoCalculoPrazo.serializer(),
            PedidoCalculoPrazo(
                tribunal = "TJMG",
                dataDisponibilizacao = "2026-07-01",
                prazo = 15,
            ),
        )

        assertFalse(json.parseToJsonElement(corpo).jsonObject.containsKey("artigo"))
    }
}
