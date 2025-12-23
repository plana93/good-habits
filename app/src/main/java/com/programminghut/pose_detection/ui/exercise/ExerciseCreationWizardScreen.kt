package com.programminghut.pose_detection.ui.exercise

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.data.model.ExerciseMode
import com.programminghut.pose_detection.data.model.ExerciseType

enum class WizardStep {
    TYPE_SELECTION,
    BASIC_INFO,
    IMAGE_UPLOAD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCreationWizardScreen(
    wizardData: ExerciseWizardData = ExerciseWizardData(),
    onDataChanged: (ExerciseWizardData) -> Unit = {},
    onComplete: (ExerciseWizardData) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(WizardStep.TYPE_SELECTION) }
    var localWizardData by remember { mutableStateOf(wizardData) }

    LaunchedEffect(wizardData) {
        localWizardData = wizardData
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crea un nuovo esercizio")
                        Text(
                            text = getStepTitle(currentStep),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LinearProgressIndicator(
                progress = (currentStep.ordinal + 1) / WizardStep.values().size.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentStep,
                label = "wizard-step"
            ) { step ->
                when (step) {
                    WizardStep.TYPE_SELECTION -> TypeSelectionStep(
                        selectedMode = localWizardData.mode,
                        onModeSelected = {
                            localWizardData = localWizardData.copy(mode = it)
                            onDataChanged(localWizardData)
                        },
                        onNext = { currentStep = WizardStep.BASIC_INFO }
                    )

                    WizardStep.BASIC_INFO -> BasicInfoStep(
                        name = localWizardData.name,
                        description = localWizardData.description,
                        onNameChanged = {
                            localWizardData = localWizardData.copy(name = it)
                            onDataChanged(localWizardData)
                        },
                        onDescriptionChanged = {
                            localWizardData = localWizardData.copy(description = it)
                            onDataChanged(localWizardData)
                        },
                        onNext = { currentStep = WizardStep.IMAGE_UPLOAD },
                        onBack = { currentStep = WizardStep.TYPE_SELECTION }
                    )

                    WizardStep.IMAGE_UPLOAD -> ImageUploadStep(
                        imagePath = localWizardData.imagePath,
                        onImageSelected = { path ->
                            localWizardData = localWizardData.copy(imagePath = path)
                            onDataChanged(localWizardData)
                        },
                        onBack = { currentStep = WizardStep.BASIC_INFO },
                        onComplete = { onComplete(localWizardData) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelectionStep(
    selectedMode: ExerciseMode?,
    onModeSelected: (ExerciseMode) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Che tipo di esercizio vuoi creare?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scegli se vuoi tracciare ripetizioni oppure un tempo",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        ExerciseModeCard(
            title = "Ripetizioni",
            subtitle = "Movimenti dinamici",
            description = "Push-up, pull-up, squat e tutti gli esercizi che contano reps",
            examples = listOf("Push-up", "Pull-up", "Squat"),
            icon = Icons.Default.Refresh,
            iconBackground = Color(0xFF4CAF50),
            isSelected = selectedMode == ExerciseMode.REPS,
            onClick = { onModeSelected(ExerciseMode.REPS) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExerciseModeCard(
            title = "Isometrico",
            subtitle = "A tempo",
            description = "Plank, wall sit e tutti gli esercizi da mantenere per X secondi",
            examples = listOf("Plank", "Wall sit", "Hollow hold"),
            icon = Icons.Default.Lock,
            iconBackground = Color(0xFFF44336),
            isSelected = selectedMode == ExerciseMode.TIME,
            onClick = { onModeSelected(ExerciseMode.TIME) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            enabled = selectedMode != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continua")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicInfoStep(
    name: String,
    description: String,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Dettagli esercizio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Descrizione") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Categoria",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        val types = listOf(
            "Squat",
            "Push-up",
            "Pull-up",
            "Lunge",
            "Plank",
            "Altro"
        )

        types.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { label ->
                    FilterChip(
                        selected = label == "Altro",
                        onClick = { /* placeholder per future categorie */ },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Indietro")
            }

            Button(
                onClick = onNext,
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text("Continua")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ImageUploadStep(
    imagePath: String?,
    onImageSelected: (String?) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Aggiungi un'immagine (opzionale)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "L'immagine verrà usata come anteprima e miniatura",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    // TODO: implementare image picker reale
                    onImageSelected(
                        if (imagePath == null) {
                            "placeholder://image"
                        } else {
                            null
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = if (imagePath == null) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val previewIcon = if (imagePath == null) Icons.Default.AddCircle else Icons.Default.Check
                val previewTint = if (imagePath == null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
                Icon(
                    imageVector = previewIcon,
                    contentDescription = null,
                    tint = previewTint,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (imagePath == null) "Tocca per selezionare" else "Immagine selezionata",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "L'immagine è opzionale",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Puoi modificarla anche dopo aver salvato l'esercizio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Indietro")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salva esercizio")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseModeCard(
    title: String,
    subtitle: String,
    description: String,
    examples: List<String>,
    icon: ImageVector,
    iconBackground: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        tonalElevation = if (isSelected) 8.dp else 0.dp,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(description, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                examples.forEach { example ->
                    AssistChip(
                        onClick = {},
                        label = { Text(example) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun getStepTitle(step: WizardStep): String = when (step) {
    WizardStep.TYPE_SELECTION -> "Passo 1 di 3 · Tipo"
    WizardStep.BASIC_INFO -> "Passo 2 di 3 · Dettagli"
    WizardStep.IMAGE_UPLOAD -> "Passo 3 di 3 · Immagine"
}

data class ExerciseWizardData(
    val mode: ExerciseMode? = null,
    val name: String = "",
    val description: String = "",
    val imagePath: String? = null,
    val type: ExerciseType = ExerciseType.CUSTOM
)
