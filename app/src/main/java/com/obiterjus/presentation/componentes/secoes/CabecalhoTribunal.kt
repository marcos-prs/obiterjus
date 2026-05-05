package com.obiterjus.presentation.componentes.secoes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun CabecalhoTribunal(
    siglaTribunal: String,
    contagem: Int,
    expandido: Boolean,
    corFundoBadge: Color,
    aoAlternar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val isDark = isSystemInDarkTheme()

    val chevronRotation by animateFloatAsState(
        targetValue = if (expandido) 0f else 180f,
        label = "chevronRotation",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = aoAlternar),
        shape = RoundedCornerShape(dimens.tribunalHeaderRadius),
        color = colors.primaryPale,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimens.settingsItemPaddingH,
                vertical = dimens.sectionGap,
            ),
            horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Badge tribunal com cor customizada
            Surface(
                shape = RoundedCornerShape(dimens.badgeRadius),
                color = if (isDark) colors.tribunalBadgeBackground else corFundoBadge,
            ) {
                Text(
                    text = siglaTribunal,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isDark) colors.tribunalBadgeText else Color.White,
                    modifier = Modifier.padding(
                        horizontal = dimens.badgePaddingH,
                        vertical = dimens.badgePaddingV,
                    ),
                )
            }

            Text(
                text = stringResource(R.string.comarca_contagem_formato, contagem),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.weight(1f),
            )

            Icon(
                imageVector = ObiterIcones.ExpandirAbaixo,
                contentDescription = stringResource(
                    if (expandido) R.string.cd_recolher else R.string.cd_expandir,
                ),
                tint = colors.textMuted,
                modifier = Modifier
                    .size(dimens.iconChevronSize)
                    .rotate(chevronRotation),
            )
        }
    }
}
