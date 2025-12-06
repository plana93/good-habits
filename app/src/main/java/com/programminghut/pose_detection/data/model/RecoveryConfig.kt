package com.programminghut.pose_detection.data.model

/**
 * RecoveryConfig - Configuration for recovery session rules
 * 
 * Phase 4: Session Recovery system
 */
data class RecoveryConfig(
    val minRepsRequired: Int = 50,    // Minimum reps to complete recovery
    val maxDaysBack: Int = 7,         // Maximum days back that can be recovered
    val isEnabled: Boolean = true     // Whether recovery system is enabled
)
