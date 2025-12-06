package com.programminghut.pose_detection.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.model.SessionCreationParams
import com.programminghut.pose_detection.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ManualSessionViewModel - Manages manual session creation
 * 
 * Phase 4: Manual Session Creation
 */
class ManualSessionViewModel(
    private val repository: SessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ManualSessionUiState>(ManualSessionUiState.Editing)
    val uiState: StateFlow<ManualSessionUiState> = _uiState.asStateFlow()
    
    // Form fields
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()
    
    private val _exerciseType = MutableStateFlow("SQUAT")
    val exerciseType: StateFlow<String> = _exerciseType.asStateFlow()
    
    private val _repsCount = MutableStateFlow("")
    val repsCount: StateFlow<String> = _repsCount.asStateFlow()
    
    private val _duration = MutableStateFlow("")
    val duration: StateFlow<String> = _duration.asStateFlow()
    
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()
    
    private val _affectsStreak = MutableStateFlow(true)
    val affectsStreak: StateFlow<Boolean> = _affectsStreak.asStateFlow()
    
    /**
     * Update selected date
     */
    fun updateDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }
    
    /**
     * Update exercise type
     */
    fun updateExerciseType(type: String) {
        _exerciseType.value = type
    }
    
    /**
     * Update reps count
     */
    fun updateRepsCount(reps: String) {
        _repsCount.value = reps
    }
    
    /**
     * Update duration
     */
    fun updateDuration(durationStr: String) {
        _duration.value = durationStr
    }
    
    /**
     * Update notes
     */
    fun updateNotes(notesText: String) {
        _notes.value = notesText
    }
    
    /**
     * Toggle affects streak
     */
    fun toggleAffectsStreak() {
        _affectsStreak.value = !_affectsStreak.value
    }
    
    /**
     * Validate and create manual session
     */
    fun createSession() {
        viewModelScope.launch {
            try {
                _uiState.value = ManualSessionUiState.Saving
                
                // Validate inputs
                val reps = _repsCount.value.toIntOrNull()
                if (reps == null || reps <= 0) {
                    _uiState.value = ManualSessionUiState.Error("Inserisci un numero valido di ripetizioni")
                    return@launch
                }
                
                val durationMinutes = _duration.value.toIntOrNull()
                if (durationMinutes == null || durationMinutes <= 0) {
                    _uiState.value = ManualSessionUiState.Error("Inserisci una durata valida in minuti")
                    return@launch
                }
                
                // Check for duplicate session on same day
                val dayStart = getStartOfDay(_selectedDate.value)
                val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
                val existingSessions = repository.getSessionsForDateRange(dayStart, dayEnd)
                
                if (existingSessions.isNotEmpty()) {
                    _uiState.value = ManualSessionUiState.DuplicateWarning(
                        existingCount = existingSessions.size,
                        onConfirm = { 
                            viewModelScope.launch {
                                createSessionConfirmed()
                            }
                        },
                        onCancel = { _uiState.value = ManualSessionUiState.Editing }
                    )
                    return@launch
                }
                
                // Create session
                createSessionConfirmed()
                
            } catch (e: Exception) {
                _uiState.value = ManualSessionUiState.Error(
                    e.message ?: "Errore creazione sessione"
                )
            }
        }
    }
    
    /**
     * Create session after confirmation
     */
    private suspend fun createSessionConfirmed() {
        try {
            val reps = _repsCount.value.toInt()
            val durationMinutes = _duration.value.toInt()
            
            val params = SessionCreationParams(
                timestamp = _selectedDate.value,
                exerciseType = _exerciseType.value,
                totalReps = reps,
                durationSeconds = durationMinutes * 60,
                notes = _notes.value.takeIf { it.isNotBlank() },
                affectsStreak = _affectsStreak.value
            )
            
            val sessionId = repository.createManualSession(params)
            
            _uiState.value = ManualSessionUiState.Success(sessionId)
            
        } catch (e: Exception) {
            _uiState.value = ManualSessionUiState.Error(
                e.message ?: "Errore salvataggio sessione"
            )
        }
    }
    
    /**
     * Get start of day timestamp
     */
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

/**
 * UI State for Manual Session screen
 */
sealed class ManualSessionUiState {
    object Editing : ManualSessionUiState()
    object Saving : ManualSessionUiState()
    data class Success(val sessionId: Long) : ManualSessionUiState()
    data class Error(val message: String) : ManualSessionUiState()
    data class DuplicateWarning(
        val existingCount: Int,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit
    ) : ManualSessionUiState()
}

/**
 * ViewModelFactory for ManualSessionViewModel
 */
class ManualSessionViewModelFactory(
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManualSessionViewModel::class.java)) {
            return ManualSessionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
