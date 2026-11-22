package com.obiterjus.data.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * O pouco que o app precisa do FCM, atrás de uma interface para o
 * [PushRegistroRepository] ser testável sem Firebase.
 *
 * Não há `token` aqui de propósito: `getToken()` está depreciado no
 * `firebase-messaging` 26. O app se inscreve em tópicos sem nunca manusear
 * credencial de entrega — o que também tira do nosso servidor a necessidade de
 * guardá-la.
 *
 * Também não há `register()`, e a ausência importa. No 26.0.1 o
 * `FirebaseMessaging.register()` público só funciona com o meta-data
 * `firebase_messaging_installation_id_enabled=true` no manifest; sem ele
 * devolve `IllegalStateException("API disabled...")` na hora. Foi o que
 * deixou a VC4 sem push: o registro falhava antes da inscrição, o aparelho
 * nunca entrava no tópico e o servidor empurrava para ninguém. A chamada é
 * dispensável: `subscribeToTopic` registra o aparelho por conta própria
 * (`TopicSubscriptionClient.subscribe` → `blockingRegister`), pelo caminho
 * interno que não passa por essa trava — conferido com `javap` no artefato.
 */
interface FcmInscricoes {
    suspend fun inscrever(topico: String)

    suspend fun desinscrever(topico: String)
}

class FirebaseFcmInscricoes(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) : FcmInscricoes {

    override suspend fun inscrever(topico: String) {
        messaging.subscribeToTopic(topico).await()
    }

    override suspend fun desinscrever(topico: String) {
        messaging.unsubscribeFromTopic(topico).await()
    }
}
