package com.obiterjus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme = lightColorScheme(
    primary = Tiber,
    onPrimary = CardWhite,
    primaryContainer = BadgeLight,
    onPrimaryContainer = Tiber,
    secondary = Husk,
    onSecondary = Ink,
    secondaryContainer = BadgeLight,
    onSecondaryContainer = Tiber,
    tertiary = Husk,
    onTertiary = Ink,
    tertiaryContainer = BadgeLight,
    onTertiaryContainer = Tiber,
    background = CoolWhite,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = BadgeLight,
    onSurfaceVariant = MutedLight,
    outline = OutlineLight,
    outlineVariant = BadgeLight,
    error = ErrorLight,
    onError = CardWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = OliveGreen,
    onPrimary = Graphite,
    primaryContainer = BadgeDark,
    onPrimaryContainer = BadgeLight,
    secondary = Husk,
    onSecondary = Ink,
    secondaryContainer = BadgeDark,
    onSecondaryContainer = BadgeLight,
    tertiary = Husk,
    onTertiary = Ink,
    tertiaryContainer = BadgeDark,
    onTertiaryContainer = BadgeLight,
    background = Graphite,
    onBackground = Paper,
    surface = CardGraphite,
    onSurface = Paper,
    surfaceVariant = BadgeDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
    outlineVariant = BadgeDark,
    error = ErrorDark,
    onError = Ink,
)

@Composable
fun ObiterJusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalObiterDimens provides ObiterDimens()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ObiterTypography,
            content = content,
        )
    }
}

object ObiterTheme {
    val dimens: ObiterDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalObiterDimens.current
}
