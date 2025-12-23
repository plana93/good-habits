package com.programminghut.pose_detection.ui.exercise

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Phase 6: Exercise Browser Screen
 * 
 * Schermata per navigare tra gli esercizi disponibili.
 * Features:
 * - Lista esercizi con preview
 * - Filtri per tipo, difficoltà, gruppo muscolare
 * - Search per nome
 * - Quick start esercizio
 * - Dettagli esercizio con regole
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseBrowserScreen(
    exercisesFlow: Flow<List<Exercise>>,
    onExerciseSelected: (Exercise) -> Unit,
    onStartExercise: (Exercise) -> Unit,
    onCreateCustomExercise: (() -> Unit)? = null,  // Reso opzionale
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val exercises by exercisesFlow.collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<ExerciseType?>(null) }
    var selectedDifficulty by remember { mutableStateOf<ExerciseDifficulty?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    
    // Filtra esercizi
    // TODO: Rimuovere questo filtro quando MainActivity sarà exercise-agnostic
    val filteredExercises = remember(exercises, searchQuery, selectedType, selectedDifficulty) {
        exercises.filter { exercise ->
            // TEMPORARY: Solo SQUAT supportato per ora
            val isSquat = exercise.type == ExerciseType.SQUAT
            
            val matchesSearch = searchQuery.isEmpty() || 
                               exercise.name.contains(searchQuery, ignoreCase = true) ||
                               exercise.description.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedType == null || exercise.type == selectedType
            // Note: difficulty filtering requires preset data, simplified here
            isSquat && matchesSearch && matchesType
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Esercizi Disponibili") },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
                actions = {
                    // Filtri toggle
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = "Filtri"
                        )
                    }
                    
                    // Crea esercizio custom (solo se callback fornito)
                    if (onCreateCustomExercise != null) {
                        IconButton(onClick = onCreateCustomExercise) {
                            Icon(Icons.Default.Add, contentDescription = "Nuovo esercizio")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Info banner temporaneo
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Al momento sono supportati solo esercizi di tipo Squat. Altri esercizi in arrivo!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Filtri (espandibili)
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterSection(
                    selectedType = selectedType,
                    selectedDifficulty = selectedDifficulty,
                    onTypeSelected = { selectedType = it },
                    onDifficultySelected = { selectedDifficulty = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Statistiche
            Text(
                text = "${filteredExercises.size} esercizi disponibili",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Lista esercizi
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredExercises, key = { it.exerciseId }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onClick = { selectedExercise = exercise },
                        onStartClick = { onStartExercise(exercise) }
                    )
                }
                
                if (filteredExercises.isEmpty()) {
                    item {
                        EmptyState(
                            message = "Nessun esercizio trovato",
                            onCreateClick = onCreateCustomExercise
                        )
                    }
                }
            }
        }
    }
    
    // Dialog dettagli esercizio
    selectedExercise?.let { exercise ->
        ExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { selectedExercise = null },
            onStart = {
                onStartExercise(exercise)
                selectedExercise = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Cerca esercizi...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancella")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    selectedType: ExerciseType?,
    selectedDifficulty: ExerciseDifficulty?,
    onTypeSelected: (ExerciseType?) -> Unit,
    onDifficultySelected: (ExerciseDifficulty?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Filtro per tipo
        Text(
            text = "Tipo Esercizio",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("Tutti") }
            )
            
            ExerciseType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(if (selectedType == type) null else type) },
                    label = { Text(type.toDisplayString()) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filtro per difficoltà
        Text(
            text = "Difficoltà",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDifficulty == null,
                onClick = { onDifficultySelected(null) },
                label = { Text("Tutte") }
            )
            
            ExerciseDifficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = selectedDifficulty == difficulty,
                    onClick = { 
                        onDifficultySelected(if (selectedDifficulty == difficulty) null else difficulty) 
                    },
                    label = { Text(difficulty.toDisplayString()) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCard(
    exercise: Exercise,
    onClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con nome e tipo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExerciseTypeBadge(type = exercise.type)
                        
                        if (exercise.isCustom) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(text = "Custom", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                // Pulsante start rapido
                FilledTonalButton(
                    onClick = onStartClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inizia")
                }
            }
            
            if (exercise.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Statistiche esercizio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExerciseStat(
                    icon = Icons.Default.Star, // Icona per il mode
                    label = exercise.mode.name // "REPS" o "TIME"
                )
                
                if (exercise.tags.isNotEmpty()) {
                    ExerciseStat(
                        icon = Icons.Default.Info,
                        label = exercise.tags.first()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseTypeBadge(type: ExerciseType, modifier: Modifier = Modifier) {
    val color = when (type) {
        ExerciseType.SQUAT -> Color(0xFF4CAF50)
        ExerciseType.PUSH_UP -> Color(0xFF2196F3)
        ExerciseType.PULL_UP -> Color(0xFF9C27B0)
        ExerciseType.LUNGE -> Color(0xFFFF9800)
        ExerciseType.PLANK -> Color(0xFFF44336)
        ExerciseType.CUSTOM -> Color(0xFF607D8B)
    }
    
    Badge(
        containerColor = color.copy(alpha = 0.2f),
        contentColor = color,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = type.toDisplayString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExerciseStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(
    message: String,
    onCreateClick: (() -> Unit)? = null,  // Reso opzionale
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Mostra il pulsante solo se callback è fornito
        if (onCreateClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            FilledTonalButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crea Esercizio")
            }
        }
    }
}

@Composable
private fun ExerciseDetailDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Regole di Validazione",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Mostra mode e descrizione invece delle rules
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Modalità: ${if (exercise.mode.name == "REPS") "Ripetizioni" else "A Tempo"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (exercise.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = exercise.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                if (exercise.tags.isNotEmpty()) {
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            exercise.tags.forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = onStart) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Inizia Allenamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}

// === EXTENSION FUNCTIONS ===

internal fun ExerciseType.toDisplayString(): String = when (this) {
    ExerciseType.SQUAT -> "Squat"
    ExerciseType.PUSH_UP -> "Push-up"
    ExerciseType.PULL_UP -> "Pull-up"
    ExerciseType.LUNGE -> "Lunge"
    ExerciseType.PLANK -> "Plank"
    ExerciseType.CUSTOM -> "Custom"
}

internal fun ExerciseDifficulty.toDisplayString(): String = when (this) {
    ExerciseDifficulty.BEGINNER -> "Base"
    ExerciseDifficulty.INTERMEDIATE -> "Intermedio"
    ExerciseDifficulty.ADVANCED -> "Avanzato"
    ExerciseDifficulty.EXPERT -> "Esperto"
}
