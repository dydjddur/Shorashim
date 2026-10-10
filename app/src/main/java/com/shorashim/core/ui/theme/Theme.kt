package com.shorashim.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = PureWhite,
    primaryContainer = PaleSageContainer,
    onPrimaryContainer = ForestGreenDark,
    secondary = EmeraldPrimary,
    onSecondary = PureWhite,
    secondaryContainer = SurfaceField,
    onSecondaryContainer = DarkText,
    tertiary = MintAccent,
    onTertiary = PureWhite,
    background = BackgroundLight,
    onBackground = DarkText,
    surface = SurfaceCard,
    onSurface = DarkText,
    surfaceVariant = SurfaceField,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = MintLight,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceField,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = MintAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceField,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkError,
    onError = DarkBackground,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

@Composable
fun ShorashimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ShorashimTypography,
            content = content,
        )
    }
}
