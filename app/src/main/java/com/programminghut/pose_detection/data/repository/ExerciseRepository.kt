package com.programminghut.pose_detection.data.repository

import com.programminghut.pose_detection.data.dao.ExerciseDao
import com.programminghut.pose_detection.data.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository per gestire le operazioni sugli esercizi nel database
 */
class ExerciseRepository(private val exerciseDao: ExerciseDao) {
    
    /**
     * Ottiene tutti gli esercizi dal database
     * @return Lista di tutti gli esercizi
     */
    suspend fun getAllExercises(): List<Exercise> {
        // Usando Flow, prendiamo il primo valore emesso
        return exerciseDao.getAllExercises().first()
    }
    
    /**
     * Ottiene tutti gli esercizi dal database come Flow
     * @return Flow con lista di tutti gli esercizi
     */
    fun getAllExercisesFlow(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises()
    }
    
    /**
     * Ottiene un esercizio specifico per ID
     * @param exerciseId ID dell'esercizio
     * @return L'esercizio trovato o null
     */
    suspend fun getExerciseById(exerciseId: Long): Exercise? {
        return exerciseDao.getExerciseById(exerciseId)
    }
    
    /**
     * Inserisce un nuovo esercizio nel database
     * @param exercise L'esercizio da inserire
     * @return L'ID dell'esercizio inserito
     */
    suspend fun insertExercise(exercise: Exercise): Long {
        return exerciseDao.insertExercise(exercise)
    }
    
    /**
     * Aggiorna un esercizio esistente
     * @param exercise L'esercizio da aggiornare
     */
    suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.updateExercise(exercise)
    }
    
    /**
     * Elimina un esercizio dal database
     * @param exerciseId ID dell'esercizio da eliminare
     */
    suspend fun deleteExercise(exerciseId: Long) {
        exerciseDao.deleteExerciseById(exerciseId)
    }
    
    /**
     * Cerca esercizi per nome
     * @param query Termine di ricerca
     * @return Lista di esercizi che contengono il termine nel nome
     */
    suspend fun searchExercisesByName(query: String): List<Exercise> {
        return exerciseDao.searchExercisesByName(query).first()
    }
}