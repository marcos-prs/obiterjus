package com.obiterjus.presentation.navegacao

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.obiterjus.presentation.autenticacao.TelaAutenticacao
import com.obiterjus.presentation.autenticacao.ModoAutenticacao
import com.obiterjus.presentation.auditoria.TelaAuditoria
import com.obiterjus.presentation.detalheprocesso.TelaDetalheProcesso
import com.obiterjus.presentation.inicio.TelaInicio
import com.obiterjus.presentation.perfil.TelaPerfil
import com.obiterjus.presentation.prazos.TelaPrazos
import com.obiterjus.presentation.principal.ObiterViewModels
import com.obiterjus.presentation.processos.ConteudoProcessos
import com.obiterjus.presentation.publicacoes.ConteudoPublicacoes

@Composable
fun ObiterNavGraph(
    navController: NavHostController,
    viewModels: ObiterViewModels,
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
            TelaInicio(
                viewModel = viewModels.inicio,
                aoNavegarParaPrazos = {
                    navController.navigate(ObiterRota.Prazos) {
                        launchSingleTop = true
                    }
                },
                aoVerTodasPublicacoes = {
                    navController.navigate(ObiterRota.Publicacoes) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.Publicacoes> {
            ConteudoPublicacoes(viewModel = viewModels.publicacoes)
        }

        composable<ObiterRota.Prazos> {
            TelaPrazos(viewModel = viewModels.prazos)
        }

        composable<ObiterRota.Processos> {
            ConteudoProcessos(
                viewModel = viewModels.processos,
                aoAbrirDetalhe = { numero ->
                    navController.navigate(ObiterRota.DetalheProcesso(numero)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.Perfil> {
            TelaPerfil(
                viewModel = viewModels.perfil,
                aoAbrirAuditoria = { navController.navigate(ObiterRota.Auditoria) },
                aoForcarSincronizacao = viewModels.perfil::aoForcarSincronizacao,
                aoLogout = viewModels.perfil::aoLogout,
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
                aoEditarPerfil = {
                    // Por enquanto navega para Autenticacao/Cadastro para permitir ajuste de dados
                    viewModels.autenticacao.aoSelecionarModo(ModoAutenticacao.CADASTRAR)
                    navController.navigate(ObiterRota.Autenticacao) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.Autenticacao> {
            TelaAutenticacao(
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

        composable<ObiterRota.Auditoria> {
            TelaAuditoria(viewModel = viewModels.auditoria)
        }

        composable<ObiterRota.DetalheProcesso> { backStackEntry ->
            val rota = backStackEntry.toRoute<ObiterRota.DetalheProcesso>()
            TelaDetalheProcesso(
                viewModel = viewModels.detalheProcesso,
                numeroProcesso = rota.numeroProcesso,
                onVoltar = { navController.popBackStack() },
            )
        }
    }
}
