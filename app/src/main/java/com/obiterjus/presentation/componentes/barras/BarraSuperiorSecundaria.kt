package com.obiterjus.presentation.componentes.barras

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.texto.TextoAjustavelUmaLinha
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun BarraSuperiorSecundaria(
    titulo: String,
    subtitulo: String? = null,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    acoes: @Composable RowScope.() -> Unit = {},
    mostrarVoltar: Boolean = true,
    corFundo: Color = ObiterTheme.colors.topAppBarBackground,
    corConteudo: Color = ObiterTheme.colors.onTopAppBar,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val lightTopBarText = Color(0xFFF4F6F8)
    val lightTopBarMuted = Color(0xFFDAE2E8)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.topAppBarHeight)
            .then(
                if (corFundo == colors.topAppBarBackground) {
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
        color = corFundo,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = if (mostrarVoltar) dimens.space1 else dimens.topAppBarPaddingH,
                    end = dimens.topAppBarPaddingH,
                    top = dimens.cardPaddingV,
                    bottom = dimens.cardPaddingV,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mostrarVoltar) {
                IconButton(onClick = onVoltar) {
                    Icon(
                        imageVector = ObiterIcones.Voltar,
                        contentDescription = stringResource(R.string.cd_voltar),
                        tint = lightTopBarMuted,
                        modifier = Modifier.size(dimens.iconBackSize),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                TextoAjustavelUmaLinha(
                    texto = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = lightTopBarText,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (subtitulo != null) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = lightTopBarMuted,
                    )
                }
            }

            acoes()
        }
    }
}
