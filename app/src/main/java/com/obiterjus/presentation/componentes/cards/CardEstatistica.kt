package com.obiterjus.presentation.componentes.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun CardEstatistica(
    valor: String,
    rotulo: String,
    corPonto: Color,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colorScheme = MaterialTheme.colorScheme
    val colors = ObiterTheme.colors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimens.statCardRadius),
        color = colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(dimens.cardPaddingV, dimens.cardPaddingH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.space1),
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.statDotSize)
                    .clip(CircleShape)
                    .background(corPonto),
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
            )
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
        }
    }
}
