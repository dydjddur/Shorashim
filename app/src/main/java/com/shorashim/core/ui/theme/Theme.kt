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
    primaryContainer = SoftGray,
    onPrimaryContainer = DarkText,
    secondary = MutedOlive,
    onSecondary = PureWhite,
    secondaryContainer = SoftGray,
    onSecondaryContainer = DarkText,
    tertiary = MutedOlive,
    onTertiary = PureWhite,
    background = WarmCream,
    onBackground = DarkText,
    surface = WarmCream,
    onSurface = DarkText,
    surfaceVariant = SoftGray,
    onSurfaceVariant = DarkText,
    outline = MutedOlive,
    error = ForestGreenDark,
    onError = PureWhite,
)

private val DarkColors = darkColorScheme(
    primary = MutedOlive,
    onPrimary = DarkText,
    primaryContainer = ForestGreen,
    onPrimaryContainer = CreamOnDark,
    secondary = MutedOlive,
    onSecondary = DarkText,
    background = ForestGreenDark,
    onBackground = CreamOnDark,
    surface = ForestGreen,
    onSurface = CreamOnDark,
    surfaceVariant = ForestGreen,
    onSurfaceVariant = CreamOnDark,
    outline = MutedOlive,
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
