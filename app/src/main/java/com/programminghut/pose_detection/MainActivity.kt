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
        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                var tensorImage = TensorImage(DataType.UINT8)
                tensorImage.load(bitmap)
                tensorImage = imageProcessor.process(tensorImage)

                val connections = arrayOf(
                    Pair(0, 1),  // Nose to Left Eye
                    Pair(0, 2),  // Nose to Right Eye
                    Pair(1, 3),  // Left Eye to Left Ear
                    Pair(2, 4),  // Right Eye to Right Ear
                    Pair(0, 5),  // Nose to Left Shoulder
                    Pair(0, 6),  // Nose to Right Shoulder
                    Pair(5, 7),  // Left Shoulder to Left Elbow
                    Pair(6, 8),  // Right Shoulder to Right Elbow
                    Pair(7, 9),  // Left Elbow to Left Wrist
                    Pair(8, 10), // Right Elbow to Right Wrist
                    Pair(5, 11), // Left Shoulder to Left Hip
                    Pair(6, 12), // Right Shoulder to Right Hip
                    Pair(11, 13), // Left Hip to Left Knee
                    Pair(12, 14), // Right Hip to Right Knee
                    Pair(13, 15), // Left Knee to Left Ankle
                    Pair(14, 16)  // Right Knee to Right Ankle
                )

                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 192, 192, 3), DataType.UINT8)
                inputFeature0.loadBuffer(tensorImage.buffer)

                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                var canvas = Canvas(mutable)
                var h = bitmap.height
                var w = bitmap.width
                var x = 0

                val linePaint = Paint()
                linePaint.color = Color.RED // Cambia il colore
                linePaint.strokeWidth = 8f // Imposta lo spessore della linea
                linePaint.style = Paint.Style.STROKE // Imposta uno stile di linea

                val noseEmoji = R.drawable.nose_emoji
                val eyeEmoji = R.drawable.eye_emoji
                val earEmoji = R.drawable.ear_emoji
                val earEmojiLeft = R.drawable.left_ear_emoji

                val smileEmoji = R.drawable.smile_emoji // Emoji per punti sconosciuti

                val emojiResources = arrayOf(
                    noseEmoji,
                    eyeEmoji,
                    eyeEmoji,
                    earEmoji,
                    earEmojiLeft,
                    smileEmoji,
                    smileEmoji,
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                    smileEmoji, // Per punti non specificati
                )


                Log.d("output__", outputFeature0.size.toString())
                while (x <= 49) {
                    val score = outputFeature0[x + 2]
                    val keypointX = outputFeature0[x + 1] * w
                    val keypointY = outputFeature0[x] * h

                    if (score > 0.45) {
                        // Carica l'emoji corrispondente in base alla posizione
                        val emojiDrawable = ContextCompat.getDrawable(this@MainActivity, emojiResources[x / 3])

                        emojiDrawable?.setBounds(
                            (keypointX - 50).toInt(), (keypointY - 50).toInt(),
                            (keypointX + 50).toInt(), (keypointY + 50).toInt()
                        )
                        emojiDrawable?.draw(canvas)
                    }

                    // Connect keypoints based on the defined connections
                    for (connection in connections) {
                        val (startIdx, endIdx) = connection
                        val startX = outputFeature0[startIdx * 3 + 1] * w
                        val startY = outputFeature0[startIdx * 3] * h
                        val endX = outputFeature0[endIdx * 3 + 1] * w
                        val endY = outputFeature0[endIdx * 3] * h

                        if (score > 0.45 && outputFeature0[startIdx * 3 + 2] > 0.45 && outputFeature0[endIdx * 3 + 2] > 0.45) {
                            // Disegna una linea tra i punti se entrambi i punti sono affidabili
                            canvas.drawLine(startX, startY, endX, endY, linePaint)
                        }
                    }

                    x += 3
                }

                imageView.setImageBitmap(mutable)
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