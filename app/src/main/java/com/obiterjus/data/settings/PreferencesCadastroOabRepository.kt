package com.obiterjus.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obiterjus.core.parser.CnjDateParser
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.repository.RepositorioCadastroOab
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.obiterSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "obiter_settings",
)

class PreferencesCadastroOabRepository(
    context: Context,
) : RepositorioCadastroOab {
    private val dataStore = context.applicationContext.obiterSettingsDataStore

    override val cadastro: Flow<OabCadastro> =
        dataStore.safeData().map { prefs ->
            OabCadastro(
                numero = prefs[KEY_OAB_NUMERO].orEmpty(),
                uf = prefs[KEY_OAB_UF].orEmpty(),
                nomeAdvogado = prefs[KEY_OAB_NOME].orEmpty(),
                dataInicio = prefs[KEY_OAB_DATA_INICIO]?.let(CnjDateParser::parseLocalDate),
                dataFim = prefs[KEY_OAB_DATA_FIM]?.let(CnjDateParser::parseLocalDate),
            )
        }

    override val status: Flow<SincronizacaoStatus> =
        dataStore.safeData().map { prefs ->
            SincronizacaoStatus(
                ultimaExecucaoEm = prefs[KEY_ULTIMA_EXECUCAO_EM]?.let(Instant::ofEpochMilli),
                ultimoSucessoEm = prefs[KEY_ULTIMO_SUCESSO_EM]?.let(Instant::ofEpochMilli),
                ultimaFalha = prefs[KEY_ULTIMA_FALHA],
                novasPublicacoesUltimaExecucao = prefs[KEY_NOVAS_ULTIMA_EXECUCAO] ?: 0,
            )
        }

    override suspend fun salvarCadastro(
        numero: String,
        uf: String,
        nomeAdvogado: String,
        dataInicio: LocalDate?,
        dataFim: LocalDate?,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_OAB_NUMERO] = numero.trim()
            prefs[KEY_OAB_UF] = uf.trim().uppercase().take(2)
            prefs[KEY_OAB_NOME] = nomeAdvogado.trim()
            dataInicio?.let { prefs[KEY_OAB_DATA_INICIO] = it.toString() }
                ?: prefs.remove(KEY_OAB_DATA_INICIO)
            dataFim?.let { prefs[KEY_OAB_DATA_FIM] = it.toString() }
                ?: prefs.remove(KEY_OAB_DATA_FIM)
        }
    }

    override suspend fun registrarSucesso(
        executadoEm: Instant,
        novasPublicacoes: Int,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_ULTIMA_EXECUCAO_EM] = executadoEm.toEpochMilli()
            prefs[KEY_ULTIMO_SUCESSO_EM] = executadoEm.toEpochMilli()
            prefs[KEY_NOVAS_ULTIMA_EXECUCAO] = novasPublicacoes
            prefs.remove(KEY_ULTIMA_FALHA)
        }
    }

    override suspend fun registrarFalha(
        executadoEm: Instant,
        mensagem: String,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_ULTIMA_EXECUCAO_EM] = executadoEm.toEpochMilli()
            prefs[KEY_ULTIMA_FALHA] = mensagem
            prefs[KEY_NOVAS_ULTIMA_EXECUCAO] = 0
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
        val KEY_OAB_NUMERO = stringPreferencesKey("oab_numero")
        val KEY_OAB_UF = stringPreferencesKey("oab_uf")
        val KEY_OAB_NOME = stringPreferencesKey("oab_nome")
        val KEY_OAB_DATA_INICIO = stringPreferencesKey("oab_data_inicio")
        val KEY_OAB_DATA_FIM = stringPreferencesKey("oab_data_fim")
        val KEY_ULTIMA_EXECUCAO_EM = longPreferencesKey("ultima_execucao_em")
        val KEY_ULTIMO_SUCESSO_EM = longPreferencesKey("ultimo_sucesso_em")
        val KEY_ULTIMA_FALHA = stringPreferencesKey("ultima_falha")
        val KEY_NOVAS_ULTIMA_EXECUCAO = intPreferencesKey("novas_ultima_execucao")
    }
}
