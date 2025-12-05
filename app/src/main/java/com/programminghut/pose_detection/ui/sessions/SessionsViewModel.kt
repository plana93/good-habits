package com.programminghut.pose_detection.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Sessions List Screen
 * 
 * Manages the state and business logic for displaying workout sessions.
 */
class SessionsViewModel(
    private val repository: SessionRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()
    
    // Filter state
    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()
    
    init {
        loadSessions()
    }
    
    /**
     * Load all sessions from repository
     */
    fun loadSessions() {
        viewModelScope.launch {
            try {
                repository.getAllSessions().collect { sessions ->
                    if (sessions.isEmpty()) {
                        _uiState.value = SessionsUiState.Empty
                    } else {
                        _uiState.value = SessionsUiState.Success(
                            sessions = sessions,
                            stats = calculateStats(sessions)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SessionsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Filter sessions by type
     */
    fun filterSessions(filterType: FilterType) {
        _filterType.value = filterType
        
        viewModelScope.launch {
            try {
                val flow = when (filterType) {
                    FilterType.ALL -> repository.getAllSessions()
                    FilterType.TODAY -> {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        val dayStart = calendar.timeInMillis
                        
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        val dayEnd = calendar.timeInMillis
                        
                        repository.getTodaySessions(dayStart, dayEnd)
                    }
                    FilterType.THIS_WEEK -> {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        val weekStart = calendar.timeInMillis
                        
                        calendar.add(Calendar.DAY_OF_WEEK, 6)
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        val weekEnd = calendar.timeInMillis
                        
                        repository.getWeekSessions(weekStart, weekEnd)
                    }
                    FilterType.SQUATS_ONLY -> repository.getSessionsByExerciseType("SQUAT")
                }
                
                flow.collect { sessions ->
                    if (sessions.isEmpty()) {
                        _uiState.value = SessionsUiState.Empty
                    } else {
                        _uiState.value = SessionsUiState.Success(
                            sessions = sessions,
                            stats = calculateStats(sessions)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SessionsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Delete a session
     */
    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            try {
                repository.deleteSession(session)
            } catch (e: Exception) {
                // Handle error - could add a snackbar message
                android.util.Log.e("SessionsViewModel", "Error deleting session: ${e.message}")
            }
        }
    }
    
    /**
     * Calculate statistics from sessions
     */
    private fun calculateStats(sessions: List<WorkoutSession>): SessionStats {
        if (sessions.isEmpty()) {
            return SessionStats(
                totalSessions = 0,
                totalReps = 0,
                totalDuration = 0,
                avgFormScore = 0f,
                avgDepthScore = 0f
            )
        }
        
        val totalReps = sessions.sumOf { it.totalReps }
        val totalDuration = sessions.sumOf { it.durationSeconds }
        val avgForm = sessions.map { it.avgFormScore }.average().toFloat()
        val avgDepth = sessions.map { it.avgDepthScore }.average().toFloat()
        
        return SessionStats(
            totalSessions = sessions.size,
            totalReps = totalReps,
            totalDuration = totalDuration,
            avgFormScore = avgForm,
            avgDepthScore = avgDepth
        )
    }
}

/**
 * UI State for Sessions Screen
 */
sealed class SessionsUiState {
    object Loading : SessionsUiState()
    object Empty : SessionsUiState()
    data class Success(
        val sessions: List<WorkoutSession>,
        val stats: SessionStats
    ) : SessionsUiState()
    data class Error(val message: String) : SessionsUiState()
}

/**
 * Filter types for sessions list
 */
enum class FilterType {
    ALL,
    TODAY,
    THIS_WEEK,
    SQUATS_ONLY
}

/**
 * Statistics data class
 */
data class SessionStats(
    val totalSessions: Int,
    val totalReps: Int,
    val totalDuration: Int, // in seconds
    val avgFormScore: Float,
    val avgDepthScore: Float
) {
    fun formatDuration(): String {
        val hours = totalDuration / 3600
        val minutes = (totalDuration % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalDuration}s"
        }
    }
}

/**
 * Factory for creating SessionsViewModel with repository dependency
 */
class SessionsViewModelFactory(
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionsViewModel::class.java)) {
            return SessionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
