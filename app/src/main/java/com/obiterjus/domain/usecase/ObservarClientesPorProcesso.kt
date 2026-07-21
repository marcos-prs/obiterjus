package com.obiterjus.domain.usecase

import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.ClienteDoProcesso
import com.obiterjus.domain.model.VinculoClienteProcesso
import com.obiterjus.domain.repository.ClientesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Cliente exibido em cada processo. Prefere o cadastro vinculado, que carrega
 * identidade e portanto abre a ficha; cai na parte marcada como cliente quando
 * ainda não houve vínculo — marcação antiga que só vira cadastro no próximo
 * salvamento do processo. Sem esse fallback, processos já marcados perderiam o
 * nome do cliente da tela até serem reabertos e salvos.
 */
class ObservarClientesPorProcesso(
    private val participanteDao: ParticipanteDao,
    private val repositorioClientes: ClientesRepository,
) {
    operator fun invoke(): Flow<Map<String, ClienteDoProcesso>> =
        combine(
            participanteDao.observeAll().map { it.clientesPorProcesso() },
            repositorioClientes.observarClientes(),
            repositorioClientes.observarVinculos(),
        ) { porParticipante, clientes, vinculos ->
            porParticipante + vinculos.porProcesso(clientes)
        }
}

/** Nomes das partes marcadas como cliente, sem identidade de cadastro. */
private fun List<ParticipanteEntity>.clientesPorProcesso(): Map<String, ClienteDoProcesso> =
    filter { it.ehCliente && !it.nome.isNullOrBlank() }
        .groupBy { it.numeroProcesso }
        .mapNotNull { (numeroProcesso, participantes) ->
            val nomes = participantes
                .mapNotNull { it.nome?.trim()?.takeIf { nome -> nome.isNotEmpty() } }
                .distinct()
            nomes.takeIf { it.isNotEmpty() }
                ?.let { numeroProcesso to ClienteDoProcesso(nome = it.joinToString(", ")) }
        }
        .toMap()

/**
 * O card tem uma linha só para o cliente: com mais de um vinculado o nome vira
 * uma lista e o toque perde destino único, então não recebe identidade.
 */
private fun List<VinculoClienteProcesso>.porProcesso(
    clientes: List<Cliente>,
): Map<String, ClienteDoProcesso> {
    val porId = clientes.associateBy { it.id }
    return groupBy { it.numeroProcesso }
        .mapNotNull { (numeroProcesso, vinculos) ->
            val vinculados = vinculos.mapNotNull { porId[it.clienteId] }
            when {
                vinculados.isEmpty() -> null
                vinculados.size == 1 -> numeroProcesso to ClienteDoProcesso(
                    nome = vinculados.single().nome,
                    clienteId = vinculados.single().id,
                )
                else -> numeroProcesso to ClienteDoProcesso(
                    nome = vinculados.joinToString(", ") { it.nome },
                )
            }
        }
        .toMap()
}
