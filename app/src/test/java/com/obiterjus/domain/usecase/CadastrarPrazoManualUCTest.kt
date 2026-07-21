package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ProvedorCalendario
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CadastrarPrazoManualUCTest {

    private val dataLimite = LocalDate.of(2026, 8, 12)

    @Test
    fun `grava campos de prazo na publicacao com texto de origem manual`() = runTest {
        val publicacaoDao = PublicacaoDaoFake()
        val uc = criarUC(publicacaoDao = publicacaoDao)

        uc.invoke(
            publicacaoId = 7L,
            quantidade = 15,
            diasUteis = true,
            dataLimite = dataLimite,
            confianca = ConfiancaCalculo.CONFIAVEL,
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.LOCAL,
        )

        val chamada = publicacaoDao.atualizacoesPrazo.single()
        assertEquals(7L, chamada.id)
        assertEquals(15, chamada.quantidade)
        assertTrue(chamada.diasUteis)
        assertEquals(CadastrarPrazoManualUC.TEXTO_PRAZO_MANUAL, chamada.texto)
        assertEquals(dataLimite, chamada.dataLimite)
        assertEquals("CONFIAVEL", chamada.confianca)
    }

    @Test
    fun `substitui linha anterior de prazos_sugeridos`() = runTest {
        val prazoSugeridoDao = PrazoSugeridoDaoEmMemoria()
        prazoSugeridoDao.insert(
            PrazoSugeridoEntity(
                publicacaoId = 7L,
                quantidade = 5,
                unidade = "dias",
                diasUteis = false,
                textoOriginal = "prazo automatico",
                dataLimite = LocalDate.of(2026, 7, 1),
                isConfirmado = true,
            ),
        )
        val uc = criarUC(prazoSugeridoDao = prazoSugeridoDao)

        uc.invoke(
            publicacaoId = 7L,
            quantidade = 30,
            diasUteis = false,
            dataLimite = dataLimite,
            confianca = ConfiancaCalculo.CONFIAVEL,
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.LOCAL,
        )

        val entity = prazoSugeridoDao.getByPublicacaoId(7L)!!
        assertEquals(30, entity.quantidade)
        assertFalse(entity.diasUteis)
        assertEquals(CadastrarPrazoManualUC.TEXTO_PRAZO_MANUAL, entity.textoOriginal)
        assertEquals(dataLimite, entity.dataLimite)
        assertNull(entity.idExternoCalendario)
        assertTrue(entity.isConfirmado) // confirmado pelo encadeamento
    }

    @Test
    fun `cancela evento externo do prazo anterior`() = runTest {
        val prazoSugeridoDao = PrazoSugeridoDaoEmMemoria()
        prazoSugeridoDao.insert(
            PrazoSugeridoEntity(
                publicacaoId = 7L,
                quantidade = 5,
                unidade = "dias",
                diasUteis = true,
                textoOriginal = "prazo automatico",
                dataLimite = LocalDate.of(2026, 7, 1),
                isConfirmado = true,
                idExternoCalendario = "evt-antigo",
                provedorCalendario = ProvedorCalendario.GOOGLE.codigo,
            ),
        )
        val sync = CalendarSyncRepositoryFake()
        val uc = criarUC(prazoSugeridoDao = prazoSugeridoDao, sync = sync)

        uc.invoke(
            publicacaoId = 7L,
            quantidade = 10,
            diasUteis = true,
            dataLimite = dataLimite,
            confianca = ConfiancaCalculo.CONFIAVEL,
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.LOCAL,
        )

        assertEquals(listOf("evt-antigo" to ProvedorCalendario.GOOGLE), sync.cancelamentos)
    }

    @Test
    fun `falha no cancelamento nao impede o cadastro`() = runTest {
        val prazoSugeridoDao = PrazoSugeridoDaoEmMemoria()
        prazoSugeridoDao.insert(
            PrazoSugeridoEntity(
                publicacaoId = 7L,
                quantidade = 5,
                unidade = "dias",
                diasUteis = true,
                textoOriginal = "prazo automatico",
                dataLimite = LocalDate.of(2026, 7, 1),
                isConfirmado = true,
                idExternoCalendario = "evt-antigo",
                provedorCalendario = ProvedorCalendario.OUTLOOK.codigo,
            ),
        )
        val sync = CalendarSyncRepositoryFake(
            cancelResultado = Result.failure(RuntimeException("rede")),
        )
        val uc = criarUC(prazoSugeridoDao = prazoSugeridoDao, sync = sync)

        val resultado = uc.invoke(
            publicacaoId = 7L,
            quantidade = 10,
            diasUteis = true,
            dataLimite = dataLimite,
            confianca = ConfiancaCalculo.CONFIAVEL,
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.LOCAL,
        )

        assertEquals(
            ConfirmacaoPrazoResultado.ConfirmadoLocalmente,
            resultado.getOrNull(),
        )
    }

    @Test
    fun `provedor google com sync ok retorna EventoCriado`() = runTest {
        val sync = CalendarSyncRepositoryFake(syncResultado = Result.success("evt-novo"))
        val uc = criarUC(sync = sync)

        val resultado = uc.invoke(
            publicacaoId = 7L,
            quantidade = 10,
            diasUteis = true,
            dataLimite = dataLimite,
            confianca = ConfiancaCalculo.INCERTO,
            title = "Prazo",
            description = "Descricao",
            provedor = ProvedorCalendario.GOOGLE,
        )

        assertEquals(
            ConfirmacaoPrazoResultado.EventoCriado(ProvedorCalendario.GOOGLE, "evt-novo"),
            resultado.getOrNull(),
        )
    }

    private fun criarUC(
        publicacaoDao: PublicacaoDaoFake = PublicacaoDaoFake(),
        prazoSugeridoDao: PrazoSugeridoDaoEmMemoria = PrazoSugeridoDaoEmMemoria(),
        sync: CalendarSyncRepositoryFake = CalendarSyncRepositoryFake(),
    ): CadastrarPrazoManualUC =
        CadastrarPrazoManualUC(
            publicacaoDao = publicacaoDao,
            prazoSugeridoDao = prazoSugeridoDao,
            calendarSyncRepository = sync,
            confirmarPrazoUC = ConfirmarPrazoUC(prazoSugeridoDao, sync),
        )

    private class PublicacaoDaoFake : PublicacaoDao {
        data class AtualizacaoPrazo(
            val id: Long,
            val quantidade: Int,
            val unidade: String,
            val diasUteis: Boolean,
            val texto: String,
            val dataLimite: LocalDate,
            val confianca: String,
        )

        val atualizacoesPrazo = mutableListOf<AtualizacaoPrazo>()

        override suspend fun atualizarPrazo(
            id: Long,
            quantidade: Int,
            unidade: String,
            diasUteis: Boolean,
            texto: String,
            dataLimite: LocalDate,
            confianca: String,
        ) {
            atualizacoesPrazo += AtualizacaoPrazo(
                id, quantidade, unidade, diasUteis, texto, dataLimite, confianca,
            )
        }

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
}
