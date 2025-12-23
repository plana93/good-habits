package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.programminghut.pose_detection.data.model.Workout
import com.programminghut.pose_detection.data.model.WorkoutExercise
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.data.repository.ExerciseRepository
import com.programminghut.pose_detection.data.repository.WorkoutRepository
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import kotlinx.coroutines.launch

/**
 * Activity per creare collezioni di workout.
 * 
 * Permette di:
 * - Creare un nuovo workout con nome e descrizione
 * - Aggiungere esercizi esistenti al workout
 * - Creare nuovi esercizi on-the-fly
 * - Impostare target specifici (reps o tempo) per ogni esercizio nel workout
 * - Salvare il workout completo nel database
 */
class WorkoutCreationActivity : ComponentActivity() {
    
    private val viewModel: WorkoutCreationViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val exerciseRepository = ExerciseRepository(database.exerciseDao())
        val workoutRepository = WorkoutRepository(database.workoutDao())
        WorkoutCreationViewModelFactory(exerciseRepository, workoutRepository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Pose_detectionTheme {
                WorkoutCreationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onCreateExercise = { navigateToExerciseCreation() },
                    onWorkoutSaved = { workoutId ->
                        // Navigate to workout tracking
                        val intent = Intent(this@WorkoutCreationActivity, WorkoutTrackingActivity::class.java)
                        intent.putExtra("WORKOUT_ID", workoutId)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
    
    private fun navigateToExerciseCreation() {
        val intent = Intent(this, ExerciseCreationActivity::class.java)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCreationScreen(
    onNavigateBack: () -> Unit,
    onCreateExercise: () -> Unit,
    onWorkoutSaved: (Long) -> Unit,
    viewModel: WorkoutCreationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        viewModel.loadExercises()
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
                Text("Annulla")
            }
            
            Text(
                text = "Nuovo Workout",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(
                onClick = {
                    scope.launch {
                        val workoutId = viewModel.saveWorkout()
                        if (workoutId > 0) {
                            onWorkoutSaved(workoutId)
                        }
                    }
                },
                enabled = uiState.workoutName.isNotBlank() && uiState.workoutExercises.isNotEmpty()
            ) {
                Text("Salva")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Workout Details
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.workoutName,
                    onValueChange = viewModel::updateWorkoutName,
                    label = { Text("Nome Workout") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.workoutDescription,
                    onValueChange = viewModel::updateWorkoutDescription,
                    label = { Text("Descrizione (opzionale)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Exercises in Workout
        Text(
            text = "Esercizi nel Workout (${uiState.workoutExercises.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (uiState.workoutExercises.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nessun esercizio aggiunto",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aggiungi esercizi dal catalogo o creane di nuovi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.workoutExercises) { workoutExercise ->
                    WorkoutExerciseCard(
                        workoutExercise = workoutExercise,
                        onUpdateTarget = { updatedExercise ->
                            viewModel.updateWorkoutExercise(updatedExercise)
                        },
                        onRemove = {
                            viewModel.removeWorkoutExercise(workoutExercise.exercise.exerciseId)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add Exercise Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.showExerciseSelection() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi Esistente")
            }
            
            Button(
                onClick = onCreateExercise,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crea Nuovo")
            }
        }
    }
    
    // Exercise Selection Dialog
    if (uiState.showExerciseSelection) {
        ExerciseSelectionDialog(
            exercises = uiState.availableExercises,
            onExerciseSelected = { exercise ->
                viewModel.addExerciseToWorkout(exercise)
            },
            onDismiss = { viewModel.hideExerciseSelection() }
        )
    }
}

@Composable
fun WorkoutExerciseCard(
    workoutExercise: WorkoutExerciseWithDetails,
    onUpdateTarget: (WorkoutExerciseWithDetails) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workoutExercise.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (workoutExercise.exercise.description.isNotBlank()) {
                        Text(
                            text = workoutExercise.exercise.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Rimuovi esercizio",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Target Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (workoutExercise.exercise.mode) {
                        ExerciseMode.REPS -> "Target Ripetizioni:"
                        ExerciseMode.TIME -> "Target Secondi:"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
                
                val currentTarget = when (workoutExercise.exercise.mode) {
                    ExerciseMode.REPS -> workoutExercise.workoutExercise.targetReps
                    ExerciseMode.TIME -> workoutExercise.workoutExercise.targetTime
                }
                
                var targetText by remember(currentTarget) {
                    mutableStateOf(currentTarget?.toString() ?: "")
                }
                
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { newValue ->
                        targetText = newValue
                        val target = newValue.toIntOrNull()
                        if (target != null && target > 0) {
                            val updatedWorkoutExercise = when (workoutExercise.exercise.mode) {
                                ExerciseMode.REPS -> workoutExercise.workoutExercise.copy(targetReps = target)
                                ExerciseMode.TIME -> workoutExercise.workoutExercise.copy(targetTime = target)
                            }
                            onUpdateTarget(workoutExercise.copy(workoutExercise = updatedWorkoutExercise))
                        }
                    },
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    placeholder = {
                        Text(when (workoutExercise.exercise.mode) {
                            ExerciseMode.REPS -> "es. 15"
                            ExerciseMode.TIME -> "es. 60"
                        })
                    }
                )
                
                Text(
                    text = when (workoutExercise.exercise.mode) {
                        ExerciseMode.REPS -> "reps"
                        ExerciseMode.TIME -> "sec"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionDialog(
    exercises: List<Exercise>,
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleziona Esercizio")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercises) { exercise ->
                    Card(
                        onClick = {
                            onExerciseSelected(exercise)
                            onDismiss()
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (exercise.description.isNotBlank()) {
                                Text(
                                    text = exercise.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = when (exercise.mode) {
                                    ExerciseMode.REPS -> "Basato su ripetizioni"
                                    ExerciseMode.TIME -> "Basato sul tempo"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}