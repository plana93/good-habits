package com.programminghut.pose_detection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Zero Dopamine Color Palette
 * Minimalist, calming, earthy tones to reduce cognitive load and overstimulation
 * Based on 2026 wellness app best practices
 */

// Earthy Terracotta & Sage Palette (primary colors)
private val Terracotta = Color(0xFFB76E5D) // Muted terracotta (warm, grounding)
private val TerracottaLight = Color(0xFFD4A59A) // Lighter terracotta for containers
private val Sage = Color(0xFF8A9A8E) // Muted sage green (calm, natural)
private val SageLight = Color(0xFFB8C4BC) // Lighter sage for backgrounds

// Neutral Gray Scale (for text and surfaces)
private val WarmGray900 = Color(0xFF2D2926) // Almost black (high contrast text)
private val WarmGray700 = Color(0xFF5A564F) // Dark gray (secondary text)
private val WarmGray400 = Color(0xFF9B9589) // Medium gray (disabled states)
private val WarmGray200 = Color(0xFFD9D3C8) // Light gray (dividers)
private val WarmGray50 = Color(0xFFF5F3EF) // Off-white background (softer than pure white)

// Accent Colors (minimal, intentional use only)
private val CalmBlue = Color(0xFF7A9AAA) // Soft blue for links/info
private val MutedRed = Color(0xFFC17A74) // Desaturated red for errors (non-alarming)

/**
 * Light Theme - Zero Dopamine Design
 * Warm, earthy tones with high readability and low visual noise
 */
private val LightColors = lightColorScheme(
    // Primary: Muted Terracotta (grounding, intentional)
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = TerracottaLight,
    onPrimaryContainer = WarmGray900,
    
    // Secondary: Sage (calm, supportive)
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = SageLight,
    onSecondaryContainer = WarmGray900,
    
    // Tertiary: Calm Blue (minimal use)
    tertiary = CalmBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4E3EC),
    onTertiaryContainer = WarmGray900,
    
    // Error: Muted Red (non-alarming)
    error = MutedRed,
    onError = Color.White,
    errorContainer = Color(0xFFF2DCD9),
    onErrorContainer = WarmGray900,
    
    // Background & Surface: Warm neutrals
    background = WarmGray50, // Soft off-white (easier on eyes than pure white)
    onBackground = WarmGray900,
    surface = Color.White,
    onSurface = WarmGray900,
    surfaceVariant = WarmGray200,
    onSurfaceVariant = WarmGray700,
    
    // Outline & borders
    outline = WarmGray400,
    outlineVariant = WarmGray200
)

/**
 * Dark Theme - Zero Dopamine Design
 * Deep, muted tones for low-light environments without harsh contrasts
 */
private val DarkColors = darkColorScheme(
    // Primary: Lighter Terracotta for dark mode
    primary = TerracottaLight,
    onPrimary = WarmGray900,
    primaryContainer = Color(0xFF8A5A4D),
    onPrimaryContainer = Color(0xFFEFDDD8),
    
    // Secondary: Lighter Sage for dark mode
    secondary = SageLight,
    onSecondary = WarmGray900,
    secondaryContainer = Color(0xFF5F6E63),
    onSecondaryContainer = Color(0xFFD9E3DC),
    
    // Tertiary: Calm Blue
    tertiary = CalmBlue,
    onTertiary = WarmGray900,
    tertiaryContainer = Color(0xFF4A5F6B),
    onTertiaryContainer = Color(0xFFD4E3EC),
    
    // Error: Muted Red
    error = Color(0xFFE0A19A),
    onError = WarmGray900,
    errorContainer = Color(0xFF8F5450),
    onErrorContainer = Color(0xFFF2DCD9),
    
    // Background & Surface: Deep warm tones
    background = Color(0xFF1A1816), // Deep warm black
    onBackground = Color(0xFFE8E3DC),
    surface = Color(0xFF241F1C), // Slightly lighter surface
    onSurface = Color(0xFFE8E3DC),
    surfaceVariant = Color(0xFF3D3833),
    onSurfaceVariant = Color(0xFFC7C1B6),
    
    // Outline & borders
    outline = Color(0xFF746F68),
    outlineVariant = Color(0xFF3D3833)
)

@Composable
fun Pose_detectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
