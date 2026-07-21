package com.obiterjus.presentation.detalhecliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.repository.ClientesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class DetalheClienteViewModel(
    private val repositorio: ClientesRepository,
) : ViewModel() {
    private val clienteId = MutableStateFlow<String?>(null)

    private val clienteSelecionado = clienteId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repositorio.observarCliente(id)
    }

    private val processosDoCliente = clienteId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repositorio.observarProcessosDoCliente(id)
    }

    val estado: StateFlow<EstadoDetalheCliente> =
        combine(clienteSelecionado, processosDoCliente) { cliente, processos ->
            EstadoDetalheCliente(
                cliente = cliente,
                numerosProcesso = processos,
                carregando = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TEMPO_ASSINATURA_MS),
            initialValue = EstadoDetalheCliente(),
        )

    fun aoAbrirCliente(id: String) {
        clienteId.value = id
    }
}

data class EstadoDetalheCliente(
    val cliente: Cliente? = null,
    val numerosProcesso: List<String> = emptyList(),
    val carregando: Boolean = true,
)

private const val TEMPO_ASSINATURA_MS = 5_000L
