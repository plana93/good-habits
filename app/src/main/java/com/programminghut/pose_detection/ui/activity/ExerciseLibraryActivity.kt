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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import com.programminghut.pose_detection.data.model.*
import com.programminghut.pose_detection.test.RuntimeChainTest
import com.programminghut.pose_detection.ui.components.ExerciseThumbnail
import com.programminghut.pose_detection.ui.components.UseTodayParametersDialog
import com.programminghut.pose_detection.ui.components.UseTodayConflictDialog
import com.programminghut.pose_detection.service.TemplateToSessionService
import com.programminghut.pose_detection.service.UseFromExerciseResult
import com.programminghut.pose_detection.util.ExerciseTemplateFileManager

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
    
    // Stato per la lista di esercizi template - CARICATI DINAMICAMENTE DAI JSON
    var exerciseTemplates by remember { mutableStateOf<List<ExerciseTemplate>>(emptyList()) }

    // Carica esercizi dai JSON all'avvio
    LaunchedEffect(Unit) {
        try {
            val loadedExercises = ExerciseTemplateFileManager.loadExerciseTemplates(context)
            exerciseTemplates = loadedExercises
            Log.d("ExerciseLibraryActivity", "✅ Caricati ${loadedExercises.size} esercizi dai JSON")
        } catch (e: Exception) {
            Log.e("ExerciseLibraryActivity", "❌ Errore caricamento esercizi dai JSON", e)
            exerciseTemplates = emptyList()
        }
    }
    
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
                    columns = GridCells.Fixed(3), // 3 colonne per box più piccole
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(exerciseTemplates) { template ->
                        ExerciseTemplateCard(
                            template = template,
                            onClick = { 
                                Log.d("🔍 EXERCISE_SELECT", "Click su template: ${template.name}, ID: ${template.id}")
                                if (isSelectionMode) {
                                    // In modalità selezione: mostra dialog quantità
                                    selectedExerciseForQuantity = template
                                    showQuantityDialog = true
                                } else {
                                    // Modalità normale: mostra dialog quantità
                                    selectedExerciseForQuantity = template
                                    showQuantityDialog = true
                                }
                            },
                            onLongClick = {
                                // Long press: apre dialog dettagli completo
                                selectedExercise = template
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
                onUpdateExercise = { updatedExercise ->
                    // Aggiorna l'esercizio nella lista
                    exerciseTemplates = exerciseTemplates.map { template ->
                        if (template.id == updatedExercise.id) updatedExercise else template
                    }
                    selectedExercise = updatedExercise
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseTemplateCard(
    template: ExerciseTemplate,
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
            containerColor = MaterialTheme.colorScheme.surface
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
            // Thumbnail dell'esercizio (più compatta)
            ExerciseThumbnail(
                exercise = template,
                modifier = Modifier.size(45.dp),
                size = 45.dp,
                cornerRadius = 6.dp,
                showTypeIcon = false
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            // Nome esercizio compatto
            Text(
                text = template.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 12.sp
            )
            
            // Parametri di default molto compatti
            if (template.defaultReps != null || template.defaultTime != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    template.defaultReps?.let { reps ->
                        Text(
                            text = "${reps}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    if (template.defaultReps != null && template.defaultTime != null) {
                        Text(" • ", style = MaterialTheme.typography.labelSmall)
                    }
                    template.defaultTime?.let { time ->
                        Text(
                            text = "${time}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// Dialog per i dettagli dell'esercizio con editing
@Composable
fun ExerciseDetailDialog(
    exercise: ExerciseTemplate,
    onDismiss: () -> Unit,
    onUpdateExercise: ((ExerciseTemplate) -> Unit)? = null
) {
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingDescription by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(exercise.name) }
    var editedDescription by remember { mutableStateOf(exercise.description) }
    var showImageOptions by remember { mutableStateOf(false) }
    var currentExercise by remember { mutableStateOf(exercise) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        Log.d("IMAGE_PICKER", "🎯 Image picker callback received, URI: $uri")
        uri?.let {
            Log.d("IMAGE_PICKER", "🎯 Immagine selezionata: $uri")
            
            // Aggiorna l'esercizio con la nuova immagine
            val updatedExercise = currentExercise.copy(imagePath = uri.toString())
            Log.d("IMAGE_PICKER", "🎯 currentExercise prima: id=${currentExercise.id}, imagePath=${currentExercise.imagePath}")
            currentExercise = updatedExercise
            Log.d("IMAGE_PICKER", "🎯 currentExercise dopo: id=${updatedExercise.id}, imagePath=${updatedExercise.imagePath}")
            
            // Notifica il parent dell'aggiornamento
            onUpdateExercise?.let { callback ->
                Log.d("IMAGE_PICKER", "🎯 Calling onUpdateExercise callback")
                callback.invoke(updatedExercise)
            } ?: Log.w("IMAGE_PICKER", "🎯 onUpdateExercise callback is null!")
            
            Log.d("IMAGE_PICKER", "🎯 Esercizio aggiornato con imagePath: ${updatedExercise.imagePath}")
        } ?: Log.w("IMAGE_PICKER", "🎯 URI è null!")
    }
    
    // Camera launcher (per future implementazioni)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // TODO: Gestire foto scattata
            Log.d("CAMERA", "Foto scattata con successo")
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = when (exercise.type) {
                        TemplateExerciseType.STRENGTH -> Icons.Default.FitnessCenter
                        TemplateExerciseType.CARDIO -> Icons.Default.DirectionsRun
                        TemplateExerciseType.FLEXIBILITY -> Icons.Default.SelfImprovement
                        TemplateExerciseType.SQUAT_AI -> Icons.Default.Visibility
                        else -> Icons.Default.SportsMartialArts
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Dettagli Esercizio",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Immagine dell'esercizio
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { 
                            showImageOptions = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ExerciseThumbnail(
                            exercise = currentExercise,
                            modifier = Modifier.size(80.dp),
                            size = 80.dp,
                            cornerRadius = 8.dp,
                            showTypeIcon = false
                        )
                        
                        // Overlay per indicare possibilità di cambio immagine
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Cambia immagine",
                                    modifier = Modifier.padding(4.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Nome editabile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nome",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            IconButton(
                                onClick = { 
                                    isEditingName = !isEditingName
                                    if (!isEditingName) {
                                        // TODO: Salvare le modifiche
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditingName) Icons.Default.Save else Icons.Default.Edit,
                                    contentDescription = if (isEditingName) "Salva" else "Modifica",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        if (isEditingName) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Nome esercizio") },
                                singleLine = true
                            )
                        } else {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Descrizione editabile  
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Descrizione",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            IconButton(
                                onClick = { 
                                    isEditingDescription = !isEditingDescription
                                    if (!isEditingDescription) {
                                        // TODO: Salvare le modifiche
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditingDescription) Icons.Default.Save else Icons.Default.Edit,
                                    contentDescription = if (isEditingDescription) "Salva" else "Modifica",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        if (isEditingDescription) {
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { editedDescription = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Descrivi l'esercizio...") },
                                maxLines = 3,
                                minLines = 2
                            )
                        } else {
                            Text(
                                text = if (exercise.description.isNotBlank()) exercise.description else "Nessuna descrizione",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (exercise.description.isNotBlank()) 
                                    MaterialTheme.colorScheme.onSurface else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Info tecniche (non editabili)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tipo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = when (exercise.type) {
                                    TemplateExerciseType.STRENGTH -> "💪 Forza"
                                    TemplateExerciseType.CARDIO -> "🏃 Cardio"
                                    TemplateExerciseType.FLEXIBILITY -> "🧘 Flessibilità"
                                    TemplateExerciseType.BALANCE -> "⚖️ Equilibrio"
                                    TemplateExerciseType.SQUAT_AI -> "🤖 Squat AI"
                                    TemplateExerciseType.CUSTOM -> "✨ Custom"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Modalità",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = when (exercise.mode) {
                                    TemplateExerciseMode.REPS -> "🔢 Ripetizioni"
                                    TemplateExerciseMode.TIME -> "⏱️ Tempo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Chiudi")
            }
        },
        dismissButton = null
    )
    
    // Dialog opzioni immagine
    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { 
                Text("Cambia Immagine", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Scegli come vuoi aggiungere l'immagine per l'esercizio:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Opzione Galleria
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageOptions = false
                                imagePickerLauncher.launch("image/*")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = "Galleria",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "Scegli dalla Galleria",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Seleziona un'immagine esistente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    // Opzione Camera (per future implementazioni)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImageOptions = false
                                // TODO: Implementare scatto foto
                                Log.d("CAMERA", "Scatto foto non ancora implementato")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    "Scatta Foto",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Scatta una nuova foto (presto)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageOptions = false }) {
                    Text("Annulla")
                }
            }
        )
    }
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

@Composable
fun QuantitySelectionDialog(
    exercise: ExerciseTemplate,
    onConfirm: (reps: Int?, time: Int?) -> Unit,
    onCancel: () -> Unit
) {
    var repsText by remember { 
        mutableStateOf((exercise.defaultReps ?: 10).toString()) 
    }
    var timeText by remember { 
        mutableStateOf((exercise.defaultTime ?: 30).toString()) 
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Esercizio: ${exercise.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                when (exercise.mode) {
                    TemplateExerciseMode.REPS -> {
                        Column {
                            Text(
                                text = "Numero di Ripetizioni",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = repsText,
                                onValueChange = { newValue ->
                                    // Permette solo numeri e virgola/punto decimale
                                    if (newValue.matches(Regex("^\\d*[,.]?\\d*$"))) {
                                        repsText = newValue.replace(",", ".")
                                    }
                                },
                                label = { Text("Ripetizioni") },
                                placeholder = { Text("es. 10 o 12.5") },
                                leadingIcon = { 
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null) 
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    TemplateExerciseMode.TIME -> {
                        Column {
                            Text(
                                text = "Durata in Minuti",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = timeText,
                                onValueChange = { newValue ->
                                    // Permette solo numeri e virgola/punto decimale
                                    if (newValue.matches(Regex("^\\d*[,.]?\\d*$"))) {
                                        timeText = newValue.replace(",", ".")
                                    }
                                },
                                label = { Text("Minuti") },
                                placeholder = { Text("es. 5 o 2.5") },
                                leadingIcon = { 
                                    Icon(Icons.Default.Timer, contentDescription = null) 
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (exercise.mode) {
                        TemplateExerciseMode.REPS -> {
                            val reps = repsText.toDoubleOrNull()?.times(1)?.toInt() // Converte da decimale
                            onConfirm(reps, null)
                        }
                        TemplateExerciseMode.TIME -> {
                            val timeMinutes = timeText.toDoubleOrNull()
                            val timeSeconds = timeMinutes?.times(60)?.toInt() // Converte minuti in secondi
                            onConfirm(null, timeSeconds)
                        }
                    }
                },
                enabled = when (exercise.mode) {
                    TemplateExerciseMode.REPS -> repsText.toDoubleOrNull() != null && repsText.toDoubleOrNull()!! > 0
                    TemplateExerciseMode.TIME -> timeText.toDoubleOrNull() != null && timeText.toDoubleOrNull()!! > 0
                }
            ) {
                Text("Conferma", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Annulla")
            }
        }
    )
}