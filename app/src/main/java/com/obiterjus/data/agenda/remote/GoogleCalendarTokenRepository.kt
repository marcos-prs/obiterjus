package com.obiterjus.data.agenda.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.obiterjus.data.settings.obiterSettingsDataStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface GoogleCalendarTokenRepository {
    val accessToken: StateFlow<String?>

    suspend fun saveAccessToken(token: String)

    suspend fun clearAccessToken()
}

class DataStoreGoogleCalendarTokenRepository(
    context: Context,
) : GoogleCalendarTokenRepository {
    private val dataStore = context.applicationContext.obiterSettingsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val currentAccessToken = MutableStateFlow<String?>(null)

    override val accessToken: StateFlow<String?> = currentAccessToken

    init {
        scope.launch {
            dataStore.safeData()
                .map { prefs -> prefs[KEY_ACCESS_TOKEN] }
                .collect { token -> currentAccessToken.value = token }
        }
    }

    override suspend fun saveAccessToken(token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = token.trim()
        }
    }

    override suspend fun clearAccessToken() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
        }
    }

    private fun DataStore<Preferences>.safeData() =
        data.catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }

    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("google_calendar_access_token")
    }
}
