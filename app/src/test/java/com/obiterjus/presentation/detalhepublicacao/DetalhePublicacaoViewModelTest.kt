package com.obiterjus.presentation.detalhepublicacao

import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoParticipante
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.ProcessosRepository
import com.obiterjus.domain.repository.PublicacoesRepository
import com.obiterjus.domain.usecase.CadastrarPrazoManualUC
import com.obiterjus.domain.usecase.CalendarSyncRepositoryFake
import com.obiterjus.domain.usecase.ConfirmarPrazoUC
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObterPublicacaoPorId
import com.obiterjus.domain.usecase.PrazoSugeridoDaoEmMemoria
import com.obiterjus.domain.usecase.ResolverNaturezaProcessoUC
import com.obiterjus.presentation.prazos.TextosPrazos
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetalhePublicacaoViewModelTest {
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
        val processos = FakeProcessosRepository(
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
        val publicacoes = FakePublicacoesRepository(
            publicacoes = listOf(
                publicacao(
                    id = 10L,
                    participantes = listOf(
                        participantePublicacao("Advogado", "Ana Ribeiro OAB/SP 12345"),
                    ),
                ),
            ),
        )

        val viewModel = criarViewModel(publicacoes = publicacoes, processos = processos)

        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        assertEquals("João de Souza", viewModel.estado.value.parteAtivaNome)
        assertEquals("Maria Lima", viewModel.estado.value.partePassivaNome)
        assertEquals(listOf("Ana Ribeiro OAB/SP 12345"), viewModel.estado.value.advogados)
    }

    @Test
    fun usaParticipantesDaPublicacaoQuandoNaoExisteDataJudLocal() = runTest {
        val publicacoes = FakePublicacoesRepository(
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

        val viewModel = criarViewModel(publicacoes = publicacoes)

        viewModel.aoCarregar(20L)
        advanceUntilIdle()

        assertEquals("Carlos Mendes", viewModel.estado.value.parteAtivaNome)
        assertEquals("Empresa Alfa", viewModel.estado.value.partePassivaNome)
        assertEquals(listOf("Patricia Gomes OAB/MG 99999"), viewModel.estado.value.advogados)
    }

    @Test
    fun expoePrazoTribunalEHabilitacaoDeCadastro() = runTest {
        val prazo = PublicacaoPrazo(
            quantidade = 5,
            unidade = "dias",
            diasUteis = true,
            textoOriginal = "prazo de 5 dias",
            dataLimiteEstimada = LocalDate.of(2026, 7, 22),
        )
        val viewModel = criarViewModel(
            publicacoes = FakePublicacoesRepository(
                publicacoes = listOf(publicacao(id = 10L, prazo = prazo)),
            ),
        )

        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        assertEquals("TJMG", viewModel.estado.value.tribunal)
        assertEquals(prazo, viewModel.estado.value.prazoAtual)
        assertTrue(viewModel.estado.value.podeCadastrarPrazo)
    }

    @Test
    fun calculoConfiavelLevaAoResultado() = runTest {
        val viewModel = criarViewModel(
            calendarioForense = { RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-20") },
        )
        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        viewModel.aoAbrirCadastroPrazo()
        viewModel.aoAlterarSelecao(quantidadeDias = 10, diasUteis = true)
        viewModel.aoCalcular()
        advanceUntilIdle()

        assertEquals(
            FluxoCadastroPrazo.Resultado(
                quantidadeDias = 10,
                diasUteis = true,
                dataCalculada = LocalDate.of(2026, 5, 20),
                confianca = ConfiancaCalculo.CONFIAVEL,
            ),
            viewModel.fluxoCadastro.value,
        )
    }

    @Test
    fun calculoBloqueadoComDataLevaAoResultadoIncerto() = runTest {
        val viewModel = criarViewModel(
            calendarioForense = {
                RespostaPrazo(estado = "BLOQUEADO_FERIADO", dataVencimento = "2026-05-21")
            },
        )
        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        viewModel.aoAbrirCadastroPrazo()
        viewModel.aoCalcular()
        advanceUntilIdle()

        val fluxo = viewModel.fluxoCadastro.value as FluxoCadastroPrazo.Resultado
        assertEquals(ConfiancaCalculo.INCERTO, fluxo.confianca)
        assertEquals(LocalDate.of(2026, 5, 21), fluxo.dataCalculada)
    }

    @Test
    fun bloqueioSemDataSinalizadoComoBloqueadoPelaApi() = runTest {
        val viewModel = criarViewModel(
            calendarioForense = { RespostaPrazo(estado = "BLOQUEADO_NATUREZA") },
        )
        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        viewModel.aoAbrirCadastroPrazo()
        viewModel.aoCalcular()
        advanceUntilIdle()

        val fluxo = viewModel.fluxoCadastro.value as FluxoCadastroPrazo.ErroCalculo
        assertTrue(fluxo.bloqueadoPelaApi)
        assertEquals(false, fluxo.tribunalAusente)
    }

    @Test
    fun falhaDeRedeLevaAoErroDeCalculo() = runTest {
        val viewModel = criarViewModel(
            calendarioForense = { throw RuntimeException("timeout") },
        )
        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        viewModel.aoAbrirCadastroPrazo()
        viewModel.aoCalcular()
        advanceUntilIdle()

        val fluxo = viewModel.fluxoCadastro.value as FluxoCadastroPrazo.ErroCalculo
        assertEquals(false, fluxo.tribunalAusente)
    }

    @Test
    fun tribunalAusenteSinalizadoNoErro() = runTest {
        val viewModel = criarViewModel(
            publicacoes = FakePublicacoesRepository(
                publicacoes = listOf(publicacao(id = 10L, tribunal = null)),
            ),
        )
        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        viewModel.aoAbrirCadastroPrazo()
        viewModel.aoCalcular()
        advanceUntilIdle()

        val fluxo = viewModel.fluxoCadastro.value as FluxoCadastroPrazo.ErroCalculo
        assertTrue(fluxo.tribunalAusente)
    }

    @Test
    fun confirmarProvedorSalvaEPublicaResultado() = runTest {
        val viewModel = criarViewModel()
        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        viewModel.aoAbrirCadastroPrazo()
        viewModel.aoCalcular()
        advanceUntilIdle()
        viewModel.aoConfirmarData()
        viewModel.aoConfirmarProvedor(ProvedorCalendario.LOCAL)
        advanceUntilIdle()

        assertEquals(FluxoCadastroPrazo.Fechado, viewModel.fluxoCadastro.value)
        assertEquals(
            ConfirmacaoPrazoResultado.ConfirmadoLocalmente,
            viewModel.resultadoCadastroPrazo.value,
        )

        viewModel.aoConsumirResultadoCadastro()
        assertNull(viewModel.resultadoCadastroPrazo.value)
    }

    @Test
    fun abrirCadastroPrePreencheComPrazoAtual() = runTest {
        val viewModel = criarViewModel(
            publicacoes = FakePublicacoesRepository(
                publicacoes = listOf(
                    publicacao(
                        id = 10L,
                        prazo = PublicacaoPrazo(
                            quantidade = 30,
                            unidade = "dias",
                            diasUteis = false,
                            textoOriginal = "trinta dias",
                            dataLimiteEstimada = LocalDate.of(2026, 8, 1),
                        ),
                    ),
                ),
            ),
        )
        viewModel.aoCarregar(10L)
        advanceUntilIdle()

        viewModel.aoAbrirCadastroPrazo()

        assertEquals(
            FluxoCadastroPrazo.Selecionando(quantidadeDias = 30, diasUteis = false),
            viewModel.fluxoCadastro.value,
        )
    }

    private fun criarViewModel(
        publicacoes: FakePublicacoesRepository = FakePublicacoesRepository(
            publicacoes = listOf(publicacao(id = 10L)),
        ),
        processos: FakeProcessosRepository = FakeProcessosRepository(emptyList()),
        calendarioForense: suspend (PedidoCalculoPrazo) -> RespostaPrazo = {
            RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-20")
        },
    ): DetalhePublicacaoViewModel {
        val prazoSugeridoDao = PrazoSugeridoDaoEmMemoria()
        val sync = CalendarSyncRepositoryFake()
        val calculadora = CalculadoraPrazos(
            object : CalendarioForenseDataSource {
                override suspend fun calcularPrazo(pedido: PedidoCalculoPrazo): RespostaPrazo =
                    calendarioForense(pedido)
            },
        )
        return DetalhePublicacaoViewModel(
            obterPublicacaoPorId = ObterPublicacaoPorId(publicacoes),
            observarProcessos = ObservarProcessos(processos),
            prazoSugeridoDao = prazoSugeridoDao,
            calculadoraPrazos = calculadora,
            resolverNaturezaProcessoUC = ResolverNaturezaProcessoUC(processos),
            cadastrarPrazoManualUC = CadastrarPrazoManualUC(
                publicacaoDao = PublicacaoDaoStub(),
                prazoSugeridoDao = prazoSugeridoDao,
                calendarSyncRepository = sync,
                confirmarPrazoUC = ConfirmarPrazoUC(prazoSugeridoDao, sync),
            ),
            textos = TextosFake,
        )
    }

    private object TextosFake : TextosPrazos {
        override fun get(resId: Int): String = resId.toString()
        override fun get(resId: Int, vararg args: Any): String = get(resId)
    }

    private class PublicacaoDaoStub : PublicacaoDao {
        override suspend fun atualizarPrazo(
            id: Long,
            quantidade: Int,
            unidade: String,
            diasUteis: Boolean,
            texto: String,
            dataLimite: LocalDate,
            confianca: String,
        ) = Unit

        override suspend fun upsert(publicacao: PublicacaoEntity) = Unit
        override suspend fun upsertAll(publicacoes: List<PublicacaoEntity>) = Unit
        override suspend fun getExistingIds(ids: List<Long>): List<Long> = emptyList()
        override suspend fun getByHashes(hashes: List<String>): List<PublicacaoEntity> = emptyList()
        override suspend fun getById(id: Long): PublicacaoEntity? = null
        override fun observeById(id: Long): Flow<PublicacaoEntity?> = flowOf(null)
        override suspend fun getByIds(ids: List<Long>): List<PublicacaoEntity> = emptyList()
        override fun observePublicacoes(
            numeroProcesso: String?,
            tribunal: String?,
            tipoComunicacao: String?,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
            somenteSigilosas: Boolean?,
        ): Flow<List<PublicacaoEntity>> = flowOf(emptyList())
        override fun observePorProcesso(numeroProcesso: String): Flow<List<PublicacaoEntity>> =
            flowOf(emptyList())
        override suspend fun getNumerosProcessoDistintos(): List<String> = emptyList()
        override suspend fun getByDataDisponibilizacao(data: LocalDate): List<PublicacaoEntity> =
            emptyList()
        override suspend fun atualizarStatusDuplicata(
            id: Long,
            duplicataDe: Long?,
            totalDuplicatas: Int,
        ) = Unit
    }

    private class FakePublicacoesRepository(
        publicacoes: List<Publicacao>,
    ) : PublicacoesRepository {
        private val fluxo = MutableStateFlow(publicacoes)

        override fun observarPublicacoes(): Flow<List<Publicacao>> = fluxo

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            fluxo.map { itens -> itens.filter { it.numeroProcesso == numeroProcesso } }

        override fun observarPublicacao(id: Long): Flow<Publicacao?> =
            fluxo.map { itens -> itens.firstOrNull { it.id == id } }
    }

    private class FakeProcessosRepository(
        processos: List<ProcessoMonitorado>,
    ) : ProcessosRepository {
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

    private companion object {
        fun publicacao(
            id: Long,
            participantes: List<PublicacaoParticipante> = emptyList(),
            prazo: PublicacaoPrazo? = null,
            tribunal: String? = "TJMG",
        ): Publicacao =
            Publicacao(
                id = id,
                hash = "hash-$id",
                numeroProcesso = "50110879520258130245",
                participantes = participantes,
                prazo = prazo,
                dataDisponibilizacao = LocalDate.of(2026, 5, 1),
                tribunal = tribunal,
                tipoComunicacao = "Intimacao",
                nomeOrgao = "1a Vara",
                textoLimpo = "Texto da publicação.",
                isSigiloso = false,
                fonte = "DJEN",
                capturadoEm = Instant.parse("2026-05-01T12:00:00Z"),
                atualizadoEm = Instant.parse("2026-05-01T12:00:00Z"),
            )

        fun participanteProcesso(
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

        fun participantePublicacao(
            tipo: String,
            nome: String,
        ): PublicacaoParticipante =
            PublicacaoParticipante(
                tipo = tipo,
                nome = nome,
                documento = null,
            )

        fun processo(
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
}
