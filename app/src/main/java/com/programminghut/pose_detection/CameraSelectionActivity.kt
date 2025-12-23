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
    
    // Phase 4: Recovery mode and AI mode parameters
    private var isRecoveryMode = false
    private var isAISquatMode = false
    private var recoveredDate: Long = 0
    private var minRepsRequired = 50
    
    // Phase 6: Exercise parameters
    private var exerciseId: Long = 0
    private var exerciseName: String = "Squat"
    private var exerciseType: String = "SQUAT"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Phase 6: Get exercise parameters
        exerciseId = intent.getLongExtra("EXERCISE_ID", 0)
        exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Squat"
        exerciseType = intent.getStringExtra("EXERCISE_TYPE") ?: "SQUAT"
        
        // Phase 4: Check for recovery mode and AI mode
        val mode = intent.getStringExtra("MODE")
        isRecoveryMode = mode == "RECOVERY"
        isAISquatMode = mode == "AI_SQUAT"
        
        if (isRecoveryMode) {
            recoveredDate = intent.getLongExtra("RECOVERED_DATE", 0)
            minRepsRequired = intent.getIntExtra("MIN_REPS_REQUIRED", 50)
            // In recovery mode, calibration positions are optional
        } else if (isAISquatMode) {
            minRepsRequired = intent.getIntExtra("MIN_REPS_REQUIRED", 20)
            // In AI mode, calibration positions are optional (auto-calibration)
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
        
        // Phase 6: Pass exercise parameters
        intent.putExtra("EXERCISE_ID", exerciseId)
        intent.putExtra("EXERCISE_NAME", exerciseName)
        intent.putExtra("EXERCISE_TYPE", exerciseType)
        
        // Add calibration positions if available (normal mode)
        outputFeature0_base_position?.let {
            intent.putExtra("base_position", it)
        }
        outputFeature0_squat_position?.let {
            intent.putExtra("squat_position", it)
        }
        
        // Phase 4: Propagate recovery mode and AI mode parameters
        if (isRecoveryMode) {
            intent.putExtra("MODE", "RECOVERY")
            intent.putExtra("RECOVERED_DATE", recoveredDate)
            intent.putExtra("MIN_REPS_REQUIRED", minRepsRequired)
            startActivity(intent)
        } else if (isAISquatMode) {
            intent.putExtra("MODE", "AI_SQUAT")
            intent.putExtra("MIN_REPS_REQUIRED", minRepsRequired)
            // ✅ In AI mode, propagate the result back to the launcher
            startActivityForResult(intent, 1001)
        } else {
            // Normal mode
            startActivity(intent)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            // AI_SQUAT mode: propagate result back to NewMainActivity
            setResult(resultCode, data)
            finish()
        }
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
