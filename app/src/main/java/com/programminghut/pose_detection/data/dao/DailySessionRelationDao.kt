package com.programminghut.pose_detection.data.dao

import androidx.room.*
import androidx.room.RewriteQueriesToDropUnusedColumns
import com.programminghut.pose_detection.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO per query complesse con relazioni tra sessioni, esercizi e allenamenti
 */
@Dao
interface DailySessionRelationDao {
    
    /**
     * Ottieni sessione completa con tutti gli elementi popolati
     */
    @Transaction
    @Query("""
        SELECT * FROM daily_sessions 
        WHERE sessionId = :sessionId
    """)
    suspend fun getSessionWithItems(sessionId: Long): DailySessionWithItems?
    
    /**
     * Ottieni sessione completa per data (Flow)
     */
    @Transaction
    @Query("""
        SELECT * FROM daily_sessions 
        WHERE date >= :startOfDay AND date < :endOfDay 
        ORDER BY createdAt DESC 
        LIMIT 1
    """)
    fun getSessionWithItemsForDate(startOfDay: Long, endOfDay: Long): Flow<DailySessionWithItems?>
    
    /**
     * Ottieni elementi completi della sessione con dettagli esercizi/allenamenti
     */
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT dsi.itemId, dsi.sessionId, dsi.exerciseId, dsi.workoutId, 
               dsi.itemType as type, dsi.`order`,
               dsi.customReps as targetReps, dsi.customTime as targetTime, 
               dsi.actualReps, dsi.actualTime, 
               dsi.isCompleted, dsi.completedAt, COALESCE(dsi.notes, '') as notes,
               dsi.aiData,
               dsi.countsAsActivity,
               dsi.trackerTemplateId,
               dsi.trackerResponseJson,
               COALESCE(e.name, w.name, '') as name,
               COALESCE(e.description, w.description) as description,
               NULL as parentWorkoutItemId,
               e.name as exerciseName, e.description as exerciseDescription, 
               e.imagePath as exerciseImagePath, e.type as exerciseType, e.mode as exerciseMode,
               w.name as workoutName, w.description as workoutDescription,
               w.imagePath as workoutImagePath
        FROM daily_session_items dsi
        LEFT JOIN exercises e ON dsi.exerciseId = e.exerciseId
        LEFT JOIN workouts w ON dsi.workoutId = w.workoutId
        WHERE dsi.sessionId = :sessionId
        ORDER BY dsi.`order` ASC
    """)
    suspend fun getSessionItemsWithDetails(sessionId: Long): List<DailySessionItemWithDetails>
    
    /**
     * Ottieni elementi completi della sessione (Flow)
     */
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT dsi.itemId, dsi.sessionId, dsi.exerciseId, dsi.workoutId, 
               dsi.itemType as type, dsi.`order`,
               dsi.customReps as targetReps, dsi.customTime as targetTime, 
               dsi.actualReps, dsi.actualTime, 
               dsi.isCompleted, dsi.completedAt, COALESCE(dsi.notes, '') as notes,
               dsi.aiData,
               dsi.countsAsActivity,
               dsi.trackerTemplateId,
               dsi.trackerResponseJson,
               COALESCE(e.name, w.name, '') as name,
               COALESCE(e.description, w.description) as description,
               NULL as parentWorkoutItemId,
               e.name as exerciseName, e.description as exerciseDescription, 
               e.imagePath as exerciseImagePath, e.type as exerciseType, e.mode as exerciseMode,
               w.name as workoutName, w.description as workoutDescription,
               w.imagePath as workoutImagePath
        FROM daily_session_items dsi
        LEFT JOIN exercises e ON dsi.exerciseId = e.exerciseId
        LEFT JOIN workouts w ON dsi.workoutId = w.workoutId
        WHERE dsi.sessionId = :sessionId
        ORDER BY dsi.`order` ASC
    """)
    fun getSessionItemsWithDetailsFlow(sessionId: Long): Flow<List<DailySessionItemWithDetails>>
    
    /**
     * Ottieni cronologia sessioni con conteggio elementi completati
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT ds.*,
               COUNT(dsi.itemId) as totalItems,
               COUNT(CASE WHEN dsi.isCompleted = 1 THEN 1 END) as completedItems
        FROM daily_sessions ds
        LEFT JOIN daily_session_items dsi ON ds.sessionId = dsi.sessionId
        GROUP BY ds.sessionId
        ORDER BY ds.date DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getSessionsHistory(limit: Int = 20, offset: Int = 0): List<DailySessionSummary>
    
    /**
     * Ottieni cronologia sessioni (Flow)
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT ds.*,
               COUNT(dsi.itemId) as totalItems,
               COUNT(CASE WHEN dsi.isCompleted = 1 THEN 1 END) as completedItems
        FROM daily_sessions ds
        LEFT JOIN daily_session_items dsi ON ds.sessionId = dsi.sessionId
        GROUP BY ds.sessionId
        ORDER BY ds.date DESC
    """)
    fun getSessionsHistoryFlow(): Flow<List<DailySessionSummary>>
    
    /**
     * Ottieni statistiche per esercizio (utilizzo, performance)
     */
    @Query("""
        SELECT e.exerciseId as id, e.name, e.imagePath,
               COUNT(dsi.itemId) as usageCount,
               AVG(CASE WHEN dsi.actualReps IS NOT NULL THEN dsi.actualReps END) as avgReps,
               AVG(CASE WHEN dsi.actualTime IS NOT NULL THEN dsi.actualTime END) as avgTime,
               COUNT(CASE WHEN dsi.isCompleted = 1 THEN 1 END) as completionCount
        FROM exercises e
        LEFT JOIN daily_session_items dsi ON e.exerciseId = dsi.exerciseId
        WHERE dsi.sessionId IN (
            SELECT sessionId FROM daily_sessions 
            WHERE date >= :startDate AND date <= :endDate
        )
        GROUP BY e.exerciseId
        ORDER BY usageCount DESC
    """)
    suspend fun getExerciseStats(startDate: Long, endDate: Long): List<ExerciseStats>
    
    /**
     * Ottieni statistiche per allenamento
     */
    @Query("""
        SELECT w.workoutId as id, w.name, w.imagePath,
               COUNT(dsi.itemId) as usageCount,
               AVG(CASE WHEN dsi.actualTime IS NOT NULL THEN dsi.actualTime END) as avgDuration,
               COUNT(CASE WHEN dsi.isCompleted = 1 THEN 1 END) as completionCount
        FROM workouts w
        LEFT JOIN daily_session_items dsi ON w.workoutId = dsi.workoutId
        WHERE dsi.sessionId IN (
            SELECT sessionId FROM daily_sessions 
            WHERE date >= :startDate AND date <= :endDate
        )
        GROUP BY w.workoutId
        ORDER BY usageCount DESC
    """)
    suspend fun getWorkoutStats(startDate: Long, endDate: Long): List<WorkoutStats>
    
    /**
     * Crea sessione giornaliera da template allenamento
     */
    @Transaction
    suspend fun createSessionFromWorkout(workoutId: Long, sessionDate: Long): Long {
        // Implementazione nel Repository
        throw NotImplementedError("Implement in Repository")
    }
    
    /**
     * Aggiungi esercizio alla sessione corrente
     */
    @Transaction
    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long, order: Int): Long {
        // Implementazione nel Repository
        throw NotImplementedError("Implement in Repository")
    }
}

/**
 * Data class per statistiche esercizi
 */
data class ExerciseStats(
    val id: Long,
    val name: String,
    val imagePath: String?,
    val usageCount: Int,
    val avgReps: Double?,
    val avgTime: Double?,
    val completionCount: Int
)

/**
 * Data class per statistiche allenamenti
 */
data class WorkoutStats(
    val id: Long,
    val name: String,
    val imagePath: String?,
    val usageCount: Int,
    val avgDuration: Double?,
    val completionCount: Int
)

/**
 * Data class per riassunto sessioni
 */
data class DailySessionSummary(
    val sessionId: Long,
    val name: String,
    val date: Long,
    val createdAt: Long,
    val startTime: Long?,
    val endTime: Long?,
    val isCompleted: Boolean,
    val notes: String,
    val totalItems: Int,
    val completedItems: Int
) {
    val progress: Float
        get() = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f
        
    val duration: Long?
        get() = if (startTime != null && endTime != null) endTime - startTime else null
}