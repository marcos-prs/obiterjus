package com.obiterjus.core.config

import kotlinx.coroutines.flow.Flow

interface AppConfigRepository {
    val config: Flow<AppConfig>

    suspend fun current(): AppConfig

    suspend fun refresh(): AppConfig
}
