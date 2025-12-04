package com.programminghut.pose_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.programminghut.pose_detection.adapters.AvailableFiltersAdapter
import com.programminghut.pose_detection.adapters.ActiveFiltersAdapter
import com.programminghut.pose_detection.ui.FilterParamsBottomSheet
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.pose_detection.effects.FilterManager
import org.tensorflow.lite.support.image.ImageProcessor

/**
 * Attività di preview per media caricati (immagini o video).
 * - Se mediaType == "image": carica bitmap, applica filtri e mostra anteprima.
 * - Se mediaType == "video": semplice anteprima con estrazione frame a intervalli e applicazione filtri.
 *
 * Nota: l'implementazione video è una versione leggera che estrae frame periodicamente
 * con MediaMetadataRetriever e li mostra nell'ImageView applicando i filtri attivi.
 */
class MediaUploadPreviewActivity : AppCompatActivity(), FilterManager.FilterChangeListener {
    private lateinit var previewImageView: ImageView
    private lateinit var infoText: TextView
    private lateinit var playSimButton: Button
    private lateinit var progress: ProgressBar

    private var mediaUriString: String? = null
    private var mediaType: String = "image"
    private var kSeconds: Int = 5

    private val handlerThread = HandlerThread("mediaPreviewThread")
    private lateinit var backgroundHandler: Handler

    private var originalBitmap: Bitmap? = null
    private var running = false
    private var desiredVideoDurationSeconds: Int = 0

