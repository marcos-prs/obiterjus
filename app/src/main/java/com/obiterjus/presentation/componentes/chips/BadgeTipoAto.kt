package com.obiterjus.presentation.componentes.chips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.obiterjus.ui.theme.ObiterTheme

enum class VarianteBadge { URGENTE, SENTENCA, DECISAO, DESPACHO, FAVORAVEL, TRIBUNAL }

@Composable
fun BadgeTipoAto(
    texto: String,
    variante: VarianteBadge,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val (fundo, textoCor) = when (variante) {
        VarianteBadge.URGENTE -> colors.dangerPale to colors.danger
        VarianteBadge.SENTENCA -> colors.accentPale to colors.warning
        VarianteBadge.DECISAO -> colors.primaryPale to colors.primaryDark
        VarianteBadge.DESPACHO -> colors.divider to colorScheme.onSurfaceVariant
        VarianteBadge.FAVORAVEL -> colors.successPale to colors.success
        VarianteBadge.TRIBUNAL -> colorScheme.primary to Color.White
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimens.badgeRadius),
        color = fundo,
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                color = textoCor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = dimens.badgePaddingH,
                    vertical = dimens.badgePaddingV,
                ),
            )
        }
    }
}
