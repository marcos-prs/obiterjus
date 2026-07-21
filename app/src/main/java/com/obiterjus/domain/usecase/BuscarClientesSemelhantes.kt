package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.repository.ClientesRepository

/**
 * Clientes que provavelmente já são a mesma pessoa da parte marcada.
 *
 * O CPF/CNPJ é decisivo: quando casa, é a mesma pessoa e não faz sentido
 * oferecer alternativas. Só na ausência de documento se cai no nome, que é
 * sugestão — homônimos existem, e a decisão fica com o usuário.
 */
class BuscarClientesSemelhantes(
    private val repositorio: ClientesRepository,
) {
    suspend operator fun invoke(participante: ParticipanteProcesso): List<Cliente> {
        val documento = participante.cpfCnpj?.takeIf { it.isNotBlank() }
        if (documento != null) {
            repositorio.buscarPorDocumento(documento)?.let { return listOf(it) }
        }
        val nome = participante.nome?.takeIf { it.isNotBlank() } ?: return emptyList()
        return repositorio.buscarPorNome(nome)
    }
}
