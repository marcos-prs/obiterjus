package com.obiterjus.presentation.navegacao

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.obiterjus.R
import com.obiterjus.presentation.autenticacao.AutenticacaoScreen
import com.obiterjus.presentation.autenticacao.ModoAutenticacao
import com.obiterjus.presentation.acervo.AbaAcervo
import com.obiterjus.presentation.acervo.ConteudoAcervo
import com.obiterjus.presentation.adicionarprocesso.AdicionarProcessoScreen
import com.obiterjus.presentation.auditoria.AuditoriaScreen
import com.obiterjus.presentation.detalhecliente.DetalheClienteScreen
import com.obiterjus.presentation.detalheprocesso.DetalheProcessoScreen
import com.obiterjus.presentation.editarcliente.EditarClienteScreen
import com.obiterjus.presentation.detalhepublicacao.DetalhePublicacaoScreen
import com.obiterjus.presentation.editarprocesso.EditarProcessoScreen
import com.obiterjus.presentation.inicio.InicioScreen
import com.obiterjus.presentation.perfil.PerfilScreen
import com.obiterjus.presentation.perfil.EditarPerfilScreen
import com.obiterjus.presentation.prazos.PrazosScreen
import com.obiterjus.presentation.principal.ObiterViewModels
import com.obiterjus.presentation.publicacoes.ConteudoPublicacoes

