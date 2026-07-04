package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Teal80,
    secondary = Sage80,
    tertiary = Amber80,
    background = Color(0xFF1B1B1F),
    surface = Color(0xFF23242A),
    primaryContainer = Color(0xFF004174),
    onPrimary = Color(0xFF002F5D),
    onSecondary = Color(0xFF2B303B),
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6),
    onPrimaryContainer = Color(0xFFD3E4FF),
    outline = Color(0xFF44474E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Teal40,
    secondary = Sage40,
    tertiary = Amber40,
    background = SoftLight,
    surface = PureWhite,
    primaryContainer = Color(0xFFD3E4FF),
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onTertiary = PureWhite,
    onBackground = DarkSlate,
    onSurface = DarkSlate,
    onPrimaryContainer = Color(0xFF001D36),
    outline = Color(0xFFE1E2EC)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
