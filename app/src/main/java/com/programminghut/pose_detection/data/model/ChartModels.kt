package com.programminghut.pose_detection.data.model

/**
 * Phase 5: Advanced Rep Visualization Models
 * 
 * Data classes per rappresentare i dati necessari alle visualizzazioni avanzate
 */

/**
 * Dati per un singolo punto nel grafico scatter
 */
data class RepChartPoint(
    val repNumber: Int,
    val timestamp: Long,
    val depthScore: Float,      // Asse Y (o X dipende dalla visualizzazione)
    val formScore: Float,        // Asse X (o Y)
    val speed: Float,            // Dimensione punto / colore alternativo
    val qualityColor: RepQuality // Colore del punto
)

/**
 * Qualità di una ripetizione (per colorazione)
 */
enum class RepQuality(val colorHex: String) {
    EXCELLENT("#4CAF50"),  // Verde
    GOOD("#8BC34A"),       // Verde chiaro
    FAIR("#FFC107"),       // Giallo/Arancione
    POOR("#FF5722"),       // Rosso
    CRITICAL("#D32F2F");   // Rosso scuro
    
    companion object {
        /**
         * Determina la qualità in base a depth e form score
         */
        fun fromScores(depthScore: Float, formScore: Float): RepQuality {
            val avgScore = (depthScore + formScore) / 2f
            return when {
                avgScore >= 0.9f -> EXCELLENT
                avgScore >= 0.75f -> GOOD
                avgScore >= 0.6f -> FAIR
                avgScore >= 0.4f -> POOR
                else -> CRITICAL
            }
        }
    }
}

/**
 * Dati per la heatline (linea colorata progressiva)
 */
data class HeatlineSegment(
    val startRepNumber: Int,
    val endRepNumber: Int,
    val quality: RepQuality,
    val avgDepthScore: Float,
    val avgFormScore: Float
)

/**
 * Dati completi per visualizzazione dettagliata di una rep
 */
data class RepDetailData(
    val rep: RepData,
    val jointAngles: Map<String, Float>? = null,  // Angoli dei giunti principali
    val warnings: List<String> = emptyList(),      // Warning sulla postura
    val comparisonWithBest: RepComparison? = null, // Confronto con miglior rep
    val previousRepComparison: RepComparison? = null // Confronto con rep precedente
)

/**
 * Confronto tra due ripetizioni
 */
data class RepComparison(
    val depthDifference: Float,     // Differenza in depth score (-1 to 1)
    val formDifference: Float,      // Differenza in form score
    val speedDifference: Float,     // Differenza in tempo
    val isImprovement: Boolean      // Se è un miglioramento rispetto al confronto
)

/**
 * Statistiche aggregate per i grafici
 */
data class ChartStatistics(
    val totalPoints: Int,
    val excellentCount: Int,
    val goodCount: Int,
    val fairCount: Int,
    val poorCount: Int,
    val criticalCount: Int,
    val avgDepth: Float,
    val avgForm: Float,
    val avgSpeed: Float,
    val trendDirection: TrendDirection
)

/**
 * Direzione del trend durante la sessione
 */
enum class TrendDirection {
    IMPROVING,    // Qualità migliora nel tempo
    STABLE,       // Qualità stabile
    DECLINING     // Qualità peggiora (fatica)
}
