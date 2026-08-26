package com.nandomclaren.watery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = WateryBlue,
    secondary = WateryRed,
    background = WateryDarkBackground,
    surface = WaterySurface,
)

private val LightColors = lightColorScheme(
    primary = WateryBlueDark,
    secondary = WateryRed,
)

@Composable
fun WateryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
