package com.obiterjus.domain.usecase

import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.data.time.RespostaSaude
import com.obiterjus.domain.model.ConfiancaCalculo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecalcularPrazosPendentesUCTest {

    private val hoje = LocalDate.of(2026, 6, 24)
    private val clock = Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), ZoneOffset.UTC)
    private lateinit var calendario: FakeCalendarioForense

    @Test
    fun pendenteViraConfiavelEPersiste() = runBlocking {
        val dao = FakePublicacaoDao(listOf(publicacao(id = 1L)))
        val uc = useCase(dao, RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-07-15"))

        val resumo = uc()

        assertEquals(1, resumo.analisadas)
        assertEquals(1, resumo.resolvidas)
        assertEquals(0, resumo.falhas)

        val gravado = dao.atualizacoes.single()
        assertEquals(1L, gravado.id)
        assertEquals(LocalDate.of(2026, 7, 15), gravado.dataLimite)
        assertEquals(ConfiancaCalculo.CONFIAVEL.name, gravado.confianca)
        assertEquals(5, gravado.quantidade)
    }

    @Test
    fun apiForaDoArNaoGravaEDeixaParaOProximoCiclo() = runBlocking {
        val dao = FakePublicacaoDao(listOf(publicacao(id = 1L)))
        val uc = useCase(dao, RespostaPrazo(estado = "INDISPONIVEL"))

        val resumo = uc()

        assertEquals(1, resumo.analisadas)
        assertEquals(0, resumo.resolvidas)
        assertEquals(1, resumo.pendentes)
        // Gravar quantidade sem vencimento tiraria a publicação da fila sem
        // resolver nada — ela precisa continuar elegível.
        assertTrue(dao.atualizacoes.isEmpty())
    }

    @Test
    fun umaFalhaNaoAbortaAVarredura() = runBlocking {
        val dao = FakePublicacaoDao(listOf(publicacao(id = 1L), publicacao(id = 2L)))
        val uc = useCase(
            dao,
            RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-07-15"),
            falharNaPublicacao = 1L,
        )

        val resumo = uc()

        assertEquals(2, resumo.analisadas)
        assertEquals(1, resumo.resolvidas)
        assertEquals(1, resumo.falhas)
        assertEquals(listOf(2L), dao.atualizacoes.map { it.id })
    }

    @Test
    fun tribunalSemCalendarioJudicialNaoEhReperguntadoNoMesmoCiclo() = runBlocking {
        val dao = FakePublicacaoDao(listOf(publicacao(id = 1L), publicacao(id = 2L)))
        val uc = useCase(dao, RespostaPrazo(estado = "BLOQUEADO_SEM_CALENDARIO"))

        val resumo = uc()

        // As duas seguem pendentes — sem data não se grava —, mas a API foi
        // chamada uma vez: a resposta da primeira já vale para o mesmo tribunal
        // e ano. Insistir nas outras 199 não resolveria nenhuma.
        assertEquals(2, resumo.analisadas)
        assertEquals(0, resumo.resolvidas)
        assertEquals(2, resumo.pendentes)
        assertEquals(1, calendario.chamadas)
        assertTrue(dao.atualizacoes.isEmpty())
    }

    @Test
    fun consultaRespeitaJanelaELimite() = runBlocking {
        val dao = FakePublicacaoDao(emptyList())
        val uc = useCase(dao, RespostaPrazo(estado = "CONFIAVEL", dataVencimento = "2026-07-15"))

        uc(janelaDias = 90L, limite = 200)

        assertEquals(hoje.minusDays(90), dao.dataMinimaConsultada)
        assertEquals(200, dao.limiteConsultado)
    }

    private fun useCase(
        dao: FakePublicacaoDao,
        resposta: RespostaPrazo,
        falharNaPublicacao: Long? = null,
    ): RecalcularPrazosPendentesUC {
        calendario = FakeCalendarioForense(resposta)
        val calculadora = CalculadoraPrazos(calendario)
        return RecalcularPrazosPendentesUC(
            publicacaoDao = dao.apply { this.falharNaPublicacao = falharNaPublicacao },
            calcularPrazoRegraUC = CalcularPrazoRegraUC(
                calculadora,
                DjenPrazoExtractor(calculadora),
            ),
            resolverNatureza = null,
            clock = clock,
        )
    }

    private fun publicacao(id: Long): PublicacaoEntity =
        PublicacaoEntity(
            id = id,
            hash = "hash-$id",
            numeroProcesso = "50110879520258130245",
            participantesJson = null,
            prazoQuantidade = null,
            prazoUnidade = null,
            prazoDiasUteis = true,
            prazoTexto = null,
            prazoDataLimite = null,
            dataDisponibilizacao = LocalDate.of(2026, 6, 23),
            tribunal = "TJMG",
            tipoComunicacao = "Intimacao",
            nomeOrgao = "1a Vara",
            idOrgao = null,
            textoRaw = null,
            textoLimpo = "Fica a parte intimada a se manifestar no prazo de 5 dias.",
            textoPossuiHtml = false,
            textoPossuiErroTemplate = false,
            isSigiloso = false,
            ativo = true,
            fonte = "DJEN",
            capturadoEm = Instant.parse("2026-06-23T12:00:00Z"),
            atualizadoEm = Instant.parse("2026-06-23T12:00:00Z"),
        )

    private class Atualizacao(
        val id: Long,
        val quantidade: Int,
        val dataLimite: LocalDate,
        val confianca: String,
    )

    private class FakePublicacaoDao(
        private val pendentes: List<PublicacaoEntity>,
    ) : PublicacaoDao {
        val atualizacoes = mutableListOf<Atualizacao>()
        var dataMinimaConsultada: LocalDate? = null
        var limiteConsultado: Int? = null
        var falharNaPublicacao: Long? = null

        override suspend fun getComPrazoPendente(
            dataMinima: LocalDate,
            limite: Int,
        ): List<PublicacaoEntity> {
            dataMinimaConsultada = dataMinima
            limiteConsultado = limite
            return pendentes
        }

        override suspend fun atualizarPrazo(
            id: Long,
            quantidade: Int,
            unidade: String,
            diasUteis: Boolean,
            texto: String,
            dataLimite: LocalDate,
            confianca: String,
        ) {
            if (id == falharNaPublicacao) error("falha simulada de escrita")
            atualizacoes += Atualizacao(id, quantidade, dataLimite, confianca)
        }

        override suspend fun upsert(publicacao: PublicacaoEntity) = Unit
        override suspend fun upsertAll(publicacoes: List<PublicacaoEntity>) = Unit
        override suspend fun getExistingIds(ids: List<Long>): List<Long> = emptyList()
        override suspend fun getByHashes(hashes: List<String>): List<PublicacaoEntity> = emptyList()
        override suspend fun getById(id: Long): PublicacaoEntity? = null
        override fun observeById(id: Long): Flow<PublicacaoEntity?> = throw NotImplementedError()
        override suspend fun getByIds(ids: List<Long>): List<PublicacaoEntity> = emptyList()
        override fun observePublicacoes(
            numeroProcesso: String?,
            tribunal: String?,
            tipoComunicacao: String?,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
            somenteSigilosas: Boolean?,
        ): Flow<List<PublicacaoEntity>> = throw NotImplementedError()
        override fun observePorProcesso(numeroProcesso: String): Flow<List<PublicacaoEntity>> =
            throw NotImplementedError()
        override suspend fun getNumerosProcessoDistintos(): List<String> = emptyList()
        override suspend fun getByDataDisponibilizacao(data: LocalDate): List<PublicacaoEntity> =
            emptyList()
        override suspend fun atualizarStatusDuplicata(
            id: Long,
            duplicataDe: Long?,
            totalDuplicatas: Int,
        ) = Unit
    }

    private class FakeCalendarioForense(
        private val resposta: RespostaPrazo,
    ) : CalendarioForenseDataSource {
        var chamadas = 0
            private set

        override suspend fun calcularPrazo(pedido: PedidoCalculoPrazo): RespostaPrazo {
            chamadas += 1
            return resposta
        }

        // Sem cobertura declarada a calculadora não rebaixa nada: estes
        // testes olham só o mapeamento do estado da resposta.
        override suspend fun consultarSaude(ano: Int): RespostaSaude = RespostaSaude()
    }
}
