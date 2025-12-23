package com.programminghut.pose_detection.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Phase 6: Multi-Exercise Tracking System - Data Models
 * 
 * Sistema flessibile per definire e tracciare diversi tipi di esercizi
 * basati su pose detection.
 */

/**
 * Definizione semplificata di un esercizio per tracking manuale
 * 
 * L'esercizio è un'entità statica che contiene solo:
 * - Metadati (nome, descrizione, immagine, mode)
 * - I valori target (reps/time) sono memorizzati in WorkoutExercise
 * - I valori effettivi eseguiti sono memorizzati in WorkoutSession
 * NON contiene logiche di riconoscimento camera (tranne SQUAT che usa camera separatamente).
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Long = 0,
    
    val name: String,                    // Nome esercizio (es. "Push-up", "Plank")
    val type: ExerciseType,              // Tipo esercizio (SQUAT usa camera, altri manuali)
    val description: String = "",        // Descrizione per export e storico
    
    // Modalità esercizio
    val mode: ExerciseMode,              // REPS (ripetizioni) o TIME (isometrico)
    
    // Immagine di riferimento
    val imagePath: String? = null,       // Path foto esercizio (preview/miniatura)
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isCustom: Boolean = false,       // Se è un esercizio custom
    val tags: List<String> = emptyList() // Tag per categorizzazione (es. "upper_body")
)

/**
 * Modalità di esecuzione esercizio
 */
enum class ExerciseMode {
    REPS,       // A ripetizioni (es. push-up, pull-up)
    TIME        // Isometrico/a tempo (es. plank, wall sit)
}

/**
 * Tipo di esercizio
 */
enum class ExerciseType {
    SQUAT,          // Squat classico (USA CAMERA per tracking automatico)
    PUSH_UP,        // Flessioni (tracking manuale)
    PULL_UP,        // Trazioni (tracking manuale)
    LUNGE,          // Affondi (tracking manuale)
    PLANK,          // Plank statico (tracking manuale)
    CUSTOM;         // Esercizio personalizzato (tracking manuale)
    
    companion object {
        fun fromString(value: String): ExerciseType {
            return values().find { it.name == value.uppercase() } ?: CUSTOM
        }
        
        /**
         * Solo SQUAT usa il riconoscimento camera.
         * Tutti gli altri sono manuali.
         */
        fun usesCameraTracking(type: ExerciseType): Boolean {
            return type == SQUAT
        }
    }
}

/**
 * Preset predefinito per un esercizio
 * Contiene configurazione base pronta all'uso
 */
data class ExercisePreset(
    val name: String,
    val type: ExerciseType,
    val description: String,
    val mode: ExerciseMode,
    val difficulty: ExerciseDifficulty,
    val muscleGroups: List<MuscleGroup>,
    
    // Factory function per creare Exercise da preset
    val createExercise: () -> Exercise
)

/**
 * Difficoltà dell'esercizio
 */
enum class ExerciseDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * Gruppi muscolari coinvolti
 */
enum class MuscleGroup {
    LEGS,           // Gambe
    CHEST,          // Petto
    BACK,           // Schiena
    SHOULDERS,      // Spalle
    ARMS,           // Braccia
    CORE,           // Core/addome
    GLUTES;         // Glutei
}

/**
 * Type converters per Room Database
 */
class ExerciseTypeConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String {
        return value.name
    }
    
    @TypeConverter
    fun toExerciseType(value: String): ExerciseType {
        return ExerciseType.fromString(value)
    }
    
    @TypeConverter
    fun fromExerciseMode(value: ExerciseMode): String {
        return value.name
    }
    
    @TypeConverter
    fun toExerciseMode(value: String): ExerciseMode {
        return try {
            ExerciseMode.valueOf(value)
        } catch (e: Exception) {
            ExerciseMode.REPS // Default fallback
        }
    }
}
