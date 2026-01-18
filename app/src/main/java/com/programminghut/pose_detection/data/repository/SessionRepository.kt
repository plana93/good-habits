package com.programminghut.pose_detection.data.repository

import com.programminghut.pose_detection.data.dao.RepDao
import com.programminghut.pose_detection.data.dao.SessionDao
import com.programminghut.pose_detection.data.dao.DailySessionDao
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
    private val repDao: RepDao,
    private val dailySessionDao: DailySessionDao? = null  // ✅ Added for calendar missed days calculation
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
    
    
    // ============================================================
    // PHASE 4: SESSION RECOVERY & CALENDAR
    // ============================================================
    
    /**
     * Create a manual session with estimated rep data
     * 
     * @param params Session creation parameters
     * @return The ID of the created session
     */
    suspend fun createManualSession(
        params: com.programminghut.pose_detection.data.model.SessionCreationParams
    ): Long {
        // Get previous sessions for estimation
        val previousSessions = getAllSessionsWithReps()
        
        // Calculate average metrics from previous sessions
        val avgMetrics = calculateAverageMetrics(previousSessions)
        
        // Create the session
        val session = WorkoutSession(
            sessionId = 0, // Auto-generated
            startTime = params.timestamp,
            endTime = params.timestamp + (params.durationSeconds * 1000L),
            durationSeconds = params.durationSeconds,
            exerciseType = params.exerciseType,
            totalReps = params.totalReps,
            avgDepthScore = avgMetrics.avgDepth,
            avgFormScore = avgMetrics.avgForm,
            avgSpeed = avgMetrics.avgSpeed,
            notes = params.notes,
            tags = emptyList(),
            location = null,
            appVersion = "2.0", // Current app version
            deviceModel = null,
            isSynced = false,
            exportedAt = null,
            sessionType = "MANUAL",
            recoveredDate = null,
            affectsStreak = params.affectsStreak
        )
        
        // Insert session
        val sessionId = sessionDao.insertSession(session)
        
        // Generate estimated reps
        val estimatedReps = com.programminghut.pose_detection.utils.RepDataEstimator.estimateReps(
            sessionId = sessionId,
            totalReps = params.totalReps,
            startTime = params.timestamp,
            previousSessions = previousSessions
        )
        
        // Insert estimated reps
        repDao.insertReps(estimatedReps)
        
        return sessionId
    }
    
    /**
     * Create a recovery session
     * 
     * @param session The recovery session to create
     * @param reps The reps performed during recovery
     * @param recoveredDate The date being recovered (start of day timestamp)
     * @return The ID of the created recovery session
     */
    suspend fun createRecoverySession(
        session: WorkoutSession,
        reps: List<RepData>,
        recoveredDate: Long
    ): Long {
        // Check if date is already recovered
        if (sessionDao.isDateAlreadyRecovered(recoveredDate)) {
            throw IllegalStateException("Date $recoveredDate has already been recovered")
        }
        
        // Create recovery session
        val recoverySession = session.copy(
            sessionType = "RECOVERY",
            recoveredDate = recoveredDate,
            affectsStreak = true
        )
        
        // Insert session and reps
        return insertCompleteWorkout(recoverySession, reps)
    }
    
    /**
     * Get sessions for a specific date range
     */
    suspend fun getSessionsForDateRange(startTime: Long, endTime: Long): List<WorkoutSession> {
        return sessionDao.getSessionsForCalendar(startTime, endTime)
    }

    /**
     * Reactive flow of sessions for a specific date range
     */
    fun getSessionsForDateRangeFlow(startTime: Long, endTime: Long): kotlinx.coroutines.flow.Flow<List<WorkoutSession>> {
        return sessionDao.getSessionsByDateRange(startTime, endTime)
    }
    
    /**
     * Get missed days within a date range
     * A day is "missed" if it has no streak-affecting sessions
     * ✅ UPDATED: Now checks BOTH workout_sessions AND daily_session_items (with countsAsActivity = true)
     * 
     * @param startDate Start of range (start of day timestamp)
     * @param endDate End of range (start of day timestamp)
     * @return List of timestamps for missed days
     */
    suspend fun getMissedDays(startDate: Long, endDate: Long): List<Long> {
        val missedDays = mutableListOf<Long>()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        
        var currentDay = startDate
        while (currentDay <= endDate) {
            val dayEnd = currentDay + oneDayMillis
            
            // ✅ Check BOTH systems:
            // 1. Old system: workout_sessions
            val hasWorkoutSessions = sessionDao.hasSessionsForDay(currentDay, dayEnd)
            
            // 2. New system: daily_session_items (only physical activities, not wellness trackers)
            val hasDailyActivities = dailySessionDao?.hasPhysicalActivityForDay(currentDay, dayEnd) ?: false
            
            // Day is NOT missed if it has activity in EITHER system
            val hasSessions = hasWorkoutSessions || hasDailyActivities
            
            // Don't count future days as missed
            if (!hasSessions && currentDay < System.currentTimeMillis()) {
                missedDays.add(currentDay)
            }
            
            currentDay += oneDayMillis
        }
        
        return missedDays
    }
    
    /**
     * Check if a day can be recovered
     * 
     * @param dayTimestamp Start of day timestamp
     * @param config Recovery configuration
     * @return True if day can be recovered
     */
    suspend fun canRecoverDay(
        dayTimestamp: Long,
        config: com.programminghut.pose_detection.data.model.RecoveryConfig
    ): Boolean {
        // Check if already recovered
        if (sessionDao.isDateAlreadyRecovered(dayTimestamp)) {
            return false
        }
        
        // Check if within allowed time window
        val currentTime = System.currentTimeMillis()
        val maxDaysBackMillis = config.maxDaysBack * 24 * 60 * 60 * 1000L
        val oldestRecoverableDate = currentTime - maxDaysBackMillis
        
        if (dayTimestamp < oldestRecoverableDate) {
            return false // Too old to recover
        }
        
        // Check if day is actually missed
        val dayEnd = dayTimestamp + (24 * 60 * 60 * 1000L)
        val hasSessions = sessionDao.hasSessionsForDay(dayTimestamp, dayEnd)
        
        return !hasSessions
    }
    
    /**
     * Calculate current streak with recovery logic
     * 
     * @return Current streak count (consecutive days with sessions, including recovered)
     */
    suspend fun calculateStreakWithRecovery(): Int {
        val oneDayMillis = 24 * 60 * 60 * 1000L
        var streak = 0
        val today = getStartOfDay(System.currentTimeMillis())
        
        // 🔥 FIX: Check if TODAY has activity first
        val todayEnd = today + oneDayMillis
        val todayHasSessions = sessionDao.hasSessionsForDay(today, todayEnd)
        val todayAllSessions = sessionDao.getSessionsForDay(today, todayEnd)
        val todayHasRecovery = todayAllSessions.any { it.sessionType == "RECOVERY" }
        val todayHasActivity = todayHasSessions || todayHasRecovery
        
        // Start from today if has activity, otherwise start from yesterday
        var currentDay = if (todayHasActivity) today else (today - oneDayMillis)
        
        // Go backwards day by day
        while (true) {
            val dayEnd = currentDay + oneDayMillis
            val hasSessions = sessionDao.hasSessionsForDay(currentDay, dayEnd)
            val allSessionsForDay = sessionDao.getSessionsForDay(currentDay, dayEnd)
            val hasRecoveryByType = allSessionsForDay.any { it.sessionType == "RECOVERY" }
            
            if (hasSessions || hasRecoveryByType) {
                streak++
                currentDay -= oneDayMillis
            } else {
                break
            }
        }
        
        return streak
    }
    
    /**
     * Calculate streak using both WorkoutSession AND DailySession data
     * This is used by CalendarViewModel which has access to both data sources
     * 
     * ⚠️ IMPORTANTE: NON usa la mappa dailySummaries.containsKey() perché quella
     * contiene solo giorni con countsAsActivity=1, ma deve controllare direttamente il DB
     */
    suspend fun calculateStreakWithDailySessions(
        dailySummaries: Map<Long, com.programminghut.pose_detection.data.dao.DailySessionDaySummary>
    ): Int {
        val oneDayMillis = 24 * 60 * 60 * 1000L
        var streak = 0
        val today = getStartOfDay(System.currentTimeMillis())
        
        // 🔥 FIX: Check if TODAY has activity first usando le query DB corrette
        val todayEnd = today + oneDayMillis
        val todayHasSessions = sessionDao.hasSessionsForDay(today, todayEnd)
        val todayAllSessions = sessionDao.getSessionsForDay(today, todayEnd)
        val todayHasRecovery = todayAllSessions.any { it.sessionType == "RECOVERY" }
        // ✅ Usa hasPhysicalActivityForDay() invece di containsKey()
        val todayHasDailyActivity = dailySessionDao?.hasPhysicalActivityForDay(today, todayEnd) ?: false
        val todayHasActivity = todayHasSessions || todayHasRecovery || todayHasDailyActivity
        
        // Start from today if has activity, otherwise start from yesterday
        var currentDay = if (todayHasActivity) today else (today - oneDayMillis)
        
        // Go backwards day by day
        while (true) {
            val dayEnd = currentDay + oneDayMillis
            
            // Check WorkoutSession
            val hasSessions = sessionDao.hasSessionsForDay(currentDay, dayEnd)
            val allSessionsForDay = sessionDao.getSessionsForDay(currentDay, dayEnd)
            val hasRecoveryByType = allSessionsForDay.any { it.sessionType == "RECOVERY" }
            
            // ✅ Check DailySession usando query DB invece di containsKey()
            val hasDailyActivity = dailySessionDao?.hasPhysicalActivityForDay(currentDay, dayEnd) ?: false
            
            // Count if either WorkoutSession OR DailySession has data
            if (hasSessions || hasRecoveryByType || hasDailyActivity) {
                streak++
                currentDay -= oneDayMillis
            } else {
                break
            }
        }
        
        return streak
    }
    
    /**
     * Get all sessions with their reps (for estimation)
     */
    private suspend fun getAllSessionsWithReps(): List<Pair<WorkoutSession, List<RepData>>> {
        val sessions = sessionDao.getAllSessions()
        val result = mutableListOf<Pair<WorkoutSession, List<RepData>>>()
        
        // This is a simplification - in a real implementation you'd use Flow properly
        // For now, we'll return empty list and fallback to defaults
        return emptyList()
    }
    
    /**
     * Calculate average metrics from previous sessions
     */
    private fun calculateAverageMetrics(
        sessions: List<Pair<WorkoutSession, List<RepData>>>
    ): AverageMetrics {
        if (sessions.isEmpty()) {
            return AverageMetrics(
                avgDepth = 0.75f,
                avgForm = 0.75f,
                avgSpeed = 2.5f
            )
        }
        
        val avgDepth = sessions.map { it.first.avgDepthScore }.average().toFloat()
        val avgForm = sessions.map { it.first.avgFormScore }.average().toFloat()
        val avgSpeed = sessions.map { it.first.avgSpeed }.average().toFloat()
        
        return AverageMetrics(avgDepth, avgForm, avgSpeed)
    }
    
    /**
     * Get start of day timestamp (00:00:00.000)
     */
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * ✅ Verifica se una data è stata recuperata
     * @param dateTimestamp Il timestamp della data da verificare (inizio giorno)
     * @return True se esiste una sessione RECOVERY per quella data
     */
    suspend fun isDateRecovered(dateTimestamp: Long): Boolean {
        return sessionDao.isDateAlreadyRecovered(dateTimestamp)
    }
    
    /**
     * Data class for average metrics
     */
    private data class AverageMetrics(
        val avgDepth: Float,
        val avgForm: Float,
        val avgSpeed: Float
    )
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
