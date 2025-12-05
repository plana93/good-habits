package com.programminghut.pose_detection.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.programminghut.pose_detection.data.model.WorkoutSession
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sessions List Screen - Composable
 * 
 * Displays a list of workout sessions with statistics and filtering options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsListScreen(
    viewModel: SessionsViewModel,
    onSessionClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storico Allenamenti") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            FilterChips(
                currentFilter = filterType,
                onFilterChanged = { viewModel.filterSessions(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Content based on state
            when (val state = uiState) {
                is SessionsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is SessionsUiState.Empty -> {
                    EmptyState(modifier = Modifier.fillMaxSize())
                }
                
                is SessionsUiState.Success -> {
                    // Stats summary card
                    StatsCard(
                        stats = state.stats,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    // Sessions list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.sessions, key = { it.sessionId }) { session ->
                            SessionCard(
                                session = session,
                                onClick = { onSessionClick(session.sessionId) }
                            )
                        }
                    }
                }
                
                is SessionsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadSessions() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Filter chips for session filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    currentFilter: FilterType,
    onFilterChanged: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == FilterType.ALL,
            onClick = { onFilterChanged(FilterType.ALL) },
            label = { Text("Tutti") }
        )
        FilterChip(
            selected = currentFilter == FilterType.TODAY,
            onClick = { onFilterChanged(FilterType.TODAY) },
            label = { Text("Oggi") }
        )
        FilterChip(
            selected = currentFilter == FilterType.THIS_WEEK,
            onClick = { onFilterChanged(FilterType.THIS_WEEK) },
            label = { Text("Settimana") }
        )
        FilterChip(
            selected = currentFilter == FilterType.SQUATS_ONLY,
            onClick = { onFilterChanged(FilterType.SQUATS_ONLY) },
            label = { Text("Squat") }
        )
    }
}

/**
 * Stats summary card
 */
@Composable
fun StatsCard(
    stats: SessionStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Statistiche",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Sessioni", value = "${stats.totalSessions}")
                StatItem(label = "Reps Totali", value = "${stats.totalReps}")
                StatItem(label = "Durata", value = stats.formatDuration())
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Form Medio",
                    value = "${(stats.avgFormScore * 100).toInt()}%"
                )
                StatItem(
                    label = "Depth Medio",
                    value = "${(stats.avgDepthScore * 100).toInt()}%"
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

/**
 * Individual session card
 */
@Composable
fun SessionCard(
    session: WorkoutSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with date and exercise type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = session.exerciseType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDate(session.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Quality indicator
                QualityIndicator(formScore = session.avgFormScore)
            }
            
            // Session metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip(
                    icon = Icons.Filled.Star,
                    label = "${session.totalReps} reps"
                )
                MetricChip(
                    icon = Icons.Filled.Info,
                    label = formatDuration(session.durationSeconds)
                )
                MetricChip(
                    icon = Icons.Filled.Info,
                    label = String.format("%.1fs/rep", session.avgSpeed)
                )
            }
        }
    }
}

/**
 * Quality indicator badge
 */
@Composable
fun QualityIndicator(formScore: Float) {
    val (color, label) = when {
        formScore >= 0.8f -> Pair(Color(0xFF4CAF50), "Ottimo")
        formScore >= 0.6f -> Pair(Color(0xFFFFC107), "Buono")
        else -> Pair(Color(0xFFFF5722), "Da migliorare")
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Metric chip component
 */
@Composable
fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Empty state
 */
@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nessuna sessione trovata",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Inizia un allenamento per vedere lo storico",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Error state
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Errore",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Riprova")
        }
    }
}

/**
 * Helper functions
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${seconds}s"
    }
}
