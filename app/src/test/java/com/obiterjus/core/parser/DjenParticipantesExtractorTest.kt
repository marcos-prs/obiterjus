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

    @Test
    fun extractsPoloLabelsFromDjenText() {
        val result = DjenParticipantesExtractor.extract(
            """
            POLO ATIVO: Antonio Araujo
            POLO PASSIVO: Banco Exemplo S.A.
            ADVOGADO: Marcos Paulo Rocha de Souza - OAB: 140213/MG
            """.trimIndent(),
        )

        assertEquals(listOf("Ativo", "Passivo", "Advogado"), result.map { it.tipo })
        assertEquals("Antonio Araujo", result[0].nome)
        assertEquals("Banco Exemplo S.A.", result[1].nome)
        assertEquals("OAB: 140213/MG", result[2].documento)
    }
}
