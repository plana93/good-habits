package com.programminghut.pose_detection.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.programminghut.pose_detection.data.model.HeatlineSegment
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.RepQuality
import kotlin.math.max
import kotlin.math.min

/**
 * Phase 5: Heatline Chart - Linea colorata che mostra qualità progressiva
 * 
 * Una linea orizzontale che cambia colore in base alla qualità delle ripetizioni
 * nel tempo. Verde = ottimo, Giallo = ok, Rosso = scarso.
 * 
 * Permette di vedere a colpo d'occhio:
 * - Quando la qualità cala (fatica)
 * - Quali segmenti della sessione sono stati migliori
 * - Pattern di performance
 */
@Composable
fun RepHeatlineChart(
    reps: List<RepData>,
    onSegmentClick: ((HeatlineSegment) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Calcola i segmenti della heatline
    val segments = remember(reps) {
        calculateHeatlineSegments(reps, segmentSize = 5)
    }
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "Qualità nel Tempo",
            style = MaterialTheme.typography.titleMedium
        )
        
        Text(
            text = "Ogni segmento rappresenta ${if (segments.isNotEmpty()) segments[0].endRepNumber - segments[0].startRepNumber + 1 else 5} ripetizioni",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // La heatline
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (segments.isNotEmpty()) {
                    HeatlineCanvas(
                        segments = segments,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "Dati insufficienti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Statistiche segmenti
        if (segments.isNotEmpty()) {
            SegmentStatistics(segments = segments)
        }
    }
}

/**
 * Canvas per disegnare la heatline
 */
@Composable
private fun HeatlineCanvas(
    segments: List<HeatlineSegment>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val segmentWidth = canvasWidth / segments.size.toFloat()
        
        segments.forEachIndexed { index, segment ->
            val startX = index * segmentWidth
            val endX = (index + 1) * segmentWidth
            
            // Colore basato sulla qualità
            val color = Color(android.graphics.Color.parseColor(segment.quality.colorHex))
            
            // Disegna il segmento come rettangolo colorato
            drawRect(
                color = color,
                topLeft = Offset(startX, 0f),
                size = androidx.compose.ui.geometry.Size(segmentWidth, canvasHeight)
            )
            
            // Opzionale: Disegna un bordo tra i segmenti
            if (index < segments.size - 1) {
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(endX, 0f),
                    end = Offset(endX, canvasHeight),
                    strokeWidth = 2f
                )
            }
        }
    }
}

/**
 * Statistiche dei segmenti
 */
@Composable
private fun SegmentStatistics(segments: List<HeatlineSegment>) {
    val bestSegment = segments.maxByOrNull { (it.avgDepthScore + it.avgFormScore) / 2f }
    val worstSegment = segments.minByOrNull { (it.avgDepthScore + it.avgFormScore) / 2f }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Miglior segmento
        bestSegment?.let { segment ->
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🏆 Miglior Fase",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Rep ${segment.startRepNumber}-${segment.endRepNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        // Peggior segmento
        worstSegment?.let { segment ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "⚠️ Fase Critica",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Rep ${segment.startRepNumber}-${segment.endRepNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Calcola i segmenti della heatline raggruppando le ripetizioni
 */
private fun calculateHeatlineSegments(
    reps: List<RepData>,
    segmentSize: Int = 5
): List<HeatlineSegment> {
    if (reps.isEmpty()) return emptyList()
    
    val segments = mutableListOf<HeatlineSegment>()
    
    // Raggruppa le reps in segmenti di dimensione fissa
    val sortedReps = reps.sortedBy { it.repNumber }
    var currentStart = 0
    
    while (currentStart < sortedReps.size) {
        val currentEnd = min(currentStart + segmentSize - 1, sortedReps.size - 1)
        val segmentReps = sortedReps.subList(currentStart, currentEnd + 1)
        
        // Calcola metriche medie per il segmento
        val avgDepth = segmentReps.map { it.depthScore }.average().toFloat()
        val avgForm = segmentReps.map { it.formScore }.average().toFloat()
        
        // Determina la qualità del segmento
        val quality = RepQuality.fromScores(avgDepth, avgForm)
        
        segments.add(
            HeatlineSegment(
                startRepNumber = segmentReps.first().repNumber,
                endRepNumber = segmentReps.last().repNumber,
                quality = quality,
                avgDepthScore = avgDepth,
                avgFormScore = avgForm
            )
        )
        
        currentStart = currentEnd + 1
    }
    
    return segments
}
