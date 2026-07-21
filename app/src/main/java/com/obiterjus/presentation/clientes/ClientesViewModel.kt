package com.obiterjus.presentation.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.core.texto.correspondeAoTermoBusca
import com.obiterjus.domain.model.ClienteComProcessos
import com.obiterjus.domain.usecase.ObservarClientes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ClientesViewModel(
    observarClientes: ObservarClientes,
) : ViewModel() {
    private val busca = MutableStateFlow("")

    val estado: StateFlow<EstadoClientes> =
        combine(observarClientes(), busca) { clientes, termo ->
            EstadoClientes(
                clientes = clientes.filter { it.atende(termo) },
                totalCadastrados = clientes.size,
                busca = termo,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EstadoClientes(),
        )

    fun aoAlterarBusca(valor: String) {
        busca.update { valor }
    }

    private fun ClienteComProcessos.atende(termo: String): Boolean =
        listOf(
            cliente.nome,
            cliente.documento,
            cliente.email,
            cliente.representante?.nome,
        ).correspondeAoTermoBusca(termo)
}

data class EstadoClientes(
    val clientes: List<ClienteComProcessos> = emptyList(),
    val totalCadastrados: Int = 0,
    val busca: String = "",
) {
    /** Distingue "nenhum cliente ainda" de "a busca não achou nada". */
    val vazioPorBusca: Boolean get() = clientes.isEmpty() && totalCadastrados > 0
}
