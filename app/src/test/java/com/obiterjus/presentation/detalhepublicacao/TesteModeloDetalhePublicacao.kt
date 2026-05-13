package com.obiterjus.presentation.detalhepublicacao

import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoParticipante
import com.obiterjus.domain.repository.RepositorioProcessos
import com.obiterjus.domain.repository.RepositorioPublicacoes
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObterPublicacaoPorId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class TesteModeloDetalhePublicacao {
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
    fun cruzaPartesDoDataJudComAdvogadosDaPublicacao() = runTest {
        val processos = FakeRepositorioProcessos(
            processos = listOf(
                processo(
                    numeroProcesso = "50110879520258130245",
                    participantes = listOf(
                        participanteProcesso("ATIVO", "João de Souza", "AUTOR"),
                        participanteProcesso("PASSIVO", "Maria Lima", "RÉU"),
                    ),
                ),
            ),
        )
        val publicacoes = FakeRepositorioPublicacoes(
            publicacoes = listOf(
                publicacao(
                    id = 10L,
                    participantes = listOf(
                        participantePublicacao("Advogado", "Ana Ribeiro OAB/SP 12345"),
                    ),
                ),
            ),
        )

        val viewModel = ModeloDetalhePublicacao(
            obterPublicacaoPorId = ObterPublicacaoPorId(publicacoes),
            observarProcessos = ObservarProcessos(processos),
        )

        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        assertEquals("João de Souza", viewModel.estado.value.parteAtivaNome)
        assertEquals("Maria Lima", viewModel.estado.value.partePassivaNome)
        assertEquals(listOf("Ana Ribeiro OAB/SP 12345"), viewModel.estado.value.advogados)
    }

    @Test
    fun usaParticipantesDaPublicacaoQuandoNaoExisteDataJudLocal() = runTest {
        val processos = FakeRepositorioProcessos(emptyList())
        val publicacoes = FakeRepositorioPublicacoes(
            publicacoes = listOf(
                publicacao(
                    id = 20L,
                    participantes = listOf(
                        participantePublicacao("Autor", "Carlos Mendes"),
                        participantePublicacao("Réu", "Empresa Alfa"),
                        participantePublicacao("Advogado", "Patricia Gomes OAB/MG 99999"),
                    ),
                ),
            ),
        )

        val viewModel = ModeloDetalhePublicacao(
            obterPublicacaoPorId = ObterPublicacaoPorId(publicacoes),
            observarProcessos = ObservarProcessos(processos),
        )

        viewModel.aoCarregar(20L)
        advanceUntilIdle()

        assertEquals("Carlos Mendes", viewModel.estado.value.parteAtivaNome)
        assertEquals("Empresa Alfa", viewModel.estado.value.partePassivaNome)
        assertEquals(listOf("Patricia Gomes OAB/MG 99999"), viewModel.estado.value.advogados)
    }

    private fun publicacao(
        id: Long,
        participantes: List<PublicacaoParticipante>,
    ): Publicacao =
        Publicacao(
            id = id,
            hash = "hash-$id",
            numeroProcesso = "50110879520258130245",
            participantes = participantes,
            prazo = null,
            dataDisponibilizacao = LocalDate.of(2026, 5, 1),
            tribunal = "TJMG",
            tipoComunicacao = "Intimacao",
            nomeOrgao = "1a Vara",
            textoLimpo = "Texto da publicação.",
            isSigiloso = false,
            fonte = "DJEN",
            capturadoEm = Instant.parse("2026-05-01T12:00:00Z"),
            atualizadoEm = Instant.parse("2026-05-01T12:00:00Z"),
        )

    private fun participanteProcesso(
        polo: String,
        nome: String,
        tipoParticipacao: String,
    ): ParticipanteProcesso =
        ParticipanteProcesso(
            idLocal = "$polo-$nome",
            numeroProcesso = "50110879520258130245",
            polo = polo,
            nome = nome,
            tipoPessoa = "FISICA",
            tipoParticipacao = tipoParticipacao,
        )

    private fun participantePublicacao(
        tipo: String,
        nome: String,
    ): PublicacaoParticipante =
        PublicacaoParticipante(
            tipo = tipo,
            nome = nome,
            documento = null,
        )

    private class FakeRepositorioPublicacoes(
        publicacoes: List<Publicacao>,
    ) : RepositorioPublicacoes {
        private val fluxo = MutableStateFlow(publicacoes)

        override fun observarPublicacoes(): Flow<List<Publicacao>> = fluxo

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            fluxo.map { itens -> itens.filter { it.numeroProcesso == numeroProcesso } }

        override fun observarPublicacao(id: Long): Flow<Publicacao?> =
            fluxo.map { itens -> itens.firstOrNull { it.id == id } }
    }

    private class FakeRepositorioProcessos(
        processos: List<ProcessoMonitorado>,
    ) : RepositorioProcessos {
        private val fluxo = MutableStateFlow(processos)

        override fun observarProcessos(): Flow<List<ProcessoMonitorado>> = fluxo

        override fun observarMovimentos(numeroProcesso: String) =
            flowOf(emptyList<com.obiterjus.domain.model.MovimentoProcesso>())

        override fun observarParticipantes(numeroProcesso: String) =
            flowOf(emptyList<ParticipanteProcesso>())

        override suspend fun obterProcesso(numeroProcesso: String): ProcessoMonitorado? =
            fluxo.value.firstOrNull { it.numeroProcesso == numeroProcesso }

        override suspend fun salvarProcesso(processo: ProcessoMonitorado) {
            fluxo.value = fluxo.value.filterNot { it.numeroProcesso == processo.numeroProcesso } + processo
        }

        override suspend fun excluirProcesso(numeroProcesso: String) {
            fluxo.value = fluxo.value.filterNot { it.numeroProcesso == numeroProcesso }
        }
    }

    private fun processo(
        numeroProcesso: String,
        participantes: List<ParticipanteProcesso>,
    ): ProcessoMonitorado =
        ProcessoMonitorado(
            numeroProcesso = numeroProcesso,
            tribunal = "TJMG",
            grau = null,
            classeCodigo = null,
            classeNome = "Procedimento comum",
            assuntos = listOf("Contratos"),
            orgaoJulgadorCodigo = null,
            orgaoJulgadorNome = "1a Vara Civel",
            nivelSigilo = null,
            dataAjuizamento = null,
            syncStatus = ProcessoSyncStatus.SYNCED,
            capturadoEm = Instant.parse("2026-05-01T12:00:00Z"),
            atualizadoEm = Instant.parse("2026-05-01T12:00:00Z"),
            participantes = participantes,
        )
}
