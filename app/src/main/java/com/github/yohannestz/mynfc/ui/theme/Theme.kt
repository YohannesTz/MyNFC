package com.github.yohannestz.mynfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF0B2A66),
    secondary = Orange,
    onSecondary = Color.White,
    tertiary = Emerald,
    background = Color(0xFFF2F3F7),
    onBackground = Color(0xFF111318),
    surface = Color.White,
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFE6E8EE),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FB),
    surfaceContainer = Color(0xFFF2F3F7),
    surfaceContainerHigh = Color(0xFFE9EBF0),
    surfaceContainerHighest = Color(0xFFE2E5EB),
    outline = Color(0xFFC9CDD6),
    outlineVariant = Color(0xFFE8EAEF),
    error = Red,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5B9BFF),
    onPrimary = Color(0xFF00194A),
    primaryContainer = Color(0xFF16336B),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFFF9A4D),
    tertiary = Color(0xFF34D399),
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF161922),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF232733),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainerLowest = Color(0xFF0B0D12),
    surfaceContainerLow = Color(0xFF161922),
    surfaceContainer = Color(0xFF1B1F29),
    surfaceContainerHigh = Color(0xFF222632),
    surfaceContainerHighest = Color(0xFF2A2F3C),
    outline = Color(0xFF3A4050),
    outlineVariant = Color(0xFF262B36),
    error = Color(0xFFF87171),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MyNFCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
