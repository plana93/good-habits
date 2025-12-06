package com.programminghut.pose_detection.utils

import com.programminghut.pose_detection.data.model.WorkoutSession
import java.text.SimpleDateFormat
import java.util.*

/**
 * ShareHelper - Utility for generating shareable workout summaries
 * 
 * Phase 3: Export & Share implementation
 * Provides multiple text templates for social sharing and data export
 */
object ShareHelper {
    
    /**
     * Template types for different sharing contexts
     */
    enum class TemplateType {
        BRIEF,      // Quick summary for quick shares
        DETAILED,   // Full statistics for fitness communities
        SOCIAL      // Optimized for Instagram/Twitter
    }
    
    /**
     * Generate shareable summary for a single workout session
     */
    fun generateSessionSummary(
        session: WorkoutSession,
        template: TemplateType = TemplateType.BRIEF,
        includePrivateData: Boolean = false
    ): String {
        return when (template) {
            TemplateType.BRIEF -> generateBriefSessionSummary(session)
            TemplateType.DETAILED -> generateDetailedSessionSummary(session, includePrivateData)
            TemplateType.SOCIAL -> generateSocialSessionSummary(session)
        }
    }
    
    /**
     * Generate shareable summary for dashboard statistics
     */
    fun generateDashboardSummary(
        totalSessions: Int,
        totalReps: Int,
        avgFormScore: Float,
        currentStreak: Int,
        bestSession: WorkoutSession?,
        dateRange: String,
        template: TemplateType = TemplateType.DETAILED
    ): String {
        return when (template) {
            TemplateType.BRIEF -> generateBriefDashboardSummary(
                totalSessions, totalReps, currentStreak
            )
            TemplateType.DETAILED -> generateDetailedDashboardSummary(
                totalSessions, totalReps, avgFormScore, currentStreak, bestSession, dateRange
            )
            TemplateType.SOCIAL -> generateSocialDashboardSummary(
                totalSessions, totalReps, currentStreak, dateRange
            )
        }
    }
    
    /**
     * Generate CSV export for multiple sessions
     */
    fun generateCSVExport(sessions: List<WorkoutSession>): String {
        val header = "Session ID,Start Time,End Time,Duration (min),Total Reps,Avg Depth Score,Avg Form Score,Exercise Type,Notes\n"
        val rows = sessions.joinToString("\n") { session ->
            val duration = (session.endTime - session.startTime) / 60000 // minutes
            val startDate = formatDateTime(session.startTime)
            val endDate = formatDateTime(session.endTime)
            val notes = (session.notes ?: "").replace(",", ";").replace("\n", " ")
            
            "${session.sessionId},$startDate,$endDate,$duration,${session.totalReps}," +
            "${session.avgDepthScore},${session.avgFormScore},${session.exerciseType},\"$notes\""
        }
        return header + rows
    }
    
    /**
     * Generate JSON export for multiple sessions
     */
    fun generateJSONExport(sessions: List<WorkoutSession>): String {
        val jsonSessions = sessions.joinToString(",\n    ") { session ->
            val notesEscaped = (session.notes ?: "").replace("\"", "\\\"").replace("\n", "\\n")
            """
            {
                "sessionId": ${session.sessionId},
                "startTime": ${session.startTime},
                "endTime": ${session.endTime},
                "durationMinutes": ${(session.endTime - session.startTime) / 60000},
                "totalReps": ${session.totalReps},
                "avgDepthScore": ${session.avgDepthScore},
                "avgFormScore": ${session.avgFormScore},
                "avgSpeed": ${session.avgSpeed},
                "exerciseType": "${session.exerciseType}",
                "notes": "$notesEscaped"
            }
            """.trimIndent()
        }
        
        return """
        {
            "exportDate": "${formatDateTime(System.currentTimeMillis())}",
            "sessionsCount": ${sessions.size},
            "sessions": [
                $jsonSessions
            ]
        }
        """.trimIndent()
    }
    
    // ============================================================
    // PRIVATE TEMPLATE GENERATORS
    // ============================================================
    
    private fun generateBriefSessionSummary(session: WorkoutSession): String {
        val date = formatDate(session.startTime)
        val duration = formatDuration(session.endTime - session.startTime)
        val quality = formatQualityLabel(session.avgFormScore)
        
        return """
        💪 Good Habits - Allenamento $date
        
        ✅ ${session.totalReps} ripetizioni
        ⏱️ Durata: $duration
        ⭐ Qualità: $quality
        
        #GoodHabits #Fitness #${session.exerciseType}
        """.trimIndent()
    }
    
    private fun generateDetailedSessionSummary(
        session: WorkoutSession,
        includePrivateData: Boolean
    ): String {
        val date = formatDate(session.startTime)
        val time = formatTime(session.startTime)
        val duration = formatDuration(session.endTime - session.startTime)
        val quality = formatQualityLabel(session.avgFormScore)
        val qualityScore = (session.avgFormScore * 100).toInt()
        val depthScore = (session.avgDepthScore * 100).toInt()
        val speed = String.format("%.1f", session.avgSpeed)
        
        val notesSection = if (includePrivateData && !session.notes.isNullOrBlank()) {
            "\n\n📝 Note:\n${session.notes}"
        } else ""
        
        return """
        📊 Good Habits - Riepilogo Allenamento
        
        📅 Data: $date alle $time
        🏋️ Esercizio: ${session.exerciseType}
        
        📈 Statistiche:
        • Ripetizioni: ${session.totalReps}
        • Durata: $duration
        • Qualità Form: $qualityScore% ($quality)
        • Profondità Media: $depthScore%
        • Velocità Media: ${speed}s/rep
        
        💡 Insight: ${generateInsight(session)}$notesSection
        
        Continua così! 🔥
        #GoodHabits #Fitness #Training #${session.exerciseType}
        """.trimIndent()
    }
    
