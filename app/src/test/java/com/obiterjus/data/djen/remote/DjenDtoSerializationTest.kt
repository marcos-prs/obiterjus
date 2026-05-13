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
}
