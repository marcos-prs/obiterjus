package com.obiterjus.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

/** Desfecho de um pedido de credencial Google ao sistema. */
sealed interface ResultadoCredencialGoogle {
    /**
     * [nomeExibicao] e [email] vêm do próprio ID token e servem para adiantar o
     * cadastro: quem entra com Google não precisa redigitar o que a credencial
     * já informa.
     */
    data class Sucesso(
        val idToken: String,
        val nomeExibicao: String? = null,
        val email: String? = null,
    ) : ResultadoCredencialGoogle

    /** O advogado fechou o seletor de contas. Não é erro e não rende mensagem. */
    data object Cancelado : ResultadoCredencialGoogle

    /**
     * Nenhuma conta Google utilizável no aparelho — sem conta cadastrada no
     * Android, ou sem Google Play Services (emulador puro AOSP, aparelho sem GMS).
     */
    data object SemConta : ResultadoCredencialGoogle

    data class Falha(val causa: Throwable) : ResultadoCredencialGoogle
}

/**
 * Obtém um ID token do Google para trocar por sessão no Firebase.
 *
 * O contexto tem de ser o da Activity: o Credential Manager abre a folha de
 * seleção de contas por cima dela e recusa um contexto de aplicação. É anulável
 * porque quem o fornece na tela é `LocalActivity.current`, que pode não existir.
 */
interface CredencialGoogleProvider {
    suspend fun obterIdToken(contextoDaTela: Context?): ResultadoCredencialGoogle

    /**
     * Faz o sistema esquecer a credencial escolhida por último.
     *
     * Encerrar a sessão do Firebase não desautoriza a conta Google no aparelho:
     * ela continua vinculada ao app e o seletor volta oferecendo-a de pronto —
     * não é o que quem tocou em "Sair da conta" pediu.
     */
    suspend fun limparEstadoDeCredencial()
}

/**
 * Implementação sobre o Credential Manager (`GetSignInWithGoogleOption`).
 *
 * Usa o fluxo "Sign in with Google" em vez de `GetGoogleIdOption` porque aqui o
 * botão é explícito: queremos sempre o seletor com todas as contas do aparelho,
 * e não o atalho silencioso que falha quando nenhuma conta foi autorizada antes.
 *
 * [serverClientId] é o OAuth client **web** do projeto Firebase — o plugin
 * google-services o gera em `R.string.default_web_client_id`. Com o client
 * Android no lugar dele o Firebase recusa o token.
 */
class CredentialManagerGoogleProvider(
    private val serverClientId: String,
    /** Contexto de aplicação: a limpeza de credencial não abre interface alguma. */
    private val appContext: Context,
    private val criarGerenciador: (Context) -> CredentialManager = CredentialManager::create,
) : CredencialGoogleProvider {

    override suspend fun obterIdToken(contextoDaTela: Context?): ResultadoCredencialGoogle {
        if (contextoDaTela == null) {
            return ResultadoCredencialGoogle.Falha(
                IllegalStateException("Sem Activity para abrir o seletor de contas do Google"),
            )
        }
        if (serverClientId.isBlank()) {
            return ResultadoCredencialGoogle.Falha(
                IllegalStateException("default_web_client_id ausente: google-services.json sem OAuth client web"),
            )
        }

        val pedido = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        return try {
            val resposta = criarGerenciador(contextoDaTela).getCredential(contextoDaTela, pedido)
            val credencial = resposta.credential
            val google = if (credencial is CustomCredential && credencial.type in TIPOS_GOOGLE) {
                GoogleIdTokenCredential.createFrom(credencial.data)
            } else {
                null
            }
            if (google == null || google.idToken.isBlank()) {
                Log.w(TAG, "Credencial sem ID token (tipo=${credencial.type})")
                ResultadoCredencialGoogle.Falha(
                    IllegalStateException("Credencial sem ID token: ${credencial.type}"),
                )
            } else {
                ResultadoCredencialGoogle.Sucesso(
                    idToken = google.idToken,
                    nomeExibicao = google.displayName?.takeIf { it.isNotBlank() },
                    // Em credencial Google o `id` é o endereço de e-mail da conta.
                    email = google.id.takeIf { it.isNotBlank() },
                )
            }
        } catch (erro: GetCredentialCancellationException) {
            ResultadoCredencialGoogle.Cancelado
        } catch (erro: NoCredentialException) {
            Log.w(TAG, "Nenhuma credencial Google disponível no aparelho", erro)
            ResultadoCredencialGoogle.SemConta
        } catch (erro: GetCredentialException) {
            Log.w(TAG, "Falha do Credential Manager ao pedir a credencial Google", erro)
            ResultadoCredencialGoogle.Falha(erro)
        } catch (erro: CancellationException) {
            throw erro
        } catch (erro: Exception) {
            Log.w(TAG, "Erro inesperado ao pedir a credencial Google", erro)
            ResultadoCredencialGoogle.Falha(erro)
        }
    }

    override suspend fun limparEstadoDeCredencial() {
        try {
            criarGerenciador(appContext).clearCredentialState(
                ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_CREDENTIAL_STATE),
            )
        } catch (erro: CancellationException) {
            throw erro
        } catch (erro: Exception) {
            // Sair da conta não pode falhar por causa disto: quem manda no acesso
            // é a sessão do Firebase, e ela já foi encerrada.
            Log.w(TAG, "Falha ao limpar o estado de credencial do Credential Manager", erro)
        }
    }

    private companion object {
        const val TAG = "ObiterAuth"

        /**
         * O `GetSignInWithGoogleOption` devolve o tipo SIWG nas versões novas da
         * googleid e o tipo clássico nas antigas; aceitar os dois evita um
         * "credencial sem ID token" só por causa de um bump de dependência.
         */
        val TIPOS_GOOGLE = setOf(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
        )
    }
}
