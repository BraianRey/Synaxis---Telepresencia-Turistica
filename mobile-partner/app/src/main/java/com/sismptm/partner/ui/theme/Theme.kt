package com.sismptm.partner.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Synexis Dark Color Scheme - Used for both dark and light themes
 * to maintain consistent brand identity across the app
 */
private val SynexisColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = PrimaryHover,
    onSecondary = TextPrimary,
    secondaryContainer = BackgroundElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = Success,
    onTertiary = TextPrimary,
    tertiaryContainer = SuccessLight,
    onTertiaryContainer = TextPrimary,
    error = Error,
    onError = TextPrimary,
    errorContainer = ErrorLight,
    onErrorContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = Divider,
    scrim = Background.copy(alpha = 0.8f),
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,
    inversePrimary = PrimaryHover,
    surfaceTint = PrimaryAccent,
    surfaceBright = BackgroundElevated,
    surfaceDim = Background,
    surfaceContainer = CardBackground,
    surfaceContainerHigh = CardBackgroundHover,
    surfaceContainerHighest = BackgroundElevated,
    surfaceContainerLow = Background,
    surfaceContainerLowest = Background
)

@Composable
fun SISPTMPartnerTheme(
    darkTheme: Boolean = true, // Always use dark theme for brand consistency
    // Dynamic color is disabled to maintain brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = SynexisColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Background.toArgb()
            window.navigationBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
