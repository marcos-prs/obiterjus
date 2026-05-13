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
import com.obiterjus.domain.model.CorPontoTimeline
import com.obiterjus.ui.theme.ObiterTheme

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
        CorPontoTimeline.WARNING -> colors.warning
        CorPontoTimeline.PRIMARY -> colorScheme.primary
        CorPontoTimeline.DESPACHO -> colors.despacho
        CorPontoTimeline.SUCCESS -> colors.success
        CorPontoTimeline.ACCENT -> colorScheme.secondary
        CorPontoTimeline.MUTED -> colors.textMuted
    }
    val lineColor = colors.divider
    val dotRadius = dimens.timelineDotSize / 2

    Row(
        modifier = modifier.height(IntrinsicSize.Min),
    ) {
        // Coluna dot + linha
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .width(dimens.timelineDotSize)
                    .height(dimens.timelineDotSize + 8.dp)
            ) {
                val centerX = size.width / 2
                val dotY = dotRadius.toPx() + 4.dp.toPx()

                // Dot
                drawCircle(
                    color = dotColor,
                    radius = dotRadius.toPx(),
                    center = Offset(centerX, dotY),
                )
            }

            // Linha vertical
            if (mostrarLinha) {
                Canvas(
                    modifier = Modifier
                        .width(1.dp)
                        .weight(1f)
                ) {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                        ),
                    )
                }
            }
        }

        // Coluna conteúdo
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = dimens.cardPaddingV),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Linha 1: data
            Text(
                text = data,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            
            // Linha 2: título
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = colorScheme.onSurface,
            )
            
            // Linha 3 (opcional): descrição
            if (!detalhe.isNullOrBlank()) {
                Text(
                    text = detalhe,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }
    }
}
