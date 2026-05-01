package com.obiterjus.data.datajud.remote

import com.obiterjus.data.datajud.remote.dto.DataJudSearchRequestDto
import com.obiterjus.data.datajud.remote.dto.DataJudSearchResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface DataJudApi {
    @POST
    suspend fun buscarProcesso(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Body request: DataJudSearchRequestDto,
    ): DataJudSearchResponseDto
}
