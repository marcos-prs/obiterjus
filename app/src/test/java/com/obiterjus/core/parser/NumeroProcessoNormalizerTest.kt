package com.obiterjus.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumeroProcessoNormalizerTest {
    @Test
    fun normalizeAcceptsFormattedAndCompactCnjNumbers() {
        assertEquals(
            "50110879520258130245",
            NumeroProcessoNormalizer.normalize("5011087-95.2025.8.13.0245"),
        )
        assertEquals(
            "50110879520258130245",
            NumeroProcessoNormalizer.normalize("50110879520258130245"),
        )
    }

    @Test
    fun formatReturnsCnjMask() {
        assertEquals(
            "5011087-95.2025.8.13.0245",
            NumeroProcessoNormalizer.format("50110879520258130245"),
        )
    }

    @Test
    fun invalidProcessNumberReturnsNull() {
        assertNull(NumeroProcessoNormalizer.normalize("123"))
    }

    @Test
    fun extractAllReturnsDistinctNormalizedNumbers() {
        assertEquals(
            listOf("50110879520258130245"),
            NumeroProcessoNormalizer.extractAll(
                "Processo 5011087-95.2025.8.13.0245 e 50110879520258130245",
            ),
        )
    }
}
