package com.obiterjus.domain.model

import java.time.Instant

enum class SyncSource {
    DJEN, DATAJUD, FIRESTORE, ALL
}

data class SyncLog(
    val id: Long = 0,
    val executedAt: Instant,
    val isSuccess: Boolean,
    val source: SyncSource,
    val message: String? = null,
    val itemsProcessed: Int = 0,
)
