package com.rootdeck.https_browser.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ChromeBlueDark,
    onPrimary = SurfaceDark,
    primaryContainer = ChromeBlue,
    onPrimaryContainer = TextPrimaryDark,
    secondary = ChromeBlueDark,
    onSecondary = SurfaceDark,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = ChromeBlue,
    onPrimary = SurfaceLight,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF174EA6),
    secondary = ChromeBlue,
    onSecondary = SurfaceLight,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight
)

private val IncognitoColorScheme = darkColorScheme(
    primary = ChromeBlueDark,
    onPrimary = IncognitoSurface,
    primaryContainer = ChromeBlue,
    onPrimaryContainer = IncognitoText,
    secondary = ChromeBlueDark,
    onSecondary = IncognitoSurface,
    background = IncognitoSurface,
    onBackground = IncognitoText,
    surface = IncognitoSurface,
    onSurface = IncognitoText,
    surfaceVariant = IncognitoSurfaceVariant,
    onSurfaceVariant = IncognitoIcon,
    outline = BorderDark
)

@Composable
fun ChromeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isIncognito -> IncognitoColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.surface.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme && !isIncognito
                insetsController.isAppearanceLightNavigationBars = !darkTheme && !isIncognito
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
