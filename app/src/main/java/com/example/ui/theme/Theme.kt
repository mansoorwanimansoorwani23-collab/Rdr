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
    primary = EmeraldDarkPrimary,
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005236),
    onPrimaryContainer = Color(0xFF80E8BA),
    secondary = EmeraldDarkSecondary,
    onSecondary = Color(0xFF003824),
    background = EmeraldDarkBg,
    onBackground = Color(0xFFE1EBE5),
    surface = EmeraldDarkSurface,
    onSurface = Color(0xFFE1EBE5),
    surfaceVariant = EmeraldDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5C8BF),
    outline = EmeraldDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7F3DC),
    onPrimaryContainer = Color(0xFF003824),
    secondary = EmeraldPrimaryVariant,
    onSecondary = Color.White,
    background = EmeraldLightBg,
    onBackground = Color(0xFF131D18),
    surface = EmeraldLightSurface,
    onSurface = Color(0xFF131D18),
    surfaceVariant = Color(0xFFE5EFE9),
    onSurfaceVariant = Color(0xFF3F4D46),
    outline = EmeraldLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted emerald branding
    content: @Composable () -> Unit,
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

