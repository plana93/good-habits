package com.programminghut.pose_detection.data.dao

import androidx.room.*
import com.programminghut.pose_detection.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WorkoutSession entities
 * 
 * Provides methods to interact with workout_sessions table.
 * Uses Flow for reactive data streams and suspend functions for coroutines.
 */
@Dao
interface SessionDao {
    
    // ============================================================
    // CREATE
    // ============================================================
    
    /**
     * Insert a new workout session
     * @return The ID of the inserted session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long
    
    /**
     * Insert multiple sessions at once
     * @return List of inserted session IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSession>): List<Long>
    
    
    // ============================================================
    // READ - Basic Queries
    // ============================================================
    
    /**
     * Get all workout sessions ordered by start time (most recent first)
     */
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>
    
    /**
     * Get a specific session by ID
     */
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Long): WorkoutSession?
    
    /**
     * Get a specific session by ID as Flow (for reactive updates)
     */
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    fun getSessionByIdFlow(sessionId: Long): Flow<WorkoutSession?>
    
    
    // ============================================================
    // READ - Filtered Queries
    // ============================================================
    
    /**
     * Get sessions filtered by exercise type
     */
    @Query("SELECT * FROM workout_sessions WHERE exerciseType = :exerciseType ORDER BY startTime DESC")
    fun getSessionsByExerciseType(exerciseType: String): Flow<List<WorkoutSession>>
    
    /**
     * Get sessions within a date range
     * @param startTime Start of range (unix timestamp in milliseconds)
     * @param endTime End of range (unix timestamp in milliseconds)
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE startTime >= :startTime AND startTime <= :endTime 
        ORDER BY startTime DESC
    """)
    fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>>
    
    /**
     * Get sessions with a specific tag
     */
    @Query("SELECT * FROM workout_sessions WHERE tags LIKE '%' || :tag || '%' ORDER BY startTime DESC")
    fun getSessionsByTag(tag: String): Flow<List<WorkoutSession>>
    
    
    // ============================================================
    // READ - Analytics & Statistics
    // ============================================================
    
    /**
     * Get total number of sessions
     */
    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun getTotalSessionsCount(): Flow<Int>
    
    /**
     * Get total number of sessions for a specific exercise type
     */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE exerciseType = :exerciseType")
    fun getSessionsCountByType(exerciseType: String): Flow<Int>
    
    /**
     * Get total reps across all sessions
     */
    @Query("SELECT SUM(totalReps) FROM workout_sessions")
    fun getTotalRepsAllTime(): Flow<Int?>
    
    /**
     * Get total reps for a specific exercise type
     */
    @Query("SELECT SUM(totalReps) FROM workout_sessions WHERE exerciseType = :exerciseType")
    fun getTotalRepsByType(exerciseType: String): Flow<Int?>
    
    /**
     * Get total workout time in seconds
     */
    @Query("SELECT SUM(durationSeconds) FROM workout_sessions")
    fun getTotalWorkoutTimeSeconds(): Flow<Int?>
    
    /**
     * Get average form score across all sessions
     */
    @Query("SELECT AVG(avgFormScore) FROM workout_sessions")
    fun getAverageFormScore(): Flow<Float?>
    
    /**
     * Get recent sessions (last N sessions)
     */
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<WorkoutSession>>
    
    /**
     * Get sessions for today
     * @param dayStartTimestamp Start of day (00:00) in milliseconds
     * @param dayEndTimestamp End of day (23:59) in milliseconds
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE startTime >= :dayStartTimestamp AND startTime <= :dayEndTimestamp
        ORDER BY startTime DESC
    """)
    fun getTodaySessions(dayStartTimestamp: Long, dayEndTimestamp: Long): Flow<List<WorkoutSession>>
    
    /**
     * Get sessions for current week
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE startTime >= :weekStartTimestamp AND startTime <= :weekEndTimestamp
        ORDER BY startTime DESC
    """)
    fun getWeekSessions(weekStartTimestamp: Long, weekEndTimestamp: Long): Flow<List<WorkoutSession>>
    
    /**
     * Get best session by form score for a specific exercise
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE exerciseType = :exerciseType 
        ORDER BY avgFormScore DESC 
        LIMIT 1
    """)
    suspend fun getBestSessionByFormScore(exerciseType: String): WorkoutSession?
    
    /**
     * Get session with highest rep count
     */
    @Query("SELECT * FROM workout_sessions ORDER BY totalReps DESC LIMIT 1")
    suspend fun getSessionWithMaxReps(): WorkoutSession?
    
    
    // ============================================================
    // UPDATE
    // ============================================================
    
    /**
     * Update an existing session
     */
    @Update
    suspend fun updateSession(session: WorkoutSession)
    
    /**
     * Update session notes
     */
    @Query("UPDATE workout_sessions SET notes = :notes WHERE sessionId = :sessionId")
    suspend fun updateSessionNotes(sessionId: Long, notes: String)
    
    /**
     * Update session tags
     */
    @Query("UPDATE workout_sessions SET tags = :tags WHERE sessionId = :sessionId")
    suspend fun updateSessionTags(sessionId: Long, tags: String)
    
    /**
     * Mark session as synced
     */
    @Query("UPDATE workout_sessions SET isSynced = :isSynced WHERE sessionId = :sessionId")
    suspend fun updateSyncStatus(sessionId: Long, isSynced: Boolean)
    
    /**
     * Update export timestamp
     */
    @Query("UPDATE workout_sessions SET exportedAt = :exportedAt WHERE sessionId = :sessionId")
    suspend fun updateExportTimestamp(sessionId: Long, exportedAt: Long)
    
    
    // ============================================================
    // DELETE
    // ============================================================
    
    /**
     * Delete a specific session
     * Note: This will cascade delete all associated RepData entries
     */
    @Delete
    suspend fun deleteSession(session: WorkoutSession)
    
    /**
     * Delete a session by ID
     */
    @Query("DELETE FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    /**
     * Delete all sessions (use with caution!)
     */
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()
    
    /**
     * Delete sessions older than a specific timestamp
     */
    @Query("DELETE FROM workout_sessions WHERE startTime < :timestamp")
    suspend fun deleteSessionsOlderThan(timestamp: Long)
    
    
    // ============================================================
    // PHASE 4: Session Recovery & Calendar Queries
    // ============================================================
    
    /**
     * Get sessions filtered by type
     */
    @Query("SELECT * FROM workout_sessions WHERE sessionType = :sessionType ORDER BY startTime DESC")
    fun getSessionsByType(sessionType: String): Flow<List<WorkoutSession>>
    
    /**
     * Get sessions that affect streak
     */
    @Query("SELECT * FROM workout_sessions WHERE affectsStreak = 1 ORDER BY startTime DESC")
    fun getStreakAffectingSessions(): Flow<List<WorkoutSession>>
    
    /**
     * Get sessions for a specific date (day)
     * @param dayStartTimestamp Start of day (00:00) in milliseconds
     * @param dayEndTimestamp End of day (23:59) in milliseconds
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE startTime >= :dayStartTimestamp 
        AND startTime < :dayEndTimestamp
        AND affectsStreak = 1
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long): List<WorkoutSession>
    
    /**
     * Check if a specific day has any sessions
     * @param dayStartTimestamp Start of day (00:00) in milliseconds
     * @param dayEndTimestamp End of day (23:59) in milliseconds
     * @return True if day has at least one streak-affecting session
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM workout_sessions 
        WHERE startTime >= :dayStartTimestamp 
        AND startTime < :dayEndTimestamp
        AND affectsStreak = 1
    """)
    suspend fun hasSessionsForDay(dayStartTimestamp: Long, dayEndTimestamp: Long): Boolean
    
    /**
     * Get recovery sessions
     */
    @Query("SELECT * FROM workout_sessions WHERE sessionType = 'RECOVERY' ORDER BY startTime DESC")
    fun getRecoverySessions(): Flow<List<WorkoutSession>>
    
    /**
     * Get manual sessions
     */
    @Query("SELECT * FROM workout_sessions WHERE sessionType = 'MANUAL' ORDER BY startTime DESC")
    fun getManualSessions(): Flow<List<WorkoutSession>>
    
    /**
     * Check if a date has already been recovered
     * @param recoveredDate The date (start of day timestamp) that was recovered
     * @return True if a recovery session exists for that date
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM workout_sessions 
        WHERE sessionType = 'RECOVERY' 
        AND recoveredDate = :recoveredDate
    """)
    suspend fun isDateAlreadyRecovered(recoveredDate: Long): Boolean
    
    /**
     * Get all sessions for calendar display (with recovery info)
     * Returns sessions from a date range for calendar visualization
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE startTime >= :startTime 
        AND startTime <= :endTime
        ORDER BY startTime ASC
    """)
    suspend fun getSessionsForCalendar(startTime: Long, endTime: Long): List<WorkoutSession>
    
    /**
     * Count sessions by type
     */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE sessionType = :sessionType")
    fun getCountBySessionType(sessionType: String): Flow<Int>
}
