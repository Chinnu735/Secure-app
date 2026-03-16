package com.securefolder.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SecureFolderColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = TextOnAccent,
    primaryContainer = CyanAccentDim,
    onPrimaryContainer = TextPrimary,
    secondary = TealAccent,
    onSecondary = TextOnAccent,
    secondaryContainer = CardElevatedDark,
    onSecondaryContainer = TextPrimary,
    tertiary = GreenSecure,
    onTertiary = TextOnAccent,
    background = PrimaryDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = RedAlert,
    onError = TextPrimary,
    errorContainer = RedAlertLight,
    outline = GlassBorder,
    outlineVariant = TextTertiary
)

@Composable
fun SecureFolderTheme(content: @Composable () -> Unit) {
    val colorScheme = SecureFolderColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PrimaryDark.toArgb()
            window.navigationBarColor = PrimaryDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SecureFolderTypography,
        content = content
    )
}
