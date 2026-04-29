package com.obiterjus.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ObiterDimens(
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
    val radiusNone: Dp = 0.dp,
    val radiusExtraSmall: Dp = 4.dp,
    val radiusSmall: Dp = 8.dp,
    val radiusMedium: Dp = 12.dp,
    val radiusLarge: Dp = 16.dp,
    val radiusExtraLarge: Dp = 28.dp,
    val radiusFull: Dp = 999.dp,
    val strokeThin: Dp = 1.dp,
    val strokeMedium: Dp = 2.dp,
    val iconSmall: Dp = 18.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val touchTargetMin: Dp = 48.dp,
    val appBarHeight: Dp = 64.dp,
    val bottomBarHeight: Dp = 80.dp,
    val contentMaxWidth: Dp = 720.dp,
    val cardMinHeight: Dp = 72.dp,
)

val LocalObiterDimens = staticCompositionLocalOf { ObiterDimens() }
