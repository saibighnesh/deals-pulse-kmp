package com.dealspulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF1976D2),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFCCE5FF),
    onSecondaryContainer = Color(0xFF001E2F),
    tertiary = Color(0xFF6750A4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21005D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF1A1C1E),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFF90CAF9),
    surfaceDim = Color(0xFFDADDE0),
    surfaceBright = Color(0xFFFDFCFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFF1F0F4),
    surfaceContainerHigh = Color(0xFFEBEAEE),
    surfaceContainerHighest = Color(0xFFE5E4E8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF9CCAFF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF001E2F),
    onSecondaryContainer = Color(0xFFCCE5FF),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF1A1C1E),
    inversePrimary = Color(0xFF1976D2),
    surfaceDim = Color(0xFF1A1C1E),
    surfaceBright = Color(0xFF3F4144),
    surfaceContainerLowest = Color(0xFF141618),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2022),
    surfaceContainerHigh = Color(0xFF282A2D),
    surfaceContainerHighest = Color(0xFF333537),
)

@Composable
fun DealsPulseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}