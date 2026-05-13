package com.obiterjus.presentation.principal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.obiterjus.presentation.autenticacao.ModoAutenticacao

@Composable
fun TelaPrincipal(
    viewModels: ObiterViewModels,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destinoAtual = navBackStackEntry?.destination
    val estadoInicio by viewModels.inicio.estado.collectAsStateWithLifecycle()
    val estadoPerfil by viewModels.perfil.estado.collectAsStateWithLifecycle()
    val mostrarMenuConta by viewModels.perfil.mostrarMenuConta.collectAsStateWithLifecycle()
    val primeiroNomeUsuario = estadoInicio.nomeUsuario.primeiroNome()
    val isDark = isSystemInDarkTheme()
    val textColorBarra = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary

    val isDetailScreen = destinoAtual?.hasRoute<ObiterRota.DetalheProcesso>() == true
    val isDetailPublicacaoScreen = destinoAtual?.hasRoute<ObiterRota.DetalhePublicacao>() == true
    val isAddScreen = destinoAtual?.hasRoute<ObiterRota.AdicionarProcesso>() == true
    val isEditScreen = destinoAtual?.hasRoute<ObiterRota.EditarProcesso>() == true
    val isPushScreen = isDetailScreen || isDetailPublicacaoScreen || isAddScreen || isEditScreen
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
                visible = !isPushScreen && !isAuthScreen,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                when (abaAtual) {
                    ObiterRota.Inicio -> BarraSuperiorPrincipal(
                        nomeUsuario = primeiroNomeUsuario.ifEmpty {
                            stringResource(R.string.saudacao_advogado)
                        },
                        numeroOab = estadoInicio.oab,
                        ufOab = estadoInicio.uf,
                        ultimaSincronizacao = estadoInicio.ultimaSincronizacaoTexto,
                    )
                    ObiterRota.Publicacoes -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_publicacoes),
                        onVoltar = { navController.popBackStack() },
                        mostrarVoltar = false,
                    )
                    ObiterRota.Prazos -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_prazos),
                        onVoltar = { navController.popBackStack() },
                        mostrarVoltar = false,
                    )
                    ObiterRota.Processos -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_processos),
                        onVoltar = { navController.popBackStack() },
                        mostrarVoltar = false,
                        acoes = {
                            IconButton(
                                onClick = {
                                    navController.navigate(ObiterRota.AdicionarProcesso) {
                                        launchSingleTop = true
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = ObiterIcones.Adicionar,
                                    contentDescription = stringResource(R.string.cd_adicionar_processo),
                                    tint = textColorBarra,
                                )
                            }
                        },
                    )
                    ObiterRota.Perfil -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_perfil),
                        onVoltar = { navController.popBackStack() },
                        mostrarVoltar = false,
                        corFundo = Color.Transparent,
                        corConteudo = MaterialTheme.colorScheme.onSurface,
                        acoes = {
                            if (estadoPerfil.autenticado) {
                                Box {
                                    IconButton(onClick = { viewModels.perfil.aoAbrirMenu() }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.perfil_menu_conta),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = mostrarMenuConta,
                                        onDismissRequest = { viewModels.perfil.aoFecharMenu() }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.perfil_editar_dados)) },
                                            onClick = {
                                                viewModels.perfil.aoFecharMenu()
                                                viewModels.autenticacao.aoSelecionarModo(ModoAutenticacao.CADASTRAR)
                                                navController.navigate(ObiterRota.Autenticacao) {
                                                    launchSingleTop = true
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.perfil_logout)) },
                                            onClick = {
                                                viewModels.perfil.aoFecharMenu()
                                                viewModels.perfil.aoLogout()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    ObiterRota.Auditoria -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_auditoria),
                        onVoltar = { navController.popBackStack() },
                    )
                    else -> BarraSuperiorPrincipal(
                        nomeUsuario = primeiroNomeUsuario.ifEmpty {
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
                visible = !isPushScreen && !isAuthScreen && abaAtual != ObiterRota.Auditoria,
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

private fun String.primeiroNome(): String =
    trim().substringBefore(' ').trim()

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
