package com.obiterjus.domain.usecase.matcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicationMatcherTest {

    @Test
    fun `matches exato de OAB`() {
        val texto = "Ato publicado pelo advogado OAB MG 123456 referente ao processo"
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Qualquer Nome"))
    }

    @Test
    fun `matches OAB suja pontuacao`() {
        val texto = "Advogado OAB/MG 123.456, requereu"
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Qualquer Nome"))
    }

    @Test
    fun `matches OAB suja invertida e com traco`() {
        val texto = "Advogado 123-456 / MG, requereu"
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Qualquer Nome"))
    }

    @Test
    fun `nao matches OAB errada mas nome diferente`() {
        val texto = "OAB MG 999999 referente ao advogado Ze Ninguem"
        assertFalse(PublicationMatcher.matches(texto, "123456", "MG", "Marcos Silva"))
    }

    @Test
    fun `matches Nome completo sem OAB`() {
        val texto = "Intime-se o advogado Marcos Silva Carvalho para manifestar"
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Marcos Silva Carvalho"))
    }

    @Test
    fun `matches Nome com abreviacao do meio`() {
        val texto = "Intime-se o advogado Marcos S. Carvalho para manifestar"
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Marcos Silva Carvalho"))
    }

    @Test
    fun `matches Nome com abreviacao sem ponto do meio`() {
        val texto = "Intime-se o advogado Marcos S Carvalho para manifestar"
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Marcos Silva Carvalho"))
    }

    @Test
    fun `matches Nome com stopwords cadastradas`() {
        val texto = "Intime-se o advogado Marcos Carvalho para manifestar"
        // Cadastro tem "da Silva", a publicação sonegou o "Silva" -> a tokenização exige First e Last.
        // Se a publicação for "Marcos Carvalho", FIRST é Marcos, LAST é Carvalho.
        // middleRegex accepts middle parts optionally!
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Marcos da Silva Carvalho"))
    }

    @Test
    fun `matches Fuzzy digitacao errada (Souza vs Sousa)`() {
        val texto = "Intime-se o advogado Marcos Silva Sousa para"
        assertTrue(PublicationMatcher.matches(texto, "123456", "MG", "Marcos Silva Souza"))
    }
}
