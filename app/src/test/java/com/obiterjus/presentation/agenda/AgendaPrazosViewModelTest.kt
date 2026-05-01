package com.obiterjus.presentation.agenda

import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.RepositorioPublicacoes
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
class AgendaPrazosViewModelTest {
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
    fun classifiesDeadlineAgendaItems() = runTest {
        val repositorio = FakeRepositorioPublicacoes(
            publicacoes = listOf(
                publicacao(id = 1L, dataLimite = LocalDate.of(2026, 4, 29)),
                publicacao(id = 2L, dataLimite = LocalDate.of(2026, 5, 2)),
                publicacao(id = 3L, dataLimite = null),
                publicacao(id = 4L, prazo = null),
            ),
        )
        val viewModel = AgendaPrazosViewModel(
            observarAgendaPrazos = ObservarAgendaPrazos(repositorio),
            clock = Clock.fixed(
                Instant.parse("2026-04-30T12:00:00Z"),
                ZoneOffset.UTC,
            ),
        )

        advanceUntilIdle()

        assertEquals(3, viewModel.estado.value.total)
        assertEquals(1, viewModel.estado.value.vencidos)
        assertEquals(1, viewModel.estado.value.proximos)
        assertEquals(1, viewModel.estado.value.semData)
        assertEquals(
            listOf(1L, 2L, 3L),
            viewModel.estado.value.itens.map { it.item.publicacao.id },
        )
    }

    private fun publicacao(
        id: Long,
        dataLimite: LocalDate? = LocalDate.of(2026, 5, 10),
        prazo: PublicacaoPrazo? = PublicacaoPrazo(
            quantidade = 5,
            unidade = "dias",
            diasUteis = false,
            textoOriginal = "prazo de 5 dias",
            dataLimiteEstimada = dataLimite,
        ),
    ): Publicacao =
        Publicacao(
            id = id,
            hash = "hash-$id",
            numeroProcesso = "50110879520258130245",
            participantes = emptyList(),
            prazo = prazo,
            dataDisponibilizacao = LocalDate.of(2026, 4, 29),
            tribunal = "TJMG",
            tipoComunicacao = "Intimacao",
            nomeOrgao = "1a Vara",
            textoLimpo = "Intime-se no prazo de 5 dias.",
            isSigiloso = false,
            fonte = "DJEN",
            capturadoEm = Instant.parse("2026-04-29T12:00:00Z"),
            atualizadoEm = Instant.parse("2026-04-29T12:00:00Z"),
        )

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
