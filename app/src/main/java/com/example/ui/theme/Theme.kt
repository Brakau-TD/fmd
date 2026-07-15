package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val TrackerColorScheme = lightColorScheme(
    primary = TrackerBlue,
    secondary = TrackerGreen,
    tertiary = TrackerRed,
    background = CyberBlack, // Now 0xFFFDFBFF (ultra clean light background)
    surface = CyberGrayDeep,  // Now 0xFFEEF1F8 (soft grayish-blue background)
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary, // Now 0xFF1B1B1F
    onSurface = TextPrimary,
    surfaceVariant = CyberGrayLight, // Now 0xFFDBE1FF (highlight accent)
    outline = CyberGrayBorder // Now 0xFFDCE2F0
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to false for light-mode Geometric Balance theme
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our cohesive palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TrackerColorScheme,
        typography = Typography,
        content = content
    )
}
