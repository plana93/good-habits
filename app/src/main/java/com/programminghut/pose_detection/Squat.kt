package com.programminghut.pose_detection

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class Squat : ComponentActivity()  {

    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var model: LiteModelMovenetSingleposeLightningTfliteFloat164
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager
    lateinit var numberTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = LiteModelMovenetSingleposeLightningTfliteFloat164.newInstance(this)
        imageProcessor =
            ImageProcessor.Builder().add(ResizeOp(192, 192, ResizeOp.ResizeMethod.BILINEAR)).build()
        val path_base_position = R.drawable.base
        val path_squat_position = R.drawable.squat

        val outputFeature0_base_position = processImageAndDetectPose(path_base_position)
        val outputFeature0_squat_position = processImageAndDetectPose(path_squat_position)

        setContent {
            Pose_detectionTheme {
                // Use a column layout for better appearance
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Display a welcome message
                    Text(
                        text = "Squat Info",
                        color = Color.Gray

                    )
                    // Add some space between the text and the button
                    Spacer(modifier = Modifier.height(16.dp))
                    // Use a custom-styled button for better appearance
                    CustomButtonSquat(
                        text = "Continue",

                        onClick = {
                            val intent = Intent(this@Squat, CameraSelectionActivity::class.java)
                            intent.putExtra("base_position", outputFeature0_base_position)
                            intent.putExtra("squat_position", outputFeature0_squat_position)

                            startActivity(intent)
                        },

                        )
                }
            }
        }

    }

    private fun processImageAndDetectPose(image: Int): FloatArray {
        try {
            //val bitmap = BitmapFactory.decodeFile(imagePath)
            bitmap = BitmapFactory.decodeResource(resources, image);

            if (bitmap != null) {
                // Fai qualcosa con il bitmap
            } else {
                Log.e("Bitmap", "Decodifica dell'immagine fallita: Bitmap è null")
            }
        } catch (e: Exception) {
            Log.e("Bitmap", "Errore durante la decodifica dell'immagine: ${e.message}")
        }
        val tensorImage = preprocessImage(bitmap)
        return runPoseDetection(tensorImage)
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    private fun runPoseDetection(tensorImage: TensorImage): FloatArray {
        val inputFeature0 =
            TensorBuffer.createFixedSize(intArrayOf(1, 192, 192, 3), DataType.UINT8)
        inputFeature0.loadBuffer(tensorImage.buffer)
        val outputs = model.process(inputFeature0)
        return outputs.outputFeature0AsTensorBuffer.floatArray
    }

}

@Composable
fun CustomButtonSquat(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Adjust padding as needed
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = text)
        }
    }

}

@Preview(showBackground = true)
@Composable
fun squatPreview() {
    Pose_detectionTheme {
        Habits()
    }
}


