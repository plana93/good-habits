package com.programminghut.pose_detection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.programminghut.pose_detection.data.model.Exercise
import com.programminghut.pose_detection.data.model.WorkoutExercise
import com.programminghut.pose_detection.data.model.WorkoutWithExercises
import com.programminghut.pose_detection.data.repository.WorkoutRepository
import com.programminghut.pose_detection.data.repository.ExerciseRepository

/**
 * Data class per tracciare il risultato di ogni esercizio nel workout
 */
data class ExerciseTrackingResult(
    val exercise: Exercise,
    val workoutExercise: WorkoutExercise,
    val actualValue: Int? = null,
    val isCompleted: Boolean = false,
    val timestamp: Long? = null
)

/**
 * UI State per la schermata di tracking workout
 */
data class WorkoutTrackingUiState(
    val workoutName: String = "",
    val workoutDescription: String = "",
    val exerciseResults: List<ExerciseTrackingResult> = emptyList(),
    val completedExercises: Int = 0,
    val totalExercises: Int = 0,
    val canCompleteWorkout: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel per gestire il tracking di un workout
 */
class WorkoutTrackingViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkoutTrackingUiState())
    val uiState: StateFlow<WorkoutTrackingUiState> = _uiState.asStateFlow()
    
    private var currentWorkoutId: Long = 0
    private var sessionStartTime: Long = 0
    
    fun loadWorkout(workoutId: Long) {
        currentWorkoutId = workoutId
        sessionStartTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get workout details
                val workouts = workoutRepository.getAllWorkouts().first()
                val workout = workouts.find { it.workoutId == workoutId }
                
                if (workout != null) {
                    // Get workout exercises
                    val workoutExercises = workoutRepository.getWorkoutExercises(workoutId).first()
                    val exerciseResults = workoutExercises.mapNotNull { workoutExercise ->
                        val exercise = exerciseRepository.getExerciseById(workoutExercise.exerciseId)
                        if (exercise != null) {
                            ExerciseTrackingResult(
                                exercise = exercise,
                                workoutExercise = workoutExercise
                            )
                        } else {
                            null
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        workoutName = workout.name,
                        workoutDescription = workout.description,
                        exerciseResults = exerciseResults,
                        totalExercises = exerciseResults.size,
                        completedExercises = 0,
                        canCompleteWorkout = exerciseResults.isNotEmpty(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Workout non trovato",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Errore nel caricamento workout: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun updateExerciseResult(exerciseId: Long, actualValue: Int) {
        val currentState = _uiState.value
        val updatedResults = currentState.exerciseResults.map { result ->
            if (result.exercise.exerciseId == exerciseId) {
                result.copy(
                    actualValue = actualValue,
                    isCompleted = actualValue > 0,
                    timestamp = if (actualValue > 0) System.currentTimeMillis() else null
                )
            } else {
                result
            }
        }
        
        val completedCount = updatedResults.count { it.isCompleted }
        
        _uiState.value = currentState.copy(
            exerciseResults = updatedResults,
            completedExercises = completedCount,
            canCompleteWorkout = completedCount > 0 // Allow completion with at least one exercise done
        )
    }
    
    /**
     * Completa il workout e salva la sessione nel database
     * @return ID della sessione salvata
     */
    suspend fun completeWorkout(): Long {
        val currentState = _uiState.value
        
        if (currentState.completedExercises == 0) {
            _uiState.value = currentState.copy(errorMessage = "Completa almeno un esercizio")
            return 0
        }
        
        try {
            _uiState.value = currentState.copy(isLoading = true)
            
            // TODO: Implementare salvataggio sessione quando avremo WorkoutSession entity
            // Per ora, ritorniamo un ID fittizio
            val sessionId = System.currentTimeMillis()
            
            // In futuro:
            // 1. Creare WorkoutSession entity
            // 2. Creare WorkoutSessionExercise entities per ogni esercizio completato
            // 3. Salvare tutto nel database tramite SessionRepository
            
            _uiState.value = currentState.copy(isLoading = false)
            return sessionId
            
        } catch (e: Exception) {
            _uiState.value = currentState.copy(
                errorMessage = "Errore nel salvataggio sessione: ${e.message}",
                isLoading = false
            )
            return 0
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Ottiene il progresso attuale del workout (0.0 - 1.0)
     */
    fun getProgress(): Float {
        val currentState = _uiState.value
        return if (currentState.totalExercises > 0) {
            currentState.completedExercises.toFloat() / currentState.totalExercises.toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Verifica se il workout è completamente finito
     */
    fun isWorkoutComplete(): Boolean {
        val currentState = _uiState.value
        return currentState.completedExercises == currentState.totalExercises && currentState.totalExercises > 0
    }
}

/**
 * Factory per creare il WorkoutTrackingViewModel con le dipendenze
 */
class WorkoutTrackingViewModelFactory(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutTrackingViewModel::class.java)) {
            return WorkoutTrackingViewModel(workoutRepository, exerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}