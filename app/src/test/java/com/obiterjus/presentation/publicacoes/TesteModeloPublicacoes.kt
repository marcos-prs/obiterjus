package com.obiterjus.presentation.publicacoes

import android.net.Uri
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.CertidaoDjenRepository
import com.obiterjus.domain.repository.RepositorioPublicacoes
import com.obiterjus.domain.usecase.ObservarPublicacoes
import com.obiterjus.domain.usecase.ObterCertidaoDjen
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TesteModeloPublicacoes {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun filtersPublicationsByTextAndSecrecy() = runTest {
        val repositorio = FakeRepositorioPublicacoes(
            publicacoes = listOf(
                publicacao(id = 1L, tribunal = "TJMG", isSigiloso = false),
                publicacao(id = 2L, tribunal = "TJSP", isSigiloso = true),
            ),
        )
        val viewModel = PublicacoesViewModel(
            observarPublicacoes = ObservarPublicacoes(repositorio),
            obterCertidaoDjen = ObterCertidaoDjen(FakeCertidaoDjenRepository()),
        )
        advanceUntilIdle()

        viewModel.aoAlterarFiltroTexto("tjsp")
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.estado.value.publicacoes.map { it.id })

        viewModel.aoAlternarSomenteSigilosas()
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.estado.value.publicacoes.map { it.id })

        viewModel.aoAlterarFiltroTexto("tjmg")
        advanceUntilIdle()

        assertEquals(emptyList<Long>(), viewModel.estado.value.publicacoes.map { it.id })
    }

    @Test
    fun filtersPublicationsByStructuredFieldsAndDateRange() = runTest {
        val repositorio = FakeRepositorioPublicacoes(
            publicacoes = listOf(
                publicacao(
                    id = 1L,
                    tribunal = "TJMG",
                    tipoComunicacao = "Intimacao",
                    dataDisponibilizacao = LocalDate.of(2026, 4, 10),
                    isSigiloso = false,
                ),
                publicacao(
                    id = 2L,
                    tribunal = "TJSP",
                    tipoComunicacao = "Citacao",
                    dataDisponibilizacao = LocalDate.of(2026, 4, 20),
                    isSigiloso = false,
                ),
                publicacao(
                    id = 3L,
                    tribunal = "TJMG",
                    tipoComunicacao = "Citacao",
                    dataDisponibilizacao = LocalDate.of(2026, 5, 2),
                    isSigiloso = false,
                ),
            ),
        )
        val viewModel = PublicacoesViewModel(
            observarPublicacoes = ObservarPublicacoes(repositorio),
            obterCertidaoDjen = ObterCertidaoDjen(FakeCertidaoDjenRepository()),
        )
        advanceUntilIdle()

        viewModel.aoAlterarFiltroTribunal("tjmg")
        viewModel.aoAlterarFiltroTipo("cit")
        viewModel.aoAlterarFiltroDataInicio("01/05/2026")
        viewModel.aoAlterarFiltroDataFim("05/05/2026")
        advanceUntilIdle()

        assertEquals(listOf(3L), viewModel.estado.value.publicacoes.map { it.id })

        viewModel.aoLimparFiltros()
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), viewModel.estado.value.publicacoes.map { it.id })
    }

    @Test
    fun selectsAndClosesPublicationDetail() = runTest {
        val repositorio = FakeRepositorioPublicacoes(
            publicacoes = listOf(
                publicacao(id = 1L, tribunal = "TJMG", isSigiloso = false),
            ),
        )
        val viewModel = PublicacoesViewModel(
            observarPublicacoes = ObservarPublicacoes(repositorio),
            obterCertidaoDjen = ObterCertidaoDjen(FakeCertidaoDjenRepository()),
        )
        advanceUntilIdle()

        viewModel.aoSelecionarPublicacao(1L)
        advanceUntilIdle()

        assertEquals(1L, viewModel.estado.value.publicacaoSelecionada?.id)

        viewModel.aoFecharDetalhe()
        advanceUntilIdle()

        assertEquals(null, viewModel.estado.value.publicacaoSelecionada)
    }

    @Test
    fun reportsErrorWhenPublicationDoesNotHaveCertificateHash() = runTest {
        val repositorio = FakeRepositorioPublicacoes(
            publicacoes = listOf(
                publicacao(id = 1L, tribunal = "TJMG", isSigiloso = false, hash = null),
            ),
        )
        val viewModel = PublicacoesViewModel(
            observarPublicacoes = ObservarPublicacoes(repositorio),
            obterCertidaoDjen = ObterCertidaoDjen(FakeCertidaoDjenRepository()),
        )
        advanceUntilIdle()

        viewModel.aoAbrirCertidao(viewModel.estado.value.publicacoes.first())
        advanceUntilIdle()

        assertEquals(true, viewModel.estado.value.certidao.error)
    }

    private fun publicacao(
        id: Long,
        tribunal: String,
        isSigiloso: Boolean,
        tipoComunicacao: String = "Intimacao",
        dataDisponibilizacao: LocalDate = LocalDate.of(2026, 4, 29),
        hash: String? = "hash-$id",
        prazo: PublicacaoPrazo? = null,
    ): Publicacao =
        Publicacao(
            id = id,
            hash = hash,
            numeroProcesso = "50110879520258130245",
            participantes = emptyList(),
            prazo = prazo,
            dataDisponibilizacao = dataDisponibilizacao,
            tribunal = tribunal,
            tipoComunicacao = tipoComunicacao,
            nomeOrgao = "1a Vara",
            textoLimpo = "Intime-se.",
            isSigiloso = isSigiloso,
            fonte = "DJEN",
            capturadoEm = Instant.parse("2026-04-29T12:00:00Z"),
            atualizadoEm = Instant.parse("2026-04-29T12:00:00Z"),
        )

    private class FakeCertidaoDjenRepository : CertidaoDjenRepository {
        override suspend fun obterCertidao(hash: String): Uri =
            error("A certidao nao deveria ser consultada neste teste.")
    }

    private class FakeRepositorioPublicacoes(
        publicacoes: List<Publicacao>,
    ) : RepositorioPublicacoes {
        private val fluxo = MutableStateFlow(publicacoes)

        override fun observarPublicacoes(): Flow<List<Publicacao>> = fluxo

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            fluxo.map { publicacoes ->
                publicacoes.filter { it.numeroProcesso == numeroProcesso }
            }
    }
}
