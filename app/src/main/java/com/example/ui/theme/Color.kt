package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Youth Cadre OS — Modern Clean White & Deep Navy Theme
// ==========================================

// Base Clean Surfaces
val CleanCanvas = Color(0xFFF8FAFC)            // Ultra-clean light background
val PureWhite = Color(0xFFFFFFFF)              // Pure white card & dialog surface
val PearlBackground = CleanCanvas              // Compatibility
val CrystalSurface = PureWhite                 // Compatibility
val GlassCardLight = PureWhite                 // Compatibility
val GlassCardSubtle = Color(0xFFF8FAFC)        // Compatibility

// Subtle Borders & Dividers
val BorderLight = Color(0xFFE2E8F0)            // Slate-200 hairline border
val BorderSubtle = Color(0xFFEEF2F6)           // Extra-soft border
val GlassBorderBright = Color(0xFFFFFFFF)
val GlassBorderSubtle = BorderLight

// Deep Navy Gradients ("Gradient Xanh Đậm")
val NavyHeroDark = Color(0xFF090E24)           // Ultra-deep midnight navy
val NavyDeep = Color(0xFF16195E)               // Official Youth Cadre Navy
val NavyPrimary = Color(0xFF1E3A8A)            // Executive Royal Navy
val NavyMedium = Color(0xFF2563EB)             // Electric Cobalt
val BlueVibrant = Color(0xFF3B82F6)            // Vibrant accent
val LuminousCyan = Color(0xFF0284C7)           // Crisp cyan accent
val SoftCyan = Color(0xFF38BDF8)

// Predefined Navy Gradients
val DeepNavyGradient = Brush.linearGradient(
    listOf(NavyHeroDark, NavyDeep, NavyPrimary)
)
val FreshExecutiveGradient = Brush.linearGradient(
    listOf(
        Color(0xFF0F3D7D), // Vibrant Royal Blue
        Color(0xFF1D4ED8), // Electric Sapphire
        Color(0xFF0284C7), // Azure Blue
        Color(0xFF0EA5E9)  // Sky Cyan Accent
    )
)
val FreshExecutiveFabGradient = Brush.linearGradient(
    listOf(
        Color(0xFF1D4ED8),
        Color(0xFF0284C7)
    )
)
val DeepNavyHorizontal = Brush.horizontalGradient(
    listOf(NavyDeep, NavyPrimary, NavyMedium)
)
val DeepNavyButtonGradient = Brush.horizontalGradient(
    listOf(NavyDeep, NavyPrimary)
)
val CardWhiteGradient = Brush.verticalGradient(
    listOf(PureWhite, Color(0xFFF8FAFC))
)

// Legacy compatibility tokens
val ElectricBlue = NavyPrimary
val ElectricBlueGlow = Color(0x1F1E3A8A)
val ChampagneGold = Color(0xFFD97706)
val AmberGlow = Color(0x20D97706)
val EmeraldSuccess = Color(0xFF059669)
val EmeraldGlow = Color(0x20059669)
val CoralUrgent = Color(0xFFE11D48)
val CoralGlow = Color(0x20E11D48)

// Typography
val TextPrimary = Color(0xFF0F172A)            // Deep slate-black for maximum readability
val TextSecondary = Color(0xFF475569)          // Refined mid-slate
val TextMuted = Color(0xFF64748B)              // Muted slate
val PremiumWhite = TextPrimary                 // Compatibility
val TextSecondaryDark = TextSecondary
val TextMutedDark = TextMuted

// Dark/Night tokens
val VoidBlack = Color(0xFF0F172A)
val MidnightNavy = Color(0xFFF8FAFC)
val DeepSlate = PureWhite
val CardSlate = CleanCanvas
val BorderGlassDark = BorderLight
