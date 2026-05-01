package com.obiterjus.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface SyncPreferencesRepository {
    val syncFrequencyHours: Flow<Int>
    suspend fun saveSyncFrequencyHours(hours: Int)
}

private val Context.syncSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sync_settings",
)

class DataStoreSyncPreferencesRepository(
    context: Context,
) : SyncPreferencesRepository {
    private val dataStore = context.applicationContext.syncSettingsDataStore

    override val syncFrequencyHours: Flow<Int> = dataStore.safeData().map { prefs ->
        prefs[KEY_SYNC_FREQUENCY_HOURS] ?: 24
    }

    override suspend fun saveSyncFrequencyHours(hours: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_FREQUENCY_HOURS] = hours
        }
    }

    private fun DataStore<Preferences>.safeData(): Flow<Preferences> =
        data.catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }

    private companion object {
        val KEY_SYNC_FREQUENCY_HOURS = intPreferencesKey("sync_frequency_hours")
    }
}
