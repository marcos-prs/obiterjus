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
    fun cleanRemovesStyleAndScriptBlocksIncludingTheirContent() {
        val html = """
            <html><head><style type="text/css">
            body { padding: 10px; font-family: Arial, sans-serif; }
            </style></head>
            <body><script>console.log('x');</script>
            <p>Intime-se a parte.</p></body></html>
        """.trimIndent()

        val result = DjenTextCleaner.clean(html)

        assertTrue(result.hasHtml)
        assertFalse(result.clean.contains("padding"))
        assertFalse(result.clean.contains("font-family"))
        assertFalse(result.clean.contains("console.log"))
        assertEquals("Intime-se a parte.", result.clean)
    }

    @Test
    fun cleanPreservesPlainTextAndDetectsTemplateError() {
        val result = DjenTextCleaner.clean("Falha no template da comunicação")

        assertFalse(result.hasHtml)
        assertTrue(result.hasTemplateError)
        assertEquals("Falha no template da comunicação", result.clean)
    }
}
