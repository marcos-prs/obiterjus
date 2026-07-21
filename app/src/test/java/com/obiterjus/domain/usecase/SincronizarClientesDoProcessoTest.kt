package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.TipoPessoa
import com.obiterjus.domain.model.VinculoClienteProcesso
import com.obiterjus.domain.repository.ClientesRepository
import com.obiterjus.domain.repository.FakeClientesRepository
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SincronizarClientesDoProcessoTest {

    private val agora = Instant.parse("2026-07-20T12:00:00Z")
    private val numeroProcesso = "50110879520258130245"

    @Test
    fun criaClienteEVinculaQuandoAParteNaoEstaNaCarteira() = runTest {
        val repositorio = FakeClientesRepository()
        val participante = participante(idLocal = "p1", nome = "Carlos Menezes", cpfCnpj = "555.666.777-88")

        sincronizar(repositorio)(
            numeroProcesso,
            listOf(participante),
            mapOf("p1" to DecisaoCliente.CriarNovo),
        )

        assertEquals(1, repositorio.clientes.size)
        val criado = repositorio.clientes.values.single()
        assertEquals("Carlos Menezes", criado.nome)
        assertEquals("555.666.777-88", criado.documento)
        assertEquals(listOf("p1"), repositorio.vinculos.map { it.participanteIdLocal })
        assertEquals(criado.id, repositorio.vinculos.single().clienteId)
    }

    @Test
    fun vincularExistenteNaoCriaClienteNovo() = runTest {
        val repositorio = FakeClientesRepository()
        val existente = cliente(id = "cli-1", nome = "Construtora Alfa", documento = "12.345.678/0001-90")
        repositorio.semear(existente)

        sincronizar(repositorio)(
            numeroProcesso,
            listOf(participante(idLocal = "p1", nome = "CONSTRUTORA ALFA LTDA")),
            mapOf("p1" to DecisaoCliente.Vincular("cli-1")),
        )

        assertEquals(1, repositorio.clientes.size)
        assertEquals("cli-1", repositorio.vinculos.single().clienteId)
    }

    /**
     * O documento tem índice único: se "criar novo" ignorasse um cadastro com o
     * mesmo CPF, a gravação estouraria por violação de constraint.
     */
    @Test
    fun criarNovoReaproveitaCadastroComOMesmoDocumento() = runTest {
        val repositorio = FakeClientesRepository()
        val existente = cliente(id = "cli-1", nome = "Carlos Menezes", documento = "55566677788")
        repositorio.semear(existente)

        sincronizar(repositorio)(
            numeroProcesso,
            listOf(participante(idLocal = "p1", nome = "C. Menezes", cpfCnpj = "555.666.777-88")),
            mapOf("p1" to DecisaoCliente.CriarNovo),
        )

        assertEquals(1, repositorio.clientes.size)
        assertEquals("cli-1", repositorio.vinculos.single().clienteId)
    }

    @Test
    fun desmarcarAParteRemoveOVinculo() = runTest {
        val repositorio = FakeClientesRepository()
        repositorio.semear(cliente(id = "cli-1", nome = "Carlos Menezes"))
        repositorio.semearVinculo(VinculoClienteProcesso("cli-1", numeroProcesso, "p1"))

        sincronizar(repositorio)(
            numeroProcesso,
            listOf(participante(idLocal = "p1", nome = "Carlos Menezes", ehCliente = false)),
            emptyMap(),
        )

        assertTrue(repositorio.vinculos.isEmpty())
    }

    /**
     * Vínculo sem participante de origem foi feito por outro caminho; desmarcar
     * uma parte não pode desfazê-lo.
     */
    @Test
    fun vinculoManualSobreviveAReconciliacao() = runTest {
        val repositorio = FakeClientesRepository()
        repositorio.semear(cliente(id = "cli-manual", nome = "Joana Ribeiro"))
        repositorio.semearVinculo(VinculoClienteProcesso("cli-manual", numeroProcesso, participanteIdLocal = null))

        sincronizar(repositorio)(numeroProcesso, emptyList(), emptyMap())

        assertEquals(listOf("cli-manual"), repositorio.vinculos.map { it.clienteId })
    }

    @Test
    fun parteSemNomeNaoViraCliente() = runTest {
        val repositorio = FakeClientesRepository()

        sincronizar(repositorio)(
            numeroProcesso,
            listOf(participante(idLocal = "p1", nome = "  ")),
            mapOf("p1" to DecisaoCliente.CriarNovo),
        )

        assertTrue(repositorio.clientes.isEmpty())
        assertTrue(repositorio.vinculos.isEmpty())
    }

    @Test
    fun pessoaJuridicaEhDetectadaPeloTipoDaParte() = runTest {
        val repositorio = FakeClientesRepository()

        sincronizar(repositorio)(
            numeroProcesso,
            listOf(participante(idLocal = "p1", nome = "Construtora Alfa Ltda", tipoPessoa = "J")),
            mapOf("p1" to DecisaoCliente.CriarNovo),
        )

        assertEquals(TipoPessoa.JURIDICA, repositorio.clientes.values.single().tipoPessoa)
    }

    // --- Apoio ---

    private fun sincronizar(repositorio: ClientesRepository) =
        SincronizarClientesDoProcesso(repositorio) { agora }

    private fun participante(
        idLocal: String,
        nome: String?,
        cpfCnpj: String? = null,
        tipoPessoa: String? = null,
        ehCliente: Boolean = true,
    ) = ParticipanteProcesso(
        idLocal = idLocal,
        numeroProcesso = numeroProcesso,
        polo = "ATIVO",
        nome = nome,
        tipoPessoa = tipoPessoa,
        tipoParticipacao = "Autor",
        ehCliente = ehCliente,
        cpfCnpj = cpfCnpj,
    )

    private fun cliente(
        id: String,
        nome: String,
        documento: String? = null,
    ) = Cliente(
        id = id,
        tipoPessoa = TipoPessoa.FISICA,
        nome = nome,
        documento = documento,
        criadoEm = agora,
        atualizadoEm = agora,
    )
}
