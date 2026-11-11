package com.obiterjus.core.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.obiterjus.core.worker.DjenSyncScheduler

/**
 * Recebe o "acorda" do servidor e dispara a sincronização.
 *
 * A mensagem é data-only de propósito: não traz conteúdo de publicação, nem
 * contagem. Quem decide o que notificar é o app, depois da própria consulta ao
 * DJEN — o inteiro teor continua chegando só ao aparelho.
 *
 * `onMessageReceived` tem orçamento curto, então aqui só se enfileira trabalho:
 * a busca roda no [com.obiterjus.core.worker.DjenSyncWorker], que já sabe
 * sincronizar, recalcular prazos, notificar e registrar em `sync_logs`.
 *
 * Não há `onNewToken`: está depreciado no `firebase-messaging` 26, e o aparelho
 * é endereçado por tópico — o SDK refaz as inscrições sozinho quando o token
 * rotaciona.
 */
class ObiterMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(mensagem: RemoteMessage) {
        if (mensagem.data[CAMPO_TIPO] != TIPO_SYNC_DIARIA) {
            Log.w(TAG, "Push ignorado: tipo inesperado")
            return
        }
        DjenSyncScheduler.agendarAgora(applicationContext)
    }

    private companion object {
        const val TAG = "ObiterMessaging"
        const val CAMPO_TIPO = "tipo"
        const val TIPO_SYNC_DIARIA = "sync_diaria"
    }
}
