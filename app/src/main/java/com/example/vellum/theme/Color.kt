package com.example.vellum.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.MaterialTheme

// Chalkboard Colors
val ChalkboardSlate = Color(0xFF1E2322)
val ChalkWhite = Color(0xFFF5F5F5)
val ChalkGreen = Color(0xFF8FCE5E)
val ChalkRed = Color(0xFFF07D7D)
val ChalkBlue = Color(0xFF87CEEB)
val ChalkGray = Color(0xFF8B8C8D)

// Parchment Colors (remapped dynamically for Chalkboard vs Parchment aesthetic)
val isDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background != Color(0xFFFAF3E0) &&
            MaterialTheme.colorScheme.background != Color(0xFFEFEFEF) &&
            MaterialTheme.colorScheme.background != Color(0xFFD0E6F8)

val ParchmentBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val ParchmentLine: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkTheme) {
        when (MaterialTheme.colorScheme.background) {
            Color(0xFF1B3D2B) -> Color(0xFF385E48)
            Color(0xFF0C1B33) -> Color(0xFF243E63)
            else -> Color(0xFF4A5250)
        }
    } else {
        when (MaterialTheme.colorScheme.background) {
            Color(0xFFEFEFEF) -> Color(0xFFCCCCCC)
            Color(0xFFD0E6F8) -> Color(0x99FFFFFF)
            else -> Color(0xFFE2D6BE)
        }
    }

val ParchmentDarkBrown: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkTheme) ChalkWhite else Color(0xFF4A3B32)

val ParchmentBlueText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkTheme) ChalkBlue else Color(0xFF2E6B8E)

val SettingsSectionHeader: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkTheme) Color(0xFF2C3231) else Color(0xFFF0E5CE)

val TabUnselected: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkTheme) ChalkGray else Color(0xFF8B7B6B)
