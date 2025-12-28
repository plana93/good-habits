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
import kotlinx.coroutines.launch
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
import com.programminghut.pose_detection.data.repository.WorkoutTemplateFileManager
import com.programminghut.pose_detection.util.ExerciseTemplateFileManager
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
    val scope = rememberCoroutineScope()
    
    // Stato per la lista di workout template e esercizi disponibili
    var workoutTemplates by remember { mutableStateOf<List<WorkoutTemplate>>(emptyList()) }
    var availableExercises by remember { mutableStateOf<List<ExerciseTemplate>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedWorkout by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var debugMode by remember { mutableStateOf(false) }
    var fileInspectionResult by remember { mutableStateOf<String?>(null) }
    
    // Carica workout ed esercizi dai file JSON all'avvio
    LaunchedEffect(Unit) {
        try {
            val loadedWorkouts = WorkoutTemplateFileManager.loadWorkoutTemplates(context)
            workoutTemplates = loadedWorkouts
            android.util.Log.d("WorkoutLibraryActivity", "Caricati ${loadedWorkouts.size} workout dai JSON")

            // Log dei file disponibili per debug - utile se la lista risulta vuota
            val availableFiles = WorkoutTemplateFileManager.getAvailableTemplateFiles(context)
            android.util.Log.d("WorkoutLibraryActivity", "File template disponibili: ${availableFiles.joinToString()}")

            val loadedExercises = ExerciseTemplateFileManager.loadExerciseTemplates(context)
            availableExercises = loadedExercises
            android.util.Log.d("WorkoutLibraryActivity", "Caricati ${loadedExercises.size} esercizi dai JSON")
        } catch (e: Exception) {
            android.util.Log.e("WorkoutLibraryActivity", "Errore caricamento templates dai JSON", e)
            // Invece di usare fallback hardcoded, mostra errore
            // Gli array rimangono vuoti e l'UI gestisce il caso vuoto
        }
    }
    
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
                actions = {
                    IconButton(onClick = { debugMode = !debugMode }) {
                        Icon(Icons.Default.BugReport, contentDescription = "Toggle debug")
                    }
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
                            // Mostra i file template se presenti per aiutare il debugging
                            val files = WorkoutTemplateFileManager.getAvailableTemplateFiles(context)
                            if (files.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "File template rilevati: ${files.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = {
                                    // Permette di forzare un reload dei template per debug
                                    scope.launch {
                                        try {
                                            val reloaded = WorkoutTemplateFileManager.loadWorkoutTemplates(context)
                                            workoutTemplates = reloaded
                                            android.util.Log.d("WorkoutLibraryActivity", "Reload completato: ${reloaded.size} workout")
                                        } catch (e: Exception) {
                                            android.util.Log.e("WorkoutLibraryActivity", "Errore reload templates", e)
                                        }
                                    }
                                }) {
                                    Text("Ricarica template")
                                }
                            }
                            // Se in debug mode mostra l'elenco dei file con pulsante di preview
                            if (debugMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("DEBUG: Ispeziona file template", style = MaterialTheme.typography.titleSmall)
                                val filesList = WorkoutTemplateFileManager.getAvailableTemplateFiles(context)
                                filesList.forEach { fname ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(fname, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        TextButton(onClick = {
                                            scope.launch {
                                                val tpl = WorkoutTemplateFileManager.loadWorkoutTemplateFromFile(context, fname)
                                                fileInspectionResult = tpl?.let { "Parsed: ${it.name} (id=${it.id}), exercises=${it.exercises.size}" } ?: "Errore parsing $fname"
                                            }
                                        }) { Text("Mostra") }
                                    }
                                }
                                fileInspectionResult?.let { msg ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(msg, style = MaterialTheme.typography.bodySmall)
                                }
                            }
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
                    // Crea un workout vuoto o prepopolato con i primi esercizi disponibili (se presenti)
                    val selectedExercises = availableExercises.take(2).mapIndexed { idx, ex ->
                        WorkoutExerciseTemplate(
                            exerciseId = ex.id,
                            orderIndex = idx,
                            targetReps = ex.defaultReps,
                            targetTime = ex.defaultTime
                        )
                    }

                    val newWorkout = WorkoutTemplate(
                        id = (workoutTemplates.size + 1).toLong(),
                        name = name,
                        description = "Allenamento personalizzato",
                        exercises = selectedExercises,
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

// Funzioni rimosse - ora tutto viene caricato dinamicamente dai JSON
// private fun getSampleExerciseTemplatesForWorkout(): List<ExerciseTemplate> { ... }
// private fun getSampleWorkoutTemplates(): List<WorkoutTemplate> { ... }