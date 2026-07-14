package com.alhaq.amnshield.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alhaq.amnshield.ui.state.AppTheme

// Light dynamic fallback scheme
private val DefaultColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Sunset Glow Custom Scheme
private val SunsetColorScheme = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color.White,
    primaryContainer = SunsetPrimaryContainer,
    onPrimaryContainer = Color(0xFF3C1E11),
    background = SunsetBg,
    onBackground = SunsetOnSurface,
    surface = SunsetSurface,
    onSurface = SunsetOnSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onSurfaceVariant = SunsetTextMuted,
    outline = SunsetOutline,
    outlineVariant = SunsetOutline
)

// Emerald Calm Custom Scheme
private val EmeraldColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = Color(0xFF142921),
    background = EmeraldBg,
    onBackground = EmeraldOnSurface,
    surface = EmeraldSurface,
    onSurface = EmeraldOnSurface,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = EmeraldTextMuted,
    outline = EmeraldOutline,
    outlineVariant = EmeraldOutline
)

// Cosmic Night Custom Scheme
private val CosmicColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    onPrimary = Color(0xFF2B1450),
    primaryContainer = CosmicPrimaryContainer,
    onPrimaryContainer = CosmicOnSurface,
    background = CosmicBg,
    onBackground = CosmicOnSurface,
    surface = CosmicSurface,
    onSurface = CosmicOnSurface,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = CosmicTextMuted,
    outline = CosmicOutline,
    outlineVariant = CosmicOutline
)

@Composable
fun AmnShieldTheme(
    appTheme: AppTheme = AppTheme.SUNSET_GLOW,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.SUNSET_GLOW -> SunsetColorScheme
        AppTheme.EMERALD_CALM -> EmeraldColorScheme
        AppTheme.COSMIC_NIGHT -> CosmicColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
