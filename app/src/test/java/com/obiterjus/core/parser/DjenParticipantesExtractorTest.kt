package com.obiterjus.core.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class DjenParticipantesExtractorTest {
    @Test
    fun extractsParticipantsFromDjenText() {
        val result = DjenParticipantesExtractor.extract(
            """
            Processo 5011087-95.2025.8.13.0245
            Destinatário: Maria Silva
            Advogado: Joao Souza OAB/MG 12345
            Requerido: Banco Exemplo S.A.
            """.trimIndent(),
        )

        assertEquals(listOf("Destinatário", "Advogado", "Requerido"), result.map { it.tipo })
        assertEquals("Maria Silva", result[0].nome)
        assertEquals("OAB/MG 12345", result[1].documento)
    }
}
