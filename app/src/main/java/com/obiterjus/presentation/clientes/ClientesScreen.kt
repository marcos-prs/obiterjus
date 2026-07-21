package com.obiterjus.presentation.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.domain.model.TipoPessoa
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraBusca
import com.obiterjus.presentation.componentes.cards.CardCliente
import com.obiterjus.ui.theme.ObiterTheme

/**
 * Carteira de clientes — o eixo "por pessoa" do mesmo acervo que
 * [com.obiterjus.presentation.processos.ProcessosScreen] mostra por número.
 */
@Composable
fun ClientesScreen(
    estado: EstadoClientes,
    aoAlterarBusca: (String) -> Unit,
    aoAbrirCliente: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            BarraBusca(
                consulta = estado.busca,
                aoMudarConsulta = aoAlterarBusca,
                placeholder = stringResource(R.string.clientes_busca_placeholder),
            )
        }

        if (estado.clientes.isEmpty()) {
            item {
                // "Ainda não tem cliente" e "a busca não achou" pedem saídas
                // diferentes: cadastrar um versus corrigir o termo.
                EstadoVazioObiter(
                    titulo = stringResource(
                        if (estado.vazioPorBusca) R.string.clientes_busca_vazia_title
                        else R.string.clientes_empty_title,
                    ),
                    corpo = stringResource(
                        if (estado.vazioPorBusca) R.string.clientes_busca_vazia_body
                        else R.string.clientes_empty_body,
                    ),
                    icone = ObiterIcones.Cliente,
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        } else {
            items(
                items = estado.clientes,
                key = { it.cliente.id },
            ) { item ->
                CardCliente(
                    nome = item.cliente.nome,
                    documento = item.cliente.documento,
                    rotuloTipoPessoa = stringResource(item.cliente.tipoPessoa.rotuloResId()),
                    quantidadeProcessos = item.numerosProcesso.size,
                    representante = item.cliente.representante?.nome,
                    aoClicar = { aoAbrirCliente(item.cliente.id) },
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        }
    }
}

@Composable
fun ConteudoClientes(
    viewModel: ClientesViewModel,
    aoAbrirCliente: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ClientesScreen(
        estado = estado,
        aoAlterarBusca = viewModel::aoAlterarBusca,
        aoAbrirCliente = aoAbrirCliente,
        modifier = modifier,
    )
}

private fun TipoPessoa.rotuloResId(): Int =
    when (this) {
        TipoPessoa.FISICA -> R.string.clientes_tipo_fisica
        TipoPessoa.JURIDICA -> R.string.clientes_tipo_juridica
    }
