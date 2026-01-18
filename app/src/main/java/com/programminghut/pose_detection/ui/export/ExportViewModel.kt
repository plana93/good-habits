package com.programminghut.pose_detection.ui.export

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.model.DailySessionItemWithDetails
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.data.repository.DailySessionRepository
import com.programminghut.pose_detection.utils.ShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ExportViewModel - Manages data export functionality
 * 
 * Phase 3: Export & Share implementation
 * Handles CSV/JSON/TXT export with daily session data
 */
class ExportViewModel(
    private val sessionRepository: SessionRepository,
    private val dailySessionRepository: DailySessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Loading)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    private val _dailyData = MutableStateFlow<Map<String, List<DailySessionItemWithDetails>>>(emptyMap())
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load both old sessions and new daily sessions
                val sessionsJob = launch {
                    sessionRepository.getAllSessions().collect { sessions ->
                        if (_uiState.value is ExportUiState.Loading) {
                            _uiState.value = ExportUiState.Success(sessions, emptyMap())
                        } else if (_uiState.value is ExportUiState.Success) {
                            _uiState.value = (_uiState.value as ExportUiState.Success).copy(sessions = sessions)
                        }
                    }
                }
                
                val dailyJob = launch {
                    loadDailySessionData()
                }
                
                sessionsJob.join()
                dailyJob.join()
                
            } catch (e: Exception) {
                Log.e("ExportViewModel", "Error loading data", e)
                _uiState.value = ExportUiState.Error(e.message ?: "Errore caricamento dati")
            }
        }
    }
    
    private suspend fun loadDailySessionData() {
        try {
            // Get all daily sessions and group by date
            val dailyDataMap = mutableMapOf<String, MutableList<DailySessionItemWithDetails>>()
            
            val summaries = dailySessionRepository.getSessionsHistory().first()
            Log.d("ExportViewModel", "loadDailySessionData - Found ${summaries.size} session summaries")
            
            // Process each session
            for (summary in summaries) {
                try {
                    // Get items for this session (take first emission)
                    val items = dailySessionRepository.getSessionItemsWithDetails(summary.sessionId).first()
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ITALIAN).format(Date(summary.date))
                    
                    Log.d("ExportViewModel", "Session ${summary.sessionId} on $dateKey: ${items.size} items")
                    
                    if (!dailyDataMap.containsKey(dateKey)) {
                        dailyDataMap[dateKey] = mutableListOf()
                    }
                    dailyDataMap[dateKey]?.addAll(items)
                } catch (e: Exception) {
                    Log.e("ExportViewModel", "Error loading items for session ${summary.sessionId}", e)
                }
            }
            
            Log.d("ExportViewModel", "loadDailySessionData - Total dates: ${dailyDataMap.size}, Total items: ${dailyDataMap.values.sumOf { it.size }}")
            
            _dailyData.value = dailyDataMap.toMap()
            
            if (_uiState.value is ExportUiState.Success) {
                _uiState.value = (_uiState.value as ExportUiState.Success).copy(dailyData = dailyDataMap)
            } else {
                _uiState.value = ExportUiState.Success(emptyList(), dailyDataMap)
            }
        } catch (e: Exception) {
            Log.e("ExportViewModel", "Error loading daily sessions", e)
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
    
    /**
     * Generate TXT export content (comprehensive daily diary)
     * This function blocks to ensure all data is loaded before generating export
     */
    suspend fun generateTXTSuspend(): String {
        return try {
            // Force reload of daily data to ensure we have everything
            val dailyDataMap = mutableMapOf<String, MutableList<DailySessionItemWithDetails>>()
            
            val summaries = dailySessionRepository.getSessionsHistory().first()
            Log.d("ExportViewModel", "generateTXTSuspend - Found ${summaries.size} session summaries")
            
            // Process each session
            for (summary in summaries) {
                try {
                    // Use direct suspend function instead of Flow to avoid race conditions
                    val items = dailySessionRepository.getSessionItemsWithDetailsDirect(summary.sessionId)
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.ITALIAN).format(Date(summary.date))
                    
                    Log.d("ExportViewModel", "generateTXTSuspend - Session ${summary.sessionId} on $dateKey: ${items.size} items")
                    items.forEach { item ->
                        Log.d("ExportViewModel", "  Item: type=${item.type}, name=${item.name}, completed: ${item.isCompleted}")
                    }
                    
                    if (!dailyDataMap.containsKey(dateKey)) {
                        dailyDataMap[dateKey] = mutableListOf()
                    }
                    dailyDataMap[dateKey]?.addAll(items)
                } catch (e: Exception) {
                    Log.e("ExportViewModel", "Error loading items for session ${summary.sessionId}", e)
                }
            }
            
            Log.d("ExportViewModel", "generateTXTSuspend - Total dates: ${dailyDataMap.size}, Total items: ${dailyDataMap.values.sumOf { it.size }}")
            
            if (dailyDataMap.isNotEmpty()) {
                ShareHelper.generateDailyDiaryTXT(dailyDataMap)
            } else {
                "Nessun dato disponibile per l'export."
            }
        } catch (e: Exception) {
            Log.e("ExportViewModel", "Error generating TXT export", e)
            "Errore durante la generazione dell'export: ${e.message}"
        }
    }
    
    /**
     * Generate TXT export content (comprehensive daily diary)
     */
    fun generateTXT(): String {
        val state = _uiState.value
        Log.d("ExportViewModel", "generateTXT - state: $state")
        if (state is ExportUiState.Success) {
            Log.d("ExportViewModel", "generateTXT - dailyData size: ${state.dailyData.size}")
            Log.d("ExportViewModel", "generateTXT - dailyData keys: ${state.dailyData.keys}")
            state.dailyData.forEach { (date, items) ->
                Log.d("ExportViewModel", "generateTXT - $date: ${items.size} items")
            }
            return if (state.dailyData.isNotEmpty()) {
                ShareHelper.generateDailyDiaryTXT(state.dailyData)
            } else {
                // Fallback to old format if no daily data
                @Suppress("DEPRECATION")
                ShareHelper.generateTXTExport(state.sessions)
            }
        }
        return ""
    }
}

/**
 * UI State for Export screen
 */
sealed class ExportUiState {
    object Loading : ExportUiState()
    data class Success(
        val sessions: List<WorkoutSession>,
        val dailyData: Map<String, List<DailySessionItemWithDetails>> = emptyMap()
    ) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

/**
 * ViewModelFactory for ExportViewModel
 */
class ExportViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val dailySessionRepository: DailySessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
            return ExportViewModel(sessionRepository, dailySessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
