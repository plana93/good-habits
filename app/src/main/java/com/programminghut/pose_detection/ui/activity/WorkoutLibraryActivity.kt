package com.programminghut.pose_detection.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.data.model.WorkoutTemplate
import com.programminghut.pose_detection.data.model.WorkoutExerciseTemplate
import com.programminghut.pose_detection.data.model.ExerciseTemplate
import com.programminghut.pose_detection.data.model.TodaySession
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import com.programminghut.pose_detection.ui.components.WorkoutThumbnail
import com.programminghut.pose_detection.service.TemplateToSessionService
import com.programminghut.pose_detection.service.UseFromExerciseResult
import com.programminghut.pose_detection.ui.components.WorkoutUseTodayDialog

class WorkoutLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ Controlla se è in modalità selezione
        val isSelectionMode = intent.getBooleanExtra("SELECTION_MODE", false)
        android.util.Log.d("TODAY_DEBUG", "🏃 WorkoutLibraryActivity onCreate - isSelectionMode: $isSelectionMode")
        
        setContent {
            Pose_detectionTheme {
                WorkoutLibraryScreen(
                    onNavigateBack = { 
                        android.util.Log.d("TODAY_DEBUG", "🏃 WorkoutLibraryActivity onNavigateBack chiamato")
                        finish() 
                    },
                    isSelectionMode = isSelectionMode,
                    onWorkoutSelected = { workout ->
                        android.util.Log.d("TODAY_DEBUG", "🏃 WorkoutLibraryActivity onWorkoutSelected chiamato - workout: ${workout.name}, id: ${workout.id}, isSelectionMode: $isSelectionMode")
                        if (isSelectionMode) {
                            // ✅ Restituisce il workoutId selezionato
                            val resultIntent = Intent().apply {
                                putExtra("SELECTED_WORKOUT_ID", workout.id)
                            }
                            android.util.Log.d("TODAY_DEBUG", "🏃 WorkoutLibraryActivity setting result con workoutId: ${workout.id}")
                            setResult(RESULT_OK, resultIntent)
                            android.util.Log.d("TODAY_DEBUG", "🏃 WorkoutLibraryActivity chiamando finish()")
                            finish()
                        } else {
                            // Comportamento normale "Usa Oggi"
                            android.util.Log.d("TODAY_DEBUG", "🏃 WorkoutLibraryActivity modalità normale - finish()")
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLibraryScreen(
    onNavigateBack: () -> Unit,
    isSelectionMode: Boolean = false,
    onWorkoutSelected: (WorkoutTemplate) -> Unit
) {
    val context = LocalContext.current
    
    // Stato per la lista di workout template e esercizi disponibili
    var workoutTemplates by remember { mutableStateOf(getSampleWorkoutTemplates()) }
    val availableExercises by remember { mutableStateOf(getSampleExerciseTemplatesForWorkout()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedWorkout by remember { mutableStateOf<WorkoutTemplate?>(null) }
    
    // Stati per "Usa Oggi" da workout (più complesso, gestisce interi circuiti)
    var existingSessions by remember { mutableStateOf<List<TodaySession>>(emptyList()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (isSelectionMode) "✅ Seleziona Allenamento" else "🔥 Libreria Allenamenti",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        },
        bottomBar = {
            // ✅ Bottom bar per navigazione rapida
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context as ComponentActivity, NewMainActivity::class.java).apply {
                            putExtra("NAVIGATE_TO", "dashboard")
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(intent)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Today, contentDescription = "Oggi") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context as ComponentActivity, NewMainActivity::class.java).apply {
                            putExtra("NAVIGATE_TO", "today")
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(intent)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Esercizi") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context as ComponentActivity, ExerciseLibraryActivity::class.java)
                        context.startActivity(intent)
                        (context as ComponentActivity).finish()
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Allenamenti") },
                    selected = true,
                    onClick = { /* Already here */ }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "Storico") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context as ComponentActivity, NewMainActivity::class.java).apply {
                            putExtra("NAVIGATE_TO", "history")
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(intent)
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, "Crea Allenamento")
            }
        }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header con statistiche
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${workoutTemplates.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Allenamenti",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${workoutTemplates.sumOf { it.exercises.size }}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tot. Esercizi",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${if (workoutTemplates.isNotEmpty()) workoutTemplates.map { it.estimatedDuration }.average().toInt() else 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Min. Med.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Lista allenamenti
            if (workoutTemplates.isEmpty()) {
                // Stato vuoto
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Nessun allenamento creato",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Usa il pulsante + per creare il tuo primo circuito",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // 3 colonne per box più piccole
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(workoutTemplates) { workout ->
                        WorkoutTemplateCard(
                            workout = workout,
                            availableExercises = availableExercises,
                            onClick = { 
                                if (isSelectionMode) {
                                    // In modalità selezione: restituisce direttamente il workout
                                    onWorkoutSelected(workout)
                                } else {
                                    // Modalità normale: mostra dialog dettagli
                                    selectedWorkout = workout
                                }
                            },
                            onLongClick = {
                                // Long press: sempre apre dialog dettagli
                                selectedWorkout = workout
                            }
                        )
                    }
                }
            }
        }

        // Dialog dettagli allenamento con preview e "Usa Oggi"
        selectedWorkout?.let { workout ->
            val hasExistingSession = TemplateToSessionService.getTodaySession(existingSessions) != null
            
            WorkoutUseTodayDialog(
                workout = workout,
                availableExercises = availableExercises,
                existingSessionExists = hasExistingSession,
                onDismiss = { selectedWorkout = null },
                onConfirm = {
                    // Converti workout in sessione oggi
                    val session = TemplateToSessionService.convertWorkoutToTodaySession(
                        workoutTemplate = workout,
                        availableExercises = availableExercises
                    )
                    
                    // Controlla conflitti con sessioni esistenti
                    val existingToday = TemplateToSessionService.getTodaySession(existingSessions)
                    if (existingToday != null) {
                        // Sostituisci la sessione esistente
                        existingSessions = existingSessions.map { existingSession ->
                            if (existingSession.id == existingToday.id) session.copy(id = existingToday.id) else existingSession 
                        }
                    } else {
                        existingSessions = existingSessions + session
                    }
                    
                    onWorkoutSelected(workout)
                    selectedWorkout = null
                }
            )
        }

        // Dialog creazione - placeholder semplice
        if (showCreateDialog) {
            SimpleCreateDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    val newWorkout = WorkoutTemplate(
                        id = (workoutTemplates.size + 1).toLong(),
                        name = name,
                        description = "Allenamento personalizzato",
                        exercises = listOf(
                            WorkoutExerciseTemplate(exerciseId = 1, orderIndex = 0, targetReps = 10),
                            WorkoutExerciseTemplate(exerciseId = 2, orderIndex = 1, targetTime = 30)
                        ),
                        estimatedDuration = 15
                    )
                    workoutTemplates = workoutTemplates + newWorkout
                    showCreateDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutTemplateCard(
    workout: WorkoutTemplate,
    availableExercises: List<ExerciseTemplate>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thumbnail del workout compatto
            WorkoutThumbnail(
                workout = workout,
                availableExercises = availableExercises,
                modifier = Modifier.size(45.dp),
                size = 45.dp,
                cornerRadius = 6.dp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Nome workout compatto
            Text(
                text = workout.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 12.sp
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Info compatte: durata e numero esercizi
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${workout.exercises.size} ex",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "${workout.estimatedDuration}min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable 
private fun SimpleWorkoutDialog(
    workout: WorkoutTemplate,
    onDismiss: () -> Unit,
    onUseToday: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(workout.name) },
        text = {
            Column {
                Text(workout.description)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${workout.exercises.size} esercizi - ${workout.estimatedDuration} min")
            }
        },
        confirmButton = {
            TextButton(onClick = onUseToday) {
                Text("Usa Oggi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}

@Composable
private fun SimpleCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Allenamento") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome Allenamento") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun getSampleExerciseTemplatesForWorkout(): List<ExerciseTemplate> {
    return listOf(
        ExerciseTemplate(
            id = 1,
            name = "Push-up",
            type = com.programminghut.pose_detection.data.model.TemplateExerciseType.STRENGTH,
            mode = com.programminghut.pose_detection.data.model.TemplateExerciseMode.REPS,
            description = "Flessioni tradizionali",
            defaultReps = 15
        ),
        ExerciseTemplate(
            id = 2,
            name = "Plank",
            type = com.programminghut.pose_detection.data.model.TemplateExerciseType.STRENGTH,
            mode = com.programminghut.pose_detection.data.model.TemplateExerciseMode.TIME,
            description = "Posizione di plank",
            defaultTime = 45
        ),
        ExerciseTemplate(
            id = 3,
            name = "Burpees",
            type = com.programminghut.pose_detection.data.model.TemplateExerciseType.CARDIO,
            mode = com.programminghut.pose_detection.data.model.TemplateExerciseMode.TIME,
            description = "Burpees completi",
            defaultTime = 60
        ),
        ExerciseTemplate(
            id = 4,
            name = "Squat",
            type = com.programminghut.pose_detection.data.model.TemplateExerciseType.SQUAT_AI,
            mode = com.programminghut.pose_detection.data.model.TemplateExerciseMode.REPS,
            description = "Squat con AI",
            defaultReps = 20
        )
    )
}

private fun getSampleWorkoutTemplates(): List<WorkoutTemplate> {
    return listOf(
        WorkoutTemplate(
            id = 1,
            name = "💪 Upper Body Power",
            description = "Allenamento intensivo per parte superiore",
            exercises = listOf(
                WorkoutExerciseTemplate(exerciseId = 1, orderIndex = 0, targetReps = 15),
                WorkoutExerciseTemplate(exerciseId = 2, orderIndex = 1, targetTime = 45),
                WorkoutExerciseTemplate(exerciseId = 3, orderIndex = 2, targetTime = 30),
                WorkoutExerciseTemplate(exerciseId = 4, orderIndex = 3, targetReps = 12)
            ),
            estimatedDuration = 25
        ),
        WorkoutTemplate(
            id = 2,
            name = "🏃 Cardio Blast", 
            description = "Brucia calorie con questo cardio esplosivo",
            exercises = listOf(
                WorkoutExerciseTemplate(exerciseId = 3, orderIndex = 0, targetTime = 60),
                WorkoutExerciseTemplate(exerciseId = 4, orderIndex = 1, targetReps = 20)
            ),
            estimatedDuration = 30
        ),
        WorkoutTemplate(
            id = 3,
            name = "🧘 Flexibility Flow",
            description = "Stretching e flessibilità",
            exercises = listOf(
                WorkoutExerciseTemplate(exerciseId = 2, orderIndex = 0, targetTime = 90)
            ),
            estimatedDuration = 15
        )
    )
}