package com.programminghut.pose_detection.data.model

/**
 * SessionType - Enumeration for workout session types
 * 
 * Phase 4: Session Recovery & Calendar implementation
 */
enum class SessionType {
    /**
     * Real-time session recorded with camera and pose detection
     */
    REAL_TIME,
    
    /**
     * Manually added session (workout done without app)
     */
    MANUAL,
    
    /**
     * Recovery session to restore missed day in streak
     */
    RECOVERY
}
