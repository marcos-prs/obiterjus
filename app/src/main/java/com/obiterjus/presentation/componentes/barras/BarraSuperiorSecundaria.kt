package com.obiterjus.presentation.componentes.barras

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun BarraSuperiorSecundaria(
    titulo: String,
    subtitulo: String? = null,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) colorScheme.onSurface else colorScheme.onPrimary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isDark) {
                    Modifier.drawBehind {
                        drawLine(
                            color = colors.divider,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                }
            ),
        color = if (isDark) colorScheme.surface else colorScheme.primary,
        shadowElevation = if (isDark) 0.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = dimens.space1,
                    end = dimens.topAppBarPaddingH,
                    top = dimens.cardPaddingV,
                    bottom = dimens.cardPaddingV,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = ObiterIcones.Voltar,
                    contentDescription = stringResource(R.string.cd_voltar),
                    tint = textColor.copy(alpha = 0.60f),
                    modifier = Modifier.size(dimens.iconBackSize),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                )
                if (subtitulo != null) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.50f),
                    )
                }
            }
        }
    }
}
