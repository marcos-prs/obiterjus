package com.obiterjus.presentation.componentes.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.presentation.componentes.texto.TextoAjustavelUmaLinha
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.ui.theme.ObiterTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardProcesso(
    numeroProcesso: String,
    partes: String?,
    badges: List<String>,
    temPrazoAtivo: Boolean,
    ultimaMovimentacao: String,
    fonte: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    cliente: String? = null,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) colors.primaryPale else colorScheme.surface,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "processCardPress",
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
            // Linha 1: número + ícones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextoAjustavelUmaLinha(
                    texto = numeroProcesso.formatarCnj(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (temPrazoAtivo) {
                        Icon(
                            imageVector = ObiterIcones.Aviso,
                            contentDescription = stringResource(R.string.cd_prazo_ativo),
                            tint = colors.danger,
                            modifier = Modifier.size(dimens.iconStarSize),
                        )
                    }
                    Icon(
                        imageVector = ObiterIcones.ExpandirAbaixo,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dimens.iconStarSize),
                    )
                }
            }

            // Linha 2: partes
            Text(
                text = partes ?: stringResource(R.string.processos_partes_indisponiveis),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = if (partes == null) FontStyle.Italic else FontStyle.Normal,
                ),
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Linha 2b: cliente representado
            if (!cliente.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                ) {
                    Icon(
                        imageVector = ObiterIcones.Cliente,
                        contentDescription = stringResource(R.string.detalhe_chip_cliente),
                        tint = colors.accent,
                        modifier = Modifier.size(dimens.iconStarSize),
                    )
                    Text(
                        text = cliente,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Linha 3: badges
            if (badges.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                    verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                ) {
                    badges.forEachIndexed { index, badge ->
                        BadgeTipoAto(
                            texto = badge,
                            variante = varianteParaBadgeProcesso(index),
                        )
                    }
                }
            }

            // Divisor + rodapé
            HorizontalDivider(
                color = colors.divider,
                thickness = dimens.borderWidth,
            )

            Text(
                text = stringResource(
                    R.string.processos_rodape_formato,
                    ultimaMovimentacao,
                    fonte,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

private fun varianteParaBadgeProcesso(index: Int): VarianteBadge =
    when (index) {
        0 -> VarianteBadge.DECISAO
        1 -> VarianteBadge.TRIBUNAL
        else -> VarianteBadge.DESPACHO
    }
