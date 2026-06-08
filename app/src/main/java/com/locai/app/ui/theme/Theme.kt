package com.locai.app.ui.theme

import android.app.Activity
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
    primary = Indigo80,
    onPrimary = Color(0xFF2D1F8F),
    primaryContainer = Color(0xFF3F35A8),
    onPrimaryContainer = Color(0xFFE3DFFF),
    secondary = IndigoGrey80,
    tertiary = Coral80,
    background = IndigoBackgroundDark,
    surface = Color(0xFF1D1A2C)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1A1148),
    secondary = IndigoGrey40,
    tertiary = Coral40,
    background = IndigoBackgroundLight,
    surface = Color.White
)

/**
 * LocAi defaults dynamic color OFF: the indigo/violet identity is the brand, not a
 * suggestion — wallpaper-derived Material You palettes would wash that out on most devices.
 */
@Composable
fun LocAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
