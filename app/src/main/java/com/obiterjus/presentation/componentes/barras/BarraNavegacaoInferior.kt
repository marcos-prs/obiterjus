package com.obiterjus.presentation.componentes.barras

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obiterjus.presentation.navegacao.ObiterRota
import com.obiterjus.ui.theme.Inter
import com.obiterjus.ui.theme.ObiterTheme

data class ItemNavegacao(
    val rota: ObiterRota,
    @param:StringRes val rotuloResId: Int,
    val iconeAtivo: ImageVector,
    val iconeInativo: ImageVector,
)

@Composable
fun BarraNavegacaoObiter(
    abas: List<ItemNavegacao>,
    abaAtual: ObiterRota,
    aoSelecionar: (ObiterRota) -> Unit,
    contagemBadges: Map<ObiterRota, Int>,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val dividerColor = colors.divider

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        containerColor = colorScheme.surface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars,
    ) {
        abas.forEach { item ->
            val selecionado = item.rota == abaAtual
            val badgeCount = contagemBadges[item.rota] ?: 0

            NavigationBarItem(
                selected = selecionado,
                onClick = { aoSelecionar(item.rota) },
                icon = {
                    Box(contentAlignment = Alignment.TopCenter) {
                        if (selecionado) {
                            Box(
                                modifier = Modifier
                                    .offset(y = (-4).dp)
                                    .size(
                                        width = dimens.navPillWidth,
                                        height = dimens.navPillHeight,
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .background(colorScheme.primary),
                            )
                        }

                        BadgedBox(
                            badge = {
                                if (badgeCount > 0) {
                                    Badge(
                                        containerColor = colors.danger,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(dimens.badgeNavSize),
                                    ) {
                                        Text(
                                            text = badgeCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (selecionado) item.iconeAtivo else item.iconeInativo,
                                contentDescription = stringResource(item.rotuloResId),
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = stringResource(item.rotuloResId),
                        style = if (selecionado) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.labelSmall.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorScheme.primary,
                    selectedTextColor = colorScheme.primary,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
