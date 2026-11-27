package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Deep Obsidian Night Canvas
val ObsidianDeep = Color(0xFF07070A)
val ObsidianMid = Color(0xFF0E0E14)
val ObsidianSurface = Color(0xFF13131A)
val ObsidianElevated = Color(0xFF1B1B24)
val ObsidianCard = Color(0xFF201F27)
val ObsidianHighest = Color(0xFF2E2D36)

// Metallic Champagne Gold Palette
val GoldBright = Color(0xFFF3D06B)
val GoldPrimary = Color(0xFFD4AF37)
val GoldDeep = Color(0xFFAA821C)
val GoldDarkInk = Color(0xFF261C00)
val AmberGlow = Color(0xFF5C4012)

// Typography & Semantic Accents
val WarmCream = Color(0xFFFAF6EE)
val SoftSilver = Color(0xFFB8B8C2)
val MutedAsh = Color(0xFF8C8894)
val EmeraldLuminous = Color(0xFF58E7AA)
val EmeraldContainer = Color(0xFF0E3D2D)
val AmberWarning = Color(0xFFF5A623)
val CrimsonSoft = Color(0xFFFF8A80)
val CrimsonContainer = Color(0xFF4A181B)

// Glassmorphism Surface & Refraction Strokes
val GlassFillSubtle = Color(0x0FFFFFFF)      // ~6% white
val GlassFillMedium = Color(0x16FFFFFF)      // ~8.5% white
val GlassFillStrong = Color(0x21FFFFFF)      // ~13% white
val GlassDarkTint = Color(0xCC101016)        // Frosted dark bar tint
val GlassBorderGold = Color(0x47FFD782)      // ~28% warm gold highlight
val GlassBorderFade = Color(0x12FFFFFF)      // ~7% soft white fade

val ChampagneGoldGradient = Brush.linearGradient(
    colors = listOf(GoldBright, GoldPrimary, GoldDeep)
)

val ObsidianCanvasGradient = Brush.verticalGradient(
    colors = listOf(ObsidianDeep, Color(0xFF0C0C12), Color(0xFF111118))
)

val GlassBorderBrush = Brush.linearGradient(
    colors = listOf(GlassBorderGold, Color(0x22FFD782), GlassBorderFade)
)

val ActiveGoldBorderBrush = Brush.linearGradient(
    colors = listOf(GoldBright, GoldPrimary, Color(0x66D4AF37))
)
