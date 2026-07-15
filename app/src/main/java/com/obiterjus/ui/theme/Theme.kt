package com.obiterjus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.core.view.WindowCompat

enum class TipoTema {
    SISTEMA, CLARO, ESCURO
}

// ── Material 3 Color Schemes ────────────────────────────────

val ObiterLightColorScheme = lightColorScheme(
    primary = SanJuan,
    onPrimary = Color.White,
    primaryContainer = SanJuanPale,
    onPrimaryContainer = SanJuanDark,
    secondary = Husk,
    onSecondary = Color.White,
    secondaryContainer = HuskPale,
    onSecondaryContainer = HuskDark,
    tertiary = MulledWine,
    onTertiary = Color.White,
    tertiaryContainer = SanJuanPale,
    onTertiaryContainer = MulledWine,
    background = CoolWhite,
    onBackground = Grafite,
    surface = CardWhite,
    onSurface = Grafite,
    surfaceVariant = CoolWhite,
    onSurfaceVariant = SteelGray,
    surfaceContainerHighest = ElevatedWhite,
    outline = Border,
    outlineVariant = Divider,
    error = CherryRed,
    onError = Color.White,
    errorContainer = CherryRedPale,
    onErrorContainer = CherryRed,
    inverseSurface = Grafite,
    inverseOnSurface = CoolWhite,
    inversePrimary = SanJuanLight,
    scrim = Color.Black,
)

val ObiterDarkColorScheme = darkColorScheme(
    primary = SanJuanLight,
    onPrimary = DarkCard,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = SanJuanLighter,
    secondary = HuskLight,
    onSecondary = DarkCard,
    secondaryContainer = DarkWarningPale,
    onSecondaryContainer = HuskLight,
    tertiary = MulledWineLight,
    onTertiary = DarkCard,
    tertiaryContainer = MulledWineDark,
    onTertiaryContainer = MulledWineLight,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerHighest = DarkElevated,
    outline = DarkBorder,
    outlineVariant = DarkDivider,
    error = CherryRedLight,
    onError = DarkCard,
    errorContainer = DarkDangerPale,
    onErrorContainer = CherryRedLight,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBackground,
    inversePrimary = SanJuan,
    scrim = Color.Black,
)

// ── Theme composable ────────────────────────────────────────

@Composable
fun ObiterJusTheme(
    tema: TipoTema = TipoTema.SISTEMA,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (tema) {
        TipoTema.SISTEMA -> isSystemInDarkTheme()
        TipoTema.CLARO -> false
        TipoTema.ESCURO -> true
    }
    val colorScheme = if (darkTheme) ObiterDarkColorScheme else ObiterLightColorScheme
    val extendedColors = if (darkTheme) obiterDarkColors() else obiterLightColors()
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = extendedColors.topAppBarBackground.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    CompositionLocalProvider(
        LocalObiterColors provides extendedColors,
        LocalObiterDimens provides ObiterDimens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ObiterTypography,
            content = content,
        )
    }
}

// ── Accessor object ─────────────────────────────────────────

object ObiterTheme {
    val dimens: ObiterDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalObiterDimens.current

    val colors: ObiterExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalObiterColors.current
}
