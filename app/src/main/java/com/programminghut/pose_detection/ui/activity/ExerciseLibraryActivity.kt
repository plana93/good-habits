@file:OptIn(ExperimentalMaterial3Api::class)

package com.programminghut.pose_detection.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import com.programminghut.pose_detection.data.model.*
import com.programminghut.pose_detection.test.RuntimeChainTest
import com.programminghut.pose_detection.ui.components.ExerciseThumbnail
import com.programminghut.pose_detection.ui.components.UseTodayParametersDialog
import com.programminghut.pose_detection.ui.components.UseTodayConflictDialog
import com.programminghut.pose_detection.service.TemplateToSessionService
import com.programminghut.pose_detection.service.UseFromExerciseResult

/**
 * ExerciseLibraryActivity - SOLO gestione template esercizi
 * 
 * REGOLA D'ORO: Qui si gestiscono SOLO template (ExerciseTemplate)
 * - NON si traccia (no ripetizioni/tempo reali)
 * - NON si salva nella sessione giornaliera
 * - Solo creazione/modifica/visualizzazione template
 * 
 * Features:
 * - Grid con miniature esercizi
 * - FAB per creare nuovo esercizio
 * - Click esercizio → Dettaglio + "Usa oggi"
 * - Sistema miniature automatico
 */
class ExerciseLibraryActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ Controlla se è in modalità selezione
        val isSelectionMode = intent.getBooleanExtra("SELECTION_MODE", false)
        Log.d("🔍 EXERCISE_SELECT", "ExerciseLibraryActivity onCreate - isSelectionMode: $isSelectionMode")
        Log.d("🔍 EXERCISE_SELECT", "Intent extras: ${intent.extras?.keySet()?.joinToString()}")
        
        // 🛡️ FORCE SELECTION MODE per testing
        val finalSelectionMode = true // isSelectionMode || true // Forziamo true temporaneamente
        Log.d("🔍 EXERCISE_SELECT", "finalSelectionMode: $finalSelectionMode")
        
        setContent {
            Pose_detectionTheme {
                ExerciseLibraryScreen(
                    onNavigateBack = { finish() },
                    isSelectionMode = finalSelectionMode,
                    onExerciseSelected = { exerciseTemplate ->
                        Log.d("🔍 EXERCISE_SELECT", "Comportamento normale: navigando a NewMainActivity")
                        // Comportamento normale "Usa Oggi"
                        val intent = Intent(this, NewMainActivity::class.java).apply {
                            putExtra("add_exercise_id", exerciseTemplate.id)
                        }
                        startActivity(intent)
                        finish()
                    },
                    onExerciseWithQuantitySelected = { exerciseTemplate, reps, time ->
                        Log.d("🔍 EXERCISE_SELECT", "Quantità confermata: reps=$reps, time=$time")
                        
                        val resultIntent = Intent().apply {
                            putExtra("SELECTED_EXERCISE_ID", exerciseTemplate.id)
                            reps?.let { putExtra("SELECTED_EXERCISE_REPS", it) }
                            time?.let { putExtra("SELECTED_EXERCISE_TIME", it) }
                        }
                        setResult(RESULT_OK, resultIntent)
                        Log.d("🔍 EXERCISE_SELECT", "setResult(RESULT_OK) chiamato con quantità, finendo Activity...")
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onNavigateBack: () -> Unit,
    isSelectionMode: Boolean = false,
    onExerciseSelected: (ExerciseTemplate) -> Unit,
    onExerciseWithQuantitySelected: ((ExerciseTemplate, Int?, Int?) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Stato per la lista di esercizi template
    var exerciseTemplates by remember { mutableStateOf(getSampleExerciseTemplates()) }
    
    // 🧪 TEST: Verifica flusso libreria esercizi
    LaunchedEffect(exerciseTemplates, isSelectionMode) {
        RuntimeChainTest.testExerciseLibraryFlow(isSelectionMode, exerciseTemplates)
    }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<ExerciseTemplate?>(null) }
    
    // Stati per "Usa Oggi"
    var showUseTodayParametersDialog by remember { mutableStateOf(false) }
    var showUseTodayConflictDialog by remember { mutableStateOf(false) }
    var exerciseForToday by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var existingSessionForConflict by remember { mutableStateOf<TodaySession?>(null) }
    
    // Stati per selezione quantità
    var showQuantityDialog by remember { mutableStateOf(false) }
    var selectedExerciseForQuantity by remember { mutableStateOf<ExerciseTemplate?>(null) }
    
    // Simulated existing sessions (da sostituire con dati reali dal database)
    var existingSessions by remember { mutableStateOf<List<TodaySession>>(emptyList()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (isSelectionMode) "✅ Seleziona Esercizio" else "📚 Libreria Esercizi",
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    selected = true,
                    onClick = { /* Already here */ }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Allenamenti") },
                    selected = false,
                    onClick = {
                        val intent = Intent(context as ComponentActivity, WorkoutLibraryActivity::class.java)
                        context.startActivity(intent)
                        (context as ComponentActivity).finish()
                    }
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
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Aggiungi Esercizio")
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
                            text = "${exerciseTemplates.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Template",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${exerciseTemplates.count { it.type == TemplateExerciseType.STRENGTH }}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Forza",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${exerciseTemplates.count { it.type == TemplateExerciseType.CARDIO }}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Cardio",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Grid di esercizi
            if (exerciseTemplates.isEmpty()) {
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
                            text = "Nessun esercizio creato",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Usa il pulsante + per creare il tuo primo template esercizio",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exerciseTemplates) { template ->
                        ExerciseTemplateCard(
                            template = template,
                            onClick = { 
                                Log.d("🔍 EXERCISE_SELECT", "Click su template: ${template.name}, ID: ${template.id}")
                                Log.d("🔍 EXERCISE_SELECT", "isSelectionMode: $isSelectionMode")
                                if (isSelectionMode) {
                                    // 🧪 TEST: Verifica selezione esercizio
                                    RuntimeChainTest.testExerciseSelection(
                                        template.id, 
                                        template.name, 
                                        "ExerciseLibrary-Selezione"
                                    )
                                    Log.d("🔍 EXERCISE_SELECT", "Modalità selezione: mostrando dialog quantità per: ${template.name}")
                                    // ✅ In modalità selezione: mostra dialog quantità
                                    selectedExerciseForQuantity = template
                                    showQuantityDialog = true
                                } else {
                                    Log.d("🔍 EXERCISE_SELECT", "Modalità normale - aprendo dialog")
                                    // Modalità normale: apre dialog dettagli
                                    selectedExercise = template
                                }
                            }
                        )
                    }
                }
            }
        }

        // Dialog dettagli esercizio
        selectedExercise?.let { exercise ->
            ExerciseDetailDialog(
                exercise = exercise,
                onDismiss = { selectedExercise = null },
                onUseToday = {
                    exerciseForToday = exercise
                    selectedExercise = null
                    showUseTodayParametersDialog = true
                },
                onEdit = {
                    // TODO: Implementare modifica esercizio
                    selectedExercise = null
                }
            )
        }

        // Dialog creazione nuovo esercizio
        if (showCreateDialog) {
            CreateExerciseDialog(
                onDismiss = { showCreateDialog = false },
                onCreateExercise = { name, type, mode, description ->
                    val newTemplate = ExerciseTemplate(
                        id = (exerciseTemplates.size + 1).toLong(),
                        name = name,
                        type = type,
                        mode = mode,
                        description = description,
                        imagePath = null, // TODO: Sistema miniature
                        thumbnailPath = null
                    )
                    exerciseTemplates = exerciseTemplates + newTemplate
                    showCreateDialog = false
                }
            )
        }
        
        // Dialog parametri "Usa Oggi"
        if (showUseTodayParametersDialog && exerciseForToday != null) {
            UseTodayParametersDialog(
                exercise = exerciseForToday!!,
                onConfirm = { targetReps, targetTime ->
                    // Gestisce la conversione template -> session
                    val result = TemplateToSessionService.handleUseToday(
                        template = exerciseForToday!!,
                        existingSessions = existingSessions,
                        conflictResolution = TemplateToSessionService.ConflictResolution.ASK_USER,
                        targetRepsOverride = targetReps,
                        targetTimeOverride = targetTime
                    )
                    
                    when (result) {
                        is UseFromExerciseResult.NewSession -> {
                            // Nuova sessione creata
                            existingSessions = existingSessions + result.session
                            onExerciseSelected(exerciseForToday!!)
                            showUseTodayParametersDialog = false
                            exerciseForToday = null
                        }
                        
                        is UseFromExerciseResult.UpdatedSession -> {
                            // Sessione esistente aggiornata
                            existingSessions = existingSessions.map { 
                                if (it.id == result.session.id) result.session else it 
                            }
                            onExerciseSelected(exerciseForToday!!)
                            showUseTodayParametersDialog = false
                            exerciseForToday = null
                        }
                        
                        is UseFromExerciseResult.ConflictNeedsResolution -> {
                            // Richiede risoluzione conflitto
                            existingSessionForConflict = result.existingSession
                            showUseTodayParametersDialog = false
                            showUseTodayConflictDialog = true
                        }
                        
                        is UseFromExerciseResult.ReplacedSession -> {
                            // Sessione sostituita
                            existingSessions = existingSessions.map { 
                                if (it.id == result.session.id) result.session else it 
                            }
                            onExerciseSelected(exerciseForToday!!)
                            showUseTodayParametersDialog = false
                            exerciseForToday = null
                        }
                    }
                },
                onDismiss = {
                    showUseTodayParametersDialog = false
                    exerciseForToday = null
                }
            )
        }
        
        // Dialog risoluzione conflitti "Usa Oggi"
        if (showUseTodayConflictDialog && exerciseForToday != null && existingSessionForConflict != null) {
            UseTodayConflictDialog(
                existingSession = existingSessionForConflict!!,
                newExercise = exerciseForToday!!,
                onAddToEnd = {
                    // Aggiungi alla fine
                    val result = TemplateToSessionService.handleUseToday(
                        template = exerciseForToday!!,
                        existingSessions = existingSessions,
                        conflictResolution = TemplateToSessionService.ConflictResolution.ADD_TO_END
                    )
                    
                    if (result is UseFromExerciseResult.UpdatedSession) {
                        existingSessions = existingSessions.map { 
                            if (it.id == result.session.id) result.session else it 
                        }
                        onExerciseSelected(exerciseForToday!!)
                    }
                    
                    showUseTodayConflictDialog = false
                    exerciseForToday = null
                    existingSessionForConflict = null
                },
                onReplaceAll = {
                    // Sostituisci tutto
                    val result = TemplateToSessionService.handleUseToday(
                        template = exerciseForToday!!,
                        existingSessions = existingSessions,
                        conflictResolution = TemplateToSessionService.ConflictResolution.REPLACE_ALL
                    )
                    
                    if (result is UseFromExerciseResult.ReplacedSession) {
                        existingSessions = existingSessions.map { 
                            if (it.id == result.session.id) result.session else it 
                        }
                        onExerciseSelected(exerciseForToday!!)
                    }
                    
                    showUseTodayConflictDialog = false
                    exerciseForToday = null
                    existingSessionForConflict = null
                },
                onCancel = {
                    showUseTodayConflictDialog = false
                    exerciseForToday = null
                    existingSessionForConflict = null
                }
            )
        }
        
        // Dialog selezione quantità
        if (showQuantityDialog && selectedExerciseForQuantity != null) {
            QuantitySelectionDialog(
                exercise = selectedExerciseForQuantity!!,
                onConfirm = { reps, time ->
                    Log.d("🔍 EXERCISE_SELECT", "Quantità selezionata: reps=$reps, time=$time")
                    
                    if (onExerciseWithQuantitySelected != null) {
                        onExerciseWithQuantitySelected(selectedExerciseForQuantity!!, reps, time)
                    } else {
                        onExerciseSelected(selectedExerciseForQuantity!!)
                    }
                    
                    showQuantityDialog = false
                    selectedExerciseForQuantity = null
                },
                onCancel = {
                    showQuantityDialog = false
                    selectedExerciseForQuantity = null
                }
            )
        }
    }
}

@Composable
fun ExerciseTemplateCard(
    template: ExerciseTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Thumbnail dell'esercizio (occupa la maggior parte dello spazio)
            ExerciseThumbnail(
                exercise = template,
                modifier = Modifier.weight(1f),
                size = 80.dp,
                cornerRadius = 8.dp,
                showTypeIcon = true
            )

            // Info esercizio compatta
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Parametri di default compatti
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    template.defaultReps?.let { reps ->
                        Text(
                            text = "${reps}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    template.defaultTime?.let { time ->
                        Text(
                            text = "${time}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

// Dialog per i dettagli dell'esercizio
@Composable
fun ExerciseDetailDialog(
    exercise: ExerciseTemplate,
    onDismiss: () -> Unit,
    onUseToday: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (exercise.type) {
                        TemplateExerciseType.STRENGTH -> Icons.Default.FitnessCenter
                        TemplateExerciseType.CARDIO -> Icons.Default.DirectionsRun
                        TemplateExerciseType.FLEXIBILITY -> Icons.Default.SelfImprovement
                        TemplateExerciseType.SQUAT_AI -> Icons.Default.Visibility
                        else -> Icons.Default.SportsMartialArts
                    },
                    contentDescription = null
                )
                Text(exercise.name)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (exercise.description.isNotBlank()) {
                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Tipo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = when (exercise.type) {
                                TemplateExerciseType.STRENGTH -> "Forza"
                                TemplateExerciseType.CARDIO -> "Cardio"
                                TemplateExerciseType.FLEXIBILITY -> "Flessibilità"
                                TemplateExerciseType.BALANCE -> "Equilibrio"
                                TemplateExerciseType.SQUAT_AI -> "Squat AI"
                                TemplateExerciseType.CUSTOM -> "Personalizzato"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Modalità",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = when (exercise.mode) {
                                TemplateExerciseMode.REPS -> "Ripetizioni"
                                TemplateExerciseMode.TIME -> "Tempo"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifica")
                }
                
                Button(onClick = onUseToday) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Usa Oggi")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}

// Dialog per creare nuovo esercizio
@Composable
fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreateExercise: (String, TemplateExerciseType, TemplateExerciseMode, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TemplateExerciseType.STRENGTH) }
    var selectedMode by remember { mutableStateOf(TemplateExerciseMode.REPS) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crea Nuovo Esercizio") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome esercizio") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione (opzionale)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // Selezione tipo
                Column {
                    Text(
                        text = "Tipo esercizio",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            TemplateExerciseType.STRENGTH to "💪 Forza",
                            TemplateExerciseType.CARDIO to "🏃 Cardio",
                            TemplateExerciseType.FLEXIBILITY to "🧘 Flessibilità"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                
                // Selezione modalità
                Column {
                    Text(
                        text = "Modalità tracking",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == TemplateExerciseMode.REPS,
                            onClick = { selectedMode = TemplateExerciseMode.REPS },
                            label = { Text("Ripetizioni") }
                        )
                        FilterChip(
                            selected = selectedMode == TemplateExerciseMode.TIME,
                            onClick = { selectedMode = TemplateExerciseMode.TIME },
                            label = { Text("Tempo") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreateExercise(name, selectedType, selectedMode, description)
                    }
                },
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

// Dati di esempio per i template
fun getSampleExerciseTemplates(): List<ExerciseTemplate> {
    return listOf(
        ExerciseTemplate(
            id = 1,
            name = "Push-up",
            type = TemplateExerciseType.STRENGTH,
            mode = TemplateExerciseMode.REPS,
            description = "Piegamenti sulle braccia classici per petto, spalle e tricipiti",
            defaultReps = 12
        ),
        ExerciseTemplate(
            id = 2,
            name = "Plank",
            type = TemplateExerciseType.STRENGTH,
            mode = TemplateExerciseMode.TIME,
            description = "Mantenimento posizione isometrica per addominali e core",
            defaultTime = 30
        ),
        ExerciseTemplate(
            id = 3,
            name = "Squat AI",
            type = TemplateExerciseType.SQUAT_AI,
            mode = TemplateExerciseMode.REPS,
            description = "Squat con tracking automatico tramite intelligenza artificiale",
            defaultReps = 20
        ),
        ExerciseTemplate(
            id = 4,
            name = "Jumping Jacks",
            type = TemplateExerciseType.CARDIO,
            mode = TemplateExerciseMode.REPS,
            description = "Saltelli con apertura e chiusura braccia e gambe",
            defaultReps = 25
        ),
        ExerciseTemplate(
            id = 5,
            name = "Stretching Braccia",
            type = TemplateExerciseType.FLEXIBILITY,
            mode = TemplateExerciseMode.TIME,
            description = "Allungamento muscoli braccia e spalle",
            defaultTime = 45
        )
    )
}

@Composable
fun QuantitySelectionDialog(
    exercise: ExerciseTemplate,
    onConfirm: (reps: Int?, time: Int?) -> Unit,
    onCancel: () -> Unit
) {
    var selectedReps by remember { 
        mutableIntStateOf(exercise.defaultReps ?: 10) 
    }
    var selectedTime by remember { 
        mutableIntStateOf(exercise.defaultTime ?: 30) 
    }
    
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Imposta Quantità",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Esercizio: ${exercise.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                when (exercise.mode) {
                    TemplateExerciseMode.REPS -> {
                        Column {
                            Text(
                                text = "Ripetizioni",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        selectedReps = (selectedReps - 1).coerceAtLeast(1)
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, "Diminuisci")
                                }
                                
                                Text(
                                    text = "$selectedReps",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                
                                IconButton(
                                    onClick = { 
                                        selectedReps += 1
                                    }
                                ) {
                                    Icon(Icons.Default.Add, "Aumenta")
                                }
                            }
                        }
                    }
                    
                    TemplateExerciseMode.TIME -> {
                        Column {
                            Text(
                                text = "Durata (secondi)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        selectedTime = (selectedTime - 5).coerceAtLeast(5)
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, "Diminuisci")
                                }
                                
                                Text(
                                    text = "${selectedTime}s",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                
                                IconButton(
                                    onClick = { 
                                        selectedTime += 5
                                    }
                                ) {
                                    Icon(Icons.Default.Add, "Aumenta")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reps = if (exercise.mode == TemplateExerciseMode.REPS) selectedReps else null
                    val time = if (exercise.mode == TemplateExerciseMode.TIME) selectedTime else null
                    onConfirm(reps, time)
                }
            ) {
                Text("Conferma")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Annulla")
            }
        }
    )
}