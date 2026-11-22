package com.obiterjus.data.djen.remote

import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

private val SEM_ESPERA = listOf(0L, 0L)

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

    @Test
    fun retriesTransientServerErrorAndSucceeds() = runBlocking {
        // A API do CNJ devolve 500 de forma intermitente. Sem retentativa, um
        // desses derruba a sincronização do dia inteiro.
        val api = FlakyDjenApi(
            respostas = listOf(
                Resultado.Erro(httpException(500)),
                Resultado.Ok(DjenResponseDto(count = 1, items = listOf(item(7)))),
            ),
        )
        val dataSource = DjenRemoteDataSource(
            api = api,
            itensPorPagina = 2,
            maxPaginas = 10,
            esperasEntreTentativasMs = SEM_ESPERA,
        )

        val result = dataSource.buscarComunicacoes(
            numeroOab = "123",
            ufOab = "MG",
            dataInicio = LocalDate.of(2026, 11, 4),
            dataFim = LocalDate.of(2026, 11, 19),
        )

        assertEquals(listOf(7L), result.items.map { it.id })
        assertEquals(2, api.chamadas)
    }

    @Test
    fun givesUpAfterRetriesWithMessageThatNamesTheSource() = runBlocking {
        val api = FlakyDjenApi(
            respostas = List(3) { Resultado.Erro(httpException(500)) },
        )
        val dataSource = DjenRemoteDataSource(
            api = api,
            esperasEntreTentativasMs = SEM_ESPERA,
        )

        val erro = runCatching {
            dataSource.buscarComunicacoes(
                numeroOab = "123",
                ufOab = "MG",
                dataInicio = LocalDate.of(2026, 11, 4),
                dataFim = LocalDate.of(2026, 11, 19),
            )
        }.exceptionOrNull()

        assertTrue(erro is DjenIndisponivelException)
        // A mensagem vai parar na Auditoria, lida por advogado: tem de dizer de
        // quem é a falha, não só repetir o código do Retrofit.
        assertEquals(
            "DJEN indisponível (HTTP 500) após 3 tentativas. A próxima execução tenta de novo.",
            erro?.message,
        )
        assertEquals(3, api.chamadas)
    }

    @Test
    fun doesNotRetryClientError() = runBlocking {
        val api = FlakyDjenApi(respostas = listOf(Resultado.Erro(httpException(400))))
        val dataSource = DjenRemoteDataSource(
            api = api,
            esperasEntreTentativasMs = SEM_ESPERA,
        )

        val erro = runCatching {
            dataSource.buscarComunicacoes(
                numeroOab = "123",
                ufOab = "MG",
                dataInicio = LocalDate.of(2026, 11, 4),
                dataFim = LocalDate.of(2026, 11, 19),
            )
        }.exceptionOrNull()

        assertTrue(erro is HttpException)
        assertEquals(1, api.chamadas)
    }

    private fun httpException(codigo: Int): HttpException =
        HttpException(Response.error<DjenResponseDto>(codigo, "".toResponseBody()))

    private fun item(id: Long): DjenComunicacaoDto =
        DjenComunicacaoDto(id = id, texto = "Publicacao $id")

    private sealed interface Resultado {
        data class Ok(val resposta: DjenResponseDto) : Resultado
        data class Erro(val erro: Exception) : Resultado
    }

    private class FlakyDjenApi(
        private val respostas: List<Resultado>,
    ) : DjenApi {
        var chamadas = 0

        override suspend fun buscarComunicacoes(
            numeroOab: String?,
            ufOab: String?,
            nomeAdvogado: String?,
            dataDisponibilizacaoInicio: String,
            dataDisponibilizacaoFim: String,
            pagina: Int,
            itensPorPagina: Int,
        ): DjenResponseDto {
            val resultado = respostas[chamadas]
            chamadas += 1
            return when (resultado) {
                is Resultado.Ok -> resultado.resposta
                is Resultado.Erro -> throw resultado.erro
            }
        }

        override suspend fun baixarCertidao(hash: String): ResponseBody =
            ByteArray(0).toResponseBody()
    }

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
