package com.obiterjus.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.obiterjus.data.cliente.local.ClienteEntity
import com.obiterjus.data.cliente.local.ClienteProcessoEntity
import com.obiterjus.data.cliente.local.RepresentanteLegalEmbutido
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClienteDaoInstrumentedTest {
    private lateinit var database: ObiterDatabase

    private val agora = Instant.parse("2026-07-20T12:00:00Z")
    private val numeroProcesso = "50110879520258130245"

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ObiterDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun pessoaJuridicaPreservaRepresentanteLegal() = runBlocking {
        val dao = database.clienteDao()
        dao.upsert(
            clientePadrao(
                id = "cli-pj",
                tipoPessoa = "JURIDICA",
                nome = "Construtora Alfa Ltda",
                nomeNormalizado = "CONSTRUTORA ALFA LTDA",
                documento = "12.345.678/0001-90",
                documentoNormalizado = "12345678000190",
                representante = RepresentanteLegalEmbutido(
                    nome = "Joana Ribeiro",
                    documento = "111.222.333-44",
                    nacionalidade = "brasileira",
                    estadoCivil = "casada",
                    profissao = "administradora",
                    cargo = "sócia-administradora",
                ),
            ),
        )

        val salvo = requireNotNull(dao.buscarPorId("cli-pj"))
        val representante = requireNotNull(salvo.representante)
        assertEquals("Joana Ribeiro", representante.nome)
        assertEquals("sócia-administradora", representante.cargo)
        assertEquals("casada", representante.estadoCivil)
    }

    /**
     * O [androidx.room.Embedded] nulável só volta nulo quando TODAS as colunas
     * prefixadas estão nulas — é o que garante que a pessoa física não carregue
     * um representante vazio.
     */
    @Test
    fun pessoaFisicaVoltaSemRepresentante() = runBlocking {
        val dao = database.clienteDao()
        dao.upsert(
            clientePadrao(
                id = "cli-pf",
                tipoPessoa = "FISICA",
                nome = "Carlos Menezes",
                nomeNormalizado = "CARLOS MENEZES",
                documento = "555.666.777-88",
                documentoNormalizado = "55566677788",
                representante = null,
            ),
        )

        assertNull(requireNotNull(dao.buscarPorId("cli-pf")).representante)
    }

    @Test
    fun buscaPorDocumentoEncontraOMesmoCliente() = runBlocking {
        val dao = database.clienteDao()
        dao.upsert(
            clientePadrao(
                id = "cli-doc",
                tipoPessoa = "FISICA",
                nome = "Carlos Menezes",
                nomeNormalizado = "CARLOS MENEZES",
                documento = "555.666.777-88",
                documentoNormalizado = "55566677788",
            ),
        )

        assertEquals("cli-doc", dao.buscarPorDocumento("55566677788")?.id)
        assertNull(dao.buscarPorDocumento("00000000000"))
    }

    @Test
    fun excluirClienteRemoveOsVinculosEmCascata() = runBlocking {
        val dao = database.clienteDao()
        inserirProcesso()
        dao.upsert(
            clientePadrao(
                id = "cli-vinc",
                tipoPessoa = "FISICA",
                nome = "Carlos Menezes",
                nomeNormalizado = "CARLOS MENEZES",
            ),
        )
        dao.vincular(
            ClienteProcessoEntity(
                clienteId = "cli-vinc",
                numeroProcesso = numeroProcesso,
                participanteIdLocal = "part-1",
                vinculadoEm = agora,
            ),
        )

        assertEquals(listOf(numeroProcesso), dao.observarProcessosDoCliente("cli-vinc").first())
        assertEquals(1, dao.observarClientesDoProcesso(numeroProcesso).first().size)

        dao.excluir("cli-vinc")

        assertTrue(dao.observarVinculos().first().isEmpty())
    }

    private suspend fun inserirProcesso() {
        database.processoDao().upsert(
            ProcessoEntity(
                numeroProcesso = numeroProcesso,
                tribunal = "TJMG",
                grau = "G1",
                classeCodigo = 7,
                classeNome = "Procedimento Comum Civel",
                assuntosJson = null,
                orgaoJulgadorCodigo = 1,
                orgaoJulgadorNome = "1a Vara",
                nivelSigilo = 0,
                dataAjuizamento = agora,
                syncStatus = ProcessoSyncStatus.SYNCED,
                capturadoEm = agora,
                atualizadoEm = agora,
            ),
        )
    }

    private fun clientePadrao(
        id: String,
        tipoPessoa: String,
        nome: String,
        nomeNormalizado: String,
        documento: String? = null,
        documentoNormalizado: String? = null,
        representante: RepresentanteLegalEmbutido? = null,
    ) = ClienteEntity(
        id = id,
        tipoPessoa = tipoPessoa,
        nome = nome,
        nomeNormalizado = nomeNormalizado,
        documento = documento,
        documentoNormalizado = documentoNormalizado,
        representante = representante,
        criadoEm = agora,
        atualizadoEm = agora,
    )
}
