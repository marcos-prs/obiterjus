package com.obiterjus.data.minuta.remote

import com.obiterjus.data.minuta.remote.dto.IngestRespostaDto
import com.obiterjus.data.minuta.remote.dto.MinutaPackageDto
import com.obiterjus.data.minuta.remote.dto.MinutaSugestaoDto
import com.obiterjus.data.minuta.remote.dto.StatusJobDto
import com.obiterjus.data.minuta.remote.dto.SugestaoMinutaRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Cliente da API Obiter-Minuta.
 *
 * Fluxo em duas etapas:
 * 1. [ingerir] envia o PDF da peça recebida → polling em [consultarStatus] →
 *    [obterResultado] devolve a estrutura extraída. Atenção: o resultado expira
 *    na primeira leitura (ou TTL de 2h) — persista localmente ao receber.
 * 2. [sugerirMinuta] recebe a estrutura extraída e devolve o rascunho da peça
 *    de resposta (síncrono, 10–45s).
 */
interface ObiterMinutaDataSource {

    @Multipart
    @POST("ingest")
    suspend fun ingerir(
        @Part file: MultipartBody.Part,
        @Part("tipo_declarado") tipoDeclarado: RequestBody,
        @Part("numero_processo") numeroProcesso: RequestBody? = null,
        @Part("vara") vara: RequestBody? = null,
        @Part("origem") origem: RequestBody? = null,
    ): IngestRespostaDto

    @GET("status/{jobId}")
    suspend fun consultarStatus(@Path("jobId") jobId: String): StatusJobDto

    @GET("result/{jobId}")
    suspend fun obterResultado(@Path("jobId") jobId: String): MinutaPackageDto

    @POST("minuta/sugerir")
    suspend fun sugerirMinuta(@Body pedido: SugestaoMinutaRequestDto): MinutaSugestaoDto
}
