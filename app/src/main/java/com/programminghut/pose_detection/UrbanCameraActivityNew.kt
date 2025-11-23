package com.programminghut.pose_detection

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Urban Camera Activity con grafica animata fluida
 * - Camera a tutto schermo
 * - Menu laterali scorrevoli
 * - Linee animate dalla testa a tutti i keypoints
 * - Pallini verdi fluo
 */
class UrbanCameraActivityNew : AppCompatActivity() {
    
    // Camera components
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
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: android.media.ImageReader? = null
    
    // UI Components
    private lateinit var leftMenuContainer: FrameLayout
    private lateinit var rightMenuContainer: FrameLayout
    private lateinit var leftMenu: LinearLayout
    private lateinit var rightMenu: LinearLayout
    private lateinit var leftMenuIcon: View
    private lateinit var rightMenuIcon: View
    private lateinit var captureButton: FrameLayout
    private lateinit var recordingStatus: TextView
    
    // Video recording
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var videoFilePath: String? = null
    
    // Feature toggles
    private var animatedLinesEnabled = true
    private var glowDotsEnabled = true
    private var urbanBoxesEnabled = false
    private var saveWithEffects = false // Toggle per salvare con/senza effetti pose
    
    // Effect color (default neon green)
    private var effectColorR = 57
    private var effectColorG = 255
    private var effectColorB = 20
    
    // Camera filters
    private enum class CameraFilter {
        NONE, BLACK_WHITE, SOBEL, PIXELATED
    }
    private var currentFilter = CameraFilter.NONE
    
    // Urban effects manager
    private lateinit var urbanEffects: com.programminghut.pose_detection.urban.UrbanEffectsManager
    
    // Animation settings
    private var lineThickness = 2f
    private var animationSpeed = 1.0f
    private var curveAmount = 0.35f
    
    // Animation time
    private var animationTime = 0L
    private val animationStartTime = System.currentTimeMillis()
    
    // Menu state
    private var leftMenuOpen = false
    private var rightMenuOpen = false
    private var menuWidth = 0
    private var menuWidthLeft = 0
    private var menuWidthRight = 0

