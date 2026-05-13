package com.obiterjus.data.djen.remote

import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class DjenRemoteDataSourceTest {
    @Test
    fun fetchesPagesUntilCountIsConsumed() = runBlocking {
        val api = FakeDjenApi(
            pages = mapOf(
                1 to DjenResponseDto(count = 3, items = listOf(item(1), item(2))),
                2 to DjenResponseDto(count = 3, items = listOf(item(3))),
            ),
        )
        val dataSource = DjenRemoteDataSource(
            api = api,
            itensPorPagina = 2,
            maxPaginas = 10,
        )

        val result = dataSource.buscarComunicacoes(
            numeroOab = "123",
            ufOab = "mg",
            dataInicio = LocalDate.of(2026, 4, 1),
            dataFim = LocalDate.of(2026, 4, 29),
        )

        assertEquals(listOf(1L, 2L, 3L), result.items.map { it.id })
        assertEquals(2, result.paginasConsultadas)
        assertEquals(DjenPaginationStopReason.PARTIAL_PAGE, result.motivoParada)
        assertEquals(listOf(1, 2), api.requestedPages)
        assertEquals("MG", api.lastUf)
    }

    @Test
    fun stopsBeforeAddingRepeatedPage() = runBlocking {
        val api = FakeDjenApi(
            pages = mapOf(
                1 to DjenResponseDto(count = 4, items = listOf(item(1), item(2))),
                2 to DjenResponseDto(count = 4, items = listOf(item(1), item(2))),
            ),
        )
        val dataSource = DjenRemoteDataSource(
            api = api,
            itensPorPagina = 2,
            maxPaginas = 10,
        )

        val result = dataSource.buscarComunicacoes(
            numeroOab = "123",
            ufOab = "MG",
            dataInicio = LocalDate.of(2026, 4, 1),
            dataFim = LocalDate.of(2026, 4, 29),
        )

        assertEquals(listOf(1L, 2L), result.items.map { it.id })
        assertEquals(2, result.paginasConsultadas)
        assertEquals(DjenPaginationStopReason.REPEATED_PAGE, result.motivoParada)
    }

    private fun item(id: Long): DjenComunicacaoDto =
        DjenComunicacaoDto(id = id, texto = "Publicacao $id")

    private class FakeDjenApi(
        private val pages: Map<Int, DjenResponseDto>,
    ) : DjenApi {
        val requestedPages = mutableListOf<Int>()
        var lastUf: String? = null

        override suspend fun buscarComunicacoes(
            numeroOab: String?,
            ufOab: String?,
            nomeAdvogado: String?,
            dataDisponibilizacaoInicio: String,
            dataDisponibilizacaoFim: String,
            pagina: Int,
            itensPorPagina: Int,
        ): DjenResponseDto {
            requestedPages += pagina
            lastUf = ufOab
            return pages.getValue(pagina)
        }

        override suspend fun baixarCertidao(hash: String): ResponseBody =
            ByteArray(0).toResponseBody()
    }
}
