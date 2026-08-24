package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5C),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = ElectricEmerald,
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF005327),
    onSecondaryContainer = Color(0xFF67FF9A),
    tertiary = ElectricViolet,
    onTertiary = Color.White,
    background = CyberDarkBackground,
    onBackground = TextPrimaryDark,
    surface = CyberDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberDarkCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = Color(0xFF1E2B46),
    error = StatusError,
    onError = Color.White
)

private val CyberLightColorScheme = lightColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF008947),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA6F5BA),
    onSecondaryContainer = Color(0xFF00210B),
    tertiary = ElectricViolet,
    onTertiary = Color.White,
    background = CyberLightBackground,
    onBackground = TextPrimaryLight,
    surface = CyberLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = StatusError,
    onError = Color.White
)

@Composable
fun ShadowRayTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) CyberDarkColorScheme else CyberLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
