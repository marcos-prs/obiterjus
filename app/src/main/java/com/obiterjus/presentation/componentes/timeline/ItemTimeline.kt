package com.obiterjus.presentation.componentes.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.obiterjus.ui.theme.ObiterTheme

enum class CorPontoTimeline { DANGER, ACCENT, PRIMARY, MUTED, SUCCESS }

@Composable
fun ItemTimeline(
    data: String,
    titulo: String,
    detalhe: String?,
    corPonto: CorPontoTimeline,
    mostrarLinha: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val dotColor = when (corPonto) {
        CorPontoTimeline.DANGER -> colors.danger
        CorPontoTimeline.ACCENT -> colorScheme.secondary
        CorPontoTimeline.PRIMARY -> colorScheme.primary
        CorPontoTimeline.MUTED -> colors.textMuted
        CorPontoTimeline.SUCCESS -> colors.success
    }
    val lineColor = colors.divider
    val dotRadius = dimens.timelineDotSize / 2

    Row(
        modifier = modifier.height(IntrinsicSize.Min),
    ) {
        // Coluna data
        Text(
            text = data,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.width(52.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Coluna dot + linha
        Canvas(
            modifier = Modifier
                .width(dimens.timelineDotSize + 8.dp)
                .fillMaxHeight(),
        ) {
            val centerX = size.width / 2
            val dotY = dotRadius.toPx() + 4.dp.toPx()

            // Dot
            drawCircle(
                color = dotColor,
                radius = dotRadius.toPx(),
                center = Offset(centerX, dotY),
            )

            // Linha tracejada
            if (mostrarLinha) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, dotY + dotRadius.toPx() + 2.dp.toPx()),
                    end = Offset(centerX, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                    ),
                )
            }
        }

        // Coluna conteúdo
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = dimens.cardPaddingV),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
            )
            if (!detalhe.isNullOrBlank()) {
                Text(
                    text = detalhe,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
