package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.ui.sessions.SessionsListScreen
import com.programminghut.pose_detection.ui.sessions.SessionsViewModel
import com.programminghut.pose_detection.ui.sessions.SessionsViewModelFactory
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

/**
 * Activity for displaying workout sessions history
 * 
 * Shows a list of all workout sessions with filtering and statistics.
 * Part of Phase 1 MVP implementation.
 */
class SessionsHistoryActivity : ComponentActivity() {
    
    private val viewModel: SessionsViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SessionRepository(database.sessionDao(), database.repDao())
        SessionsViewModelFactory(repository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Pose_detectionTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SessionsListScreen(
                        viewModel = viewModel,
                        onSessionClick = { sessionId ->
                            // Navigate to detail screen
                            val intent = Intent(this, SessionDetailActivity::class.java)
                            intent.putExtra("SESSION_ID", sessionId)
                            startActivity(intent)
                        },
                        onBackClick = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}
