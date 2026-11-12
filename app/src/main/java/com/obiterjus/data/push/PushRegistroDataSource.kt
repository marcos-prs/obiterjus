package com.obiterjus.data.push

import com.obiterjus.data.push.dto.PushConsultaRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Registro da pré-checagem no servidor próprio.
 *
 * Só a Fase 2 passa por aqui. A Fase 1 — acordar cego no horário — é inscrição
 * em tópico do FCM e não toca servidor nosso: o emissor (`Obiter-Push`) só faz
 * chamada de saída para o FCM e não tem endpoint nenhum ainda.
 *
 * O host está em [PushRetrofitFactory], com a ressalva de que ele aponta hoje
 * para a API de minutas e precisa ser decidido antes de a Fase 2 sair do papel.
 *
 * O cabeçalho `X-Obiter-App-Key` identifica o app, não o usuário: o registro
 * não pode exigir conta, porque o app não tem gate de login de propósito. A
 * chave vai dentro do APK e é extraível — serve para cortar abuso casual, não
 * como autenticação de verdade.
 */
interface PushRegistroDataSource {

    @PUT("push/consulta")
    suspend fun registrarConsulta(
        @Header("X-Obiter-App-Key") chaveApp: String,
        @Body pedido: PushConsultaRequestDto,
    )

    @DELETE("push/consulta/{instalacao}")
    suspend fun cancelarConsulta(
        @Header("X-Obiter-App-Key") chaveApp: String,
        @Path("instalacao") instalacao: String,
    )
}
