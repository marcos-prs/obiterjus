package com.obiterjus.presentation.publicacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraBusca
import com.obiterjus.presentation.componentes.cards.CardPublicacao
import com.obiterjus.presentation.componentes.cards.PrioridadeStripe
import com.obiterjus.presentation.componentes.chips.ChipFiltroRow
import com.obiterjus.ui.theme.ObiterTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private const val LOTE_PAGINACAO = 20

@Composable
fun TelaPublicacoes(
    estado: EstadoPublicacoes,
    aoAlterarFiltroTexto: (String) -> Unit,
    aoAlterarFiltroTribunal: (String) -> Unit,
    aoAlterarFiltroTipo: (String) -> Unit,
    aoAlterarFiltroDataInicio: (String) -> Unit,
    aoAlterarFiltroDataFim: (String) -> Unit,
    aoAlternarSomenteSigilosas: () -> Unit,
    aoLimparFiltros: () -> Unit,
    aoSelecionarPublicacao: (Long) -> Unit,
    aoFecharDetalhe: () -> Unit,
    aoAbrirCertidao: (Publicacao) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val listState = rememberLazyListState()
    var limiteVisivel by remember { mutableIntStateOf(LOTE_PAGINACAO) }
    val publicacoesVisiveis = estado.publicacoes.take(limiteVisivel)
    val tribunalTodos = stringResource(R.string.filtro_todos)

    LaunchedEffect(estado.publicacoes.size) {
        limiteVisivel = LOTE_PAGINACAO
    }
    LaunchedEffect(listState, estado.publicacoes.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { indice ->
                indice >= publicacoesVisiveis.lastIndex - 4
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (limiteVisivel < estado.publicacoes.size) {
                    limiteVisivel = (limiteVisivel + LOTE_PAGINACAO).coerceAtMost(estado.publicacoes.size)
                }
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            BarraBusca(
                consulta = estado.filtros.texto,
                aoMudarConsulta = aoAlterarFiltroTexto,
                placeholder = stringResource(R.string.busca_publicacoes_placeholder),
            )
        }
        item {
            ChipFiltroRow(
                chips = listOf(
                    tribunalTodos,
                    stringResource(R.string.filtro_tjmg),
                    stringResource(R.string.filtro_trt),
                    stringResource(R.string.filtro_trf),
                ),
                chipAtivo = estado.filtros.tribunal.ifBlank { tribunalTodos },
                aoSelecionar = { chip -> aoAlterarFiltroTribunal(chip.takeUnless { it == tribunalTodos }.orEmpty()) },
            )
        }
        item {
            ChipFiltroRow(
                chips = listOf(
                    tribunalTodos,
                    stringResource(R.string.filtro_sentenca),
                    stringResource(R.string.filtro_decisao),
                    stringResource(R.string.filtro_despacho),
                ),
                chipAtivo = estado.filtros.tipoComunicacao.ifBlank { tribunalTodos },
                aoSelecionar = { chip -> aoAlterarFiltroTipo(chip.takeUnless { it == tribunalTodos }.orEmpty()) },
            )
        }

        if (publicacoesVisiveis.isEmpty()) {
            item {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.publicacoes_empty_title),
                    corpo = stringResource(R.string.publicacoes_empty_body),
                    icone = ObiterIcones.PublicacoesInativo,
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        } else {
            items(
                items = publicacoesVisiveis,
                key = { it.id },
            ) { publicacao ->
                CardPublicacao(
                    tituloAto = publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo),
                    tipoAto = publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo),
                    data = publicacao.dataDisponibilizacao?.let(FormatadorData::formatarData)
                        ?: stringResource(R.string.publicacoes_sem_data),
                    numeroProcesso = publicacao.numeroProcesso
                        ?: stringResource(R.string.publicacoes_sem_numero_processo),
                    prazoDias = publicacao.prazo?.quantidade?.let { dias ->
                        stringResource(R.string.prazos_badge_dias, dias)
                    },
                    trechoTexto = publicacao.textoLimpo,
                    prioridade = publicacao.prioridadeStripe(),
                    aoClicar = { aoSelecionarPublicacao(publicacao.id) },
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        }

        if (limiteVisivel < estado.publicacoes.size) {
            item {
                Text(
                    text = stringResource(R.string.lista_carregando_mais),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObiterTheme.colors.textMuted,
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        }
    }
}

private fun Publicacao.prioridadeStripe(): PrioridadeStripe =
    when {
        isSigiloso -> PrioridadeStripe.CRITICA
        tipoComunicacao?.contains("sent", ignoreCase = true) == true -> PrioridadeStripe.SENTENCA
        tipoComunicacao?.contains("decis", ignoreCase = true) == true -> PrioridadeStripe.DECISAO
        prazo != null -> PrioridadeStripe.FAVORAVEL
        else -> PrioridadeStripe.ROTINEIRO
    }

@Composable
fun ConteudoPublicacoes(
    viewModel: PublicacoesViewModel,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    TelaPublicacoes(
        estado = estado,
        aoAlterarFiltroTexto = viewModel::aoAlterarFiltroTexto,
        aoAlterarFiltroTribunal = viewModel::aoAlterarFiltroTribunal,
        aoAlterarFiltroTipo = viewModel::aoAlterarFiltroTipo,
        aoAlterarFiltroDataInicio = viewModel::aoAlterarFiltroDataInicio,
        aoAlterarFiltroDataFim = viewModel::aoAlterarFiltroDataFim,
        aoAlternarSomenteSigilosas = viewModel::aoAlternarSomenteSigilosas,
        aoLimparFiltros = viewModel::aoLimparFiltros,
        aoSelecionarPublicacao = viewModel::aoSelecionarPublicacao,
        aoFecharDetalhe = viewModel::aoFecharDetalhe,
        aoAbrirCertidao = viewModel::aoAbrirCertidao,
        modifier = modifier,
    )
}
