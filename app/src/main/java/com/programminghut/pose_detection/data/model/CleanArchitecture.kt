package com.programminghut.pose_detection.data.model

/**
 * REGOLA D'ORO - SEPARAZIONE CONCETTUALE
 * 
 * Questa separazione è FONDAMENTALE per evitare bug:
 * 
 * ❌ TEMPLATE (Exercise, Workout) NON hanno MAI:
 *   - date
 *   - risultati
 *   - stato completato
 *   - ripetizioni effettive
 * 
 * ✅ SESSION (TodaySession, SessionItem) È L'UNICA con:
 *   - ripetizioni reali
 *   - tempo effettivo
 *   - note
 *   - stato completamento
 */

// ========================================
// TEMPLATE - RIUTILIZZABILI, NO TRACKING
// ========================================

/**
 * Template di esercizio - SOLO definizione, NO tracking
 */
data class ExerciseTemplate(
    val id: Long = 0,
    val name: String,
    val type: TemplateExerciseType,
    val mode: TemplateExerciseMode,
    val description: String = "",
    val imagePath: String? = null,
    val thumbnailPath: String? = null,
    
    // Parametri SUGGERITI (non tracking)
    val defaultReps: Int? = null,
    val defaultTime: Int? = null,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val isCustom: Boolean = true
)

/**
 * Template di allenamento - SOLO definizione, NO tracking
 */
data class WorkoutTemplate(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val imagePath: String? = null,
    val thumbnailPath: String? = null,
    
    // Lista esercizi del circuito (ordine + parametri target)
    val exercises: List<WorkoutExerciseTemplate> = emptyList(),
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val estimatedDuration: Int = 0, // minuti stimati
    val isCustom: Boolean = true
)

/**
 * Relazione esercizio in allenamento - SOLO template
 */
data class WorkoutExerciseTemplate(
    val exerciseId: Long,
    val orderIndex: Int,
    val targetReps: Int? = null,
    val targetTime: Int? = null,
    val restTime: Int = 30 // secondi di riposo dopo l'esercizio
)

// ========================================
// SESSION - TRACKING REALE, CON DATE
// ========================================

/**
 * Sessione giornaliera - L'UNICA con tracking reale
 */
data class TodaySession(
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val notes: String = "",
    
    // Lista elementi nella sessione
    val items: List<SessionItem> = emptyList()
)

/**
 * Elemento nella sessione - COPIA del template con tracking
 */
data class SessionItem(
    val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    
    // Riferimento al template originale (per info)
    val sourceType: SourceType,
    val sourceId: Long, // ID del template originale
    
    // COPIA dei dati template (modificabili nella sessione)
    val name: String,
    val type: TemplateExerciseType,
    val mode: TemplateExerciseMode,
    
    // Parametri TARGET per questa sessione (override del template)
    val targetReps: Int? = null,
    val targetTime: Int? = null,
    
    // TRACKING REALE - solo qui esistono
    val actualReps: Int = 0,
    val actualTime: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String = "",
    
    // Dati speciali (per squat AI, etc)
    val specialData: String? = null
)

// ========================================
// ENUMS E TIPI CONDIVISI
// ========================================

enum class TemplateExerciseType {
    STRENGTH,    // Forza (push-up, sit-up, etc)
    CARDIO,      // Cardio (burpees, jumping jacks)
    FLEXIBILITY, // Flessibilità (stretching)
    BALANCE,     // Equilibrio (plank, pose holds)
    SQUAT_AI,    // Squat speciale con AI
    CUSTOM       // Personalizzato
}

enum class TemplateExerciseMode {
    REPS,   // Ripetizioni (push-up, squat)
    TIME    // Tempo (plank, wall-sit)
}

enum class SourceType {
    EXERCISE_TEMPLATE,  // Creato da ExerciseTemplate
    WORKOUT_TEMPLATE,   // Creato da WorkoutTemplate  
    QUICK_ADD          // Aggiunto rapidamente (squat, etc)
}

// ========================================
// REGOLE DI CONVERSIONE
// ========================================

/**
 * Converte ExerciseTemplate in SessionItem (per "Usa oggi")
 */
fun ExerciseTemplate.toSessionItem(
    sessionId: Long,
    orderIndex: Int,
    targetReps: Int? = this.defaultReps,
    targetTime: Int? = this.defaultTime
): SessionItem = SessionItem(
    sessionId = sessionId,
    orderIndex = orderIndex,
    sourceType = SourceType.EXERCISE_TEMPLATE,
    sourceId = this.id,
    name = this.name,
    type = this.type,
    mode = this.mode,
    targetReps = targetReps,
    targetTime = targetTime
)

/**
 * Converte WorkoutTemplate in lista di SessionItem (per "Usa oggi")
 */
fun WorkoutTemplate.toSessionItems(
    sessionId: Long,
    startOrderIndex: Int
): List<SessionItem> = exercises.mapIndexed { index, exerciseTemplate ->
    // Qui dovremmo fare lookup del template esercizio, per ora mock
    SessionItem(
        sessionId = sessionId,
        orderIndex = startOrderIndex + index,
        sourceType = SourceType.WORKOUT_TEMPLATE,
        sourceId = this.id,
        name = "Esercizio ${index + 1}", // TODO: lookup reale
        type = TemplateExerciseType.CUSTOM,
        mode = TemplateExerciseMode.REPS,
        targetReps = exerciseTemplate.targetReps,
        targetTime = exerciseTemplate.targetTime
    )
}

// Extension functions per compatibilità
val ExerciseTemplate.targetReps: Int? get() = defaultReps
val ExerciseTemplate.targetTime: Int? get() = defaultTime