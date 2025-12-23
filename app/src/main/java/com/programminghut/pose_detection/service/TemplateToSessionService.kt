package com.programminghut.pose_detection.service

import com.programminghut.pose_detection.data.model.*
import java.util.*

/**
 * Servizio per la conversione Template → Session (funzionalità "Usa Oggi")
 * 
 * RESPONSABILITÀ:
 * - Converte ExerciseTemplate in SessionItem
 * - Converte WorkoutTemplate in TodaySession
 * - Gestisce gli override dei parametri 
 * - Mantiene la connessione con il template originale
 * - NON modifica MAI i template originali
 */
object TemplateToSessionService {
    
    /**
     * Converte un singolo ExerciseTemplate in SessionItem per oggi
     * 
     * @param template Template originale
     * @param sessionId ID della sessione che conterrà l'item
     * @param orderIndex Posizione nell'ordine di esecuzione
     * @param targetRepsOverride Override ripetizioni (se null, usa default del template)
     * @param targetTimeOverride Override tempo (se null, usa default del template)
     * @return SessionItem pronto per il tracking
     */
    fun convertExerciseToSessionItem(
        template: ExerciseTemplate,
        sessionId: Long,
        orderIndex: Int,
        targetRepsOverride: Int? = null,
        targetTimeOverride: Int? = null
    ): SessionItem {
        return SessionItem(
            id = 0, // Sarà assegnato dal database
            sessionId = sessionId,
            orderIndex = orderIndex,
            
            // Riferimento al template originale
            sourceType = SourceType.EXERCISE_TEMPLATE,
            sourceId = template.id,
            
            // COPIA dei dati template
            name = template.name,
            type = template.type,
            mode = template.mode,
            
            // Parametri TARGET (con override se specificato)
            targetReps = when {
                targetRepsOverride != null -> targetRepsOverride
                template.mode == TemplateExerciseMode.REPS -> template.defaultReps
                else -> null
            },
            targetTime = when {
                targetTimeOverride != null -> targetTimeOverride
                template.mode == TemplateExerciseMode.TIME -> template.defaultTime
                else -> null
            },
            
            // Tracking inizializzato vuoto
            actualReps = 0,
            actualTime = 0,
            isCompleted = false,
            completedAt = null,
            notes = ""
        )
    }
    
    /**
     * Converte un WorkoutTemplate completo in TodaySession
     * 
     * @param workoutTemplate Template del workout
     * @param availableExercises Lista degli esercizi disponibili per risolvere gli ID
     * @param parameterOverrides Mappa di override per singoli esercizi (exerciseId -> Pair(reps, time))
     * @return TodaySession con tutti gli esercizi del workout
     */
    fun convertWorkoutToTodaySession(
        workoutTemplate: WorkoutTemplate,
        availableExercises: List<ExerciseTemplate>,
        parameterOverrides: Map<Long, Pair<Int?, Int?>> = emptyMap()
    ): TodaySession {
        
        val sessionId = generateSessionId()
        
        // Converte tutti gli esercizi del workout in SessionItem
        val sessionItems = workoutTemplate.exercises.mapNotNull { workoutExercise ->
            
            // Trova il template dell'esercizio
            val exerciseTemplate = availableExercises.find { it.id == workoutExercise.exerciseId }
                ?: return@mapNotNull null
            
            // Recupera eventuali override per questo esercizio
            val override = parameterOverrides[workoutExercise.exerciseId]
            val repsOverride = override?.first ?: workoutExercise.targetReps
            val timeOverride = override?.second ?: workoutExercise.targetTime
            
            // Converte in SessionItem
            convertExerciseToSessionItem(
                template = exerciseTemplate,
                sessionId = sessionId,
                orderIndex = workoutExercise.orderIndex,
                targetRepsOverride = repsOverride,
                targetTimeOverride = timeOverride
            )
        }
        
        return TodaySession(
            id = sessionId,
            date = getTodayTimestamp(),
            isCompleted = false,
            startTime = null,
            endTime = null,
            notes = "Sessione creata da: ${workoutTemplate.name}",
            items = sessionItems
        )
    }
    
    /**
     * Converte un singolo esercizio in una sessione di oggi (per "Usa Oggi" da ExerciseLibrary)
     * 
     * @param template Template dell'esercizio
     * @param targetRepsOverride Override ripetizioni
     * @param targetTimeOverride Override tempo
     * @return TodaySession con un solo esercizio
     */
    fun convertSingleExerciseToTodaySession(
        template: ExerciseTemplate,
        targetRepsOverride: Int? = null,
        targetTimeOverride: Int? = null
    ): TodaySession {
        
        val sessionId = generateSessionId()
        
        val sessionItem = convertExerciseToSessionItem(
            template = template,
            sessionId = sessionId,
            orderIndex = 0,
            targetRepsOverride = targetRepsOverride,
            targetTimeOverride = targetTimeOverride
        )
        
        return TodaySession(
            id = sessionId,
            date = getTodayTimestamp(),
            isCompleted = false,
            startTime = null,
            endTime = null,
            notes = "Esercizio singolo: ${template.name}",
            items = listOf(sessionItem)
        )
    }
    
