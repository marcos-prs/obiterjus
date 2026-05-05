package com.obiterjus.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.obiterjus.ui.theme.TipoTema
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class PerfilPreferences(
    val intervaloBuscaDias: Int = 7,
    val sincronizacaoAutomatica: Boolean = true,
    val notificarPublicacoes: Boolean = true,
    val notificarPrazosUrgentes: Boolean = true,
    val notificarMovimentacoes: Boolean = true,
    val tema: TipoTema = TipoTema.SISTEMA,
)

interface PerfilPreferencesRepository {
    val preferencias: Flow<PerfilPreferences>

    suspend fun saveIntervaloBuscaDias(dias: Int)

    suspend fun saveSincronizacaoAutomatica(ativo: Boolean)

    suspend fun saveNotificarPublicacoes(ativo: Boolean)

    suspend fun saveNotificarPrazosUrgentes(ativo: Boolean)

    suspend fun saveNotificarMovimentacoes(ativo: Boolean)
    suspend fun saveTema(tema: TipoTema)
}

class DataStorePerfilPreferencesRepository(
    context: Context,
) : PerfilPreferencesRepository {
    private val dataStore = context.applicationContext.obiterSettingsDataStore

    override val preferencias: Flow<PerfilPreferences> = dataStore.safeData().map { prefs ->
        PerfilPreferences(
            intervaloBuscaDias = prefs[KEY_INTERVALO_BUSCA_DIAS] ?: 7,
            sincronizacaoAutomatica = prefs[KEY_SINCRONIZACAO_AUTOMATICA] ?: true,
            notificarPublicacoes = prefs[KEY_NOTIFICAR_PUBLICACOES] ?: true,
            notificarPrazosUrgentes = prefs[KEY_NOTIFICAR_PRAZOS_URGENTES] ?: true,
            notificarMovimentacoes = prefs[KEY_NOTIFICAR_MOVIMENTACOES] ?: true,
            tema = try {
                TipoTema.valueOf(prefs[KEY_TEMA] ?: TipoTema.SISTEMA.name)
            } catch (e: Exception) {
                TipoTema.SISTEMA
            },
        )
    }

    override suspend fun saveIntervaloBuscaDias(dias: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_INTERVALO_BUSCA_DIAS] = dias.coerceAtLeast(1)
        }
    }

    override suspend fun saveSincronizacaoAutomatica(ativo: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SINCRONIZACAO_AUTOMATICA] = ativo
        }
    }

    override suspend fun saveNotificarPublicacoes(ativo: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICAR_PUBLICACOES] = ativo
        }
    }

    override suspend fun saveNotificarPrazosUrgentes(ativo: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICAR_PRAZOS_URGENTES] = ativo
        }
    }

    override suspend fun saveNotificarMovimentacoes(ativo: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICAR_MOVIMENTACOES] = ativo
        }
    }

    override suspend fun saveTema(tema: TipoTema) {
        dataStore.edit { prefs ->
            prefs[KEY_TEMA] = tema.name
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
        val KEY_INTERVALO_BUSCA_DIAS = intPreferencesKey("intervalo_busca_dias")
        val KEY_SINCRONIZACAO_AUTOMATICA = booleanPreferencesKey("sincronizacao_automatica")
        val KEY_NOTIFICAR_PUBLICACOES = booleanPreferencesKey("notificar_publicacoes")
        val KEY_NOTIFICAR_PRAZOS_URGENTES = booleanPreferencesKey("notificar_prazos_urgentes")
        val KEY_NOTIFICAR_MOVIMENTACOES = booleanPreferencesKey("notificar_movimentacoes")
        val KEY_TEMA = stringPreferencesKey("tema")
    }
}
