package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AetherionDarkColorScheme = darkColorScheme(
    primary = MetallicGold,
    onPrimary = ObsidianVoid,
    primaryContainer = MetallicGoldContainer,
    onPrimaryContainer = MetallicGoldBright,
    secondary = ChampagneSecondary,
    onSecondary = ObsidianVoid,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = IvoryText,
    tertiary = LiveEmerald,
    onTertiary = ObsidianVoid,
    tertiaryContainer = LiveEmeraldContainer,
    onTertiaryContainer = LiveEmerald,
    error = CrimsonAlert,
    onError = IvoryText,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonBright,
    background = ObsidianVoid,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = ChampagneSecondary,
    outline = ObsidianBorder,
    outlineVariant = SlateMuted
)

private val AetherionShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AetherionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AetherionDarkColorScheme,
        typography = AetherionTypography,
        shapes = AetherionShapes,
        content = content
    )
}
