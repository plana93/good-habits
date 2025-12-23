package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.model.Exercise
import com.programminghut.pose_detection.ui.exercise.ExerciseCreationWizardScreen
import com.programminghut.pose_detection.ui.exercise.ExerciseWizardData
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Activity per la creazione guidata di esercizi custom
 */
class ExerciseCreationActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "ExerciseCreationActivity"
    }
    
    private lateinit var database: AppDatabase
    private var wizardData by mutableStateOf(ExerciseWizardData())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = AppDatabase.getDatabase(applicationContext)
        
        setContent {
            Pose_detectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExerciseCreationWizardScreen(
                        wizardData = wizardData,
                        onDataChanged = { newData ->
                            wizardData = newData
                        },
                        onComplete = { finalData ->
                            Log.d(TAG, "Wizard completed: ${finalData.name}")
                            saveExercise(finalData)
                        },
                        onCancel = {
                            Log.d(TAG, "Wizard cancelled")
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    private fun saveExercise(wizardData: com.programminghut.pose_detection.ui.exercise.ExerciseWizardData) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Saving exercise: ${wizardData.name}")
                Log.d(TAG, "Mode: ${wizardData.mode}")
                Log.d(TAG, "Image path: ${wizardData.imagePath}")
                
                val exercise = Exercise(
                    exerciseId = 0,
                    name = wizardData.name,
                    type = wizardData.type,
                    description = wizardData.description,
                    mode = wizardData.mode ?: com.programminghut.pose_detection.data.model.ExerciseMode.REPS,
                    imagePath = wizardData.imagePath,
                    tags = listOf(wizardData.mode?.name ?: "CUSTOM"),
                    isCustom = true
                )
                
                val exerciseId = database.exerciseDao().insertExercise(exercise)
                Log.d(TAG, "✅ Exercise saved successfully with ID: $exerciseId")
                
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@ExerciseCreationActivity,
                        "✅ Esercizio '${wizardData.name}' salvato!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    
                    // Torna indietro con successo
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving exercise", e)
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this@ExerciseCreationActivity,
                        "❌ Errore nel salvataggio: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }
}
