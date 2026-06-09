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

private val GreenboardColorScheme = darkColorScheme(
    primary = ChalkBlue,
    secondary = ChalkGreen,
    tertiary = ChalkRed,
    background = Color(0xFF1B3D2B), // Green chalkboard slate
    surface = Color(0xFF1B3D2B),
    onPrimary = Color(0xFF1B3D2B),
    onSecondary = Color(0xFF1B3D2B),
    onTertiary = Color(0xFF1B3D2B),
    onBackground = Color(0xFFE8F5EE),
    onSurface = Color(0xFFE8F5EE)
)

private val BlueprintColorScheme = darkColorScheme(
    primary = ChalkBlue,
    secondary = ChalkGreen,
    tertiary = ChalkRed,
    background = Color(0xFF0C1B33), // Blueprint slate
    surface = Color(0xFF0C1B33),
    onPrimary = Color(0xFF0C1B33),
    onSecondary = Color(0xFF0C1B33),
    onTertiary = Color(0xFF0C1B33),
    onBackground = Color(0xFFD3E7FF),
    onSurface = Color(0xFFD3E7FF)
)

private val CementColorScheme = lightColorScheme(
    primary = Color(0xFF333333),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFFC62828),
    background = Color(0xFFEFEFEF), // Cement wall color
    surface = Color(0xFFEFEFEF),
    onPrimary = Color(0xFFEFEFEF),
    onSecondary = Color(0xFFEFEFEF),
    onTertiary = Color(0xFFEFEFEF),
    onBackground = Color(0xFF222222),
    onSurface = Color(0xFF222222)
)

private val GlassColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    secondary = Color(0xFF0D9488),
    tertiary = Color(0xFFE11D48),
    background = Color(0xFFD0E6F8), // Soft ice blue
    surface = Color(0xFFD0E6F8),
    onPrimary = Color(0xFFD0E6F8),
    onSecondary = Color(0xFFD0E6F8),
    onTertiary = Color(0xFFD0E6F8),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun VellumTheme(
    theme: String = "System",
    handwritingStyle: String = "Default",
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (theme) {
        "Dark" -> DarkColorScheme
        "Light" -> LightColorScheme
        "Greenboard" -> GreenboardColorScheme
        "Blueprint" -> BlueprintColorScheme
        "Cement" -> CementColorScheme
        "Glass" -> GlassColorScheme
        "System" -> if (isSystemDark) DarkColorScheme else LightColorScheme
        else -> LightColorScheme
    }
    val isDark = theme == "Dark" || theme == "Greenboard" || theme == "Blueprint" || (theme == "System" && isSystemDark)

    // Resolve font families based on selected handwriting style
    val activeFontFamily = when (handwritingStyle) {
        "Patrick Hand" -> ChalkFontFamily
        "Cursive" -> CaveatFont
        "Chalk" -> CabinSketchLocalFont
        "Architects Daughter" -> ArchitectsDaughterFont
        else -> if (isDark) CabinSketchLocalFont else CaveatFont
    }

    val activeTitleFontFamily = when (handwritingStyle) {
        "Patrick Hand" -> ChalkFontFamily
        "Cursive" -> CaveatFont
        "Chalk" -> FrederickaLocalFont
        "Architects Daughter" -> ArchitectsDaughterFont
        else -> if (isDark) FrederickaLocalFont else CaveatFont
    }

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
