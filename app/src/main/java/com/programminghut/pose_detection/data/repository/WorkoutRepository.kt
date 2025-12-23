package com.programminghut.pose_detection.data.repository

import com.programminghut.pose_detection.data.dao.WorkoutDao
import com.programminghut.pose_detection.data.model.Workout
import com.programminghut.pose_detection.data.model.WorkoutExercise
import com.programminghut.pose_detection.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * Repository per gestire le operazioni sui workout nel database
 */
class WorkoutRepository(private val workoutDao: WorkoutDao) {
    
    /**
     * Inserisce un nuovo workout nel database
     * @param workout Il workout da inserire
     * @return L'ID del workout inserito
     */
    suspend fun insertWorkout(workout: Workout): Long {
        return workoutDao.insertWorkout(workout)
    }
    
    /**
     * Inserisce una lista di workout exercises nel database
     * @param workoutExercises Lista di workout exercises da inserire
     */
    suspend fun insertWorkoutExercises(workoutExercises: List<WorkoutExercise>) {
        workoutDao.insertWorkoutExercises(workoutExercises)
    }
    
    /**
     * Ottiene tutti i workout dal database
     * @return Flow con la lista di tutti i workout
     */
    fun getAllWorkouts(): Flow<List<Workout>> {
        return workoutDao.getAllWorkouts()
    }
    
    /**
     * Ottiene un workout specifico con i suoi esercizi
     * @param workoutId ID del workout
     * @return Flow con il workout e i suoi esercizi
     */
    fun getWorkoutWithExercises(workoutId: Long): Flow<WorkoutWithExercises?> {
        return workoutDao.getWorkoutWithExercisesFlow(workoutId)
    }
    
    /**
     * Ottiene tutti i workout con i loro esercizi
     * @return Flow con lista di workout con esercizi
     */
    fun getAllWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>> {
        return workoutDao.getAllWorkoutsWithExercises()
    }
    
    /**
     * Aggiorna un workout esistente
     * @param workout Il workout da aggiornare
     */
    suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout)
    }
    
    /**
     * Elimina un workout dal database
     * @param workoutId ID del workout da eliminare
     */
    suspend fun deleteWorkout(workoutId: Long) {
        workoutDao.deleteWorkoutById(workoutId)
    }
    
    /**
     * Elimina tutti gli esercizi di un workout
     * @param workoutId ID del workout
     */
    suspend fun deleteWorkoutExercises(workoutId: Long) {
        workoutDao.deleteWorkoutExercises(workoutId)
    }
    
    /**
     * Ottiene gli esercizi di un workout specifico
     * @param workoutId ID del workout
     * @return Flow con lista di workout exercises
     */
    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>> {
        return workoutDao.getWorkoutExercisesFlow(workoutId)
    }
}