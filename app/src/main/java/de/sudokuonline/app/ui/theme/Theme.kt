package de.sudokuonline.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import de.sudokuonline.app.data.repository.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SecondaryDark,
    onSecondary = BackgroundDark,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = Player2ColorDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CellDefaultDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorColor,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryLight,
    onSecondary = SurfaceLight,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Player2Color,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = CellDefault,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    onError = SurfaceLight
)

/**
 * Creates a color scheme from an AppTheme
 */
private fun createColorScheme(theme: AppTheme, darkTheme: Boolean) = if (darkTheme) {
    darkColorScheme(
        primary = theme.primaryDark,
        onPrimary = theme.backgroundDark,
        primaryContainer = theme.primaryLight.copy(alpha = 0.3f),
        onPrimaryContainer = theme.primaryDark,
        secondary = theme.secondaryDark,
        onSecondary = theme.backgroundDark,
        secondaryContainer = theme.secondaryLight.copy(alpha = 0.3f),
        onSecondaryContainer = theme.secondaryDark,
        tertiary = theme.tertiaryDark,
        onTertiary = theme.backgroundDark,
        tertiaryContainer = theme.tertiaryLight.copy(alpha = 0.3f),
        onTertiaryContainer = theme.tertiaryDark,
        background = theme.backgroundDark,
        onBackground = Color.White.copy(alpha = 0.87f),
        surface = theme.surfaceDark,
        onSurface = Color.White.copy(alpha = 0.87f),
        surfaceVariant = theme.surfaceDark.copy(alpha = 0.7f),
        onSurfaceVariant = Color.White.copy(alpha = 0.6f),
        error = ErrorColor,
        onError = Color.White
    )
} else {
    lightColorScheme(
        primary = theme.primaryLight,
        onPrimary = Color.White,
        primaryContainer = theme.primaryLight.copy(alpha = 0.15f),
        onPrimaryContainer = theme.primaryLight,
        secondary = theme.secondaryLight,
        onSecondary = Color.White,
        secondaryContainer = theme.secondaryLight.copy(alpha = 0.15f),
        onSecondaryContainer = theme.secondaryLight,
        tertiary = theme.tertiaryLight,
        onTertiary = Color.White,
        tertiaryContainer = theme.tertiaryLight.copy(alpha = 0.15f),
        onTertiaryContainer = theme.tertiaryLight,
        background = theme.backgroundLight,
        onBackground = Color.Black.copy(alpha = 0.87f),
        surface = theme.surfaceLight,
        onSurface = Color.Black.copy(alpha = 0.87f),
        surfaceVariant = theme.surfaceLight.copy(alpha = 0.7f),
        onSurfaceVariant = Color.Black.copy(alpha = 0.6f),
        error = ErrorColor,
        onError = Color.White
    )
}

@Composable
fun SudokuOnlineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled dynamic color to prevent red statusbar
    appTheme: AppTheme? = null, // Custom app theme from ThemeRepository
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // If we have a custom theme selected (not default), use it
        appTheme != null && appTheme != AppTheme.DEFAULT -> {
            createColorScheme(appTheme, darkTheme)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
