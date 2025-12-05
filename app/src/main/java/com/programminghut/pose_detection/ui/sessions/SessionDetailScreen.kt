package com.programminghut.pose_detection.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.WorkoutSession
import com.programminghut.pose_detection.data.repository.SessionStatistics
import java.text.SimpleDateFormat
import java.util.*

/**
 * Session Detail Screen - Composable
 * 
 * Displays detailed information about a single workout session including:
 * - Session metadata and statistics
 * - Rep-by-rep breakdown
 * - Quality metrics
 * - Notes and tags
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettagli Sessione") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is SessionDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is SessionDetailUiState.Success -> {
                SessionDetailContent(
                    session = state.session,
                    reps = state.reps,
                    statistics = state.statistics,
                    onFlagRep = { repId, isFlagged -> viewModel.flagRep(repId, isFlagged) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is SessionDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDetailContent(
    session: WorkoutSession,
    reps: List<RepData>,
    statistics: SessionStatistics?,
    onFlagRep: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Session header
        item {
            SessionHeaderCard(session = session)
        }
        
        // Statistics summary
        if (statistics != null) {
            item {
                StatisticsCard(statistics = statistics)
            }
        }
        
        // Reps breakdown header
        item {
            Text(
                text = "Ripetizioni (${reps.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Individual reps
        itemsIndexed(reps) { index, rep ->
            RepCard(
                rep = rep,
                onFlagToggle = { onFlagRep(rep.repId, !rep.isFlaggedForReview) }
            )
        }
    }
}

/**
 * Session header card with main info
 */
@Composable
fun SessionHeaderCard(session: WorkoutSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exercise type badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = session.exerciseType,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Date and time
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = formatFullDate(session.startTime),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Duration
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Durata: ${formatDuration(session.durationSeconds)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Device info (if available)
            session.deviceModel?.let { device ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = device,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Statistics summary card
 */
@Composable
fun StatisticsCard(statistics: SessionStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Statistiche Dettagliate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatMetric(
                    label = "Reps",
                    value = "${statistics.totalReps}",
                    icon = Icons.Filled.Star
                )
                StatMetric(
                    label = "Velocità Media",
                    value = String.format("%.1fs", statistics.avgSpeed),
                    icon = Icons.Filled.Star
                )
            }
            
            Divider()
            
            // Quality scores
            Text(
                text = "Qualità Media",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            QualityBar(
                label = "Form",
                score = statistics.avgFormScore
            )
            
            QualityBar(
                label = "Depth",
                score = statistics.avgDepthScore
            )
            
            if (statistics.flaggedRepsCount > 0) {
                Divider()
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "${statistics.flaggedRepsCount} ripetizioni segnalate per revisione",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun StatMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QualityBar(label: String, score: Float) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(score * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgressIndicator(
            progress = score,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = getQualityColor(score)
        )
    }
}

/**
 * Individual rep card
 */
@Composable
fun RepCard(
    rep: RepData,
    onFlagToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (rep.isFlaggedForReview) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rep number
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${rep.repNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Metrics
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Form:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${(rep.formScore * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = getQualityColor(rep.formScore)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Depth:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${(rep.depthScore * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = getQualityColor(rep.depthScore)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tempo:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format("%.1fs", rep.speed),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Flag button
            IconButton(onClick = onFlagToggle) {
                Icon(
                    imageVector = if (rep.isFlaggedForReview) Icons.Filled.Warning else Icons.Filled.Star,
                    contentDescription = if (rep.isFlaggedForReview) "Rimuovi segnalazione" else "Segnala",
                    tint = if (rep.isFlaggedForReview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Helper functions
 */
private fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy 'alle' HH:mm", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        hours > 0 -> String.format("%dh %dm %ds", hours, minutes, secs)
        minutes > 0 -> String.format("%dm %ds", minutes, secs)
        else -> String.format("%ds", secs)
    }
}

private fun getQualityColor(score: Float): Color {
    return when {
        score >= 0.8f -> Color(0xFF4CAF50) // Green
        score >= 0.6f -> Color(0xFFFFC107) // Amber
        else -> Color(0xFFFF5722) // Red
    }
}
