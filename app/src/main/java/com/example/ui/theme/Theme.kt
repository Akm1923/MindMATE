package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MindMateDarkColorScheme = darkColorScheme(
    primary = LuminousViolet,
    onPrimary = VoidMidnight,
    primaryContainer = ColorTokens.PrimaryContainer,
    onPrimaryContainer = SoftLavender,
    secondary = ElectricIndigo,
    onSecondary = VoidMidnight,
    secondaryContainer = ColorTokens.SecondaryContainer,
    onSecondaryContainer = SoftLavender,
    tertiary = CalmMint,
    onTertiary = VoidMidnight,
    tertiaryContainer = ColorTokens.MintContainer,
    onTertiaryContainer = CalmMint,
    background = VoidMidnight,
    onBackground = TextPrimary,
    surface = DeepIndigoSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = ErrorCoral,
    onError = VoidMidnight
)

private object ColorTokens {
    val PrimaryContainer = androidx.compose.ui.graphics.Color(0xFF2B244D)
    val SecondaryContainer = androidx.compose.ui.graphics.Color(0xFF21264D)
    val MintContainer = androidx.compose.ui.graphics.Color(0xFF16352E)
}

val MindMateShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MindMateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MindMateDarkColorScheme,
        typography = MindMateTypography,
        shapes = MindMateShapes,
        content = content
    )
}
