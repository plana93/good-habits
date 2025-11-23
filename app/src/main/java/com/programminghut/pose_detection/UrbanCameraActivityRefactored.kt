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
import com.programminghut.pose_detection.filters.FilterManager
import com.programminghut.pose_detection.ml.LiteModelMovenetSingleposeLightningTfliteFloat164
import com.programminghut.pose_detection.ui.FilterParamsBottomSheet
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity refactorizzata con sistema di filtri adattivi modulare
 */
class UrbanCameraActivityRefactored : AppCompatActivity(), FilterManager.FilterChangeListener {
    private val PREFS_NAME = "urban_camera_filter_prefs"
    private val KEY_ACTIVE_FILTERS = "active_filters"
    private val KEY_FILTER_PARAMS_PREFIX = "filter_params_"
    
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
    private var imageReader: ImageReader? = null
    
    // UI Components
    private lateinit var leftMenuContainer: FrameLayout
    private lateinit var rightMenuContainer: FrameLayout
    private lateinit var toggleLeftMenuButton: ImageButton
    private lateinit var toggleRightMenuButton: ImageButton
    private lateinit var captureButton: View
    private lateinit var videoButton: View
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
    
    // Latest frame data
    private var latestProcessedBitmap: Bitmap? = null
    private var latestPoseKeypoints: FloatArray? = null
    
    // Processing state
    private var isProcessingFrame = false
    private var shouldContinueProcessing = true
    private var parametersUpdatePending = false
    private val parameterUpdateRunnable = Runnable {
        parametersUpdatePending = false
    }
    
    private val CAMERA_PERMISSION_CODE = 100
    private val WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 101
    
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
        
        initializeComponents()
        initializeFilterSystem()
        initializeUI()
        loadFilterSettings()
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
        captureButton.setOnClickListener { capturePhoto() }
        videoButton.setOnClickListener { toggleVideoRecording() }
        clearAllFiltersButton.setOnClickListener { 
            FilterManager.deactivateAll()
            activeFiltersAdapter.notifyDataSetChanged()
            availableFiltersAdapter.notifyDataSetChanged()
        }
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
    
    private fun showFilterParamsDialog(filter: com.programminghut.pose_detection.filters.AdaptiveFilter) {
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
    
    override fun onFilterActivated(filter: com.programminghut.pose_detection.filters.AdaptiveFilter) {
        runOnUiThread {
            activeFiltersAdapter.updateFilters(FilterManager.getActiveFilters())
            availableFiltersAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onFilterDeactivated(filter: com.programminghut.pose_detection.filters.AdaptiveFilter) {
        runOnUiThread {
            activeFiltersAdapter.updateFilters(FilterManager.getActiveFilters())
            availableFiltersAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onFiltersReordered(filters: List<com.programminghut.pose_detection.filters.AdaptiveFilter>) {
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
            // Perform pose detection
            val tensorImage = TensorImage.fromBitmap(sourceBitmap)
            val processedImage = imageProcessor.process(tensorImage)
            val outputs = model.process(processedImage.tensorBuffer)
            val normalizedKeypoints = outputs.outputFeature0AsTensorBuffer.floatArray
            if (normalizedKeypoints.isEmpty()) {
                android.util.Log.w("UrbanCamera", "Keypoints vuoti dal modello in processFrame")
            }
            val scaledKeypoints = FloatArray(normalizedKeypoints.size)
            for (i in normalizedKeypoints.indices step 3) {
                scaledKeypoints[i] = normalizedKeypoints[i + 1] * sourceBitmap.width
                scaledKeypoints[i + 1] = normalizedKeypoints[i] * sourceBitmap.height
                scaledKeypoints[i + 2] = normalizedKeypoints[i + 2]
            }
            latestPoseKeypoints = scaledKeypoints
            val outputBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(outputBitmap)
            FilterManager.applyFilters(canvas, sourceBitmap, scaledKeypoints)
            runOnUiThread {
                imageView.setImageBitmap(outputBitmap)
            }
        } catch (e: Exception) {
            android.util.Log.e("UrbanCamera", "Error in processFrame: ${e.message}", e)
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
                "${'$'}k:${'$'}{v.serialize()}"
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
}
