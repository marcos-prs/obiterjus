package com.obiterjus.presentation.componentes.navegacao

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.obiterjus.ui.theme.ObiterTheme
import kotlinx.coroutines.launch

/**
 * Componente de navegação por abas com suporte a swipe.
 * Sincroniza bidirecionalmente o [PrimaryTabRow] com o [HorizontalPager].
 */
@Composable
fun NavegadorAbasSwipeable(
    tabs: List<String>,
    modifier: Modifier = Modifier,
    initialTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    content: @Composable (Int) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialTabIndex) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val colors = ObiterTheme.colors

    // Sincroniza a página do pager com a seleção externa/interna de abas
    LaunchedEffect(pagerState.currentPage) {
        onTabSelected(pagerState.currentPage)
    }

    // Caso a etapa mude externamente (ex: via botões Avançar/Voltar no ViewModel)
    LaunchedEffect(initialTabIndex) {
        if (initialTabIndex != pagerState.currentPage && initialTabIndex in 0 until tabs.size) {
            pagerState.animateScrollToPage(initialTabIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = pagerState.currentPage == index
                Tab(
                    selected = selected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else colors.textMuted
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = true,
            beyondViewportPageCount = 1
        ) { page ->
            content(page)
        }
    }
}
