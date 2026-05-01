package com.obiterjus.core.database

import androidx.room.TypeConverter
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant
import java.time.LocalDate

class CnjTypeConverters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun processoSyncStatusToString(value: ProcessoSyncStatus?): String? = value?.name

    @TypeConverter
    fun stringToProcessoSyncStatus(value: String?): ProcessoSyncStatus? =
        value?.let(ProcessoSyncStatus::valueOf)
}