    private fun generateSocialSessionSummary(session: WorkoutSession): String {
        val reps = session.totalReps
        val quality = when {
            session.avgFormScore >= 0.8f -> "eccellente 🔥"
            session.avgFormScore >= 0.6f -> "ottima 💪"
            else -> "buona ✅"
        }
        
        return """
        Oggi: $reps ripetizioni con form $quality!
        
        💪 Obiettivo raggiunto
        📈 Progressi costanti
        🔥 #NeverGiveUp
        
        #GoodHabits #Fitness #WorkoutMotivation #FitnessJourney #${session.exerciseType}
        """.trimIndent()
    }
    
    private fun generateBriefDashboardSummary(
        totalSessions: Int,
        totalReps: Int,
        currentStreak: Int
    ): String {
        return """
        📊 Good Habits - I miei progressi
        
        🏋️ $totalSessions allenamenti
        💪 $totalReps ripetizioni totali
        🔥 $currentStreak giorni di streak
        
        #GoodHabits #FitnessGoals
        """.trimIndent()
    }
    
    private fun generateDetailedDashboardSummary(
        totalSessions: Int,
        totalReps: Int,
        avgFormScore: Float,
        currentStreak: Int,
        bestSession: WorkoutSession?,
        dateRange: String
    ): String {
        val avgQuality = (avgFormScore * 100).toInt()
        val avgPerSession = if (totalSessions > 0) totalReps / totalSessions else 0
        
        val bestSessionInfo = bestSession?.let {
            val bestDate = formatDate(it.startTime)
            "\n🏆 Miglior sessione: ${it.totalReps} reps ($bestDate)"
        } ?: ""
        
        val totalMinutes = totalSessions * 20 // stima 20 min per sessione
        val totalHours = totalMinutes / 60
        
        return """
        📊 Good Habits — Riepilogo $dateRange
        
        📈 Statistiche Generali:
        • Totale sessioni: $totalSessions
        • Totale ripetizioni: $totalReps
        • Media reps/sessione: $avgPerSession
        • Qualità media: $avgQuality%
        • Tempo totale: ~${totalHours}h ${totalMinutes % 60}min
        
        🔥 Streak attuale: $currentStreak giorni$bestSessionInfo
        
        💡 Insight: ${generateDashboardInsight(totalSessions, currentStreak, avgFormScore)}
        
        Continua il grande lavoro! 💪
        #GoodHabits #FitnessJourney #ProgressNotPerfection
        """.trimIndent()
    }
    
    private fun generateSocialDashboardSummary(
        totalSessions: Int,
        totalReps: Int,
        currentStreak: Int,
        dateRange: String
    ): String {
        val emoji = when {
            currentStreak >= 30 -> "🔥🔥🔥"
            currentStreak >= 14 -> "🔥🔥"
            currentStreak >= 7 -> "🔥"
            else -> "💪"
        }
        
        return """
        $dateRange: Il mio viaggio fitness $emoji
        
        ✨ $totalSessions allenamenti completati
        💪 $totalReps ripetizioni fatte
        🔥 $currentStreak giorni consecutivi
        
        La costanza paga! Chi viene con me? 🚀
        
        #GoodHabits #FitnessMotivation #WorkoutStreak #NeverGiveUp #FitnessGoals
        """.trimIndent()
    }
    
    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }
    
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }
    
    private fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }
    
    private fun formatDuration(millis: Long): String {
        val minutes = millis / 60000
        val seconds = (millis % 60000) / 1000
        return if (minutes > 0) {
            "${minutes}min ${seconds}s"
        } else {
            "${seconds}s"
        }
    }
    
    private fun formatQualityLabel(score: Float): String {
        return when {
            score >= 0.8f -> "Eccellente"
            score >= 0.6f -> "Buona"
            score >= 0.4f -> "Discreta"
            else -> "Da migliorare"
        }
    }
    
    private fun generateInsight(session: WorkoutSession): String {
        return when {
            session.avgFormScore >= 0.8f && session.avgDepthScore >= 0.8f ->
                "Esecuzione impeccabile! Mantieni questa tecnica."
            session.avgFormScore >= 0.6f && session.totalReps >= 100 ->
                "Ottimo volume di lavoro con buona qualità."
            session.avgDepthScore < 0.6f ->
                "Prova ad andare più in profondità nelle ripetizioni."
            session.avgFormScore < 0.6f ->
                "Concentrati sulla tecnica per risultati migliori."
            else ->
                "Buon allenamento! Continua a migliorare."
        }
    }
    
    private fun generateDashboardInsight(
        totalSessions: Int,
        currentStreak: Int,
        avgFormScore: Float
    ): String {
        return when {
            currentStreak >= 30 ->
                "Incredibile! 30+ giorni di costanza. Sei una leggenda! 🏆"
            currentStreak >= 14 ->
                "Due settimane di streak! La costanza è la chiave del successo."
            currentStreak >= 7 ->
                "Una settimana di fila! Stai costruendo un'ottima abitudine."
            totalSessions >= 50 && avgFormScore >= 0.7f ->
                "Oltre 50 sessioni con qualità elevata. Impressionante!"
            totalSessions >= 20 ->
                "20+ allenamenti completati. Continua così!"
            avgFormScore >= 0.8f ->
                "Qualità eccellente. La tecnica fa la differenza!"
            else ->
                "Ottimo inizio! Ogni sessione ti avvicina ai tuoi obiettivi."
        }
    }
}
