package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

/**
 * Activity per la selezione della camera quando si vuole registrare lo scheletro.
 * Simile a CameraSelectionActivity ma avvia MainActivity in modalità RECORD_SKELETON.
 */
class RecordingCameraSelectionActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Pose_detectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecordingCameraSelectionButtons { cameraIndex ->
                        onCameraSelected(cameraIndex)
                    }
                }
            }
        }
    }
    
    private fun onCameraSelected(cameraIndex: Int) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("cameraIndex", cameraIndex)
        intent.putExtra("isFrontCamera", cameraIndex == 1) // Front camera index is 1
        intent.putExtra("RECORD_SKELETON", true)  // Flag per attivare la registrazione
        startActivity(intent)
        finish()
    }
}

@Composable
fun RecordingCameraSelectionButtons(onCameraSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Seleziona Camera per Registrazione",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = { onCameraSelected(1) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Front Camera")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onCameraSelected(0) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Back Camera")
        }
    }
}