    // UI - filters
    private var availableFiltersAdapter: AvailableFiltersAdapter? = null
    private var activeFiltersAdapter: ActiveFiltersAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_upload_preview)

        previewImageView = findViewById(R.id.mediaPreviewImageView)
        infoText = findViewById(R.id.mediaInfoText)
        playSimButton = findViewById(R.id.playSimButton)
        progress = findViewById(R.id.mediaProgress)

        mediaUriString = intent.getStringExtra("mediaUri")
        mediaType = intent.getStringExtra("mediaType") ?: "image"
        kSeconds = intent.getIntExtra("kSeconds", 5)

        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)

        FilterManager.addListener(this)

        playSimButton.setOnClickListener {
            // Open the refactored UrbanCameraActivity in media playback mode so user can edit filters using same UI
            val intent = android.content.Intent(this, UrbanCameraActivityRefactored::class.java)
            intent.putExtra("isMediaPlayback", true)
            intent.putExtra("mediaPlaybackUri", mediaUriString)
            intent.putExtra("playbackFromImage", mediaType == "image")
            // Use the appropriate duration based on media type
            val duration = if (mediaType == "image") kSeconds else desiredVideoDurationSeconds
            intent.putExtra("playbackDurationSeconds", duration)
            startActivity(intent)
            // Don't call finish() so the user can return to this activity after export
        }

        // Initialize filter recyclers
        setupFilterRecyclers()

        // Duration controls
        val secondsLabel = findViewById<TextView>(R.id.secondsLabel)
        val durationSeekBar = findViewById<SeekBar>(R.id.durationSeekBar)
        // default values
        if (mediaType == "image") {
            desiredVideoDurationSeconds = kSeconds
        } else {
            // for video default, we'll set it to 5s until metadata is available
            desiredVideoDurationSeconds = 5
        }
        secondsLabel.text = "Duration: ${desiredVideoDurationSeconds}s"
        durationSeekBar.progress = desiredVideoDurationSeconds
        durationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sec = progress.coerceAtLeast(1)
                secondsLabel.text = "Duration: ${sec}s"
                if (mediaType == "image") {
                    kSeconds = sec
                } else {
                    desiredVideoDurationSeconds = sec
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        loadAndShow()
    }

    private fun loadAndShow() {
        val uriStr = mediaUriString ?: run {
            finish()
            return
        }
        val uri = Uri.parse(uriStr)
        progress.visibility = View.VISIBLE
        backgroundHandler.post {
            try {
                if (mediaType == "image") {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        originalBitmap = BitmapFactory.decodeStream(stream)
                    }
                    runOnUiThread {
                        progress.visibility = View.GONE
                        applyFiltersAndShow()
                        infoText.text = "Image preview — puoi modificare i filtri"
                    }
                } else {
                    // Video: per semplicità mostriamo il primo frame processato e permettiamo il loop su richiesta
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(this, uri)
                    val frame = retriever.getFrameAtTime(0)
                    originalBitmap = frame
                    retriever.release()
                    runOnUiThread {
                        progress.visibility = View.GONE
                        applyFiltersAndShow()
                        infoText.text = "Video preview — premi Play per far partire l'elaborazione dei frame"
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaPreview", "Errore caricamento media: ${e.message}", e)
                runOnUiThread {
                    progress.visibility = View.GONE
                    Toast.makeText(this, "Errore caricamento media", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun applyFiltersAndShow() {
        val src = originalBitmap ?: return
        try {
            val displayBitmap = src.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(displayBitmap)
            // Applichiamo i filtri attivi (senza keypoints)
            FilterManager.applyFilters(canvas, src, null)
            previewImageView.setImageBitmap(displayBitmap)
        } catch (e: Exception) {
            Log.e("MediaPreview", "Errore apply filters: ${e.message}", e)
        }
    }

    private fun simulateImageAsVideo() {
        val durationMs = (kSeconds * 1000).coerceAtLeast(1000)
        val frameInterval = 1000L / 30L // 30 fps simulation
        val frames = (durationMs / frameInterval).toInt().coerceAtLeast(1)
        running = true
        playSimButton.isEnabled = false
        backgroundHandler.post(object : Runnable {
            var count = 0
            override fun run() {
                if (!running || count >= frames) {
                    runOnUiThread { playSimButton.isEnabled = true }
                    running = false
                    return
                }
                applyFiltersAndShow()
                count++
                backgroundHandler.postDelayed(this, frameInterval)
            }
        })
    }

    private fun startVideoFrameLoop() {
        val uriStr = mediaUriString ?: return
        val uri = Uri.parse(uriStr)
        running = true
        playSimButton.isEnabled = false
        backgroundHandler.post(object : Runnable {
            var positionUs = 0L
            override fun run() {
                if (!running) {
                    runOnUiThread { playSimButton.isEnabled = true }
                    return
                }
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(this@MediaUploadPreviewActivity, uri)
                    val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    // estrai un frame a intervallo di ~100ms
                    val frame = retriever.getFrameAtTime(positionUs)
                    retriever.release()
                    if (frame != null) {
                        originalBitmap = frame
                        runOnUiThread { applyFiltersAndShow() }
                    }
                    if (durationMs > 0) {
                        positionUs += 100_000L // 100ms
                        if (positionUs / 1000L > durationMs) positionUs = 0L
                    }
                } catch (e: Exception) {
                    Log.e("MediaPreview", "Errore estrazione frame: ${e.message}", e)
                }
                backgroundHandler.postDelayed(this, 100)
            }
        })
    }

    override fun onFilterActivated(filter: com.programminghut.pose_detection.effects.AdaptiveFilter) {
        // Reapplica i filtri sulla UI e aggiorna liste
        runOnUiThread {
            applyFiltersAndShow()
            activeFiltersAdapter?.updateFilters(FilterManager.getActiveFilters())
            availableFiltersAdapter?.updateFilter(filter)
        }
    }

    override fun onFilterDeactivated(filter: com.programminghut.pose_detection.effects.AdaptiveFilter) {
        runOnUiThread {
            applyFiltersAndShow()
            activeFiltersAdapter?.updateFilters(FilterManager.getActiveFilters())
            availableFiltersAdapter?.updateFilter(filter)
        }
    }

    override fun onFiltersReordered(filters: List<com.programminghut.pose_detection.effects.AdaptiveFilter>) {
        runOnUiThread {
            applyFiltersAndShow()
            activeFiltersAdapter?.updateFilters(filters)
        }
    }

    override fun onAllFiltersDeactivated() {
        runOnUiThread {
            applyFiltersAndShow()
            activeFiltersAdapter?.updateFilters(emptyList())
            availableFiltersAdapter?.notifyDataSetChanged()
        }
    }

    private fun setupFilterRecyclers() {
        val availableRecycler = findViewById<RecyclerView>(R.id.availableFiltersRecycler)
        val activeRecycler = findViewById<RecyclerView>(R.id.activeFiltersRecycler)

        availableRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        activeRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        availableFiltersAdapter = AvailableFiltersAdapter(
            filters = FilterManager.getAvailableFilters(),
            onFilterClick = { filter ->
                if (filter.isActive) {
                    FilterManager.deactivateFilter(filter)
                } else {
                    FilterManager.activateFilter(filter)
                }
                // refresh adapters
                activeFiltersAdapter?.updateFilters(FilterManager.getActiveFilters())
                availableFiltersAdapter?.updateFilter(filter)
                // reapply filters to preview
                applyFiltersAndShow()
            },
            onFilterLongClick = { filter ->
                // open params bottom sheet
                val bottomSheet = FilterParamsBottomSheet(this, filter) {
                    // on changed
                    applyFiltersAndShow()
                }
                bottomSheet.show()
            }
        )

        activeFiltersAdapter = ActiveFiltersAdapter(
            onFilterRemove = { filter ->
                FilterManager.deactivateFilter(filter)
                activeFiltersAdapter?.updateFilters(FilterManager.getActiveFilters())
                availableFiltersAdapter?.notifyDataSetChanged()
                applyFiltersAndShow()
            },
            onFilterLongClick = { filter ->
                val bottomSheet = FilterParamsBottomSheet(this, filter) {
                    applyFiltersAndShow()
                }
                bottomSheet.show()
            }
        )

        availableRecycler.adapter = availableFiltersAdapter
        activeRecycler.adapter = activeFiltersAdapter

        // initialize active list
        activeFiltersAdapter?.updateFilters(FilterManager.getActiveFilters())
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        handlerThread.quitSafely()
        FilterManager.removeListener(this)
    }
}
