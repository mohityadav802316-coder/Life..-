package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkColorScheme = darkColorScheme(
  primary = CyanNeon,
  onPrimary = Color(0xFF00363D),
  primaryContainer = Color(0xFF004F58),
  onPrimaryContainer = Color(0xFF97F0FF),

  secondary = VioletNeon,
  onSecondary = Color(0xFF2E0066),
  secondaryContainer = VioletDeep,
  onSecondaryContainer = Color(0xFFE9DDFF),

  tertiary = BlueAccent,
  onTertiary = Color(0xFF003544),
  tertiaryContainer = Color(0xFF004D63),
  onTertiaryContainer = Color(0xFFBEE9FF),

  background = DarkBackground,
  onBackground = TextPrimary,

  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = TextSecondary,

  outline = DarkSurfaceBorder,
  outlineVariant = Color(0xFF16253F),

  error = StatusMissed,
  onError = Color.White,
  errorContainer = StatusMissedContainer,
  onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun LifeTrackerTheme(
  darkTheme: Boolean = true, // Force premium dark luxury theme for Life Tracker
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = PremiumDarkColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  LifeTrackerTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
