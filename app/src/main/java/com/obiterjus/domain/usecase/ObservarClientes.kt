package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.ClienteComProcessos
import com.obiterjus.domain.repository.ClientesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Clientes com os processos em que atuam. Junta as duas consultas aqui em vez
 * de num JOIN para que um cliente sem processo vinculado continue aparecendo —
 * cadastrado, mas ainda sem caso associado.
 */
class ObservarClientes(
    private val repositorio: ClientesRepository,
) {
    operator fun invoke(): Flow<List<ClienteComProcessos>> =
        combine(
            repositorio.observarClientes(),
            repositorio.observarVinculos(),
        ) { clientes, vinculos ->
            val processosPorCliente = vinculos.groupBy { it.clienteId }
            clientes.map { cliente ->
                ClienteComProcessos(
                    cliente = cliente,
                    numerosProcesso = processosPorCliente[cliente.id]
                        ?.map { it.numeroProcesso }
                        .orEmpty(),
                )
            }
        }
}
