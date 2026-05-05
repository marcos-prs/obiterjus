package com.obiterjus.presentation.navegacao

import kotlinx.serialization.Serializable

/**
 * Rotas de navegação do ObiterJus.
 *
 * Usa a type-safe navigation do Navigation Compose 2.8+.
 * Cada objeto/data class representa um destino no NavGraph.
 */
sealed interface ObiterRota {

    /** Telas de nível superior (bottom navigation). */
    @Serializable
    data object Inicio : ObiterRota

    @Serializable
    data object Publicacoes : ObiterRota

    @Serializable
    data object Prazos : ObiterRota

    @Serializable
    data object Processos : ObiterRota

    @Serializable
    data object Perfil : ObiterRota

    @Serializable
    data object Autenticacao : ObiterRota

    /** Telas de detalhe (push navigation). */
    @Serializable
    data class DetalheProcesso(val numeroProcesso: String) : ObiterRota

    @Serializable
    data object Auditoria : ObiterRota
}

/**
 * Agrupa as rotas que aparecem na barra de navegação inferior.
 * A ordem define a ordem visual das abas.
 */
val rotasBarraInferior: List<ObiterRota> = listOf(
    ObiterRota.Inicio,
    ObiterRota.Publicacoes,
    ObiterRota.Prazos,
    ObiterRota.Processos,
    ObiterRota.Perfil,
)
