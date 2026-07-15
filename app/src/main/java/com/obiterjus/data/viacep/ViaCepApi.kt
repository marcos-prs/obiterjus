package com.obiterjus.data.viacep

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepApi {
    @GET("ws/{cep}/json/")
    suspend fun buscarCep(@Path("cep") cep: String): Response<ViaCepDto>
}
