package com.programminghut.pose_detection.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.programminghut.pose_detection.data.repository.SessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for Dashboard Screen
 * 
 * Manages dashboard state with KPIs, charts data, and statistics.
 * Phase 2: Dashboard Core implementation.
 */
class DashboardViewModel(
    private val repository: SessionRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    /**
     * Load all dashboard data
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                // Combine multiple flows
                combine(
                    repository.getTotalSessionsCount(),
                    repository.getTotalRepsAllTime(),
                    repository.getAverageFormScore(),
                    repository.getTotalWorkoutTimeSeconds(),
                    repository.getAllSessions()
                ) { totalSessions, totalReps, avgForm, totalTime, allSessions ->
                    
                    // Calculate KPIs
                    val kpiData = KpiData(
                        totalSessions = totalSessions,
                        totalReps = totalReps,
                        avgFormScore = avgForm,
                        totalWorkoutTimeSeconds = totalTime,
                        currentStreak = calculateStreak(allSessions),
                        bestSession = allSessions.maxByOrNull { it.avgFormScore }
                    )
                    
                    // Prepare chart data
                    val chartData = prepareChartData(allSessions)
                    
                    DashboardUiState.Success(
                        kpiData = kpiData,
                        chartData = chartData
                    )
                }.collect { state ->
                    _uiState.value = state
                }
                
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Calculate current workout streak (consecutive days with workouts)
     */
    private fun calculateStreak(sessions: List<com.programminghut.pose_detection.data.model.WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Group sessions by date
        val sessionsByDate = sessions
            .groupBy { session ->
                Calendar.getInstance().apply {
                    timeInMillis = session.startTime
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            .keys
            .sorted()
            .reversed() // Most recent first
        
        if (sessionsByDate.isEmpty()) return 0
        
        // Check if there's a session today or yesterday
        val yesterday = today - (24 * 60 * 60 * 1000)
        val mostRecentDay = sessionsByDate.first()
        
        if (mostRecentDay != today && mostRecentDay != yesterday) {
            return 0 // Streak broken
        }
        
        // Count consecutive days
        var streak = 0
        var currentDay = if (mostRecentDay == today) today else yesterday
        
        for (day in sessionsByDate) {
            if (day == currentDay) {
                streak++
                currentDay -= (24 * 60 * 60 * 1000) // Go back one day
            } else {
                break
            }
        }
        
        return streak
    }
    
    /**
     * Prepare data for charts
     */
    private fun prepareChartData(sessions: List<com.programminghut.pose_detection.data.model.WorkoutSession>): ChartData {
        // Last 30 days data
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentSessions = sessions.filter { it.startTime >= thirtyDaysAgo }
        
        // Group by date for line chart
        val repsByDate = mutableMapOf<Long, Int>()
        recentSessions.forEach { session ->
            val dayStart = getDayStart(session.startTime)
            repsByDate[dayStart] = (repsByDate[dayStart] ?: 0) + session.totalReps
        }
        
        // Form quality distribution for bar chart
        val formDistribution = sessions.groupBy { 
            when {
                it.avgFormScore >= 0.8f -> "Excellent"
                it.avgFormScore >= 0.6f -> "Good"
                it.avgFormScore >= 0.4f -> "Fair"
                else -> "Poor"
            }
        }.mapValues { it.value.size }
        
        // Calendar heatmap data (last 90 days)
        val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        val heatmapData = sessions
            .filter { it.startTime >= ninetyDaysAgo }
            .groupBy { getDayStart(it.startTime) }
            .mapValues { it.value.sumOf { session -> session.totalReps } }
        
        return ChartData(
            repsByDate = repsByDate.toList().sortedBy { it.first },
            formDistribution = formDistribution,
            heatmapData = heatmapData
        )
    }
    
    /**
     * Get start of day timestamp
     */
    private fun getDayStart(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

/**
 * UI State for Dashboard
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val kpiData: KpiData,
        val chartData: ChartData
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

/**
 * KPI Data
 */
data class KpiData(
    val totalSessions: Int,
    val totalReps: Int,
    val avgFormScore: Float,
    val totalWorkoutTimeSeconds: Int,
    val currentStreak: Int,
    val bestSession: com.programminghut.pose_detection.data.model.WorkoutSession?
) {
    fun formatTotalTime(): String {
        val hours = totalWorkoutTimeSeconds / 3600
        val minutes = (totalWorkoutTimeSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalWorkoutTimeSeconds}s"
        }
    }
}

/**
 * Chart Data
 */
data class ChartData(
    val repsByDate: List<Pair<Long, Int>>,  // Timestamp to reps count
    val formDistribution: Map<String, Int>,  // Quality category to count
    val heatmapData: Map<Long, Int>          // Date to total reps
)

/**
 * Factory for DashboardViewModel
 */
class DashboardViewModelFactory(
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
