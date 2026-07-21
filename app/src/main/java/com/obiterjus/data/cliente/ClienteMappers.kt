package com.obiterjus.data.cliente

import com.obiterjus.data.cliente.local.ClienteEntity
import com.obiterjus.data.cliente.local.RepresentanteLegalEmbutido
import com.obiterjus.data.processo.ProcessoDadosResolver
import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.EnderecoCliente
import com.obiterjus.domain.model.RepresentanteLegal
import com.obiterjus.domain.model.TipoPessoa

/** Somente os dígitos de um CPF/CNPJ; null quando não sobra nenhum. */
fun normalizarDocumento(documento: String?): String? =
    documento?.filter(Char::isDigit)?.takeIf { it.isNotEmpty() }

fun ClienteEntity.toDomain(): Cliente = Cliente(
    id = id,
    // Uma linha com tipo inválido não deve derrubar a lista inteira: o padrão
    // é pessoa física, que é a esmagadora maioria.
    tipoPessoa = runCatching { TipoPessoa.valueOf(tipoPessoa) }.getOrDefault(TipoPessoa.FISICA),
    nome = nome,
    documento = documento,
    nacionalidade = nacionalidade,
    estadoCivil = estadoCivil,
    profissao = profissao,
    endereco = EnderecoCliente(
        cep = cep,
        logradouro = logradouro,
        numero = numeroEndereco,
        complemento = complemento,
        bairro = bairro,
        municipio = municipio,
        uf = uf,
    ),
    telefone = telefone,
    email = email,
    observacoes = observacoes,
    representante = representante?.toDomain(),
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
)

fun Cliente.toEntity(): ClienteEntity = ClienteEntity(
    id = id,
    tipoPessoa = tipoPessoa.name,
    nome = nome,
    // Mesma normalização usada para reconciliar participantes entre fontes —
    // se divergir, o dedupe deixa de casar com a parte marcada no processo.
    nomeNormalizado = ProcessoDadosResolver.normalizarNome(nome),
    documento = documento,
    documentoNormalizado = normalizarDocumento(documento),
    nacionalidade = nacionalidade,
    estadoCivil = estadoCivil,
    profissao = profissao,
    cep = endereco.cep,
    logradouro = endereco.logradouro,
    numeroEndereco = endereco.numero,
    complemento = endereco.complemento,
    bairro = endereco.bairro,
    municipio = endereco.municipio,
    uf = endereco.uf,
    telefone = telefone,
    email = email,
    observacoes = observacoes,
    representante = representante?.toEmbutido(),
    criadoEm = criadoEm,
    atualizadoEm = atualizadoEm,
)

private fun RepresentanteLegalEmbutido.toDomain(): RepresentanteLegal = RepresentanteLegal(
    nome = nome,
    documento = documento,
    nacionalidade = nacionalidade,
    estadoCivil = estadoCivil,
    profissao = profissao,
    cargo = cargo,
)

private fun RepresentanteLegal.toEmbutido(): RepresentanteLegalEmbutido = RepresentanteLegalEmbutido(
    nome = nome,
    documento = documento,
    nacionalidade = nacionalidade,
    estadoCivil = estadoCivil,
    profissao = profissao,
    cargo = cargo,
)
