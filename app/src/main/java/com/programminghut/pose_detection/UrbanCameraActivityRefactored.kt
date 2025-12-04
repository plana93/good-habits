package com.programminghut.pose_detection

import android.Manifest
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
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.programminghut.pose_detection.adapters.ActiveFiltersAdapter
import com.programminghut.pose_detection.adapters.AvailableFiltersAdapter
// import com.programminghut.pose_detection.effects.FilterManager (already imported above)
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import com.programminghut.pose_detection.ui.FilterParamsBottomSheet
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.io.FileInputStream
import android.opengl.GLES20
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLUtils
import com.programminghut.pose_detection.effects.FrameClock
import android.opengl.EGLDisplay
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLSurface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.programminghut.pose_detection.effects.FilterManager

// Main activity for camera / media playback with filters. Implements FilterManager.FilterChangeListener
class UrbanCameraActivityRefactored : AppCompatActivity(), FilterManager.FilterChangeListener {
    companion object {
        private const val PREFS_NAME = "UrbanCameraPrefs"
        private const val KEY_ACTIVE_FILTERS = "active_filters"
        private const val KEY_FILTER_PARAMS_PREFIX = "filter_params_"
        private const val CAMERA_PERMISSION_CODE = 100
    }
    private lateinit var recordingStatus: TextView
    private lateinit var clearAllFiltersButton: com.google.android.material.button.MaterialButton
    
    // RecyclerViews for filters
    private lateinit var availableFiltersRecycler: RecyclerView
    private lateinit var activeFiltersRecycler: RecyclerView
    private lateinit var availableFiltersAdapter: AvailableFiltersAdapter
    private lateinit var activeFiltersAdapter: ActiveFiltersAdapter
    
    // Video recording
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var videoFilePath: String? = null
    
    // Menu state
    private var leftMenuOpen = false
    private var rightMenuOpen = false
    private var menuWidthLeft = 0
    private var menuWidthRight = 0
    // UI containers and buttons
    private lateinit var leftMenuContainer: View
    private lateinit var rightMenuContainer: View
    private lateinit var toggleLeftMenuButton: View
    private lateinit var toggleRightMenuButton: View
    private lateinit var captureButton: View
    private lateinit var videoButton: View
    
    // Latest frame data
    private var latestProcessedBitmap: Bitmap? = null
    private var latestPoseKeypoints: FloatArray? = null
    // Media playback mode
    private var isMediaPlayback: Boolean = false
    private var mediaPlaybackUri: String? = null
    private var playbackFromImage: Boolean = false
    private var playbackDurationSeconds: Int = 5
    private var framesForExport: MutableList<Bitmap> = mutableListOf()
    // Parallel list storing serialized filter parameter snapshots for each captured frame
    private var framesForExportParams: MutableList<Map<String, String>> = mutableListOf()
    // Parallel list storing RNG seed used for each captured frame (if seeded)
    private var framesForExportSeeds: MutableList<Long> = mutableListOf()
    // Parallel list storing raw source frames (for video replay/ deterministic re-render)
    private var framesForExportSources: MutableList<Bitmap> = mutableListOf()
    // Last debug dump directory written by debugDumpFrames (so export can update summary if it generates seeds later)
    private var lastDebugDumpDir: File? = null
    // Target FPS for exported MP4
    private val TARGET_EXPORT_FPS = 30
    
    // Processing state
    private var isProcessingFrame = false
    private var shouldContinueProcessing = true
    private var parametersUpdatePending = false
    private val parameterUpdateRunnable = Runnable {
        parametersUpdatePending = false
    }

