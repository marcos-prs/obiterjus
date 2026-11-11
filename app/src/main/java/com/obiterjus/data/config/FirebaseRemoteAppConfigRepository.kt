package com.obiterjus.data.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.obiterjus.core.config.AppConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.core.config.AppConfigSource
import java.net.URI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseRemoteAppConfigRepository(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val fallbackDataJudApiKey: String = AppConfig.DEFAULT_DATAJUD_API_KEY,
) : AppConfigRepository {
    private val fallbackConfig = AppConfig(dataJudApiKey = fallbackDataJudApiKey.ifBlank { AppConfig.DEFAULT_DATAJUD_API_KEY })
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
            KEY_DJEN_TOKEN to djenToken,
            KEY_DATAJUD_BASE_URL to dataJudBaseUrl,
            KEY_DATAJUD_API_KEY to dataJudApiKey,
            KEY_DJEN_DEFAULT_ITEMS_PER_PAGE to djenDefaultItemsPerPage,
            KEY_SYNC_LOOKBACK_DAYS to syncLookbackDays,
            KEY_DATAJUD_ENABLED to dataJudEnabled,
            KEY_DJEN_ENABLED to djenEnabled,
            KEY_PUSH_DIARIO_ENABLED to pushDiarioEnabled,
            KEY_PUSH_PRE_CHECAGEM_DISPONIVEL to pushPreChecagemDisponivel,
            KEY_REQUEST_TIMEOUT_SECONDS to requestTimeoutSeconds,
        )

    private fun FirebaseRemoteConfig.toAppConfig(
        source: AppConfigSource,
        fallback: AppConfig,
    ): AppConfig {
        val remoteApiKey = getString(KEY_DATAJUD_API_KEY).trim()
        return AppConfig(
            djenBaseUrl = getString(KEY_DJEN_BASE_URL).urlPermitidaOuNull() ?: fallback.djenBaseUrl,
            djenToken = getString(KEY_DJEN_TOKEN).trim().ifBlank { fallback.djenToken },
            dataJudBaseUrl = getString(KEY_DATAJUD_BASE_URL).urlPermitidaOuNull() ?: fallback.dataJudBaseUrl,
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
            pushDiarioEnabled = getBooleanOrFallback(
                KEY_PUSH_DIARIO_ENABLED,
                fallback.pushDiarioEnabled,
            ),
            pushPreChecagemDisponivel = getBooleanOrFallback(
                KEY_PUSH_PRE_CHECAGEM_DISPONIVEL,
                fallback.pushPreChecagemDisponivel,
            ),
            requestTimeoutSeconds = getPositiveLongOrFallback(
                KEY_REQUEST_TIMEOUT_SECONDS,
                fallback.requestTimeoutSeconds,
            ),
            source = source,
        )
    }

    /**
     * A APIKey do DataJud e o token do DJEN vão no cabeçalho para o host que
     * estiver configurado aqui. Sem esta checagem, um valor errado no console
     * — ou acesso indevido ao projeto Firebase — entregaria as credenciais a
     * um terceiro. Exige https e domínio do Judiciário.
     */
    private fun String.urlPermitidaOuNull(): String? {
        val valor = trim().takeIf { it.isNotBlank() } ?: return null
        if (!valor.startsWith("https://", ignoreCase = true)) return null
        val host = runCatching { URI(valor).host }.getOrNull()?.lowercase() ?: return null
        val permitido = host == DOMINIO_PERMITIDO || host.endsWith(".$DOMINIO_PERMITIDO")
        return valor.takeIf { permitido }
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
        const val KEY_DJEN_TOKEN = "djen_token"
        const val KEY_DATAJUD_BASE_URL = "datajud_base_url"
        const val KEY_DATAJUD_API_KEY = "datajud_api_key"
        const val KEY_DJEN_DEFAULT_ITEMS_PER_PAGE = "djen_default_items_per_page"
        const val KEY_SYNC_LOOKBACK_DAYS = "sync_lookback_days"
        const val KEY_DATAJUD_ENABLED = "datajud_enabled"
        const val KEY_DJEN_ENABLED = "djen_enabled"
        const val KEY_PUSH_DIARIO_ENABLED = "push_diario_enabled"
        const val KEY_PUSH_PRE_CHECAGEM_DISPONIVEL = "push_pre_checagem_disponivel"
        const val KEY_REQUEST_TIMEOUT_SECONDS = "request_timeout_seconds"
        const val MIN_FETCH_INTERVAL_SECONDS = 60L * 60L
        const val DOMINIO_PERMITIDO = "jus.br"
    }
}
