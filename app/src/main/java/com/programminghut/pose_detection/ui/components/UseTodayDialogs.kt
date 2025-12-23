package com.programminghut.pose_detection.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.data.model.*

/**
 * Dialog per personalizzare i parametri prima di usare "Usa Oggi"
 * Permette all'utente di override dei valori target del template
 */
@Composable
fun UseTodayParametersDialog(
    exercise: ExerciseTemplate,
    onConfirm: (targetReps: Int?, targetTime: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var customReps by remember { 
        mutableStateOf(
            if (exercise.mode == TemplateExerciseMode.REPS) 
                exercise.defaultReps?.toString() ?: "" 
            else ""
        ) 
    }
    var customTime by remember { 
        mutableStateOf(
            if (exercise.mode == TemplateExerciseMode.TIME) 
                exercise.defaultTime?.toString() ?: "" 
            else ""
        ) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Today,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Usa Oggi",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info esercizio
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExerciseThumbnail(
                        exercise = exercise,
                        size = 60.dp,
                        cornerRadius = 8.dp,
                        showTypeIcon = false
                    )
                    
                    Column {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${exercise.type.name} • ${exercise.mode.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Divider()
                
                Text(
                    text = "Personalizza i parametri per questa sessione:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Campi di input basati sul mode
                when (exercise.mode) {
                    TemplateExerciseMode.REPS -> {
                        OutlinedTextField(
                            value = customReps,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    customReps = it
                                }
                            },
                            label = { Text("Ripetizioni target") },
                            placeholder = { Text("es. ${exercise.defaultReps ?: 10}") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    TemplateExerciseMode.TIME -> {
                        OutlinedTextField(
                            value = customTime,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    customTime = it
                                }
                            },
                            label = { Text("Tempo target (secondi)") },
                            placeholder = { Text("es. ${exercise.defaultTime ?: 30}") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                
                // Info aggiuntiva
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Questi valori saranno usati solo per oggi. Il template originale non verrà modificato.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reps = if (exercise.mode == TemplateExerciseMode.REPS) 
                        customReps.toIntOrNull() else null
                    val time = if (exercise.mode == TemplateExerciseMode.TIME) 
                        customTime.toIntOrNull() else null
                    
                    onConfirm(reps, time)
                }
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi a Oggi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Dialog per gestire conflitti quando esiste già una sessione per oggi
 */
@Composable
fun UseTodayConflictDialog(
    existingSession: TodaySession,
    newExercise: ExerciseTemplate,
    onAddToEnd: () -> Unit,
    onReplaceAll: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text("Sessione già esistente")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hai già una sessione di allenamento per oggi con ${existingSession.items.size} esercizi.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Cosa vuoi fare con \"${newExercise.name}\"?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Divider()
                
                // Opzioni
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Aggiungi alla fine",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Mantieni gli esercizi esistenti e aggiungi questo alla fine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Sostituisci tutto",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Rimuovi tutti gli esercizi esistenti e usa solo questo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column {
                Button(
                    onClick = onAddToEnd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aggiungi alla fine")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onReplaceAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sostituisci tutto")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Annulla")
            }
        }
    )
}