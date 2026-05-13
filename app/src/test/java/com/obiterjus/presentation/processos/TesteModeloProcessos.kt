package com.obiterjus.presentation.processos

import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TimelineProcessoTipo
import com.obiterjus.domain.repository.RepositorioProcessos
import com.obiterjus.domain.repository.RepositorioPublicacoes
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObservarTimelineProcesso
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
class TesteModeloProcessos {
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
    fun filtroTextoBuscaPorTribunalClasseEOrgao() = runTest {
        val repositorio = RepositorioProcessosFake()
        val modelo = modelo(repositorio, RepositorioPublicacoesFake())
        advanceUntilIdle()

        modelo.aoAlterarFiltroTexto("vara")
        advanceUntilIdle()

        assertEquals(1, modelo.estado.value.processos.size)
        assertEquals("50110879520258130245", modelo.estado.value.processos.first().numeroProcesso)
    }

    @Test
    fun selecaoCarregaProcessoEMovimentos() = runTest {
        val repositorio = RepositorioProcessosFake()
        val publicacoes = RepositorioPublicacoesFake()
        val modelo = modelo(repositorio, publicacoes)
        advanceUntilIdle()

        modelo.aoSelecionarProcesso("50110879520258130245")
        advanceUntilIdle()

        assertEquals("50110879520258130245", modelo.estado.value.processoSelecionado?.numeroProcesso)
        assertEquals(2, modelo.estado.value.timelineSelecionada.size)
        assertEquals(
            TimelineProcessoTipo.MOVIMENTO_DATAJUD,
            modelo.estado.value.timelineSelecionada.first().tipo,
        )
    }

    private fun modelo(
        repositorio: RepositorioProcessosFake,
        repositorioPublicacoes: RepositorioPublicacoesFake,
    ): ModeloProcessos =
        ModeloProcessos(
            observarProcessos = ObservarProcessos(repositorio),
            observarTimelineProcesso = ObservarTimelineProcesso(
                repositorioProcessos = repositorio,
                repositorioPublicacoes = repositorioPublicacoes,
            ),
        )

    private class RepositorioProcessosFake : RepositorioProcessos {
        private val processos = MutableStateFlow(
            listOf(
                processo(
                    numeroProcesso = "50110879520258130245",
                    tribunal = "TJMG",
                    classeNome = "Procedimento comum",
                    orgaoJulgadorNome = "1a Vara Civel",
                ),
                processo(
                    numeroProcesso = "00000000000000000000",
                    tribunal = "TJSP",
                    classeNome = "Agravo",
                    orgaoJulgadorNome = "Camara",
                ),
            ),
        )
        private val movimentos = MutableStateFlow(
            mapOf(
                "50110879520258130245" to listOf(
                    MovimentoProcesso(
                        idLocal = "movimento-1",
                        numeroProcesso = "50110879520258130245",
                        codigo = 1,
                        nome = "Juntada",
                        dataHora = Instant.parse("2026-04-29T12:00:00Z"),
                        complementosJson = null,
                    ),
                ),
            ),
        )
        private val participantes = MutableStateFlow(
            mapOf(
                "50110879520258130245" to listOf(
                    ParticipanteProcesso(
                        idLocal = "part-1",
                        numeroProcesso = "50110879520258130245",
                        polo = "ATIVO",
                        nome = "João da Silva",
                        tipoPessoa = "Fisica",
                        tipoParticipacao = "ADVOGADO",
                    ),
                ),
            ),
        )

        override fun observarProcessos(): Flow<List<ProcessoMonitorado>> = processos

        override fun observarMovimentos(numeroProcesso: String): Flow<List<MovimentoProcesso>> =
            movimentos.map { it[numeroProcesso].orEmpty() }

        override fun observarParticipantes(numeroProcesso: String): Flow<List<ParticipanteProcesso>> =
            participantes.map { it[numeroProcesso].orEmpty() }

        override suspend fun obterProcesso(numeroProcesso: String): ProcessoMonitorado? =
            processos.value.firstOrNull { it.numeroProcesso == numeroProcesso }

        override suspend fun salvarProcesso(processo: ProcessoMonitorado) {
            processos.value = processos.value.filterNot { it.numeroProcesso == processo.numeroProcesso } + processo
        }

        override suspend fun excluirProcesso(numeroProcesso: String) {
            processos.value = processos.value.filterNot { it.numeroProcesso == numeroProcesso }
        }

        private companion object {
            fun processo(
                numeroProcesso: String,
                tribunal: String,
                classeNome: String,
                orgaoJulgadorNome: String,
            ) = ProcessoMonitorado(
                numeroProcesso = numeroProcesso,
                tribunal = tribunal,
                grau = null,
                classeCodigo = null,
                classeNome = classeNome,
                assuntos = listOf("Contratos Bancarios"),
                orgaoJulgadorCodigo = null,
                orgaoJulgadorNome = orgaoJulgadorNome,
                nivelSigilo = null,
                dataAjuizamento = null,
                syncStatus = ProcessoSyncStatus.SYNCED,
                capturadoEm = Instant.parse("2026-04-29T12:00:00Z"),
                atualizadoEm = Instant.parse("2026-04-29T12:00:00Z"),
            )
        }
    }

    private class RepositorioPublicacoesFake : RepositorioPublicacoes {
        private val publicacoes = MutableStateFlow(
            listOf(
                Publicacao(
                    id = 10L,
                    hash = "hash-10",
                    numeroProcesso = "50110879520258130245",
                    participantes = emptyList(),
                    prazo = null,
                    dataDisponibilizacao = LocalDate.of(2026, 4, 28),
                    tribunal = "TJMG",
                    tipoComunicacao = "Intimacao",
                    nomeOrgao = "1a Vara Civel",
                    textoLimpo = "Intime-se.",
                    isSigiloso = false,
                    fonte = "DJEN",
                    capturadoEm = Instant.parse("2026-04-28T12:00:00Z"),
                    atualizadoEm = Instant.parse("2026-04-28T12:00:00Z"),
                ),
            ),
        )

        override fun observarPublicacoes(): Flow<List<Publicacao>> = publicacoes

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            publicacoes.map { itens ->
                itens.filter { it.numeroProcesso == numeroProcesso }
            }

        override fun observarPublicacao(id: Long): Flow<Publicacao?> =
            publicacoes.map { itens -> itens.firstOrNull { it.id == id } }
    }
}
