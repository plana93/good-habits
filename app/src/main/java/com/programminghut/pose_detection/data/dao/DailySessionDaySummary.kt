package com.programminghut.pose_detection.data.dao

/**
 * Summary DTO for daily sessions used in calendar aggregation queries.
 */
data class DailySessionDaySummary(
    val date: Long,
    val itemCount: Int,
    val completedCount: Int,
    val totalReps: Int
)
