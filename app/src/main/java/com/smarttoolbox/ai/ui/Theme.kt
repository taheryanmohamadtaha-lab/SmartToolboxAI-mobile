package com.smarttoolbox.ai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.smarttoolbox.ai.data.Colors

val LocalFa = compositionLocalOf { false }

@Composable
fun t(en: String, fa: String): String = if (LocalFa.current) fa else en

@Composable
fun accentBrush(): Brush {
    val a = MaterialTheme.colorScheme.primary.toArgb()
    return Brush.linearGradient(listOf(Color(Colors.dark(a)), Color(Colors.light(a))))
}

@Composable
fun AppTheme(mode: Int, accent: Int, content: @Composable () -> Unit) {
    val dark = mode == 2 || mode == 3 || (mode == 0 && isSystemInDarkTheme())
    val amoled = mode == 3
    val p = Color(accent)
    val scheme = if (!dark) lightColorScheme(
        primary = p, onPrimary = Color.White,
        primaryContainer = Color(ColorUtils.blendARGB(accent, android.graphics.Color.WHITE, 0.82f)),
        onPrimaryContainer = Color(Colors.dark(accent)),
        secondary = Color(Colors.light(accent)),
        background = Color(0xFFF8F4FC), onBackground = Color(0xFF1B1B1F),
        surface = Color.White, onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFEFE8F6), onSurfaceVariant = Color(0xFF5E5A66)
    ) else darkColorScheme(
        primary = p, onPrimary = Color.White,
        primaryContainer = Color(ColorUtils.blendARGB(accent, android.graphics.Color.BLACK, 0.6f)),
        onPrimaryContainer = Color.White,
        secondary = Color(Colors.light(accent)),
        background = if (amoled) Color.Black else Color(0xFF14111A), onBackground = Color(0xFFEDE7F3),
        surface = if (amoled) Color(0xFF0B0A0E) else Color(0xFF1E1A26), onSurface = Color(0xFFEDE7F3),
        surfaceVariant = if (amoled) Color(0xFF1A1720) else Color(0xFF2B2635), onSurfaceVariant = Color(0xFFB9B3C4)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
