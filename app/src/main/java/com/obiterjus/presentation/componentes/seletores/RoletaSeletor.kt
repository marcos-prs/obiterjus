package com.obiterjus.presentation.componentes.seletores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.obiterjus.ui.theme.ObiterJusTheme
import com.obiterjus.ui.theme.ObiterTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Roleta rolante (wheel picker) genérica: coluna com snap por item em que o
 * valor selecionado é o da linha central, delimitada por dois divisores.
 */
@Composable
fun RoletaSeletor(
    itens: List<String>,
    indiceSelecionado: Int,
    aoSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    alturaItem: Dp = 40.dp,
    linhasVisiveis: Int = 5,
) {
    require(linhasVisiveis % 2 == 1) { "linhasVisiveis deve ser ímpar" }
    val linhasDeMargem = linhasVisiveis / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = indiceSelecionado.coerceIn(0, itens.lastIndex),
    )
    val coroutineScope = rememberCoroutineScope()

    // Com o contentPadding de meia-janela, o item central é o firstVisibleItem
    // quando o offset assenta em zero; arredonda pelo offset enquanto rola.
    LaunchedEffect(listState, itens.size) {
        snapshotFlow {
            val alturaPx = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            if (alturaPx <= 0) {
                listState.firstVisibleItemIndex
            } else {
                listState.firstVisibleItemIndex +
                    (listState.firstVisibleItemScrollOffset.toFloat() / alturaPx).roundToInt()
            }
        }
            .distinctUntilChanged()
            .collect { indice -> aoSelecionar(indice.coerceIn(0, itens.lastIndex)) }
    }

    Box(
        modifier = modifier.height(alturaItem * linhasVisiveis),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(vertical = alturaItem * linhasDeMargem),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(itens.size) { indice ->
                val selecionado = indice == indiceSelecionado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(alturaItem)
                        .alpha(if (selecionado) 1f else 0.4f)
                        .clickable {
                            coroutineScope.launch { listState.animateScrollToItem(indice) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = itens[indice],
                        style = if (selecionado) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = if (selecionado) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        // Moldura da linha central
        Column(modifier = Modifier.align(Alignment.Center)) {
            HorizontalDivider(color = ObiterTheme.colors.divider)
            Spacer(modifier = Modifier.height(alturaItem))
            HorizontalDivider(color = ObiterTheme.colors.divider)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoletaSeletorPreview() {
    ObiterJusTheme {
        RoletaSeletor(
            itens = (1..120).map(Int::toString),
            indiceSelecionado = 14,
            aoSelecionar = {},
        )
    }
}
