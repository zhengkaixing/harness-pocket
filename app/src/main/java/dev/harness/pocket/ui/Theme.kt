package dev.harness.pocket.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

var Blue = Color(0xFF536DFE)
var BlueDeep = Color(0xFF4058D8)
var Violet = Color(0xFF7E6BFF)
var Ink = Color(0xFF202534)
var InkSoft = Color(0xFF5B6274)
var Muted = Color(0xFF8B92A5)
var Line = Color(0xFFE7E9F2)
var WarmWhite = Color(0xFFF7F8FC)
var Mist = Color(0xFFEEF2FF)
var Green = Color(0xFF35B979)
var Amber = Color(0xFFE2A23F)
var Red = Color(0xFFDD6371)
var CodeBg = Color(0xFF1C2130)
var CardBg = Color(0xFFFFFFFF)
var CardAlt = Color(0xFFF1F3FA)
var DrawerBg = Color(0xFFF7F8FC)

private val HarnessSelectionColors = TextSelectionColors(
    handleColor = Color(0xFFE38A00),
    backgroundColor = Color(0xFFFFC857).copy(alpha = 0.55f)
)

val HarnessTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

val HarnessShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9EFFF),
    onPrimaryContainer = Color(0xFF173A8F),
    secondary = Violet,
    onSecondary = Color.White,
    background = WarmWhite,
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F3FA),
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Red,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA2FF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF25325C),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFFB4A6FF),
    onSecondary = Color(0xFF171226),
    background = Color(0xFF0F131C),
    onBackground = Color(0xFFE7EAF3),
    surface = Color(0xFF171C27),
    onSurface = Color(0xFFE7EAF3),
    surfaceVariant = Color(0xFF1E2431),
    onSurfaceVariant = Color(0xFFB6BDCC),
    outline = Color(0xFF2A3042),
    error = Color(0xFFFF8A96),
    onError = Color(0xFF2B0A0E)
)

private fun applyLightPalette() {
    Blue = Color(0xFF536DFE); BlueDeep = Color(0xFF4058D8); Violet = Color(0xFF7E6BFF)
    Ink = Color(0xFF202534); InkSoft = Color(0xFF5B6274); Muted = Color(0xFF8B92A5)
    Line = Color(0xFFE7E9F2); WarmWhite = Color(0xFFF7F8FC); Mist = Color(0xFFEEF2FF)
    Green = Color(0xFF35B979); Amber = Color(0xFFE2A23F); Red = Color(0xFFDD6371)
    CodeBg = Color(0xFF1C2130); CardBg = Color(0xFFFFFFFF)
    CardAlt = Color(0xFFF1F3FA); DrawerBg = Color(0xFFF7F8FC)
}

private fun applyDarkPalette() {
    Blue = Color(0xFF8FA2FF); BlueDeep = Color(0xFF6D82F5); Violet = Color(0xFFB4A6FF)
    Ink = Color(0xFFE7EAF3); InkSoft = Color(0xFFB6BDCC); Muted = Color(0xFF8B93A6)
    Line = Color(0xFF2A3042); WarmWhite = Color(0xFF0F131C); Mist = Color(0xFF1A2233)
    Green = Color(0xFF63D6A0); Amber = Color(0xFFF0BE6A); Red = Color(0xFFFF8A96)
    CodeBg = Color(0xFF0B0E14); CardBg = Color(0xFF171C27)
    CardAlt = Color(0xFF1E2431); DrawerBg = Color(0xFF121722)
}

@Composable
fun HarnessTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    if (dark) applyDarkPalette() else applyLightPalette()
    key(dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = HarnessTypography,
            shapes = HarnessShapes
        ) {
            CompositionLocalProvider(LocalTextSelectionColors provides HarnessSelectionColors) {
                content()
            }
        }
    }
}
