package com.obiterjus.presentation.acervo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.obiterjus.R
import com.obiterjus.presentation.clientes.ClientesViewModel
import com.obiterjus.presentation.clientes.ConteudoClientes
import com.obiterjus.presentation.componentes.navegacao.NavegadorAbasSwipeable
import com.obiterjus.presentation.processos.ConteudoProcessos
import com.obiterjus.presentation.processos.ProcessosViewModel

/** Abas do acervo, na ordem de exibição. */
enum class AbaAcervo { PROCESSOS, CLIENTES }

/**
 * Hospeda os dois eixos do acervo — o mesmo conjunto de casos indexado por
 * número (Processos) ou por pessoa (Clientes). Fica sob a rota
 * [com.obiterjus.presentation.navegacao.ObiterRota.Processos] para não gastar
 * um sexto item na barra inferior, que já está no limite de cinco.
 *
 * A aba selecionada é içada porque a barra superior depende dela: o título e a
 * ação de adicionar mudam conforme o eixo.
 */
@Composable
fun ConteudoAcervo(
    viewModelProcessos: ProcessosViewModel,
    viewModelClientes: ClientesViewModel,
    aoAbrirProcesso: (String) -> Unit,
    aoAbrirCliente: (String) -> Unit,
    abaSelecionada: AbaAcervo,
    aoSelecionarAba: (AbaAcervo) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavegadorAbasSwipeable(
        tabs = listOf(
            stringResource(R.string.nav_processos),
            stringResource(R.string.nav_clientes),
        ),
        modifier = modifier,
        initialTabIndex = abaSelecionada.ordinal,
        onTabSelected = { indice -> aoSelecionarAba(AbaAcervo.entries[indice]) },
    ) { pagina ->
        when (AbaAcervo.entries[pagina]) {
            AbaAcervo.PROCESSOS -> ConteudoProcessos(
                viewModel = viewModelProcessos,
                aoAbrirDetalhe = aoAbrirProcesso,
            )
            AbaAcervo.CLIENTES -> ConteudoClientes(
                viewModel = viewModelClientes,
                aoAbrirCliente = aoAbrirCliente,
            )
        }
    }
}
