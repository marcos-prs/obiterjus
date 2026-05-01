package com.obiterjus.data.djen.remote

import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DjenApi {
    @GET("api/v1/comunicacao")
    suspend fun buscarComunicacoes(
        @Query("numeroOab") numeroOab: String,
        @Query("ufOab") ufOab: String,
        @Query("dataDisponibilizacaoInicio") dataDisponibilizacaoInicio: String,
        @Query("dataDisponibilizacaoFim") dataDisponibilizacaoFim: String,
        @Query("pagina") pagina: Int,
        @Query("itensPorPagina") itensPorPagina: Int,
    ): DjenResponseDto

    @GET("api/v1/comunicacao/{hash}/certidao")
    suspend fun baixarCertidao(
        @Path("hash") hash: String,
    ): ResponseBody
}
