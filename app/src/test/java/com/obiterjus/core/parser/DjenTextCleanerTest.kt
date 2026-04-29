package com.obiterjus.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DjenTextCleanerTest {
    @Test
    fun cleanTransformsHtmlIntoReadableText() {
        val result = DjenTextCleaner.clean("<p>Intime-se&nbsp;a parte.</p><p>Prazo: 5 dias.</p>")

        assertTrue(result.hasHtml)
        assertEquals("Intime-se a parte.\nPrazo: 5 dias.", result.clean)
    }

    @Test
    fun cleanPreservesPlainTextAndDetectsTemplateError() {
        val result = DjenTextCleaner.clean("Falha no template da comunicação")

        assertFalse(result.hasHtml)
        assertTrue(result.hasTemplateError)
        assertEquals("Falha no template da comunicação", result.clean)
    }
}
