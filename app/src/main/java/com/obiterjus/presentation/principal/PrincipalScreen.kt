package com.obiterjus.presentation.principal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.obiterjus.R
import com.obiterjus.presentation.acervo.AbaAcervo
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraNavegacaoObiter
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.presentation.componentes.barras.ItemNavegacao
import com.obiterjus.presentation.navegacao.ObiterNavGraph
import com.obiterjus.presentation.navegacao.ObiterRota
import com.obiterjus.presentation.navegacao.rotasBarraInferior
import com.obiterjus.presentation.autenticacao.ModoAutenticacao
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun PrincipalScreen(
    viewModels: ObiterViewModels,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destinoAtual = navBackStackEntry?.destination
    val estadoInicio by viewModels.inicio.estado.collectAsStateWithLifecycle()
    val estadoPerfil by viewModels.perfil.estado.collectAsStateWithLifecycle()
    val mostrarMenuConta by viewModels.perfil.mostrarMenuConta.collectAsStateWithLifecycle()
    val colors = ObiterTheme.colors
    val textColorBarra = Color(0xFFF4F6F8)

    val isDetailScreen = destinoAtual?.hasRoute<ObiterRota.DetalheProcesso>() == true
    val isDetailPublicacaoScreen = destinoAtual?.hasRoute<ObiterRota.DetalhePublicacao>() == true
    val isAddScreen = destinoAtual?.hasRoute<ObiterRota.AdicionarProcesso>() == true
    val isEditScreen = destinoAtual?.hasRoute<ObiterRota.EditarProcesso>() == true
    val isDetailClienteScreen = destinoAtual?.hasRoute<ObiterRota.DetalheCliente>() == true
    val isEditClienteScreen = destinoAtual?.hasRoute<ObiterRota.EditarCliente>() == true
    val isAddClienteScreen = destinoAtual?.hasRoute<ObiterRota.AdicionarCliente>() == true
    val isPushScreen = isDetailScreen || isDetailPublicacaoScreen || isAddScreen ||
        isEditScreen || isDetailClienteScreen || isEditClienteScreen || isAddClienteScreen
    val isAuthScreen = destinoAtual?.hasRoute<ObiterRota.Autenticacao>() == true

    val abasNavegacao = lembrarAbasNavegacao()

    // A aba do acervo mora aqui porque a barra superior depende dela — título e
    // ação de adicionar mudam entre Processos e Clientes.
    var abaAcervo by rememberSaveable { mutableStateOf(AbaAcervo.PROCESSOS) }

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
                    ObiterRota.Inicio -> Unit
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
                        titulo = when (abaAcervo) {
                            AbaAcervo.PROCESSOS -> stringResource(R.string.nav_processos)
                            AbaAcervo.CLIENTES -> stringResource(R.string.nav_clientes)
                        },
                        onVoltar = { navController.popBackStack() },
                        mostrarVoltar = false,
                        acoes = {
                            // Cada eixo tem seu "+": Processos abre o cadastro de
                            // processo; Clientes abre o cadastro avulso de cliente,
                            // sem depender de uma parte marcada num processo.
                            val (rotaAdicionar, descricaoAdicionar) = when (abaAcervo) {
                                AbaAcervo.PROCESSOS ->
                                    ObiterRota.AdicionarProcesso to R.string.cd_adicionar_processo
                                AbaAcervo.CLIENTES ->
                                    ObiterRota.AdicionarCliente to R.string.cd_adicionar_cliente
                            }
                            IconButton(
                                onClick = {
                                    navController.navigate(rotaAdicionar) {
                                        launchSingleTop = true
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = ObiterIcones.Adicionar,
                                    contentDescription = stringResource(descricaoAdicionar),
                                    tint = textColorBarra,
                                )
                            }
                        },
                    )
                    ObiterRota.Perfil -> BarraSuperiorSecundaria(
                        titulo = stringResource(R.string.nav_perfil),
                        onVoltar = { navController.popBackStack() },
                        mostrarVoltar = false,
                        acoes = {
                            if (estadoPerfil.autenticado) {
                                Box {
                                    IconButton(onClick = { viewModels.perfil.aoAbrirMenu() }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.perfil_menu_conta),
                                            tint = textColorBarra.copy(alpha = 0.92f),
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
                                                navController.navigate(ObiterRota.EditarPerfil) {
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
                    else -> Unit
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
            abaAcervo = abaAcervo,
            aoSelecionarAbaAcervo = { abaAcervo = it },
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
