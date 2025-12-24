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
     * 🦵 Conta tutti gli squat totali (exerciseId=3) 
     * Include AI squat, template squat e recupero passati
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
        WHERE exerciseId = 3 
        AND (customReps > 0 OR actualReps > 0)
    """)
    fun getTotalSquatsCount(): Flow<Int>
    
    /**
     * 🔄 Metodo di supporto per forzare invalidazione cache del conteggio squat
     * Questo è un workaround per assicurarsi che il Flow si aggiorni dopo eliminazioni
     */
    @Query("SELECT COUNT(*) FROM daily_session_items WHERE exerciseId = 3")
    suspend fun invalidateSquatCountCache(): Int
    
    /**
     * 🏋️ Ottiene tutti gli esercizi figli di un workout
     */
    @Query("SELECT * FROM daily_session_items WHERE parentWorkoutItemId = :workoutItemId")
    suspend fun getItemsByParentWorkout(workoutItemId: Long): List<DailySessionItem>
}