package com.example.vellum.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography as MaterialTypography

private val DarkColorScheme = darkColorScheme(
    primary = ChalkBlue,
    secondary = ChalkGreen,
    tertiary = ChalkRed,
    background = ChalkboardSlate,
    surface = ChalkboardSlate,
    onPrimary = ChalkboardSlate,
    onSecondary = ChalkboardSlate,
    onTertiary = ChalkboardSlate,
    onBackground = ChalkWhite,
    onSurface = ChalkWhite
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E6B8E),
    secondary = Color(0xFF8FCE5E),
    tertiary = Color(0xFFF07D7D),
    background = Color(0xFFFAF3E0), // Warm Parchment Background
    surface = Color(0xFFFAF3E0),
    onPrimary = Color(0xFFFAF3E0),
    onSecondary = Color(0xFFFAF3E0),
    onTertiary = Color(0xFFFAF3E0),
    onBackground = Color(0xFF4A3B32), // Ink Text
    onSurface = Color(0xFF4A3B32)
)

@Composable
fun VellumTheme(
    theme: String = "System",
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (theme) {
        "Dark" -> true
        "Light" -> false
        "System" -> isSystemDark
        else -> false // default fallback
    }

    // Extensible Themes Pattern:
    // In the future, a developer can define a new ColorScheme (e.g. NeonColorScheme, ClassicColorScheme)
    // and expand this selection logic:
    // val colorScheme = when(theme) {
    //     "Dark" -> DarkColorScheme
    //     "Light" -> LightColorScheme
    //     "Neon" -> NeonColorScheme
    //     "System" -> if (isSystemDark) DarkColorScheme else LightColorScheme
    //     else -> LightColorScheme
    // }
    val colorScheme = if (isDark) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    // Similarly, custom themes can resolve different font families:
    val activeFontFamily = if (isDark) CabinSketchLocalFont else CaveatFont
    val activeTitleFontFamily = if (isDark) FrederickaLocalFont else CaveatFont

    val typography = MaterialTypography(
        bodyLarge = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = activeTitleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = activeTitleFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelSmall = TextStyle(
            fontFamily = activeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
