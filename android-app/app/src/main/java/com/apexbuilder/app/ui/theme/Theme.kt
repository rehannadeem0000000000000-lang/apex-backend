package com.apexbuilder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ApexDarkColors = darkColorScheme(
    primary = Color(0xFFD1FA62),
    onPrimary = Color(0xFF182014),
    secondary = Color(0xFFB6D881),
    onSecondary = Color(0xFF1B2414),
    background = Color(0xFF101310),
    onBackground = Color(0xFFE8EDE8),
    surface = Color(0xFF171B18),
    onSurface = Color(0xFFE2E8E1),
    surfaceVariant = Color(0xFF222722),
    onSurfaceVariant = Color(0xFFADB8AD),
    outline = Color(0xFF3B433A),
    error = Color(0xFFFF8A7A),
)

@Composable
fun ApexBuilderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ApexDarkColors,
        content = content,
    )
}