package com.obiterjus.data.djen

import com.obiterjus.data.datajud.local.MovimentoDao
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoDao
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoParticipante
import com.obiterjus.domain.repository.PublicacoesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class DjenPartesResolverTest {
    @Test
    fun semeiaProcessoComTribunalEOrgaoDaPublicacao() = runBlocking {
        val processoDao = FakeProcessoDao()
        val resolver = DjenPartesResolver(
            publicacoesRepository = FakePublicacoesRepository(
                publicacoesPorProcesso = mapOf(
                    "50106871820248130245" to listOf(
                        Publicacao(
                            id = 1L,
                            hash = null,
                            numeroProcesso = "50106871820248130245",
                            participantes = emptyList(),
                            prazo = null,
                            dataDisponibilizacao = null,
                            tribunal = "TJMG",
                            tipoComunicacao = "Intimacao",
                            nomeOrgao = "2ª Vara Cível",
                            textoLimpo = null,
                            isSigiloso = false,
                            fonte = "DJEN",
                            capturadoEm = Instant.EPOCH,
                            atualizadoEm = Instant.EPOCH,
                        ),
                    ),
                ),
            ),
            localProcessoRepository = LocalProcessoRepository(
                processoDao = processoDao,
                movimentoDao = FakeMovimentoDao(),
                participanteDao = FakeParticipanteDao(),
            ),
        )

        val resumo = resolver.atualizarPartesDosProcessos(listOf("50106871820248130245"))

        assertEquals(1, resumo.processosSemeados)
        val processo = processoDao.savedProcessos.getValue("50106871820248130245")
        assertEquals("TJMG", processo.tribunal)
        assertEquals("2ª Vara Cível", processo.orgaoJulgadorNome)
        assertEquals(ProcessoSyncStatus.PENDING, processo.syncStatus)

        // Segunda passada não altera nada
        val resumoSegundaVez = resolver.atualizarPartesDosProcessos(listOf("50106871820248130245"))
        assertEquals(0, resumoSegundaVez.processosSemeados)
    }

    @Test
    fun semeiaProcessoMesmoSemTribunalNemOrgao() = runBlocking {
        val processoDao = FakeProcessoDao()
        val resolver = DjenPartesResolver(
            publicacoesRepository = FakePublicacoesRepository(
                publicacoesPorProcesso = mapOf(
                    "50106871820248130245" to listOf(
                        Publicacao(
                            id = 1L,
                            hash = null,
                            numeroProcesso = "50106871820248130245",
                            participantes = emptyList(),
                            prazo = null,
                            dataDisponibilizacao = null,
                            tribunal = null,
                            tipoComunicacao = "Intimacao",
                            nomeOrgao = null,
                            textoLimpo = null,
                            isSigiloso = false,
                            fonte = "DJEN",
                            capturadoEm = Instant.EPOCH,
                            atualizadoEm = Instant.EPOCH,
                        ),
                    ),
                ),
            ),
            localProcessoRepository = LocalProcessoRepository(
                processoDao = processoDao,
                movimentoDao = FakeMovimentoDao(),
                participanteDao = FakeParticipanteDao(),
            ),
        )

        val resumo = resolver.atualizarPartesDosProcessos(listOf("50106871820248130245"))

        assertEquals(1, resumo.processosSemeados)
        val processo = processoDao.savedProcessos.getValue("50106871820248130245")
        assertEquals(null, processo.tribunal)
        assertEquals(ProcessoSyncStatus.PENDING, processo.syncStatus)
    }

    @Test
    fun resolvesPoloLabelsAndAvoidsDuplicates() = runBlocking {
        val participanteDao = FakeParticipanteDao()
        val resolver = DjenPartesResolver(
            publicacoesRepository = FakePublicacoesRepository(
                publicacoesPorProcesso = mapOf(
                    "50106871820248130245" to listOf(
                        Publicacao(
                            id = 1L,
                            hash = null,
                            numeroProcesso = "50106871820248130245",
                            participantes = listOf(
                                PublicacaoParticipante(tipo = "POLO ATIVO", nome = "Antonio Araujo"),
                                PublicacaoParticipante(tipo = "POLO PASSIVO", nome = "Banco Exemplo S.A."),
                                PublicacaoParticipante(tipo = "ADVOGADO", nome = "Marcos Paulo Rocha de Souza"),
                            ),
                            prazo = null,
                            dataDisponibilizacao = null,
                            tribunal = "TJMG",
                            tipoComunicacao = "Intimacao",
                            nomeOrgao = "2ª Vara Cível",
                            textoLimpo = null,
                            isSigiloso = false,
                            fonte = "DJEN",
                            capturadoEm = Instant.EPOCH,
                            atualizadoEm = Instant.EPOCH,
                        ),
                    ),
                ),
            ),
            localProcessoRepository = LocalProcessoRepository(
                processoDao = FakeProcessoDao(),
                movimentoDao = FakeMovimentoDao(),
                participanteDao = participanteDao,
            ),
        )

        val resumo = resolver.atualizarPartesDosProcessos(
            listOf("5010687-18.2024.8.13.0245"),
        )

        assertEquals(1, resumo.processosAtualizados)
        assertEquals(3, resumo.participantesInseridos)

        val participantes = participanteDao.saved["50106871820248130245"].orEmpty()
        assertEquals(3, participantes.size)
        assertEquals("ATIVO", participantes.first { it.nome == "Antonio Araujo" }.polo)
        assertEquals("PASSIVO", participantes.first { it.nome == "Banco Exemplo S.A." }.polo)

        val resumoSegundaVez = resolver.atualizarPartesDosProcessos(
            listOf("50106871820248130245"),
        )

        assertEquals(0, resumoSegundaVez.processosAtualizados)
        assertEquals(0, resumoSegundaVez.participantesInseridos)
        assertEquals(3, participanteDao.saved["50106871820248130245"].orEmpty().size)
    }

    private class FakePublicacoesRepository(
        private val publicacoesPorProcesso: Map<String, List<Publicacao>>,
    ) : PublicacoesRepository {
        override fun observarPublicacoes(): Flow<List<Publicacao>> = emptyFlow()

        override fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>> =
            flowOf(publicacoesPorProcesso[numeroProcesso].orEmpty())

        override fun observarPublicacao(id: Long): Flow<Publicacao?> = emptyFlow()
    }

    private class FakeProcessoDao : ProcessoDao {
        val savedProcessos = mutableMapOf<String, ProcessoEntity>()

        override suspend fun upsert(processo: ProcessoEntity) {
            savedProcessos[processo.numeroProcesso] = processo
        }

        override suspend fun upsertAll(processos: List<ProcessoEntity>) {
            processos.forEach { savedProcessos[it.numeroProcesso] = it }
        }

        override fun observeAll(): Flow<List<ProcessoEntity>> = emptyFlow()
        override fun observeByNumero(numeroProcesso: String): Flow<ProcessoEntity?> = emptyFlow()
        override suspend fun getByNumero(numeroProcesso: String): ProcessoEntity? = savedProcessos[numeroProcesso]
        override suspend fun getByNumeros(numerosProcesso: List<String>): List<ProcessoEntity> =
            numerosProcesso.mapNotNull(savedProcessos::get)
        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) {
            savedProcessos.remove(numeroProcesso)
        }
    }

    private class FakeMovimentoDao : MovimentoDao() {
        override suspend fun upsertAll(movimentos: List<MovimentoEntity>) = Unit
        override fun observeByProcesso(numeroProcesso: String): Flow<List<MovimentoEntity>> = emptyFlow()
        override suspend fun getByProcesso(numeroProcesso: String): List<MovimentoEntity> = emptyList()
        override suspend fun getByIds(ids: List<String>): List<MovimentoEntity> = emptyList()
        override suspend fun replaceForProcesso(numeroProcesso: String, movimentos: List<MovimentoEntity>) = Unit
        override suspend fun deleteByProcesso(numeroProcesso: String) = Unit
    }

    private class FakeParticipanteDao : ParticipanteDao {
        val saved = mutableMapOf<String, MutableList<ParticipanteEntity>>()

        override suspend fun upsertAll(participantes: List<ParticipanteEntity>) {
            participantes.groupBy { it.numeroProcesso }.forEach { (numeroProcesso, itens) ->
                saved.getOrPut(numeroProcesso) { mutableListOf() }.apply {
                    addAll(itens)
                }
            }
        }

        override fun observeByNumeroProcesso(numeroProcesso: String): Flow<List<ParticipanteEntity>> = emptyFlow()
        override fun observeAll(): Flow<List<ParticipanteEntity>> = emptyFlow()
        override suspend fun getByNumeroProcesso(numeroProcesso: String): List<ParticipanteEntity> =
            saved[numeroProcesso].orEmpty()
        override suspend fun getAll(): List<ParticipanteEntity> = emptyList()
        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) {
            saved.remove(numeroProcesso)
        }
    }
}
