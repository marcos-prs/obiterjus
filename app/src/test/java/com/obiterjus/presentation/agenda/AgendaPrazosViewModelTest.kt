package com.obiterjus.presentation.prazos

import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.CalendarSyncRepository
import com.obiterjus.domain.repository.PublicacoesRepository
import com.obiterjus.domain.usecase.ClassificarPublicacaoUC
import com.obiterjus.domain.usecase.ConfirmarPrazoUC
import com.obiterjus.domain.usecase.MarcarPrazoCumpridoUC
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import com.obiterjus.domain.repository.FakeClientesRepository
import com.obiterjus.domain.usecase.ObservarClientesPorProcesso
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
        val prazoSugeridoDao = FakePrazoSugeridoDao()
        val repositorio = FakePublicacoesRepository(
            publicacoes = listOf(
                publicacao(id = 1L, dataLimite = LocalDate.of(2026, 4, 29)),
                publicacao(id = 2L, dataLimite = LocalDate.of(2026, 5, 2)),
                publicacao(id = 3L, dataLimite = null),
                publicacao(id = 4L, prazo = null),
            ),
        )
        val viewModel = PrazosViewModel(
            observarAgendaPrazos = ObservarAgendaPrazos(
                repository = repositorio,
                prazoSugeridoDao = prazoSugeridoDao,
                observarClientesPorProcesso = ObservarClientesPorProcesso(FakeParticipanteDao(), FakeClientesRepository()),
                classificarPublicacaoUC = classificarPublicacaoUC(),
            ),
            confirmarPrazoUC = ConfirmarPrazoUC(
                prazoSugeridoDao = prazoSugeridoDao,
                calendarSyncRepository = FakeCalendarSyncRepository(),
            ),
            marcarPrazoCumpridoUC = MarcarPrazoCumpridoUC(
                prazoSugeridoDao = prazoSugeridoDao,
            ),
            clock = Clock.fixed(
                Instant.parse("2026-04-30T12:00:00Z"),
                ZoneOffset.UTC,
            ),
            textos = TextosPrazosFake,
        )

        advanceUntilIdle()

        assertEquals(4, viewModel.estado.value.itens.size)
        assertEquals(1, viewModel.estado.value.expirados.size)
        assertEquals(1, viewModel.estado.value.urgente.size)
        assertEquals(2, viewModel.estado.value.estaSemana.size)
        assertEquals(0, viewModel.estado.value.semData.size)
        assertEquals(
            listOf(1L, 2L, 3L, 4L),
            viewModel.estado.value.itens.map { prazo -> prazo.item.publicacao.id },
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

    private fun classificarPublicacaoUC(): ClassificarPublicacaoUC =
        ClassificarPublicacaoUC(CalculadoraPrazos(FakeCalendarioForenseDataSource()))

    private class FakePublicacoesRepository(
        publicacoes: List<Publicacao>,
    ) : PublicacoesRepository {
        private val fluxo = MutableStateFlow(publicacoes)

        override fun observarPublicacoes(): Flow<List<Publicacao>> = fluxo

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            fluxo.map { publicacoes ->
                publicacoes.filter { it.numeroProcesso == numeroProcesso }
            }

        override fun observarPublicacao(id: Long): Flow<Publicacao?> =
            fluxo.map { publicacoes -> publicacoes.firstOrNull { it.id == id } }
    }

    private class FakePrazoSugeridoDao : PrazoSugeridoDao {
        private val prazos = MutableStateFlow(emptyList<PrazoSugeridoEntity>())

        override suspend fun insert(prazoSugerido: PrazoSugeridoEntity): Long = prazoSugerido.id
        override suspend fun update(prazoSugerido: PrazoSugeridoEntity) = Unit
        override suspend fun getByPublicacaoId(publicacaoId: Long): PrazoSugeridoEntity? = null
        override fun observeByPublicacaoId(publicacaoId: Long): Flow<PrazoSugeridoEntity?> =
            prazos.map { itens -> itens.firstOrNull { it.publicacaoId == publicacaoId } }
        override suspend fun getPrazosParaSincronizar(provedores: List<String>): List<PrazoSugeridoEntity> = emptyList()
        override fun observeAll(): Flow<List<PrazoSugeridoEntity>> = prazos
    }

    private class FakeParticipanteDao : ParticipanteDao {
        private val participantes = MutableStateFlow(emptyList<ParticipanteEntity>())

        override suspend fun upsertAll(participantes: List<ParticipanteEntity>) {
            this.participantes.value = participantes
        }
        override fun observeByNumeroProcesso(numeroProcesso: String): Flow<List<ParticipanteEntity>> =
            participantes.map { itens -> itens.filter { it.numeroProcesso == numeroProcesso } }
        override suspend fun getByNumeroProcesso(numeroProcesso: String): List<ParticipanteEntity> =
            participantes.value.filter { it.numeroProcesso == numeroProcesso }
        override fun observeAll(): Flow<List<ParticipanteEntity>> = participantes
        override suspend fun getAll(): List<ParticipanteEntity> = participantes.value
        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) {
            participantes.value = participantes.value.filterNot { it.numeroProcesso == numeroProcesso }
        }
    }

    private class FakeCalendarSyncRepository : CalendarSyncRepository {
        override suspend fun syncPrazo(
            prazo: PublicacaoPrazo,
            title: String,
            description: String,
            provedor: ProvedorCalendario,
        ): Result<String> = Result.success("evento")

        override suspend fun cancelPrazo(
            idExterno: String,
            provedor: ProvedorCalendario,
        ): Result<Unit> = Result.success(Unit)
    }

    // O app não calcula prazos localmente: a data-limite dos itens 3 e 4 vem
    // da API CalendárioForense (aqui simulada com vencimento em 05/05).
    private class FakeCalendarioForenseDataSource : CalendarioForenseDataSource {
        override suspend fun calcularPrazo(pedido: PedidoCalculoPrazo): RespostaPrazo =
            RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-05-05")
    }

    private object TextosPrazosFake : TextosPrazos {
        override fun get(resId: Int): String = resId.toString()
        override fun get(resId: Int, vararg args: Any): String = get(resId)
    }
}
