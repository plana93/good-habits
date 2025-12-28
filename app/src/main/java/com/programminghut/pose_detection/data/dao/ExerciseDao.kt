package com.programminghut.pose_detection.data.dao

import androidx.room.*
import com.programminghut.pose_detection.data.model.Exercise
import com.programminghut.pose_detection.data.model.ExerciseType
import kotlinx.coroutines.flow.Flow

/**
 * Phase 6: DAO per gestire gli esercizi nel database
 * 
 * Permette di salvare, modificare e recuperare esercizi predefiniti e custom
 */
@Dao
interface ExerciseDao {
    
    /**
     * Inserisci un nuovo esercizio
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long
    
    /**
     * Inserisci multipli esercizi
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)
    
    /**
     * Aggiorna un esercizio esistente
     */
    @Update
    suspend fun updateExercise(exercise: Exercise)
    
    /**
     * Elimina un esercizio
     */
    @Delete
    suspend fun deleteExercise(exercise: Exercise)
    
    /**
     * Elimina esercizio per ID
     */
    @Query("DELETE FROM exercises WHERE exerciseId = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Long)
    
    /**
     * Ottieni tutti gli esercizi
     */
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>
    
    /**
     * Ottieni esercizio per ID
     */
    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    suspend fun getExerciseById(exerciseId: Long): Exercise?
    
    /**
     * Ottieni esercizio per ID (Flow)
     */
    @Query("SELECT * FROM exercises WHERE exerciseId = :exerciseId")
    fun getExerciseByIdFlow(exerciseId: Long): Flow<Exercise?>
    
    /**
     * Ottieni esercizi per tipo
     */
    @Query("SELECT * FROM exercises WHERE type = :type ORDER BY name ASC")
    fun getExercisesByType(type: ExerciseType): Flow<List<Exercise>>
    
    /**
     * Ottieni solo esercizi predefiniti
     */
    @Query("SELECT * FROM exercises WHERE isCustom = 0 ORDER BY name ASC")
    fun getPredefinedExercises(): Flow<List<Exercise>>
    
    /**
     * Ottieni solo esercizi custom
     */
    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomExercises(): Flow<List<Exercise>>
    
    /**
     * Cerca esercizi per nome
     */
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercisesByName(query: String): Flow<List<Exercise>>

    /**
     * Ottieni esercizio per nome esatto
     */
    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getExerciseByName(name: String): Exercise?
    
    /**
     * Ottieni esercizi per tag
     */
    @Query("SELECT * FROM exercises WHERE tags LIKE '%' || :tag || '%' ORDER BY name ASC")
    fun getExercisesByTag(tag: String): Flow<List<Exercise>>
    
    /**
     * Conta tutti gli esercizi
     */
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int
    
    /**
     * Conta esercizi custom
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE isCustom = 1")
    suspend fun getCustomExerciseCount(): Int
    
    /**
     * Verifica se esiste un esercizio con un dato nome
     */
    @Query("SELECT EXISTS(SELECT 1 FROM exercises WHERE name = :name LIMIT 1)")
    suspend fun exerciseExistsByName(name: String): Boolean
    
    /**
     * Ottieni gli ultimi esercizi modificati
     */
    @Query("SELECT * FROM exercises ORDER BY modifiedAt DESC LIMIT :limit")
    fun getRecentlyModifiedExercises(limit: Int = 10): Flow<List<Exercise>>
    
    /**
     * Elimina tutti gli esercizi custom
     */
    @Query("DELETE FROM exercises WHERE isCustom = 1")
    suspend fun deleteAllCustomExercises()
}
