package com.obiterjus.presentation.componentes.strips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun AlertaStrip(
    mensagem: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    val dangerColor = colors.danger
    val borderWidthPx = dimens.alertStripBorder

    Surface(
        modifier = modifier
            .padding(horizontal = dimens.alertStripMargin)
            .fillMaxWidth()
            .clickable(onClick = aoClicar),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = dimens.alertStripRightRadius,
            bottomEnd = dimens.alertStripRightRadius,
            bottomStart = 0.dp,
        ),
        color = colors.dangerPale,
    ) {
        Text(
            text = mensagem,
            style = MaterialTheme.typography.titleSmall,
            color = colors.danger,
            modifier = Modifier
                .drawBehind {
                    drawRect(
                        color = dangerColor,
                        topLeft = Offset.Zero,
                        size = Size(borderWidthPx.toPx(), size.height),
                    )
                }
                .padding(
                    start = dimens.alertStripBorder + 8.dp,
                    end = 8.dp,
                    top = dimens.sectionGap,
                    bottom = dimens.sectionGap,
                ),
        )
    }
}
