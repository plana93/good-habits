package com.programminghut.pose_detection.data.model

/**
 * SessionCreationParams - Parameters for creating manual sessions
 * 
 * Phase 4: Manual session creation
 */
data class SessionCreationParams(
    val timestamp: Long,              // When the workout was done
    val exerciseType: String,         // Type of exercise (squat, push-up, etc.)
    val totalReps: Int,               // Number of repetitions
    val durationSeconds: Int,         // Estimated duration in seconds
    val notes: String? = null,        // Optional notes
    val affectsStreak: Boolean = true // Whether this session counts for streak
)
