package com.obiterjus.domain.usecase.matcher

import com.obiterjus.domain.model.ConfiancaMatch
import org.junit.Assert.assertEquals
import org.junit.Test

class PublicationMatcherTest {

    @Test
    fun `OAB exata no corpo classifica como ALTA`() {
        val texto = "Ato publicado pelo advogado OAB MG 123456 referente ao processo"
        assertEquals(
            ConfiancaMatch.ALTA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Qualquer Nome"),
        )
    }

    @Test
    fun `OAB com pontuacao classifica como ALTA`() {
        val texto = "Advogado OAB/MG 123.456, requereu"
        assertEquals(
            ConfiancaMatch.ALTA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Qualquer Nome"),
        )
    }

    @Test
    fun `OAB invertida e com traco classifica como ALTA`() {
        val texto = "Advogado 123-456 / MG, requereu"
        assertEquals(
            ConfiancaMatch.ALTA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Qualquer Nome"),
        )
    }

    @Test
    fun `OAB errada e nome ausente classifica como BAIXA`() {
        val texto = "OAB MG 999999 referente ao advogado Ze Ninguem"
        assertEquals(
            ConfiancaMatch.BAIXA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Marcos Silva"),
        )
    }

    @Test
    fun `nome completo sem OAB classifica como MEDIA`() {
        val texto = "Intime-se o advogado Marcos Silva Carvalho para manifestar"
        assertEquals(
            ConfiancaMatch.MEDIA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Marcos Silva Carvalho"),
        )
    }

    @Test
    fun `nome com abreviacao do meio classifica como MEDIA`() {
        val texto = "Intime-se o advogado Marcos S. Carvalho para manifestar"
        assertEquals(
            ConfiancaMatch.MEDIA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Marcos Silva Carvalho"),
        )
    }

    @Test
    fun `nome com abreviacao sem ponto do meio classifica como MEDIA`() {
        val texto = "Intime-se o advogado Marcos S Carvalho para manifestar"
        assertEquals(
            ConfiancaMatch.MEDIA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Marcos Silva Carvalho"),
        )
    }

    @Test
    fun `cadastro tem stopwords mas publicacao usa primeiro mais ultimo classifica como MEDIA`() {
        val texto = "Intime-se o advogado Marcos Carvalho para manifestar"
        // Tokenização remove stopwords "da", então tokens = [marcos, silva, carvalho].
        // Como middleRegex aceita o nome do meio opcionalmente, "Marcos Carvalho" casa.
        assertEquals(
            ConfiancaMatch.MEDIA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Marcos da Silva Carvalho"),
        )
    }

    @Test
    fun `fuzzy de typo (Sousa vs Souza) classifica como BAIXA para forcar verificacao`() {
        // Match por Levenshtein não tem como distinguir typo do usuário de homônimo
        // real com sobrenome parecido. Por isso a regra é não auto-confirmar: vai
        // entrar no banco com badge "Verificar".
        val texto = "Intime-se o advogado Marcos Silva Sousa para"
        assertEquals(
            ConfiancaMatch.BAIXA,
            PublicationMatcher.classificar(texto, "123456", "MG", "Marcos Silva Souza"),
        )
    }

    @Test
    fun `texto vazio classifica como BAIXA`() {
        assertEquals(
            ConfiancaMatch.BAIXA,
            PublicationMatcher.classificar(null, "123456", "MG", "Marcos Silva"),
        )
        assertEquals(
            ConfiancaMatch.BAIXA,
            PublicationMatcher.classificar("", "123456", "MG", "Marcos Silva"),
        )
    }
}
