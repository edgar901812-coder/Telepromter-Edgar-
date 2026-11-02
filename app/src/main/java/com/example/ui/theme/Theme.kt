package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00354E),
    primaryContainer = Color(0xFF004D70),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = StudioAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF623F00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = StudioCoral,
    background = StudioDarkBg,
    onBackground = TextPrimaryDark,
    surface = StudioSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = StudioCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = StudioLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2E8FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = StudioLightSecondary,
    onSecondary = Color.White,
    tertiary = StudioCoral,
    background = StudioLightBg,
    onBackground = TextPrimaryLight,
    surface = StudioLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun TeleprompterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature studio palette for cohesive prompter branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
