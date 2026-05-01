package com.obiterjus.data.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.obiterjus.core.config.AppConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.core.config.AppConfigSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseRemoteAppConfigRepository(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val fallbackDataJudApiKey: String = "",
) : AppConfigRepository {
    private val fallbackConfig = AppConfig(dataJudApiKey = fallbackDataJudApiKey)
    private val _config = MutableStateFlow(fallbackConfig)
    override val config: Flow<AppConfig> = _config

    init {
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(MIN_FETCH_INTERVAL_SECONDS)
                .build(),
        )
        remoteConfig.setDefaultsAsync(fallbackConfig.toRemoteDefaults())
    }

    override suspend fun current(): AppConfig {
        val currentConfig = remoteConfig.toAppConfig(
            source = AppConfigSource.CACHED,
            fallback = fallbackConfig,
        )
        _config.value = currentConfig
        return currentConfig
    }

    override suspend fun refresh(): AppConfig {
        val source = runCatching {
            remoteConfig.fetchAndActivate().await()
            AppConfigSource.REMOTE
        }.getOrElse {
            AppConfigSource.CACHED
        }
        val currentConfig = remoteConfig.toAppConfig(
            source = source,
            fallback = fallbackConfig,
        )
        _config.value = currentConfig
        return currentConfig
    }

    private fun AppConfig.toRemoteDefaults(): Map<String, Any> =
        mapOf(
            KEY_DJEN_BASE_URL to djenBaseUrl,
            KEY_DATAJUD_BASE_URL to dataJudBaseUrl,
            KEY_DATAJUD_API_KEY to dataJudApiKey,
            KEY_DJEN_DEFAULT_ITEMS_PER_PAGE to djenDefaultItemsPerPage,
            KEY_SYNC_LOOKBACK_DAYS to syncLookbackDays,
            KEY_DATAJUD_ENABLED to dataJudEnabled,
            KEY_DJEN_ENABLED to djenEnabled,
            KEY_REQUEST_TIMEOUT_SECONDS to requestTimeoutSeconds,
        )

    private fun FirebaseRemoteConfig.toAppConfig(
        source: AppConfigSource,
        fallback: AppConfig,
    ): AppConfig {
        val remoteApiKey = getString(KEY_DATAJUD_API_KEY).trim()
        return AppConfig(
            djenBaseUrl = getString(KEY_DJEN_BASE_URL).trim().ifBlank { fallback.djenBaseUrl },
            dataJudBaseUrl = getString(KEY_DATAJUD_BASE_URL).trim().ifBlank { fallback.dataJudBaseUrl },
            dataJudApiKey = remoteApiKey.ifBlank { fallback.dataJudApiKey },
            djenDefaultItemsPerPage = getPositiveLongOrFallback(
                KEY_DJEN_DEFAULT_ITEMS_PER_PAGE,
                fallback.djenDefaultItemsPerPage.toLong(),
            ).toInt(),
            syncLookbackDays = getPositiveLongOrFallback(
                KEY_SYNC_LOOKBACK_DAYS,
                fallback.syncLookbackDays,
            ),
            dataJudEnabled = getBooleanOrFallback(KEY_DATAJUD_ENABLED, fallback.dataJudEnabled),
            djenEnabled = getBooleanOrFallback(KEY_DJEN_ENABLED, fallback.djenEnabled),
            requestTimeoutSeconds = getPositiveLongOrFallback(
                KEY_REQUEST_TIMEOUT_SECONDS,
                fallback.requestTimeoutSeconds,
            ),
            source = source,
        )
    }

    private fun FirebaseRemoteConfig.getPositiveLongOrFallback(
        key: String,
        fallback: Long,
    ): Long {
        if (getValue(key).source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return fallback
        return getLong(key).takeIf { it > 0 } ?: fallback
    }

    private fun FirebaseRemoteConfig.getBooleanOrFallback(
        key: String,
        fallback: Boolean,
    ): Boolean {
        if (getValue(key).source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return fallback
        return getBoolean(key)
    }

    private companion object {
        const val KEY_DJEN_BASE_URL = "djen_base_url"
        const val KEY_DATAJUD_BASE_URL = "datajud_base_url"
        const val KEY_DATAJUD_API_KEY = "datajud_api_key"
        const val KEY_DJEN_DEFAULT_ITEMS_PER_PAGE = "djen_default_items_per_page"
        const val KEY_SYNC_LOOKBACK_DAYS = "sync_lookback_days"
        const val KEY_DATAJUD_ENABLED = "datajud_enabled"
        const val KEY_DJEN_ENABLED = "djen_enabled"
        const val KEY_REQUEST_TIMEOUT_SECONDS = "request_timeout_seconds"
        const val MIN_FETCH_INTERVAL_SECONDS = 60L * 60L
    }
}
