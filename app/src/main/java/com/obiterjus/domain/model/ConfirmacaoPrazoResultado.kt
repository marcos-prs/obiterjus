package com.obiterjus.domain.model

sealed interface ConfirmacaoPrazoResultado {
    data object ConfirmadoLocalmente : ConfirmacaoPrazoResultado

    data class EventoCriado(
        val provedor: ProvedorCalendario,
        val idExterno: String,
    ) : ConfirmacaoPrazoResultado

    data class SincronizacaoPendente(
        val provedor: ProvedorCalendario,
    ) : ConfirmacaoPrazoResultado

    data object Falha : ConfirmacaoPrazoResultado
}

