package com.obiterjus.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.obiterjus.core.database.ObiterDatabase
import com.obiterjus.core.security.CofreLocal
import com.obiterjus.data.djen.CertidaoDjenRepositoryImpl
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Dono da base local.
 *
 * Um aparelho pode passar por mais de um advogado — troca de conta, aparelho
 * emprestado, revenda. A base do Room e o DataStore não são por usuário, então
 * sem esta guarda o acervo de quem entrou antes continua visível, e a primeira
 * sincronização o envia para o Firestore da conta nova.
 */
interface SessaoLocalRepository {
    suspend fun prepararPara(uid: String)

    suspend fun limparTudo()
}

class DataStoreSessaoLocalRepository(
    private val context: Context,
    private val database: ObiterDatabase,
) : SessaoLocalRepository {
    private val dataStore = context.applicationContext.obiterSettingsDataStore

    /**
     * Deixa o aparelho pronto para [uid]. Se a base pertencia a outra conta,
     * apaga tudo antes — nunca se restaura por cima de dado alheio.
     */
    override suspend fun prepararPara(uid: String) {
        val dono = dataStore.data.first()[KEY_UID_PROPRIETARIO]
        if (dono != null && dono != uid) {
            limparTudo()
        }
        dataStore.edit { prefs -> prefs[KEY_UID_PROPRIETARIO] = uid }
    }

    /**
     * Apaga o acervo, as preferências e as credenciais guardadas no aparelho.
     * A senha do banco fica de fora de propósito: ela não é dado do usuário e
     * descartá-la tornaria o arquivo cifrado ilegível sem necessidade.
     */
    override suspend fun limparTudo() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            CofreLocal(context).guardar(CofreLocal.SEGREDO_TOKEN_CALENDAR, null)
            CertidaoDjenRepositoryImpl.limparCache(context, Duration.ZERO)
        }
        dataStore.edit { prefs -> prefs.clear() }
    }

    private companion object {
        val KEY_UID_PROPRIETARIO = stringPreferencesKey("uid_proprietario_base")
    }
}
