package com.obiterjus.presentation.componentes.texto

import org.junit.Assert.assertEquals
import org.junit.Test

class NomeAbreviadoTest {

    @Test
    fun `nome completo com conectivos gera candidatos progressivos`() {
        val candidatos = candidatosAbreviacaoNome("Marcos Paulo Rodrigues da Silva Rocha")

        assertEquals(
            listOf(
                "Marcos Paulo Rodrigues da Silva Rocha",
                "Marcos P. R. S. Rocha",
                "Marcos Rocha",
                "Marcos R.",
                "Marcos",
            ),
            candidatos,
        )
    }

    @Test
    fun `nome com dois termos nao gera etapa de iniciais do meio`() {
        val candidatos = candidatosAbreviacaoNome("Ana Souza")

        assertEquals(
            listOf("Ana Souza", "Ana S.", "Ana"),
            candidatos,
        )
    }

    @Test
    fun `nome unico retorna apenas ele mesmo`() {
        assertEquals(listOf("Madonna"), candidatosAbreviacaoNome("Madonna"))
    }

    @Test
    fun `nome vazio nao quebra`() {
        assertEquals(listOf(""), candidatosAbreviacaoNome("   "))
    }

    @Test
    fun `espacos extras sao normalizados`() {
        val candidatos = candidatosAbreviacaoNome("  João   da  Silva  ")

        assertEquals("João da Silva", candidatos.first())
        assertEquals("João", candidatos.last())
    }

    @Test
    fun `candidatos nao contem duplicados`() {
        val candidatos = candidatosAbreviacaoNome("Ana Souza")

        assertEquals(candidatos.distinct(), candidatos)
    }
}
