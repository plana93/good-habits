package com.programminghut.pose_detection.data.dao

import androidx.room.*
import com.programminghut.pose_detection.data.model.Workout
import com.programminghut.pose_detection.data.model.WorkoutExercise
import com.programminghut.pose_detection.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * WorkoutDao - Data Access Object per Workout e WorkoutExercise
 * 
 * Gestisce le operazioni CRUD per allenamenti composti
 */
@Dao
interface WorkoutDao {
    
    /**
     * Inserisci un nuovo workout
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long
    
    /**
     * Inserisci esercizi in un workout
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExercise>)
    
    /**
     * Ottieni tutti i workout
     */
    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    fun getAllWorkouts(): Flow<List<Workout>>
    
    /**
     * Ottieni workout per ID
     */
    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): Workout?
    
    /**
     * Ottieni esercizi di un workout
     */
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    suspend fun getWorkoutExercises(workoutId: Long): List<WorkoutExercise>
    
    /**
     * Aggiorna workout
     */
    @Update
    suspend fun updateWorkout(workout: Workout)
    
    /**
     * Elimina workout
     */
    @Delete
    suspend fun deleteWorkout(workout: Workout)
    
    /**
     * Elimina esercizi di un workout
     */
    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutExercises(workoutId: Long)
    
    /**
     * Query complessa: Ottieni workout con i suoi esercizi (JOIN)
     * Questo richiederà una Transaction per assemblare WorkoutWithExercises
     */
    @Transaction
    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId")
    suspend fun getWorkoutWithExercises(workoutId: Long): WorkoutWithExercises?
    
    /**
     * Ottieni workout con esercizi come Flow
     */
    @Transaction
    @Query("SELECT * FROM workouts WHERE workoutId = :workoutId")
    fun getWorkoutWithExercisesFlow(workoutId: Long): Flow<WorkoutWithExercises?>
    
    /**
     * Ottieni tutti i workout con esercizi come Flow
     */
    @Transaction
    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    fun getAllWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>>
    
    /**
     * Ottieni esercizi di un workout come Flow
     */
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    fun getWorkoutExercisesFlow(workoutId: Long): Flow<List<WorkoutExercise>>
    
    /**
     * Elimina workout per ID
     */
    @Query("DELETE FROM workouts WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutById(workoutId: Long)
}
