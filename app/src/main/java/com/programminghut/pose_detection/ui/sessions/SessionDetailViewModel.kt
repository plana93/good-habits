package com.programminghut.pose_detection.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.data.repository.SessionStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Session Detail Screen
 * 
 * Manages the state and business logic for displaying a single workout session's details.
 */
class SessionDetailViewModel(
    private val sessionId: Long,
    private val repository: SessionRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadSessionDetails()
    }
    
    /**
     * Load session details and reps
     */
    private fun loadSessionDetails() {
        viewModelScope.launch {
            try {
                // Load session
                repository.getSessionByIdFlow(sessionId).collect { session ->
                    if (session == null) {
                        _uiState.value = SessionDetailUiState.Error("Sessione non trovata")
                        return@collect
                    }
                    
                    // Load reps for this session
                    repository.getRepsForSession(sessionId).collect { reps ->
                        // Load detailed statistics
                        val stats = repository.getSessionStatistics(sessionId)
                        
                        _uiState.value = SessionDetailUiState.Success(
                            session = session,
                            reps = reps,
                            statistics = stats
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SessionDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Update session notes
     */
    fun updateNotes(notes: String) {
        viewModelScope.launch {
            try {
                repository.updateSessionNotes(sessionId, notes)
            } catch (e: Exception) {
                android.util.Log.e("SessionDetailViewModel", "Error updating notes: ${e.message}")
            }
        }
    }
    
    /**
     * Update session tags
     */
    fun updateTags(tags: List<String>) {
        viewModelScope.launch {
            try {
                repository.updateSessionTags(sessionId, tags)
            } catch (e: Exception) {
                android.util.Log.e("SessionDetailViewModel", "Error updating tags: ${e.message}")
            }
        }
    }
    
    /**
     * Flag a rep for review
     */
    fun flagRep(repId: Long, isFlagged: Boolean) {
        viewModelScope.launch {
            try {
                repository.flagRepForReview(repId, isFlagged)
            } catch (e: Exception) {
                android.util.Log.e("SessionDetailViewModel", "Error flagging rep: ${e.message}")
            }
        }
    }
}

/**
 * UI State for Session Detail Screen
 */
sealed class SessionDetailUiState {
    object Loading : SessionDetailUiState()
    data class Success(
        val session: WorkoutSession,
        val reps: List<RepData>,
        val statistics: SessionStatistics?
    ) : SessionDetailUiState()
    data class Error(val message: String) : SessionDetailUiState()
}

/**
 * Factory for creating SessionDetailViewModel
 */
class SessionDetailViewModelFactory(
    private val sessionId: Long,
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionDetailViewModel::class.java)) {
            return SessionDetailViewModel(sessionId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
