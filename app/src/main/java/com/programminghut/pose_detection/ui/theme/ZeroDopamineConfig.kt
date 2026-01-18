package com.programminghut.pose_detection.ui.theme

import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Zero Dopamine Design Configuration
 * 
 * Following 2026 wellness app best practices:
 * - Minimalist UI with reduced cognitive load (20% rule)
 * - Predictable, steady feedback (no variable rewards)
 * - Calming animations and haptics
 * - Focus on utility over engagement
 */
object ZeroDopamineConfig {
    
    /**
     * Animation Durations - Slow, predictable, calming
     * No sudden or flashy animations
     */
    object Animation {
        const val QUICK = 200 // Micro-interactions (button press)
        const val STANDARD = 400 // Standard transitions
        const val SLOW = 600 // Page transitions
        const val NONE = 0 // For distraction-free modes
        
        // Standard tween for all animations (no bouncing or spring effects)
        val standardTween = tween<Float>(durationMillis = STANDARD)
        val slowTween = tween<Float>(durationMillis = SLOW)
    }
    
    /**
     * Spacing - Generous whitespace following the 20% rule
     * Reduces cognitive load by creating visual breathing room
     */
    object Spacing {
        val extraSmall = 4.dp
        val small = 8.dp
        val medium = 16.dp
        val large = 24.dp
        val extraLarge = 32.dp
        val huge = 48.dp
        
        // Content padding for reading comfort
        val contentPadding = medium
        val sectionSpacing = large
    }
    
    /**
     * Shapes - Soft, rounded corners for calm aesthetic
     */
    object Shapes {
        val small = RoundedCornerShape(8.dp)
        val medium = RoundedCornerShape(12.dp)
        val large = RoundedCornerShape(16.dp)
        val extraLarge = RoundedCornerShape(24.dp)
    }
    
    /**
     * Typography Scale - High readability, minimal hierarchy
     */
    object Typography {
        const val DISPLAY_SIZE = 32 // Rare use, only for critical info
        const val TITLE_SIZE = 24 // Section headers
        const val HEADLINE_SIZE = 20 // Card titles
        const val BODY_SIZE = 16 // Main content (high readability)
        const val CAPTION_SIZE = 14 // Secondary info
        const val LABEL_SIZE = 12 // Minimal use
    }
    
    /**
     * Haptic Feedback - Gentle, not jarring
     * Use sparingly for confirmations only
     */
    object Haptics {
        const val SUCCESS_DURATION = 50L // Light tap for completion
        const val ERROR_DURATION = 100L // Slightly longer for errors
        const val ENABLED = true // Can be disabled for focus mode
    }
    
    /**
     * Notifications - Smart, context-aware
     * Only when user is likely to act, avoid notification fatigue
     */
    object Notifications {
        const val GENTLE_NUDGE_DELAY = 300_000L // 5 minutes after ideal time
        const val MAX_DAILY_NUDGES = 2 // Prevent spam
        const val QUIET_HOURS_START = 21 // 9 PM
        const val QUIET_HOURS_END = 8 // 8 AM
    }
    
    /**
     * Progress Visualization - Internal motivation, not competition
     */
    object Progress {
        const val SHOW_LEADERBOARDS = false // No social comparison
        const val SHOW_STREAK_CHAIN = true // Simple day chain
        const val SHOW_XP_LEVELS = false // No gamification
        const val SHOW_PERSONAL_TRENDS = true // Heat maps, trend lines
    }
    
    /**
     * Focus Mode - Distraction-free workout/meditation
     * Hides all non-essential UI elements
     */
    object FocusMode {
        const val HIDE_NOTIFICATIONS = true
        const val HIDE_STATS = true
        const val SHOW_TIMER_ONLY = true
        const val DIM_BACKGROUND = true
    }
    
    /**
     * Micro-Goals - Actionable, measurable targets
     */
    object Goals {
        val DEFAULT_WORKOUT_DURATION = 10 // minutes (achievable)
        val DEFAULT_DAILY_REPS = 20 // realistic target
        const val ENCOURAGE_MICRO_HABITS = true // "Just 1 minute is enough"
    }
}
