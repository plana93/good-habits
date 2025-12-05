package com.programminghut.pose_detection.data.repository

import com.programminghut.pose_detection.data.dao.RepDao
import com.programminghut.pose_detection.data.dao.SessionDao
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for Workout Sessions
 * 
 * This class abstracts access to the data layer and provides a clean API
 * for the rest of the app. It handles data operations and business logic.
 * 
 * Single source of truth for workout session data.
 */
class SessionRepository(
    private val sessionDao: SessionDao,
    private val repDao: RepDao
) {
    
    // ============================================================
    // SESSION OPERATIONS
    // ============================================================
    
    /**
     * Get all sessions ordered by most recent first
     */
    fun getAllSessions(): Flow<List<WorkoutSession>> {
        return sessionDao.getAllSessions()
    }
    
    /**
     * Get a specific session by ID
     */
    suspend fun getSessionById(sessionId: Long): WorkoutSession? {
        return sessionDao.getSessionById(sessionId)
    }
    
    /**
     * Get a specific session by ID as Flow
     */
    fun getSessionByIdFlow(sessionId: Long): Flow<WorkoutSession?> {
        return sessionDao.getSessionByIdFlow(sessionId)
    }
    
    /**
     * Insert a new session and return its ID
     */
    suspend fun insertSession(session: WorkoutSession): Long {
        return sessionDao.insertSession(session)
    }
    
    /**
     * Insert a complete workout with session and reps in a single transaction
     * 
     * @param session The workout session to insert
     * @param reps List of reps for this session
     * @return The ID of the inserted session
     */
    suspend fun insertCompleteWorkout(
        session: WorkoutSession,
        reps: List<RepData>
    ): Long {
        // Insert session first to get the sessionId
        val sessionId = sessionDao.insertSession(session)
        
        // Associate reps with the session and insert them
        val repsWithSessionId = reps.map { it.copy(sessionId = sessionId) }
        repDao.insertReps(repsWithSessionId)
        
        return sessionId
    }
    
    /**
     * Update an existing session
     */
    suspend fun updateSession(session: WorkoutSession) {
        sessionDao.updateSession(session)
    }
    
    /**
     * Update session notes
     */
    suspend fun updateSessionNotes(sessionId: Long, notes: String) {
        sessionDao.updateSessionNotes(sessionId, notes)
    }
    
    /**
     * Update session tags
     */
    suspend fun updateSessionTags(sessionId: Long, tags: List<String>) {
        // Convert list to JSON string for storage
        val tagsJson = tags.joinToString("\",\"", "[\"", "\"]")
        sessionDao.updateSessionTags(sessionId, tagsJson)
    }
    
    /**
     * Delete a session (will cascade delete associated reps)
     */
    suspend fun deleteSession(session: WorkoutSession) {
        sessionDao.deleteSession(session)
    }
    
    /**
     * Delete a session by ID
     */
    suspend fun deleteSessionById(sessionId: Long) {
        sessionDao.deleteSessionById(sessionId)
    }
    
    
    // ============================================================
    // REP OPERATIONS
    // ============================================================
    
    /**
     * Get all reps for a specific session
     */
    fun getRepsForSession(sessionId: Long): Flow<List<RepData>> {
        return repDao.getRepsForSession(sessionId)
    }
    
    /**
     * Get all reps for a session (one-time read)
     */
    suspend fun getRepsForSessionOnce(sessionId: Long): List<RepData> {
        return repDao.getRepsForSessionOnce(sessionId)
    }
    
    /**
     * Insert a single rep
     */
    suspend fun insertRep(rep: RepData): Long {
        return repDao.insertRep(rep)
    }
    
    /**
     * Insert multiple reps
     */
    suspend fun insertReps(reps: List<RepData>): List<Long> {
        return repDao.insertReps(reps)
    }
    
    /**
     * Get reps flagged for review
     */
    fun getFlaggedRepsForSession(sessionId: Long): Flow<List<RepData>> {
        return repDao.getFlaggedRepsForSession(sessionId)
    }
    
    /**
     * Flag a rep for review
     */
    suspend fun flagRepForReview(repId: Long, isFlagged: Boolean) {
        repDao.flagRepForReview(repId, isFlagged)
    }
    
    
    // ============================================================
    // FILTERED QUERIES
    // ============================================================
    
    /**
     * Get sessions filtered by exercise type
     */
    fun getSessionsByExerciseType(exerciseType: String): Flow<List<WorkoutSession>> {
        return sessionDao.getSessionsByExerciseType(exerciseType)
    }
    
    /**
     * Get sessions within a date range
     */
    fun getSessionsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutSession>> {
        return sessionDao.getSessionsByDateRange(startTime, endTime)
    }
    
    /**
     * Get today's sessions
     */
    fun getTodaySessions(dayStart: Long, dayEnd: Long): Flow<List<WorkoutSession>> {
        return sessionDao.getTodaySessions(dayStart, dayEnd)
    }
    
    /**
     * Get this week's sessions
     */
    fun getWeekSessions(weekStart: Long, weekEnd: Long): Flow<List<WorkoutSession>> {
        return sessionDao.getWeekSessions(weekStart, weekEnd)
    }
    
    /**
     * Get recent N sessions
     */
    fun getRecentSessions(limit: Int): Flow<List<WorkoutSession>> {
        return sessionDao.getRecentSessions(limit)
    }
    
    
    // ============================================================
    // ANALYTICS & STATISTICS
    // ============================================================
    
    /**
     * Get total number of sessions
     */
    fun getTotalSessionsCount(): Flow<Int> {
        return sessionDao.getTotalSessionsCount()
    }
    
    /**
     * Get total reps across all sessions
     */
    fun getTotalRepsAllTime(): Flow<Int> {
        return sessionDao.getTotalRepsAllTime().map { it ?: 0 }
    }
    
    /**
     * Get total reps for a specific exercise type
     */
    fun getTotalRepsByType(exerciseType: String): Flow<Int> {
        return sessionDao.getTotalRepsByType(exerciseType).map { it ?: 0 }
    }
    
    /**
     * Get total workout time in seconds
     */
    fun getTotalWorkoutTimeSeconds(): Flow<Int> {
        return sessionDao.getTotalWorkoutTimeSeconds().map { it ?: 0 }
    }
    
    /**
     * Get average form score across all sessions
     */
    fun getAverageFormScore(): Flow<Float> {
        return sessionDao.getAverageFormScore().map { it ?: 0f }
    }
    
    /**
     * Get best session by form score for an exercise
     */
    suspend fun getBestSessionByFormScore(exerciseType: String): WorkoutSession? {
        return sessionDao.getBestSessionByFormScore(exerciseType)
    }
    
    /**
     * Get session with highest rep count
     */
    suspend fun getSessionWithMaxReps(): WorkoutSession? {
        return sessionDao.getSessionWithMaxReps()
    }
    
    /**
     * Get detailed statistics for a session
     */
    suspend fun getSessionStatistics(sessionId: Long): SessionStatistics? {
        val session = sessionDao.getSessionById(sessionId) ?: return null
        val repCount = repDao.getRepCountForSession(sessionId)
        val avgDepth = repDao.getAverageDepthScore(sessionId) ?: 0f
        val avgForm = repDao.getAverageFormScore(sessionId) ?: 0f
        val avgSpeed = repDao.getAverageSpeed(sessionId) ?: 0f
        val bestRep = repDao.getBestRepForSession(sessionId)
        val worstRep = repDao.getWorstRepForSession(sessionId)
        val flaggedCount = repDao.getFlaggedRepCount(sessionId)
        
        return SessionStatistics(
            session = session,
            totalReps = repCount,
            avgDepthScore = avgDepth,
            avgFormScore = avgForm,
            avgSpeed = avgSpeed,
            bestRep = bestRep,
            worstRep = worstRep,
            flaggedRepsCount = flaggedCount
        )
    }
    
    
    // ============================================================
    // BULK OPERATIONS
    // ============================================================
    
    /**
     * Delete old sessions (older than timestamp)
     */
    suspend fun deleteOldSessions(timestamp: Long) {
        sessionDao.deleteSessionsOlderThan(timestamp)
    }
    
    /**
     * Clear all data (use with caution!)
     */
    suspend fun clearAllData() {
        sessionDao.deleteAllSessions()
        repDao.deleteAllReps()
    }
}

/**
 * Data class representing detailed statistics for a session
 */
data class SessionStatistics(
    val session: WorkoutSession,
    val totalReps: Int,
    val avgDepthScore: Float,
    val avgFormScore: Float,
    val avgSpeed: Float,
    val bestRep: RepData?,
    val worstRep: RepData?,
    val flaggedRepsCount: Int
)
