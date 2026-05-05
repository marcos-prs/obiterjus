package com.obiterjus.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.obiterSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "obiter_settings",
)
