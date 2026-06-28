package com.chathala.hala.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = HalaPrimaryLight,
    onPrimary = HalaTextInvertedLight,
    primaryContainer = HalaInputLight,
    onPrimaryContainer = HalaPrimaryLight,

    secondary = HalaSecondaryLight,
    onSecondary = HalaTextInvertedLight,
    secondaryContainer = HalaDividerLight,
    onSecondaryContainer = HalaSecondaryLight,

    tertiary = HalaAccentLight,
    onTertiary = HalaTextInvertedLight,

    background = HalaBgLight,
    onBackground = HalaTextPrimaryLight,

    surface = HalaCardLight,
    onSurface = HalaTextPrimaryLight,

    surfaceVariant = HalaInputLight,
    onSurfaceVariant = HalaTextSecondaryLight,

    surfaceContainer = HalaCardLight,
    surfaceContainerHigh = HalaInputLight,

    outline = HalaBorderLight,
    outlineVariant = HalaDividerLight,

    error = HalaErrorLight,
    onError = HalaTextInvertedLight,
    errorContainer = HalaInputLight,
    onErrorContainer = HalaErrorLight
)

private val DarkColors = darkColorScheme(
    primary = HalaPrimaryDark,
    onPrimary = HalaTextInvertedDark,
    primaryContainer = HalaInputDark,
    onPrimaryContainer = HalaPrimaryDark,

    secondary = HalaSecondaryDark,
    onSecondary = HalaTextInvertedDark,
    secondaryContainer = HalaDividerDark,
    onSecondaryContainer = HalaSecondaryDark,

    tertiary = HalaAccentDark,
    onTertiary = HalaTextInvertedDark,

    background = HalaBgDark,
    onBackground = HalaTextPrimaryDark,

    surface = HalaCardDark,
    onSurface = HalaTextPrimaryDark,

    surfaceVariant = HalaInputDark,
    onSurfaceVariant = HalaTextSecondaryDark,

    surfaceContainer = HalaCardDark,
    surfaceContainerHigh = HalaInputDark,

    outline = HalaBorderDark,
    outlineVariant = HalaDividerDark,

    error = HalaErrorDark,
    onError = HalaTextInvertedDark,
    errorContainer = HalaInputDark,
    onErrorContainer = HalaErrorDark
)

@Composable
fun HalaTheme(
    themeMode: com.chathala.hala.core.storage.AppTheme =
        com.chathala.hala.core.storage.AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        com.chathala.hala.core.storage.AppTheme.LIGHT -> false
        com.chathala.hala.core.storage.AppTheme.DARK -> true
        com.chathala.hala.core.storage.AppTheme.SYSTEM -> systemDark
    }

    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = HalaTypography,
        shapes = HalaShapes,
        content = content
    )
}
