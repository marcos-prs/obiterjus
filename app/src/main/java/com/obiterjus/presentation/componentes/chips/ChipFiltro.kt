package com.obiterjus.presentation.componentes.chips

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.obiterjus.ui.theme.Inter
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun ChipFiltro(
    texto: String,
    ativo: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = if (ativo) colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (ativo) Color.White else colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "chipText",
    )

    Surface(
        modifier = modifier
            .height(dimens.chipHeight)
            .clickable(onClick = aoClicar),
        shape = RoundedCornerShape(dimens.chipRadius),
        color = containerColor,
        border = if (ativo) null else BorderStroke(dimens.borderWidth, colors.border),
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = texto,
                style = if (ativo) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                    )
                },
                color = contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = dimens.chipPaddingH),
            )
        }
    }
}
