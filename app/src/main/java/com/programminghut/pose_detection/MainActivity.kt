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
import android.widget.Toast
import java.util.Collections.copy
import kotlin.math.abs

data class SquatMetric(
    var distance_shoulderKneeLeft: Double,
    var check_shoulderKneeLeft: Boolean,
    var distance_shoulderHipLeft: Double,
    var distance_shoulderHipRight: Double,
    var distance_hipKneeLeft: Double,
    var distance_hipKneeRight: Double,
) {
    // Costruttore di copia
    constructor(other: SquatMetric) : this(
        other.distance_shoulderKneeLeft,
        other.check_shoulderKneeLeft,
        other.distance_shoulderHipLeft,
        other.distance_shoulderHipRight,
        other.distance_hipKneeLeft,
        other.distance_hipKneeRight
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedCameraIndex = intent.getIntExtra("cameraIndex", -1)

        setContentView(R.layout.activity_main)
        get_permissions()

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
        numberTextView.text =
            count_repetition.toString() // Inizializza con il valore attuale del contatore
        numberTextView.setTextColor(Color.WHITE)




        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {


            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                val tensorImage = preprocessImage(bitmap)
                val outputFeature0 = runPoseDetection(tensorImage)
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
                    numberTextView.text = count_repetition.toString()
                }

            }

            private fun startStep3(outputFeature0: FloatArray) {
                squatMetric_current = computeSquatMetric(outputFeature0)
                showToast("Start step 3")

                if (detectedOriginalPosition()) {
                    start_to_monitoring = true
                }

            }

            private fun detectedSquat(): Boolean {
                val shoulderKneeLeft = squatMetric_current.distance_shoulderKneeLeft
                val check_shoulderKneeLeft = squatMetric_current.check_shoulderKneeLeft
                showToast("SQUAT ${squatMetric_squat.distance_shoulderKneeLeft}")
                return ((abs(shoulderKneeLeft) <= abs(squatMetric_squat.distance_shoulderKneeLeft) + 0.06) && check_shoulderKneeLeft)
            }

            private fun detectedOriginalPosition(): Boolean {
                val shoulderKneeLeft = squatMetric_current.distance_shoulderKneeLeft
                val check_shoulderKneeLeft = squatMetric_current.check_shoulderKneeLeft
                showToast("BASE ${squatMetric_base.distance_shoulderKneeLeft}")

                return ((abs(shoulderKneeLeft) <= abs(squatMetric_base.distance_shoulderKneeLeft) - 0.06) && check_shoulderKneeLeft)
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
                showToast("Pose detected for $K consecutive frames!")
                squatMetric_current = computeSquatMetric(outputFeature0)
                squatMetric_base = computeSquatMetric(outputFeature0_base)
                squatMetric_squat = computeSquatMetric(outputFeature0_squat)
            }

            private fun computeSquatMetric(outputFeature0: FloatArray): SquatMetric {
                //take the vector of interest
                val shoulderLeft_position = 6
                val shoulderRight_position = 8
                val hipLeft_position = 11
                val hipRight_position = 12
                val kneeLeft_position = 14
                val kneeRight_position = 15

                var position = shoulderLeft_position
                val shoulderLeft_x = outputFeature0[position * 3 + 1]
                val shoulderLeft_y = outputFeature0[position * 3 + 0]
                position = shoulderRight_position
                val shoulderRight_x = outputFeature0[position * 3 + 1]
                val shoulderRight_y = outputFeature0[position * 3 + 0]
                position = hipLeft_position
                val hipLeft_x = outputFeature0[position * 3 + 1]
                val hipLeft_y = outputFeature0[position * 3 + 0]
                position = hipRight_position
                val hipRight_x = outputFeature0[position * 3 + 1]
                val hipRight_y = outputFeature0[position * 3 + 0]
                position = kneeLeft_position
                val kneeLeft_x = outputFeature0[position * 3 + 1]
                val kneeLeft_y = outputFeature0[position * 3 + 0]
                position = kneeRight_position
                val kneeRight_x = outputFeature0[position * 3 + 1]
                val kneeRight_y = outputFeature0[position * 3 + 0]


                val shoulderKneeLeft = calculateDistance(
                    x1 = shoulderLeft_x,
                    y1 = shoulderLeft_y,
                    x2 = kneeLeft_x,
                    y2 = kneeLeft_y,
                )

                val shoulderHipLeft = calculateDistance(
                    x1 = shoulderLeft_x,
                    y1 = shoulderLeft_y,
                    x2 = hipLeft_x,
                    y2 = hipLeft_y,
                )
                val shoulderHipRight = calculateDistance(
                    x1 = shoulderRight_x,
                    y1 = shoulderRight_y,
                    x2 = hipRight_x,
                    y2 = hipRight_y,
                )
                val hipKneeLeft = calculateDistance(
                    x1 = hipLeft_x,
                    y1 = hipLeft_y,
                    x2 = kneeLeft_x,
                    y2 = kneeLeft_y,
                )
                val hipKneeRight = calculateDistance(
                    x1 = hipRight_x,
                    y1 = hipRight_y,
                    x2 = kneeRight_x,
                    y2 = kneeRight_y,
                )

                return SquatMetric(
                    distance_shoulderKneeLeft = shoulderKneeLeft,
                    check_shoulderKneeLeft = true,
                    distance_shoulderHipLeft = shoulderHipLeft,
                    distance_shoulderHipRight = shoulderHipRight,
                    distance_hipKneeLeft = hipKneeLeft,
                    distance_hipKneeRight = hipKneeRight
                )

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
                    R.drawable.nose_emoji,
                    R.drawable.eye_emoji,
                    R.drawable.eye_emoji,
                    R.drawable.ear_emoji,
                    R.drawable.left_ear_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.dot,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.dot,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji
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

    override fun onDestroy() {
        super.onDestroy()
        model.close()
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