package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cinematic Deep Dark Canvas
val DarkBackground = Color(0xFF07090E)        // Obsidian Void
val DarkBackgroundElevated = Color(0xFF0D101A) // Deep Night
val DarkSurface = Color(0xFF121624)           // Glass Core
val DarkSurfaceElevated = Color(0xFF181E30)   // Layered Glass Card
val DarkSurfaceHighlight = Color(0xFF222B42)  // High-light surface
val DarkSurfaceBorder = Color(0xFF232B40)     // Thin crisp border
val DarkSurfaceBorderSubtle = Color(0xFF1A2133)

// Luminous Accents
val CyanNeon = Color(0xFF00F0FF)              // Electric Cyan / Teal
val CyanGlow = Color(0xFF00E5FF)
val CyanDim = Color(0xFF006875)
val CyanMuted = Color(0xFF003840)

val VioletNeon = Color(0xFF818CF8)            // Luminous Indigo / Violet
val VioletDeep = Color(0xFF4338CA)
val VioletGlow = Color(0xFFA855F7)            // Electric Amethyst

val BlueAccent = Color(0xFF38BDF8)            // Sky Blue Accent
val BlueGlow = Color(0xFF0284C7)

// Semantic Status Lighting (Refined & Subtle, not gaudy)
val StatusComplete = Color(0xFF10B981)        // Pure Mint Emerald
val StatusCompleteGlow = Color(0xFF059669)
val StatusCompleteContainer = Color(0xFF064E3B)
val StatusCompleteSurface = Color(0x1F10B981)

val StatusPartial = Color(0xFFF59E0B)         // Warm Amber Sun
val StatusPartialGlow = Color(0xFFD97706)
val StatusPartialContainer = Color(0xFF78350F)
val StatusPartialSurface = Color(0x1FF59E0B)

val StatusMissed = Color(0xFFF43F5E)          // Neon Crimson Rose
val StatusMissedGlow = Color(0xFFE11D48)
val StatusMissedContainer = Color(0xFF881337)
val StatusMissedSurface = Color(0x1FF43F5E)

// Today Extra Task Theme (Electrified Amethyst & Gold)
val ExtraTaskAccent = Color(0xFFC084FC)       // Electric Violet / Lilac
val ExtraTaskGlow = Color(0xFFA855F7)
val ExtraTaskContainer = Color(0xFF3B0764)
val ExtraTaskSurface = Color(0x22A855F7)

// Premium Typography Hierarchy
val TextPrimary = Color(0xFFF8FAFC)           // Pure Titanium Crisp
val TextSecondary = Color(0xFF94A3B8)         // Cool Slate
val TextMuted = Color(0xFF64748B)             // Deep Neutral
val TextDisabled = Color(0xFF475569)

// Luxury Gradients
val GlassGradient = Brush.verticalGradient(
  colors = listOf(
    Color(0xFF1A2033).copy(alpha = 0.85f),
    Color(0xFF101422).copy(alpha = 0.95f)
  )
)

val CardBorderGradient = Brush.linearGradient(
  colors = listOf(
    CyanNeon.copy(alpha = 0.45f),
    VioletNeon.copy(alpha = 0.25f),
    DarkSurfaceBorder
  )
)

val ActiveCardBorderGradient = Brush.linearGradient(
  colors = listOf(
    CyanNeon,
    BlueAccent,
    VioletGlow
  )
)

val ExtraTaskBorderGradient = Brush.linearGradient(
  colors = listOf(
    ExtraTaskAccent.copy(alpha = 0.8f),
    VioletGlow.copy(alpha = 0.4f),
    DarkSurfaceBorder
  )
)
