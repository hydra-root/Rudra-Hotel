package com.teasupplier.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand & Mesh Colors ────────────────────────────────────────────────────
val MeshColor1 = Color(0xFF5D4037) // Deep Brown (Tea)
val MeshColor2 = Color(0xFFFF9933) // Saffron / Orange
val MeshColor3 = Color(0xFFFFB300) // Amber / Gold
val MeshColor4 = Color(0xFFE65100) // Dark Orange

// ── Glass Surfaces ─────────────────────────────────────────────────────────
// Light: warm cream-white card so cards are legible on saffron gradient
val GlassSurfaceLight = Color(0xFFFFF8F0) // Warm cream-white (fully opaque base)
val GlassSurfaceDark  = Color(0x26FFFFFF) // 15% White (dark glassmorphism)
val GlassBorderLight  = Color(0xFFE8C080) // Warm golden border (light mode)
val GlassBorderDark   = Color(0x55FFFFFF) // 33% White border (dark mode)

// ── Light Mode Color Scheme ────────────────────────────────────────────────
val PrimaryLight           = Color(0xFFE07B00) // Deeper saffron for contrast on white
val OnPrimaryLight         = Color(0xFFFFFFFF)
val PrimaryContainerLight  = Color(0xFFFFE5B4) // Pale peach/mango
val OnPrimaryContainerLight= Color(0xFF7A3C00)
val BackgroundLight        = Color(0xFFFFF3E0) // Warm orange-white
val OnBackgroundLight      = Color(0xFF3E2723) // Deep warm brown
val SurfaceLight           = Color(0xFFFFFBF5) // Off-white cream surface for cards
val OnSurfaceLight         = Color(0xFF3E2723) // Same warm brown — readable on cream
val SurfaceVariantLight    = Color(0xFFF5E6C8) // Light tan surface variant
val OnSurfaceVariantLight  = Color(0xFF6D4C41) // Mid brown for secondary text
val SecondaryLight         = Color(0xFF8BC34A)
val OnSecondaryLight       = Color(0xFFFFFFFF)

// ── Dark Mode Color Scheme ─────────────────────────────────────────────────
val PrimaryDark            = Color(0xFFFFB74D) // Lighter saffron for dark mode
val OnPrimaryDark          = Color(0xFF4E342E)
val PrimaryContainerDark   = Color(0xFF4A2800)
val OnPrimaryContainerDark = Color(0xFFFFE0B2)
val BackgroundDark         = Color(0xFF1A1208) // Very dark warm brown
val OnBackgroundDark       = Color(0xFFFFF3E0)
val SurfaceDark            = Color(0xFF261A0D) // Dark warm card surface
val OnSurfaceDark          = Color(0xFFF5E6C8) // Warm cream text on dark
val SurfaceVariantDark     = Color(0xFF3D2812)
val OnSurfaceVariantDark   = Color(0xFFD4A96A)
val SecondaryDark          = Color(0xFFAED581)
val OnSecondaryDark        = Color(0xFF33691E)
