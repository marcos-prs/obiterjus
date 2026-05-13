package com.obiterjus.data.djen.remote

import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DjenApi {
    @GET("comunicacao")
    suspend fun buscarComunicacoes(
        @Query("numeroOab") numeroOab: String? = null,
        @Query("ufOab") ufOab: String? = null,
        @Query("nomeAdvogado") nomeAdvogado: String? = null,
        @Query("dataDisponibilizacaoInicio") dataDisponibilizacaoInicio: String,
        @Query("dataDisponibilizacaoFim") dataDisponibilizacaoFim: String,
        @Query("pagina") pagina: Int,
        @Query("itensPorPagina") itensPorPagina: Int,
    ): DjenResponseDto

    @GET("comunicacao/{hash}/certidao")
    suspend fun baixarCertidao(
        @Path("hash") hash: String,
    ): ResponseBody
}
