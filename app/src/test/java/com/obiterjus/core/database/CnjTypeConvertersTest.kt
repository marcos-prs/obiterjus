package com.obiterjus.core.database

import com.obiterjus.domain.model.ProcessoSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CnjTypeConvertersTest {
    private val converters = CnjTypeConverters()

    @Test
    fun convertsInstantUsingEpochMillis() {
        val instant = Instant.parse("2026-04-29T12:34:56Z")

        assertEquals(1_777_466_096_000L, converters.instantToLong(instant))
        assertEquals(instant, converters.longToInstant(1_777_466_096_000L))
    }

    @Test
    fun convertsLocalDateUsingIsoText() {
        val date = LocalDate.of(2026, 4, 29)

        assertEquals("2026-04-29", converters.localDateToString(date))
        assertEquals(date, converters.stringToLocalDate("2026-04-29"))
    }

    @Test
    fun convertsProcessoSyncStatusByName() {
        assertEquals("SYNCED", converters.processoSyncStatusToString(ProcessoSyncStatus.SYNCED))
        assertEquals(
            ProcessoSyncStatus.NOT_FOUND,
            converters.stringToProcessoSyncStatus("NOT_FOUND"),
        )
    }
}
