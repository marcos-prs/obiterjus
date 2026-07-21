package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.EnderecoCliente
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.TipoPessoa
import com.obiterjus.domain.model.VinculoClienteProcesso
import com.obiterjus.domain.repository.ClientesRepository
import java.time.Instant
import java.util.UUID

/** O que o usuário decidiu no momento de marcar uma parte como cliente. */
sealed interface DecisaoCliente {
    /** Vincular a um cliente que já existe na carteira. */
    data class Vincular(val clienteId: String) : DecisaoCliente

    /** Criar um cliente novo a partir da qualificação da parte. */
    data object CriarNovo : DecisaoCliente
}

/**
 * Reconcilia os vínculos cliente↔processo com o que a tela de edição deixou
 * marcado. Roda depois de salvar o processo, junto do resto da edição — marcar
 * a caixa não persiste nada sozinha, do mesmo jeito que os outros campos.
 */
class SincronizarClientesDoProcesso(
    private val repositorio: ClientesRepository,
    private val agora: () -> Instant = Instant::now,
) {
    suspend operator fun invoke(
        numeroProcesso: String,
        participantes: List<ParticipanteProcesso>,
        decisoes: Map<String, DecisaoCliente>,
    ) {
        val marcados = participantes.filter { it.ehCliente && !it.nome.isNullOrBlank() }

        marcados.forEach { participante ->
            val clienteId = when (val decisao = decisoes[participante.idLocal]) {
                is DecisaoCliente.Vincular -> decisao.clienteId
                // Sem decisão registrada (marcação anterior a este fluxo) cai
                // no mesmo caminho de criar, que já resolve duplicata sozinho.
                DecisaoCliente.CriarNovo, null -> obterOuCriarCliente(participante)
            }
            repositorio.vincular(
                VinculoClienteProcesso(
                    clienteId = clienteId,
                    numeroProcesso = numeroProcesso,
                    participanteIdLocal = participante.idLocal,
                ),
            )
        }

        val idsMarcados = marcados.map { it.idLocal }.toSet()
        repositorio.obterVinculosDoProcesso(numeroProcesso)
            // Vínculo sem participante de origem foi feito à mão: desmarcar uma
            // parte não pode desfazer o que o usuário vinculou por outro caminho.
            .filter { it.participanteIdLocal != null && it.participanteIdLocal !in idsMarcados }
            .forEach { repositorio.desvincular(it.clienteId, numeroProcesso) }
    }

    /**
     * O documento é único na tabela de clientes: criar sem checar antes
     * derrubaria a gravação por violação de índice quando a mesma pessoa já
     * está na carteira por outro processo.
     */
    private suspend fun obterOuCriarCliente(participante: ParticipanteProcesso): String {
        participante.cpfCnpj?.takeIf { it.isNotBlank() }
            ?.let { documento -> repositorio.buscarPorDocumento(documento) }
            ?.let { return it.id }

        val novo = participante.paraCliente(agora())
        repositorio.salvar(novo)
        return novo.id
    }
}

private fun ParticipanteProcesso.paraCliente(instante: Instant): Cliente = Cliente(
    id = UUID.randomUUID().toString(),
    tipoPessoa = if (tipoPessoa?.trim()?.uppercase() == "J") {
        TipoPessoa.JURIDICA
    } else {
        TipoPessoa.FISICA
    },
    nome = nome.orEmpty().trim(),
    documento = cpfCnpj,
    estadoCivil = estadoCivil,
    profissao = profissao,
    endereco = EnderecoCliente(
        cep = cep,
        logradouro = logradouro,
        numero = numeroEndereco,
    ),
    telefone = telefone,
    email = email,
    criadoEm = instante,
    atualizadoEm = instante,
)
