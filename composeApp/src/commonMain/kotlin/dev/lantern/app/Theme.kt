package dev.lantern.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Amber = Color(0xFF9A5B00)
private val Light = lightColorScheme(
    primary = Amber, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB0), onPrimaryContainer = Color(0xFF301A00),
    secondary = Color(0xFF4F6B5B), tertiary = Color(0xFF3F5F8A),
    background = Color(0xFFFFF8F0), surface = Color(0xFFFFF8F0),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFF6ECDF),
    onSurface = Color(0xFF211A12), onSurfaceVariant = Color(0xFF5E5547), outlineVariant = Color(0xFFE2D6C4),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFFFB84D), onPrimary = Color(0xFF4A2A00),
    primaryContainer = Color(0xFF6B3F00), onPrimaryContainer = Color(0xFFFFDDB0),
    secondary = Color(0xFFA7C8B3), tertiary = Color(0xFFA9C7F5),
    background = Color(0xFF141720), surface = Color(0xFF141720),
    surfaceContainer = Color(0xFF1C202B), surfaceContainerHigh = Color(0xFF262B38),
    onSurface = Color(0xFFEDE6DA), onSurfaceVariant = Color(0xFFB7AE9F), outlineVariant = Color(0xFF3A4152),
)

@Composable
fun LanternTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
