package com.programminghut.pose_detection.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverters

/**
 * Workout Entity
 * 
 * Rappresenta un allenamento composto da più esercizi.
 * Ogni workout ha un nome, descrizione, e una lista di esercizi con target specifici.
 */
@Entity(tableName = "workouts")
@TypeConverters(ExerciseTypeConverters::class)
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val workoutId: Long = 0,
    
    val name: String,                    // Nome allenamento (es. "Upper Body Push", "Leg Day")
    val description: String = "",        // Descrizione per export
    
    // Immagine rappresentativa
    val imagePath: String? = null,       // Path immagine (generata o caricata dall'utente)
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isCustom: Boolean = true,        // Di default custom (preset futuri = false)
    val tags: List<String> = emptyList(), // Tag per categorizzazione
    
    // Metriche calcolate (aggiornate dinamicamente)
    val totalExercises: Int = 0,         // Numero totale esercizi
    val estimatedDuration: Int = 0       // Durata stimata in minuti (calcolata dai target)
)

/**
 * WorkoutExercise Junction Table
 * 
 * Relazione many-to-many tra Workout ed Exercise.
 * Permette di specificare target specifici (reps/time) per ogni esercizio nell'allenamento.
 */
@Entity(
    tableName = "workout_exercises",
    primaryKeys = ["workoutId", "exerciseId", "orderIndex"]
)
data class WorkoutExercise(
    val workoutId: Long,                 // FK verso Workout
    val exerciseId: Long,                // FK verso Exercise
    val orderIndex: Int,                 // Ordine nell'allenamento (0, 1, 2...)
    
    // Target per questo esercizio in questo workout
    val targetReps: Int? = null,         // Reps target (per esercizi REPS)
    val targetTime: Int? = null,         // Secondi target (per esercizi TIME)
    val targetSets: Int = 1,             // Numero di serie (futuro)
    val restTime: Int? = null,           // Secondi di riposo (futuro)
    
    val notes: String? = null            // Note specifiche per questo esercizio
)

/**
 * Data class per rappresentare un workout con i suoi esercizi (per query JOIN)
 * Usata con @Relation per query automatiche di Room
 */
data class WorkoutWithExercises(
    @Embedded val workout: Workout,
    @Relation(
        entity = Exercise::class,
        parentColumn = "workoutId",
        entityColumn = "exerciseId",
        associateBy = Junction(
            value = WorkoutExercise::class,
            parentColumn = "workoutId",
            entityColumn = "exerciseId"
        )
    )
    val exercises: List<Exercise>
)

/**
 * Data class semplice per rappresentare un esercizio con i suoi target in un workout
 * Non usata direttamente da Room, ma per logica applicativa
 */
data class ExerciseInWorkout(
    val exercise: Exercise,
    val workoutExercise: WorkoutExercise
)
