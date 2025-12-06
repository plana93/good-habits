package com.programminghut.pose_detection

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.ui.manual.ManualSessionScreen
import com.programminghut.pose_detection.ui.manual.ManualSessionViewModel
import com.programminghut.pose_detection.ui.manual.ManualSessionViewModelFactory
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

/**
 * ManualSessionActivity - Create manual workout sessions
 * 
 * Phase 4: Manual Session Creation
 * Allows users to manually add workouts completed without the app
 */
class ManualSessionActivity : ComponentActivity() {
    
    private lateinit var viewModel: ManualSessionViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SessionRepository(
            sessionDao = database.sessionDao(),
            repDao = database.repDao()
        )
        
        // Initialize ViewModel
        val factory = ManualSessionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ManualSessionViewModel::class.java]
        
        setContent {
            Pose_detectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ManualSessionScreen(
                        viewModel = viewModel,
                        onBackClick = { finish() },
                        onSessionCreated = { sessionId ->
                            Toast.makeText(
                                this,
                                "Sessione creata con successo!",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    )
                }
            }
        }
    }
}
