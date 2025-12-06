package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.ui.sessions.SessionDetailScreen
import com.programminghut.pose_detection.ui.sessions.SessionDetailViewModel
import com.programminghut.pose_detection.ui.sessions.SessionDetailViewModelFactory
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

/**
 * Activity for displaying detailed information about a single workout session
 * 
 * Shows session metadata, statistics, and rep-by-rep breakdown.
 * Part of Phase 1 MVP implementation.
 * Phase 3: Added share functionality
 */
class SessionDetailActivity : ComponentActivity() {
    
    private val viewModel: SessionDetailViewModel by viewModels {
        val sessionId = intent.getLongExtra("SESSION_ID", -1L)
        if (sessionId == -1L) {
            throw IllegalArgumentException("SESSION_ID must be provided")
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SessionRepository(database.sessionDao(), database.repDao())
        SessionDetailViewModelFactory(sessionId, repository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Pose_detectionTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SessionDetailScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            finish()
                        },
                        onShareClick = { summary ->
                            shareText(summary)
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
            putExtra(Intent.EXTRA_SUBJECT, "Good Habits - Riepilogo Allenamento")
        }
        startActivity(Intent.createChooser(shareIntent, "Condividi Allenamento"))
    }
}
