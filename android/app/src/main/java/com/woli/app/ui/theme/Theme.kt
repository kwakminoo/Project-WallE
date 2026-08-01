package com.woli.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val WoliBlack = Color(0xFF000000)
val WoliSurface = Color(0xFF121212)
val WoliCard = Color(0xFF1C1C1E)
val WoliYellow = Color(0xFFFFCC00)
val WoliYellowDim = Color(0xFFC9A000)
val WoliCyan = Color(0xFF00E5FF)
val WoliCyanSoft = Color(0xFF40E0F0)
val WoliWarning = Color(0xFFFF3B30)
val WoliText = Color(0xFFFFFFFF)
val WoliMuted = Color(0xFF9E9E9E)
val WoliOrange = Color(0xFFFF9F0A)

private val WoliColorScheme = darkColorScheme(
    primary = WoliYellow,
    onPrimary = WoliBlack,
    secondary = WoliCyan,
    onSecondary = WoliBlack,
    background = WoliBlack,
    onBackground = WoliText,
    surface = WoliSurface,
    onSurface = WoliText,
    error = WoliWarning,
    onError = WoliText,
)

private val WoliTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        color = WoliText,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        color = WoliText,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = WoliText,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = WoliText,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = WoliMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = WoliBlack,
    ),
)

@Composable
fun WoliTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WoliColorScheme,
        typography = WoliTypography,
        content = content,
    )
}
