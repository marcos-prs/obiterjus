package com.obiterjus.domain.logic

import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.EnderecoCliente
import com.obiterjus.domain.model.RepresentanteLegal
import com.obiterjus.domain.model.TipoPessoa

/**
 * Monta a qualificação da parte no formato usado no preâmbulo de uma peça:
 *
 * PF — "FULANO, brasileiro, casado, engenheiro, inscrito no CPF sob o nº …,
 * residente e domiciliado na Rua X, nº 10, Centro, Belo Horizonte/MG, CEP …"
 *
 * PJ — "EMPRESA LTDA, pessoa jurídica de direito privado, inscrita no CNPJ sob
 * o nº …, com sede na …, neste ato representada por FULANA, brasileira, casada,
 * administradora, inscrita no CPF sob o nº …, na qualidade de sócia-administradora"
 *
 * Campos não preenchidos são omitidos em vez de virarem "não informado": o
 * texto precisa poder ir direto para a petição, e um buraco visível é melhor
 * que um placeholder que passa despercebido na revisão.
 */
object QualificacaoPorExtenso {

    fun montar(cliente: Cliente): String {
        val partes = buildList {
            add(cliente.nome.trim())
            if (cliente.tipoPessoa == TipoPessoa.JURIDICA) {
                add("pessoa jurídica de direito privado")
                documento(cliente.documento, TipoPessoa.JURIDICA)?.let(::add)
                cliente.endereco.montar()?.let { add("com sede na $it") }
                cliente.representante?.montar()?.let(::add)
            } else {
                cliente.nacionalidade?.limpo()?.let(::add)
                cliente.estadoCivil?.limpo()?.let(::add)
                cliente.profissao?.limpo()?.let(::add)
                documento(cliente.documento, TipoPessoa.FISICA)?.let(::add)
                cliente.endereco.montar()?.let { add("residente e domiciliado na $it") }
            }
        }
        return partes.joinToString(", ")
    }

    private fun RepresentanteLegal.montar(): String? {
        val nomeRep = nome?.limpo() ?: return null
        val qualificacao = listOfNotNull(
            nacionalidade?.limpo(),
            estadoCivil?.limpo(),
            profissao?.limpo(),
            documento(documento, TipoPessoa.FISICA),
            cargo?.limpo()?.let { "na qualidade de $it" },
        )
        return if (qualificacao.isEmpty()) {
            "neste ato representada por $nomeRep"
        } else {
            "neste ato representada por $nomeRep, ${qualificacao.joinToString(", ")}"
        }
    }

    /**
     * "pessoa jurídica" é feminino, então o CNPJ pede "inscrita". Na pessoa
     * física o gênero é desconhecido e fica no masculino, como é praxe.
     */
    private fun documento(valor: String?, tipo: TipoPessoa): String? {
        val documento = valor?.limpo() ?: return null
        return if (tipo == TipoPessoa.JURIDICA) {
            "inscrita no CNPJ sob o nº $documento"
        } else {
            "inscrito no CPF sob o nº $documento"
        }
    }

    private fun EnderecoCliente.montar(): String? {
        val logradouroComNumero = listOfNotNull(logradouro?.limpo(), numero?.limpo()?.let { "nº $it" })
            .joinToString(", ")
            .takeIf { it.isNotBlank() }
        val municipioComUf = listOfNotNull(municipio?.limpo(), uf?.limpo())
            .joinToString("/")
            .takeIf { it.isNotBlank() }

        return listOfNotNull(
            logradouroComNumero,
            complemento?.limpo(),
            bairro?.limpo(),
            municipioComUf,
            cep?.limpo()?.let { "CEP $it" },
        ).joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun String.limpo(): String? = trim().takeIf { it.isNotEmpty() }
}
