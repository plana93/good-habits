package com.programminghut.pose_detection.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.utils.ShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ExportViewModel - Manages data export functionality
 * 
 * Phase 3: Export & Share implementation
 * Handles CSV/JSON export and multiple template formats
 */
class ExportViewModel(
    private val repository: SessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Loading)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    init {
        loadSessions()
    }
    
    private fun loadSessions() {
        viewModelScope.launch {
            try {
                repository.getAllSessions().collect { sessions ->
                    _uiState.value = ExportUiState.Success(sessions)
                }
            } catch (e: Exception) {
                _uiState.value = ExportUiState.Error(e.message ?: "Errore caricamento dati")
            }
        }
    }
    
    /**
     * Generate CSV export content
     */
    fun generateCSV(): String {
        val state = _uiState.value
        if (state is ExportUiState.Success) {
            return ShareHelper.generateCSVExport(state.sessions)
        }
        return ""
    }
    
    /**
     * Generate JSON export content
     */
    fun generateJSON(): String {
        val state = _uiState.value
        if (state is ExportUiState.Success) {
            return ShareHelper.generateJSONExport(state.sessions)
        }
        return ""
    }
}

/**
 * UI State for Export screen
 */
sealed class ExportUiState {
    object Loading : ExportUiState()
    data class Success(val sessions: List<WorkoutSession>) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

/**
 * ViewModelFactory for ExportViewModel
 */
class ExportViewModelFactory(
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
            return ExportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