    // Latest processed bitmap (without pose/effects overlay) — used for saving natural photo
    private var latestProcessedBitmap: Bitmap? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_urban_camera_new)
        
        selectedCameraIndex = intent.getIntExtra("cameraIndex", -1)
        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)
        
    // Calcola larghezze menu: left piccolo (icone), right più ampio per impostazioni
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    menuWidthLeft = displayMetrics.widthPixels / 8
    menuWidthRight = (displayMetrics.widthPixels * 0.42).toInt()
    menuWidth = menuWidthLeft // default value used in some places
        
        initializeComponents()
        initializeUI()
        getPermissions()
        setupTextureListener()
    }
    
    private fun initializeComponents() {
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(192, 192, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        
        model = LiteModelMovenetSingleposeLightningTfliteFloat164.newInstance(this)
        
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        recordingStatus = findViewById(R.id.recordingStatus)
        
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        handlerThread = HandlerThread("urbanCameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        
        // Inizializza urban effects manager
        urbanEffects = com.programminghut.pose_detection.urban.UrbanEffectsManager()
    }
    
    private fun initializeUI() {
        leftMenuContainer = findViewById(R.id.leftMenuContainer)
        rightMenuContainer = findViewById(R.id.rightMenuContainer)
        leftMenu = findViewById(R.id.leftMenu)
        rightMenu = findViewById(R.id.rightMenu)
        leftMenuIcon = findViewById(R.id.leftMenuIcon)
        rightMenuIcon = findViewById(R.id.rightMenuIcon)
        captureButton = findViewById(R.id.captureButton)
        
    // Imposta larghezza menu containers (left small, right wider)
    leftMenuContainer.layoutParams.width = menuWidthLeft
    rightMenuContainer.layoutParams.width = menuWidthRight
    leftMenuContainer.requestLayout()
    rightMenuContainer.requestLayout()

    // Imposta posizione iniziale (fuori schermo)
    leftMenuContainer.translationX = -menuWidthLeft.toFloat()
    rightMenuContainer.translationX = menuWidthRight.toFloat()
        
        // Menu icon click listeners
        leftMenuIcon.setOnClickListener {
            toggleLeftMenu()
        }
        
        rightMenuIcon.setOnClickListener {
            toggleRightMenu()
        }
        
        // Capture button listeners: cattura in-app
        captureButton.setOnClickListener {
            // Tap - cattura foto con effetti pose
            capturePhoto()
            animateCaptureButton()
        }

        // TODO: Video recording richiede integrazione con Camera2 CaptureSession
        // Per ora disabilitato - solo foto supportate
        /*
        // Touch listener per video: tieni premuto = registra, rilascia = stop
        captureButton.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (!isRecording) {
                        handler.postDelayed({
                            startVideoRecording()
                            animateCaptureButton()
                        }, 300)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
                    if (isRecording) {
                        stopVideoRecording()
                        animateCaptureButton()
                    }
                    view.performClick()
                    true
                }
                else -> false
            }
        }
        */
        
        // Left menu switches
        findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchSkeleton).setOnCheckedChangeListener { _, isChecked ->
            animatedLinesEnabled = isChecked
        }
        
        findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchDots).setOnCheckedChangeListener { _, isChecked ->
            glowDotsEnabled = isChecked
        }
        
        findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchBoxes).setOnCheckedChangeListener { _, isChecked ->
            urbanBoxesEnabled = isChecked
        }
        
        // Save with effects toggle
        findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchSaveWithEffects).setOnCheckedChangeListener { _, isChecked ->
            saveWithEffects = isChecked
            val msg = if (isChecked) "📸 Saving WITH pose effects" else "📸 Saving natural photo"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        
        // Color RGB seekbars
        findViewById<SeekBar>(R.id.seekColorR).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                effectColorR = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.seekColorG).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                effectColorG = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.seekColorB).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                effectColorB = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Make left menu show only icons (hide label text but keep emoji if present)
        for (i in 0 until leftMenu.childCount) {
            val child = leftMenu.getChildAt(i)
            // Each feature item is a LinearLayout - find first TextView inside and reduce to the emoji before space
            if (child is LinearLayout) {
                for (j in 0 until child.childCount) {
                    val inner = child.getChildAt(j)
                    if (inner is LinearLayout) {
                        for (k in 0 until inner.childCount) {
                            val possibleText = inner.getChildAt(k)
                            if (possibleText is TextView) {
                                val txt = possibleText.text.toString()
                                val icon = txt.split(" ").getOrNull(0) ?: txt
                                possibleText.text = icon
                                break
                            }
                        }
                        break
                    }
                }
            }
        }
        
        // Filter buttons
        findViewById<Button>(R.id.btnFilterNone).setOnClickListener {
            currentFilter = CameraFilter.NONE
            Toast.makeText(this, "✨ Filter: OFF", Toast.LENGTH_SHORT).show()
            highlightSelectedFilter(R.id.btnFilterNone)
        }
        
        findViewById<Button>(R.id.btnFilterBW).setOnClickListener {
            currentFilter = CameraFilter.BLACK_WHITE
            Toast.makeText(this, "⚫ Filter: B&W", Toast.LENGTH_SHORT).show()
            highlightSelectedFilter(R.id.btnFilterBW)
        }
        
        findViewById<Button>(R.id.btnFilterSobel).setOnClickListener {
            currentFilter = CameraFilter.SOBEL
            Toast.makeText(this, "📐 Filter: Edge Detection", Toast.LENGTH_SHORT).show()
            highlightSelectedFilter(R.id.btnFilterSobel)
        }
        
        findViewById<Button>(R.id.btnFilterPixel).setOnClickListener {
            currentFilter = CameraFilter.PIXELATED
            Toast.makeText(this, "🎮 Filter: Pixelated", Toast.LENGTH_SHORT).show()
            highlightSelectedFilter(R.id.btnFilterPixel)
        }
        
        // Right menu seek bars
        findViewById<SeekBar>(R.id.seekLineThickness).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lineThickness = (progress + 1) / 2f  // 0.5 to 5.5
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.seekAnimSpeed).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                animationSpeed = (progress + 1) / 10f  // 0.1 to 2.1
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.seekCurveAmount).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                curveAmount = (progress + 10) / 100f  // 0.1 to 1.1
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<Button>(R.id.btnSwitchCamera).setOnClickListener {
            Toast.makeText(this, "Switch Camera", Toast.LENGTH_SHORT).show()
        }
        
        // Evidenzia il filtro iniziale
        highlightSelectedFilter(R.id.btnFilterNone)
    }
    
    private fun highlightSelectedFilter(selectedId: Int) {
        // Reset tutti i bottoni
        findViewById<Button>(R.id.btnFilterNone).backgroundTintList = 
            android.content.res.ColorStateList.valueOf(Color.parseColor("#222432"))
        findViewById<Button>(R.id.btnFilterBW).backgroundTintList = 
            android.content.res.ColorStateList.valueOf(Color.parseColor("#222432"))
        findViewById<Button>(R.id.btnFilterSobel).backgroundTintList = 
            android.content.res.ColorStateList.valueOf(Color.parseColor("#222432"))
        findViewById<Button>(R.id.btnFilterPixel).backgroundTintList = 
            android.content.res.ColorStateList.valueOf(Color.parseColor("#222432"))
        
        // Evidenzia il selezionato
        findViewById<Button>(selectedId).backgroundTintList = 
            android.content.res.ColorStateList.valueOf(Color.parseColor("#39FF14"))
        findViewById<Button>(selectedId).setTextColor(Color.BLACK)
    }
    
    private fun toggleLeftMenu() {
        leftMenuOpen = !leftMenuOpen
        leftMenuContainer.animate()
            .translationX(if (leftMenuOpen) 0f else -menuWidthLeft.toFloat())
            .setDuration(300)
            .start()
    }
    
    private fun toggleRightMenu() {
        rightMenuOpen = !rightMenuOpen
        rightMenuContainer.animate()
            .translationX(if (rightMenuOpen) 0f else menuWidthRight.toFloat())
            .setDuration(300)
            .start()
    }
    
    private fun animateCaptureButton() {
        captureButton.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
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
    
    private fun setupTextureListener() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
                CameraAspectRatioHelper.configureTextureView16x9(textureView, isFrontCamera)
            }
            
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                CameraAspectRatioHelper.configureTextureView16x9(textureView, isFrontCamera)
            }
            
            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = false
            
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                
                val tensorImage = preprocessImage(bitmap)
                val outputFeature0 = runPoseDetection(tensorImage)
                
                // Applica filtro camera se attivo
                var processedBitmap = bitmap
                if (currentFilter != CameraFilter.NONE) {
                    processedBitmap = applyFilter(bitmap, currentFilter)
                }
                // Aggiorna l'ultimo processedBitmap (senza effetti pose) per salvataggio naturale
                try {
                    latestProcessedBitmap?.recycle()
                } catch (e: Exception) {}
                latestProcessedBitmap = processedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                
                val mutableBitmap = Bitmap.createBitmap(
                    processedBitmap.width,
                    processedBitmap.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(mutableBitmap)
                canvas.drawBitmap(processedBitmap, 0f, 0f, null)
                
                // Disegna gli effetti pose
                drawPoseEffects(canvas, outputFeature0, processedBitmap.width, processedBitmap.height)
                
                // Disegna i box urbani se attivi
                if (urbanBoxesEnabled) {
                    urbanEffects.updateBoxes(outputFeature0, processedBitmap.width, processedBitmap.height)
                    urbanEffects.drawBoxes(canvas, processedBitmap)
                }
                
                imageView.setImageBitmap(mutableBitmap)
            }
        }
    }
    
    private fun drawPoseEffects(canvas: Canvas, outputFeature0: FloatArray, w: Int, h: Int) {
        val headIdx = 0
        val headX = outputFeature0[headIdx * 3 + 1] * w
        val headY = outputFeature0[headIdx * 3] * h
        val headScore = outputFeature0[headIdx * 3 + 2]
        
        if (headScore <= 0.45) return
        
        val linePaint = Paint().apply {
            color = Color.rgb(effectColorR, effectColorG, effectColorB)
            strokeWidth = lineThickness
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        
        // Disegna linee dalla testa
        if (animatedLinesEnabled) {
            for (keypointIdx in 1 until 17) {
                val keypointX = outputFeature0[keypointIdx * 3 + 1] * w
                val keypointY = outputFeature0[keypointIdx * 3] * h
                val keypointScore = outputFeature0[keypointIdx * 3 + 2]
                
                if (keypointScore > 0.45) {
                    val numLines = (3..5).random()
                    for (lineNum in 0 until numLines) {
                        drawCurvedLine(headX, headY, keypointX, keypointY, canvas, linePaint, lineNum)
                    }
                }
            }
        }
        
        // Disegna pallini
        if (glowDotsEnabled) {
            for (x in 0 until 51 step 3) {
                val score = outputFeature0[x + 2]
                val keypointX = outputFeature0[x + 1] * w
                val keypointY = outputFeature0[x] * h
                
                if (score > 0.45) {
                    drawGlowDot(canvas, keypointX, keypointY)
                }
            }
        }
    }
    
    private fun drawCurvedLine(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        canvas: Canvas,
        paint: Paint,
        lineNum: Int
    ) {
        animationTime = System.currentTimeMillis() - animationStartTime
        
        val distance = sqrt((endX - startX).pow(2) + (endY - startY).pow(2))
        val midX = (startX + endX) / 2
        val midY = (startY + endY) / 2
        
        val dx = endX - startX
        val dy = endY - startY
        val perpX = -dy / distance
        val perpY = dx / distance
        
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f
        val toCenterX = midX - centerX
        val toCenterY = midY - centerY
        val dotProduct = perpX * toCenterX + perpY * toCenterY
        
        val outwardPerpX = if (dotProduct < 0) -perpX else perpX
        val outwardPerpY = if (dotProduct < 0) -perpY else perpY
        
        val timeInSeconds = (animationTime / 1000.0) * animationSpeed
        val lineOffset = lineNum * 0.8 + Math.random() * 0.5
        
        val slowWave = Math.sin(timeInSeconds * 1.5 + lineOffset) * 0.6
        val fastWave = Math.sin(timeInSeconds * 5.0 + startX * 0.02 + lineOffset * 2) * 0.4
        val mediumWave = Math.sin(timeInSeconds * 3.0 + (startX + startY + lineNum * 50) * 0.01) * 0.5
        val randomWave = Math.sin(timeInSeconds * 7.0 + Math.random() * Math.PI * 2) * 0.3
        val pureRandom = (Math.random() - 0.5) * 0.4
        
        val waveModulation = (slowWave + fastWave + mediumWave + randomWave + pureRandom).toFloat()
        
        val baseAmplitude = curveAmount + (lineNum * 0.05f)
        val baseCurveFactor = distance * baseAmplitude
        val amplitudePulse = 1.0f + Math.sin(timeInSeconds * 2.0 + lineOffset).toFloat() * 0.6f
        val randomAmplitude = 0.7f + Math.random().toFloat() * 0.6f
        val curveFactor = baseCurveFactor * amplitudePulse * randomAmplitude * (1f + waveModulation)
        
        val randomOffsetX = (Math.random().toFloat() - 0.5f) * distance * 0.15f
        val randomOffsetY = (Math.random().toFloat() - 0.5f) * distance * 0.15f
        
        val controlX = midX + outwardPerpX * curveFactor + randomOffsetX
        val controlY = midY + outwardPerpY * curveFactor + randomOffsetY
        
        val offset1Factor = 0.25f + Math.sin(timeInSeconds * 3.0 + lineOffset).toFloat() * 0.2f + (Math.random().toFloat() - 0.5f) * 0.15f
        val offset2Factor = 0.75f + Math.sin(timeInSeconds * 3.0 + Math.PI + lineOffset).toFloat() * 0.2f + (Math.random().toFloat() - 0.5f) * 0.15f
        
        val control1X = startX * (1 - offset1Factor) + controlX * offset1Factor
        val control1Y = startY * (1 - offset1Factor) + controlY * offset1Factor
        val control2X = endX * (1 - offset2Factor) + controlX * offset2Factor
        val control2Y = endY * (1 - offset2Factor) + controlY * offset2Factor
        
        val alphaPaint = Paint(paint)
        val alphaVariation = 0.3f + (lineNum * 0.15f) + Math.random().toFloat() * 0.3f
        alphaPaint.alpha = (255 * alphaVariation.coerceIn(0.2f, 1.0f)).toInt()
        val strokeVariation = 0.6f + Math.random().toFloat() * 0.8f
        alphaPaint.strokeWidth = paint.strokeWidth * strokeVariation
        
        val path = Path()
        path.moveTo(startX, startY)
        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY)
        canvas.drawPath(path, alphaPaint)
    }
    
    private fun drawGlowDot(canvas: Canvas, x: Float, y: Float) {
        val dotRadius = 8f
        
        val dotPaint = Paint().apply {
            color = Color.rgb(effectColorR, effectColorG, effectColorB)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val borderPaint = Paint().apply {
            color = Color.rgb(
                (effectColorR * 1.2f).toInt().coerceAtMost(255),
                (effectColorG * 1.2f).toInt().coerceAtMost(255),
                (effectColorB * 1.2f).toInt().coerceAtMost(255)
            )
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        
        canvas.drawCircle(x, y, dotRadius, dotPaint)
        canvas.drawCircle(x, y, dotRadius, borderPaint)
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
    
    private fun applyFilter(bitmap: Bitmap, filter: CameraFilter): Bitmap {
        return when (filter) {
            CameraFilter.NONE -> bitmap
            CameraFilter.BLACK_WHITE -> applyBlackWhiteFilter(bitmap)
            CameraFilter.SOBEL -> applySobelFilter(bitmap)
            CameraFilter.PIXELATED -> applyPixelateFilter(bitmap)
        }
    }
    
    private fun applyBlackWhiteFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = android.graphics.ColorMatrix()
        colorMatrix.setSaturation(0f)  // Rimuove saturazione = B&W
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun applySobelFilter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val grayPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            grayPixels[i] = gray
        }
        
        val sobelPixels = IntArray(width * height)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = grayPixels[(y - 1) * width + (x + 1)] - grayPixels[(y - 1) * width + (x - 1)] +
                         2 * grayPixels[y * width + (x + 1)] - 2 * grayPixels[y * width + (x - 1)] +
                         grayPixels[(y + 1) * width + (x + 1)] - grayPixels[(y + 1) * width + (x - 1)]
                
                val gy = grayPixels[(y + 1) * width + (x - 1)] - grayPixels[(y - 1) * width + (x - 1)] +
                         2 * grayPixels[(y + 1) * width + x] - 2 * grayPixels[(y - 1) * width + x] +
                         grayPixels[(y + 1) * width + (x + 1)] - grayPixels[(y - 1) * width + (x + 1)]
                
                val magnitude = kotlin.math.min(255, kotlin.math.sqrt((gx * gx + gy * gy).toDouble()).toInt())
                sobelPixels[y * width + x] = Color.rgb(magnitude, magnitude, magnitude)
            }
        }
        
        result.setPixels(sobelPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    private fun applyPixelateFilter(bitmap: Bitmap): Bitmap {
        val pixelSize = 15
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, bitmap.config)
        
        for (y in 0 until height step pixelSize) {
            for (x in 0 until width step pixelSize) {
                val pixel = bitmap.getPixel(x, y)
                for (dy in 0 until pixelSize) {
                    for (dx in 0 until pixelSize) {
                        val px = x + dx
                        val py = y + dy
                        if (px < width && py < height) {
                            result.setPixel(px, py, pixel)
                        }
                    }
                }
            }
        }
        return result
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
        val permissions = mutableListOf<String>()
        
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.CAMERA)
        }
        
        // Permessi storage per Android 9 e precedenti
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 101)
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

    // ====== PHOTO & VIDEO CAPTURE ======
    
    private fun capturePhoto() {
        try {
            // Step 1: Cattura bitmap - se saveWithEffects=true usa imageView (con effetti), altrimenti textureView (raw)
            val rawBitmap = if (saveWithEffects && imageView.drawable != null) {
                // Salva con effetti pose
                val width = imageView.width
                val height = imageView.height
                if (width <= 0 || height <= 0) {
                    Toast.makeText(this, "❌ Invalid view size", Toast.LENGTH_SHORT).show()
                    return
                }
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = Canvas(bmp)
                    imageView.drawable.setBounds(0, 0, width, height)
                    imageView.drawable.draw(canvas)
                }
            } else {
                // Salva foto naturale dalla camera (senza effetti verdi)
                textureView.bitmap
            }
            
            if (rawBitmap == null) {
                Toast.makeText(this, "❌ Error capturing photo", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Step 2: Applica miglioramenti qualità (come fa il telefono)
            val enhancedBitmap = enhancePhotoQuality(rawBitmap)
            
            // Step 3: Salva con qualità massima (100%)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "UrbanCam_$timestamp.jpg"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - usa MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/UrbanCamera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { stream ->
                        // Salva a qualità massima (100)
                        enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                    
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(it, values, null, null)
                    
                    Toast.makeText(this, "📸 Photo saved!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 e precedenti
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val urbanCameraDir = File(picturesDir, "UrbanCamera")
                if (!urbanCameraDir.exists()) {
                    urbanCameraDir.mkdirs()
                }
                
                val file = File(urbanCameraDir, filename)
                FileOutputStream(file).use { stream ->
                    enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                
                // Aggiungi alla galleria
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                
                Toast.makeText(this, "📸 Photo saved!", Toast.LENGTH_SHORT).show()
            }
            
            // Cleanup
            rawBitmap.recycle()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Migliora la qualità della foto applicando tecniche usate dalle app camera native:
     * - Sharpening (nitidezza)
     * - Contrast enhancement (migliora contrasto)
     * - Saturation boost (colori più vividi)
     * - Noise reduction (riduce rumore)
     */
    private fun enhancePhotoQuality(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Canvas per applicare effetti
        val canvas = Canvas(enhanced)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        
        // 1. Sharpening con ColorMatrix
        val sharpenMatrix = ColorMatrix(floatArrayOf(
            0f, -1f, 0f, 0f, 0f,
            -1f, 5f, -1f, 0f, 0f,
            0f, -1f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // 2. Contrast enhancement (aumenta contrasto del 20%)
        val contrastMatrix = ColorMatrix()
        val contrast = 1.2f
        val translate = (-.5f * contrast + .5f) * 255f
        contrastMatrix.set(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // 3. Saturation boost (colori più vividi del 15%)
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(1.15f)
        
        // Combina tutti i miglioramenti
        val finalMatrix = ColorMatrix()
        finalMatrix.postConcat(sharpenMatrix)
        finalMatrix.postConcat(contrastMatrix)
        finalMatrix.postConcat(saturationMatrix)
        
        paint.colorFilter = ColorMatrixColorFilter(finalMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        
        return enhanced
    }
    
    private fun startVideoRecording() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "UrbanCam_$timestamp.mp4"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - usa MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/UrbanCamera")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                
                val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    val fileDescriptor = contentResolver.openFileDescriptor(it, "w")
                    fileDescriptor?.let { fd ->
                        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            MediaRecorder(this)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaRecorder()
                        }
                        
                        mediaRecorder?.apply {
                            setVideoSource(MediaRecorder.VideoSource.SURFACE)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setOutputFile(fd.fileDescriptor)
                            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                            setVideoSize(1280, 720)
                            setVideoFrameRate(30)
                            setVideoEncodingBitRate(10000000)
                            
                            prepare()
                            start()
                        }
                        
                        videoFilePath = it.toString()
                        isRecording = true
                        recordingStatus.visibility = View.VISIBLE
                        Toast.makeText(this, "🎥 Recording...", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Android 9 e precedenti
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val urbanCameraDir = File(moviesDir, "UrbanCamera")
                if (!urbanCameraDir.exists()) {
                    urbanCameraDir.mkdirs()
                }
                
                val file = File(urbanCameraDir, filename)
                videoFilePath = file.absolutePath
                
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                
                mediaRecorder?.apply {
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(file.absolutePath)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setVideoSize(1280, 720)
                    setVideoFrameRate(30)
                    setVideoEncodingBitRate(10000000)
                    
                    prepare()
                    start()
                }
                
                isRecording = true
                recordingStatus.visibility = View.VISIBLE
                Toast.makeText(this, "🎥 Recording...", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Recording error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopVideoRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false
            recordingStatus.visibility = View.GONE
            
            // Finalizza il file in MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && videoFilePath != null) {
                val uri = android.net.Uri.parse(videoFilePath)
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            } else if (videoFilePath != null) {
                // Aggiungi alla galleria per Android 9 e precedenti
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DATA, videoFilePath)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                }
                contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            }
            
            Toast.makeText(this, "✅ Video saved!", Toast.LENGTH_SHORT).show()
            videoFilePath = null
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Error saving video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopVideoRecording()
        }
        model.close()
    }
}
