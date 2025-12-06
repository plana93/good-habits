package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.repository.SessionRepository
import com.programminghut.pose_detection.ui.export.ExportScreen
import com.programminghut.pose_detection.ui.export.ExportViewModel
import com.programminghut.pose_detection.ui.export.ExportViewModelFactory
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

/**
 * Export Activity
 * 
 * Provides data export functionality in multiple formats (CSV, JSON)
 * Phase 3: Export & Share implementation
 */
class ExportActivity : ComponentActivity() {
    
    private lateinit var viewModel: ExportViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SessionRepository(
            sessionDao = database.sessionDao(),
            repDao = database.repDao()
        )
        
        // Initialize ViewModel
        val factory = ExportViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ExportViewModel::class.java]
        
        setContent {
            Pose_detectionTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ExportScreen(
                        viewModel = viewModel,
                        onBackClick = { finish() },
                        onExportClick = { content, fileName, mimeType ->
                            exportFile(content, fileName, mimeType)
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Export data as file using Android Share Sheet
     * Phase 3: Data export feature
     */
    private fun exportFile(content: String, fileName: String, mimeType: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
        }
        startActivity(Intent.createChooser(shareIntent, "Esporta $fileName"))
    }
}
