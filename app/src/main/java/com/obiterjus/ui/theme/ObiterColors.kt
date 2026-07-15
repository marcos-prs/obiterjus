package com.obiterjus.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens de cor estendidos que não possuem equivalente direto
 * no [androidx.compose.material3.ColorScheme].
 *
 * Acessíveis via [ObiterTheme.colors].
 */
@Immutable
data class ObiterExtendedColors(
    val accent: Color,
    val accentPale: Color,
    val danger: Color,
    val dangerPale: Color,
    val success: Color,
    val successPale: Color,
    val warning: Color,
    val warningPale: Color,
    val despacho: Color,
    val surfacePergaminho: Color,
    val primaryDark: Color,
    val primaryPale: Color,
    val textMuted: Color,
    val border: Color,
    val divider: Color,
    val mulledWine: Color,
    val tribunalBadgeBackground: Color,
    val tribunalBadgeText: Color,
    val topAppBarBackground: Color,
    val onTopAppBar: Color,
    val topAppBarAccent: Color,
)

fun obiterLightColors(): ObiterExtendedColors = ObiterExtendedColors(
    accent = Husk,
    accentPale = HuskPale,
    danger = CherryRed,
    dangerPale = CherryRedPale,
    success = Tiber,
    successPale = TiberPale,
    warning = HuskDark,
    despacho = OliveGreen,
    warningPale = WarningPale,
    surfacePergaminho = Pergaminho,
    primaryDark = SanJuanDark,
    primaryPale = SanJuanPale,
    textMuted = Color(0xFF8FA0B0),
    border = Border,
    divider = Divider,
    mulledWine = MulledWine,
    tribunalBadgeBackground = SanJuan,
    tribunalBadgeText = Color.White,
    topAppBarBackground = SanJuan,
    onTopAppBar = Husk,
    topAppBarAccent = Husk,
)

fun obiterDarkColors(): ObiterExtendedColors = ObiterExtendedColors(
    accent = HuskLight,
    accentPale = DarkWarningPale,
    danger = CherryRedLight,
    dangerPale = DarkDangerPale,
    success = OliveGreen,
    successPale = DarkSuccessPale,
    warning = HuskLight,
    warningPale = DarkWarningPale,
    despacho = OliveGreen,
    surfacePergaminho = DarkPergaminho,
    primaryDark = SanJuanLighter,
    primaryPale = DarkPrimaryContainer,
    textMuted = Color(0xFF4A5E6A),  // Dark muted text color
    border = DarkBorder,
    divider = DarkDivider,
    mulledWine = MulledWineLight,
    tribunalBadgeBackground = DarkPrimaryContainer,
    tribunalBadgeText = DarkTribunalBadgeText,
    topAppBarBackground = Color(0xFF111111),  // Dark top bar background
    onTopAppBar = Color(0xFFC9AA70),  // Light text/icon color for dark top bar
    topAppBarAccent = Husk,
)

val LocalObiterColors = staticCompositionLocalOf { obiterLightColors() }
