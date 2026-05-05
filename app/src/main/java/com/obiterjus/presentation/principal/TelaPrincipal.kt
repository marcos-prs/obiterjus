package com.obiterjus.presentation.principal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraNavegacaoObiter
import com.obiterjus.presentation.componentes.barras.BarraSuperiorPrincipal
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.presentation.componentes.barras.ItemNavegacao
import com.obiterjus.presentation.navegacao.ObiterNavGraph
import com.obiterjus.presentation.navegacao.ObiterRota
import com.obiterjus.presentation.navegacao.rotasBarraInferior

@Composable
fun TelaPrincipal(
    viewModels: ObiterViewModels,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destinoAtual = navBackStackEntry?.destination
    val estadoInicio by viewModels.inicio.estado.collectAsStateWithLifecycle()

    val isDetailScreen = destinoAtual?.hasRoute<ObiterRota.DetalheProcesso>() == true
    val isAuthScreen = destinoAtual?.hasRoute<ObiterRota.Autenticacao>() == true

    val abasNavegacao = lembrarAbasNavegacao()

    val abaAtual: ObiterRota? = when {
        destinoAtual?.hasRoute<ObiterRota.Inicio>() == true -> ObiterRota.Inicio
        destinoAtual?.hasRoute<ObiterRota.Publicacoes>() == true -> ObiterRota.Publicacoes
        destinoAtual?.hasRoute<ObiterRota.Prazos>() == true -> ObiterRota.Prazos
        destinoAtual?.hasRoute<ObiterRota.Processos>() == true -> ObiterRota.Processos
        destinoAtual?.hasRoute<ObiterRota.Perfil>() == true -> ObiterRota.Perfil
        destinoAtual?.hasRoute<ObiterRota.Auditoria>() == true -> ObiterRota.Auditoria
        else -> null
    }

    val contagemBadges = mapOf<ObiterRota, Int>(
        ObiterRota.Prazos to (estadoInicio.prazosVencidos.takeIf { it > 0 }
            ?: estadoInicio.prazosUrgentes),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(
                visible = !isDetailScreen && !isAuthScreen,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                when (abaAtual) {
                    ObiterRota.Inicio -> BarraSuperiorPrincipal(
                        nomeUsuario = estadoInicio.nomeUsuario.ifEmpty {
                            stringResource(R.string.saudacao_advogado)
                        },
                        numeroOab = estadoInicio.oab,
                        ufOab = estadoInicio.uf,
                        ultimaSincronizacao = estadoInicio.ultimaSincronizacaoTexto,
                    )
                    ObiterRota.Publicacoes -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_publicacoes),
                        onVoltar = {},
                    )
                    ObiterRota.Prazos -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_prazos),
                        onVoltar = {},
                    )
                    ObiterRota.Processos -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_processos),
                        onVoltar = {},
                    )
                    ObiterRota.Perfil -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_perfil),
                        onVoltar = {},
                    )
                    ObiterRota.Auditoria -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_auditoria),
                        onVoltar = { navController.popBackStack() },
                    )
                    else -> BarraSuperiorPrincipal(
                        nomeUsuario = estadoInicio.nomeUsuario.ifEmpty {
                            stringResource(R.string.saudacao_advogado)
                        },
                        numeroOab = estadoInicio.oab,
                        ufOab = estadoInicio.uf,
                        ultimaSincronizacao = estadoInicio.ultimaSincronizacaoTexto,
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isDetailScreen && !isAuthScreen && abaAtual != ObiterRota.Auditoria,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                BarraNavegacaoObiter(
                    abas = abasNavegacao,
                    abaAtual = abaAtual ?: ObiterRota.Inicio,
                    aoSelecionar = { rota ->
                        navController.navigate(rota) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    contagemBadges = contagemBadges,
                )
            }
        },
    ) { paddingValues ->
        ObiterNavGraph(
            navController = navController,
            viewModels = viewModels,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun lembrarAbasNavegacao(): List<ItemNavegacao> {
    return rotasBarraInferior.map { rota ->
        when (rota) {
            ObiterRota.Inicio -> ItemNavegacao(
                rota = rota,
                rotuloResId = R.string.nav_inicio,
                iconeAtivo = ObiterIcones.InicioAtivo,
                iconeInativo = ObiterIcones.InicioInativo,
            )
            ObiterRota.Publicacoes -> ItemNavegacao(
                rota = rota,
                rotuloResId = R.string.nav_publicacoes,
                iconeAtivo = ObiterIcones.PublicacoesAtivo,
                iconeInativo = ObiterIcones.PublicacoesInativo,
            )
            ObiterRota.Prazos -> ItemNavegacao(
                rota = rota,
                rotuloResId = R.string.nav_prazos,
                iconeAtivo = ObiterIcones.PrazosAtivo,
                iconeInativo = ObiterIcones.PrazosInativo,
            )
            ObiterRota.Processos -> ItemNavegacao(
                rota = rota,
                rotuloResId = R.string.nav_processos,
                iconeAtivo = ObiterIcones.ProcessosAtivo,
                iconeInativo = ObiterIcones.ProcessosInativo,
            )
            ObiterRota.Perfil -> ItemNavegacao(
                rota = rota,
                rotuloResId = R.string.nav_perfil,
                iconeAtivo = ObiterIcones.PerfilAtivo,
                iconeInativo = ObiterIcones.PerfilInativo,
            )
            else -> ItemNavegacao(
                rota = rota,
                rotuloResId = R.string.nav_inicio,
                iconeAtivo = ObiterIcones.InicioAtivo,
                iconeInativo = ObiterIcones.InicioInativo,
            )
        }
    }
}
