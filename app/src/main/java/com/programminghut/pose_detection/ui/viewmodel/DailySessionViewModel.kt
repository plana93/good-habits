package com.programminghut.pose_detection.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.dao.DailySessionSummary
import com.programminghut.pose_detection.data.dao.ExerciseStats  
import com.programminghut.pose_detection.data.dao.WorkoutStats
import com.programminghut.pose_detection.data.model.*
import com.programminghut.pose_detection.data.repository.DailySessionRepository
import com.programminghut.pose_detection.data.repository.ExerciseRepository
import com.programminghut.pose_detection.data.repository.WorkoutRepository
import com.programminghut.pose_detection.utils.MiniatureManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel per gestire le sessioni giornaliere
 * Coordina esercizi, allenamenti e sessioni modulari
 */
class DailySessionViewModel(
    private val dailySessionRepository: DailySessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val miniatureManager: MiniatureManager
) : ViewModel() {

    // ============================================================================
    // STATE MANAGEMENT
    // ============================================================================
    
    private val _uiState = MutableStateFlow(DailySessionUiState())
    val uiState: StateFlow<DailySessionUiState> = _uiState.asStateFlow()
    
    // Sessione odierna con elementi
    val todaySessionWithItems: StateFlow<DailySessionWithItems?> = 
        dailySessionRepository.getTodaySessionWithItems()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    
    // Lista esercizi disponibili
    val availableExercises: StateFlow<List<Exercise>> = 
        exerciseRepository.getAllExercisesFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    // Lista allenamenti disponibili  
    val availableWorkouts: StateFlow<List<WorkoutWithExercises>> = 
        workoutRepository.getAllWorkoutsWithExercises()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    // Cronologia sessioni
    val sessionsHistory: StateFlow<List<DailySessionSummary>> = 
        dailySessionRepository.getSessionsHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    // ============================================================================
    // SESSIONE CORRENTE
    // ============================================================================
    
    /**
     * Inizializza la sessione odierna
     */
    fun initializeTodaySession() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val session = dailySessionRepository.getTodaySession()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentSessionId = session.sessionId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore nel caricamento della sessione: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Avvia la sessione di allenamento
     */
    fun startWorkoutSession() {
        viewModelScope.launch {
            try {
                val sessionId = _uiState.value.currentSessionId
                if (sessionId != null) {
                    dailySessionRepository.startSession(sessionId)
                    _uiState.value = _uiState.value.copy(isSessionActive = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore nell'avvio della sessione: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Completa la sessione di allenamento
     */
    fun completeWorkoutSession() {
        viewModelScope.launch {
            try {
                val sessionId = _uiState.value.currentSessionId
                if (sessionId != null) {
                    dailySessionRepository.completeSession(sessionId)
                    _uiState.value = _uiState.value.copy(
                        isSessionActive = false,
                        showSessionCompleted = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore nel completamento della sessione: ${e.message}"
                )
            }
        }
    }
    
    // ============================================================================
    // GESTIONE ELEMENTI SESSIONE
    // ============================================================================
    
    /**
     * Aggiungi esercizio alla sessione odierna
     */
    fun addExerciseToSession(exerciseId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val item = dailySessionRepository.addExerciseToTodaySession(exerciseId)
                if (item != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Esercizio aggiunto alla sessione"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore nell'aggiunta dell'esercizio"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Aggiungi allenamento alla sessione odierna
     */
    fun addWorkoutToSession(workoutId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val items = dailySessionRepository.addWorkoutToTodaySession(workoutId)
                if (items.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Allenamento aggiunto alla sessione (${items.size} elementi)"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Errore nell'aggiunta dell'allenamento"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Errore: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Rimuovi elemento dalla sessione
     */
    fun removeItemFromSession(itemId: Long) {
        viewModelScope.launch {
            try {
                dailySessionRepository.removeItemFromSession(itemId)
                _uiState.value = _uiState.value.copy(
                    message = "Elemento rimosso dalla sessione"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore nella rimozione: ${e.message}"
                )
            }
        }
    }
    
    // ============================================================================
    // COMPLETAMENTO ESERCIZI
    // ============================================================================
    
    /**
     * Completa elemento della sessione
     */
    fun completeSessionItem(
        itemId: Long,
        actualReps: Int? = null,
        actualTime: Int? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                dailySessionRepository.updateItemCompletion(
                    itemId = itemId,
                    isCompleted = true,
                    actualReps = actualReps,
                    actualTime = actualTime,
                    notes = notes
                )
                _uiState.value = _uiState.value.copy(
                    message = "Esercizio completato!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore nel completamento: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Segna elemento come non completato
     */
    fun uncompleteSessionItem(itemId: Long) {
        viewModelScope.launch {
            try {
                dailySessionRepository.updateItemCompletion(
                    itemId = itemId,
                    isCompleted = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore: ${e.message}"
                )
            }
        }
    }
    
    // ============================================================================
    // GESTIONE MINIATURE
    // ============================================================================
    
    /**
     * Ottieni miniatura per esercizio
     */
    fun getExerciseMiniature(exercise: Exercise): String? {
        return miniatureManager.getExerciseMiniature(
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.name,
            exerciseType = exercise.type.name.lowercase()
        )
    }
    
    /**
     * Ottieni miniatura per allenamento
     */
    fun getWorkoutMiniature(workout: WorkoutWithExercises): String? {
        return miniatureManager.getWorkoutMiniature(
            workoutId = workout.workout.workoutId,
            workoutName = workout.workout.name,
            exerciseCount = workout.exercises.size
        )
    }
    
    // ============================================================================
    // STATISTICHE
    // ============================================================================
    
    /**
     * Carica statistiche esercizi
     */
    fun loadExerciseStats(days: Int = 30) {
        viewModelScope.launch {
            try {
                val stats = dailySessionRepository.getExerciseStats(days)
                _uiState.value = _uiState.value.copy(exerciseStats = stats)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore nel caricamento statistiche: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Carica statistiche allenamenti
     */
    fun loadWorkoutStats(days: Int = 30) {
        viewModelScope.launch {
            try {
                val stats = dailySessionRepository.getWorkoutStats(days)
                _uiState.value = _uiState.value.copy(workoutStats = stats)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Errore nel caricamento statistiche: ${e.message}"
                )
            }
        }
    }
    
    // ============================================================================
    // UTILITY E STATE MANAGEMENT
    // ============================================================================
    
    /**
     * Pulisci messaggio di errore
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Pulisci messaggio
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    /**
     * Nascondi dialog completamento sessione
     */
    fun dismissSessionCompletedDialog() {
        _uiState.value = _uiState.value.copy(showSessionCompleted = false)
    }
    
    /**
     * Ottieni progresso sessione corrente
     */
    fun getSessionProgress(): Flow<SessionProgress> = todaySessionWithItems.map { sessionWithItems ->
        if (sessionWithItems == null) {
            SessionProgress(0, 0, 0f)
        } else {
            val total = sessionWithItems.items.size
            val completed = sessionWithItems.items.count { it.isCompleted }
            val progress = if (total > 0) completed.toFloat() / total else 0f
            SessionProgress(completed, total, progress)
        }
    }
}

/**
 * UI State per le sessioni giornaliere
 */
data class DailySessionUiState(
    val isLoading: Boolean = false,
    val isSessionActive: Boolean = false,
    val currentSessionId: Long? = null,
    val showSessionCompleted: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val exerciseStats: List<ExerciseStats> = emptyList(),
    val workoutStats: List<WorkoutStats> = emptyList()
)

/**
 * Progresso della sessione corrente
 */
data class SessionProgress(
    val completed: Int,
    val total: Int,
    val percentage: Float
)