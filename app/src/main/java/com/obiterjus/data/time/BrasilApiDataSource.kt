package com.obiterjus.data.time

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Response

@Serializable
data class FeriadoDto(
    val date: String,
    val name: String,
    val type: String
)

interface BrasilApiDataSource {
    @GET("api/feriados/v1/{ano}")
    suspend fun getFeriadosNacionais(@Path("ano") ano: Int): Response<List<FeriadoDto>>
}
