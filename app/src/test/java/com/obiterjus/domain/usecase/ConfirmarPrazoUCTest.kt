package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.CalendarSyncRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmarPrazoUCTest {

    @Test
    fun `cria linha em prazos_sugeridos quando nao existe e confirma`() = runTest {
        val dao = PrazoSugeridoDaoEmMemoria()
        val uc = ConfirmarPrazoUC(dao, CalendarSyncRepositoryFake())

        val resultado = uc.invoke(
            publicacaoId = 1L,
            prazo = prazo(),
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.LOCAL,
        )

        assertEquals(
            ConfirmacaoPrazoResultado.ConfirmadoLocalmente,
            resultado.getOrNull(),
        )
        val entity = dao.getByPublicacaoId(1L)
        assertNotNull(entity)
        assertTrue(entity!!.isConfirmado)
        assertEquals(ProvedorCalendario.LOCAL.codigo, entity.provedorCalendario)
        assertEquals(5, entity.quantidade)
        assertEquals(LocalDate.of(2026, 7, 20), entity.dataLimite)
    }

    @Test
    fun `preserva linha existente ao confirmar`() = runTest {
        val dao = PrazoSugeridoDaoEmMemoria()
        dao.insert(
            PrazoSugeridoEntity(
                publicacaoId = 1L,
                quantidade = 15,
                unidade = "dias",
                diasUteis = false,
                textoOriginal = "quinze dias",
                dataLimite = LocalDate.of(2026, 8, 1),
            ),
        )
        val uc = ConfirmarPrazoUC(dao, CalendarSyncRepositoryFake())

        uc.invoke(
            publicacaoId = 1L,
            prazo = prazo(),
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.LOCAL,
        )

        val entity = dao.getByPublicacaoId(1L)
        assertNotNull(entity)
        assertTrue(entity!!.isConfirmado)
        assertEquals(15, entity.quantidade)
        assertEquals("quinze dias", entity.textoOriginal)
    }

    @Test
    fun `provedor local nao chama sincronizacao`() = runTest {
        val sync = CalendarSyncRepositoryFake()
        val uc = ConfirmarPrazoUC(PrazoSugeridoDaoEmMemoria(), sync)

        uc.invoke(
            publicacaoId = 1L,
            prazo = prazo(),
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.LOCAL,
        )

        assertEquals(0, sync.syncChamadas)
    }

    @Test
    fun `sync com sucesso grava id externo e retorna EventoCriado`() = runTest {
        val dao = PrazoSugeridoDaoEmMemoria()
        val uc = ConfirmarPrazoUC(dao, CalendarSyncRepositoryFake(syncResultado = Result.success("evt-1")))

        val resultado = uc.invoke(
            publicacaoId = 1L,
            prazo = prazo(),
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.GOOGLE,
        )

        assertEquals(
            ConfirmacaoPrazoResultado.EventoCriado(ProvedorCalendario.GOOGLE, "evt-1"),
            resultado.getOrNull(),
        )
        assertEquals("evt-1", dao.getByPublicacaoId(1L)?.idExternoCalendario)
    }

    @Test
    fun `sync com falha mantem confirmado e retorna SincronizacaoPendente`() = runTest {
        val dao = PrazoSugeridoDaoEmMemoria()
        val uc = ConfirmarPrazoUC(
            dao,
            CalendarSyncRepositoryFake(syncResultado = Result.failure(RuntimeException("rede"))),
        )

        val resultado = uc.invoke(
            publicacaoId = 1L,
            prazo = prazo(),
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.OUTLOOK,
        )

        assertEquals(
            ConfirmacaoPrazoResultado.SincronizacaoPendente(ProvedorCalendario.OUTLOOK),
            resultado.getOrNull(),
        )
        val entity = dao.getByPublicacaoId(1L)
        assertTrue(entity!!.isConfirmado)
        assertNull(entity.idExternoCalendario)
    }

    private fun prazo(): PublicacaoPrazo =
        PublicacaoPrazo(
            quantidade = 5,
            unidade = "dias",
            diasUteis = true,
            textoOriginal = "prazo de 5 dias",
            dataLimiteEstimada = LocalDate.of(2026, 7, 20),
        )
}

/** Fake com estado real, compartilhado pelos testes de UC de prazo. */
internal class PrazoSugeridoDaoEmMemoria : PrazoSugeridoDao {
    private val prazos = MutableStateFlow(emptyMap<Long, PrazoSugeridoEntity>())
    private var proximoId = 1L

    override suspend fun insert(prazoSugerido: PrazoSugeridoEntity): Long {
        val id = if (prazoSugerido.id != 0L) prazoSugerido.id else proximoId++
        // REPLACE pelo índice UNIQUE de publicacaoId
        prazos.value = prazos.value
            .filterValues { it.publicacaoId != prazoSugerido.publicacaoId }
            .plus(id to prazoSugerido.copy(id = id))
        return id
    }

    override suspend fun update(prazoSugerido: PrazoSugeridoEntity) {
        if (prazos.value.containsKey(prazoSugerido.id)) {
            prazos.value = prazos.value + (prazoSugerido.id to prazoSugerido)
        }
    }

    override suspend fun getByPublicacaoId(publicacaoId: Long): PrazoSugeridoEntity? =
        prazos.value.values.firstOrNull { it.publicacaoId == publicacaoId }

    override fun observeByPublicacaoId(publicacaoId: Long): Flow<PrazoSugeridoEntity?> =
        prazos.map { mapa -> mapa.values.firstOrNull { it.publicacaoId == publicacaoId } }

    override suspend fun getPrazosParaSincronizar(provedores: List<String>): List<PrazoSugeridoEntity> =
        prazos.value.values.filter {
            it.isConfirmado && it.idExternoCalendario == null && it.provedorCalendario in provedores
        }

    override fun observeAll(): Flow<List<PrazoSugeridoEntity>> =
        prazos.map { it.values.toList() }
}

/** Fake de sync com contadores de chamadas. */
internal class CalendarSyncRepositoryFake(
    private val syncResultado: Result<String> = Result.success("evento"),
    private val cancelResultado: Result<Unit> = Result.success(Unit),
) : CalendarSyncRepository {
    var syncChamadas: Int = 0
        private set
    var cancelamentos: MutableList<Pair<String, ProvedorCalendario>> = mutableListOf()
        private set

    override suspend fun syncPrazo(
        prazo: PublicacaoPrazo,
        title: String,
        description: String,
        provedor: ProvedorCalendario,
    ): Result<String> {
        syncChamadas++
        return syncResultado
    }

    override suspend fun cancelPrazo(
        idExterno: String,
        provedor: ProvedorCalendario,
    ): Result<Unit> {
        cancelamentos += idExterno to provedor
        return cancelResultado
    }
}
