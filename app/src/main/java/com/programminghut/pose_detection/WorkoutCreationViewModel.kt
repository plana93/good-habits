package com.programminghut.pose_detection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.programminghut.pose_detection.data.model.Exercise
import com.programminghut.pose_detection.data.model.ExerciseMode
import com.programminghut.pose_detection.data.model.Workout
import com.programminghut.pose_detection.data.model.WorkoutExercise
import com.programminghut.pose_detection.data.repository.ExerciseRepository
import com.programminghut.pose_detection.data.repository.WorkoutRepository

/**
 * Data class che combina WorkoutExercise con i dettagli dell'Exercise
 */
data class WorkoutExerciseWithDetails(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise
)

/**
 * UI State per la schermata di creazione workout
 */
data class WorkoutCreationUiState(
    val workoutName: String = "",
    val workoutDescription: String = "",
    val workoutExercises: List<WorkoutExerciseWithDetails> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val showExerciseSelection: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel per gestire la logica di creazione workout
 */
class WorkoutCreationViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkoutCreationUiState())
    val uiState: StateFlow<WorkoutCreationUiState> = _uiState.asStateFlow()
    
    fun updateWorkoutName(name: String) {
        _uiState.value = _uiState.value.copy(workoutName = name)
    }
    
    fun updateWorkoutDescription(description: String) {
        _uiState.value = _uiState.value.copy(workoutDescription = description)
    }
    
    fun loadExercises() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val exercises = exerciseRepository.getAllExercises()
                _uiState.value = _uiState.value.copy(
                    availableExercises = exercises,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Errore nel caricamento esercizi: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun addExerciseToWorkout(exercise: Exercise) {
        val currentExercises = _uiState.value.workoutExercises
        
        // Check if exercise is already in workout
        if (currentExercises.any { it.exercise.exerciseId == exercise.exerciseId }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Esercizio già presente nel workout"
            )
            return
        }
        
        val workoutExercise = WorkoutExercise(
            workoutId = 0, // Will be set when saving
            exerciseId = exercise.exerciseId,
            orderIndex = currentExercises.size,
            targetReps = null, // User will set this based on exercise mode
            targetTime = null  // User will set this based on exercise mode
        )
        
        val workoutExerciseWithDetails = WorkoutExerciseWithDetails(
            workoutExercise = workoutExercise,
            exercise = exercise
        )
        
        _uiState.value = _uiState.value.copy(
            workoutExercises = currentExercises + workoutExerciseWithDetails,
            showExerciseSelection = false
        )
    }
    
    fun removeWorkoutExercise(exerciseId: Long) {
        val currentExercises = _uiState.value.workoutExercises
        val updatedExercises = currentExercises
            .filter { it.exercise.exerciseId != exerciseId }
            .mapIndexed { index, workoutExercise ->
                workoutExercise.copy(
                    workoutExercise = workoutExercise.workoutExercise.copy(orderIndex = index)
                )
            }
        
        _uiState.value = _uiState.value.copy(workoutExercises = updatedExercises)
    }
    
    fun updateWorkoutExercise(updatedWorkoutExercise: WorkoutExerciseWithDetails) {
        val currentExercises = _uiState.value.workoutExercises
        val updatedExercises = currentExercises.map { existing ->
            if (existing.exercise.exerciseId == updatedWorkoutExercise.exercise.exerciseId) {
                updatedWorkoutExercise
            } else {
                existing
            }
        }
        
        _uiState.value = _uiState.value.copy(workoutExercises = updatedExercises)
    }
    
    fun showExerciseSelection() {
        _uiState.value = _uiState.value.copy(showExerciseSelection = true)
    }
    
    fun hideExerciseSelection() {
        _uiState.value = _uiState.value.copy(showExerciseSelection = false)
    }
    
    /**
     * Salva il workout nel database e ritorna l'ID del workout creato
     */
    suspend fun saveWorkout(): Long {
        val currentState = _uiState.value
        
        if (currentState.workoutName.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "Il nome del workout è obbligatorio")
            return 0
        }
        
        if (currentState.workoutExercises.isEmpty()) {
            _uiState.value = currentState.copy(errorMessage = "Aggiungi almeno un esercizio al workout")
            return 0
        }
        
        // Check if all exercises have target values
        val exercisesWithoutTargets = currentState.workoutExercises.filter { workoutExercise ->
            val hasTarget = when (workoutExercise.exercise.mode) {
                ExerciseMode.REPS -> workoutExercise.workoutExercise.targetReps != null && workoutExercise.workoutExercise.targetReps!! > 0
                ExerciseMode.TIME -> workoutExercise.workoutExercise.targetTime != null && workoutExercise.workoutExercise.targetTime!! > 0
            }
            !hasTarget
        }
        
        if (exercisesWithoutTargets.isNotEmpty()) {
            _uiState.value = currentState.copy(
                errorMessage = "Imposta un target per tutti gli esercizi"
            )
            return 0
        }
        
        try {
            _uiState.value = currentState.copy(isLoading = true)
            
            // Create workout
            val workout = Workout(
                name = currentState.workoutName.trim(),
                description = currentState.workoutDescription.trim().ifBlank { "" }
            )
            
            // Save workout and get ID
            val workoutId = workoutRepository.insertWorkout(workout)
            
            // Create workout exercises with correct workout ID
            val workoutExercises = currentState.workoutExercises.map { workoutExerciseWithDetails ->
                workoutExerciseWithDetails.workoutExercise.copy(workoutId = workoutId)
            }
            
            // Save workout exercises
            workoutRepository.insertWorkoutExercises(workoutExercises)
            
            _uiState.value = currentState.copy(isLoading = false)
            return workoutId
            
        } catch (e: Exception) {
            _uiState.value = currentState.copy(
                errorMessage = "Errore nel salvataggio: ${e.message}",
                isLoading = false
            )
            return 0
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * Factory per creare il WorkoutCreationViewModel con le dipendenze
 */
class WorkoutCreationViewModelFactory(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutCreationViewModel::class.java)) {
            return WorkoutCreationViewModel(exerciseRepository, workoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}