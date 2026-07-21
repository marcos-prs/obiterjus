package com.obiterjus.presentation.componentes.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun CardCliente(
    nome: String,
    documento: String?,
    rotuloTipoPessoa: String,
    quantidadeProcessos: Int,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    representante: String? = null,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) colors.primaryPale else colorScheme.surface,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "clientCardPress",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = aoClicar,
            ),
        shape = RoundedCornerShape(dimens.cardRadius),
        color = containerColor,
        border = BorderStroke(dimens.borderWidth, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimens.settingsItemPaddingH,
                vertical = dimens.settingsItemPaddingV,
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ObiterIcones.Cliente,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(dimens.iconStarSize),
                )
                Text(
                    text = nome,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                BadgeTipoAto(
                    texto = rotuloTipoPessoa,
                    variante = VarianteBadge.TRIBUNAL,
                )
            }

            Text(
                text = documento?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.clientes_sem_documento),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = if (documento.isNullOrBlank()) FontStyle.Italic else FontStyle.Normal,
                ),
                color = colorScheme.onSurfaceVariant,
            )

            if (!representante.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.clientes_representado_por, representante),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = pluralStringResource(
                    R.plurals.clientes_quantidade_processos,
                    quantidadeProcessos,
                    quantidadeProcessos,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}
