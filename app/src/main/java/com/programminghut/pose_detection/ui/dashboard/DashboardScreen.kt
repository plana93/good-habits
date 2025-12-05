package com.programminghut.pose_detection.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard Screen - Composable
 * 
 * Main dashboard with KPIs, charts, and statistics.
 * Phase 2: Dashboard Core implementation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is DashboardUiState.Success -> {
                DashboardContent(
                    kpiData = state.kpiData,
                    chartData = state.chartData,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is DashboardUiState.Error -> {
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
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    kpiData: KpiData,
    chartData: ChartData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI Cards Section
        item {
            Text(
                text = "Panoramica",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            KpiCardsGrid(kpiData = kpiData)
        }
        
        // Reps Trend Chart
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Andamento Ripetizioni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            RepsTrendCard(chartData = chartData)
        }
        
        // Form Quality Distribution
        item {
            Text(
                text = "Distribuzione Qualità",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            FormDistributionCard(chartData = chartData)
        }
        
        // Best Session Card
        kpiData.bestSession?.let { bestSession ->
            item {
                Text(
                    text = "Miglior Sessione",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                BestSessionCard(session = bestSession)
            }
        }
    }
}

/**
 * KPI Cards Grid - 2x3 grid of key metrics
 */
@Composable
fun KpiCardsGrid(kpiData: KpiData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Sessioni",
                value = "${kpiData.totalSessions}",
                icon = Icons.Filled.Star,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Ripetizioni",
                value = "${kpiData.totalReps}",
                icon = Icons.Filled.Star,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Qualità Media",
                value = "${(kpiData.avgFormScore * 100).toInt()}%",
                icon = Icons.Filled.Star,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Serie",
                value = "${kpiData.currentStreak} 🔥",
                icon = Icons.Filled.Star,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Tempo Totale",
                value = kpiData.formatTotalTime(),
                icon = Icons.Filled.Info,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Media/Sessione",
                value = if (kpiData.totalSessions > 0) {
                    "${kpiData.totalReps / kpiData.totalSessions}"
                } else "0",
                icon = Icons.Filled.Star,
                color = Color(0xFF00BCD4),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual KPI Card
 */
@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Reps Trend Chart Card (simplified visualization)
 */
@Composable
fun RepsTrendCard(chartData: ChartData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ultimi 30 giorni",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (chartData.repsByDate.isEmpty()) {
                Text(
                    text = "Nessun dato disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Simple bar visualization
                val maxReps = chartData.repsByDate.maxOfOrNull { it.second } ?: 1
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    chartData.repsByDate.takeLast(7).forEach { (timestamp, reps) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatDateShort(timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(60.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE0E0E0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = (reps.toFloat() / maxReps))
                                        .background(Color(0xFF2196F3))
                                )
                            }
                            Text(
                                text = "$reps",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Form Distribution Chart Card
 */
@Composable
fun FormDistributionCard(chartData: ChartData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Distribuzione Qualità Form",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (chartData.formDistribution.isEmpty()) {
                Text(
                    text = "Nessun dato disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val total = chartData.formDistribution.values.sum()
                val categories = listOf("Excellent", "Good", "Fair", "Poor")
                val colors = mapOf(
                    "Excellent" to Color(0xFF4CAF50),
                    "Good" to Color(0xFF8BC34A),
                    "Fair" to Color(0xFFFFC107),
                    "Poor" to Color(0xFFFF5722)
                )
                
                categories.forEach { category ->
                    val count = chartData.formDistribution[category] ?: 0
                    val percentage = if (total > 0) (count * 100f / total) else 0f
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors[category] ?: Color.Gray)
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(80.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE0E0E0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = percentage / 100f)
                                    .background(colors[category] ?: Color.Gray)
                            )
                        }
                        Text(
                            text = "$count (${percentage.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Best Session Card
 */
@Composable
fun BestSessionCard(session: com.programminghut.pose_detection.data.model.WorkoutSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(48.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${session.totalReps} ripetizioni",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Qualità: ${(session.avgFormScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatDate(session.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Helper functions
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}

private fun formatDateShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}
