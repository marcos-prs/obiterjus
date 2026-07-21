package com.obiterjus.domain.logic

import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.EnderecoCliente
import com.obiterjus.domain.model.RepresentanteLegal
import com.obiterjus.domain.model.TipoPessoa
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class QualificacaoPorExtensoTest {

    private val agora = Instant.parse("2026-07-20T12:00:00Z")

    @Test
    fun pessoaFisicaCompletaSaiNaOrdemDoPreambulo() {
        val texto = QualificacaoPorExtenso.montar(
            cliente(
                nome = "Carlos Menezes",
                documento = "555.666.777-88",
                nacionalidade = "brasileiro",
                estadoCivil = "casado",
                profissao = "engenheiro",
                endereco = EnderecoCliente(
                    cep = "31000-000",
                    logradouro = "Rua das Acácias",
                    numero = "10",
                    bairro = "Centro",
                    municipio = "Belo Horizonte",
                    uf = "MG",
                ),
            ),
        )

        assertEquals(
            "Carlos Menezes, brasileiro, casado, engenheiro, " +
                "inscrito no CPF sob o nº 555.666.777-88, " +
                "residente e domiciliado na Rua das Acácias, nº 10, Centro, " +
                "Belo Horizonte/MG, CEP 31000-000",
            texto,
        )
    }

    /** "pessoa jurídica" é feminino: o CNPJ pede "inscrita", não "inscrito". */
    @Test
    fun pessoaJuridicaConcordaNoFeminino() {
        val texto = QualificacaoPorExtenso.montar(
            cliente(
                nome = "Construtora Alfa Ltda",
                tipoPessoa = TipoPessoa.JURIDICA,
                documento = "12.345.678/0001-90",
            ),
        )

        assertEquals(
            "Construtora Alfa Ltda, pessoa jurídica de direito privado, " +
                "inscrita no CNPJ sob o nº 12.345.678/0001-90",
            texto,
        )
    }

    @Test
    fun pessoaJuridicaIncluiRepresentanteQualificado() {
        val texto = QualificacaoPorExtenso.montar(
            cliente(
                nome = "Construtora Alfa Ltda",
                tipoPessoa = TipoPessoa.JURIDICA,
                documento = "12345678000190",
                representante = RepresentanteLegal(
                    nome = "Joana Ribeiro",
                    documento = "111.222.333-44",
                    nacionalidade = "brasileira",
                    estadoCivil = "casada",
                    profissao = "administradora",
                    cargo = "sócia-administradora",
                ),
            ),
        )

        assertEquals(
            "Construtora Alfa Ltda, pessoa jurídica de direito privado, " +
                "inscrita no CNPJ sob o nº 12345678000190, " +
                "neste ato representada por Joana Ribeiro, brasileira, casada, " +
                "administradora, inscrito no CPF sob o nº 111.222.333-44, " +
                "na qualidade de sócia-administradora",
            texto,
        )
    }

    /**
     * Campo vazio some do texto em vez de virar "não informado": a string vai
     * direto para a peça, e um buraco visível é revisável — um placeholder passa.
     */
    @Test
    fun camposVaziosSaoOmitidos() {
        val texto = QualificacaoPorExtenso.montar(
            cliente(nome = "Carlos Menezes", estadoCivil = "solteiro"),
        )

        assertEquals("Carlos Menezes, solteiro", texto)
    }

    @Test
    fun enderecoParcialNaoDeixaVirgulaSolta() {
        val texto = QualificacaoPorExtenso.montar(
            cliente(
                nome = "Carlos Menezes",
                endereco = EnderecoCliente(municipio = "Belo Horizonte", uf = "MG"),
            ),
        )

        assertEquals("Carlos Menezes, residente e domiciliado na Belo Horizonte/MG", texto)
    }

    @Test
    fun representanteSemQualificacaoSaiApenasComONome() {
        val texto = QualificacaoPorExtenso.montar(
            cliente(
                nome = "Alfa Ltda",
                tipoPessoa = TipoPessoa.JURIDICA,
                representante = RepresentanteLegal(nome = "Joana Ribeiro"),
            ),
        )

        assertEquals(
            "Alfa Ltda, pessoa jurídica de direito privado, " +
                "neste ato representada por Joana Ribeiro",
            texto,
        )
    }

    private fun cliente(
        nome: String,
        tipoPessoa: TipoPessoa = TipoPessoa.FISICA,
        documento: String? = null,
        nacionalidade: String? = null,
        estadoCivil: String? = null,
        profissao: String? = null,
        endereco: EnderecoCliente = EnderecoCliente(),
        representante: RepresentanteLegal? = null,
    ) = Cliente(
        id = "cli-1",
        tipoPessoa = tipoPessoa,
        nome = nome,
        documento = documento,
        nacionalidade = nacionalidade,
        estadoCivil = estadoCivil,
        profissao = profissao,
        endereco = endereco,
        representante = representante,
        criadoEm = agora,
        atualizadoEm = agora,
    )
}
