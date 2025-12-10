package com.programminghut.pose_detection.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.programminghut.pose_detection.data.model.RepChartPoint
import com.programminghut.pose_detection.data.model.RepData
import com.programminghut.pose_detection.data.model.RepQuality

/**
 * Phase 5: Scatter Chart per visualizzare tutte le ripetizioni
 * 
 * Mostra un grafico scatter con:
 * - Asse X: Numero ripetizione (tempo)
 * - Asse Y: Depth Score / Form Score
 * - Colore punto: Qualità della rep
 * - Tocco punto: Apre dialog con dettagli
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepScatterChart(
    reps: List<RepData>,
    onRepClick: (RepData) -> Unit,
    modifier: Modifier = Modifier
) {
    // Converti reps in chart points
    val chartPoints = remember(reps) {
        reps.map { rep ->
            RepChartPoint(
                repNumber = rep.repNumber,
                timestamp = rep.timestamp,
                depthScore = rep.depthScore,
                formScore = rep.formScore,
                speed = rep.speed,
                qualityColor = RepQuality.fromScores(rep.depthScore, rep.formScore)
            )
        }
    }
    
    // State per selezionare cosa mostrare sull'asse Y
    var selectedMetric by remember { mutableStateOf(ChartMetric.FORM) }
    
    Column(modifier = modifier) {
        // Header con selector metrica
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Qualità Ripetizioni",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Metric selector buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartMetric.values().forEach { metric ->
                    FilterChip(
                        selected = selectedMetric == metric,
                        onClick = { selectedMetric = metric },
                        label = { Text(metric.label) }
                    )
                }
            }
        }
        
        // Il grafico scatter
        if (chartPoints.isNotEmpty()) {
            RepScatterChartContent(
                chartPoints = chartPoints,
                selectedMetric = selectedMetric,
                onPointClick = { point ->
                    // Trova la rep corrispondente e chiama callback
                    reps.find { it.repNumber == point.repNumber }?.let(onRepClick)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        } else {
            // Placeholder quando non ci sono dati
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "Nessun dato disponibile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Legenda
        ChartLegend(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

/**
 * Contenuto del grafico scatter usando Vico
 */
@Composable
private fun RepScatterChartContent(
    chartPoints: List<RepChartPoint>,
    selectedMetric: ChartMetric,
    onPointClick: (RepChartPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    // Prepara i dati per Vico
    val chartEntryModel = remember(chartPoints, selectedMetric) {
        // Raggruppa i punti per qualità (per colorarli diversamente)
        val entriesByQuality = chartPoints.groupBy { it.qualityColor }
        
        val entries = entriesByQuality.map { (quality, points) ->
            points.map { point ->
                FloatEntry(
                    x = point.repNumber.toFloat(),
                    y = when (selectedMetric) {
                        ChartMetric.FORM -> point.formScore
                        ChartMetric.DEPTH -> point.depthScore
                        ChartMetric.COMBINED -> (point.formScore + point.depthScore) / 2f
                    }
                )
            }
        }
        
        ChartEntryModelProducer(entries).getModel()
    }
    
    // Se non ci sono dati, mostra placeholder
    if (chartEntryModel == null) {
        Box(
            modifier = modifier,
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "Dati non disponibili per il grafico",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    // Grafico Vico
    Card(
        modifier = modifier.padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Chart(
            chart = lineChart(
                // Configurazioni per rendere il grafico come scatter
                // (linee molto sottili o punti grandi)
            ),
            model = chartEntryModel,
            startAxis = rememberStartAxis(
                title = selectedMetric.label
            ),
            bottomAxis = rememberBottomAxis(
                title = "Ripetizione #"
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            chartScrollState = rememberChartScrollState()
        )
    }
}

/**
 * Legenda per i colori della qualità
 */
@Composable
private fun ChartLegend(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Legenda Qualità",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RepQuality.values().forEach { quality ->
                    LegendItem(
                        quality = quality,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Singolo item della legenda
 */
@Composable
private fun LegendItem(
    quality: RepQuality,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = Color(android.graphics.Color.parseColor(quality.colorHex)),
                    shape = CircleShape
                )
        )
        Text(
            text = quality.name.lowercase().capitalize(),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Metriche disponibili per l'asse Y
 */
enum class ChartMetric(val label: String) {
    FORM("Form"),
    DEPTH("Depth"),
    COMBINED("Media")
}
