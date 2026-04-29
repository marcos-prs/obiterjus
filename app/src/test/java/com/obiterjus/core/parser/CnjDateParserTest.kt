package com.obiterjus.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CnjDateParserTest {
    @Test
    fun parseInstantAcceptsIsoWithTimezone() {
        assertEquals(
            Instant.parse("2026-04-02T05:01:47.757Z"),
            CnjDateParser.parseInstant("2026-04-02T05:01:47.757000Z"),
        )
    }

    @Test
    fun parseLocalDateAcceptsSimpleBrazilianAndCompactDataJudDates() {
        assertEquals(LocalDate.of(2026, 4, 20), CnjDateParser.parseLocalDate("2026-04-20"))
        assertEquals(LocalDate.of(2026, 4, 20), CnjDateParser.parseLocalDate("20/04/2026"))
        assertEquals(LocalDate.of(2026, 2, 19), CnjDateParser.parseLocalDate("20260219143412"))
    }

    @Test
    fun parseInvalidDateReturnsNull() {
        assertNull(CnjDateParser.parseInstant(""))
        assertNull(CnjDateParser.parseLocalDate("sem-data"))
    }
}
