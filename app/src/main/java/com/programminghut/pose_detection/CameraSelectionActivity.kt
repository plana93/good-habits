package com.programminghut.pose_detection


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat.startActivityForResult
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height


interface OnCameraSelectedListener {
    fun onCameraSelected(cameraIndex: Int)
}

class CameraSelectionActivity : ComponentActivity(), OnCameraSelectedListener {
    var outputFeature0_base_position: FloatArray? = null
    var outputFeature0_squat_position: FloatArray? = null
    
    // Phase 4: Recovery mode parameters
    private var isRecoveryMode = false
    private var recoveredDate: Long = 0
    private var minRepsRequired = 50
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Phase 4: Check for recovery mode
        isRecoveryMode = intent.getStringExtra("MODE") == "RECOVERY"
        if (isRecoveryMode) {
            recoveredDate = intent.getLongExtra("RECOVERED_DATE", 0)
            minRepsRequired = intent.getIntExtra("MIN_REPS_REQUIRED", 50)
            // In recovery mode, calibration positions are optional
        } else {
            // Normal mode: require calibration positions
            outputFeature0_base_position = intent.getFloatArrayExtra("base_position")
            outputFeature0_squat_position = intent.getFloatArrayExtra("squat_position")
        }

        setContent {
            Pose_detectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraSelectionButtons(this@CameraSelectionActivity)
                }
            }
        }
    }


    override fun onCameraSelected(cameraIndex: Int) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("cameraIndex", cameraIndex)
        intent.putExtra("isFrontCamera", cameraIndex == 1) // Front camera index is 1
        
        // Add calibration positions if available (normal mode)
        outputFeature0_base_position?.let {
            intent.putExtra("base_position", it)
        }
        outputFeature0_squat_position?.let {
            intent.putExtra("squat_position", it)
        }
        
        // Phase 4: Propagate recovery mode parameters
        if (isRecoveryMode) {
            intent.putExtra("MODE", "RECOVERY")
            intent.putExtra("RECOVERED_DATE", recoveredDate)
            intent.putExtra("MIN_REPS_REQUIRED", minRepsRequired)
        }
        
        startActivity(intent)
    }
}

@Composable
fun CameraSelectionButtons(onCameraSelectedListener: OnCameraSelectedListener) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CameraSelectionButton("Front Camera") {
            onCameraSelectedListener.onCameraSelected(1)
        }
        Spacer(modifier = Modifier.height(16.dp))
        CameraSelectionButton("Back Camera") {
            onCameraSelectedListener.onCameraSelected(0)
        }
    }
}

@Composable
fun CameraSelectionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(text = text)
    }
}
