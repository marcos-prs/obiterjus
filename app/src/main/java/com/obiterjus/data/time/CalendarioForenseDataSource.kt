package com.obiterjus.data.time

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class PedidoCalculoPrazo(
    val tribunal: String,
    @SerialName("data_disponibilizacao") val dataDisponibilizacao: String,
    val origem: String = "explicito",
    val prazo: Int,
    val unidade: String = "dias_uteis",
    val classe: String = "parte_intimacao",
    @SerialName("termo_inicial") val termoInicial: String = "intimacao_publicacao",
    val multiplicador: Int = 1,
)

@Serializable
data class RespostaPrazo(
    val estado: String,
    @SerialName("data_vencimento") val dataVencimento: String? = null,
    val avisos: List<String> = emptyList(),
)

interface CalendarioForenseDataSource {
    @POST("calcular-prazo")
    suspend fun calcularPrazo(@Body pedido: PedidoCalculoPrazo): RespostaPrazo
}
