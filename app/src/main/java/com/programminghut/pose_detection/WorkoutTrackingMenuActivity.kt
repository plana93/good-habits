package com.programminghut.pose_detection

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme

/**
 * Menu per iniziare un allenamento
 * Permette di scegliere tra allenamenti preconfigurati o crearne di nuovi
 */
class WorkoutTrackingMenuActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            Pose_detectionTheme {
                WorkoutTrackingMenuScreen(
                    onNavigateBack = { finish() },
                    onCreateWorkout = {
                        // Naviga alla creazione workout completa
                        val intent = Intent(this@WorkoutTrackingMenuActivity, WorkoutCreationActivitySimple::class.java)
                        startActivity(intent)
                    },
                    onStartWorkout = { workoutType ->
                        when (workoutType) {
                            "SQUAT_AUTO" -> {
                                // Squat con camera (modalità completa)
                                val intent = Intent(this@WorkoutTrackingMenuActivity, CameraSelectionActivity::class.java)
                                startActivity(intent)
                            }
                            "SQUAT_MANUAL" -> {
                                // Squat solo tracking (retroattivo)
                                val intent = Intent(this@WorkoutTrackingMenuActivity, ManualExerciseActivity::class.java)
                                intent.putExtra("EXERCISE_NAME", "Squat")
                                intent.putExtra("EXERCISE_MODE", "REPS")
                                intent.putExtra("TARGET_VALUE", 20)
                                startActivity(intent)
                            }
                            "PUSH_UP" -> {
                                val intent = Intent(this@WorkoutTrackingMenuActivity, ManualExerciseActivity::class.java)
                                intent.putExtra("EXERCISE_NAME", "Push-up")
                                intent.putExtra("EXERCISE_MODE", "REPS")
                                intent.putExtra("TARGET_VALUE", 15)
                                startActivity(intent)
                            }
                            "PLANK" -> {
                                val intent = Intent(this@WorkoutTrackingMenuActivity, ManualExerciseActivity::class.java)
                                intent.putExtra("EXERCISE_NAME", "Plank")
                                intent.putExtra("EXERCISE_MODE", "TIME")
                                intent.putExtra("TARGET_VALUE", 60)
                                startActivity(intent)
                            }
                            "FULL_BODY" -> {
                                // Workout strutturato multi-esercizio
                                val intent = Intent(this@WorkoutTrackingMenuActivity, WorkoutCreationActivitySimple::class.java)
                                startActivity(intent)
                            }
                            "PERSONALIZZATO" -> {
                                // Crea nuovo workout personalizzato
                                val intent = Intent(this@WorkoutTrackingMenuActivity, WorkoutCreationActivitySimple::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackingMenuScreen(
    onNavigateBack: () -> Unit,
    onCreateWorkout: () -> Unit,
    onStartWorkout: (String) -> Unit
) {
    val presetWorkouts = listOf(
        WorkoutPreset("SQUAT_AUTO", "Squat con camera (automatico)", "🦵", "AUTOMATICO"),
        WorkoutPreset("SQUAT_MANUAL", "Squat solo tracking", "🦵", "MANUALE"),
        WorkoutPreset("PUSH_UP", "Push-up (manuale)", "💪", "MANUALE"),
        WorkoutPreset("PLANK", "Plank (timer)", "⏱️", "TIMER"),
        WorkoutPreset("FULL_BODY", "Workout corpo intero", "💪", "MISTO"),
        WorkoutPreset("PERSONALIZZATO", "Crea workout personalizzato", "⚡", "CUSTOM")
    )
    
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
            
            Text(
                text = "Inizia Allenamento",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onCreateWorkout) {
                Icon(Icons.Default.Add, contentDescription = "Crea nuovo workout")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Workout presets
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(presetWorkouts) { workout ->
                WorkoutPresetCard(
                    workout = workout,
                    onStart = { onStartWorkout(workout.id) }
                )
            }
            
            // Quick stats
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📊 Statistiche Rapide",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem("Questa settimana", "3", "🔥")
                            StatItem("Streak attuale", "5 giorni", "⚡")
                            StatItem("Totale esercizi", "147", "💪")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutPresetCard(
    workout: WorkoutPreset,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workout.emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = workout.id.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = workout.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Badge per tipo di workout
                    Text(
                        text = workout.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Button(
                onClick = onStart
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inizia")
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class WorkoutPreset(
    val id: String,
    val description: String,
    val emoji: String,
    val type: String
)