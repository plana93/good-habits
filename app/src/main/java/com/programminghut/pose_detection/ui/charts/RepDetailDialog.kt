package com.programminghut.pose_detection.ui.charts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.RepDetailData
import com.programminghut.pose_detection.data.model.RepQuality
import java.text.SimpleDateFormat
import java.util.*

/**
 * Phase 5: Dialog interattivo con dettagli della ripetizione
 * 
 * Mostra informazioni complete quando l'utente tocca un punto nel grafico:
 * - Timestamp
 * - Metriche (depth, form, speed)
 * - Angoli dei giunti (se disponibili)
 * - Warning sulla postura
 * - Confronto con miglior rep
 * - Confronto con rep precedente
 */
@Composable
fun RepDetailDialog(
    rep: RepData,
    repDetailData: RepDetailData? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quality = RepQuality.fromScores(rep.depthScore, rep.formScore)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                RepDetailHeader(
                    repNumber = rep.repNumber,
                    quality = quality,
                    onClose = onDismiss
                )
                
                Divider()
                
                // Timestamp
                RepTimestamp(timestamp = rep.timestamp)
                
                // Metriche principali
                RepMetrics(rep = rep)
                
                Divider()
                
                // Angoli dei giunti (se disponibili)
                repDetailData?.jointAngles?.let { angles ->
                    JointAnglesSection(angles = angles)
                    Divider()
                }
                
                // Warning sulla postura (se presenti)
                if (repDetailData?.warnings?.isNotEmpty() == true) {
                    WarningsSection(warnings = repDetailData.warnings)
                    Divider()
                }
                
                // Confronti
                repDetailData?.let { details ->
                    ComparisonsSection(details = details)
                }
                
                // Bottone chiudi
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chiudi")
                }
            }
        }
    }
}

/**
 * Header del dialog con numero rep e qualità
 */
@Composable
private fun RepDetailHeader(
    repNumber: Int,
    quality: RepQuality,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Ripetizione #$repNumber",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Badge qualità
            Surface(
                color = androidx.compose.ui.graphics.Color(
                    android.graphics.Color.parseColor(quality.colorHex)
                ).copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = quality.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(
                        android.graphics.Color.parseColor(quality.colorHex)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Chiudi"
            )
        }
    }
}

/**
 * Timestamp della ripetizione
 */
@Composable
private fun RepTimestamp(timestamp: Long) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeString = dateFormat.format(Date(timestamp))
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Ora: $timeString",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Metriche della ripetizione
 */
@Composable
private fun RepMetrics(rep: RepData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Metriche",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Form Score
        MetricRow(
            label = "Form Score",
            value = "${(rep.formScore * 100).toInt()}%",
            icon = Icons.Filled.CheckCircle,
            progress = rep.formScore
        )
        
        // Depth Score
        MetricRow(
            label = "Depth Score",
            value = "${(rep.depthScore * 100).toInt()}%",
            icon = Icons.Filled.Star,
            progress = rep.depthScore
        )
        
        // Speed
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Tempo: ${String.format("%.1f", rep.speed)}s",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Singola riga di metrica con barra di progresso
 */
@Composable
private fun MetricRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = when {
                progress >= 0.8f -> MaterialTheme.colorScheme.primary
                progress >= 0.6f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

/**
 * Sezione con angoli dei giunti
 */
@Composable
private fun JointAnglesSection(angles: Map<String, Float>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Angoli Articolazioni",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        angles.forEach { (joint, angle) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = joint,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${angle.toInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Sezione con warning sulla postura
 */
@Composable
private fun WarningsSection(warnings: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Avvisi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
        
        warnings.forEach { warning ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "• $warning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * Sezione con confronti
 */
@Composable
private fun ComparisonsSection(details: RepDetailData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Confronti",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Confronto con miglior rep
        details.comparisonWithBest?.let { comparison ->
            ComparisonCard(
                title = "vs Miglior Rep",
                comparison = comparison,
                icon = Icons.Filled.Star
            )
        }
        
        // Confronto con rep precedente
        details.previousRepComparison?.let { comparison ->
            ComparisonCard(
                title = "vs Rep Precedente",
                comparison = comparison,
                icon = Icons.Filled.KeyboardArrowDown
            )
        }
    }
}

/**
 * Card di confronto
 */
@Composable
private fun ComparisonCard(
    title: String,
    comparison: com.programminghut.pose_detection.data.model.RepComparison,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = if (comparison.isImprovement) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = buildString {
                    append("Form: ${formatDifference(comparison.formDifference)}")
                    append(" • ")
                    append("Depth: ${formatDifference(comparison.depthDifference)}")
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Formatta una differenza con segno + o -
 */
private fun formatDifference(difference: Float): String {
    val percentage = (difference * 100).toInt()
    return if (percentage >= 0) "+$percentage%" else "$percentage%"
}
