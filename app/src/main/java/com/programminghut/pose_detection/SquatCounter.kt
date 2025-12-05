package com.programminghut.pose_detection

import android.content.Context
import android.os.Build
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Gestisce il salvataggio e caricamento del conteggio totale degli squat
 * 
 * Updated for Phase 1: Now integrates with Room database to save workout sessions
 * while maintaining backward compatibility with file-based total count.
 */
class SquatCounter(private val context: Context) {
    
    companion object {
        private const val SQUAT_COUNT_FILE = "squat_total_count.txt"
        private const val TAG = "SquatCounter"
        private const val EXERCISE_TYPE_SQUAT = "SQUAT"
    }
    
    private var totalSquats: Int = 0
    private var currentSessionSquats: Int = 0
    
    // Room database integration
    private val database: AppDatabase = AppDatabase.getDatabase(context)
    private val repository: SessionRepository = SessionRepository(
        database.sessionDao(),
        database.repDao()
    )
    
    // Session tracking for Room database
    private var sessionStartTime: Long = 0
    private val sessionReps = mutableListOf<RepData>()
    private var currentRepStartTime: Long = 0
    
    init {
        // Carica il totale salvato all'inizializzazione
        loadTotalSquats()
        startNewSession()
    }
    
    /**
     * Start a new workout session
     */
    private fun startNewSession() {
        sessionStartTime = System.currentTimeMillis()
        sessionReps.clear()
        currentRepStartTime = 0
        android.util.Log.d(TAG, "Nuova sessione avviata")
    }
    
    /**
     * Carica il totale degli squat dal file locale
     * @return Il numero totale di squat salvati
     */
    fun loadTotalSquats(): Int {
        try {
            val file = File(context.filesDir, SQUAT_COUNT_FILE)
            if (file.exists()) {
                val content = file.readText().trim()
                totalSquats = content.toIntOrNull() ?: 0
                android.util.Log.d(TAG, "Caricati $totalSquats squat totali dal file")
            } else {
                totalSquats = 0
                android.util.Log.d(TAG, "File non esistente, totale impostato a 0")
            }
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Errore nel caricamento degli squat: ${e.message}")
            totalSquats = 0
        } catch (e: NumberFormatException) {
            android.util.Log.e(TAG, "Formato numero non valido nel file: ${e.message}")
            totalSquats = 0
        }
        return totalSquats
    }
    
    /**
     * Salva il totale degli squat nel file locale
     * @return true se il salvataggio ha avuto successo
     */
    fun saveTotalSquats(): Boolean {
        return try {
            val file = File(context.filesDir, SQUAT_COUNT_FILE)
            file.writeText(totalSquats.toString())
            android.util.Log.d(TAG, "Salvati $totalSquats squat totali nel file: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Errore nel salvataggio degli squat: ${e.message}")
            false
        }
    }
    
    /**
     * Incrementa il conteggio degli squat
     * 
     * Updated: Now also tracks individual rep data for database storage
     * 
     * @param depthScore Depth quality score (0.0-1.0), default 0.5
     * @param formScore Form quality score (0.0-1.0), default 0.5
     */
    fun incrementSquat(depthScore: Float = 0.5f, formScore: Float = 0.5f) {
        totalSquats++
        currentSessionSquats++
        
        // Calculate rep duration
        val currentTime = System.currentTimeMillis()
        val repDuration = if (currentRepStartTime > 0) {
            (currentTime - currentRepStartTime) / 1000f // in seconds
        } else {
            2.0f // default 2 seconds for first rep
        }
        currentRepStartTime = currentTime
        
        // Create rep data for database
        val repData = RepData(
            sessionId = 0, // Will be set when session is saved
            repNumber = currentSessionSquats,
            timestamp = currentTime,
            depthScore = depthScore,
            formScore = formScore,
            speed = repDuration,
            confidence = 0.8f, // Could be passed from pose detection
            keypoints = null, // Could store full pose data if needed
            isFlaggedForReview = formScore < 0.6f || depthScore < 0.6f
        )
        
        sessionReps.add(repData)
        
        // Salva automaticamente ad ogni incremento per sicurezza
        saveTotalSquats()
        android.util.Log.d(TAG, "Squat incrementato: sessione=$currentSessionSquats, totale=$totalSquats, qualità form=$formScore, depth=$depthScore")
    }
    
    /**
     * Ottiene il totale degli squat
     */
    fun getTotalSquats(): Int = totalSquats
    
    /**
     * Ottiene gli squat della sessione corrente
     */
    fun getCurrentSessionSquats(): Int = currentSessionSquats
    
    /**
     * Resetta il conteggio della sessione corrente (ma mantiene il totale)
     * 
     * Updated: Now saves session to database before resetting
     */
    fun resetSessionSquats() {
        // Save session to database before resetting
        saveCurrentSessionToDatabase()
        
        currentSessionSquats = 0
        android.util.Log.d(TAG, "Conteggio sessione resettato")
        
        // Start a new session
        startNewSession()
    }
    
    /**
     * Save current session to Room database
     * 
     * This is called when:
     * - User exits the workout
     * - User resets session counter
     * - App is closing
     */
    private fun saveCurrentSessionToDatabase() {
        // Only save if there were actual reps in this session
        if (currentSessionSquats == 0 || sessionReps.isEmpty()) {
            android.util.Log.d(TAG, "Nessun rep da salvare, sessione vuota")
            return
        }
        
        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - sessionStartTime) / 1000).toInt()
        
        // Calculate average scores from reps
        val avgDepth = sessionReps.map { it.depthScore }.average().toFloat()
        val avgForm = sessionReps.map { it.formScore }.average().toFloat()
        val avgSpeed = sessionReps.map { it.speed }.average().toFloat()
        
        // Get app version
        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        
        // Get device model
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        
        // Create workout session
        val session = WorkoutSession(
            startTime = sessionStartTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            exerciseType = EXERCISE_TYPE_SQUAT,
            totalReps = currentSessionSquats,
            avgDepthScore = avgDepth,
            avgFormScore = avgForm,
            avgSpeed = avgSpeed,
            notes = null,
            tags = emptyList(),
            location = null,
            appVersion = appVersion,
            deviceModel = deviceModel,
            isSynced = false,
            exportedAt = null
        )
        
        // Save to database in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionId = repository.insertCompleteWorkout(session, sessionReps)
                withContext(Dispatchers.Main) {
                    android.util.Log.d(TAG, "Sessione salvata nel database con ID: $sessionId")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e(TAG, "Errore salvataggio sessione nel database: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Resetta completamente il conteggio (totale e sessione)
     * Usare con cautela!
     */
    fun resetAllSquats() {
        totalSquats = 0
        currentSessionSquats = 0
        saveTotalSquats()
        android.util.Log.d(TAG, "Conteggio totale resettato a 0")
        
        // Start a new session
        startNewSession()
    }
    
    /**
     * Da chiamare quando l'app viene chiusa per salvare i dati
     */
    fun onAppClosing() {
        // Save current session if it has reps
        saveCurrentSessionToDatabase()
        
        // Save total count to file
        saveTotalSquats()
        android.util.Log.d(TAG, "Salvataggio finale prima della chiusura dell'app")
    }
    
    /**
     * Get the repository for accessing database operations directly
     * Useful for UI components that want to display session history
     */
    fun getRepository(): SessionRepository = repository
}
