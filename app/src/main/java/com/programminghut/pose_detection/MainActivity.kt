package com.programminghut.pose_detection

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.view.View
import android.widget.Button
import android.content.Intent
import android.widget.Toast
import java.util.Collections.copy
import kotlin.math.abs
import kotlin.Pair
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SquatMetric(
    var distance_shoulderKneeLeft: Double,
    var check_shoulderKneeLeft: Boolean,
    var distance_shoulderKneeRight: Double,
    var check_shoulderKneeRight: Boolean,
    var footLeft : Pair<Float, Float>,
    var footRight : Pair<Float, Float>,
    var check_foots: Pair<Float,Float>

) {
    // Costruttore di copia
    constructor(other: SquatMetric) : this(
        other.distance_shoulderKneeLeft,
        other.check_shoulderKneeLeft,
        other.distance_shoulderKneeRight,
        other.check_shoulderKneeRight,
        other.footLeft,
        other.footRight,
        other.check_foots,
    )
}

/**
 * Data class per tracciare ogni singola ripetizione durante la sessione
 */
data class RepTrackingData(
    val repNumber: Int,
    val timestamp: Long,
    val depthScore: Float,      // Calcol based on metrics
    val formScore: Float,        // Based on symmetry and posture
    val speed: Float,            // Time taken for this rep
    val confidence: Float = 1.0f // Pose detection confidence
)

class MainActivity : AppCompatActivity() {

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
    lateinit var activationProgressText: TextView
    val threshold_pose = 0.45
    var count_repetition = 0
    var consecutiveFramesWithPose = 0
    val K = 5
    lateinit var outputFeature0_base_position: FloatArray
    lateinit var outputFeature0_squat_position: FloatArray

    lateinit var squatMetric_current: SquatMetric
    lateinit var squatMetric_base: SquatMetric
    lateinit var squatMetric_squat: SquatMetric

    var start_to_monitoring = false
    var step1Complete = false
    
    // *** VARIABILI PER ANIMAZIONE FLUIDA DELLE CURVE ***
    private var animationTime = 0L
    private val animationStartTime = System.currentTimeMillis()
    
    // *** BUFFER PER SMOOTHING TEMPORALE - Riduce flickering ***
    private val poseBuffer = mutableListOf<FloatArray>()
    private val BUFFER_SIZE = 3  // Media sugli ultimi 3 frame per stabilità
    private val ESSENTIAL_KEYPOINTS = listOf(5, 6, 11, 12, 13, 14, 15, 16)  // Spalle, anche, ginocchia, caviglie
    private var stabilityCounter = 0
    private val STABILITY_THRESHOLD = 8  // Richiedi 8 frame stabili invece di tutti i punti perfetti
    var selectedCameraIndex = -1
    private var isFrontCamera = false
    
    // *** NUOVE VARIABILI PER RECORDING ***
    private var recordSkeleton = false
    private var poseLogger: PoseLogger? = null
    private lateinit var btnExitAndCopy: Button
    
    // *** SQUAT COUNTER PER PERSISTENZA ***
    private lateinit var squatCounter: SquatCounter
    
    // *** SESSION TRACKING PER DATABASE (PHASE 1) ***
    private val sessionReps = mutableListOf<RepTrackingData>()
    private var sessionStartTime: Long = 0
    private var lastRepTime: Long = 0
    
