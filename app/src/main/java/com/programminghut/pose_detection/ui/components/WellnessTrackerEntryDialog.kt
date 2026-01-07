package com.programminghut.pose_detection.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.programminghut.pose_detection.data.model.WellnessTrackerTemplate
import com.programminghut.pose_detection.data.model.TrackerResponseType
import com.programminghut.pose_detection.data.model.TrackerResponse
import com.programminghut.pose_detection.data.model.CustomEmotion

/**
 * Dialog per inserire la risposta a un wellness tracker
 */
@Composable
fun WellnessTrackerEntryDialog(
    tracker: WellnessTrackerTemplate,
    onSave: (TrackerResponse) -> Unit,
    onDismiss: () -> Unit
) {
    // State per la risposta
    var selectedRating by remember { mutableStateOf<Int?>(null) }
    var selectedBoolean by remember { mutableStateOf<Boolean?>(null) }
    var selectedEmotion by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    var selectedReferenceDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Quick date options
    val dateOptions = remember { 
        ReferenceDateHelper.getQuickDateOptions(7) 
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tracker.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tracker.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider()
                
                // Content - scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Reference Date Selector
                    ReferenceDateSelector(
                        selectedDate = selectedReferenceDate,
                        dateOptions = dateOptions,
                        onDateSelected = { selectedReferenceDate = it }
                    )
                    
                    Divider()
                    
                    // Response Input (dipende dal tipo)
                    when (tracker.responseType) {
                        TrackerResponseType.RATING_5 -> {
                            RatingInput(
                                tracker = tracker,
                                selectedRating = selectedRating,
                                onRatingSelected = { selectedRating = it }
                            )
                        }
                        TrackerResponseType.BOOLEAN -> {
                            BooleanInput(
                                selectedBoolean = selectedBoolean,
                                onBooleanSelected = { selectedBoolean = it }
                            )
                        }
                        TrackerResponseType.EMOTION_SET -> {
                            EmotionSetInput(
                                tracker = tracker,
                                selectedEmotion = selectedEmotion,
                                onEmotionSelected = { selectedEmotion = it }
                            )
                        }
                        TrackerResponseType.TEXT_NOTE -> {
                            // Il campo note è sempre visibile sotto
                        }
                    }
                    
                    Divider()
                    
                    // Note field (sempre disponibile)
                    NotesInput(
                        noteText = noteText,
                        onNoteChange = { noteText = it }
                    )
                }
                
                Divider()
                
                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            // Create response
                            val response = TrackerResponse(
                                trackerId = tracker.id,
                                trackerName = tracker.name,
                                responseType = tracker.responseType,
                                ratingValue = selectedRating,
                                booleanValue = selectedBoolean,
                                selectedEmotion = selectedEmotion,
                                textNote = noteText.takeIf { it.isNotBlank() },
                                timestamp = System.currentTimeMillis(),
                                referenceDate = selectedReferenceDate
                            )
                            onSave(response)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = when (tracker.responseType) {
                            TrackerResponseType.RATING_5 -> selectedRating != null
                            TrackerResponseType.BOOLEAN -> selectedBoolean != null
                            TrackerResponseType.EMOTION_SET -> selectedEmotion != null
                            TrackerResponseType.TEXT_NOTE -> noteText.isNotBlank()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Reference Date Selector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceDateSelector(
    selectedDate: Long,
    dateOptions: List<ReferenceDateHelper.DateOption>,
    onDateSelected: (Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "When did this happen?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dateOptions) { option ->
                FilterChip(
                    selected = option.timestamp == selectedDate,
                    onClick = { onDateSelected(option.timestamp) },
                    label = { Text(option.label) }
                )
            }
        }
    }
}

/**
 * Rating Input (0-5 with emoticons)
 */
@Composable
fun RatingInput(
    tracker: WellnessTrackerTemplate,
    selectedRating: Int?,
    onRatingSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "How was it?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        // Emoticons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tracker.emoticons?.forEachIndexed { index, emoticon ->
                val ratingValue = index + 1  // Rating from 1 to 5
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onRatingSelected(ratingValue) }
                        .padding(8.dp)
                ) {
                    Text(
                        text = emoticon,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (selectedRating == ratingValue) {
                                    Modifier
                                } else {
                                    Modifier
                                }
                            )
                    )
                    
                    // Label
                    tracker.labels?.getOrNull(index)?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedRating == ratingValue) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                            fontWeight = if (selectedRating == ratingValue) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Boolean Input (Yes/No)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooleanInput(
    selectedBoolean: Boolean?,
    onBooleanSelected: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your answer",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = selectedBoolean == true,
                onClick = { onBooleanSelected(true) },
                label = { Text("Yes") },
                modifier = Modifier.weight(1f)
            )
            
            FilterChip(
                selected = selectedBoolean == false,
                onClick = { onBooleanSelected(false) },
                label = { Text("No") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Emotion Set Input
 */
@Composable
fun EmotionSetInput(
    tracker: WellnessTrackerTemplate,
    selectedEmotion: String?,
    onEmotionSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "What happened?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        tracker.customEmotions?.let { emotions ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emotions.forEach { emotion ->
                    EmotionOption(
                        emotion = emotion,
                        isSelected = selectedEmotion == emotion.emoticon,
                        onClick = { onEmotionSelected(emotion.emoticon) }
                    )
                }
            }
        }
    }
}

/**
 * Single emotion option
 */
@Composable
fun EmotionOption(
    emotion: CustomEmotion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = emotion.emoticon,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = emotion.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Notes Input
 */
@Composable
fun NotesInput(
    noteText: String,
    onNoteChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Notes (optional)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add any additional notes...") },
            minLines = 3,
            maxLines = 5
        )
    }
}
