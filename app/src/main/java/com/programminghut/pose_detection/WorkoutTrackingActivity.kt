package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.programminghut.pose_detection.data.model.Exercise
import com.programminghut.pose_detection.data.model.ExerciseMode
import com.programminghut.pose_detection.data.model.WorkoutExercise
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.repository.ExerciseRepository
import com.programminghut.pose_detection.data.repository.WorkoutRepository
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import kotlinx.coroutines.launch

/**
 * Activity per eseguire un workout creato.
 * 
 * Permette di:
 * - Visualizzare la lista degli esercizi del workout
 * - Eseguire ciascun esercizio con tracking automatico (squat) o manuale
 * - Registrare i valori effettivi ottenuti
 * - Salvare la sessione completa nel database
 */
class WorkoutTrackingActivity : ComponentActivity() {
    
    private val viewModel: WorkoutTrackingViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val exerciseRepository = ExerciseRepository(database.exerciseDao())
        val workoutRepository = WorkoutRepository(database.workoutDao())
        WorkoutTrackingViewModelFactory(workoutRepository, exerciseRepository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val workoutId = intent.getLongExtra("WORKOUT_ID", 0)
        if (workoutId == 0L) {
            finish()
            return
        }
        
        setContent {
            Pose_detectionTheme {
                WorkoutTrackingScreen(
                    workoutId = workoutId,
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onExerciseStart = { exercise, workoutExercise ->
                        startExercise(exercise, workoutExercise)
                    },
                    onWorkoutComplete = { sessionId ->
                        // Navigate to session details or back to main menu
                        finish()
                    }
                )
            }
        }
    }
    
    private fun startExercise(exercise: Exercise, workoutExercise: WorkoutExercise) {
        when (exercise.name.uppercase()) {
            "SQUAT" -> {
                // Navigate to camera-based squat detection
                val intent = Intent(this, CameraSelectionActivity::class.java)
                intent.putExtra("EXERCISE_ID", exercise.exerciseId)
                intent.putExtra("EXERCISE_NAME", exercise.name)
                intent.putExtra("TARGET_REPS", workoutExercise.targetReps)
                startActivity(intent)
            }
            else -> {
                // Navigate to manual exercise tracking
                val intent = Intent(this, ManualExerciseActivity::class.java)
                intent.putExtra("EXERCISE_ID", exercise.exerciseId)
                intent.putExtra("EXERCISE_NAME", exercise.name)
                intent.putExtra("EXERCISE_MODE", exercise.mode.name)
                val targetValue = when (exercise.mode) {
                    ExerciseMode.REPS -> workoutExercise.targetReps
                    ExerciseMode.TIME -> workoutExercise.targetTime
                }
                intent.putExtra("TARGET_VALUE", targetValue)
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackingScreen(
    workoutId: Long,
    onNavigateBack: () -> Unit,
    onExerciseStart: (Exercise, WorkoutExercise) -> Unit,
    onWorkoutComplete: (Long) -> Unit,
    viewModel: WorkoutTrackingViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("Indietro")
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.workoutName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Progresso: ${uiState.completedExercises}/${uiState.totalExercises}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            TextButton(
                onClick = {
                    scope.launch {
                        val sessionId = viewModel.completeWorkout()
                        if (sessionId > 0) {
                            onWorkoutComplete(sessionId)
                        }
                    }
                },
                enabled = uiState.canCompleteWorkout
            ) {
                Text("Termina")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Bar
        LinearProgressIndicator(
            progress = if (uiState.totalExercises > 0) {
                uiState.completedExercises.toFloat() / uiState.totalExercises.toFloat()
            } else 0f,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Exercise List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.exerciseResults) { exerciseResult ->
                    WorkoutExerciseTrackingCard(
                        exerciseResult = exerciseResult,
                        onStartExercise = { exercise, workoutExercise ->
                            onExerciseStart(exercise, workoutExercise)
                        },
                        onUpdateResult = { actualValue ->
                            viewModel.updateExerciseResult(exerciseResult.exercise.exerciseId, actualValue)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutExerciseTrackingCard(
    exerciseResult: ExerciseTrackingResult,
    onStartExercise: (Exercise, WorkoutExercise) -> Unit,
    onUpdateResult: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseResult.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (exerciseResult.exercise.description.isNotBlank()) {
                        Text(
                            text = exerciseResult.exercise.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    val targetValue = when (exerciseResult.exercise.mode) {
                        ExerciseMode.REPS -> exerciseResult.workoutExercise.targetReps
                        ExerciseMode.TIME -> exerciseResult.workoutExercise.targetTime
                    }
                    Text(
                        text = when (exerciseResult.exercise.mode) {
                            ExerciseMode.REPS -> "Target: $targetValue ripetizioni"
                            ExerciseMode.TIME -> "Target: $targetValue secondi"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Status Icon
                if (exerciseResult.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completato",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Results Input/Display
            if (exerciseResult.isCompleted) {
                // Show results
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Eseguito:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${exerciseResult.actualValue} ${when (exerciseResult.exercise.mode) {
                                ExerciseMode.REPS -> "reps"
                                ExerciseMode.TIME -> "sec"
                            }}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Allow editing result
                    var newValue by remember { mutableStateOf(exerciseResult.actualValue?.toString() ?: "") }
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { value ->
                            newValue = value
                            value.toIntOrNull()?.let { onUpdateResult(it) }
                        },
                        label = { Text("Modifica") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }
            } else {
                // Start exercise button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onStartExercise(exerciseResult.exercise, exerciseResult.workoutExercise)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inizia Esercizio")
                    }
                    
                    // Manual input for non-camera exercises
                    if (exerciseResult.exercise.name.uppercase() != "SQUAT") {
                        var manualValue by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = manualValue,
                            onValueChange = { value ->
                                manualValue = value
                                value.toIntOrNull()?.let { onUpdateResult(it) }
                            },
                            label = { Text("Inserisci manualmente") },
                            modifier = Modifier.width(150.dp),
                            singleLine = true,
                            placeholder = {
                                Text(when (exerciseResult.exercise.mode) {
                                    ExerciseMode.REPS -> "reps"
                                    ExerciseMode.TIME -> "sec"
                                })
                            }
                        )
                    }
                }
            }
        }
    }
}