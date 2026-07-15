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
    // origem=explicito exige prazo; origem=ruleset exige artigo (a API
    // resolve o número de dias na tabela prazos_cpc versionada).
    val prazo: Int? = null,
    val unidade: String = "dias_uteis",
    val classe: String = "parte_intimacao",
    val artigo: String? = null,
    @SerialName("termo_inicial") val termoInicial: String = "intimacao_publicacao",
    val multiplicador: Int = 1,
    // Declarada pelo app — a API não infere. penal = dias corridos (CPP 798).
    val natureza: String = "civel",
)

@Serializable
data class RespostaPrazo(
    val estado: String,
    @SerialName("data_vencimento") val dataVencimento: String? = null,
    // Preenchidos pela API quando o prazo vem do ruleset (origem=ruleset).
    val prazo: Int? = null,
    val unidade: String? = null,
    val avisos: List<String> = emptyList(),
)

interface CalendarioForenseDataSource {
    @POST("calcular-prazo")
    suspend fun calcularPrazo(@Body pedido: PedidoCalculoPrazo): RespostaPrazo
}
