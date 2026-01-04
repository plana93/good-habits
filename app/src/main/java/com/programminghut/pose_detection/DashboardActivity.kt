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
import com.programminghut.pose_detection.data.repository.DailySessionRepository
import com.programminghut.pose_detection.ui.dashboard.DashboardScreen
import com.programminghut.pose_detection.ui.dashboard.DashboardViewModel
import com.programminghut.pose_detection.ui.dashboard.DashboardViewModelFactory

/**
 * Dashboard Activity
 * 
 * Displays analytics dashboard with KPIs, charts, and workout statistics.
 * Part of Phase 2: Dashboard Core implementation.
 * Phase 3: Added export and share functionality
 * Phase 4: Added calendar navigation
 */
class DashboardActivity : ComponentActivity() {
    
    private lateinit var viewModel: DashboardViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("DASH_Activity", "🚀 DashboardActivity.onCreate() called")
        
        // Setup database and repositories
        val database = AppDatabase.getDatabase(applicationContext)
        val sessionRepository = SessionRepository(
            sessionDao = database.sessionDao(),
            repDao = database.repDao()
        )
        val dailySessionRepository = DailySessionRepository(
            dailySessionDao = database.dailySessionDao(),
            dailySessionRelationDao = database.dailySessionRelationDao(),
            exerciseDao = database.exerciseDao(),
            workoutDao = database.workoutDao()
        )
        
        android.util.Log.d("DASH_Activity", "✅ Repositories created")
        
        // Initialize ViewModel
        val factory = DashboardViewModelFactory(sessionRepository, dailySessionRepository)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]
        
        android.util.Log.d("DASH_Activity", "✅ ViewModel created")
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onBackClick = { finish() },
                        onShareClick = { summary ->
                            shareText(summary)
                        },
                        onExportDataClick = { _, _ ->
                            // Launch Export Activity
                            val intent = Intent(this@DashboardActivity, ExportActivity::class.java)
                            startActivity(intent)
                        },
                        onCalendarClick = {
                            // Phase 4: Launch Calendar Activity
                            val intent = Intent(this@DashboardActivity, StreakCalendarActivity::class.java)
                            startActivity(intent)
                        },
                        onAddManualSessionClick = {
                            // Phase 4: Launch Manual Session Activity
                            val intent = Intent(this@DashboardActivity, ManualSessionActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Share text content using Android Share Sheet
     * Phase 3: Export & Share feature
     */
    private fun shareText(text: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Good Habits - Statistiche Dashboard")
        }
        startActivity(Intent.createChooser(shareIntent, "Condividi Dashboard"))
    }
    
    /**
     * Export data as file (CSV/JSON)
     * Phase 3: Data export feature
     */
    private fun exportData(content: String, mimeType: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(shareIntent, "Esporta Dati"))
    }
}