    /**
     * Verifica se esiste già una sessione per oggi
     * 
     * @param existingSessions Lista delle sessioni esistenti
     * @return TodaySession esistente per oggi, null se non esiste
     */
    fun getTodaySession(existingSessions: List<TodaySession>): TodaySession? {
        val today = getTodayTimestamp()
        return existingSessions.find { isSameDay(it.date, today) }
    }
    
    /**
     * Aggiunge esercizi a una sessione esistente (se si usa "Usa Oggi" multiple volte)
     * 
     * @param existingSession Sessione esistente
     * @param newItems Nuovi SessionItem da aggiungere
     * @return TodaySession aggiornata
     */
    fun addItemsToExistingSession(
        existingSession: TodaySession,
        newItems: List<SessionItem>
    ): TodaySession {
        
        val maxOrderIndex = existingSession.items.maxOfOrNull { it.orderIndex } ?: -1
        
        val adjustedNewItems = newItems.mapIndexed { index, item ->
            item.copy(
                sessionId = existingSession.id,
                orderIndex = maxOrderIndex + 1 + index
            )
        }
        
        return existingSession.copy(
            items = existingSession.items + adjustedNewItems
        )
    }
    
    /**
     * Strategia per gestire conflitti quando si aggiunge a sessione esistente
     */
    enum class ConflictResolution {
        ADD_TO_END,      // Aggiungi alla fine (default)
        REPLACE_ALL,     // Sostituisci tutto
        ASK_USER         // Chiedi all'utente
    }
    
    /**
     * Gestisce il "Usa Oggi" con strategia per conflitti
     */
    fun handleUseToday(
        template: ExerciseTemplate,
        existingSessions: List<TodaySession>,
        conflictResolution: ConflictResolution = ConflictResolution.ADD_TO_END,
        targetRepsOverride: Int? = null,
        targetTimeOverride: Int? = null
    ): UsetodayResult {
        
        val todaySession = getTodaySession(existingSessions)
        
        return if (todaySession == null) {
            // Nessuna sessione oggi, creane una nuova
            val newSession = convertSingleExerciseToTodaySession(
                template, targetRepsOverride, targetTimeOverride
            )
            UseFromExerciseResult.NewSession(newSession)
            
        } else {
            // Sessione esiste già, gestisci il conflitto
            when (conflictResolution) {
                ConflictResolution.ADD_TO_END -> {
                    val newItem = convertExerciseToSessionItem(
                        template = template,
                        sessionId = todaySession.id,
                        orderIndex = (todaySession.items.maxOfOrNull { it.orderIndex } ?: -1) + 1,
                        targetRepsOverride = targetRepsOverride,
                        targetTimeOverride = targetTimeOverride
                    )
                    val updatedSession = addItemsToExistingSession(todaySession, listOf(newItem))
                    UseFromExerciseResult.UpdatedSession(updatedSession)
                }
                
                ConflictResolution.REPLACE_ALL -> {
                    val newSession = convertSingleExerciseToTodaySession(
                        template, targetRepsOverride, targetTimeOverride
                    ).copy(id = todaySession.id)
                    UseFromExerciseResult.ReplacedSession(newSession)
                }
                
                ConflictResolution.ASK_USER -> {
                    UseFromExerciseResult.ConflictNeedsResolution(todaySession, template)
                }
            }
        }
    }
    
    // UTILITY FUNCTIONS
    
    private fun generateSessionId(): Long = System.currentTimeMillis()
    
    private fun getTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * Risultati possibili per "Usa Oggi" da singolo esercizio
 */
sealed class UseFromExerciseResult {
    data class NewSession(val session: TodaySession) : UseFromExerciseResult()
    data class UpdatedSession(val session: TodaySession) : UseFromExerciseResult()
    data class ReplacedSession(val session: TodaySession) : UseFromExerciseResult()
    data class ConflictNeedsResolution(
        val existingSession: TodaySession, 
        val newExercise: ExerciseTemplate
    ) : UseFromExerciseResult()
}

/**
 * Alias per risultati "Usa Oggi" - riutilizziamo la stessa struttura per workout e esercizi
 */
typealias UsetodayResult = UseFromExerciseResult