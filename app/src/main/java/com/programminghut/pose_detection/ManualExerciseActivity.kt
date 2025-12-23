package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programminghut.pose_detection.data.model.ExerciseMode
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity per eseguire esercizi manuali (non basati su camera/pose detection)
 * 
 * Supporta:
 * - Esercizi basati su ripetizioni (REPS): inserimento manuale del numero
 * - Esercizi basati sul tempo (TIME): timer per cronometrare la durata
 */
class ManualExerciseActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val exerciseId = intent.getLongExtra("EXERCISE_ID", 0)
        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Esercizio"
        val exerciseMode = intent.getStringExtra("EXERCISE_MODE") ?: ExerciseMode.REPS.name
        val targetValue = intent.getIntExtra("TARGET_VALUE", 0)
        
        setContent {
            Pose_detectionTheme {
                ManualExerciseScreen(
                    exerciseName = exerciseName,
                    exerciseMode = ExerciseMode.valueOf(exerciseMode),
                    targetValue = targetValue,
                    onComplete = { actualValue ->
                        // Return result to WorkoutTrackingActivity
                        val resultIntent = Intent().apply {
                            putExtra("EXERCISE_ID", exerciseId)
                            putExtra("ACTUAL_VALUE", actualValue)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualExerciseScreen(
    exerciseName: String,
    exerciseMode: ExerciseMode,
    targetValue: Int,
    onComplete: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var actualValue by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Timer logic
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Annulla")
            }
            
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(56.dp)) // Balance the layout
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Exercise Information Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (exerciseMode) {
                    ExerciseMode.REPS -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Target: $targetValue ripetizioni",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Quante ripetizioni hai completato?",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = actualValue,
                            onValueChange = { actualValue = it },
                            label = { Text("Ripetizioni eseguite") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    ExerciseMode.TIME -> {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Target: $targetValue secondi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Timer Display
                        Text(
                            text = formatTime(timerSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Timer Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!isTimerRunning && timerSeconds == 0) {
                                Button(
                                    onClick = { isTimerRunning = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Inizia Timer")
                                }
                            } else if (isTimerRunning) {
                                Button(
                                    onClick = { 
                                        isTimerRunning = false
                                        isCompleted = true
                                        actualValue = timerSeconds.toString()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop")
                                }
                            } else {
                                Column {
                                    Button(
                                        onClick = { 
                                            isCompleted = true
                                            actualValue = timerSeconds.toString()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Conferma Tempo")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedButton(
                                        onClick = { 
                                            timerSeconds = 0
                                            isCompleted = false
                                            actualValue = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Reset")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Complete Button
        if (isCompleted || (exerciseMode == ExerciseMode.REPS && actualValue.isNotBlank())) {
            Button(
                onClick = {
                    val value = actualValue.toIntOrNull() ?: 0
                    if (value > 0) {
                        onComplete(value)
                    }
                },
                enabled = actualValue.toIntOrNull()?.let { it > 0 } == true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Completa Esercizio",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // Alternative manual input for time exercises
        if (exerciseMode == ExerciseMode.TIME && !isTimerRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Oppure inserisci manualmente:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = if (isCompleted) actualValue else "",
                onValueChange = { value ->
                    actualValue = value
                    if (value.isNotBlank()) {
                        isCompleted = true
                        timerSeconds = value.toIntOrNull() ?: 0
                    }
                },
                label = { Text("Secondi eseguiti") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isTimerRunning
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}