    // Camera / model helpers (declared here to match other activities)
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var model: LiteModelMovenetSingleposeLightningTfliteFloat164
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private var selectedCameraIndex = -1
    private var isFrontCamera = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_urban_camera_refactored)
        
        selectedCameraIndex = intent.getIntExtra("cameraIndex", -1)
        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)
        
        // Calcola larghezze menu (stessa dimensione)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        menuWidthLeft = (displayMetrics.widthPixels * 0.30).toInt()
        menuWidthRight = (displayMetrics.widthPixels * 0.30).toInt()
        
        // check if launched in media playback mode (read extras early so UI can adapt)
        isMediaPlayback = intent.getBooleanExtra("isMediaPlayback", false)
        mediaPlaybackUri = intent.getStringExtra("mediaPlaybackUri")
        playbackFromImage = intent.getBooleanExtra("playbackFromImage", false)
        playbackDurationSeconds = intent.getIntExtra("playbackDurationSeconds", 5)

        initializeComponents()
        initializeFilterSystem()
        initializeUI()

        // Load filter settings in both modes
        loadFilterSettings()

    if (isMediaPlayback) {
            // don't open the camera, start playback pipeline
            startMediaPlayback()
        } else {
            getPermissions()
            setupTextureListener()
        }
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
        
        handlerThread = HandlerThread("cameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }
    
    private fun initializeFilterSystem() {
        // Inizializza FilterManager con tutti i filtri disponibili
        FilterManager.registerDefaultFilters()
        FilterManager.addListener(this)
    }
    
    private fun initializeUI() {
        // Trova views
        leftMenuContainer = findViewById(R.id.leftMenuContainer)
        rightMenuContainer = findViewById(R.id.rightMenuContainer)
        toggleLeftMenuButton = findViewById(R.id.toggleLeftMenuButton)
        toggleRightMenuButton = findViewById(R.id.toggleRightMenuButton)
        captureButton = findViewById(R.id.captureButton)
        videoButton = findViewById(R.id.videoButton)
        availableFiltersRecycler = findViewById(R.id.availableFiltersRecycler)
        activeFiltersRecycler = findViewById(R.id.activeFiltersRecycler)
        clearAllFiltersButton = findViewById(R.id.clearAllFiltersButton)
        
        // Imposta dimensioni menu
        leftMenuContainer.layoutParams.width = menuWidthLeft
        rightMenuContainer.layoutParams.width = menuWidthRight
        leftMenuContainer.translationX = -menuWidthLeft.toFloat()
        rightMenuContainer.translationX = menuWidthRight.toFloat()
        
        // Setup RecyclerViews
        setupAvailableFiltersRecycler()
        setupActiveFiltersRecycler()
        
        // Setup buttons
        toggleLeftMenuButton.setOnClickListener { toggleLeftMenu() }
        toggleRightMenuButton.setOnClickListener { toggleRightMenu() }

    // Exporter selection removed: only MediaRecorder exporter is supported now.
    val exporterSpinner = findViewById<Spinner>(R.id.exporterSpinner)
    exporterSpinner?.visibility = View.GONE

        // If launched in media playback mode, keep left capture as Save Frame and right button as Save Video
        if (isMediaPlayback) {
            // Ensure both buttons are visible: left saves current frame, right exports MP4
            captureButton.visibility = View.VISIBLE
            videoButton.visibility = View.VISIBLE
            captureButton.setOnClickListener {
                captureButton.isEnabled = false
                Thread {
                    try {
                        val src = synchronized(this) { latestProcessedBitmap }
                        if (src == null) {
                            runOnUiThread {
                                Toast.makeText(this@UrbanCameraActivityRefactored, "No frame available to save", Toast.LENGTH_LONG).show()
                                captureButton.isEnabled = true
                            }
                        } else {
                            // Apply active filters on a copy before saving so saved frame always has filters applied
                            val processed = src.copy(Bitmap.Config.ARGB_8888, true)
                            try {
                                val canvas = Canvas(processed)
                                FilterManager.applyFilters(canvas, src, latestPoseKeypoints)
                            } catch (e: Exception) {
                                android.util.Log.w("UrbanCamera", "Error applying filters while saving frame: ${e.message}")
                            }
                            val saved = saveBitmapToPublicPictures(processed, "frame_${System.currentTimeMillis()}.jpg")
                            runOnUiThread {
                                if (saved != null) {
                                    Toast.makeText(this@UrbanCameraActivityRefactored, "Frame saved: ${saved.absolutePath}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this@UrbanCameraActivityRefactored, "Failed to save frame", Toast.LENGTH_LONG).show()
                                }
                                captureButton.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UrbanCamera", "Error saving frame: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(this@UrbanCameraActivityRefactored, "Error saving frame: ${e.message}", Toast.LENGTH_LONG).show()
                            captureButton.isEnabled = true
                        }
                    }
                }.start()
            }

            videoButton.setOnClickListener {
                videoButton.isEnabled = false
                
                // Show progress dialog on UI thread
                val progressDialog = android.app.ProgressDialog(this@UrbanCameraActivityRefactored).apply {
                    setMessage("Generating video frames...")
                    setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
                    max = 100
                    setCancelable(false)
                    show()
                }
                
                Thread {
                    var outFile: File? = null
                    try {
                        android.util.Log.i("UrbanCamera", "=== STARTING VIDEO EXPORT ===")
                        android.util.Log.i("UrbanCamera", "playbackFromImage=$playbackFromImage, duration=${playbackDurationSeconds}s, fps=$TARGET_EXPORT_FPS")
                        
                        // Clear any previously collected frames
                        synchronized(framesForExport) { 
                            framesForExport.clear()
                            framesForExportParams.clear()
                            framesForExportSeeds.clear()
                            framesForExportSources.clear()
                        }

                        val uriStr = mediaPlaybackUri
                        if (uriStr.isNullOrEmpty()) {
                            android.util.Log.e("UrbanCamera", "ERROR: No mediaPlaybackUri available!")
                            throw IllegalStateException("No media URI")
                        }
                        
                        val uri = android.net.Uri.parse(uriStr)
                        android.util.Log.i("UrbanCamera", "Media URI: $uriStr")
                        
                        runOnUiThread {
                            progressDialog.setMessage("Processing frames...")
                        }
                        
                        if (playbackFromImage) {
                            exportFramesFromImage(uri, progressDialog)
                        } else {
                            exportFramesFromVideo(uri, progressDialog)
                        }
                        
                        // Export collected frames
                        val framesToExport = synchronized(framesForExport) { framesForExport.toList() }
                        android.util.Log.i("UrbanCamera", "Collected ${framesToExport.size} frames for export")
                        
                        if (framesToExport.isEmpty()) {
                            android.util.Log.e("UrbanCamera", "ERROR: No frames collected!")
                            throw IllegalStateException("No frames generated")
                        }
                        
                        runOnUiThread {
                            progressDialog.setMessage("Encoding video...")
                            progressDialog.progress = 50
                        }
                        
                        // Debug dump
                        try {
                            val paramsForDump = synchronized(framesForExportParams) {
                                if (framesForExportParams.size >= framesToExport.size) 
                                    framesForExportParams.take(framesToExport.size).toList() 
                                else null
                            }
                            val seedsForDump = synchronized(framesForExportSeeds) {
                                if (framesForExportSeeds.size >= framesToExport.size) 
                                    framesForExportSeeds.take(framesToExport.size).toList() 
                                else null
                            }
                            val sourcesForDump = synchronized(framesForExportSources) {
                                if (framesForExportSources.size >= framesToExport.size) 
                                    framesForExportSources.take(framesToExport.size).toList() 
                                else null
                            }
                            val dump = debugDumpFrames(framesToExport, paramsForDump, seedsForDump, sourcesForDump)
                            if (dump != null) {
                                android.util.Log.i("UrbanCamera", "Debug frames written to: ${dump.absolutePath}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("UrbanCamera", "Could not write debug frames: ${e.message}")
                        }
                        
                        runOnUiThread {
                            progressDialog.setMessage("Creating MP4 file...")
                            progressDialog.progress = 75
                        }
                        
                        // Export to video file
                        outFile = exportFramesWithMediaRecorder(framesToExport, TARGET_EXPORT_FPS)
                        
                        if (outFile != null) {
                            android.util.Log.i("UrbanCamera", "Export SUCCESS: ${outFile.absolutePath} (${outFile.length()} bytes)")
                            try {
                                val dur = validateVideoFile(outFile)
                                android.util.Log.i("UrbanCamera", "Video duration: ${dur}ms")
                            } catch (e: Exception) {
                                android.util.Log.w("UrbanCamera", "Could not validate video: ${e.message}")
                            }
                        } else {
                            android.util.Log.e("UrbanCamera", "ERROR: exportFramesWithMediaRecorder returned null!")
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("UrbanCamera", "Export FAILED with exception: ${e.message}", e)
                        e.printStackTrace()
                    } finally {
                        runOnUiThread {
                            progressDialog.dismiss()
                            
                            if (outFile != null && outFile.exists()) {
                                Toast.makeText(
                                    this@UrbanCameraActivityRefactored, 
                                    "✓ Video saved: ${outFile.name}", 
                                    Toast.LENGTH_LONG
                                ).show()
                                // Close activity after successful export
                                handler.postDelayed({
                                    finish()
                                }, 2000)
                            } else {
                                Toast.makeText(
                                    this@UrbanCameraActivityRefactored, 
                                    "✗ Video export failed - check logs", 
                                    Toast.LENGTH_LONG
                                ).show()
                                videoButton.isEnabled = true
                            }
                        }
                    }
                }.start()
            }
        } else {
            // Normal camera behavior
            captureButton.setOnClickListener { capturePhoto() }
            videoButton.setOnClickListener { toggleVideoRecording() }
        }

        clearAllFiltersButton.setOnClickListener {
            FilterManager.deactivateAll()
            activeFiltersAdapter.notifyDataSetChanged()
            availableFiltersAdapter.notifyDataSetChanged()
        }
    }
    
    private fun exportFramesFromImage(uri: android.net.Uri, progressDialog: android.app.ProgressDialog? = null) {
        android.util.Log.i("UrbanCamera", "=== exportFramesFromImage START ===")
        try {
            // Load source image
            val sourceBitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
            android.util.Log.i("UrbanCamera", "Loaded image: ${sourceBitmap.width}x${sourceBitmap.height}")
            
            val fps = TARGET_EXPORT_FPS.coerceAtLeast(1)
            val totalFrames = (playbackDurationSeconds * fps).coerceAtLeast(1)
            android.util.Log.i("UrbanCamera", "Will generate $totalFrames frames at ${fps}fps for ${playbackDurationSeconds}s")
            
            val collected = mutableListOf<Bitmap>()
            val collectedStates = mutableListOf<Map<String, String>>()
            val collectedSeeds = mutableListOf<Long>()
            
            val baseSeed = System.nanoTime()
            val baseTime = System.currentTimeMillis()
            
            // Get active filters once
            val activeFilters = FilterManager.getActiveFilters()
            android.util.Log.i("UrbanCamera", "Active filters: ${activeFilters.size}")
            
            // Detect pose once if needed (since source image doesn't change)
            val needPose = FilterManager.activeFiltersRequirePose()
            val poseKeypoints = if (needPose) {
                android.util.Log.i("UrbanCamera", "Detecting pose on source image...")
                detectPoseOnBitmap(sourceBitmap)
            } else {
                null
            }
            
            for (i in 0 until totalFrames) {
                if (!shouldContinueProcessing) {
                    android.util.Log.w("UrbanCamera", "Export cancelled by user")
                    break
                }
                
                try {
                    // Update progress
                    val progress = ((i.toFloat() / totalFrames) * 100).toInt()
                    runOnUiThread {
                        progressDialog?.progress = progress
                        progressDialog?.setMessage("Generating frame ${i+1}/$totalFrames")
                    }
                    
                    // Set seed for this frame (for randomic filters)
                    val seed = baseSeed + i
                    com.programminghut.pose_detection.effects.RandomProvider.setSeed(seed)
                    
                    // Set frame time (for time-based filters)
                    val frameTimeMs = baseTime + (i * 1000L / fps)
                    FrameClock.setFrameTimeMs(frameTimeMs)
                    
                    // Render frame with filters
                    val outputBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(outputBitmap)
                    
                    // Apply each active filter
                    activeFilters.forEach { filter ->
                        try {
                            filter.apply(canvas, sourceBitmap, poseKeypoints)
                        } catch (e: Exception) {
                            android.util.Log.w("UrbanCamera", "Filter ${filter.id} failed: ${e.message}")
                        }
                    }
                    
                    // Store the rendered frame
                    collected.add(outputBitmap)
                    collectedStates.add(captureFilterState())
                    collectedSeeds.add(seed)
                    
                    if ((i + 1) % 30 == 0) {
                        android.util.Log.i("UrbanCamera", "Generated ${i+1}/$totalFrames frames")
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("UrbanCamera", "Error generating frame $i: ${e.message}", e)
                    // Continue with next frame
                }
            }
            
            android.util.Log.i("UrbanCamera", "Frame generation complete: ${collected.size} frames")
            
            // Store in export lists
            if (collected.isNotEmpty()) {
                synchronized(framesForExport) {
                    framesForExport.addAll(collected)
                    framesForExportParams.addAll(collectedStates)
                    framesForExportSeeds.addAll(collectedSeeds)
                }
                android.util.Log.i("UrbanCamera", "✓ Frames stored in export list")
            } else {
                android.util.Log.e("UrbanCamera", "✗ No frames collected!")
            }
            
            // Clean up source bitmap
            sourceBitmap.recycle()
            
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "exportFramesFromImage FAILED: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
        android.util.Log.i("UrbanCamera", "=== exportFramesFromImage END ===")
    }
    
    private fun exportFramesFromVideo(uri: android.net.Uri, progressDialog: android.app.ProgressDialog? = null) {
        android.util.Log.i("UrbanCamera", "=== exportFramesFromVideo START ===")
        val retriever = android.media.MediaMetadataRetriever()
        var tempCopiedFile: File? = null
        
        try {
            // Try to get persistable URI permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val takeFlags = intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (uri.scheme == "content" && takeFlags != 0) {
                    try {
                        contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        android.util.Log.i("UrbanCamera", "Persistable URI permission granted")
                    } catch (e: SecurityException) {
                        android.util.Log.w("UrbanCamera", "Could not get persistable URI permission: ${e.message}")
                    }
                }
            }
            
            // Try to set data source with multiple fallback strategies
            var dataSourceSet = false
            
            // Strategy 1: Direct context + URI
            try {
                retriever.setDataSource(this@UrbanCameraActivityRefactored, uri)
                dataSourceSet = true
                android.util.Log.i("UrbanCamera", "Data source set via context+URI")
            } catch (e: Exception) {
                android.util.Log.w("UrbanCamera", "Failed to set data source via context+URI: ${e.message}")
            }
            
            // Strategy 2: File descriptor (for content:// URIs)
            if (!dataSourceSet && (uri.scheme == "content" || uri.scheme == "file")) {
                try {
                    contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                        dataSourceSet = true
                        android.util.Log.i("UrbanCamera", "Data source set via file descriptor")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("UrbanCamera", "Failed to set data source via file descriptor: ${e.message}")
                }
            }
            
            // Strategy 3: Copy to temp file and open by path
            if (!dataSourceSet) {
                tempCopiedFile = copyUriToTempFile(this@UrbanCameraActivityRefactored, uri)
                if (tempCopiedFile != null) {
                    android.util.Log.i("UrbanCamera", "Copied to temp file: ${tempCopiedFile.absolutePath}")
                    
                    // Try multiple ways to open the temp file
                    try {
                        FileInputStream(tempCopiedFile).use { fis ->
                            retriever.setDataSource(fis.fd)
                            dataSourceSet = true
                            android.util.Log.i("UrbanCamera", "Data source set via temp file descriptor")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("UrbanCamera", "Failed via temp file descriptor: ${e.message}")
                        
                        try {
                            retriever.setDataSource(tempCopiedFile.absolutePath)
                            dataSourceSet = true
                            android.util.Log.i("UrbanCamera", "Data source set via temp file path")
                        } catch (e2: Exception) {
                            android.util.Log.w("UrbanCamera", "Failed via temp file path: ${e2.message}")
                            
                            try {
                                retriever.setDataSource("file://${tempCopiedFile.absolutePath}")
                                dataSourceSet = true
                                android.util.Log.i("UrbanCamera", "Data source set via file:// URL")
                            } catch (e3: Exception) {
                                android.util.Log.e("UrbanCamera", "All temp file strategies failed: ${e3.message}")
                            }
                        }
                    }
                }
            }
            
            if (!dataSourceSet) {
                throw RuntimeException("Could not set data source for video URI")
            }
            
            // Extract frames at target FPS
            val fps = TARGET_EXPORT_FPS.coerceAtLeast(1)
            val frameIntervalUs = 1_000_000L / fps
            val maxDurationUs = playbackDurationSeconds * 1_000_000L
            val estimatedFrames = ((maxDurationUs / frameIntervalUs).toInt())
            android.util.Log.i("UrbanCamera", "Will extract ~$estimatedFrames frames at ${fps}fps for ${playbackDurationSeconds}s")
            
            val collected = mutableListOf<Bitmap>()
            val collectedStates = mutableListOf<Map<String, String>>()
            val collectedSeeds = mutableListOf<Long>()
            val collectedSources = mutableListOf<Bitmap>()
            
            val baseSeed = System.nanoTime()
            val baseTime = System.currentTimeMillis()
            var frameIndex = 0
            var timeUs = 0L
            
            while (timeUs <= maxDurationUs && shouldContinueProcessing) {
                // Update progress
                val progress = ((timeUs.toFloat() / maxDurationUs) * 100).toInt().coerceIn(0, 100)
                runOnUiThread {
                    progressDialog?.progress = progress
                    progressDialog?.setMessage("Extracting frame ${frameIndex+1}...")
                }
                
                val frame = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                
                if (frame != null) {
                    try {
                        val srcCopy = frame.copy(Bitmap.Config.ARGB_8888, false)
                        val seed = baseSeed + frameIndex
                        
                        FrameClock.setFrameTimeMs(baseTime + (frameIndex * (frameIntervalUs / 1000L)))
                        val rendered = renderBitmapWithFilters(srcCopy, null, seed)
                        
                        if (rendered != null) {
                            collected.add(rendered)
                            collectedStates.add(captureFilterState())
                            collectedSeeds.add(seed)
                            collectedSources.add(srcCopy)
                        } else {
                            srcCopy.recycle()
                        }
                        
                        if ((frameIndex + 1) % 30 == 0) {
                            android.util.Log.i("UrbanCamera", "Extracted ${frameIndex+1} frames")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("UrbanCamera", "Error processing frame $frameIndex: ${e.message}")
                    }
                }
                
                timeUs += frameIntervalUs
                frameIndex++
            }
            
            android.util.Log.i("UrbanCamera", "Frame extraction complete: ${collected.size} frames")
            
            if (collected.isNotEmpty()) {
                synchronized(framesForExport) {
                    framesForExport.addAll(collected)
                    framesForExportParams.addAll(collectedStates)
                    framesForExportSeeds.addAll(collectedSeeds)
                    framesForExportSources.addAll(collectedSources)
                }
                android.util.Log.i("UrbanCamera", "✓ Frames stored in export list")
            } else {
                android.util.Log.e("UrbanCamera", "✗ No frames extracted!")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "exportFramesFromVideo FAILED: ${e.message}", e)
            e.printStackTrace()
            throw e
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                android.util.Log.w("UrbanCamera", "Error releasing retriever: ${e.message}")
            }
            
            tempCopiedFile?.let {
                try {
                    it.delete()
                } catch (e: Exception) {
                    android.util.Log.w("UrbanCamera", "Error deleting temp file: ${e.message}")
                }
            }
        }
        android.util.Log.i("UrbanCamera", "=== exportFramesFromVideo END ===")
    }
    
    private fun setupAvailableFiltersRecycler() {
        availableFiltersAdapter = AvailableFiltersAdapter(
            filters = FilterManager.getAvailableFilters(),
            onFilterClick = { filter ->
                if (filter.isActive) {
                    FilterManager.deactivateFilter(filter)
                } else {
                    FilterManager.activateFilter(filter)
                }
                availableFiltersAdapter.updateFilter(filter)
            },
            onFilterLongClick = { filter ->
                showFilterParamsDialog(filter)
            }
        )
        
        availableFiltersRecycler.apply {
            layoutManager = LinearLayoutManager(this@UrbanCameraActivityRefactored)
            adapter = availableFiltersAdapter
        }
    }
    
    private fun setupActiveFiltersRecycler() {
        activeFiltersAdapter = ActiveFiltersAdapter(
            onFilterRemove = { filter ->
                FilterManager.deactivateFilter(filter)
                availableFiltersAdapter.notifyDataSetChanged()
            },
            onFilterLongClick = { filter ->
                showFilterParamsDialog(filter)
            }
        )
        
        // Inizializza con i filtri attivi correnti
        activeFiltersAdapter.updateFilters(FilterManager.getActiveFilters())
        
        activeFiltersRecycler.apply {
            layoutManager = LinearLayoutManager(this@UrbanCameraActivityRefactored)
            adapter = activeFiltersAdapter
        }
        
        // Setup swipe to remove
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position >= 0 && position < FilterManager.getActiveFilters().size) {
                    try {
                        activeFiltersAdapter.removeFilter(position)
                    } catch (e: Exception) {
                        android.util.Log.e("UrbanCamera", "Error removing filter: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(
                                this@UrbanCameraActivityRefactored,
                                "Error removing filter",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Refresh adapter to restore state
                            activeFiltersAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        })
        
        itemTouchHelper.attachToRecyclerView(activeFiltersRecycler)
    }
    
    private fun showFilterParamsDialog(filter: com.programminghut.pose_detection.effects.AdaptiveFilter) {
        val bottomSheet = FilterParamsBottomSheet(
            context = this,
            filter = filter,
            onParametersChanged = {
                // Debouncing: segnala che c'è un update pendente invece di chiamare direttamente processFrame()
                // Il loop di processing principale lo gestirà
                handler.removeCallbacks(parameterUpdateRunnable)
                parametersUpdatePending = true
                handler.postDelayed(parameterUpdateRunnable, 100)
                // Salva subito i parametri modificati
                saveFilterSettings()
            }
        )
        bottomSheet.show()
    }
    
    private fun toggleLeftMenu() {
        leftMenuOpen = !leftMenuOpen
        leftMenuContainer.animate()
            .translationX(if (leftMenuOpen) 0f else -menuWidthLeft.toFloat())
            .setDuration(300)
            .start()
        
        toggleLeftMenuButton.animate()
            .rotation(if (leftMenuOpen) 180f else 0f)
            .setDuration(300)
            .start()
    }
    
    private fun toggleRightMenu() {
        rightMenuOpen = !rightMenuOpen
        rightMenuContainer.animate()
            .translationX(if (rightMenuOpen) 0f else menuWidthRight.toFloat())
            .setDuration(300)
            .start()
        
        toggleRightMenuButton.animate()
            .rotation(if (rightMenuOpen) 0f else 180f)
            .setDuration(300)
            .start()
    }
    
    // ===== FilterChangeListener Implementation =====
    
    override fun onFilterActivated(filter: com.programminghut.pose_detection.effects.AdaptiveFilter) {
        runOnUiThread {
            activeFiltersAdapter.updateFilters(FilterManager.getActiveFilters())
            availableFiltersAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onFilterDeactivated(filter: com.programminghut.pose_detection.effects.AdaptiveFilter) {
        runOnUiThread {
            activeFiltersAdapter.updateFilters(FilterManager.getActiveFilters())
            availableFiltersAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onFiltersReordered(filters: List<com.programminghut.pose_detection.effects.AdaptiveFilter>) {
        runOnUiThread {
            activeFiltersAdapter.updateFilters(filters)
        }
    }
    
    override fun onAllFiltersDeactivated() {
        runOnUiThread {
            activeFiltersAdapter.updateFilters(emptyList())
            availableFiltersAdapter.notifyDataSetChanged()
        }
    }
    
    // ===== Camera Setup =====
    
    private fun setupTextureListener() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
                configureTransform(width, height)
            }
            
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                configureTransform(width, height)
            }
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }
    
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (viewWidth == 0 || viewHeight == 0) return
        
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = android.graphics.Matrix()
        val viewRect = android.graphics.RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = android.graphics.RectF(0f, 0f, 1080f, 1920f)
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        
        when (rotation) {
            Surface.ROTATION_0 -> {
                // Portrait normale - nessuna trasformazione necessaria
            }
            Surface.ROTATION_90 -> {
                // Ruota 90 gradi
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
                matrix.setRectToRect(viewRect, bufferRect, android.graphics.Matrix.ScaleToFit.FILL)
                matrix.postRotate(90f, centerX, centerY)
            }
            Surface.ROTATION_180 -> {
                // Ruota 180 gradi
                matrix.postRotate(180f, centerX, centerY)
            }
            Surface.ROTATION_270 -> {
                // Ruota 270 gradi
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
                matrix.setRectToRect(viewRect, bufferRect, android.graphics.Matrix.ScaleToFit.FILL)
                matrix.postRotate(270f, centerX, centerY)
            }
        }
        
        textureView.setTransform(matrix)
    }
    
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraId = if (selectedCameraIndex >= 0) {
            cameraManager.cameraIdList[selectedCameraIndex]
        } else {
            cameraManager.cameraIdList.find { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == 
                    if (isFrontCamera) CameraCharacteristics.LENS_FACING_FRONT 
                    else CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList[0]
        }
        
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startCameraPreview()
            }
            
            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }
            
            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
            }
        }, handler)
    }
    
    private fun startCameraPreview() {
        val surfaceTexture = textureView.surfaceTexture
        if (surfaceTexture == null) {
            android.util.Log.e("UrbanCamera", "SurfaceTexture nullo in startCameraPreview")
            return
        }
        surfaceTexture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(surfaceTexture)
        val camera = cameraDevice
        if (camera == null) {
            android.util.Log.e("UrbanCamera", "cameraDevice nullo in startCameraPreview")
            return
        }
        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        camera.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )
                    val captureRequest = captureRequestBuilder.build()
                    try {
                        session.setRepeatingRequest(captureRequest, null, handler)
                        startFrameProcessing()
                    } catch (e: Exception) {
                        android.util.Log.e("UrbanCamera", "Error starting preview: ${e.message}", e)
                    }
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    runOnUiThread {
                        Toast.makeText(
                            this@UrbanCameraActivityRefactored,
                            "Camera configuration failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            handler
        )
    }
    
    private fun startFrameProcessing() {
        shouldContinueProcessing = true
        handler.post(object : Runnable {
            override fun run() {
                if (shouldContinueProcessing && !isProcessingFrame && textureView.isAvailable) {
                    isProcessingFrame = true
                    try {
                        val bitmap = textureView.bitmap
                        if (bitmap == null) {
                            android.util.Log.w("UrbanCamera", "Bitmap null in startFrameProcessing")
                        } else {
                            latestProcessedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            processFrame()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UrbanCamera", "Error capturing frame: ${e.message}", e)
                    } finally {
                        isProcessingFrame = false
                    }
                }
                if (shouldContinueProcessing) {
                    handler.postDelayed(this, 33)
                }
            }
        })
    }
    
    private fun processFrame() {
        try {
            val sourceBitmap = latestProcessedBitmap
            if (sourceBitmap == null) {
                android.util.Log.w("UrbanCamera", "latestProcessedBitmap nullo in processFrame")
                return
            }
            // Perform pose detection only if an active filter requires pose
            val needPose = com.programminghut.pose_detection.effects.FilterManager.activeFiltersRequirePose()
            var scaledKeypoints: FloatArray? = null
            if (needPose) {
                val tensorImage = TensorImage.fromBitmap(sourceBitmap)
                val processedImage = imageProcessor.process(tensorImage)
                val outputs = model.process(processedImage.tensorBuffer)
                val normalizedKeypoints = outputs.outputFeature0AsTensorBuffer.floatArray
                if (normalizedKeypoints.isEmpty()) {
                    android.util.Log.w("UrbanCamera", "Keypoints vuoti dal modello in processFrame")
                } else {
                    scaledKeypoints = FloatArray(normalizedKeypoints.size)
                    for (i in normalizedKeypoints.indices step 3) {
                        scaledKeypoints[i] = normalizedKeypoints[i + 1] * sourceBitmap.width
                        scaledKeypoints[i + 1] = normalizedKeypoints[i] * sourceBitmap.height
                        scaledKeypoints[i + 2] = normalizedKeypoints[i + 2]
                    }
                }
            }
            latestPoseKeypoints = scaledKeypoints
            val outputBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(outputBitmap)
            FilterManager.applyFilters(canvas, sourceBitmap, scaledKeypoints)
            // If in media playback mode and we're collecting frames for export, store a copy
            if (isMediaPlayback) {
                try {
                    val bmpCopy = outputBitmap.copy(Bitmap.Config.ARGB_8888, false)
                    val state = captureFilterState()
                    synchronized(framesForExport) {
                        framesForExport.add(bmpCopy)
                                framesForExportParams.add(state)
                                // No explicit seed for live video frames; mark with -1
                                framesForExportSeeds.add(-1L)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("UrbanCamera", "Error storing frame for export: ${e.message}", e)
                }
            }
            runOnUiThread {
                imageView.setImageBitmap(outputBitmap)
            }
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "Error in processFrame: ${e.message}", e)
        }
    }

    // Process a single bitmap (used for media playback). Runs the same detection + filters pipeline synchronously on handler thread.
    private fun processBitmapForMedia(bitmap: Bitmap) {
        try {
            latestProcessedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            // Reuse same processing logic as processFrame by setting latestProcessedBitmap and calling processFrame
            processFrame()
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "Error processBitmapForMedia: ${e.message}", e)
        }
    }

    // Detect pose keypoints for a bitmap without modifying shared state. Returns scaled keypoints or null.
    private fun detectPoseOnBitmap(bitmap: Bitmap): FloatArray? {
        try {
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)
            val outputs = model.process(processedImage.tensorBuffer)
            val normalizedKeypoints = outputs.outputFeature0AsTensorBuffer.floatArray
            if (normalizedKeypoints.isEmpty()) return null
            val scaledKeypoints = FloatArray(normalizedKeypoints.size)
            for (i in normalizedKeypoints.indices step 3) {
                scaledKeypoints[i] = normalizedKeypoints[i + 1] * bitmap.width
                scaledKeypoints[i + 1] = normalizedKeypoints[i] * bitmap.height
                scaledKeypoints[i + 2] = normalizedKeypoints[i + 2]
            }
            return scaledKeypoints
        } catch (e: Exception) {
            android.util.Log.w("UrbanCamera", "detectPoseOnBitmap failed: ${e.message}")
            return null
        }
    }

    // Build clones of active filters and apply a param snapshot (map of "filterId:paramKey" -> serializedValue)
    private fun buildFilterClonesFromParamState(paramState: Map<String, String>?): List<com.programminghut.pose_detection.effects.AdaptiveFilter> {
        val clones = mutableListOf<com.programminghut.pose_detection.effects.AdaptiveFilter>()
        try {
            val active = com.programminghut.pose_detection.effects.FilterManager.getActiveFilters()
            active.forEach { f ->
                val c = f.clone()
                // Apply paramState entries for this filter
                paramState?.forEach { (k, v) ->
                    if (k.startsWith(f.id + ":")) {
                        val paramKey = k.substringAfter(":")
                        try {
                            c.parameters[paramKey]?.deserialize(v)
                        } catch (_: Exception) {}
                    }
                }
                clones.add(c)
            }
        } catch (e: Exception) {
            android.util.Log.w("UrbanCamera", "buildFilterClonesFromParamState failed: ${e.message}")
        }
        return clones
    }

    // Render a bitmap with the cloned filters and a seed. Returns a processed Bitmap or null on failure.
    private fun renderBitmapWithFilters(src: Bitmap, paramState: Map<String, String>?, seed: Long): Bitmap? {
        try {
            // Seed RNG
            com.programminghut.pose_detection.effects.RandomProvider.setSeed(seed)

            // Detect pose for this bitmap only if active filters require it
            val needPoseForRender = com.programminghut.pose_detection.effects.FilterManager.activeFiltersRequirePose()
            val pose = if (needPoseForRender) detectPoseOnBitmap(src) else null

            // Clone filters and apply param snapshot
            val clones = buildFilterClonesFromParamState(paramState)

            // Create output bitmap and canvas
            val out = src.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(out)

            // Apply each cloned filter
            clones.forEach { cf ->
                try {
                    cf.apply(canvas, src, pose)
                } catch (e: Exception) {
                    android.util.Log.w("UrbanCamera", "Filter apply failed in renderBitmapWithFilters: ${e.message}")
                }
            }

            return out
        } catch (e: Exception) {
            android.util.Log.w("UrbanCamera", "renderBitmapWithFilters failed: ${e.message}")
            return null
        }
    }

    private fun startMediaPlayback() {
        // In playback mode show the left capture (Save Frame) button and the right video button for saving
        runOnUiThread {
            captureButton.visibility = View.VISIBLE
            captureButton.isEnabled = true
            videoButton.visibility = View.VISIBLE
            videoButton.isEnabled = true
        }

        // clear any previously collected frames
        synchronized(framesForExport) { framesForExport.clear() }

        val uriStr = mediaPlaybackUri ?: return
        val uri = android.net.Uri.parse(uriStr)

        if (playbackFromImage) {
            // load image and repeat for playbackDurationSeconds at 15fps
            Thread {
                try {
                    val bmp = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val totalFrames = playbackDurationSeconds * 15
                    val frameIntervalMs = 1000L / 15L
                    for (i in 0 until totalFrames) {
                        if (!shouldContinueProcessing) break
                        processBitmapForMedia(bmp)
                        Thread.sleep(frameIntervalMs)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("UrbanCamera", "Error in image playback: ${e.message}", e)
                }
            }.start()
        } else {
            // Video playback: extract frames at ~15fps and process
            Thread {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(this, uri)
                    val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    val fps = 15
                    val frameIntervalUs = 1_000_000L / fps
                    var timeUs = 0L
                    // Limit playback to the configured playbackDurationSeconds so exported video
                    // length is controlled by the activity setting rather than user press time.
                    val maxDurationUs = playbackDurationSeconds * 1_000_000L
                    while (timeUs <= maxDurationUs && shouldContinueProcessing) {
                        val frame = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                        if (frame != null) {
                            processBitmapForMedia(frame)
                        }
                        timeUs += frameIntervalUs
                        Thread.sleep((1000L / fps))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("UrbanCamera", "Error in video playback: ${e.message}", e)
                } finally {
                    retriever.release()
                }
            }.start()
        }
    }
    
    // ===== Photo Capture =====
    
    private fun capturePhoto() {
        val sourceBitmap = latestProcessedBitmap ?: textureView.bitmap
        if (sourceBitmap == null) {
            android.util.Log.e("UrbanCamera", "Bitmap nulla in capturePhoto, impossibile salvare")
            runOnUiThread {
                Toast.makeText(this, "Errore: nessuna immagine da salvare", Toast.LENGTH_SHORT).show()
            }
            return
        }
        // Applica i filtri attivi al bitmap per la preview e il salvataggio
        val processedBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        // Usa gli ultimi keypoints se disponibili, altrimenti null
        val keypoints = latestPoseKeypoints
        FilterManager.applyFilters(canvas, sourceBitmap, keypoints)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "URBAN_POSE_${timestamp}.jpg"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/UrbanPose")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri == null) {
                    android.util.Log.e("UrbanCamera", "URI nullo in capturePhoto")
                } else {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                    runOnUiThread {
                        Toast.makeText(this, "Photo saved!", Toast.LENGTH_SHORT).show()
                        showPhotoPreviewDialog(processedBitmap)
                    }
                }
            } else {
                val file = File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES
                    ),
                    "UrbanPose/$filename"
                )
                file.parentFile?.mkdirs()
                file.outputStream().use { outputStream ->
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                runOnUiThread {
                    Toast.makeText(this, "Photo saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    showPhotoPreviewDialog(processedBitmap)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "Errore salvataggio foto: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Save a bitmap into public Pictures/UrbanCamera and return the File or null
    private fun saveBitmapToPublicPictures(bitmap: Bitmap, filename: String): File? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/UrbanCamera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                    return File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "UrbanCamera/$filename")
                }
                return null
            } else {
                val publicFolderFile = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "UrbanCamera")
                if (!publicFolderFile.exists()) publicFolderFile.mkdirs()
                val file = File(publicFolderFile, filename)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                return file
            }
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "Error saving bitmap to public pictures: ${e.message}", e)
            return null
        }
    }

    // Write a small diagnostic dump: frame count, sizes, and save first frame into app external files to inspect
    // Capture current active filters' parameter values as a flat map (filterId:paramKey -> serializedValue)
    private fun captureFilterState(): Map<String, String> {
        val snapshot = mutableMapOf<String, String>()
        try {
            val active = FilterManager.getActiveFilters()
            active.forEach { filter ->
                try {
                    filter.getParameters().forEach { p ->
                        try {
                            snapshot["${filter.id}:${p.key}"] = p.serialize()
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return snapshot
    }

    private fun debugDumpFrames(frames: List<Bitmap>, paramStates: List<Map<String,String>>? = null, seeds: List<Long>? = null, sources: List<Bitmap>? = null): File? {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val debugDir = File(getExternalFilesDir(null), "debug_frames_$timestamp")
            if (!debugDir.exists()) debugDir.mkdirs()
            val infoFile = File(debugDir, "info.txt")
            infoFile.printWriter().use { pw ->
                pw.println("frames_count=${frames.size}")
                frames.forEachIndexed { idx, bmp ->
                    pw.println("frame_$idx: ${bmp.width}x${bmp.height}")
                }
            }
            // Save all processed frames (beware of disk usage)
            frames.forEachIndexed { idx, bmp ->
                try {
                    val f = File(debugDir, "frame_${idx}.jpg")
                    f.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                } catch (e: Exception) {
                    android.util.Log.w("UrbanCamera", "Could not write frame_${idx}.jpg: ${e.message}")
                }
            }

            // If raw source frames are provided (video extraction), save them too
            sources?.let { srcs ->
                srcs.forEachIndexed { idx, sBmp ->
                    try {
                        val sf = File(debugDir, "source_${idx}.jpg")
                        sf.outputStream().use { out -> sBmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                    } catch (e: Exception) {
                        android.util.Log.w("UrbanCamera", "Could not write source_${idx}.jpg: ${e.message}")
                    }
                }
            }
            // If parameter snapshots are available, write them alongside frames for diagnosis/replay
            paramStates?.let { states ->
                try {
                    states.forEachIndexed { idx, map ->
                        val f = File(debugDir, "frame_${idx}_params.txt")
                        f.printWriter().use { pw ->
                            map.forEach { (k, v) -> pw.println("${k}=${v}") }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("UrbanCamera", "Could not write frame param snapshots: ${e.message}")
                }
            }
            // If seeds are available, write them too for reproducible replay
            seeds?.let { sList ->
                try {
                    sList.forEachIndexed { idx, seed ->
                        val f = File(debugDir, "frame_${idx}_seed.txt")
                        f.printWriter().use { pw -> pw.println(seed.toString()) }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("UrbanCamera", "Could not write frame seed snapshots: ${e.message}")
                }
            }
            // Also write a combined JSON summary for convenience (use helper)
            try {
                writeSummaryJson(debugDir, frames, paramStates, seeds)
            } catch (e: Exception) {
                android.util.Log.w("UrbanCamera", "Could not write summary.json: ${e.message}")
            }
            // remember last debug dir so export can update summary.json if it generates seeds later
            lastDebugDumpDir = debugDir
            return debugDir
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "Error writing debug dump: ${e.message}", e)
            return null
        }
    }

    // Helper to write a structured summary.json into a debug directory
    private fun writeSummaryJson(debugDir: File, frames: List<Bitmap>, paramStates: List<Map<String,String>>?, seeds: List<Long>?) {
        try {
            val sb = StringBuilder()
            sb.append("{")
            sb.append("\"frames\": ${frames.size}")
            seeds?.let { sList ->
                sb.append(", \"seeds\": [")
                sList.forEachIndexed { idx, seed ->
                    sb.append(seed)
                    if (idx < sList.size - 1) sb.append(",")
                }
                sb.append("]")
            }
            paramStates?.let { pStates ->
                sb.append(", \"params\": [")
                pStates.forEachIndexed { idx, map ->
                    sb.append("{")
                    val entries = map.entries.toList()
                    entries.forEachIndexed { j, e ->
                        val k = e.key.replace("\"", "\\\"")
                        val v = e.value.replace("\"", "\\\"")
                        sb.append("\"").append(k).append("\": \"").append(v).append("\"")
                        if (j < entries.size - 1) sb.append(",")
                    }
                    sb.append("}")
                    if (idx < pStates.size - 1) sb.append(",")
                }
                sb.append("]")
            }
            sb.append("}")
            val summaryFile = File(debugDir, "summary.json")
            summaryFile.writeText(sb.toString())
        } catch (e: Exception) {
            android.util.Log.w("UrbanCamera", "writeSummaryJson failed: ${e.message}")
        }
    }

    // Copy the contents of a Uri into a temporary file and return it (or null on failure).
    private fun copyUriToTempFile(context: Context, uri: android.net.Uri): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outDir = File(context.cacheDir, "uri_copy")
            if (!outDir.exists()) outDir.mkdirs()
            val outFile = File(outDir, "copied_$ts")
            input.use { ins ->
                outFile.outputStream().use { outs ->
                    ins.copyTo(outs)
                }
            }
            outFile
        } catch (e: Exception) {
            android.util.Log.w("UrbanCamera", "copyUriToTempFile failed: ${e.message}")
            null
        }
    }

    private fun showPhotoPreviewDialog(bitmap: Bitmap) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_photo_preview)
        val imageView = dialog.findViewById<ImageView>(R.id.photoPreviewImageView)
        val closeButton = dialog.findViewById<Button>(R.id.closePreviewButton)
        imageView?.setImageBitmap(bitmap)
        closeButton?.setOnClickListener { dialog.dismiss() }
        dialog.setTitle("Foto scattata")
        dialog.setCancelable(true)
        dialog.show()
    }
    
    // ===== Video Recording =====
    
    private fun toggleVideoRecording() {
        if (isRecording) {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }
    
    private fun startVideoRecording() {
        // TODO: Implement video recording with MediaRecorder
        Toast.makeText(this, "Video recording will be implemented", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopVideoRecording() {
        // TODO: Stop video recording
        Toast.makeText(this, "Stop recording", Toast.LENGTH_SHORT).show()
    }
    
    // ===== Permissions =====
    
    private fun getPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                CAMERA_PERMISSION_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                if (textureView.isAvailable) {
                    openCamera()
                }
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    // ===== Lifecycle =====
    
    override fun onDestroy() {
        super.onDestroy()
        shouldContinueProcessing = false
        model.close()
        cameraDevice?.close()
        imageReader?.close()
        handlerThread.quitSafely()
        FilterManager.removeListener(this)
        saveFilterSettings()
    }



    
    override fun onPause() {
        super.onPause()
        saveFilterSettings()
    }
    
    private fun saveFilterSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // Salva lista filtri attivi
        val activeIds = FilterManager.getActiveFilters().joinToString(",") { it.id }
        editor.putString(KEY_ACTIVE_FILTERS, activeIds)
        // Salva parametri di tutti i filtri
        FilterManager.getAvailableFilters().forEach { filter ->
            val params = filter.parameters.map { (k, v) ->
                "${k}:${v.serialize()}"
            }.joinToString("|")
            editor.putString(KEY_FILTER_PARAMS_PREFIX + filter.id, params)
        }
        editor.apply()
    }
    
    private fun loadFilterSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Ripristina parametri di tutti i filtri
        FilterManager.getAvailableFilters().forEach { filter ->
            val paramsString = prefs.getString(KEY_FILTER_PARAMS_PREFIX + filter.id, null)
            if (!paramsString.isNullOrEmpty()) {
                paramsString.split("|").forEach { entry ->
                    val parts = entry.split(":", limit = 2)
                    if (parts.size == 2 && filter.parameters.containsKey(parts[0])) {
                        filter.parameters[parts[0]]?.deserialize(parts[1])
                    }
                }
            }
        }
        // Ripristina filtri attivi
        val activeIds = prefs.getString(KEY_ACTIVE_FILTERS, null)
        FilterManager.deactivateAll()
        if (!activeIds.isNullOrEmpty()) {
            activeIds.split(",").forEach { id ->
                FilterManager.getAvailableFilters().find { it.id == id }?.let { filter ->
                    FilterManager.activateFilter(filter)
                }
            }
        }
        activeFiltersAdapter?.updateFilters(FilterManager.getActiveFilters())
        availableFiltersAdapter?.notifyDataSetChanged()
    }

    // The CPU YUV encoder path, EGL input-surface encoder, and sequence-stitcher
    // implementations were intentionally removed to simplify the codebase and
    // keep only the working MediaRecorder-based exporter. If you need to
    // reintroduce these paths later, they can be restored from version control.

    // Compute presentation time in nanoseconds for frame index (monotonic helper)
    private fun computePtsNsForFrame(index: Int, fps: Int, startNs: Long = 0L): Long {
        val frameNs = 1_000_000_000L / fps
        return startNs + index * frameNs
    }

    // Validate a produced video file and return duration in ms (or null on error)
    private fun validateVideoFile(file: File?): Long? {
        if (file == null || !file.exists()) return null
        return try {
            val mmr = android.media.MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            mmr.release()
            dur?.toLongOrNull()
        } catch (e: Exception) {
            android.util.Log.w("UrbanCamera", "validateVideoFile failed: ${e.message}")
            null
        }
    }

    // The EGL/MediaCodec input-surface encoder implementation was removed to
    // keep only the MediaRecorder-based exporter. Restore from version control
    // if needed in the future.

    private fun createSimpleTextureProgram(): Int {
        val vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragmentShader = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """.trimIndent()

        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val msg = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Could not link program: $msg")
        }
        return program
    }

    /**
     * Export MP4 using MediaRecorder with an input Surface.
     * This is a robust fallback: configure MediaRecorder, get its input Surface, create an EGL context
     * and render Bitmaps into that Surface at the requested fps. Then stop MediaRecorder and copy file to MediaStore.
     */
    private fun exportFramesWithMediaRecorder(frames: List<Bitmap>, fps: Int): File? {
        if (frames.isEmpty()) return null

        val width = frames[0].width
        val height = frames[0].height
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outDir = File(getExternalFilesDir(null), "UrbanCamera")
        if (!outDir.exists()) outDir.mkdirs()
        val tempFile = File(outDir, "export_rec_$timestamp.mp4")

        var mediaRecorder: MediaRecorder? = null
        var eglDisplay: EGLDisplay? = null
        var eglContext: EGLContext? = null
        var eglSurface: EGLSurface? = null

        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            // Use a higher effective fps for export so frame-by-frame changes are more visible.
            val effectiveFps = fps.coerceAtLeast(60)
            // Use the effective fps when computing bitrate so encoder configuration matches the feed rate.
            mediaRecorder.setVideoEncodingBitRate((width * height * effectiveFps * 0.06).toInt().coerceAtLeast(500_000))
            mediaRecorder.setVideoFrameRate(effectiveFps)
            mediaRecorder.setVideoSize(width, height)
            mediaRecorder.setOutputFile(tempFile.absolutePath)
            mediaRecorder.prepare()
            val recorderSurface = mediaRecorder.surface
            mediaRecorder.start()

            // Setup EGL to render into recorderSurface
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) throw RuntimeException("Unable to get EGL14 display")
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) throw RuntimeException("Unable to initialize EGL14")

            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                0x3142, 1, // EGL_RECORDABLE_ANDROID
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
            val eglConfig = configs[0] ?: throw RuntimeException("Unable to find EGL config")

            val attrib_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, attrib_list, 0)
            if (eglContext == null || eglContext == EGL14.EGL_NO_CONTEXT) throw RuntimeException("Failed to create EGL context")

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, recorderSurface, surfaceAttribs, 0)
            if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) throw RuntimeException("Failed to create EGL surface for MediaRecorder")

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) throw RuntimeException("eglMakeCurrent failed")

            val program = createSimpleTextureProgram()
            GLES20.glUseProgram(program)
            val aPosition = GLES20.glGetAttribLocation(program, "aPosition")
            val aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
            val uTexture = GLES20.glGetUniformLocation(program, "uTexture")

            val vertexData = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
            val texData = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)
            val vb: FloatBuffer = ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertexData).apply { position(0) }
            val tb: FloatBuffer = ByteBuffer.allocateDirect(texData.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(texData).apply { position(0) }

            val texIds = IntArray(1)
            GLES20.glGenTextures(1, texIds, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            val frameIntervalNs = 1_000_000_000L / effectiveFps
            // Use a real monotonic start time (system nano time) for presentation timestamps.
            val startTimeNs = System.nanoTime()
            // Pace frames to the selected fps to give the encoder time to process. This matches real-time
            // spacing and prevents too-fast feeding that some MediaRecorder implementations reject.
            val interFrameSleepMs = (1000L / effectiveFps).coerceAtLeast(5L)
            val desiredFrameCount = (effectiveFps * playbackDurationSeconds).coerceAtLeast(1)
            val totalFramesToRender = kotlin.math.max(frames.size, desiredFrameCount)
            // If we have per-frame seeds and params saved, and the source was an image, we'll
            // re-process the original source image per-seed to produce deterministic frames
            var originalSrcForReplay: Bitmap? = null
            if (playbackFromImage && !mediaPlaybackUri.isNullOrEmpty()) {
                val uri = try { android.net.Uri.parse(mediaPlaybackUri) } catch (_: Exception) { null }
                if (uri != null) {
                    // Try multiple ways to load the original image: MediaStore.getBitmap (older),
                    // then contentResolver.openInputStream + BitmapFactory.decodeStream as a fallback
                    try {
                        originalSrcForReplay = try {
                            android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        } catch (_: Exception) {
                            null
                        }
                        if (originalSrcForReplay == null) {
                            try {
                                contentResolver.openInputStream(uri)?.use { ins ->
                                    originalSrcForReplay = android.graphics.BitmapFactory.decodeStream(ins)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("UrbanCamera", "decodeStream fallback failed: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("UrbanCamera", "Could not load original image for replay: ${e.message}")
                        originalSrcForReplay = null
                    }
                }
            }
            // Ensure we have a seed for every exported frame; generate if missing so frames vary.
            synchronized(framesForExportSeeds) {
                if (framesForExportSeeds.size < totalFramesToRender) {
                    val missing = totalFramesToRender - framesForExportSeeds.size
                    val base = System.nanoTime()
                    for (j in 0 until missing) {
                        framesForExportSeeds.add(base + j + framesForExportSeeds.size)
                    }
                }
            }
            // If a debug dump was previously written, update its summary.json with the (possibly) newly generated seeds.
            try {
                val debugDir = synchronized(this) { lastDebugDumpDir }
                if (debugDir != null) {
                    val snapshotFrames = synchronized(framesForExport) { framesForExport.toList() }
                    val snapshotParams = synchronized(framesForExportParams) { if (framesForExportParams.size >= snapshotFrames.size) framesForExportParams.take(snapshotFrames.size).toList() else framesForExportParams.toList() }
                    val snapshotSeeds = synchronized(framesForExportSeeds) { if (framesForExportSeeds.size >= snapshotFrames.size) framesForExportSeeds.take(snapshotFrames.size).toList() else framesForExportSeeds.toList() }
                    writeSummaryJson(debugDir, snapshotFrames, snapshotParams, snapshotSeeds)
                    android.util.Log.i("UrbanCamera", "Updated debug summary.json with generated seeds at: ${debugDir.absolutePath}")
                }
            } catch (e: Exception) {
                android.util.Log.w("UrbanCamera", "Could not update debug summary with generated seeds: ${e.message}")
            }

            for (i in 0 until totalFramesToRender) {
                var bmp: Bitmap = frames[i % frames.size]
                // If playback came from a single image, prefer to re-render each output frame
                // deterministically using the stored per-frame seed and parameter snapshot so
                // the exported video shows the same frame-by-frame randomness/time animation
                // that the preview displayed.
                if (playbackFromImage && (originalSrcForReplay != null || frames.isNotEmpty())) {
                    val seedForFrame = synchronized(framesForExportSeeds) { if (i < framesForExportSeeds.size) framesForExportSeeds[i] else Long.MIN_VALUE }
                    val paramsForFrame = synchronized(framesForExportParams) { if (i < framesForExportParams.size) framesForExportParams[i] else null }
                    // copy reference to local val to avoid smart-cast issues across threads
                    val origForThisLoop = originalSrcForReplay
                    try {
                        val frameTimeMs = ((startTimeNs + i * frameIntervalNs) / 1_000_000L)
                        FrameClock.setFrameTimeMs(frameTimeMs)
                        val seedToUse = if (seedForFrame != Long.MIN_VALUE) seedForFrame else (System.nanoTime() + i)
                        // Choose a source for re-rendering: prefer original image copy; otherwise use the first collected source/frame
                        val srcForRender = origForThisLoop ?: synchronized(framesForExportSources) { if (framesForExportSources.isNotEmpty()) framesForExportSources[0] else frames[0] }
                        val rendered = try { renderBitmapWithFilters(srcForRender, paramsForFrame, seedToUse) } catch (e: Exception) { android.util.Log.w("UrbanCamera","renderBitmapWithFilters threw: ${e.message}"); null }
                        if (rendered != null) {
                            bmp = rendered
                        } else {
                            // fallback: if rendering failed, ensure we at least use the collected frame
                            bmp = frames[i % frames.size]
                        }
                        android.util.Log.d("UrbanCamera", "Export frame $i seed=$seedToUse timeMs=$frameTimeMs rendered=${rendered != null}")
                    } catch (e: Exception) {
                        android.util.Log.w("UrbanCamera", "Re-render frame $i failed: ${e.message}")
                    }
                } else {
                    // Non-image playback (video source) or missing original: fall back to existing logic
                    val seedForFrame = synchronized(framesForExportSeeds) { if (i < framesForExportSeeds.size) framesForExportSeeds[i] else -1L }
                    val paramsForFrame = synchronized(framesForExportParams) { if (i < framesForExportParams.size) framesForExportParams[i] else null }
                    val origForThisLoop = originalSrcForReplay
                    if (seedForFrame >= 0 && origForThisLoop != null) {
                        try {
                            val frameTimeMs = ((startTimeNs + i * frameIntervalNs) / 1_000_000L)
                            FrameClock.setFrameTimeMs(frameTimeMs)
                            val rendered = renderBitmapWithFilters(origForThisLoop, paramsForFrame, seedForFrame)
                            if (rendered != null) {
                                bmp = rendered
                            }
                            android.util.Log.d("UrbanCamera", "Export frame $i seed=$seedForFrame timeMs=$frameTimeMs rendered=${rendered != null}")
                        } catch (e: Exception) {
                            android.util.Log.w("UrbanCamera", "Re-render frame $i failed: ${e.message}")
                        }
                    }
                }
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0])
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)

                vb.position(0)
                tb.position(0)
                GLES20.glViewport(0, 0, width, height)
                GLES20.glEnableVertexAttribArray(aPosition)
                GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vb)
                GLES20.glEnableVertexAttribArray(aTexCoord)
                GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, tb)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glUniform1i(uTexture, 0)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                GLES20.glFinish()

                // Compute presentation timestamp using the effective fps used to pace frames.
                val pts = startTimeNs + i * frameIntervalNs
                EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, pts)
                val swapOk = EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                if (!swapOk) android.util.Log.w("UrbanCamera", "eglSwapBuffers returned false at frame $i")
                val glErr = GLES20.glGetError()
                if (glErr != GLES20.GL_NO_ERROR) android.util.Log.w("UrbanCamera", "GL error after draw: 0x${Integer.toHexString(glErr)} at frame $i")
                if (i == 0 || i == totalFramesToRender - 1) android.util.Log.i("UrbanCamera", "Rendered frame ${i+1}/${totalFramesToRender}, swapOk=${swapOk}")
                try {
                    Thread.sleep(interFrameSleepMs)
                } catch (_: InterruptedException) {}
            }

            // Give the encoder a small moment to finish processing the last frame, then stop.
            try {
                // ensure GL commands are flushed
                try { GLES20.glFinish() } catch (_: Exception) {}
                try { Thread.sleep(200) } catch (_: Exception) {}
                mediaRecorder.stop()
            } catch (e: Exception) {
                android.util.Log.e("UrbanCamera", "MediaRecorder stop error: ${e.message}", e)
            }
            try { mediaRecorder.release() } catch (_: Exception) {}

            // Log file size and try to read duration for diagnostics
            try {
                val fileLen = tempFile.length()
                var durationMs: Long? = null
                try {
                    val mmr = android.media.MediaMetadataRetriever()
                    mmr.setDataSource(tempFile.absolutePath)
                    val dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durationMs = dur?.toLongOrNull()
                    mmr.release()
                } catch (e: Exception) {
                    android.util.Log.w("UrbanCamera", "Could not read duration from temp file: ${e.message}")
                }
                android.util.Log.i("UrbanCamera", "MediaRecorder output temp file size=${fileLen} bytes durationMs=${durationMs}")
            } catch (e: Exception) {
                android.util.Log.w("UrbanCamera", "Error logging temp file info: ${e.message}")
            }

            // Copy to MediaStore (best-effort) and return the actual temp file so callers can validate it.
            return try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, tempFile.name)
                        put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/UrbanCamera")
                    }
                    val uri = contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { out ->
                            FileInputStream(tempFile).use { fis ->
                                fis.copyTo(out)
                            }
                        }
                        // ensure public folder exists for visibility
                        File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "UrbanCamera").apply { mkdirs() }
                    }
                    // Always return the temp file (it contains the encoded bytes)
                    tempFile
                } else {
                    val publicFolder = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "UrbanCamera")
                    if (!publicFolder.exists()) publicFolder.mkdirs()
                    val dest = File(publicFolder, tempFile.name)
                    tempFile.copyTo(dest, overwrite = true)
                    dest
                }
            } catch (e: Exception) {
                android.util.Log.e("UrbanCamera", "Error copying mediarec mp4 to public store: ${e.message}", e)
                tempFile
            }
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "MediaRecorder MP4 export failed: ${e.message}", e)
            try { mediaRecorder?.release() } catch (_: Exception) {}
            return null
        } finally {
            try { EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT) } catch (_: Exception) {}
            try { if (eglSurface != null && eglDisplay != null) EGL14.eglDestroySurface(eglDisplay, eglSurface) } catch (_: Exception) {}
            try { if (eglContext != null && eglDisplay != null) EGL14.eglDestroyContext(eglDisplay, eglContext) } catch (_: Exception) {}
            try { if (eglDisplay != null) EGL14.eglTerminate(eglDisplay) } catch (_: Exception) {}
        }
    }

    // Sequence + stitch helper removed. Use the MediaRecorder exporter directly.

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val msg = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Could not compile shader $type: $msg")
        }
        return shader
    }
}
