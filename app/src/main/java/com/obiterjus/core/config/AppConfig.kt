package com.obiterjus.core.config

data class AppConfig(
    val djenBaseUrl: String = DEFAULT_DJEN_BASE_URL,
    val dataJudBaseUrl: String = DEFAULT_DATAJUD_BASE_URL,
    val dataJudApiKey: String = DEFAULT_DATAJUD_API_KEY,
    val djenDefaultItemsPerPage: Int = DEFAULT_DJEN_ITEMS_PER_PAGE,
    val syncLookbackDays: Long = DEFAULT_SYNC_LOOKBACK_DAYS,
    val djenToken: String = "",
    val dataJudEnabled: Boolean = true,
    val djenEnabled: Boolean = true,
    val requestTimeoutSeconds: Long = DEFAULT_REQUEST_TIMEOUT_SECONDS,
    val source: AppConfigSource = AppConfigSource.FALLBACK,
) {
    companion object {
        const val DEFAULT_DJEN_BASE_URL = "https://comunicaapi.pje.jus.br/api/v1"
        const val DEFAULT_DATAJUD_BASE_URL = "https://api-publica.datajud.cnj.jus.br/"
        const val DEFAULT_DATAJUD_API_KEY = "cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw=="
        const val DEFAULT_DJEN_ITEMS_PER_PAGE = 100
        const val DEFAULT_SYNC_LOOKBACK_DAYS = 15L
        const val DEFAULT_REQUEST_TIMEOUT_SECONDS = 30L
    }
}

enum class AppConfigSource {
    REMOTE,
    CACHED,
    FALLBACK,
}
