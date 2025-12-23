package com.programminghut.pose_detection.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.programminghut.pose_detection.R
import com.programminghut.pose_detection.data.model.ExerciseTemplate
import com.programminghut.pose_detection.data.model.TemplateExerciseType
import com.programminghut.pose_detection.util.ThumbnailGenerator
import java.io.File

/**
 * Componente per visualizzare la thumbnail di un esercizio
 * Gestisce automaticamente:
 * - Caricamento da file esistente
 * - Generazione on-demand se mancante
 * - Placeholder colorati per tipo
 * - Fallback per errori
 */
@Composable
fun ExerciseThumbnail(
    exercise: ExerciseTemplate,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    cornerRadius: Dp = 12.dp,
    showTypeIcon: Boolean = true
) {
    val context = LocalContext.current
    var thumbnailPath by remember(exercise.id) { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    // Genera thumbnail se necessario
    LaunchedEffect(exercise.id) {
        try {
            val path = ThumbnailGenerator.generateThumbnail(
                context = context,
                exerciseId = exercise.id,
                imagePath = exercise.imagePath,
                exerciseType = exercise.type,
                exerciseName = exercise.name
            )
            thumbnailPath = path
            isLoading = false
        } catch (e: Exception) {
            hasError = true
            isLoading = false
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        when {
            isLoading -> {
                // Skeleton loader
                LoadingSkeleton(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            hasError || thumbnailPath == null -> {
                // Fallback placeholder
                TypePlaceholder(
                    exerciseType = exercise.type,
                    exerciseName = exercise.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                // Thumbnail generata
                AsyncImage(
                    model = File(thumbnailPath!!),
                    contentDescription = "Thumbnail ${exercise.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { hasError = true }
                )
            }
        }
        
        // Icona del tipo nell'angolo
        if (showTypeIcon) {
            TypeIconBadge(
                exerciseType = exercise.type,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}

/**
 * Skeleton loader per il caricamento
 */
@Composable
private fun LoadingSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                RoundedCornerShape(8.dp)
            )
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Placeholder colorato quando non c'è thumbnail
 */
@Composable
private fun TypePlaceholder(
    exerciseType: TemplateExerciseType,
    exerciseName: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = getTypeColor(exerciseType)
    val icon = getTypeIcon(exerciseType)
    
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            // Iniziali del nome
            Text(
                text = getInitials(exerciseName),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Badge con icona del tipo di esercizio
 */
@Composable
private fun TypeIconBadge(
    exerciseType: TemplateExerciseType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(20.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Icon(
            imageVector = getTypeIcon(exerciseType),
            contentDescription = "Tipo: $exerciseType",
            tint = getTypeColor(exerciseType),
            modifier = Modifier
                .padding(2.dp)
                .size(16.dp)
        )
    }
}

/**
 * Ottiene il colore associato al tipo di esercizio
 */
private fun getTypeColor(type: TemplateExerciseType): Color {
    return when (type) {
        TemplateExerciseType.STRENGTH -> Color(0xFFE57373) // Rosso
        TemplateExerciseType.CARDIO -> Color(0xFF64B5F6)   // Blu
        TemplateExerciseType.FLEXIBILITY -> Color(0xFF81C784) // Verde
        TemplateExerciseType.SQUAT_AI -> Color(0xFFBA68C8)   // Viola
        TemplateExerciseType.BALANCE -> Color(0xFF90A4AE)    // Grigio
        TemplateExerciseType.CUSTOM -> Color(0xFFFFB74D)     // Arancione
    }
}

/**
 * Ottiene l'icona associata al tipo di esercizio
 */
private fun getTypeIcon(type: TemplateExerciseType): ImageVector {
    return when (type) {
        TemplateExerciseType.STRENGTH -> Icons.Default.FitnessCenter
        TemplateExerciseType.CARDIO -> Icons.Default.DirectionsRun
        TemplateExerciseType.FLEXIBILITY -> Icons.Default.SelfImprovement
        TemplateExerciseType.SQUAT_AI -> Icons.Default.Visibility
        TemplateExerciseType.BALANCE -> Icons.Default.Balance
        TemplateExerciseType.CUSTOM -> Icons.Default.FitnessCenter
    }
}

/**
 * Estrae le iniziali dal nome
 */
private fun getInitials(name: String): String {
    return name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
}