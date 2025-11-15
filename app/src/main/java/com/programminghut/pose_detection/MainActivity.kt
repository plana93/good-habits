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
    var selectedCameraIndex = -1
    private var isFrontCamera = false
    
    // *** NUOVE VARIABILI PER RECORDING ***
    private var recordSkeleton = false
    private var poseLogger: PoseLogger? = null
    private lateinit var btnExitAndCopy: Button
    
    // *** SQUAT COUNTER PER PERSISTENZA ***
    private lateinit var squatCounter: SquatCounter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCameraIndex = intent.getIntExtra("cameraIndex", -1)
        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)
        recordSkeleton = intent.getBooleanExtra("RECORD_SKELETON", false)

        setContentView(R.layout.activity_main)
        get_permissions()

        // *** INIZIALIZZA SQUAT COUNTER PER PERSISTENZA ***
        squatCounter = SquatCounter(this)
        
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
        count_repetition = 0
        
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
                if (hasSkeletonDetected(outputFeature0, threshold_pose)) {
                    consecutiveFramesWithPose++
                    if (consecutiveFramesWithPose >= K) {
                        onPoseDetected(
                            outputFeature0,
                            outputFeature0_base,
                            outputFeature0_squat
                        )
                        step1Complete = true
                        start_to_monitoring = true
                    }
                } else {
                    consecutiveFramesWithPose = 0
                }

                val canvas = textureView.lockCanvas()
                canvas?.let {
                    drawGreenBorder(it)
                    textureView.unlockCanvasAndPost(it)
                }
            }

            private fun startStep2(outputFeature0: FloatArray) {
                squatMetric_current = computeSquatMetric(outputFeature0)
                showToast("Start step 2")
                if (detectedSquat()) {
                    start_to_monitoring = false
                    count_repetition++
                    
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
                squatMetric_current = computeSquatMetric(outputFeature0)
                showToast("Start step 3")

                if (detectedOriginalPosition()  ) {
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
                    findViewById<View>(R.id.topBorder).setBackgroundColor(color)
                    findViewById<View>(R.id.bottomBorder).setBackgroundColor(color)
                    findViewById<View>(R.id.leftBorder).setBackgroundColor(color)
                    findViewById<View>(R.id.rightBorder).setBackgroundColor(color)
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
                val emojiResources = getEmojiResources()

                for (x in 0 until 50 step 3) {
                    val score = outputFeature0[x + 2]
                    val keypointX = outputFeature0[x + 1] * w
                    val keypointY = outputFeature0[x] * h

                    if (score > threshold_pose) {
                        drawEmojiOnCanvas(keypointX, keypointY, x, w, h, emojiResources, canvas)
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
                linePaint.color = Color.RED
                linePaint.strokeWidth = 8f
                linePaint.style = Paint.Style.STROKE
                linePaint.alpha = 255
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

            private fun getEmojiResources(): Array<Int> {
                return arrayOf(
                    R.drawable.dot_small, //nose_emoji,
                    R.drawable.dot_small,  //eye_emoji,
                    R.drawable.dot_small, //eye_emoji,
                    R.drawable.dot_small, //ear_emoji,
                    R.drawable.dot_small, //left_ear_emoji,
                    R.drawable.dot_small, //dot,
                    R.drawable.dot_small, //dot,
                    R.drawable.dot_small, //smile_emoji,
                    R.drawable.dot_small, //smile_emoji,
                    R.drawable.dot_small, //smile_emoji,
                    R.drawable.dot_small, //smile_emoji,
                    R.drawable.dot_small, //smile_emoji,
                    R.drawable.dot_small, //smile_emoji,
                    R.drawable.dot_small, //dot,
                    R.drawable.dot_small, //dot,
                    R.drawable.dot_small, //dot_white,
                    R.drawable.dot_small, //dot_white
                )
            }

            private fun drawEmojiOnCanvas(
                keypointX: Float,
                keypointY: Float,
                x: Int,
                w: Int,
                h: Int,
                emojiResources: Array<Int>, canvas: Canvas
            ) {
                val emojiDrawable =
                    ContextCompat.getDrawable(this@MainActivity, emojiResources[x / 3])

                emojiDrawable?.setBounds(
                    (keypointX - 50).toInt(), (keypointY - 50).toInt(),
                    (keypointX + 50).toInt(), (keypointY + 50).toInt()
                )
                emojiDrawable?.draw(canvas)
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
                val connections = arrayOf(
                    Pair(0, 1), Pair(0, 2), Pair(1, 3), Pair(2, 4),
                    Pair(0, 5), Pair(0, 6), Pair(5, 7), Pair(6, 8),
                    Pair(7, 9), Pair(8, 10), Pair(5, 11), Pair(6, 12),
                    Pair(11, 13), Pair(12, 14), Pair(13, 15), Pair(14, 16)
                )

                for (connection in connections) {
                    val (startIdx, endIdx) = connection
                    val startX = outputFeature0[startIdx * 3 + 1] * w
                    val startY = outputFeature0[startIdx * 3] * h
                    val endX = outputFeature0[endIdx * 3 + 1] * w
                    val endY = outputFeature0[endIdx * 3] * h

                    if (score > 0.45 && outputFeature0[startIdx * 3 + 2] > 0.45 && outputFeature0[endIdx * 3 + 2] > 0.45) {
                        canvas.drawLine(startX, startY, endX, endY, linePaint)
                    }
                }
            }

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

    override fun onDestroy() {
        super.onDestroy()
        
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