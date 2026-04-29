package com.obiterjus.core.parser

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object CnjDateParser {
    private val brDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dataJudCompactFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun parseInstant(raw: String?): Instant? {
        val value = raw.cleanDateInput() ?: return null

        return parseSafely { Instant.parse(value) }
            ?: parseSafely { OffsetDateTime.parse(value).toInstant() }
            ?: parseSafely { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
            ?: parseSafely { LocalDateTime.parse(value, dataJudCompactFormatter).toInstant(ZoneOffset.UTC) }
            ?: parseLocalDate(value)?.atStartOfDay()?.toInstant(ZoneOffset.UTC)
    }

    fun parseLocalDate(raw: String?): LocalDate? {
        val value = raw.cleanDateInput() ?: return null

        return parseSafely { LocalDate.parse(value) }
            ?: parseSafely { LocalDate.parse(value, brDateFormatter) }
            ?: parseSafely { LocalDateTime.parse(value, dataJudCompactFormatter).toLocalDate() }
            ?: parseSafely { Instant.parse(value).atOffset(ZoneOffset.UTC).toLocalDate() }
            ?: parseSafely { OffsetDateTime.parse(value).toLocalDate() }
    }

    private inline fun <T> parseSafely(block: () -> T): T? =
        try {
            block()
        } catch (_: DateTimeParseException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun String?.cleanDateInput(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
