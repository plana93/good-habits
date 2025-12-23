package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.model.Exercise
import com.programminghut.pose_detection.data.model.ExerciseType
// import com.programminghut.pose_detection.data.manager.ExercisePresetManager // DISABLED - TODO: Update for new schema
import com.programminghut.pose_detection.ui.exercise.ExerciseBrowserScreen
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Exercise Selector Activity
 * 
 * Mostra la lista degli esercizi disponibili e permette di:
 * - Scegliere un esercizio da avviare
 * - Creare nuovi esercizi custom
 */
class ExerciseSelectorActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "ExerciseSelectorActivity"
    }
    
    private lateinit var database: AppDatabase
    // private lateinit var presetManager: ExercisePresetManager // DISABLED
    private var shouldRefresh by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database
        database = AppDatabase.getDatabase(applicationContext)
        
        // TODO: Re-enable preset manager after updating for new schema
        // presetManager = ExercisePresetManager(
        //     exerciseDao = database.exerciseDao(),
        //     context = applicationContext
        // )
        
        // Initialize presets in background BEFORE showing UI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Skipping preset initialization (disabled)")
                // presetManager.initializePresetsIfNeeded()
                Log.d(TAG, "Ready to show UI")
                // Once ready, show UI on main thread
                runOnUiThread {
                    setupUI()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during setup", e)
                e.printStackTrace()
                // Show UI anyway to avoid black screen
                runOnUiThread {
                    setupUI()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Trigger refresh when returning from ExerciseCreationActivity
        Log.d(TAG, "onResume: triggering refresh")
        shouldRefresh = !shouldRefresh
    }
    
    private fun setupUI() {
        Log.d(TAG, "Setting up UI...")
        setContent {
            // Trigger recomposition when shouldRefresh changes
            val refreshKey = shouldRefresh
            Log.d(TAG, "Composing UI with refresh key: $refreshKey")
            
            Pose_detectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExerciseBrowserScreen(
                        exercisesFlow = getAllExercises(),
                        onBackClick = { 
                            Log.d(TAG, "Back button clicked")
                            finish() 
                        },
                        onExerciseSelected = { exercise ->
                            Log.d(TAG, "Exercise selected: ${exercise.name}")
                            // Mostra dettagli esercizio (già gestito internamente)
                        },
                        onStartExercise = { exercise ->
                            Log.d(TAG, "Starting exercise: ${exercise.name}")
                            // Avvia l'esercizio selezionato
                            startExercise(exercise)
                        },
                        onCreateCustomExercise = {
                            Log.d(TAG, "Create custom exercise clicked")
                            // Apri wizard creazione esercizio
                            val intent = Intent(this@ExerciseSelectorActivity, ExerciseCreationActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Recupera tutti gli esercizi dal database
     */
    private fun getAllExercises(): Flow<List<Exercise>> {
        return database.exerciseDao().getAllExercises()
    }
    
    /**
     * Avvia l'esercizio selezionato
     * Passa alla calibrazione/selezione camera
     */
    private fun startExercise(exercise: Exercise) {
        when (exercise.type) {
            ExerciseType.SQUAT -> {
                // Per squat usa il flusso esistente
                val intent = Intent(this, Squat::class.java)
                intent.putExtra("EXERCISE_ID", exercise.exerciseId)
                intent.putExtra("EXERCISE_NAME", exercise.name)
                startActivity(intent)
            }
            else -> {
                // Per altri esercizi, vai direttamente alla selezione camera
                // TODO: In futuro aggiungere calibrazione custom
                val intent = Intent(this, CameraSelectionActivity::class.java)
                intent.putExtra("EXERCISE_ID", exercise.exerciseId)
                intent.putExtra("EXERCISE_NAME", exercise.name)
                intent.putExtra("EXERCISE_TYPE", exercise.type.name)
                startActivity(intent)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}
