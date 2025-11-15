package com.programminghut.pose_detection

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import com.programminghut.pose_detection.urban.UrbanEffectsManager
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

/**
 * Activity per la modalità Urban Camera.
 * Mostra effetti grafici urban street art basati sulla pose detection.
 */
class UrbanCameraActivity : AppCompatActivity() {
    
    // Camera e processing
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var model: LiteModelMovenetSingleposeLightningTfliteFloat164
    private lateinit var bitmap: Bitmap
    private lateinit var imageView: ImageView
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private var selectedCameraIndex = -1
    private var isFrontCamera = false
    
    // Urban effects
    private lateinit var urbanEffects: UrbanEffectsManager
    private val paint = Paint()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_urban_camera)
        
        selectedCameraIndex = intent.getIntExtra("cameraIndex", -1)
        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)
        
        // Inizializza componenti
        initializeComponents()
        getPermissions()
        
        // Setup texture listener
        setupTextureListener()
    }
    
    private fun initializeComponents() {
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(192, 192, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        
        model = LiteModelMovenetSingleposeLightningTfliteFloat164.newInstance(this)
        
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        handlerThread = HandlerThread("urbanCameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        
        // Inizializza il manager degli effetti urban
        urbanEffects = UrbanEffectsManager()
        
        paint.color = Color.YELLOW
    }
    
    private fun setupTextureListener() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
                // Configura aspect ratio corretto
                CameraAspectRatioHelper.configureTextureView16x9(textureView, isFrontCamera)
            }
            
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                // Riconfigura quando cambia dimensione
                CameraAspectRatioHelper.configureTextureView16x9(textureView, isFrontCamera)
            }
            
            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = false
            
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                
                // Processa l'immagine e rileva la pose
                val tensorImage = preprocessImage(bitmap)
                val outputFeature0 = runPoseDetection(tensorImage)
                
                // Aggiorna gli effetti urban in base alla pose
                urbanEffects.updateBoxes(outputFeature0, bitmap.width, bitmap.height)
                
                // Crea una bitmap mutabile per disegnare gli effetti
                // Usa la stessa dimensione della bitmap originale per evitare distorsioni
                val mutableBitmap = Bitmap.createBitmap(
                    bitmap.width, 
                    bitmap.height, 
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(mutableBitmap)
                
                // Disegna prima l'immagine originale
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                
                // Poi disegna gli effetti urban sopra
                urbanEffects.drawBoxes(canvas, bitmap)
                
                // Mostra il risultato (l'ImageView userà centerCrop per mantenere aspect ratio)
                imageView.setImageBitmap(mutableBitmap)
            }
        }
    }
    
    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }
    
    private fun runPoseDetection(tensorImage: TensorImage): FloatArray {
        val inputFeature0 = TensorBuffer.createFixedSize(
            intArrayOf(1, 192, 192, 3),
            DataType.UINT8
        )
        inputFeature0.loadBuffer(tensorImage.buffer)
        
        val outputs = model.process(inputFeature0)
        return outputs.outputFeature0AsTensorBuffer.floatArray
    }
    
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[selectedCameraIndex],
            object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    val surface = Surface(textureView.surfaceTexture)
                    captureRequest.addTarget(surface)
                    
                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                            }
                            
                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        },
                        handler
                    )
                }
                
                override fun onDisconnected(cameraDevice: CameraDevice) {}
                
                override fun onError(cameraDevice: CameraDevice, error: Int) {}
            },
            handler
        )
    }
    
    private fun getPermissions() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermissions()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        model.close()
        urbanEffects.reset()
    }
}
