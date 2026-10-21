package com.teasupplier.app.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun GlassBackground(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val gradientBrush = if (darkTheme) {
        // Dark mode: deep warm brown with saffron accents — rich & moody
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1A1208),          // Very dark warm brown (top-left)
                Color(0xFF2E1A06),          // Dark amber-brown (mid)
                Color(0xFF3D2000),          // Deep saffron-brown (bottom-right)
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 2000f)
        )
    } else {
        // Light mode: warm saffron → amber → cream gradient — visible and vibrant
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFE0B2),          // Light peach-amber (top)
                Color(0xFFFFCC70),          // Warm amber-gold (mid)
                Color(0xFFFFF3E0),          // Soft cream (bottom)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 1800f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
    ) {
        content()
    }
}
