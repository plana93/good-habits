package com.programminghut.pose_detection.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.model.RecoveryConfig
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * CalendarViewModel - Manages calendar state and recovery logic
 * 
 * Phase 4: Session Recovery & Calendar
 */
class CalendarViewModel(
    private val repository: SessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    private val _selectedDate = MutableStateFlow<Long?>(null)
    val selectedDate: StateFlow<Long?> = _selectedDate.asStateFlow()
    
    private val recoveryConfig = RecoveryConfig(
        minRepsRequired = 50,
        maxDaysBack = 7,
        isEnabled = true
    )
    
    init {
        loadCalendarData()
    }
    
    /**
     * Load calendar data for current month
     */
    fun loadCalendarData(monthOffset: Int = 0) {
        viewModelScope.launch {
            try {
                _uiState.value = CalendarUiState.Loading
                
                // Calculate date range for the month
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, monthOffset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val monthEnd = calendar.timeInMillis
                
                // Get sessions for the month
                val sessions = repository.getSessionsForDateRange(monthStart, monthEnd)
                
                // Get missed days
                val missedDays = repository.getMissedDays(monthStart, monthEnd)
                
                // Calculate current streak
                val currentStreak = repository.calculateStreakWithRecovery()
                
                // Build day data map
                val dayDataMap = buildDayDataMap(sessions, missedDays)
                
                _uiState.value = CalendarUiState.Success(
                    dayDataMap = dayDataMap,
                    currentStreak = currentStreak,
                    recoveryConfig = recoveryConfig,
                    monthStart = monthStart,
                    monthEnd = monthEnd
                )
                
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(
                    e.message ?: "Errore caricamento calendario"
                )
            }
        }
    }
    
    /**
     * Select a date for detailed view or recovery
     */
    fun selectDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }
    
    /**
     * Check if a day can be recovered
     */
    suspend fun canRecoverDay(dayTimestamp: Long): Boolean {
        return repository.canRecoverDay(dayTimestamp, recoveryConfig)
    }
    
    /**
     * Build a map of day data for calendar display
     */
    private fun buildDayDataMap(
        sessions: List<WorkoutSession>,
        missedDays: List<Long>
    ): Map<Long, DayData> {
        val dayDataMap = mutableMapOf<Long, DayData>()
        
        // Mark completed days
        sessions.forEach { session ->
            val dayStart = getStartOfDay(session.startTime)
            val existing = dayDataMap[dayStart]
            
            val dayData = when {
                session.sessionType == "RECOVERY" -> {
                    DayData(
                        dayTimestamp = dayStart,
                        status = DayStatus.RECOVERED,
                        sessionCount = (existing?.sessionCount ?: 0) + 1,
                        totalReps = (existing?.totalReps ?: 0) + session.totalReps,
                        sessions = (existing?.sessions ?: emptyList()) + session
                    )
                }
                session.sessionType == "MANUAL" -> {
                    DayData(
                        dayTimestamp = dayStart,
                        status = if (existing?.status == DayStatus.RECOVERED) 
                            DayStatus.RECOVERED 
                        else 
                            DayStatus.COMPLETED_MANUAL,
                        sessionCount = (existing?.sessionCount ?: 0) + 1,
                        totalReps = (existing?.totalReps ?: 0) + session.totalReps,
                        sessions = (existing?.sessions ?: emptyList()) + session
                    )
                }
                else -> {
                    DayData(
                        dayTimestamp = dayStart,
                        status = if (existing?.status == DayStatus.RECOVERED) 
                            DayStatus.RECOVERED 
                        else 
                            DayStatus.COMPLETED,
                        sessionCount = (existing?.sessionCount ?: 0) + 1,
                        totalReps = (existing?.totalReps ?: 0) + session.totalReps,
                        sessions = (existing?.sessions ?: emptyList()) + session
                    )
                }
            }
            
            dayDataMap[dayStart] = dayData
        }
        
        // Mark missed days
        missedDays.forEach { missedDay ->
            if (!dayDataMap.containsKey(missedDay)) {
                dayDataMap[missedDay] = DayData(
                    dayTimestamp = missedDay,
                    status = DayStatus.MISSED,
                    sessionCount = 0,
                    totalReps = 0,
                    sessions = emptyList()
                )
            }
        }
        
        return dayDataMap
    }
    
    /**
     * Get start of day timestamp (00:00:00.000)
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
 * UI State for Calendar screen
 */
sealed class CalendarUiState {
    object Loading : CalendarUiState()
    data class Success(
        val dayDataMap: Map<Long, DayData>,
        val currentStreak: Int,
        val recoveryConfig: RecoveryConfig,
        val monthStart: Long,
        val monthEnd: Long
    ) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

/**
 * Day data for calendar display
 */
data class DayData(
    val dayTimestamp: Long,
    val status: DayStatus,
    val sessionCount: Int,
    val totalReps: Int,
    val sessions: List<WorkoutSession>
)

/**
 * Day status enum
 */
enum class DayStatus {
    COMPLETED,          // Regular session completed (green)
    COMPLETED_MANUAL,   // Manual session (green with icon)
    RECOVERED,          // Recovered day (special icon)
    MISSED,             // No session (red)
    FUTURE              // Future day (grey)
}

/**
 * ViewModelFactory for CalendarViewModel
 */
class CalendarViewModelFactory(
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
