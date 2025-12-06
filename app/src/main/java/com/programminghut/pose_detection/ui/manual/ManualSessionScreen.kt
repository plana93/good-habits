package com.programminghut.pose_detection.ui.manual

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * ManualSessionScreen - Form for creating manual workout sessions
 * 
 * Phase 4: Manual Session Creation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSessionScreen(
    viewModel: ManualSessionViewModel,
    onBackClick: () -> Unit,
    onSessionCreated: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val exerciseType by viewModel.exerciseType.collectAsState()
    val repsCount by viewModel.repsCount.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val affectsStreak by viewModel.affectsStreak.collectAsState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aggiungi Sessione Manuale") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Info dialog */ }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ManualSessionUiState.Editing, is ManualSessionUiState.Error -> {
                ManualSessionForm(
                    selectedDate = selectedDate,
                    exerciseType = exerciseType,
                    repsCount = repsCount,
                    duration = duration,
                    notes = notes,
                    affectsStreak = affectsStreak,
                    errorMessage = (state as? ManualSessionUiState.Error)?.message,
                    onDateClick = { showDatePicker = true },
                    onExerciseClick = { showExercisePicker = true },
                    onRepsChange = { viewModel.updateRepsCount(it) },
                    onDurationChange = { viewModel.updateDuration(it) },
                    onNotesChange = { viewModel.updateNotes(it) },
                    onStreakToggle = { viewModel.toggleAffectsStreak() },
                    onSaveClick = { viewModel.createSession() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
                
                // Date picker dialog
                if (showDatePicker) {
                    DatePickerDialog(
                        selectedDate = selectedDate,
                        onDateSelected = {
                            viewModel.updateDate(it)
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }
                
                // Exercise picker dialog
                if (showExercisePicker) {
                    ExercisePickerDialog(
                        selectedExercise = exerciseType,
                        onExerciseSelected = {
                            viewModel.updateExerciseType(it)
                            showExercisePicker = false
                        },
                        onDismiss = { showExercisePicker = false }
                    )
                }
            }
            
            is ManualSessionUiState.Saving -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Salvataggio in corso...")
                    }
                }
            }
            
            is ManualSessionUiState.Success -> {
                LaunchedEffect(state.sessionId) {
                    onSessionCreated(state.sessionId)
                }
            }
            
            is ManualSessionUiState.DuplicateWarning -> {
                AlertDialog(
                    onDismissRequest = { state.onCancel() },
                    icon = {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                    },
                    title = {
                        Text("Sessione già esistente")
                    },
                    text = {
                        Text("Esiste già ${state.existingCount} sessione${if (state.existingCount > 1) "i" else ""} per questo giorno. Vuoi aggiungerne un'altra?")
                    },
                    confirmButton = {
                        TextButton(onClick = { state.onConfirm() }) {
                            Text("Aggiungi comunque")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { state.onCancel() }) {
                            Text("Annulla")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSessionForm(
    selectedDate: Long,
    exerciseType: String,
    repsCount: String,
    duration: String,
    notes: String,
    affectsStreak: Boolean,
    errorMessage: String?,
    onDateClick: () -> Unit,
    onExerciseClick: () -> Unit,
    onRepsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onStreakToggle: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Usa questo form per aggiungere sessioni di allenamento completate senza l'app.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Error message
        errorMessage?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // Date selector
        OutlinedCard(
            onClick = onDateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Data Allenamento",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(selectedDate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(Icons.Filled.DateRange, contentDescription = null)
            }
        }
        
        // Exercise type selector
        OutlinedCard(
            onClick = onExerciseClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tipo di Esercizio",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exerciseType,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
        }
        
        // Reps count input
        OutlinedTextField(
            value = repsCount,
            onValueChange = onRepsChange,
            label = { Text("Numero Ripetizioni") },
            leadingIcon = {
                Icon(Icons.Filled.Check, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("Inserisci il numero totale di ripetizioni completate")
            }
        )
        
        // Duration input
        OutlinedTextField(
            value = duration,
            onValueChange = onDurationChange,
            label = { Text("Durata (minuti)") },
            leadingIcon = {
                Icon(Icons.Filled.Create, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("Durata stimata dell'allenamento in minuti")
            }
        )
        
        // Notes input
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Note (opzionale)") },
            leadingIcon = {
                Icon(Icons.Filled.Edit, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            supportingText = {
                Text("Aggiungi note sull'allenamento")
            }
        )
        
        // Affects streak toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (affectsStreak)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conta per la Streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (affectsStreak)
                            "Questa sessione conta per la tua streak consecutiva"
                        else
                            "Sessione informativa, non conta per la streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = affectsStreak,
                    onCheckedChange = { onStreakToggle() }
                )
            }
        }
        
        // Save button
        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = repsCount.isNotBlank() && duration.isNotBlank()
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salva Sessione", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun ExercisePickerDialog(
    selectedExercise: String,
    onExerciseSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val exercises = listOf(
        "SQUAT",
        "PUSH_UP",
        "LUNGE",
        "PLANK",
        "CRUNCH",
        "JUMPING_JACK"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleziona Esercizio") },
        text = {
            Column {
                exercises.forEach { exercise ->
                    TextButton(
                        onClick = { onExerciseSelected(exercise) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(exercise)
                            if (exercise == selectedExercise) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
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

/**
 * Helper functions
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ITALIAN)
    return sdf.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
}
