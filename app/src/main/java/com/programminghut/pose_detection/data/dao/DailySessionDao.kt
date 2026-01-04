package com.programminghut.pose_detection.data.dao

import androidx.room.*
import com.programminghut.pose_detection.data.model.DailySession
import com.programminghut.pose_detection.data.model.DailySessionItem
import com.programminghut.pose_detection.data.model.DailySessionWithItems
import kotlinx.coroutines.flow.Flow

/**
 * DAO per gestire le sessioni giornaliere
 */
@Dao
interface DailySessionDao {
    
    /**
     * Inserisci una nuova sessione giornaliera
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySession(session: DailySession): Long
    
    /**
     * Inserisci elementi nella sessione
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionItems(items: List<DailySessionItem>)
    
    /**
     * Inserisci singolo elemento nella sessione
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionItem(item: DailySessionItem): Long
    
    /**
     * Ottieni sessione giornaliera per data
     */
    @Query("""
        SELECT * FROM daily_sessions 
        WHERE date >= :startOfDay AND date < :endOfDay 
        ORDER BY createdAt DESC 
        LIMIT 1
    """)
    suspend fun getSessionForDate(startOfDay: Long, endOfDay: Long): DailySession?
    
    /**
     * Ottieni sessione giornaliera per data (Flow)
     */
    @Query("""
        SELECT * FROM daily_sessions 
        WHERE date >= :startOfDay AND date < :endOfDay 
        ORDER BY createdAt DESC 
        LIMIT 1
    """)
    fun getSessionForDateFlow(startOfDay: Long, endOfDay: Long): Flow<DailySession?>
    
