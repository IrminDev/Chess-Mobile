package com.github.irmin.chess.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Esquemas de color para tema Guinda
private val MaroonDarkColorScheme = darkColorScheme(
    primary = Maroon80,
    secondary = MaroonGrey80,
    tertiary = MaroonPink80
)

private val MaroonLightColorScheme = lightColorScheme(
    primary = Maroon40,
    secondary = MaroonGrey40,
    tertiary = MaroonPink40
)

// Esquemas de color para tema Azul
private val BlueDarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = BlueCyan80
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = BlueCyan40
)

enum class AppTheme {
    MAROON,  // Guinda - Tema por defecto
    BLUE     // Azul
}

@Composable
fun ChessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.MAROON,
    dynamicColor: Boolean = false,  // Deshabilitado por defecto para usar nuestros temas
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        appTheme == AppTheme.MAROON -> {
            if (darkTheme) MaroonDarkColorScheme else MaroonLightColorScheme
        }
        else -> {  // AppTheme.BLUE
            if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}