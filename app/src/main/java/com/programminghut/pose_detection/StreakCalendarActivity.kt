package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
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
        val repository = SessionRepository(
            sessionDao = database.sessionDao(),
            repDao = database.repDao()
        )
        
        // Initialize ViewModel
        val factory = CalendarViewModelFactory(repository)
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
                            when (status) {
                                DayStatus.COMPLETED, DayStatus.COMPLETED_MANUAL, DayStatus.RECOVERED -> {
                                    // Could open session detail
                                }
                                DayStatus.MISSED -> {
                                    // Show recovery option in detail card
                                }
                                DayStatus.FUTURE -> {
                                    // Do nothing
                                }
                            }
                        },
                        onRecoveryClick = { missedDayTimestamp ->
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
