package com.programminghut.pose_detection

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
import com.programminghut.pose_detection.ui.dashboard.DashboardScreen
import com.programminghut.pose_detection.ui.dashboard.DashboardViewModel
import com.programminghut.pose_detection.ui.dashboard.DashboardViewModelFactory

/**
 * Dashboard Activity
 * 
 * Displays analytics dashboard with KPIs, charts, and workout statistics.
 * Part of Phase 2: Dashboard Core implementation.
 */
class DashboardActivity : ComponentActivity() {
    
    private lateinit var viewModel: DashboardViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SessionRepository(
            sessionDao = database.sessionDao(),
            repDao = database.repDao()
        )
        
        // Initialize ViewModel
        val factory = DashboardViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
