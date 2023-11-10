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


class MainActivity : AppCompatActivity() {

    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var model: LiteModelMovenetSingleposeLightningTfliteFloat164
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var handler:Handler
    lateinit var handlerThread: HandlerThread
    lateinit var textureView: TextureView
    lateinit var cameraManager: CameraManager
    lateinit var numberTextView: TextView
    val threshold_pose = 0.45
    var count_repetition = 0
    var consecutiveFramesWithPose = 0
    val K = 5
    var shoulderHipLeft = 0.0
    var shoulderHipRight = 0.0
    var hipKneeLeft = 0.0
    var hipKneeRight = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        get_permissions()

        imageProcessor = ImageProcessor.Builder().add(ResizeOp(192, 192, ResizeOp.ResizeMethod.BILINEAR)).build()
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
        numberTextView.text = count_repetition.toString() // Inizializza con il valore attuale del contatore
        numberTextView.setTextColor(Color.WHITE)



        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
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

                if (hasSkeletonDetected(outputFeature0, threshold_pose)) {
                    consecutiveFramesWithPose++
                    if (consecutiveFramesWithPose >= K) {
                        onPoseDetected()
                    }
                } else {
                    consecutiveFramesWithPose = 0
                }
                updatePoseState(outputFeature0)

                val canvas = textureView.lockCanvas()
                canvas?.let {
                    drawGreenBorder(it)
                    textureView.unlockCanvasAndPost(it)
                }
                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                drawPoseOnBitmap(mutableBitmap, outputFeature0, threshold_pose)
                imageView.setImageBitmap(mutableBitmap)
            }

            private fun hasSkeletonDetected(outputFeature0: FloatArray, threshold_pose: Double): Boolean {
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
            // Aggiorna questa funzione per gestire l'attivazione del bordo verde
            private fun onPoseDetected() {
                //colorScreenBorders(Color.GREEN)

                showToast("Pose detected for $K consecutive frames!")
                // ... Altre azioni da eseguire quando uno scheletro è rilevato per K frame consecutivi ...
            }

            private fun preprocessImage(bitmap: Bitmap): TensorImage {
                val tensorImage = TensorImage(DataType.UINT8)
                tensorImage.load(bitmap)
                return imageProcessor.process(tensorImage)
            }

            private fun runPoseDetection(tensorImage: TensorImage): FloatArray {
                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 192, 192, 3), DataType.UINT8)
                inputFeature0.loadBuffer(tensorImage.buffer)

                val outputs = model.process(inputFeature0)
                return outputs.outputFeature0AsTensorBuffer.floatArray
            }

            private fun drawPoseOnBitmap(bitmap: Bitmap, outputFeature0: FloatArray, threshold_pose: Double) {
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

            private fun updatePoseState(outputFeature0: FloatArray) {
                val scoreThreshold = 0.45

                val shoulderLeftX = outputFeature0[5 * 3 + 1] * bitmap.width
                val shoulderLeftY = outputFeature0[5 * 3] * bitmap.height
                val hipLeftX = outputFeature0[11 * 3 + 1] * bitmap.width
                val hipLeftY = outputFeature0[11 * 3] * bitmap.height

                val shoulderRightX = outputFeature0[6 * 3 + 1] * bitmap.width
                val shoulderRightY = outputFeature0[6 * 3] * bitmap.height
                val hipRightX = outputFeature0[12 * 3 + 1] * bitmap.width
                val hipRightY = outputFeature0[12 * 3] * bitmap.height

                val distanceShoulderHipLeft = calculateDistance(shoulderLeftX, shoulderLeftY, hipLeftX, hipLeftY)
                val distanceShoulderHipRight = calculateDistance(shoulderRightX, shoulderRightY, hipRightX, hipRightY)
                val distanceHipKneeLeft = calculateDistance(hipLeftX, hipLeftY, outputFeature0[13 * 3 + 1] * bitmap.width, outputFeature0[13 * 3] * bitmap.height)
                val distanceHipKneeRight = calculateDistance(hipRightX, hipRightY, outputFeature0[14 * 3 + 1] * bitmap.width, outputFeature0[14 * 3] * bitmap.height)

                if (distanceShoulderHipLeft > 0 && distanceShoulderHipRight > 0 && distanceHipKneeLeft > 0 && distanceHipKneeRight > 0) {
                    shoulderHipLeft = distanceShoulderHipLeft
                    shoulderHipRight = distanceShoulderHipRight
                    hipKneeLeft = distanceHipKneeLeft
                    hipKneeRight = distanceHipKneeRight
                    consecutiveFramesWithPose++

                    if (consecutiveFramesWithPose >= 5) {
                        onStep1Complete()
                    }
                } else {
                    consecutiveFramesWithPose = 0
                }
            }

            private fun onStep1Complete() {
                //colorScreenBorders(Color.GREEN)
                startStep2()
            }

            private fun startStep2() {
                // Inserisci qui la logica per lo Step 2
            }

            private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
                return Math.sqrt(Math.pow((x2 - x1).toDouble(), 2.0) + Math.pow((y2 - y1).toDouble(), 2.0))
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
                    R.drawable.dot,
                    R.drawable.dot,
                    R.drawable.dot,
                    R.drawable.dot,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.smile_emoji,
                    R.drawable.dot,
                    R.drawable.dot
                )
            }

            private fun drawEmojiOnCanvas(
                keypointX: Float, keypointY: Float, x: Int, w: Int, h: Int,
                emojiResources: Array<Int>, canvas: Canvas
            ) {
                val emojiDrawable = ContextCompat.getDrawable(this@MainActivity, emojiResources[x / 3])

                emojiDrawable?.setBounds(
                    (keypointX - 50).toInt(), (keypointY - 50).toInt(),
                    (keypointX + 50).toInt(), (keypointY + 50).toInt()
                )
                emojiDrawable?.draw(canvas)
            }

            private fun connectKeypoints(
                score: Float, x: Int, w: Int, h: Int, linePaint: Paint, canvas: Canvas, outputFeature0: FloatArray
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
    fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[1], object:CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                var captureRequest = p0.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface = Surface(textureView.surfaceTexture)
                captureRequest.addTarget(surface)
                p0.createCaptureSession(listOf(surface), object:CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }

    fun get_permissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }
    override fun onRequestPermissionsResult(  requestCode: Int, permissions: Array<out String>, grantResults: IntArray  ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED) get_permissions()
    }
}