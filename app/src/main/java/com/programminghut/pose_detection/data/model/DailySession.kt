package com.programminghut.pose_detection.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.Embedded
import androidx.room.Relation

/**
 * Sessione giornaliera - Container temporaneo per l'allenamento del giorno
 * 
 * Contiene una lista di esercizi singoli e/o circuiti completi da eseguire.
 * È effimera e flessibile: permette modifiche senza alterare template originali.
 */
@Entity(tableName = "daily_sessions")
@TypeConverters(ExerciseTypeConverters::class)
data class DailySession(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    
    val date: Long = System.currentTimeMillis(), // Data sessione (timestamp giornaliero)
    val name: String = "Allenamento del giorno",  // Nome personalizzabile
    
    // Stato sessione
    val isCompleted: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    
    // Metadata
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Elemento di sessione giornaliera - Può essere un esercizio singolo o un circuito
 */
@Entity(tableName = "daily_session_items")
data class DailySessionItem(
    @PrimaryKey(autoGenerate = true)
    val itemId: Long = 0,
    
    val sessionId: Long,                 // FK verso DailySession
    val order: Int,                      // Ordine di esecuzione nella sessione
    
    // Tipo di elemento
    val itemType: SessionItemType,       // EXERCISE o WORKOUT
    val exerciseId: Long? = null,        // FK verso Exercise (se EXERCISE)
    val workoutId: Long? = null,         // FK verso Workout (se WORKOUT)
    
    // Parametri temporanei per questa sessione (sovrascrivono i default)
    val customReps: Int? = null,         // Ripetizioni custom per questa sessione
    val customTime: Int? = null,         // Tempo custom per questa sessione
    val customSets: Int? = null,         // Serie custom per questa sessione
    val customRest: Int? = null,         // Riposo custom per questa sessione
    
    // Stato di completamento
    val isCompleted: Boolean = false,
    val actualReps: Int? = null,         // Ripetizioni effettive eseguite
    val actualTime: Int? = null,         // Tempo effettivo eseguito
    val completedAt: Long? = null,
    
    // Note specifiche per questa esecuzione
    val notes: String = "",
    
    // Relazione gerarchica (se questo esercizio appartiene a un workout nella sessione)
    val parentWorkoutItemId: Long? = null, // FK verso l'elemento workout parent (se applicabile)
    
    // Dati AI (solo per esercizi automatici come squat)
    val aiData: String? = null           // JSON con dati pose detection se applicabile
)

/**
 * Tipo di elemento nella sessione giornaliera
 */
enum class SessionItemType {
    EXERCISE,   // Singolo esercizio
    WORKOUT     // Circuito completo
}

/**
 * Relazione per ottenere una sessione con tutti i suoi elementi (per Room)
 */
data class DailySessionWithItems(
    @Embedded val session: DailySession,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId"
    )
    val items: List<DailySessionItem>
)

/**
 * Elemento di sessione con dettagli di esercizio/workout (per query manuali)
 * Mappato direttamente dalle query JOIN SQL - deve corrispondere ai risultati SQL
 */
data class DailySessionItemWithDetails(
    // Campi della tabella daily_session_items (DailySessionItem)
    val itemId: Long,
    val sessionId: Long,
    val exerciseId: Long?,
    val workoutId: Long?,
    val type: String,  // SessionItemType viene serializzato come String
    val order: Int,
    val targetReps: Int?,
    val targetTime: Int?,
    val actualReps: Int?,
    val actualTime: Int?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val notes: String,
    val aiData: String?,  // ✅ Aggiungo campo AI data
    val name: String,
    val description: String?,
    val parentWorkoutItemId: Long?,
    
    // Dettagli esercizio (da JOIN con exercises) - tutti nullable
    val exerciseName: String?,
    val exerciseDescription: String?,
    val exerciseImagePath: String?,
    val exerciseType: String?,
    val exerciseMode: String?,
    
    // Dettagli workout (da JOIN con workouts) - tutti nullable  
    val workoutName: String?,
    val workoutDescription: String?,
    val workoutImagePath: String?
)