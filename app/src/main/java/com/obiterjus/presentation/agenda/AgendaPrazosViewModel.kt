package com.obiterjus.presentation.agenda

import androidx.lifecycle.ViewModel
import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

class AgendaPrazosViewModel(
    observarAgendaPrazos: ObservarAgendaPrazos,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    val estado: StateFlow<EstadoAgendaPrazos> =
        observarAgendaPrazos()
            .map { itens ->
                val hoje = LocalDate.now(clock)
                val itensComStatus = itens.map { item ->
                    AgendaPrazoUiItem(
                        item = item,
                        status = item.status(hoje),
                    )
                }
                EstadoAgendaPrazos(
                    itens = itensComStatus,
                    total = itensComStatus.size,
                    vencidos = itensComStatus.count { it.status == AgendaPrazoStatus.VENCIDO },
                    proximos = itensComStatus.count { it.status == AgendaPrazoStatus.PROXIMO },
                    semData = itensComStatus.count { it.status == AgendaPrazoStatus.SEM_DATA },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = EstadoAgendaPrazos(),
            )

    private fun PrazoAgendaItem.status(hoje: LocalDate): AgendaPrazoStatus {
        val vencimento = prazo.dataLimiteEstimada ?: return AgendaPrazoStatus.SEM_DATA
        return when {
            vencimento.isBefore(hoje) -> AgendaPrazoStatus.VENCIDO
            !vencimento.isAfter(hoje.plusDays(PROXIMO_LIMITE_DIAS)) -> AgendaPrazoStatus.PROXIMO
            else -> AgendaPrazoStatus.FUTURO
        }
    }

    private companion object {
        const val PROXIMO_LIMITE_DIAS = 7L
    }
}

data class EstadoAgendaPrazos(
    val itens: List<AgendaPrazoUiItem> = emptyList(),
    val total: Int = 0,
    val vencidos: Int = 0,
    val proximos: Int = 0,
    val semData: Int = 0,
)

data class AgendaPrazoUiItem(
    val item: PrazoAgendaItem,
    val status: AgendaPrazoStatus,
)

enum class AgendaPrazoStatus {
    VENCIDO,
    PROXIMO,
    FUTURO,
    SEM_DATA,
}
