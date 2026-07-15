package com.obiterjus.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens de dimensão do ObiterJus — Design System v1.0.
 *
 * Grid de 4dp. Margem lateral de tela: 10dp. Gap entre cards: 7dp.
 * Acessíveis via [ObiterTheme.dimens].
 */
@Immutable
data class ObiterDimens(
    // ── Margens e paddings ───────────────────────────────────
    val screenMargin: Dp = 10.dp,
    val cardGap: Dp = 7.dp,
    val sectionGap: Dp = 6.dp,
    val chipRowGap: Dp = 5.dp,
    val cardPaddingH: Dp = 12.dp,
    val cardPaddingV: Dp = 10.dp,
    val topAppBarPaddingH: Dp = 12.dp,
    val topAppBarHeight: Dp = 84.dp,
    val bottomNavHeight: Dp = 60.dp,
    val bottomNavIconLabelGap: Dp = 1.dp,
    val settingsGroupTopMargin: Dp = 16.dp,
    val settingsItemPaddingV: Dp = 7.dp,
    val settingsItemPaddingH: Dp = 9.dp,
    val alertStripMargin: Dp = 10.dp,
    val searchBarHeight: Dp = 48.dp,
    val chipHeight: Dp = 28.dp,
    val chipPaddingH: Dp = 12.dp,
    val badgePaddingV: Dp = 2.dp,
    val badgePaddingH: Dp = 6.dp,

    // ── Radii ────────────────────────────────────────────────
    val cardRadius: Dp = 10.dp,
    val statCardRadius: Dp = 8.dp,
    val badgeRadius: Dp = 4.dp,
    val chipRadius: Dp = 20.dp,
    val searchBarRadius: Dp = 7.dp,
    val toggleRadius: Dp = 8.dp,
    val tribunalHeaderRadius: Dp = 7.dp,
    val alertStripRightRadius: Dp = 6.dp,
    val settingsGroupRadius: Dp = 8.dp,

    // ── Elementos visuais ────────────────────────────────────
    val stripeWidth: Dp = 4.dp,
    val navPillWidth: Dp = 32.dp,
    val navPillHeight: Dp = 3.dp,
    val timelineDotSize: Dp = 9.dp,
    val statDotSize: Dp = 6.dp,
    val avatarSize: Dp = 40.dp,
    val toggleWidth: Dp = 28.dp,
    val toggleHeight: Dp = 15.dp,
    val toggleThumbSize: Dp = 12.dp,
    val badgeNavSize: Dp = 14.dp,

    // ── Ícones ───────────────────────────────────────────────
    val iconChevronSize: Dp = 14.dp,
    val iconSearchSize: Dp = 14.dp,
    val iconWarningSize: Dp = 16.dp,
    val iconStarSize: Dp = 16.dp,
    val iconBackSize: Dp = 24.dp,

    // ── Bordas ───────────────────────────────────────────────
    val borderWidth: Dp = 1.dp,
    val alertStripBorder: Dp = 3.dp,

    // ── Aliases de compatibilidade (remover ao refatorar telas) ──
    val space0: Dp = 0.dp,
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space5: Dp = 20.dp,
    val space6: Dp = 24.dp,
    val space8: Dp = 32.dp,
    val space10: Dp = 40.dp,
    val space12: Dp = 48.dp,
    val space14: Dp = 56.dp,
    val space16: Dp = 64.dp,
    val radiusSmall: Dp = cardRadius,
    val radiusMedium: Dp = 12.dp,
    val radiusExtraSmall: Dp = badgeRadius,
    val strokeThin: Dp = borderWidth,
    val strokeMedium: Dp = 2.dp,
    val bottomBarHeight: Dp = bottomNavHeight,
    val appBarHeight: Dp = topAppBarHeight,
    val monitoramentoUfFieldWidth: Dp = 96.dp,
)
val LocalObiterDimens = staticCompositionLocalOf { ObiterDimens() }
