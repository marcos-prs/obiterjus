package com.obiterjus.data.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * O pouco que o app precisa do FCM, atrás de uma interface para o
 * [PushRegistroRepository] ser testável sem Firebase.
 *
 * Não há `token` aqui de propósito: `getToken()` está depreciado no
 * `firebase-messaging` 26. O modelo atual é o app se registrar e se inscrever
 * em tópicos, sem nunca manusear credencial de entrega — o que também tira do
 * nosso servidor a necessidade de guardá-la.
 */
interface FcmInscricoes {
    suspend fun registrar()

    suspend fun inscrever(topico: String)

    suspend fun desinscrever(topico: String)
}

class FirebaseFcmInscricoes(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) : FcmInscricoes {

    override suspend fun registrar() {
        messaging.register().await()
    }

    override suspend fun inscrever(topico: String) {
        messaging.subscribeToTopic(topico).await()
    }

    override suspend fun desinscrever(topico: String) {
        messaging.unsubscribeFromTopic(topico).await()
    }
}