@Composable
fun ObiterNavGraph(
    navController: NavHostController,
    viewModels: ObiterViewModels,
    abaAcervo: AbaAcervo,
    aoSelecionarAbaAcervo: (AbaAcervo) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ObiterRota.Inicio,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(220),
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(180),
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220),
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(180),
                )
        },
    ) {
        composable<ObiterRota.Inicio> {
            InicioScreen(
                viewModel = viewModels.inicio,
                aoNavegarParaPrazos = {
                    navegarParaAbaPrincipal(navController, ObiterRota.Prazos)
                },
                aoVerTodasPublicacoes = {
                    navegarParaAbaPrincipal(navController, ObiterRota.Publicacoes)
                },
                aoAbrirPublicacao = { publicacaoId ->
                    navController.navigate(ObiterRota.DetalhePublicacao(publicacaoId)) {
                        launchSingleTop = true
                    }
                },
                aoNavegarParaProcessos = {
                    navegarParaAbaPrincipal(navController, ObiterRota.Processos)
                },
                aoAbrirCliente = { clienteId ->
                    navController.navigate(ObiterRota.DetalheCliente(clienteId)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.Publicacoes> {
            ConteudoPublicacoes(
                viewModel = viewModels.publicacoes,
                aoAbrirPublicacao = { publicacaoId ->
                    navController.navigate(ObiterRota.DetalhePublicacao(publicacaoId)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.Prazos> {
            PrazosScreen(
                viewModel = viewModels.prazos,
                aoAbrirPublicacao = { publicacaoId ->
                    navController.navigate(ObiterRota.DetalhePublicacao(publicacaoId)) {
                        launchSingleTop = true
                    }
                },
                aoAbrirProcesso = { numeroProcesso ->
                    navController.navigate(ObiterRota.DetalheProcesso(numeroProcesso)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.Processos> {
            ConteudoAcervo(
                viewModelProcessos = viewModels.processos,
                viewModelClientes = viewModels.clientes,
                aoAbrirProcesso = { numero ->
                    navController.navigate(ObiterRota.DetalheProcesso(numero)) {
                        launchSingleTop = true
                    }
                },
                aoAbrirCliente = { clienteId ->
                    navController.navigate(ObiterRota.DetalheCliente(clienteId)) {
                        launchSingleTop = true
                    }
                },
                abaSelecionada = abaAcervo,
                aoSelecionarAba = aoSelecionarAbaAcervo,
            )
        }

        composable<ObiterRota.Perfil> {
            PerfilScreen(
                viewModel = viewModels.perfil,
                aoAbrirAuditoria = { navController.navigate(ObiterRota.Auditoria) },
                aoForcarSincronizacao = viewModels.perfil::aoForcarSincronizacao,
                aoEntrar = {
                    viewModels.autenticacao.aoSelecionarModo(ModoAutenticacao.ENTRAR)
                    navController.navigate(ObiterRota.Autenticacao) {
                        launchSingleTop = true
                    }
                },
                aoCriarConta = {
                    viewModels.autenticacao.aoSelecionarModo(ModoAutenticacao.CADASTRAR)
                    navController.navigate(ObiterRota.Autenticacao) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.Autenticacao> {
            AutenticacaoScreen(
                viewModel = viewModels.autenticacao,
                aoVoltar = { navController.popBackStack() },
                aoAutenticado = {
                    navController.navigate(ObiterRota.Inicio) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
            )
        }

        composable<ObiterRota.EditarPerfil> {
            EditarPerfilScreen(
                viewModel = org.koin.androidx.compose.koinViewModel(),
                aoVoltar = { navController.popBackStack() }
            )
        }

        composable<ObiterRota.Auditoria> {
            AuditoriaScreen(viewModel = viewModels.auditoria)
        }

        composable<ObiterRota.DetalheProcesso> { backStackEntry ->
            val rota = backStackEntry.toRoute<ObiterRota.DetalheProcesso>()
            DetalheProcessoScreen(
                viewModel = viewModels.detalheProcesso,
                numeroProcesso = rota.numeroProcesso,
                onVoltar = { navController.popBackStack() },
                aoEditarProcesso = {
                    navController.navigate(ObiterRota.EditarProcesso(rota.numeroProcesso)) {
                        launchSingleTop = true
                    }
                },
                aoExcluirProcesso = {
                    if (!navController.popBackStack(ObiterRota.Processos, inclusive = false)) {
                        navController.popBackStack()
                    }
                },
                aoAbrirPublicacao = { publicacaoId ->
                    navController.navigate(ObiterRota.DetalhePublicacao(publicacaoId)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.DetalheCliente> { backStackEntry ->
            val rota = backStackEntry.toRoute<ObiterRota.DetalheCliente>()
            DetalheClienteScreen(
                viewModel = viewModels.detalheCliente,
                clienteId = rota.clienteId,
                onVoltar = { navController.popBackStack() },
                aoAbrirProcesso = { numero ->
                    navController.navigate(ObiterRota.DetalheProcesso(numero)) {
                        launchSingleTop = true
                    }
                },
                aoEditarCliente = { id ->
                    navController.navigate(ObiterRota.EditarCliente(id)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.AdicionarCliente> {
            EditarClienteScreen(
                viewModel = viewModels.editarCliente,
                clienteId = "",
                onVoltar = { navController.popBackStack() },
                titulo = stringResource(R.string.adicionar_cliente_titulo),
            )
        }

        composable<ObiterRota.EditarCliente> { backStackEntry ->
            val rota = backStackEntry.toRoute<ObiterRota.EditarCliente>()
            EditarClienteScreen(
                viewModel = viewModels.editarCliente,
                clienteId = rota.clienteId,
                onVoltar = { navController.popBackStack() },
            )
        }

        composable<ObiterRota.DetalhePublicacao> { backStackEntry ->
            val rota = backStackEntry.toRoute<ObiterRota.DetalhePublicacao>()
            DetalhePublicacaoScreen(
                viewModel = viewModels.detalhePublicacao,
                publicacaoId = rota.publicacaoId,
                onVoltar = { navController.popBackStack() },
                aoAbrirProcesso = { numero ->
                    navController.navigate(ObiterRota.DetalheProcesso(numero)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.AdicionarProcesso> {
            AdicionarProcessoScreen(
                viewModel = viewModels.adicionarProcesso,
                onVoltar = { navController.popBackStack() },
                onProcessoAdicionado = { numero ->
                    navController.navigate(ObiterRota.DetalheProcesso(numero)) {
                        popUpTo(ObiterRota.AdicionarProcesso) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.EditarProcesso> { backStackEntry ->
            val rota = backStackEntry.toRoute<ObiterRota.EditarProcesso>()
            EditarProcessoScreen(
                viewModel = viewModels.editarProcesso,
                numeroProcesso = rota.numeroProcesso,
                onVoltar = { navController.popBackStack() },
                onExcluido = {
                    navController.popBackStack(ObiterRota.Processos, inclusive = false)
                },
            )
        }
    }
}

private fun navegarParaAbaPrincipal(
    navController: NavHostController,
    rota: ObiterRota,
) {
    navController.navigate(rota) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
