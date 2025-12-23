package com.programminghut.pose_detection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity per catturare foto e rilevare pose per la creazione di esercizi
 */
class PhotoCaptureActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PhotoCaptureActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        
        const val EXTRA_POSITION_TYPE = "position_type"
        const val EXTRA_KEYPOINTS = "keypoints"
        const val POSITION_START = "start"
        const val POSITION_END = "end"
    }
    
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var model: LiteModelMovenetSingleposeLightningTfliteFloat164
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var statusText: TextView
    private lateinit var instructionText: TextView
    private lateinit var switchCameraButton: Button
    
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var detectedKeypoints: FloatArray? = null
    private var positionType: String = POSITION_START
    private var useFrontCamera = false  // Default: camera posteriore
    private var cameraProvider: ProcessCameraProvider? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContentView(R.layout.activity_photo_capture)
        
        positionType = intent.getStringExtra(EXTRA_POSITION_TYPE) ?: POSITION_START
        Log.d(TAG, "Position type: $positionType")
        
        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        statusText = findViewById(R.id.statusText)
        instructionText = findViewById(R.id.instructionText)
        switchCameraButton = findViewById(R.id.switchCameraButton)
        
        Log.d(TAG, "Views initialized successfully")
        
        // Setup istruzioni
        instructionText.text = if (positionType == POSITION_START) {
            "📸 Posizionati nella posizione INIZIALE dell'esercizio"
        } else {
            "📸 Posizionati nella posizione MASSIMA dell'esercizio"
        }
        
        captureButton.isEnabled = false
        captureButton.setOnClickListener {
            capturePose()
        }
        
        switchCameraButton.setOnClickListener {
            useFrontCamera = !useFrontCamera
            switchCamera()
        }
        
        findViewById<Button>(R.id.backButton).setOnClickListener {
            Log.d(TAG, "Back button clicked")
            finish()
        }
        
        // Initialize MoveNet model
        Log.d(TAG, "Initializing MoveNet model...")
        try {
            model = LiteModelMovenetSingleposeLightningTfliteFloat164.newInstance(this)
            Log.d(TAG, "MoveNet model initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MoveNet model", e)
            statusText.text = "❌ Errore inizializzazione modello"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
            return
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        Log.d(TAG, "Camera executor created")
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            Log.d(TAG, "Camera permissions granted, starting camera")
            startCamera()
        } else {
            Log.d(TAG, "Requesting camera permissions")
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun startCamera() {
        Log.d(TAG, "startCamera() called")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "CameraProvider obtained successfully")
                bindCamera()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get CameraProvider", e)
                runOnUiThread {
                    statusText.text = "❌ Errore inizializzazione camera"
                    statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun switchCamera() {
        Log.d(TAG, "switchCamera() called - switching to ${if (!useFrontCamera) "front" else "back"} camera")
        bindCamera()
    }
    
    private fun bindCamera() {
        val cameraProviderInstance = cameraProvider ?: return
        
        try {
            // Unbind solo i use cases esistenti, non tutto
            preview?.let { cameraProviderInstance.unbind(it) }
            imageAnalyzer?.let { cameraProviderInstance.unbind(it) }
            
            // Crea nuovi use cases
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, PoseAnalyzer())
                }
            
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            
            // Bind use cases to camera
            camera = cameraProviderInstance.bindToLifecycle(
                this, cameraSelector, preview!!, imageAnalyzer!!
            )
            
            Log.d(TAG, "Camera bound successfully. Front camera: $useFrontCamera")
            
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }
    
    private inner class PoseAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            Log.d(TAG, "Analyzing frame: ${imageProxy.width}x${imageProxy.height}, format: ${imageProxy.format}")
            
            val bitmap = imageProxyToBitmap(imageProxy)
            
            if (bitmap != null) {
                Log.d(TAG, "Bitmap created successfully: ${bitmap.width}x${bitmap.height}")
                // Process con MoveNet
                val keypoints = processPose(bitmap)
                
                if (keypoints != null) {
                    detectedKeypoints = keypoints
                    runOnUiThread {
                        statusText.text = "✅ Postura rilevata - Premi Cattura"
                        statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                        captureButton.isEnabled = true
                    }
                } else {
                    runOnUiThread {
                        statusText.text = "⚠️ Posizionati in modo visibile"
                        statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
                        captureButton.isEnabled = false
                    }
                }
            } else {
                Log.e(TAG, "Failed to create bitmap from ImageProxy")
                runOnUiThread {
                    statusText.text = "❌ Errore conversione immagine"
                    statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
            
            imageProxy.close()
        }
    }
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            Log.d(TAG, "Converting ImageProxy to Bitmap - Planes: ${imageProxy.planes.size}")
            
            val buffer = imageProxy.planes[0].buffer
            val pixelStride = imageProxy.planes[0].pixelStride
            val rowStride = imageProxy.planes[0].rowStride
            val rowPadding = rowStride - pixelStride * imageProxy.width
            
            Log.d(TAG, "Buffer details - pixelStride: $pixelStride, rowStride: $rowStride, rowPadding: $rowPadding")
            
            // Crea bitmap con padding corretto
            val bitmap = Bitmap.createBitmap(
                imageProxy.width + rowPadding / pixelStride,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Crop per rimuovere il padding
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                imageProxy.width,
                imageProxy.height
            )
            
            // Ruota il bitmap se necessario
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            Log.d(TAG, "Rotation needed: $rotationDegrees degrees")
            
            if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(
                    croppedBitmap,
                    0,
                    0,
                    croppedBitmap.width,
                    croppedBitmap.height,
                    matrix,
                    true
                )
            } else {
                croppedBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            e.printStackTrace()
            null
        }
    }
    
    private fun processPose(bitmap: Bitmap): FloatArray? {
        try {
            Log.d(TAG, "Processing bitmap: ${bitmap.width}x${bitmap.height}")
            
            // Ridimensiona direttamente il bitmap a 192x192
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 192, 192, true)
            Log.d(TAG, "Scaled bitmap: ${scaledBitmap.width}x${scaledBitmap.height}")
            
            // Crea TensorBuffer manualmente con la dimensione corretta
            val inputShape = intArrayOf(1, 192, 192, 3)
            val tensorBuffer = TensorBuffer.createFixedSize(inputShape, DataType.FLOAT32)
            
            // Converti bitmap in array di float e carica nel buffer
            val pixels = IntArray(192 * 192)
            scaledBitmap.getPixels(pixels, 0, 192, 0, 0, 192, 192)
            
            val floatArray = FloatArray(192 * 192 * 3)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                floatArray[i * 3] = ((pixel shr 16) and 0xFF) / 255.0f      // R
                floatArray[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255.0f   // G
                floatArray[i * 3 + 2] = (pixel and 0xFF) / 255.0f           // B
            }
            tensorBuffer.loadArray(floatArray)
            
            Log.d(TAG, "TensorBuffer created manually: ${tensorBuffer.flatSize} elements")
            
            // Run inference
            val outputs = model.process(tensorBuffer)
            val outputFeature = outputs.outputFeature0AsTensorBuffer
            
            // Estrai keypoints (17 keypoints × 3 valori = 51)
            val keypoints = outputFeature.floatArray
            
            Log.d(TAG, "Keypoints received: ${keypoints.size} values")
            Log.d(TAG, "First few keypoints: ${keypoints.take(9).joinToString { "%.3f".format(it) }}")
            
            // Verifica che ci siano dati validi (almeno un valore di confidenza > 0.1)
            val hasValidData = keypoints.filterIndexed { index, _ -> 
                (index + 1) % 3 == 0  // Ogni terzo valore è la confidenza
            }.any { it > 0.1f }
            
            Log.d(TAG, "Has valid data: $hasValidData")
            
            return if (hasValidData) keypoints else null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pose", e)
            return null
        }
    }
    
    private fun capturePose() {
        val keypoints = detectedKeypoints ?: return
        
        Log.d(TAG, "Captured keypoints for $positionType: ${keypoints.contentToString()}")
        
        // Ritorna i keypoints
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_KEYPOINTS, keypoints)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
        cameraExecutor.shutdown()
        try {
            model.close()
            Log.d(TAG, "Model closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing model", e)
        }
    }
}
