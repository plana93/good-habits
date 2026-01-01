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
import kotlinx.coroutines.flow.first
import com.programminghut.pose_detection.util.calendarDebug
import com.programminghut.pose_detection.util.recoveryDebug
import com.programminghut.pose_detection.util.Logging
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Job
import java.util.*

/**
 * CalendarViewModel - Manages calendar state and recovery logic
 * 
 * Phase 4: Session Recovery & Calendar
 */
class CalendarViewModel(
    private val repository: SessionRepository,
    private val dailySessionRepository: com.programminghut.pose_detection.data.repository.DailySessionRepository
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

    // Central recovery checker - single source of truth for whether a day is considered recovered
    private val recoveryChecker = com.programminghut.pose_detection.data.repository.RecoveryChecker(repository, dailySessionRepository)
    
    init {
        if (Logging.CALENDAR_DEBUG) calendarDebug("📅 CalendarViewModel init - starting load")
        loadCalendarData()
    }

    /**
     * Build day data map considering both workout sessions and daily session summaries
     */
    private fun buildDayDataMapWithDailySummaries(
        sessions: List<WorkoutSession>,
        missedDays: List<Long>,
        dailySummaries: Map<Long, com.programminghut.pose_detection.data.dao.DailySessionDaySummary>
    ): Map<Long, DayData> {
        // Delegate to shared implementation to keep logic testable
        return buildDayDataMapWithDailySummariesImpl(sessions, missedDays, dailySummaries)
    }
    
    /**
     * Load calendar data for current month
     */
    private var calendarDataJob: Job? = null

    fun loadCalendarData(monthOffset: Int = 0) {
        // Cancel any previous ongoing calendar load so we don't have multiple collectors
        calendarDataJob?.cancel()

        calendarDataJob = viewModelScope.launch {
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

                // Collect daily summaries reactively so UI stays in sync with Today changes
                dailySessionRepository.getDailySessionSummariesInRange(monthStart, monthEnd).collect { dailySummariesList ->
                    // Test debug: print to stdout so unit tests can observe when ViewModel receives summaries
                    println("[CalendarViewModel] Received ${dailySummariesList.size} daily summaries for range [$monthStart..$monthEnd]: $dailySummariesList")
                    if (Logging.CALENDAR_DEBUG) calendarDebug("🔁 Received ${dailySummariesList.size} daily summaries for range [$monthStart..$monthEnd]")
                    // Get sessions and missed days fresh for each emission
                    val sessions = repository.getSessionsForDateRange(monthStart, monthEnd)
                    if (Logging.CALENDAR_DEBUG) calendarDebug("🔎 Retrieved ${sessions.size} workout sessions")
                    val missedDays = repository.getMissedDays(monthStart, monthEnd)
                    if (Logging.CALENDAR_DEBUG) calendarDebug("⚠️ Retrieved ${missedDays.size} missed days")

                    val dailySummariesMap = dailySummariesList.associateBy { it.date }

                    // Calculate current streak
                    val currentStreak = repository.calculateStreakWithRecovery()

                    // Build day data map using daily summaries so days with only DailySession items are shown as completed
                    val dayDataMap = buildDayDataMapWithDailySummaries(sessions, missedDays, dailySummariesMap).toMutableMap()

                    // Ensure we mark days as RECOVERED if the central RecoveryChecker says so
                    val daysToCheck = dayDataMap.keys.toList()
                    daysToCheck.forEach { day ->
                        try {
                            if (recoveryChecker.isDateRecoveredFinal(day)) {
                                if (Logging.RECOVERY_DEBUG) recoveryDebug("✅ RecoveryChecker: day $day is recovered - marking RECOVERED")
                                val existing = dayDataMap[day]
                                if (existing != null) {
                                    dayDataMap[day] = existing.copy(status = DayStatus.RECOVERED)
                                } else {
                                    dayDataMap[day] = DayData(dayTimestamp = day, status = DayStatus.RECOVERED, sessionCount = 0, totalReps = 0, sessions = emptyList())
                                }
                            }
                        } catch (e: Exception) {
                            // ignore recovery check errors and leave existing status
                            if (Logging.RECOVERY_DEBUG) recoveryDebug("❌ RecoveryChecker error for day $day: ${e.message}")
                        }
                    }

                    // Special-case: if today has an existing DailySession but no items (was cleared),
                    // show IN_PROGRESS instead of MISSED to reflect that the user has an open session
                    try {
                        val todayStart = getStartOfDay(System.currentTimeMillis())
                        if (dayDataMap.containsKey(todayStart)) {
                            val todayData = dayDataMap[todayStart]
                            if (todayData?.status == DayStatus.MISSED) {
                                val existingSession = dailySessionRepository.getSessionForDate(todayStart)
                                if (existingSession != null) {
                                    if (Logging.CALENDAR_DEBUG) calendarDebug("ℹ️ Today has empty session - marking IN_PROGRESS for $todayStart")
                                    dayDataMap[todayStart] = todayData.copy(status = DayStatus.IN_PROGRESS)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }

                    if (Logging.CALENDAR_DEBUG) calendarDebug("➡️ Emitting CalendarUiState.Success with ${dayDataMap.size} days, currentStreak=$currentStreak")
                    if (Logging.CALENDAR_DEBUG) calendarDebug("   monthStart=$monthStart monthEnd=$monthEnd")

                    if (Logging.CALENDAR_DEBUG) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dayDataMap.keys.sorted().forEach { dayKey ->
                            val d = dayDataMap[dayKey]
                            calendarDebug("   ${sdf.format(Date(dayKey))} -> ${d?.status} (items=${d?.sessionCount}, reps=${d?.totalReps})")
                        }
                    }

                    // Emit new state
                    
                    _uiState.value = CalendarUiState.Success(
                        dayDataMap = dayDataMap,
                        currentStreak = currentStreak,
                        recoveryConfig = recoveryConfig,
                        monthStart = monthStart,
                        monthEnd = monthEnd
                    )
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
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
    COMPLETED_DAILY,    // Completed via daily session (not exported to workout_sessions)
    RECOVERED,          // Recovered day (special icon)
    IN_PROGRESS,        // Day has a daily session in progress (not exported to workout_sessions)
    MISSED,             // No session (red)
    FUTURE              // Future day (grey)
}

/**
 * ViewModelFactory for CalendarViewModel
 */
class CalendarViewModelFactory(
    private val repository: SessionRepository,
    private val dailySessionRepository: com.programminghut.pose_detection.data.repository.DailySessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            return CalendarViewModel(repository, dailySessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
