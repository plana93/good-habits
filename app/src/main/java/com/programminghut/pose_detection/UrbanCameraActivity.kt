package com.programminghut.pose_detection

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import java.io.File
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
import android.view.View
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import com.programminghut.pose_detection.urban.UrbanConfig
import com.programminghut.pose_detection.urban.UrbanEffectsManager
import com.programminghut.pose_detection.effects.FilterManager
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
    // Variabile per tenere traccia se la persona è rilevata (modalità conteggio squat)
    private var isPersonDetected: Boolean = false
    // Directory dove salvare le foto
    private val photoDir: File by lazy {
        File(getExternalFilesDir(null), "urban_photos").apply { mkdirs() }
    }
    
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
    
    // UI Components
    private lateinit var btnMenuTrigger: ImageButton
    private lateinit var dropdownMenu: CardView
    private lateinit var knobLeft: ImageView
    private lateinit var knobRight: ImageView
    private var menuVisible = false
    private var currentKnobRotation = 0f
    
    // Bitmap processata più di recente, usata per il salvataggio foto
    private var latestProcessedBitmap: Bitmap? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Toast PRIMA di tutto per vedere se arriviamo qui
        try {
            android.widget.Toast.makeText(this, "UrbanCamera: START!", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Ignora errori nel toast
        }
        
        super.onCreate(savedInstanceState)
        
        try {
            android.util.Log.d("UrbanCamera", "onCreate started")
            android.widget.Toast.makeText(this, "Urban Camera: onCreate", android.widget.Toast.LENGTH_SHORT).show()

            // USA LAYOUT COMPLETO RIDISEGNATO
            setContentView(R.layout.activity_urban_camera)
            android.widget.Toast.makeText(this, "Layout set", android.widget.Toast.LENGTH_SHORT).show()

            selectedCameraIndex = intent.getIntExtra("cameraIndex", -1)
            isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)

            android.util.Log.d("UrbanCamera", "Camera index: $selectedCameraIndex, isFront: $isFrontCamera")

            // Inizializza componenti
            initializeComponents()
            android.util.Log.d("UrbanCamera", "Components initialized")
            android.widget.Toast.makeText(this, "Components OK", android.widget.Toast.LENGTH_SHORT).show()

            // Inizializza filtri modulari (FilterManager)
            com.programminghut.pose_detection.effects.FilterManager.registerDefaultFilters()
            // Attiva di default lo SkeletonFilter (puoi cambiare qui quali filtri attivare)
            val skeleton = com.programminghut.pose_detection.effects.FilterManager.getAvailableFilters().find { it.javaClass.simpleName == "SkeletonFilter" }
            if (skeleton != null) com.programminghut.pose_detection.effects.FilterManager.activateFilter(skeleton)

            // Inizializza UI con layout ridisegnato
            initializeUI()
            android.util.Log.d("UrbanCamera", "UI initialized")
            android.widget.Toast.makeText(this, "UI OK", android.widget.Toast.LENGTH_SHORT).show()

            getPermissions()
            android.util.Log.d("UrbanCamera", "Permissions checked")

            // Setup texture listener
            setupTextureListener()
            android.util.Log.d("UrbanCamera", "Texture listener set up")
            android.widget.Toast.makeText(this, "Urban Camera Ready!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "Error in onCreate: ${e.message}", e)
            System.err.println("ERRORE UrbanCamera onCreate: ${e.message}")
            e.printStackTrace()
            android.widget.Toast.makeText(this, "ERRORE: ${e.javaClass.simpleName}: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
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
    
    private fun initializeUI() {
        btnMenuTrigger = findViewById(R.id.btn_menu_trigger)
        dropdownMenu = findViewById(R.id.dropdown_menu)
        knobLeft = findViewById(R.id.knob_left)
        knobRight = findViewById(R.id.knob_right)
        
        val captureButton = findViewById<View>(R.id.capture_button)
        
        // Menu trigger button
        btnMenuTrigger.setOnClickListener {
            toggleMenu()
        }
        
        // ══════════════════════════════════════════════════════════
        // CAPTURE BUTTON - Multifunzione (foto/video)
        // ══════════════════════════════════════════════════════════
        captureButton.setOnClickListener {
            processCurrentFrameAndShowDialog()
        }
        
        captureButton.setOnLongClickListener {
            // Pressione prolungata → Registra video
            android.widget.Toast.makeText(this, "🎥 Video recording...", android.widget.Toast.LENGTH_SHORT).show()
            animateCaptureButton()
            true
        }
        
        // Menu items - Filter selection
        findViewById<TextView>(R.id.menu_filter_bw).setOnClickListener {
            UrbanConfig.CURRENT_FILTER = UrbanConfig.FilterType.BLACK_WHITE
            toggleMenu()
        }
        
        findViewById<TextView>(R.id.menu_filter_sobel).setOnClickListener {
            UrbanConfig.CURRENT_FILTER = UrbanConfig.FilterType.SOBEL
            toggleMenu()
        }
        
        findViewById<TextView>(R.id.menu_filter_pixel).setOnClickListener {
            UrbanConfig.CURRENT_FILTER = UrbanConfig.FilterType.PIXELATED
            toggleMenu()
        }
        
        findViewById<TextView>(R.id.menu_filter_none).setOnClickListener {
            UrbanConfig.CURRENT_FILTER = UrbanConfig.FilterType.NONE
            toggleMenu()
        }
        
        findViewById<TextView>(R.id.menu_switch_camera).setOnClickListener {
            // TODO: Implement camera switch
            toggleMenu()
        }
        
        // Knobs - make them interactive
        knobLeft.setOnClickListener {
            rotateKnob(knobLeft, true)
            // Adjust box appearance probability
            UrbanConfig.BOX_APPEAR_PROBABILITY = 
                (UrbanConfig.BOX_APPEAR_PROBABILITY + 0.1f).coerceIn(0.1f, 1.0f)
        }
        
        knobRight.setOnClickListener {
            rotateKnob(knobRight, false)
            // Adjust box size
            UrbanConfig.BOX_SIZE = 
                (UrbanConfig.BOX_SIZE + 10).coerceIn(30, 150)
        }
    }
    
    private fun animateCaptureButton() {
        val captureButton = findViewById<View>(R.id.capture_button)
        captureButton.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(100)
            .withEndAction {
                captureButton.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    
    private fun toggleMenu() {
        menuVisible = !menuVisible
        dropdownMenu.visibility = if (menuVisible) View.VISIBLE else View.GONE
        
        // Animate menu button rotation
        val rotation = if (menuVisible) 180f else 0f
        btnMenuTrigger.animate()
            .rotation(rotation)
            .setDuration(200)
            .start()
    }
    
    private fun rotateKnob(knob: ImageView, clockwise: Boolean) {
        val rotationDelta = if (clockwise) 45f else -45f
        currentKnobRotation += rotationDelta
        
        val rotate = RotateAnimation(
            currentKnobRotation - rotationDelta,
            currentKnobRotation,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 200
        rotate.fillAfter = true
        knob.startAnimation(rotate)
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

                // --- LOGICA BLOCCO SCHERMO ---
                // Consideriamo "persona rilevata" se almeno una box attiva è presente
                val personaRilevata = urbanEffectsHasActiveBoxes()
                if (personaRilevata && !isPersonDetected) {
                    // Mantieni schermo acceso
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    isPersonDetected = true
                } else if (!personaRilevata && isPersonDetected) {
                    // Ripristina blocco schermo
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    isPersonDetected = false
                }

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

                // Applica i filtri modulari (es. Skeleton)
                com.programminghut.pose_detection.effects.FilterManager.applyFilters(canvas, bitmap, outputFeature0)

                // Poi disegna gli effetti urban sopra
                urbanEffects.drawBoxes(canvas, bitmap)

                // Mostra il risultato (l'ImageView userà centerCrop per mantenere aspect ratio)
                imageView.setImageBitmap(mutableBitmap)
                latestProcessedBitmap = mutableBitmap

                android.util.Log.d("UrbanCamera", "[PREVIEW] Frame processato e mostrato in imageView")
                logActiveFilters("[PREVIEW]")
            }
        }
    }
    
    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    // Funzione di supporto: verifica se ci sono box attive (presenza persona)
    private fun urbanEffectsHasActiveBoxes(): Boolean {
        // Usa reflection per accedere a activeBoxes (private)
        return try {
            val field = urbanEffects.javaClass.getDeclaredField("activeBoxes")
            field.isAccessible = true
            val map = field.get(urbanEffects) as? Map<*, *>
            map != null && map.isNotEmpty()
        } catch (e: Exception) {
            false
        }
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
    
    private fun logActiveFilters(context: String) {
    val active = com.programminghut.pose_detection.effects.FilterManager.getActiveFilters()
        android.util.Log.d("UrbanCamera", "$context - Filtri attivi: ${active.joinToString { it.javaClass.simpleName }}")
    }
    
    // 1. Aggiungi una funzione per mostrare la preview e chiedere conferma
    private fun showSaveDialog(bitmap: Bitmap) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Salva foto?")
            .setView(ImageView(this).apply { setImageBitmap(bitmap) })
            .setPositiveButton("Salva") { _, _ ->
                val photoFile = File(photoDir, "urban_${System.currentTimeMillis()}.png")
                val saved = com.programminghut.pose_detection.util.BitmapUtils.saveBitmapToFile(bitmap, photoFile)
                android.util.Log.d("UrbanCamera", "[SALVATAGGIO] Foto salvata: ${photoFile.absolutePath}, success: $saved")
                if (saved) {
                    android.widget.Toast.makeText(this, "📸 Foto salvata: ${photoFile.name}", android.widget.Toast.LENGTH_SHORT).show()
                    showFinalPhoto(photoFile)
                } else {
                    android.widget.Toast.makeText(this, "Errore salvataggio foto", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Scarta") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
    
    private fun showFinalPhoto(photoFile: File) {
        val imageView = ImageView(this)
        imageView.setImageBitmap(android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath))
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Foto salvata!")
            .setView(imageView)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .create()
        dialog.show()
    }
    
    private fun processCurrentFrameAndShowDialog() {
        android.util.Log.d("UrbanCamera", "[SCATTO] Inizio processing frame per salvataggio")
        val cameraBitmap = textureView.bitmap ?: run {
            android.util.Log.e("UrbanCamera", "[SCATTO] textureView.bitmap è null!")
            return
        }
        val tensorImage = preprocessImage(cameraBitmap)
        val outputFeature0 = runPoseDetection(tensorImage)
        val overlayBitmap = Bitmap.createBitmap(
            cameraBitmap.width,
            cameraBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(overlayBitmap)
        canvas.drawBitmap(cameraBitmap, 0f, 0f, null)
    com.programminghut.pose_detection.effects.FilterManager.applyFilters(canvas, cameraBitmap, outputFeature0)
        logActiveFilters("[SCATTO]")
        urbanEffects.drawBoxes(canvas, cameraBitmap)
        android.util.Log.d("UrbanCamera", "[SCATTO] Frame processato, mostro dialog di conferma")
        showSaveDialog(overlayBitmap)
    }
}
