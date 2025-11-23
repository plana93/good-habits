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
 * Activity per la selezione della camera per la modalità Urban Camera.
 * Avvia UrbanCameraActivity con effetti grafici urbani.
 */
class UrbanCameraSelectionActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Pose_detectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UrbanCameraSelectionButtons { cameraIndex ->
                        onCameraSelected(cameraIndex)
                    }
                }
            }
        }
    }
    
    private fun onCameraSelected(cameraIndex: Int) {
        try {
            android.util.Log.d("UrbanCameraSelection", "Camera selected: $cameraIndex")
            android.widget.Toast.makeText(this, "Avvio Urban Camera REFACTORED...", android.widget.Toast.LENGTH_SHORT).show()
            
            val intent = Intent(this, UrbanCameraActivityRefactored::class.java)
            intent.putExtra("cameraIndex", cameraIndex)
            intent.putExtra("isFrontCamera", cameraIndex == 1)
            
            android.util.Log.d("UrbanCameraSelection", "Intent created, starting activity...")
            startActivity(intent)
            
            android.util.Log.d("UrbanCameraSelection", "Activity started successfully")
            android.widget.Toast.makeText(this, "Activity avviata!", android.widget.Toast.LENGTH_SHORT).show()
            
            finish()
        } catch (e: Exception) {
            android.util.Log.e("UrbanCameraSelection", "ERRORE: ${e.message}", e)
            System.err.println("ERRORE UrbanCamera: ${e.message}")
            e.printStackTrace()
            android.widget.Toast.makeText(
                this, 
                "ERRORE nell'avvio: ${e.javaClass.simpleName}: ${e.message}", 
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
fun UrbanCameraSelectionButtons(onCameraSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎨 Urban Camera",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Scegli la camera per gli effetti urban street art",
            style = MaterialTheme.typography.bodyMedium,
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
