package com.example.cpuschedgame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameColorScheme = darkColorScheme(
    primary             = GoldenBright,
    onPrimary           = DarkBg,
    primaryContainer    = DarkCard,
    onPrimaryContainer  = GoldenLight,
    secondary           = GreenBright,
    onSecondary         = DarkBg,
    secondaryContainer  = DarkCard,
    onSecondaryContainer = GreenBright,
    background          = DarkBg,
    onBackground        = TextPrimary,
    surface             = DarkSurface,
    onSurface           = TextPrimary,
    surfaceVariant      = DarkCard,
    onSurfaceVariant    = TextSecondary,
    outline             = DarkBorder,
    error               = DangerRed,
    onError             = Color.White
)

@Composable
fun CPUSchedGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography  = Typography,
        content     = content
    )
}