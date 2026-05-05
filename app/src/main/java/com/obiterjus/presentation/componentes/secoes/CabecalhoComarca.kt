package com.obiterjus.presentation.componentes.secoes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun CabecalhoComarca(
    nomeComarca: String,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimens.chipRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colors.divider,
            thickness = dimens.borderWidth,
        )
        Text(
            text = nomeComarca.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.77.sp,
            ),
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = dimens.cardPaddingH),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = colors.divider,
            thickness = dimens.borderWidth,
        )
    }
}
