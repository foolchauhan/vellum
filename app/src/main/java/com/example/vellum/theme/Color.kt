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
val ParchmentBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val ParchmentLine: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == ChalkboardSlate) Color(0xFF4A5250) else Color(0xFFE2D6BE)

val ParchmentDarkBrown: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == ChalkboardSlate) ChalkWhite else Color(0xFF4A3B32)

val ParchmentBlueText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == ChalkboardSlate) ChalkBlue else Color(0xFF2E6B8E)

val SettingsSectionHeader: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == ChalkboardSlate) Color(0xFF2C3231) else Color(0xFFF0E5CE)

val TabUnselected: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background == ChalkboardSlate) ChalkGray else Color(0xFF8B7B6B)
