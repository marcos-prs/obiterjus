package com.obiterjus.presentation.componentes.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.ui.theme.ObiterTheme

enum class PrioridadeStripe { CRITICA, SENTENCA, DECISAO, ROTINEIRO, FAVORAVEL }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardPublicacao(
    tituloAto: String,
    tipoAto: String,
    data: String,
    tribunal: String? = null,
    juizo: String? = null,
    numeroProcesso: String,
    prazoDias: String?,
    trechoTexto: String?,
    prioridade: PrioridadeStripe,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    badgeOrdem: String? = null,
    onVerDetalhes: () -> Unit = {},
    mostrarBotaoDetalhes: Boolean = false,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor by animateColorAsState(
        targetValue = if (isPressed) colors.primaryPale else colorScheme.surface,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "cardPress",
    )

    val stripeColor = when (prioridade) {
        PrioridadeStripe.CRITICA -> colors.danger
        PrioridadeStripe.SENTENCA -> colorScheme.secondary
        PrioridadeStripe.DECISAO -> colorScheme.primary
        PrioridadeStripe.ROTINEIRO -> colors.divider
        PrioridadeStripe.FAVORAVEL -> colors.success
    }

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
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Stripe lateral
            Box(
                modifier = Modifier
                    .width(dimens.stripeWidth)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = dimens.cardRadius,
                            bottomStart = dimens.cardRadius,
                        ),
                    )
                    .background(stripeColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = dimens.cardPaddingH,
                        vertical = dimens.cardPaddingV,
                    ),
                verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
            ) {
                // Linha 1: ordem, tribunal e data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                        verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (!badgeOrdem.isNullOrBlank()) {
                            BadgeTipoAto(
                                texto = badgeOrdem,
                                variante = VarianteBadge.DESPACHO,
                            )
                        }
                        BadgeTipoAto(
                            texto = tribunal?.takeIf { it.isNotBlank() } ?: tipoAto,
                            variante = VarianteBadge.TRIBUNAL,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }

                // Linha 2: tipo do ato
                Text(
                    text = tituloAto,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!tribunal.isNullOrBlank()) {
                    Text(
                        text = "Tribunal: $tribunal",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!juizo.isNullOrBlank()) {
                    Text(
                        text = "Juízo: $juizo",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = "Tipo do ato: $tipoAto",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Trecho opcional, mantido para telas antigas.
                if (!trechoTexto.isNullOrBlank()) {
                    Text(
                        text = trechoTexto,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Linha 4: chips rodapé
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                    verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                ) {
                    BadgeTipoAto(
                        texto = numeroProcesso.formatarCnj(),
                        variante = VarianteBadge.DESPACHO,
                    )
                    if (!prazoDias.isNullOrBlank()) {
                        BadgeTipoAto(
                            texto = prazoDias,
                            variante = VarianteBadge.URGENTE,
                        )
                    }
                }

                // Linha 5: Ver Detalhes (Opcional)
                if (mostrarBotaoDetalhes) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "VER DETALHES",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onVerDetalhes() }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun prioridadeParaVariante(prioridade: PrioridadeStripe): VarianteBadge =
    when (prioridade) {
        PrioridadeStripe.CRITICA -> VarianteBadge.URGENTE
        PrioridadeStripe.SENTENCA -> VarianteBadge.SENTENCA
        PrioridadeStripe.DECISAO -> VarianteBadge.DECISAO
        PrioridadeStripe.ROTINEIRO -> VarianteBadge.DESPACHO
        PrioridadeStripe.FAVORAVEL -> VarianteBadge.FAVORAVEL
    }
