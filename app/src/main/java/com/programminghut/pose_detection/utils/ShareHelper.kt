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
    
    /**
     * Generate comprehensive daily diary TXT export
     * 
     * Creates a day-by-day transcription with:
     * - Exercises with quantity and time
     * - Wellness tracker responses with scores
     * - Legend at the beginning
     * - Manual offset section for user context
     * 
     * @param allDailyItems All daily session items grouped by date
     * @return Formatted text diary
     */
    fun generateDailyDiaryTXT(allDailyItems: Map<String, List<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>>): String {
        val dateFormat = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.ITALIAN)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALIAN)
        val exportDate = SimpleDateFormat("dd MMMM yyyy 'alle' HH:mm", Locale.ITALIAN).format(Date())
        
        return buildString {
            // Header
            appendLine("=".repeat(70))
            appendLine("GOOD HABITS - DIARIO ALLENAMENTI")
            appendLine("Trascrizione Completa Database")
            appendLine("=".repeat(70))
            appendLine()
            appendLine("Data Export: $exportDate")
            appendLine()
            appendLine()
            
            // Sort dates chronologically (most recent first)
            val sortedDates = allDailyItems.keys.sortedByDescending { 
                SimpleDateFormat("yyyy-MM-dd", Locale.ITALIAN).parse(it)?.time ?: 0
            }
            
            var totalExercises = 0
            var totalWellness = 0
            var totalDays = sortedDates.size
            
            sortedDates.forEach { dateKey ->
                val items = allDailyItems[dateKey] ?: emptyList()
                if (items.isEmpty()) return@forEach
                
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.ITALIAN).parse(dateKey)
                val formattedDate = date?.let { dateFormat.format(it) } ?: dateKey
                
                // Day header
                appendLine("-".repeat(70))
                appendLine("DATA: $formattedDate")
                appendLine("-".repeat(70))
                appendLine()
                
                // Separate exercises and wellness trackers
                val exercises = items.filter { it.type == "EXERCISE" }
                val workouts = items.filter { it.type == "WORKOUT" }
                val wellnessTrackers = items.filter { it.type == "WELLNESS_TRACKER" }
                
                // EXERCISES SECTION
                if (exercises.isNotEmpty()) {
                    appendLine("ESERCIZI:")
                    appendLine()
                    
                    exercises.forEachIndexed { index, item ->
                        totalExercises++
                        val status = if (item.isCompleted) "COMPLETATO" else "NON COMPLETATO"
                        val exerciseName = item.exerciseName ?: item.name
                        
                        appendLine("  ${index + 1}. $exerciseName [$status]")
                        
                        // Orario di completamento (sempre se disponibile)
                        item.completedAt?.let { 
                            val completionTime = timeFormat.format(Date(it))
                            appendLine("     Orario: $completionTime")
                        }
                        
                        // Repetitions - mostra quello che hai fatto
                        if (item.actualReps != null && item.actualReps > 0) {
                            val target = item.targetReps
                            if (target != null && target > 0) {
                                appendLine("     Ripetizioni: ${item.actualReps} / $target")
                            } else {
                                appendLine("     Ripetizioni: ${item.actualReps}")
                            }
                        }
                        
                        // Time - mostra quello che hai fatto
                        if (item.actualTime != null && item.actualTime > 0) {
                            val actualMin = item.actualTime / 60
                            val actualSec = item.actualTime % 60
                            val target = item.targetTime
                            
                            if (target != null && target > 0) {
                                val targetMin = target / 60
                                val targetSec = target % 60
                                appendLine("     Tempo: ${actualMin}m ${actualSec}s / ${targetMin}m ${targetSec}s")
                            } else {
                                appendLine("     Tempo: ${actualMin}m ${actualSec}s")
                            }
                        }
                        
                        // Notes
                        if (item.notes.isNotBlank() && item.notes != "Nessuna nota") {
                            appendLine("     Note: ${item.notes}")
                        }
                        
                        appendLine()
                    }
                }
                
                // WORKOUTS SECTION
                if (workouts.isNotEmpty()) {
                    appendLine("CIRCUITI/WORKOUT:")
                    appendLine()
                    
                    workouts.forEachIndexed { index, item ->
                        val status = if (item.isCompleted) "[COMPLETATO]" else "[NON COMPLETATO]"
                        val workoutName = item.workoutName ?: item.name
                        val completionTime = item.completedAt?.let { timeFormat.format(Date(it)) } ?: "N/A"
                        
                        appendLine("  ${index + 1}. $workoutName $status")
                        
                        if (item.isCompleted) {
                            appendLine("     Orario: $completionTime")
                        }
                        
                        if (!item.notes.isNullOrBlank() && item.notes != "Nessuna nota") {
                            appendLine("     Note: ${item.notes}")
                        }
                        
                        appendLine()
                    }
                }
                
                // WELLNESS TRACKER SECTION
                if (wellnessTrackers.isNotEmpty()) {
                    appendLine("BENESSERE & WELLNESS TRACKER:")
                    appendLine()
                    
                    wellnessTrackers.forEachIndexed { index, item ->
                        totalWellness++
                        
                        // Get tracker name (question) and response details from JSON
                        var trackerQuestion = "Wellness Tracker"
                        var trackerResponse = ""
                        var trackerTimestamp: Long? = null
                        
                        item.trackerResponseJson?.let { json ->
                            val response = com.programminghut.pose_detection.data.model.TrackerResponse.fromJson(json)
                            response?.let { resp ->
                                // Use trackerName as the question
                                trackerQuestion = resp.trackerName.takeIf { it.isNotBlank() } ?: trackerQuestion
                                trackerTimestamp = resp.timestamp
                                
                                // Build response text based on type
                                trackerResponse = when (resp.responseType) {
                                    com.programminghut.pose_detection.data.model.TrackerResponseType.RATING_5 -> {
                                        "${resp.ratingValue}/5"
                                    }
                                    com.programminghut.pose_detection.data.model.TrackerResponseType.BOOLEAN -> {
                                        if (resp.booleanValue == true) "SI" else "NO"
                                    }
                                    com.programminghut.pose_detection.data.model.TrackerResponseType.EMOTION_SET -> {
                                        resp.selectedEmotion ?: "Non specificata"
                                    }
                                    com.programminghut.pose_detection.data.model.TrackerResponseType.TEXT_NOTE -> {
                                        resp.textNote ?: ""
                                    }
                                }
                            }
                        }
                        
                        appendLine("  ${index + 1}. $trackerQuestion")
                        
                        // Orario - usa timestamp dal JSON o completedAt
                        val timestamp = trackerTimestamp ?: item.completedAt
                        timestamp?.let { 
                            val time = timeFormat.format(Date(it))
                            appendLine("     Orario: $time")
                        }
                        
                        // Risposta
                        if (trackerResponse.isNotBlank()) {
                            appendLine("     Risposta: $trackerResponse")
                        }
                        
                        // Parse tracker response JSON for additional text notes
                        item.trackerResponseJson?.let { json ->
                            val response = com.programminghut.pose_detection.data.model.TrackerResponse.fromJson(json)
                            response?.let { resp ->
                                // Add text note if available and not already shown as main response
                                if (!resp.textNote.isNullOrBlank() && resp.responseType != com.programminghut.pose_detection.data.model.TrackerResponseType.TEXT_NOTE) {
                                    appendLine("     Note: ${resp.textNote}")
                                }
                            }
                        }
                        
                        appendLine()
                    }
                }
                
                appendLine()
            }
            
            // Footer with statistics
            appendLine()
            appendLine("=".repeat(70))
            appendLine("RIEPILOGO TOTALE")
            appendLine("=".repeat(70))
            appendLine()
            appendLine("Giorni con dati: $totalDays")
            appendLine("Totale esercizi registrati: $totalExercises")
            appendLine("Totale wellness tracker: $totalWellness")
            appendLine("Totale attivita': ${totalExercises + totalWellness}")
            appendLine()
            appendLine("Export generato da Good Habits")
            appendLine("=".repeat(70))
        }
    }
    
    /**
     * Generate TXT export for multiple sessions (human-readable format)
     * @deprecated Use generateDailyDiaryTXT for comprehensive daily diary
     */
    @Deprecated("Use generateDailyDiaryTXT for better daily transcription")
    fun generateTXTExport(sessions: List<WorkoutSession>): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy 'alle' HH:mm", Locale.ITALIAN)
        val exportDate = dateFormat.format(Date())
        
        val totalReps = sessions.sumOf { it.totalReps }
        val avgFormScore = if (sessions.isNotEmpty()) {
            (sessions.map { it.avgFormScore }.average() * 100).toInt()
        } else 0
        
        val totalDurationMinutes = sessions.sumOf { 
            (it.endTime - it.startTime) / 60000 
        }
        val hours = totalDurationMinutes / 60
        val minutes = totalDurationMinutes % 60
        
        return buildString {
            appendLine("═══════════════════════════════════════════════════")
            appendLine("         GOOD HABITS - STORICO ALLENAMENTI         ")
            appendLine("═══════════════════════════════════════════════════")
            appendLine()
            appendLine("📅 Export generato il: $exportDate")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("RIEPILOGO GENERALE")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("🏋️  Totale Sessioni: ${sessions.size}")
            appendLine("💪 Totale Ripetizioni: $totalReps")
            appendLine("⭐ Qualità Media: $avgFormScore%")
            appendLine("⏱️  Tempo Totale: ${hours}h ${minutes}min")
            appendLine()
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("DETTAGLIO SESSIONI")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            
            sessions.sortedByDescending { it.startTime }.forEachIndexed { index, session ->
                val sessionDate = dateFormat.format(Date(session.startTime))
                val duration = (session.endTime - session.startTime) / 60000
                val formScore = (session.avgFormScore * 100).toInt()
                val depthScore = (session.avgDepthScore * 100).toInt()
                
                appendLine("┌─ SESSIONE #${index + 1} ─────────────────────────────────")
                appendLine("│")
                appendLine("│ 📅 Data: $sessionDate")
                appendLine("│ 🏋️  Esercizio: ${session.exerciseType}")
                appendLine("│ ⏱️  Durata: ${duration} minuti")
                appendLine("│")
                appendLine("│ 📊 Statistiche:")
                appendLine("│   • Ripetizioni: ${session.totalReps}")
                appendLine("│   • Qualità Form: $formScore%")
                appendLine("│   • Profondità Media: $depthScore%")
                appendLine("│   • Velocità Media: ${String.format("%.1f", session.avgSpeed)}s/rep")
                appendLine("│")
                
                if (!session.notes.isNullOrBlank()) {
                    appendLine("│ 📝 Note:")
                    session.notes.lines().forEach { line ->
                        appendLine("│   $line")
                    }
                    appendLine("│")
                }
                
                appendLine("└───────────────────────────────────────────────────")
                appendLine()
            }
            
            appendLine()
            appendLine("═══════════════════════════════════════════════════")
            appendLine("    Continua a migliorare ogni giorno! 💪🔥")
            appendLine("═══════════════════════════════════════════════════")
        }
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
    
    /**
     * Generate CSV export for Wellness Tracker data
     * 
     * Exports wellness/mood tracking data with timestamps for external analysis.
     * Each row represents a single tracker entry with its response and metadata.
     * 
     * IMPORTANT: Includes both entry date (when added) and reference date (what day it refers to)
     * This allows tracking emotions/events retroactively while maintaining data integrity.
     * 
     * @param trackerEntries List of DailySessionItemWithDetails filtered for WELLNESS_TRACKER type
     * @return CSV formatted string with wellness tracker data
     */
    fun generateWellnessTrackerCSV(
        trackerEntries: List<com.programminghut.pose_detection.data.model.DailySessionItemWithDetails>
    ): String {
        val header = "Entry Date,Entry Time,Reference Date,Days Ago,Tracker ID,Tracker Name,Response Type,Value,Rating (0-5),Boolean,Emotion,Notes\n"
        
        val rows = trackerEntries.mapNotNull { entry ->
            // Parse the TrackerResponse JSON
            val response = entry.trackerResponseJson?.let { json ->
                com.programminghut.pose_detection.data.model.TrackerResponse.fromJson(json)
            } ?: return@mapNotNull null
            
            // Entry timestamp (when user added this)
            val entryTimestamp = entry.completedAt ?: System.currentTimeMillis()
            val entryDate = formatDate(entryTimestamp)
            val entryTime = formatTime(entryTimestamp)
            
            // Reference date (what day this refers to)
            val referenceDate = formatDate(response.referenceDate)
            val daysAgo = response.getDaysAgo()
            
            val trackerId = entry.trackerTemplateId ?: 0
            val trackerName = entry.name
            val responseType = response.responseType.name
            
            // Extract the actual value based on response type
            val ratingValue = response.ratingValue?.toString() ?: ""
            val booleanValue = response.booleanValue?.toString() ?: ""
            val emotionValue = response.selectedEmotion ?: ""
            val notesValue = (response.textNote ?: "").replace(",", ";").replace("\n", " ")
            
            // Determine main value for "Value" column
            val mainValue = when (response.responseType) {
                com.programminghut.pose_detection.data.model.TrackerResponseType.RATING_5 -> 
                    response.ratingValue?.toString() ?: ""
                com.programminghut.pose_detection.data.model.TrackerResponseType.BOOLEAN -> 
                    response.booleanValue?.toString() ?: ""
                com.programminghut.pose_detection.data.model.TrackerResponseType.EMOTION_SET -> 
                    response.selectedEmotion ?: ""
                com.programminghut.pose_detection.data.model.TrackerResponseType.TEXT_NOTE -> 
                    notesValue.take(50) // Truncate for preview
            }
            
            "$entryDate,$entryTime,$referenceDate,$daysAgo,$trackerId,\"$trackerName\",$responseType,\"$mainValue\",$ratingValue,$booleanValue,\"$emotionValue\",\"$notesValue\""
        }
        
        return header + rows.joinToString("\n")
    }
}
