package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ========================================
// UJIANKU DESIGN SYSTEM - COLOR TOKENS
// ========================================

// --- Primary Brand ---
val Primary = Color(0xFF10B981)          // Emerald 500 - Main brand
val PrimaryDark = Color(0xFF065F46)      // Emerald 800 - Deep accent
val PrimaryLight = Color(0xFF34D399)     // Emerald 400 - Light accent
val PrimaryContainer = Color(0xFFD1FAE5) // Emerald 100 - Container bg
val OnPrimaryContainer = Color(0xFF064E3B) // Emerald 900 - Text on container

// --- Secondary Brand ---
val Secondary = Color(0xFFEAB308)        // Yellow 500 - Achievement
val SecondaryDark = Color(0xFFA16207)    // Yellow 700 - Deep gold
val SecondaryLight = Color(0xFFFDE047)   // Yellow 300 - Light gold
val SecondaryContainer = Color(0xFFFEF9C3) // Yellow 100 - Container bg
val OnSecondaryContainer = Color(0xFF713F12) // Yellow 900 - Text on container

// --- Semantic Colors ---
val Success = Color(0xFF22C55E)          // Green 600
val Warning = Color(0xFFF59E0B)          // Amber 500
val Error = Color(0xFFEF4444)            // Red 500
val ErrorDark = Color(0xFFDC2626)        // Red 600
val Info = Color(0xFF3B82F6)             // Blue 500

// --- Neutral / Background ---
val BackgroundLight = Color(0xFFF7FAF6)  // Airy mint white
val SurfaceLight = Color(0xFFFFFFFF)     // Pure white
val SurfaceVariantLight = Color(0xFFF4F8F3) // Subtle green tint
val CardBackground = Color(0xFFFAFDF9)   // Slightly warm white

val BackgroundDark = Color(0xFF0D1E16)   // Deep forest charcoal
val SurfaceDark = Color(0xFF162D24)      // Muted green-black
val SurfaceVariantDark = Color(0xFF1E3A2C) // Dark green surface

// --- Text ---
val TextPrimary = Color(0xFF1F2937)      // Gray 800
val TextSecondary = Color(0xFF6B7280)    // Gray 500
val TextTertiary = Color(0xFF9CA3AF)     // Gray 400
val TextDisabled = Color(0xFFD1D5DB)     // Gray 300
val TextOnDark = Color(0xFFF9FAFB)       // Gray 50
val TextOnDarkSecondary = Color(0xFFD1D5DB) // Gray 300

// --- Border / Divider ---
val BorderLight = Color(0xFFE5E7EB)      // Gray 200
val BorderDark = Color(0xFF374151)       // Gray 700
val DividerLight = Color(0xFFF3F4F6)     // Gray 100

// --- Legacy aliases for backward compatibility ---
val SoftGreen = Primary
val DarkSmoothGreen = PrimaryDark
val SmoothLimeGreen = PrimaryLight
val SmoothYellow = Secondary
val SmoothYellowLight = SecondaryContainer
val WarningRed = Error
val MintyLightBg = BackgroundLight
val GentleSurface = SurfaceLight
val DarkBackground = BackgroundDark
val DarkSurface = SurfaceDark
val LightCardBg = SurfaceVariantLight
val TextDark = TextPrimary
val TextMuted = TextSecondary
val SmoothWhite = Color(0xFFFCFDFB)
