package com.programminghut.pose_detection.ui.export

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.utils.FileExportHelper

/**
 * Export Settings Screen - Configure personal context for TXT exports
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_export_context", Context.MODE_PRIVATE) }
    
    var name by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var goal by remember { mutableStateOf(prefs.getString("user_goal", "") ?: "") }
    var motivation by remember { mutableStateOf(prefs.getString("user_motivation", "") ?: "") }
    var habits by remember { mutableStateOf(prefs.getString("user_habits", "") ?: "") }
    var notes by remember { mutableStateOf(prefs.getString("user_notes", "") ?: "") }
    
    var showSaveConfirmation by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo Export") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            FileExportHelper.saveUserContext(
                                context = context,
                                name = name.ifBlank { null },
                                goal = goal.ifBlank { null },
                                motivation = motivation.ifBlank { null },
                                habits = habits.ifBlank { null },
                                notes = notes.ifBlank { null }
                            )
                            showSaveConfirmation = true
                        }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Salva")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = "Personalizza i tuoi export TXT",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Le informazioni che inserisci qui appariranno all'inizio dei file TXT esportati, permettendoti di condividere il tuo percorso personale.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                placeholder = { Text("Il tuo nome") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Goal field
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Obiettivo Fitness") },
                placeholder = { Text("Es: Perdere peso, aumentare massa muscolare...") },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            // Motivation field
            OutlinedTextField(
                value = motivation,
                onValueChange = { motivation = it },
                label = { Text("La tua Motivazione") },
                placeholder = { Text("Perché hai iniziato questo percorso?") },
                leadingIcon = { Icon(Icons.Filled.FavoriteBorder, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            
            // Habits field
            OutlinedTextField(
                value = habits,
                onValueChange = { habits = it },
                label = { Text("Abitudini Chiave") },
                placeholder = { Text("• Allenamento 3x/settimana\n• Stretching quotidiano\n• 8 ore di sonno") },
                leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            
            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note Personali") },
                placeholder = { Text("Riflessioni, citazioni preferite, promemoria...") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            
            // Save confirmation snackbar
            if (showSaveConfirmation) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSaveConfirmation = false
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "✅ Profilo salvato con successo!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
