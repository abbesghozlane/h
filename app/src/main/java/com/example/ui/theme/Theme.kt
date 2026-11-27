package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val HaiderDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = GoldDarkInk,
    primaryContainer = GoldDeep,
    onPrimaryContainer = WarmCream,
    secondary = GoldBright,
    onSecondary = GoldDarkInk,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = WarmCream,
    tertiary = EmeraldLuminous,
    onTertiary = Color(0xFF003824),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldLuminous,
    background = ObsidianDeep,
    onBackground = WarmCream,
    surface = ObsidianSurface,
    onSurface = WarmCream,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = SoftSilver,
    surfaceContainerLowest = ObsidianDeep,
    surfaceContainerLow = ObsidianMid,
    surfaceContainer = ObsidianElevated,
    surfaceContainerHigh = ObsidianCard,
    surfaceContainerHighest = ObsidianHighest,
    outline = MutedAsh,
    outlineVariant = Color(0xFF3E382A),
    error = CrimsonSoft,
    onError = Color(0xFF490005),
    errorContainer = CrimsonContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val HaiderShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HaiderParfumsTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = HaiderDarkColorScheme,
            typography = Typography,
            shapes = HaiderShapes,
            content = content
        )
    }
}
