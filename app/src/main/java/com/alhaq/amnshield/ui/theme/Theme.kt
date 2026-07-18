package com.alhaq.amnshield.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
    appTheme: AppTheme = AppTheme.EMERALD_CALM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.SUNSET_GLOW -> SunsetColorScheme
        AppTheme.EMERALD_CALM -> EmeraldColorScheme
        AppTheme.COSMIC_NIGHT -> CosmicColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Set system status and navigation bar colors to match the theme background
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()

                // Set light/dark system bar icons accordingly (Cosmic Night is dark theme, others light)
                val insetsController = WindowCompat.getInsetsController(window, view)
                val isDarkTheme = appTheme == AppTheme.COSMIC_NIGHT
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
