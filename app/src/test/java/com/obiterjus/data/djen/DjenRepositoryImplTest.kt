package com.obiterjus.data.djen

import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.djen.mapper.DjenMapper
import com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper
import com.obiterjus.data.djen.remote.DjenApi
import com.obiterjus.data.djen.remote.DjenRemoteDataSource
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.time.BrasilApiDataSource
import com.obiterjus.data.time.FeriadoDto
import com.obiterjus.data.time.FeriadoRepository
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.usecase.CalcularPrazoRegraUC
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class DjenRepositoryImplTest {

    private val feriadoRepository = FeriadoRepository(
        brasilApiDataSource = object : BrasilApiDataSource {
            override suspend fun getFeriadosNacionais(ano: Int): Response<List<FeriadoDto>> =
                Response.success(emptyList())
        }
    )
    private val calculadoraPrazos = CalculadoraPrazos(feriadoRepository)
    private val djenPrazoExtractor = DjenPrazoExtractor(calculadoraPrazos)
    private val calcularPrazoRegraUC = CalcularPrazoRegraUC(calculadoraPrazos, djenPrazoExtractor)
    private val djenMapper = DjenMapper(calcularPrazoRegraUC)
    private val publicacaoPrazoMapper = PublicacaoPrazoMapper(calcularPrazoRegraUC)

    @Test
    fun returnsNewProcessesWithTribunalForDataJudSync() = runBlocking {
        val repository = DjenRepositoryImpl(
            remoteDataSource = DjenRemoteDataSource(
                api = FakeDjenApi(
                    response = DjenResponseDto(
                        count = 1,
                        items = listOf(
                            DjenComunicacaoDto(
                                id = 1L,
                                siglaTribunal = "TJMG",
                                numeroProcesso = "5011087-95.2025.8.13.0245",
                                texto = "Intime-se. OAB/MG 12345.",
                            ),
                        ),
                    ),
                ),
                itensPorPagina = 100,
            ),
            localPublicacaoRepository = LocalPublicacaoRepository(FakePublicacaoDao(existingIds = emptyList())),
            djenMapper = djenMapper,
            publicacaoPrazoMapper = publicacaoPrazoMapper,
            clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC),
        )

        val resumo = repository.monitorar(params())

        assertEquals(listOf("50110879520258130245"), resumo.processosNovos)
        assertEquals(1, resumo.processosParaSincronizar.size)
        assertEquals("50110879520258130245", resumo.processosParaSincronizar.first().numeroProcesso)
        assertEquals("TJMG", resumo.processosParaSincronizar.first().tribunal)
    }

    private fun params(): MonitorarDjenParams =
        MonitorarDjenParams(
            numeroOab = "12345",
            ufOab = "MG",
            dataInicio = LocalDate.of(2026, 4, 1),
            dataFim = LocalDate.of(2026, 4, 29),
            modo = MonitorarDjenModo.MANUAL,
        )

    private class FakeDjenApi(
        private val response: DjenResponseDto,
    ) : DjenApi {
        override suspend fun buscarComunicacoes(
            numeroOab: String?,
            ufOab: String?,
            nomeAdvogado: String?,
            dataDisponibilizacaoInicio: String,
            dataDisponibilizacaoFim: String,
            pagina: Int,
            itensPorPagina: Int,
        ): DjenResponseDto = response

        override suspend fun baixarCertidao(hash: String): ResponseBody =
            ByteArray(0).toResponseBody()
    }

    private class FakePublicacaoDao(
        private val existingIds: List<Long>,
    ) : PublicacaoDao {
        val saved = mutableListOf<PublicacaoEntity>()

        override suspend fun upsert(publicacao: PublicacaoEntity) {
            saved += publicacao
        }

        override suspend fun upsertAll(publicacoes: List<PublicacaoEntity>) {
            saved += publicacoes
        }

        override suspend fun getExistingIds(ids: List<Long>): List<Long> = existingIds

        override suspend fun getByHashes(hashes: List<String>): List<PublicacaoEntity> = emptyList()

        override suspend fun getById(id: Long): PublicacaoEntity? =
            saved.firstOrNull { it.id == id }

        override fun observeById(id: Long): Flow<PublicacaoEntity?> = emptyFlow()

        override suspend fun getByIds(ids: List<Long>): List<PublicacaoEntity> =
            saved.filter { it.id in ids }

        override fun observePublicacoes(
            numeroProcesso: String?,
            tribunal: String?,
            tipoComunicacao: String?,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
            somenteSigilosas: Boolean?,
        ): Flow<List<PublicacaoEntity>> = emptyFlow()

        override fun observePorProcesso(numeroProcesso: String): Flow<List<PublicacaoEntity>> = emptyFlow()

        override suspend fun getNumerosProcessoDistintos(): List<String> = emptyList()
    }
}