    /**
     * Ottieni tutte le sessioni
     */
    @Query("SELECT * FROM daily_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<DailySession>>

    /**
     * Ottieni sessione per ID
     */
    @Query("SELECT * FROM daily_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): DailySession?

    /**
     * Ottieni le date (startOfDay equivalenti) delle sessioni che hanno items in un range
     */
    @Query("""
        SELECT DISTINCT date FROM daily_sessions s
        JOIN daily_session_items i ON s.sessionId = i.sessionId
        WHERE s.date >= :startOfDay AND s.date <= :endOfDay
    """)
    suspend fun getSessionDatesWithItemsInRange(startOfDay: Long, endOfDay: Long): List<Long>

    @Query("""
        SELECT s.date as date,
               COUNT(i.itemId) as itemCount,
               SUM(CASE WHEN i.isCompleted = 1 THEN 1 ELSE 0 END) as completedCount,
               COALESCE(SUM(
                   CASE
                       WHEN i.actualReps IS NOT NULL THEN i.actualReps
                       WHEN i.customReps IS NOT NULL THEN i.customReps
                       ELSE 0
                   END
               ), 0) as totalReps
        FROM daily_sessions s
        LEFT JOIN daily_session_items i ON s.sessionId = i.sessionId
        WHERE s.date >= :startOfDay AND s.date <= :endOfDay
        GROUP BY s.date
    """)
    fun getDailySessionSummariesInRange(startOfDay: Long, endOfDay: Long): kotlinx.coroutines.flow.Flow<List<DailySessionDaySummary>>

    /**
     * Get total reps for a specific exercise (by exerciseId) within a specific day.
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN i.actualReps IS NOT NULL THEN i.actualReps
                WHEN i.customReps IS NOT NULL THEN i.customReps
                ELSE 0
            END
        ), 0)
        FROM daily_sessions s
        JOIN daily_session_items i ON s.sessionId = i.sessionId
        WHERE s.date >= :dayStart AND s.date <= :dayEnd
        AND i.exerciseId = :exerciseId
    """)
    suspend fun getTotalRepsForExerciseInDay(dayStart: Long, dayEnd: Long, exerciseId: Long): Int
    
    /**
     * Ottieni elementi di una sessione
     */
    @Query("SELECT * FROM daily_session_items WHERE sessionId = :sessionId ORDER BY `order` ASC")
    suspend fun getSessionItems(sessionId: Long): List<DailySessionItem>
    
    /**
     * Ottieni elementi di una sessione (Flow)
     */
    @Query("SELECT * FROM daily_session_items WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getSessionItemsFlow(sessionId: Long): Flow<List<DailySessionItem>>
    
    /**
     * Aggiorna stato completamento elemento
     */
    @Query("""
        UPDATE daily_session_items 
        SET isCompleted = :isCompleted, 
            actualReps = :actualReps, 
            actualTime = :actualTime, 
            completedAt = :completedAt,
            notes = :notes
        WHERE itemId = :itemId
    """)
    suspend fun updateItemCompletion(
        itemId: Long, 
        isCompleted: Boolean, 
        actualReps: Int?, 
        actualTime: Int?, 
        completedAt: Long?,
        notes: String = ""
    )
    
    /**
     * ✅ Ottieni singolo item per ID
     */
    @Query("SELECT * FROM daily_session_items WHERE itemId = :itemId")
    suspend fun getSessionItemById(itemId: Long): DailySessionItem?
    
    /**
     * ✅ Aggiorna item completo
     */
    @Update
    suspend fun updateSessionItem(item: DailySessionItem): Int
    
    /**
     * Aggiorna sessione (stato completamento, tempi)
     */
    @Update
    suspend fun updateSession(session: DailySession)
    
    /**
     * Elimina sessione
     */
    @Delete
    suspend fun deleteSession(session: DailySession)
    
    /**
     * Elimina elementi di una sessione
     */
    @Query("DELETE FROM daily_session_items WHERE sessionId = :sessionId")
    suspend fun deleteSessionItems(sessionId: Long)
    
    /**
     * Elimina singolo elemento
     */
    @Query("DELETE FROM daily_session_items WHERE itemId = :itemId")
    suspend fun deleteSessionItem(itemId: Long)
    
    /**
     * Conta elementi completati in una sessione
     */
    @Query("SELECT COUNT(*) FROM daily_session_items WHERE sessionId = :sessionId AND isCompleted = 1")
    suspend fun getCompletedItemsCount(sessionId: Long): Int
    
    /**
     * Conta elementi totali in una sessione
     */
    @Query("SELECT COUNT(*) FROM daily_session_items WHERE sessionId = :sessionId")
    suspend fun getTotalItemsCount(sessionId: Long): Int
    
    /**
     * 🦵 Conta tutti gli squat totali (exerciseId=2) 
     * Include AI squat, template squat e recupero passati
     */
    /**
     * Conta totale per un esercizio specifico (parametrizzato)
     * Somma rep o actualReps quando presenti
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN customReps IS NOT NULL THEN customReps
                WHEN actualReps IS NOT NULL THEN actualReps
                ELSE 0
            END
        ), 0)
        FROM daily_session_items 
        WHERE exerciseId = :exerciseId 
        AND (customReps > 0 OR actualReps > 0)
    """)
    fun getTotalCountForExercise(exerciseId: Long): Flow<Int>

    /**
     * Conta rep degli AI squat che non sono già associati all'exerciseId passato.
     * Questo permette di includere Quick Squat / AI items creati senza exerciseId
     * nel conteggio totale degli squat senza duplicare quelli già contati per
     * l'exerciseId (es. template "Squat").
     * ✅ NOTA: Conta AI Squat che HANNO aiData LIKE '%squat%' ESCLUDENDO quelli associati a exerciseId
     *          (per evitare double-counting con getTotalCountForExercise)
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN (aiData IS NOT NULL AND aiData LIKE '%squat%') AND (exerciseId IS NULL OR exerciseId != :exerciseId) THEN
                    CASE
                        WHEN customReps IS NOT NULL THEN customReps
                        WHEN actualReps IS NOT NULL THEN actualReps
                        ELSE 0
                    END
                ELSE 0
            END
        ), 0)
        FROM daily_session_items
    """)
    fun getTotalCountForAiSquatsExcludingExercise(exerciseId: Long): Flow<Int>

    /**
     * Conta rep di recovery items (notes LIKE 'Recovery - %') che non sono già
     * associati all'exerciseId passato.
     */
    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN (notes IS NOT NULL AND notes LIKE 'Recovery - %') AND (exerciseId IS NULL OR exerciseId != :exerciseId) THEN
                    CASE
                        WHEN customReps IS NOT NULL THEN customReps
                        WHEN actualReps IS NOT NULL THEN actualReps
                        ELSE 0
                    END
                ELSE 0
            END
        ), 0)
        FROM daily_session_items
    """)
    fun getTotalCountForRecoveryExcludingExercise(exerciseId: Long): Flow<Int>

    /**
     * 🔄 Metodo di supporto per forzare invalidazione cache del conteggio per un esercizio
     */
    @Query("SELECT COUNT(*) FROM daily_session_items WHERE exerciseId = :exerciseId")
    suspend fun invalidateCountCacheForExercise(exerciseId: Long): Int
    
    /**
     * 🏋️ Ottiene tutti gli esercizi figli di un workout
     */
    @Query("SELECT * FROM daily_session_items WHERE parentWorkoutItemId = :workoutItemId")
    suspend fun getItemsByParentWorkout(workoutItemId: Long): List<DailySessionItem>

    /**
     * Elimina tutti gli elementi figli di un workout (usato quando si rimuove il wrapper workout)
     */
    @Query("DELETE FROM daily_session_items WHERE parentWorkoutItemId = :parentWorkoutItemId")
    suspend fun deleteItemsByParentWorkout(parentWorkoutItemId: Long)

    /**
     * 🔄 Controlla se una data è stata recuperata (ha Recovery items)
     * Una data è considerata "recuperata" se ha almeno un item con notes LIKE 'Recovery - %'
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM daily_session_items
            WHERE sessionId IN (
                SELECT sessionId FROM daily_sessions 
                WHERE date >= :startOfDay AND date < :endOfDay
            )
            AND notes IS NOT NULL AND notes LIKE 'Recovery - %'
        )
    """)
    suspend fun isDateAlreadyRecovered(startOfDay: Long, endOfDay: Long): Boolean
}