    // Database components
    private lateinit var sessionRepository: com.programminghut.pose_detection.data.repository.SessionRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCameraIndex = intent.getIntExtra("cameraIndex", -1)
        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)
        recordSkeleton = intent.getBooleanExtra("RECORD_SKELETON", false)

        setContentView(R.layout.activity_main)
        get_permissions()

        // *** INIZIALIZZA SQUAT COUNTER PER PERSISTENZA ***
        squatCounter = SquatCounter(this)
        
        // *** INIZIALIZZA DATABASE E REPOSITORY (PHASE 1) ***
        val database = com.programminghut.pose_detection.data.database.AppDatabase.getDatabase(applicationContext)
        sessionRepository = com.programminghut.pose_detection.data.repository.SessionRepository(
            database.sessionDao(),
            database.repDao()
        )
        
        // Inizia tracking della sessione
        sessionStartTime = System.currentTimeMillis()
        sessionReps.clear()
        
        // *** GESTIONE MODALITÀ RECORDING ***
        btnExitAndCopy = findViewById(R.id.btn_exit_and_copy)
        if (recordSkeleton) {
            // Modalità registrazione: inizializza logger e mostra bottone exit
            try {
                poseLogger = PoseLogger(this)
                btnExitAndCopy.visibility = View.VISIBLE
                numberTextView.visibility = View.GONE  // Nascondi il contatore ripetizioni
                Toast.makeText(this, "Modalità Recording Attiva", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Errore inizializzazione logger", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Modalità normale: nascondi bottone exit
            btnExitAndCopy.visibility = View.GONE
        }
        
        // Collegamento bottone EXIT
        btnExitAndCopy.setOnClickListener {
            poseLogger?.copyFileToClipboardAndExit(this@MainActivity)
        }

        imageProcessor =
            ImageProcessor.Builder().add(ResizeOp(192, 192, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = LiteModelMovenetSingleposeLightningTfliteFloat164.newInstance(this)
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        paint.setColor(Color.YELLOW)



        numberTextView = findViewById(R.id.numberTextView)
        activationProgressText = findViewById(R.id.activationProgress)
        count_repetition = 0
        
        // Mostra l'indicatore di progresso inizialmente (solo in modalità squat, non recording)
        if (!recordSkeleton) {
            activationProgressText.visibility = View.VISIBLE
            updateActivationProgress(0)
        }
        
        // *** IMPOSTA DIMENSIONE TESTO RIDOTTA (1/25 DELLO SCHERMO) ***
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val textSizePx = screenHeight / 25f  // Ridotto da 15 a 25 per box più piccola
        numberTextView.textSize = textSizePx / displayMetrics.density // Converti px in sp
        
        // *** CARICA IL TOTALE DEGLI SQUAT SALVATI ***
        if (!recordSkeleton) {
            val totalSquats = squatCounter.getTotalSquats()
            // Mostra inizialmente con etichette
            numberTextView.text = "Sessione: $count_repetition\nTotale: $totalSquats"
            Toast.makeText(this, "Totale squat caricati: $totalSquats", Toast.LENGTH_SHORT).show()
            
            // Dopo 3 secondi, passa a mostrare solo i numeri
            Handler(mainLooper).postDelayed({
                updateSquatDisplay(compact = true)
            }, 3000)
        } else {
            numberTextView.text = count_repetition.toString()
        }
        numberTextView.setTextColor(Color.WHITE)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {


            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
                // Configura aspect ratio corretto per evitare allungamento
                CameraAspectRatioHelper.configureTextureView16x9(textureView, isFrontCamera)
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                // Riconfigura quando cambia dimensione
                CameraAspectRatioHelper.configureTextureView16x9(textureView, isFrontCamera)
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                val tensorImage = preprocessImage(bitmap)
                val outputFeature0 = runPoseDetection(tensorImage)
                
                // *** LOGGING IN MODALITÀ RECORDING ***
                if (recordSkeleton && poseLogger != null) {
                    poseLogger?.logFrame(outputFeature0)
                }
                
                // *** MODALITÀ RECORDING: salta la logica di squat e mostra solo lo scheletro ***
                if (recordSkeleton) {
                    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    drawPoseOnBitmap(mutableBitmap, outputFeature0, threshold_pose)
                    imageView.setImageBitmap(mutableBitmap)
                    return
                }
                
                if (!step1Complete) {
                    outputFeature0_base_position = outputFeature0
                    outputFeature0_squat_position = outputFeature0
                    outputFeature0_base_position = intent.getFloatArrayExtra("base_position")!!
                    outputFeature0_squat_position = intent.getFloatArrayExtra("squat_position")!!
                    startStep1(
                        outputFeature0,
                        outputFeature0_base_position,
                        outputFeature0_squat_position,
                        threshold_pose,
                        textureView
                    )
                } else {
                    checkAllBody()
                    if (start_to_monitoring) {
                        startStep2(outputFeature0)
                    } else {
                        startStep3(outputFeature0)
                    }
                }

                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                drawPoseOnBitmap(mutableBitmap, outputFeature0, threshold_pose)
                imageView.setImageBitmap(mutableBitmap)
            }

            private fun startStep1(
                outputFeature0: FloatArray,
                outputFeature0_base: FloatArray,
                outputFeature0_squat: FloatArray,
                threshold_pose: Double,
                textureView: TextureView,
            ) {
                // Aggiungi al buffer per smoothing temporale
                val smoothedPose = addToBufferAndSmooth(outputFeature0)
                
                // Usa la versione smoothed per detection più stabile
                if (hasEssentialKeypointsDetected(smoothedPose, threshold_pose)) {
                    stabilityCounter++
                    consecutiveFramesWithPose++
                    
                    // Aggiorna indicatore di progresso
                    updateActivationProgress(stabilityCounter)
                    
                    // Feedback visivo progressivo: da giallo a verde
                    val progress = stabilityCounter.toFloat() / STABILITY_THRESHOLD
                    val color = if (progress < 0.5f) {
                        // Giallo -> Arancione
                        interpolateColor(Color.YELLOW, Color.rgb(255, 165, 0), progress * 2)
                    } else {
                        // Arancione -> Verde
                        interpolateColor(Color.rgb(255, 165, 0), Color.GREEN, (progress - 0.5f) * 2)
                    }
                    colorScreenBorders(color)
                    
                    if (stabilityCounter >= STABILITY_THRESHOLD) {
                        onPoseDetected(
                            smoothedPose,  // Usa la versione smoothed come riferimento
                            outputFeature0_base,
                            outputFeature0_squat
                        )
                        step1Complete = true
                        start_to_monitoring = true
                        
                        // Nascondi indicatore di progresso
                        runOnUiThread {
                            activationProgressText.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "✅ Posizione rilevata! Inizia squat", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Reset con decay graduale per evitare reset troppo aggressivi
                    if (stabilityCounter > 0) {
                        stabilityCounter -= 1  // Decrementa di 1 invece di azzerare
                    }
                    consecutiveFramesWithPose = 0
                    updateActivationProgress(stabilityCounter)
                    colorScreenBorders(Color.RED)
                }
            }

            private fun startStep2(outputFeature0: FloatArray) {
                // Usa smoothing anche durante il conteggio per maggiore stabilità
                val smoothedPose = if (poseBuffer.size >= 2) {
                    addToBufferAndSmooth(outputFeature0)
                } else {
                    addToBufferAndSmooth(outputFeature0)
                    outputFeature0
                }
                
                squatMetric_current = computeSquatMetric(smoothedPose)
                
                if (detectedSquat()) {
                    start_to_monitoring = false
                    count_repetition++
                    
                    // *** TRACCIA I DATI DEL REP (PHASE 1) ***
                    val currentTime = System.currentTimeMillis()
                    val repSpeed = if (lastRepTime > 0) {
                        (currentTime - lastRepTime) / 1000f // seconds
                    } else {
                        0f
                    }
                    
                    // Calcola scores basati sulle metriche
                    val depthScore = calculateDepthScore(squatMetric_current, squatMetric_squat)
                    val formScore = calculateFormScore(squatMetric_current)
                    
                    // Aggiungi rep alla lista della sessione
                    sessionReps.add(
                        RepTrackingData(
                            repNumber = count_repetition,
                            timestamp = currentTime,
                            depthScore = depthScore,
                            formScore = formScore,
                            speed = repSpeed,
                            confidence = 1.0f
                        )
                    )
                    
                    lastRepTime = currentTime
                    Log.d("MainActivity", "Rep $count_repetition tracked: depth=$depthScore, form=$formScore, speed=$repSpeed")
                    
                    // *** INCREMENTA E SALVA IL TOTALE DEGLI SQUAT ***
                    if (!recordSkeleton) {
                        squatCounter.incrementSquat()
                        updateSquatDisplay(compact = true)
                        // Animazione zoom quando completi uno squat
                        animateSquatCounter()
                    } else {
                        numberTextView.text = count_repetition.toString()
                    }
                }

            }

            private fun startStep3(outputFeature0: FloatArray) {
                // Usa smoothing anche qui per transizione più fluida
                val smoothedPose = if (poseBuffer.size >= 2) {
                    addToBufferAndSmooth(outputFeature0)
                } else {
                    addToBufferAndSmooth(outputFeature0)
                    outputFeature0
                }
                
                squatMetric_current = computeSquatMetric(smoothedPose)

                if (detectedOriginalPosition()) {
                    start_to_monitoring = true
                }

            }

            private fun detectedSquat(): Boolean {
                var shoulderKneeLeft = squatMetric_current.distance_shoulderKneeLeft
                var check_shoulderKneeLeft = squatMetric_current.check_shoulderKneeLeft
                var shoulderKneeRight = squatMetric_current.distance_shoulderKneeRight
                var check_shoulderKneeRight = squatMetric_current.check_shoulderKneeRight
                var footLeft_y = abs(squatMetric_current.footLeft.second)
                var footRight_y = abs(squatMetric_current.footRight.second)


                var condition = (
                        (abs(shoulderKneeLeft) <= (abs(squatMetric_squat.distance_shoulderKneeLeft) + 0.02)
                        && check_shoulderKneeLeft)
                                ||
                        (abs(shoulderKneeRight) <= (abs(squatMetric_squat.distance_shoulderKneeRight) + 0.02)
                        && check_shoulderKneeRight)
                        )

                var condition_foot =  (footLeft_y <= (abs(footRight_y+0.02))) && (footLeft_y >= (abs(footRight_y-0.02)))

                return condition && condition_foot
            }

            private fun detectedOriginalPosition(): Boolean {
                var shoulderKneeLeft = squatMetric_current.distance_shoulderKneeLeft
                var check_shoulderKneeLeft = squatMetric_current.check_shoulderKneeLeft
                var shoulderKneeRight = squatMetric_current.distance_shoulderKneeRight
                var check_shoulderKneeRight = squatMetric_current.check_shoulderKneeRight
                var footLeft_y = abs(squatMetric_current.footLeft.second)
                var footRight_y = abs(squatMetric_current.footRight.second)

                var condition = (
                        (abs(shoulderKneeLeft) <= (abs(squatMetric_base.distance_shoulderKneeLeft) + 0.1) &&
                        abs(shoulderKneeLeft) >= (abs(squatMetric_base.distance_shoulderKneeLeft) - 0.01) &&
                        check_shoulderKneeLeft )
                                ||
                                (abs(shoulderKneeRight) <= (abs(squatMetric_base.distance_shoulderKneeRight) + 0.1) &&
                        abs(shoulderKneeRight) >= (abs(squatMetric_base.distance_shoulderKneeRight) - 0.01) &&
                        check_shoulderKneeRight)
                        )
                var condition_foot =  (footLeft_y <= (abs(footRight_y+0.02))) && (footLeft_y >= (abs(footRight_y-0.02)))

                return condition && condition_foot
            }

            private fun checkAllBody() {
                var body_score = (squatMetric_current.check_foots.first>0.2 &&
                        squatMetric_current.check_foots.second>0.2 &&
                        squatMetric_current.check_shoulderKneeLeft &&
                        squatMetric_current.check_shoulderKneeRight)
                if (body_score) {
                    step1Complete = true
                    colorScreenBorders(Color.GREEN)
                } else{
                    step1Complete = false
                    colorScreenBorders(Color.RED)
                }
            }

            private fun hasSkeletonDetected(
                outputFeature0: FloatArray,
                threshold_pose: Double
            ): Boolean {
                // Verifica se tutti i keypoints hanno uno score maggiore della soglia
                for (i in 0 until 50 step 3) {
                    val score = outputFeature0[i + 2]
                    if (score <= threshold_pose) {
                        return false  // Se uno qualsiasi ha uno score inferiore alla soglia, restituisci false
                    }
                }
                return true  // Tutti i keypoints hanno uno score maggiore della soglia
            }
            
            /**
             * Versione rilassata: verifica solo i keypoints essenziali per squat
             * Spalle (5,6), Anche (11,12), Ginocchia (13,14), Caviglie (15,16)
             */
            private fun hasEssentialKeypointsDetected(
                outputFeature0: FloatArray,
                threshold_pose: Double
            ): Boolean {
                var detectedCount = 0
                val minRequired = (ESSENTIAL_KEYPOINTS.size * 0.75).toInt()  // Richiedi 75% dei punti essenziali
                
                for (keypointIdx in ESSENTIAL_KEYPOINTS) {
                    val score = outputFeature0[keypointIdx * 3 + 2]
                    if (score > threshold_pose) {
                        detectedCount++
                    }
                }
                
                return detectedCount >= minRequired
            }
            
            /**
             * Aggiunge il frame corrente al buffer e restituisce la media smoothed
             * per ridurre il flickering
             */
            private fun addToBufferAndSmooth(outputFeature0: FloatArray): FloatArray {
                // Aggiungi copia del frame corrente al buffer
                poseBuffer.add(outputFeature0.copyOf())
                
                // Mantieni solo gli ultimi BUFFER_SIZE frame
                if (poseBuffer.size > BUFFER_SIZE) {
                    poseBuffer.removeAt(0)
                }
                
                // Se abbiamo meno di 2 frame, restituisci il frame corrente
                if (poseBuffer.size < 2) {
                    return outputFeature0
                }
                
                // Calcola la media mobile dei keypoints
                val smoothed = FloatArray(outputFeature0.size)
                for (i in smoothed.indices) {
                    var sum = 0f
                    for (frame in poseBuffer) {
                        sum += frame[i]
                    }
                    smoothed[i] = sum / poseBuffer.size
                }
                
                return smoothed
            }
            
            /**
             * Interpola tra due colori per feedback visivo progressivo
             */
            private fun interpolateColor(startColor: Int, endColor: Int, fraction: Float): Int {
                val clampedFraction = fraction.coerceIn(0f, 1f)
                
                val startR = Color.red(startColor)
                val startG = Color.green(startColor)
                val startB = Color.blue(startColor)
                
                val endR = Color.red(endColor)
                val endG = Color.green(endColor)
                val endB = Color.blue(endColor)
                
                val r = (startR + (endR - startR) * clampedFraction).toInt()
                val g = (startG + (endG - startG) * clampedFraction).toInt()
                val b = (startB + (endB - startB) * clampedFraction).toInt()
                
                return Color.rgb(r, g, b)
            }

            private fun showToast(message: String) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }

            private fun onPoseDetected(
                outputFeature0: FloatArray,
                outputFeature0_base: FloatArray,
                outputFeature0_squat: FloatArray
            ) {
                colorScreenBorders(Color.GREEN)
                start_to_monitoring = true
                //showToast("Pose detected for $K consecutive frames!")
                squatMetric_current = computeSquatMetric(outputFeature0)
                squatMetric_base = computeSquatMetric(outputFeature0_base)
                squatMetric_squat = computeSquatMetric(outputFeature0_squat)
                scaleReferencePositionWithRespectCurrent()

            }

            private fun computeSquatMetric(outputFeature0: FloatArray): SquatMetric {
                //take the vector of interest
                val shoulderLeft_position = 6
                val shoulderRight_position = 5
                val hipLeft_position = 11
                val hipRight_position = 12
                val kneeLeft_position = 14
                val kneeRight_position = 13
                val footLeft_position = 16
                val footRight_position = 15

                var position = shoulderLeft_position
                val shoulderLeft_x = outputFeature0[position * 3 + 1]
                val shoulderLeft_y = outputFeature0[position * 3 + 0]
                val shoulderLeft_score = outputFeature0[position * 3 + 2]

                position = kneeLeft_position
                val kneeLeft_x = outputFeature0[position * 3 + 1]
                val kneeLeft_y = outputFeature0[position * 3 + 0]
                val kneeLeft_score = outputFeature0[position * 3 + 2]


                position = shoulderRight_position
                val shoulderRight_x = outputFeature0[position * 3 + 1]
                val shoulderRight_y = outputFeature0[position * 3 + 0]
                val shoulderRight_score = outputFeature0[position * 3 + 2]


                position = kneeRight_position
                val kneeRight_x = outputFeature0[position * 3 + 1]
                val kneeRight_y = outputFeature0[position * 3 + 0]
                val kneeRight_score = outputFeature0[position * 3 + 2]

                position = footLeft_position
                val footLeft_x = outputFeature0[position * 3 + 1]
                val footLeft_y = outputFeature0[position * 3 + 0]
                val footLeft_score = outputFeature0[position * 3 + 2]

                position = footRight_position
                val footRight_x = outputFeature0[position * 3 + 1]
                val footRight_y = outputFeature0[position * 3 + 0]
                val footRight_score = outputFeature0[position * 3 + 2]

                var shoulderKneeLeft = calculateDistance(
                    x1 = shoulderLeft_x,
                    y1 = shoulderLeft_y,
                    x2 = kneeLeft_x,
                    y2 = kneeLeft_y,
                )
                var shoulderKneeLeft_score = (shoulderLeft_score > 0.2) && (kneeLeft_score > 0.2)

                var shoulderKneeRight= calculateDistance(
                    x1 = shoulderRight_x,
                    y1 = shoulderRight_y,
                    x2 = kneeRight_x,
                    y2 = kneeRight_y,
                )
                var shoulderKneeRight_score = (shoulderRight_score > 0.2) && (kneeRight_score > 0.2)

                return SquatMetric(
                    distance_shoulderKneeLeft = shoulderKneeLeft,
                    check_shoulderKneeLeft = shoulderKneeLeft_score,
                    distance_shoulderKneeRight = shoulderKneeRight,
                    check_shoulderKneeRight = shoulderKneeRight_score,
                    footLeft = Pair(footLeft_x, footLeft_y),
                    footRight = Pair(footRight_x, footRight_y),
                    check_foots = Pair(footLeft_score, footRight_score)
                )

            }

            private fun scaleReferencePositionWithRespectCurrent(){
                var  scale_value = 1.0
                scale_value = squatMetric_current.distance_shoulderKneeLeft.div(squatMetric_base.distance_shoulderKneeLeft)
                squatMetric_base.distance_shoulderKneeLeft = squatMetric_base.distance_shoulderKneeLeft * scale_value
                squatMetric_squat.distance_shoulderKneeLeft = squatMetric_squat.distance_shoulderKneeLeft * scale_value

                scale_value = squatMetric_current.distance_shoulderKneeRight / squatMetric_base.distance_shoulderKneeRight
                squatMetric_base.distance_shoulderKneeRight = squatMetric_base.distance_shoulderKneeRight * scale_value
                squatMetric_squat.distance_shoulderKneeRight = squatMetric_squat.distance_shoulderKneeRight * scale_value

            }

            private fun colorScreenBorders(color: Int) {
                runOnUiThread {
                    // Usa drawable arrotondato per i bordi
                    val borderDrawable = android.graphics.drawable.GradientDrawable()
                    borderDrawable.setColor(color)
                    borderDrawable.cornerRadius = 16f  // Bordi arrotondati
                    
                    findViewById<View>(R.id.topBorder).background = borderDrawable
                    findViewById<View>(R.id.bottomBorder).background = borderDrawable
                    findViewById<View>(R.id.leftBorder).background = borderDrawable
                    findViewById<View>(R.id.rightBorder).background = borderDrawable
                }
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

            private fun drawPoseOnBitmap(
                bitmap: Bitmap,
                outputFeature0: FloatArray,
                threshold_pose: Double
            ) {
                val canvas = Canvas(bitmap)
                val h = bitmap.height
                val w = bitmap.width

                val linePaint = createLinePaint()

                for (x in 0 until 50 step 3) {
                    val score = outputFeature0[x + 2]
                    val keypointX = outputFeature0[x + 1] * w
                    val keypointY = outputFeature0[x] * h

                    if (score > threshold_pose) {
                        drawEmojiOnCanvas(keypointX, keypointY, x, w, h, canvas)
                        connectKeypoints(score, x, w, h, linePaint, canvas, outputFeature0)
                    }
                }
                // poi usi la condizione vera
                if(step1Complete) {
                    val emojiDrawable =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.foot_left)

                    emojiDrawable?.setBounds(
                        ((squatMetric_base.footLeft.first * w) - 50).toInt(), ((squatMetric_base.footLeft.second * h) - 50).toInt(),
                        ((squatMetric_base.footLeft.first * w) + 50).toInt(), ((squatMetric_base.footLeft.second * h) + 50).toInt()
                    )
                    emojiDrawable?.draw(canvas)
                }

            }

            private fun createLinePaint(): Paint {
                val linePaint = Paint()
                linePaint.color = Color.rgb(57, 255, 20)  // Verde fluo come i pallini (#39FF14)
                linePaint.strokeWidth = 2f  // Più sottili per eleganza
                linePaint.style = Paint.Style.STROKE
                linePaint.alpha = 255
                linePaint.isAntiAlias = true  // Curve smooth
                linePaint.strokeCap = Paint.Cap.ROUND  // Estremità arrotondate
                return linePaint
            }

            private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
                return Math.sqrt(
                    Math.pow(
                        (x2 - x1).toDouble(),
                        2.0
                    ) + Math.pow((y2 - y1).toDouble(), 2.0)
                )
            }

            private fun drawGreenBorder(canvas: Canvas) {
                val greenPaint = Paint()
                greenPaint.color = Color.GREEN
                greenPaint.strokeWidth = 8f
                greenPaint.style = Paint.Style.STROKE

                val rect = Rect(0, 0, canvas.width, canvas.height)
                canvas.drawRect(rect, greenPaint)
            }

            private fun drawEmojiOnCanvas(
                keypointX: Float,
                keypointY: Float,
                x: Int,
                w: Int,
                h: Int,
                canvas: Canvas
            ) {
                // *** CREA PALLINO VERDE FLUO PROGRAMMATICAMENTE ***
                // Dimensione molto piccola e sottile
                val dotRadius = 8f  // Raggio ridotto (era 50px di bounds = ~25px raggio)
                
                // Paint per il pallino verde fluo
                val dotPaint = Paint().apply {
                    color = Color.rgb(57, 255, 20)  // Verde fluo brillante (#39FF14)
                    style = Paint.Style.FILL
                    isAntiAlias = true  // Bordi smooth
                }
                
                // Paint per il bordo sottile (opzionale, per dare più "punch")
                val borderPaint = Paint().apply {
                    color = Color.rgb(100, 255, 80)  // Verde più chiaro per il bordo
                    style = Paint.Style.STROKE
                    strokeWidth = 2f  // Bordo molto sottile
                    isAntiAlias = true
                }
                
                // Disegna il pallino pieno
                canvas.drawCircle(keypointX, keypointY, dotRadius, dotPaint)
                
                // Disegna il bordo sottile
                canvas.drawCircle(keypointX, keypointY, dotRadius, borderPaint)
            }

            private fun connectKeypoints(
                score: Float,
                x: Int,
                w: Int,
                h: Int,
                linePaint: Paint,
                canvas: Canvas,
                outputFeature0: FloatArray
            ) {
                // *** NUOVO DESIGN: COLLEGA TUTTI I PUNTI DALLA TESTA ***
                // Il punto 0 è il naso (centro della testa)
                val headIdx = 0
                val headX = outputFeature0[headIdx * 3 + 1] * w
                val headY = outputFeature0[headIdx * 3] * h
                val headScore = outputFeature0[headIdx * 3 + 2]
                
                // Se la testa non è visibile, non disegnare nulla
                if (headScore <= 0.45) return
                
                // *** COLLEGA LA TESTA A TUTTI GLI ALTRI 16 KEYPOINTS ***
                for (keypointIdx in 1 until 17) {  // Da 1 a 16 (escludi testa stessa)
                    val keypointX = outputFeature0[keypointIdx * 3 + 1] * w
                    val keypointY = outputFeature0[keypointIdx * 3] * h
                    val keypointScore = outputFeature0[keypointIdx * 3 + 2]
                    
                    // Disegna solo se il keypoint è visibile
                    if (keypointScore > 0.45) {
                        // *** DISEGNA LINEE MULTIPLE (3-5 LINEE) PER OGNI CONNESSIONE ***
                        val numLines = (3..5).random()  // Randomizza numero di linee
                        
                        for (lineNum in 0 until numLines) {
                            drawCurvedLine(headX, headY, keypointX, keypointY, canvas, linePaint, lineNum)
                        }
                    }
                }
            }
            
            /**
             * Disegna una linea curva tra due punti con MASSIMA FLUIDITÀ E RANDOMICITÀ AZZARDATA.
             * - Curve sempre bombate verso l'esterno
             * - Ondulazione continua basata sul tempo
             * - Movimento fluido e organico
             * - Design dinamico con variazioni casuali esagerate
             * - Linee multiple con offset casuali
             */
            private fun drawCurvedLine(
                startX: Float,
                startY: Float,
                endX: Float,
                endY: Float,
                canvas: Canvas,
                paint: Paint,
                lineNum: Int = 0  // Numero della linea per offset multipli
            ) {
                // Aggiorna il tempo dell'animazione
                animationTime = System.currentTimeMillis() - animationStartTime
                
                // Calcola la distanza tra i due punti
                val distance = Math.sqrt(
                    Math.pow((endX - startX).toDouble(), 2.0) +
                    Math.pow((endY - startY).toDouble(), 2.0)
                ).toFloat()
                
                // Calcola il punto medio
                val midX = (startX + endX) / 2
                val midY = (startY + endY) / 2
                
                // Calcola il vettore perpendicolare per la curvatura
                val dx = endX - startX
                val dy = endY - startY
                
                // Vettore perpendicolare normalizzato
                val perpX = -dy / distance
                val perpY = dx / distance
                
                // *** DETERMINA LA DIREZIONE "VERSO L'ESTERNO" ***
                val centerX = canvas.width / 2f
                val centerY = canvas.height / 2f
                
                val toCenterX = midX - centerX
                val toCenterY = midY - centerY
                
                val dotProduct = perpX * toCenterX + perpY * toCenterY
                
                val outwardPerpX = if (dotProduct < 0) -perpX else perpX
                val outwardPerpY = if (dotProduct < 0) -perpY else perpY
                
                // *** ONDULAZIONE FLUIDA CON FUNZIONI SINUSOIDALI ESTREME ***
                val timeInSeconds = animationTime / 1000.0
                
                // *** RANDOMICITÀ MOLTO PIÙ AZZARDATA ***
                // Offset casuale per ogni linea multipla
                val lineOffset = lineNum * 0.8 + Math.random() * 0.5
                
                // Onda lenta di base (respiro) - PIÙ AMPIA
                val slowWave = Math.sin(timeInSeconds * 1.5 + lineOffset) * 0.6
                
                // Onda veloce per dettagli (tremolo) - PIÙ CAOTICA
                val fastWave = Math.sin(timeInSeconds * 5.0 + startX * 0.02 + lineOffset * 2) * 0.4
                
                // Onda media per varietà - PIÙ VARIABILE
                val positionPhase = (startX + startY + lineNum * 50) * 0.01
                val mediumWave = Math.sin(timeInSeconds * 3.0 + positionPhase) * 0.5
                
                // Onda casuale extra per caos controllato
                val randomWave = Math.sin(timeInSeconds * 7.0 + Math.random() * Math.PI * 2) * 0.3
                
                // *** RANDOMICITÀ PURA AGGIUNTIVA ***
                val pureRandom = (Math.random() - 0.5) * 0.4  // ±20% randomicità pura
                
                // Combina TUTTE le onde per movimento MOLTO complesso
                val waveModulation = (slowWave + fastWave + mediumWave + randomWave + pureRandom).toFloat()
                
                // *** AMPIEZZA CURVATURA MOLTO PIÙ GRANDE E VARIABILE ***
                // Base MOLTO più ampia (35-50% invece di 25%)
                val baseAmplitude = 0.35f + (lineNum * 0.05f)  // Aumenta con ogni linea
                val baseCurveFactor = distance * baseAmplitude
                
                // Modulazione dell'ampiezza nel tempo (pulsazione FORTE)
                val amplitudePulse = 1.0f + Math.sin(timeInSeconds * 2.0 + lineOffset).toFloat() * 0.6f
                
                // Variazione casuale dell'ampiezza per ogni frame
                val randomAmplitude = 0.7f + Math.random().toFloat() * 0.6f  // 70% - 130%
                
                // Fattore finale con TUTTE le modulazioni ESTREME
                val curveFactor = baseCurveFactor * amplitudePulse * randomAmplitude * (1f + waveModulation)
                
                // *** PUNTO DI CONTROLLO PRINCIPALE CON OFFSET CASUALE ***
                val randomOffsetX = (Math.random().toFloat() - 0.5f) * distance * 0.15f
                val randomOffsetY = (Math.random().toFloat() - 0.5f) * distance * 0.15f
                
                val controlX = midX + outwardPerpX * curveFactor + randomOffsetX
                val controlY = midY + outwardPerpY * curveFactor + randomOffsetY
                
                // *** PUNTI DI CONTROLLO PER CURVA CUBICA CON VARIAZIONI ESTREME ***
                val offset1Factor = 0.25f + Math.sin(timeInSeconds * 3.0 + lineOffset).toFloat() * 0.2f + (Math.random().toFloat() - 0.5f) * 0.15f
                val offset2Factor = 0.75f + Math.sin(timeInSeconds * 3.0 + Math.PI + lineOffset).toFloat() * 0.2f + (Math.random().toFloat() - 0.5f) * 0.15f
                
                val control1X = startX * (1 - offset1Factor) + controlX * offset1Factor
                val control1Y = startY * (1 - offset1Factor) + controlY * offset1Factor
                
                val control2X = endX * (1 - offset2Factor) + controlX * offset2Factor
                val control2Y = endY * (1 - offset2Factor) + controlY * offset2Factor
                
                // *** VARIAZIONE OPACITÀ PER LINEE MULTIPLE (effetto profondità) ***
                val alphaPaint = Paint(paint)
                val alphaVariation = 0.3f + (lineNum * 0.15f) + Math.random().toFloat() * 0.3f
                alphaPaint.alpha = (255 * alphaVariation.coerceIn(0.2f, 1.0f)).toInt()
                
                // *** VARIAZIONE SPESSORE PER LINEE MULTIPLE ***
                val strokeVariation = 0.6f + Math.random().toFloat() * 0.8f
                alphaPaint.strokeWidth = paint.strokeWidth * strokeVariation
                
                // *** CREA CURVA CUBICA DI BÉZIER CAOTICA ***
                val path = Path()
                path.moveTo(startX, startY)
                path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY)
                
                // Disegna la curva fluida e caotica
                canvas.drawPath(path, alphaPaint)
            }

        }
    }
    
    /**
     * Aggiorna l'indicatore di progresso dell'attivazione
     */
    private fun updateActivationProgress(current: Int) {
        runOnUiThread {
            val filled = "⬤".repeat(current.coerceAtMost(STABILITY_THRESHOLD))
            val empty = "⬤".repeat((STABILITY_THRESHOLD - current).coerceAtLeast(0))
            val percentage = ((current.toFloat() / STABILITY_THRESHOLD) * 100).toInt()
            
            activationProgressText.text = """
                Rilevamento posizione...
                $percentage%
                $filled$empty
            """.trimIndent()
            
            // Cambia colore in base al progresso
            val textColor = when {
                current < STABILITY_THRESHOLD / 2 -> Color.YELLOW
                current < STABILITY_THRESHOLD -> Color.rgb(255, 165, 0)  // Arancione
                else -> Color.GREEN
            }
            activationProgressText.setTextColor(textColor)
        }
    }
    
    /**
     * Aggiorna il display del contatore squat
     * @param compact Se true, mostra solo numeri. Se false, mostra etichette
     */
    private fun updateSquatDisplay(compact: Boolean = true) {
        runOnUiThread {
            val totalSquats = squatCounter.getTotalSquats()
            numberTextView.text = if (compact) {
                // Formato compatto: solo numeri separati da |
                "$count_repetition | $totalSquats"
            } else {
                // Formato esteso: con etichette
                "Sessione: $count_repetition\nTotale: $totalSquats"
            }
        }
    }
    
    /**
     * Animazione zoom quando si completa uno squat
     */
    private fun animateSquatCounter() {
        runOnUiThread {
            numberTextView.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(150)
                .withEndAction {
                    numberTextView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }
    
    // ===============================================================
    // PHASE 1: SESSION TRACKING HELPER FUNCTIONS
    // ===============================================================
    
    /**
     * Calcola il depth score basato su quanto profondo è lo squat
     * Confronta la posizione corrente con la posizione squat di riferimento
     */
    private fun calculateDepthScore(current: SquatMetric, reference: SquatMetric): Float {
        val leftDepthRatio = current.distance_shoulderKneeLeft / reference.distance_shoulderKneeLeft
        val rightDepthRatio = current.distance_shoulderKneeRight / reference.distance_shoulderKneeRight
        
        // Media delle due gambe, normalizzata tra 0 e 1
        val avgDepth = ((leftDepthRatio + rightDepthRatio) / 2.0).toFloat()
        
        // Clamp tra 0 e 1
        return avgDepth.coerceIn(0f, 1f)
    }
    
    /**
     * Calcola il form score basato sulla simmetria e postura
     * Controlla se entrambe le gambe lavorano in modo simmetrico
     */
    private fun calculateFormScore(current: SquatMetric): Float {
        var score = 1.0f
        
        // Penalizza se una gamba è molto diversa dall'altra (asimmetria)
        val asymmetry = abs(current.distance_shoulderKneeLeft - current.distance_shoulderKneeRight)
        val asymmetryPenalty = (asymmetry / 100.0).toFloat().coerceIn(0f, 0.3f)
        score -= asymmetryPenalty
        
        // Penalizza se i check falliscono
        if (!current.check_shoulderKneeLeft) score -= 0.2f
        if (!current.check_shoulderKneeRight) score -= 0.2f
        
        // Clamp tra 0 e 1
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Salva la sessione di allenamento nel database
     * Chiamato in onDestroy se ci sono rep registrati
     */
    private fun saveWorkoutSession() {
        // Esegui in background per non bloccare la chiusura
        Thread {
            try {
                val endTime = System.currentTimeMillis()
                val durationSeconds = ((endTime - sessionStartTime) / 1000).toInt()
                
                // Calcola statistiche aggregate
                val avgDepth = sessionReps.map { it.depthScore }.average().toFloat()
                val avgForm = sessionReps.map { it.formScore }.average().toFloat()
                val avgSpeed = sessionReps.filter { it.speed > 0 }.map { it.speed }.average().toFloat()
                
                // Crea oggetto WorkoutSession
                val session = com.programminghut.pose_detection.data.model.WorkoutSession(
                    startTime = sessionStartTime,
                    endTime = endTime,
                    durationSeconds = durationSeconds,
                    exerciseType = "SQUAT",
                    totalReps = sessionReps.size,
                    avgDepthScore = avgDepth,
                    avgFormScore = avgForm,
                    avgSpeed = avgSpeed,
                    appVersion = packageManager.getPackageInfo(packageName, 0).versionName,
                    deviceModel = android.os.Build.MODEL
                )
                
                // Converti tracking data in RepData
                val reps = sessionReps.map { tracking ->
                    com.programminghut.pose_detection.data.model.RepData(
                        sessionId = 0, // Sarà assegnato dal database
                        repNumber = tracking.repNumber,
                        timestamp = tracking.timestamp,
                        depthScore = tracking.depthScore,
                        formScore = tracking.formScore,
                        speed = tracking.speed,
                        confidence = tracking.confidence
                    )
                }
                
                // Salva nel database usando coroutine
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val sessionId = sessionRepository.insertCompleteWorkout(session, reps)
                    Log.d("MainActivity", "Sessione salvata con successo! ID=$sessionId, Reps=${reps.size}")
                    
                    // Mostra Toast di conferma
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Sessione salvata: ${reps.size} squat!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Errore nel salvataggio sessione: ${e.message}", e)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // *** SALVA SESSIONE NEL DATABASE (PHASE 1) ***
        if (!recordSkeleton && sessionReps.isNotEmpty()) {
            saveWorkoutSession()
        }
        
        // *** SALVA IL TOTALE DEGLI SQUAT PRIMA DELLA CHIUSURA ***
        if (!recordSkeleton) {
            squatCounter.onAppClosing()
            Log.d("MainActivity", "Squat totali salvati: ${squatCounter.getTotalSquats()}")
        }
        
        model.close()
        poseLogger?.close()
    }
    
    override fun onPause() {
        super.onPause()
        // Salva anche quando l'app va in background
        if (!recordSkeleton) {
            squatCounter.saveTotalSquats()
        }
    }
    
    override fun onStop() {
        super.onStop()
        // Salva anche quando l'app viene fermata
        if (!recordSkeleton) {
            squatCounter.saveTotalSquats()
        }
    }

    @SuppressLint("MissingPermission")
    fun open_camera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[selectedCameraIndex],
            object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    var captureRequest = p0.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    var surface = Surface(textureView.surfaceTexture)
                    captureRequest.addTarget(surface)
                    p0.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                p0.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {

                            }
                        },
                        handler
                    )
                }

                override fun onDisconnected(p0: CameraDevice) {

                }

                override fun onError(p0: CameraDevice, p1: Int) {

                }
            },
            handler
        )
    }

    fun get_permissions() {
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
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) get_permissions()
    }
}