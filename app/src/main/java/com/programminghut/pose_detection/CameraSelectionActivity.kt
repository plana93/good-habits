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
    lateinit  var outputFeature0_base_position: FloatArray
    lateinit var outputFeature0_squat_position: FloatArray
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        outputFeature0_base_position = intent.getFloatArrayExtra("base_position")!!
        outputFeature0_squat_position = intent.getFloatArrayExtra("squat_position")!!

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
        intent.putExtra("base_position", outputFeature0_base_position)
        intent.putExtra("squat_position", outputFeature0_squat_position)
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
