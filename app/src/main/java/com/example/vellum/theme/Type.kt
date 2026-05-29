package com.example.vellum.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.vellum.R
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.MaterialTheme

// Define Google Font Provider
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Architects Daughter font (looks sketch-like and dusty, like real chalk)
val ArchitectsDaughterFont = FontFamily(
    GoogleFontClass(googleFont = GoogleFont("Architects Daughter"), fontProvider = fontProvider)
)

// Cabin Sketch font loaded locally
val CabinSketchLocalFont = FontFamily(
    Font(resId = R.font.cabin_sketch_regular, weight = FontWeight.Normal)
)

// Fredericka the Great font loaded locally
val FrederickaLocalFont = FontFamily(
    Font(resId = R.font.fredericka_the_great_regular, weight = FontWeight.Normal)
)

// Caveat font (beautiful fluid handwriting for paper/parchment)
val CaveatFont = FontFamily(
    GoogleFontClass(googleFont = GoogleFont("Caveat"), fontProvider = fontProvider)
)

// Handwriting (Chalkboard) Font loaded locally
val ChalkFontFamily = FontFamily(
    Font(resId = R.font.patrick_hand, weight = FontWeight.Normal)
)

// Clean Geometric Sans-Serif (Parchment/Settings) Font remapped globally to chalkboard font
val ParchmentFontFamily: FontFamily
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == ChalkboardSlate) CabinSketchLocalFont else CaveatFont

// Title Font remapped dynamically: Fredericka local font for chalkboard slate, Caveat for light theme parchment
val ParchmentTitleFontFamily: FontFamily
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == ChalkboardSlate) FrederickaLocalFont else CaveatFont

// App Typography Styles
val Typography: Typography
    @Composable
    @ReadOnlyComposable
    get() = Typography(
        bodyLarge = TextStyle(
            fontFamily = ParchmentFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = ParchmentFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontFamily = ParchmentTitleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = ParchmentTitleFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = ParchmentFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelSmall = TextStyle(
            fontFamily = ParchmentFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

// Chalk Chalkboard Typography overrides
val ChalkTitleLarge = TextStyle(
    fontFamily = ChalkFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 32.sp,
    lineHeight = 40.sp,
    letterSpacing = 0.sp,
    color = ChalkWhite
)

val ChalkBodyLarge = TextStyle(
    fontFamily = ChalkFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.5.sp,
    color = ChalkWhite
)

val ChalkLabelLarge = TextStyle(
    fontFamily = ChalkFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.1.sp,
    color = ChalkWhite
)
