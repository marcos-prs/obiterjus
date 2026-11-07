package com.obiterjus.domain.model

data class SincronizacaoNuvemResumo(
    val processos: Int = 0,
    val publicacoes: Int = 0,
    val movimentos: Int = 0,
    val participantes: Int = 0,
    val clientes: Int = 0,
    val prazos: Int = 0,
    val minutas: Int = 0,
) {
    val total: Int
        get() = processos + publicacoes + movimentos + participantes + clientes + prazos + minutas
}
