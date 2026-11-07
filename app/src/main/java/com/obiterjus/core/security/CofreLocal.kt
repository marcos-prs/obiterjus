package com.obiterjus.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda segredos pequenos cifrados com uma chave AES-GCM que vive no Android
 * Keystore e nunca sai dele.
 *
 * Existe por dois motivos: a senha do banco Room cifrado e o access token do
 * Google Calendar — ambos precisavam ser lidos de forma síncrona (o primeiro na
 * construção do banco), o que descarta o DataStore.
 *
 * A chave não exige autenticação do usuário, então sobrevive a troca de
 * bloqueio de tela. Se ainda assim for invalidada, [ler] devolve null e
 * [segredo] gera um valor novo — para o banco isso significa base ilegível,
 * tratada em ObiterDatabaseFactory.
 */
class CofreLocal(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(ARQUIVO_PREFS, Context.MODE_PRIVATE)

    /**
     * Segredo binário estável de [tamanhoBytes], criado na primeira chamada e
     * reutilizado nas seguintes.
     */
    fun segredo(nome: String, tamanhoBytes: Int): ByteArray {
        ler(nome)?.let { return Base64.decode(it, Base64.NO_WRAP) }

        val novo = ByteArray(tamanhoBytes).also(SecureRandom()::nextBytes)
        guardar(nome, Base64.encodeToString(novo, Base64.NO_WRAP))
        return novo
    }

    fun ler(nome: String): String? {
        val armazenado = prefs.getString(nome, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(armazenado, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, TAMANHO_IV)
            val cifrado = bytes.copyOfRange(TAMANHO_IV, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMACAO).apply {
                init(Cipher.DECRYPT_MODE, chave(), GCMParameterSpec(TAMANHO_TAG_BITS, iv))
            }
            String(cipher.doFinal(cifrado), Charsets.UTF_8)
        }.getOrNull()
    }

    fun guardar(nome: String, valor: String?) {
        if (valor == null) {
            prefs.edit().remove(nome).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMACAO).apply {
            init(Cipher.ENCRYPT_MODE, chave())
        }
        val cifrado = cipher.doFinal(valor.toByteArray(Charsets.UTF_8))
        val empacotado = cipher.iv + cifrado
        prefs.edit()
            .putString(nome, Base64.encodeToString(empacotado, Base64.NO_WRAP))
            .apply()
    }

    private fun chave(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVEDOR).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR).apply {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(TAMANHO_CHAVE_BITS)
                    .build(),
            )
        }.generateKey()
    }

    companion object {
        /** Senha do banco Room cifrado. */
        const val SEGREDO_BANCO = "senha_banco"

        /** Access token do Google Calendar. */
        const val SEGREDO_TOKEN_CALENDAR = "google_calendar_access_token"

        private const val ARQUIVO_PREFS = "obiter_cofre"
        private const val PROVEDOR = "AndroidKeyStore"
        private const val ALIAS = "obiter_cofre_v1"
        private const val TRANSFORMACAO = "AES/GCM/NoPadding"
        private const val TAMANHO_CHAVE_BITS = 256
        private const val TAMANHO_TAG_BITS = 128
        private const val TAMANHO_IV = 12
    }
}
