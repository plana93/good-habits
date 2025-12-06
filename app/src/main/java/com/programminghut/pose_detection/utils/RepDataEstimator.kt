package com.programminghut.pose_detection.utils

import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.WorkoutSession

/**
 * RepDataEstimator - Utility for estimating rep data from historical sessions
 * 
 * Phase 4: Manual Session Creation
 * When creating manual sessions, we need to generate plausible RepData based on
 * the user's historical performance patterns.
 */
object RepDataEstimator {
    
    /**
     * Estimate RepData entries for a manual session based on previous sessions
     * 
     * @param sessionId ID of the session being created
     * @param totalReps Number of reps in the manual session
     * @param startTime Start timestamp of the session
     * @param previousSessions List of previous sessions to base estimates on
     * @return List of estimated RepData entries
     */
    fun estimateReps(
        sessionId: Long,
        totalReps: Int,
        startTime: Long,
        previousSessions: List<Pair<WorkoutSession, List<RepData>>>
    ): List<RepData> {
        // Calculate average metrics from previous sessions
        val avgMetrics = calculateAverageMetrics(previousSessions)
        
        // Generate estimated reps
        val estimatedReps = mutableListOf<RepData>()
        val avgRepDuration = avgMetrics.avgSpeed * 1000 // Convert to milliseconds
        
        for (i in 1..totalReps) {
            // Add some natural variation (±10%) to make data realistic
            val depthVariation = (Math.random() * 0.2 - 0.1).toFloat() // ±10%
            val formVariation = (Math.random() * 0.2 - 0.1).toFloat()
            val speedVariation = (Math.random() * 0.2 - 0.1).toFloat()
            
            val estimatedRep = RepData(
                repId = 0, // Will be auto-generated
                sessionId = sessionId,
                repNumber = i,
                timestamp = startTime + (i * avgRepDuration).toLong(),
                depthScore = (avgMetrics.avgDepth + depthVariation).coerceIn(0f, 1f),
                formScore = (avgMetrics.avgForm + formVariation).coerceIn(0f, 1f),
                speed = (avgMetrics.avgSpeed + speedVariation).coerceAtLeast(0.5f),
                keypoints = null, // No keypoints for manual sessions
                confidence = 0.5f, // Indicate estimated data
                isFlaggedForReview = false
            )
            
            estimatedReps.add(estimatedRep)
        }
        
        return estimatedReps
    }
    
    /**
     * Calculate average metrics from previous sessions
     */
    private fun calculateAverageMetrics(
        previousSessions: List<Pair<WorkoutSession, List<RepData>>>
    ): AverageMetrics {
        if (previousSessions.isEmpty()) {
            // Default values if no history available
            return AverageMetrics(
                avgDepth = 0.75f,
                avgForm = 0.75f,
                avgSpeed = 2.5f // seconds per rep
            )
        }
        
        // Collect all reps from previous sessions
        val allReps = previousSessions.flatMap { it.second }
        
        if (allReps.isEmpty()) {
            // Fallback to session-level averages
            val avgDepth = previousSessions.map { it.first.avgDepthScore }.average().toFloat()
            val avgForm = previousSessions.map { it.first.avgFormScore }.average().toFloat()
            val avgSpeed = previousSessions.map { it.first.avgSpeed }.average().toFloat()
            
            return AverageMetrics(avgDepth, avgForm, avgSpeed)
        }
        
        // Calculate averages from rep-level data
        val avgDepth = allReps.map { it.depthScore }.average().toFloat()
        val avgForm = allReps.map { it.formScore }.average().toFloat()
        val avgSpeed = allReps.map { it.speed }.average().toFloat()
        
        return AverageMetrics(avgDepth, avgForm, avgSpeed)
    }
    
    /**
     * Data class to hold average metrics
     */
    private data class AverageMetrics(
        val avgDepth: Float,
        val avgForm: Float,
        val avgSpeed: Float
    )
}
