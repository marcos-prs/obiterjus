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
import com.obiterjus.presentation.adicionarprocesso.TelaAdicionarProcesso
import com.obiterjus.presentation.auditoria.TelaAuditoria
import com.obiterjus.presentation.detalheprocesso.TelaDetalheProcesso
import com.obiterjus.presentation.detalhepublicacao.TelaDetalhePublicacao
import com.obiterjus.presentation.editarprocesso.TelaEditarProcesso
import com.obiterjus.presentation.inicio.TelaInicio
import com.obiterjus.presentation.perfil.TelaPerfil
import com.obiterjus.presentation.perfil.TelaEditarPerfil
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
            TelaPrazos(
                viewModel = viewModels.prazos,
                aoAbrirPublicacao = { publicacaoId ->
                    navController.navigate(ObiterRota.DetalhePublicacao(publicacaoId)) {
                        launchSingleTop = true
                    }
                },
            )
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
                    navController.navigate(ObiterRota.EditarPerfil) {
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

        composable<ObiterRota.EditarPerfil> {
            TelaEditarPerfil(
                viewModel = org.koin.androidx.compose.koinViewModel(),
                aoVoltar = { navController.popBackStack() }
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
                aoEditarProcesso = {
                    navController.navigate(ObiterRota.EditarProcesso(rota.numeroProcesso)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<ObiterRota.DetalhePublicacao> { backStackEntry ->
            val rota = backStackEntry.toRoute<ObiterRota.DetalhePublicacao>()
            TelaDetalhePublicacao(
                viewModel = viewModels.detalhePublicacao,
                publicacaoId = rota.publicacaoId,
                onVoltar = { navController.popBackStack() }
            )
        }

        composable<ObiterRota.AdicionarProcesso> {
            TelaAdicionarProcesso(
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
            TelaEditarProcesso(
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
