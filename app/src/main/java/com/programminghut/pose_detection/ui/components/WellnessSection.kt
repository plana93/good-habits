package com.programminghut.pose_detection.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programminghut.pose_detection.data.model.WellnessTrackerTemplate
import com.programminghut.pose_detection.data.model.TrackerResponseType
import com.programminghut.pose_detection.data.model.TrackerResponse
import com.programminghut.pose_detection.data.model.DailySessionItemWithDetails

/**
 * Wellness Section - Sezione separata per wellness tracking nella Today screen
 */
@Composable
fun WellnessSection(
    wellnessItems: List<DailySessionItemWithDetails>,
    onAddWellnessClick: () -> Unit,
    onItemClick: (DailySessionItemWithDetails) -> Unit,
    onItemDelete: (DailySessionItemWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ State per espansione/contrazione
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header (clickable per espandere/contrarre)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (wellnessItems.isNotEmpty()) {
                            isExpanded = !isExpanded 
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = "Wellness",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Wellness Check-in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track your mental & physical state",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (wellnessItems.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${wellnessItems.size}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // ✅ Icona per espandere/contrarre
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // ✅ Contenuto espandibile
            if (wellnessItems.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                // Empty state
                EmptyWellnessState(onAddClick = onAddWellnessClick)
            } else if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                // Lista wellness trackers
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wellnessItems.forEach { item ->
                        WellnessTrackerCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemDelete(item) }
                        )
                    }
                    
                    // Add more button
                    TextButton(
                        onClick = onAddWellnessClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add tracker",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add another tracker")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyWellnessState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Spa,
                contentDescription = "Wellness",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No wellness entries yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Track your mood, energy, sleep and more",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Wellness Tracker")
        }
    }
}

/**
 * Card per singolo wellness tracker completato
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WellnessTrackerCard(
    item: DailySessionItemWithDetails,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Load template to get name and emoticons
    val template = remember(item.trackerTemplateId) {
        item.trackerTemplateId?.let { templateId ->
            val fileManager = com.programminghut.pose_detection.data.manager.WellnessTrackerFileManager(context)
            fileManager.getTrackerById(templateId)
        }
    }
    
    // Parse response JSON
    val response = remember(item.trackerResponseJson) {
        item.trackerResponseJson?.let { 
            TrackerResponse.fromJson(it) 
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tracker icon/emoticon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                template?.let { tmpl ->
                    // Use iconName (which is already an emoticon in the JSON)
                    Text(
                        text = tmpl.iconName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 28.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Tracker name from template
                Text(
                    text = template?.name ?: "Wellness Tracker",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Show reference date if different from today
                response?.let { resp ->
                    val daysAgo = resp.getDaysAgo()
                    if (daysAgo > 0) {
                        Text(
                            text = resp.getReferenceDateDescription(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Show response value with emoticon
            response?.let { resp ->
                template?.let { tmpl ->
                    ResponseValueDisplay(response = resp, template = tmpl)
                }
            }
        }
        
        // Show notes if present
        response?.textNote?.takeIf { it.isNotBlank() }?.let { noteText ->
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.StickyNote2,
                    contentDescription = "Note",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Display del valore della risposta (emoticon o numero)
 */
@Composable
fun ResponseValueDisplay(
    response: TrackerResponse,
    template: WellnessTrackerTemplate
) {
    // Debug logging
    android.util.Log.d("WELLNESS_DISPLAY", "Response type: ${response.responseType}, Rating: ${response.ratingValue}, Emotion: ${response.selectedEmotion}")
    android.util.Log.d("WELLNESS_DISPLAY", "Template emoticons: ${template.emoticons?.joinToString()}")
    
    when (response.responseType) {
        TrackerResponseType.RATING_5 -> {
            response.ratingValue?.let { rating ->
                // Show emoticon corresponding to the rating
                val emoticon = when {
                    rating >= 1 && rating <= 5 -> {
                        template.emoticons?.getOrNull(rating - 1) ?: rating.toString()
                    }
                    else -> rating.toString()
                }
                
                android.util.Log.d("WELLNESS_DISPLAY", "Rating $rating -> emoticon: $emoticon")
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoticon,
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 32.sp
                    )
                }
            }
        }
        TrackerResponseType.BOOLEAN -> {
            response.booleanValue?.let { value ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (value) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else 
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (value) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                        contentDescription = if (value) "Yes" else "No",
                        tint = if (value) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        TrackerResponseType.EMOTION_SET -> {
            response.selectedEmotion?.let { emotion ->
                // Show selected emotion
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emotion,
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 32.sp
                    )
                }
            }
        }
        TrackerResponseType.TEXT_NOTE -> {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = "Note",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
