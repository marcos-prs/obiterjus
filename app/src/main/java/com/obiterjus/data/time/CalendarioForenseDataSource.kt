package com.obiterjus.data.time

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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

/**
 * Recorte de GET /saude — só a cobertura judicial interessa ao app. O resto do
 * corpo (últimas coletas, contagens de diagnóstico) cai no ignoreUnknownKeys
 * do [CalendarioForenseRetrofitFactory].
 */
@Serializable
data class RespostaSaude(
    @SerialName("cobertura_judicial") val coberturaJudicial: CoberturaJudicial? = null,
)

/**
 * A API tem duas camadas de calendário: a legal (feriados nacionais,
 * estaduais, municipais) e a judicial (recesso, emendas, Semana Santa
 * estendida, Dia da Justiça — por portaria de cada tribunal). A judicial vem
 * de coleta autônoma e não cobre todos os órgãos.
 *
 * [zerados] e as entradas com valor 0 em [detalhe] são os tribunais cujo
 * cálculo pula apenas fim de semana, feriado legal e a suspensão do art. 220:
 * o prazo sai sem as portarias do tribunal.
 */
@Serializable
data class CoberturaJudicial(
    val zerados: List<String> = emptyList(),
    val detalhe: Map<String, Int> = emptyMap(),
)

interface CalendarioForenseDataSource {
    @POST("calcular-prazo")
    suspend fun calcularPrazo(@Body pedido: PedidoCalculoPrazo): RespostaPrazo

    @GET("saude")
    suspend fun consultarSaude(@Query("ano") ano: Int): RespostaSaude
}
