package com.shorashim.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Forest700,
    onPrimary = PureWhite,
    primaryContainer = Forest100,
    onPrimaryContainer = Forest900,
    inversePrimary = Mint,
    secondary = Forest600,
    onSecondary = PureWhite,
    secondaryContainer = Forest50,
    onSecondaryContainer = Forest800,
    tertiary = Gold,
    onTertiary = PureWhite,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = Parchment,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = ParchmentDeep,
    onSurfaceVariant = InkSoft,
    surfaceTint = Forest700,
    surfaceBright = Paper,
    surfaceDim = ParchmentDeep,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Paper,
    surfaceContainer = Color(0xFFF5F1E8),
    surfaceContainerHigh = ParchmentDeep,
    surfaceContainerHighest = Color(0xFFEAE4D7),
    inverseSurface = Color(0xFF2B312D),
    inverseOnSurface = Color(0xFFF1EFE8),
    outline = HairlineStrong,
    outlineVariant = Hairline,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = OnMint,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    inversePrimary = Forest700,
    secondary = Forest200,
    onSecondary = Forest900,
    secondaryContainer = NightSurfaceHighest,
    onSecondaryContainer = NightInk,
    tertiary = GoldDark,
    onTertiary = Color(0xFF2E2208),
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = OnGoldContainerDark,
    background = Night,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightInkSoft,
    surfaceTint = Mint,
    surfaceBright = NightSurfaceHighest,
    surfaceDim = Night,
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = NightSurface,
    surfaceContainer = Color(0xFF17201C),
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightSurfaceHighest,
    inverseSurface = NightInk,
    inverseOnSurface = Color(0xFF1B211D),
    outline = Color(0xFF3D4C45),
    outlineVariant = NightHairline,
    error = ErrorDark,
    onError = Color(0xFF601410),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    scrim = Color(0xFF000000),
)

/**
 * Semantic colors that Material's scheme has no slot for. Read them via `ShorashimTheme.colors`.
 * Every screen should use these instead of hard-coded hex values so light and dark mode both work.
 */
@Immutable
data class ShorashimExtendedColors(
    val gold: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val clay: Color,
    val clayContainer: Color,
    val onClayContainer: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    /** Background for the tree map canvas and other "workspace" areas. */
    val canvas: Color,
    val canvasDot: Color,
    /** Chat bubbles. */
    val bubbleMine: Color,
    val onBubbleMine: Color,
    val bubbleTheirs: Color,
    val onBubbleTheirs: Color,
)

private val LightExtended = ShorashimExtendedColors(
    gold = Gold,
    goldContainer = GoldContainer,
    onGoldContainer = OnGoldContainer,
    clay = Clay,
    clayContainer = ClayContainer,
    onClayContainer = OnClayContainer,
    success = Success,
    successContainer = SuccessContainer,
    warning = Warning,
    warningContainer = WarningContainer,
    info = Info,
    infoContainer = InfoContainer,
    canvas = Color(0xFFF3EFE6),
    canvasDot = Color(0xFFD9D1C0),
    bubbleMine = Forest700,
    onBubbleMine = PureWhite,
    bubbleTheirs = PureWhite,
    onBubbleTheirs = Ink,
)

private val DarkExtended = ShorashimExtendedColors(
    gold = GoldDark,
    goldContainer = GoldContainerDark,
    onGoldContainer = OnGoldContainerDark,
    clay = ClayDark,
    clayContainer = ClayContainerDark,
    onClayContainer = Color(0xFFFFDBCF),
    success = SuccessDark,
    successContainer = SuccessContainerDark,
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    info = InfoDark,
    infoContainer = InfoContainerDark,
    canvas = Color(0xFF101714),
    canvasDot = Color(0xFF26322D),
    bubbleMine = MintContainer,
    onBubbleMine = OnMintContainer,
    bubbleTheirs = NightSurfaceHighest,
    onBubbleTheirs = NightInk,
)

private val LocalShorashimColors = staticCompositionLocalOf { LightExtended }

val ShorashimShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object ShorashimTheme {
    val colors: ShorashimExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalShorashimColors.current
}

@Composable
fun ShorashimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalShorashimColors provides if (darkTheme) DarkExtended else LightExtended,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ShorashimTypography,
            shapes = ShorashimShapes,
            content = content,
        )
    }
}
