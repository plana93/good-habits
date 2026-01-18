package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.ui.calendar.CalendarViewModel
import com.programminghut.pose_detection.ui.calendar.CalendarViewModelFactory
import com.programminghut.pose_detection.ui.calendar.DayStatus
import com.programminghut.pose_detection.ui.calendar.StreakCalendarScreen
import com.programminghut.pose_detection.ui.activity.NewMainActivity
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

/**
 * StreakCalendarActivity - Calendario della Costanza
 * 
 * Phase 4: Session Recovery & Calendar
 * Displays monthly calendar with workout tracking and recovery functionality
 */
class StreakCalendarActivity : ComponentActivity() {
    
    private lateinit var viewModel: CalendarViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        
        // Initialize DailySessionRepository (needed to detect in-progress days)
        val dailySessionRepository = com.programminghut.pose_detection.data.repository.DailySessionRepository(
            dailySessionDao = database.dailySessionDao(),
            dailySessionRelationDao = database.dailySessionRelationDao(),
            exerciseDao = database.exerciseDao(),
            workoutDao = database.workoutDao()
        )
        
        // ✅ CALENDAR BUG FIX: Pass dailySessionDao for correct missed days calculation
        val repository = SessionRepository(
            sessionDao = database.sessionDao(),
            repDao = database.repDao(),
            dailySessionDao = database.dailySessionDao()
        )

        // Initialize ViewModel with both repositories
        val factory = CalendarViewModelFactory(repository, dailySessionRepository)
        viewModel = ViewModelProvider(this, factory)[CalendarViewModel::class.java]
        
        setContent {
            Pose_detectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StreakCalendarScreen(
                        viewModel = viewModel,
                        onBackClick = { finish() },
                        onDayClick = { timestamp, status ->
                            // Handle day selection
                            Log.d("CALENDAR_DEBUG", "🗓️ Day clicked: $timestamp, status: $status")
                            when (status) {
                                DayStatus.COMPLETED, DayStatus.COMPLETED_MANUAL, DayStatus.COMPLETED_DAILY -> {
                                    Log.d("CALENDAR_DEBUG", "✅ Completed day clicked - navigating to Today screen")
                                    // Per giorni completati, vai alla schermata Oggi per quella data
                                    val intent = Intent(this, NewMainActivity::class.java).apply {
                                        putExtra("NAVIGATE_TO_DATE", timestamp)
                                    }
                                    startActivity(intent)
                                }
                                DayStatus.IN_PROGRESS -> {
                                    Log.d("CALENDAR_DEBUG", "⏳ In-progress day clicked - navigating to Today screen")
                                    val intent = Intent(this, NewMainActivity::class.java).apply {
                                        putExtra("NAVIGATE_TO_DATE", timestamp)
                                    }
                                    startActivity(intent)
                                }
                                DayStatus.RECOVERED -> {
                                    Log.d("CALENDAR_DEBUG", "🎉 Recovered day clicked - navigating to Today screen")
                                    // Per giorni recuperati, vai alla schermata Oggi per quella data
                                    val intent = Intent(this, NewMainActivity::class.java).apply {
                                        putExtra("NAVIGATE_TO_DATE", timestamp)
                                    }
                                    startActivity(intent)
                                }
                                DayStatus.MISSED -> {
                                    Log.d("CALENDAR_DEBUG", "❌ Missed day clicked - launching Today in RECOVERY mode")
                                    // Avvia la Main/Today activity in modalità RECOVERY per permettere procedura AI Squat
                                    val intent = Intent(this, NewMainActivity::class.java).apply {
                                        putExtra("MODE", "RECOVERY")
                                        putExtra("RECOVERED_DATE", timestamp)
                                        putExtra("MIN_REPS_REQUIRED", 50)
                                    }
                                    startActivity(intent)
                                }
                                DayStatus.FUTURE -> {
                                    Log.d("CALENDAR_DEBUG", "⏳ Future day clicked")
                                    // Do nothing
                                }
                            }
                        },
                        onRecoveryClick = { missedDayTimestamp ->
                            Log.d("CALENDAR_DEBUG", "🔄 Recovery button clicked for: $missedDayTimestamp")
                            // Launch CameraSelectionActivity in RECOVERY mode
                            val intent = Intent(this, CameraSelectionActivity::class.java).apply {
                                putExtra("MODE", "RECOVERY")
                                putExtra("RECOVERED_DATE", missedDayTimestamp)
                                putExtra("MIN_REPS_REQUIRED", 50)
                            }
                            startActivity(intent)
                            // Don't call finish() - stay open so calendar refreshes when user returns
                        }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload calendar data when returning from recovery session
        viewModel.loadCalendarData(0)
    }
}
