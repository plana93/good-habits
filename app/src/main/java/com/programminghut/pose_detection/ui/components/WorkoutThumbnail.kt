package com.programminghut.pose_detection.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.data.model.WorkoutTemplate
import com.programminghut.pose_detection.data.model.ExerciseTemplate
import com.programminghut.pose_detection.data.model.TemplateExerciseType

/**
 * Componente per visualizzare la thumbnail di un workout (allenamento)
 * Mostra una griglia delle prime miniature degli esercizi contenuti
 */
@Composable
fun WorkoutThumbnail(
    workout: WorkoutTemplate,
    availableExercises: List<ExerciseTemplate>,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    cornerRadius: Dp = 12.dp
) {
    // Recupera i template degli esercizi per questo workout
    val exerciseTemplates = remember(workout.exercises, availableExercises) {
        workout.exercises.mapNotNull { workoutExercise ->
            availableExercises.find { it.id == workoutExercise.exerciseId }
        }.take(4) // Massimo 4 esercizi mostrati
    }
    
    Card(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (exerciseTemplates.size) {
                0 -> {
                    // Placeholder vuoto
                    WorkoutEmptyPlaceholder(
                        workoutName = workout.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    // Un solo esercizio - thumbnail singola
                    ExerciseThumbnail(
                        exercise = exerciseTemplates[0],
                        modifier = Modifier.fillMaxSize(),
                        size = size,
                        cornerRadius = 0.dp,
                        showTypeIcon = false
                    )
                }
                2 -> {
                    // Due esercizi - split verticale
                    Row(modifier = Modifier.fillMaxSize()) {
                        ExerciseThumbnail(
                            exercise = exerciseTemplates[0],
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            size = size / 2,
                            cornerRadius = 0.dp,
                            showTypeIcon = false
                        )
                        ExerciseThumbnail(
                            exercise = exerciseTemplates[1],
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            size = size / 2,
                            cornerRadius = 0.dp,
                            showTypeIcon = false
                        )
                    }
                }
                3 -> {
                    // Tre esercizi - primo grande, altri due piccoli a destra
                    Row(modifier = Modifier.fillMaxSize()) {
                        ExerciseThumbnail(
                            exercise = exerciseTemplates[0],
                            modifier = Modifier.weight(2f).fillMaxHeight(),
                            size = size * 2 / 3,
                            cornerRadius = 0.dp,
                            showTypeIcon = false
                        )
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            ExerciseThumbnail(
                                exercise = exerciseTemplates[1],
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                size = size / 3,
                                cornerRadius = 0.dp,
                                showTypeIcon = false
                            )
                            ExerciseThumbnail(
                                exercise = exerciseTemplates[2],
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                size = size / 3,
                                cornerRadius = 0.dp,
                                showTypeIcon = false
                            )
                        }
                    }
                }
                else -> {
                    // Quattro o più esercizi - griglia 2x2
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            ExerciseThumbnail(
                                exercise = exerciseTemplates[0],
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                size = size / 2,
                                cornerRadius = 0.dp,
                                showTypeIcon = false
                            )
                            ExerciseThumbnail(
                                exercise = exerciseTemplates[1],
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                size = size / 2,
                                cornerRadius = 0.dp,
                                showTypeIcon = false
                            )
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            ExerciseThumbnail(
                                exercise = exerciseTemplates[2],
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                size = size / 2,
                                cornerRadius = 0.dp,
                                showTypeIcon = false
                            )
                            if (exerciseTemplates.size > 3) {
                                ExerciseThumbnail(
                                    exercise = exerciseTemplates[3],
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    size = size / 2,
                                    cornerRadius = 0.dp,
                                    showTypeIcon = false
                                )
                            } else {
                                // Spazio vuoto
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            // Overlay con numero di esercizi se ce ne sono più di 4
            if (workout.exercises.size > 4) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "+${workout.exercises.size - 4}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Placeholder per workout senza esercizi
 */
@Composable
private fun WorkoutEmptyPlaceholder(
    workoutName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = getWorkoutInitials(workoutName),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Workout",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Estrae le iniziali dal nome del workout
 */
private fun getWorkoutInitials(name: String): String {
    return name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
}