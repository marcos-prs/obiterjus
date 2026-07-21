package com.obiterjus.domain.model

data class PrazoAgendaItem(
    val publicacao: Publicacao,
    val prazo: PublicacaoPrazo,
    // Nome(s) do(s) cliente(s) marcado(s) pelo usuário no processo respectivo.
    val nomeCliente: String? = null,
)
