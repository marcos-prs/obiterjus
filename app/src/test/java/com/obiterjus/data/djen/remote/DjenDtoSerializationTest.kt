package com.obiterjus.data.djen.remote

import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class DjenDtoSerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun acceptsSnakeAndCamelCasePayloads() {
        val snakePayload = """
            {
              "items": [
                {
                  "id": 1,
                  "data_disponibilizacao": "2026-04-29",
                  "numero_processo": "50110879520258130245"
                }
              ]
            }
        """.trimIndent()
        val camelPayload = """
            {
              "comunicacoes": [
                {
                  "id": 2,
                  "dataDisponibilizacao": "2026-04-30",
                  "numeroProcesso": "50110879520258130246"
                }
              ]
            }
        """.trimIndent()

        val snake = json.decodeFromString(DjenResponseDto.serializer(), snakePayload)
        val camel = json.decodeFromString(DjenResponseDto.serializer(), camelPayload)

        assertEquals(1, snake.items.size)
        assertEquals("2026-04-29", snake.items.first().dataDisponibilizacaoIso)
        assertEquals("50110879520258130245", snake.items.first().numeroProcesso)

        assertEquals(1, camel.items.size)
        assertEquals("2026-04-30", camel.items.first().dataDisponibilizacaoIso)
        assertEquals("50110879520258130246", camel.items.first().numeroProcesso)
    }

    @Test
    fun parsesStructuredDestinatariosAndAdvogados() {
        val payload = """
            {
              "items": [
                {
                  "id": 3,
                  "numero_processo": "50110879520258130245",
                  "destinatarios": [
                    { "comunicacao_id": 3, "nome": "Antonio Araujo", "polo": "A" }
                  ],
                  "destinatarioadvogados": [
                    {
                      "id": 9,
                      "comunicacao_id": 3,
                      "advogado_id": 77,
                      "advogado": { "id": 77, "nome": "Marcos Paulo", "numero_oab": "123456", "uf_oab": "MG" }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val decoded = json.decodeFromString(DjenResponseDto.serializer(), payload)
        val item = decoded.items.single()

        assertEquals(1, item.destinatarios.size)
        assertEquals("Antonio Araujo", item.destinatarios.first().nome)
        assertEquals("A", item.destinatarios.first().polo)
        assertEquals(1, item.destinatarioAdvogados.size)
        assertEquals("Marcos Paulo", item.destinatarioAdvogados.first().advogado?.nome)
        assertEquals("123456", item.destinatarioAdvogados.first().advogado?.numeroOab)
        assertEquals("MG", item.destinatarioAdvogados.first().advogado?.ufOab)
    }

    @Test
    fun missingDestinatariosDefaultsToEmptyLists() {
        val payload = """{"items":[{"id":4}]}"""

        val decoded = json.decodeFromString(DjenResponseDto.serializer(), payload)

        assertEquals(0, decoded.items.single().destinatarios.size)
        assertEquals(0, decoded.items.single().destinatarioAdvogados.size)
    }
}
