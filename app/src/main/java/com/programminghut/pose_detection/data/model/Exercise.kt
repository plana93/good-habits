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
 * Definizione completa di un esercizio
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Long = 0,
    
    val name: String,                    // Nome esercizio (es. "Squat", "Push-up")
    val type: ExerciseType,              // Tipo (predefinito o custom)
    val description: String = "",        // Descrizione dell'esercizio
    
    // Posizioni di riferimento
    val startPositionKeypoints: FloatArray,  // Keypoints posizione iniziale
    val endPositionKeypoints: FloatArray,    // Keypoints posizione finale
    
    // Regole di validazione
    val rules: List<ExerciseRule>,       // Regole per validare la rep
    
    // Metriche e tolleranze
    val depthTolerance: Float = 0.05f,   // Tolleranza per profondità movimento
    val symmetryTolerance: Float = 0.1f, // Tolleranza per simmetria
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isCustom: Boolean = false,       // Se è un esercizio custom
    val imageUri: String? = null,        // Immagine di riferimento (opzionale)
    val tags: List<String> = emptyList() // Tag per categorizzazione
) {
    // Override equals per FloatArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Exercise
        
        if (exerciseId != other.exerciseId) return false
        if (name != other.name) return false
        if (!startPositionKeypoints.contentEquals(other.startPositionKeypoints)) return false
        if (!endPositionKeypoints.contentEquals(other.endPositionKeypoints)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = exerciseId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + startPositionKeypoints.contentHashCode()
        result = 31 * result + endPositionKeypoints.contentHashCode()
        return result
    }
}

/**
 * Tipo di esercizio
 */
enum class ExerciseType {
    SQUAT,          // Squat classico
    PUSH_UP,        // Flessioni
    PULL_UP,        // Trazioni
    LUNGE,          // Affondi
    PLANK,          // Plank (statico)
    CUSTOM;         // Esercizio personalizzato
    
    companion object {
        fun fromString(value: String): ExerciseType {
            return values().find { it.name == value.uppercase() } ?: CUSTOM
        }
    }
}

/**
 * Regola per validare una ripetizione dell'esercizio
 */
data class ExerciseRule(
    val ruleType: RuleType,              // Tipo di regola
    val keypoints: List<Int>,            // Keypoint coinvolti (indici)
    val targetValue: Float,              // Valore target (angolo, distanza, ecc.)
    val tolerance: Float = 0.1f,         // Tolleranza accettabile
    val weight: Float = 1.0f,            // Peso della regola nel calcolo score
    val description: String = ""         // Descrizione umana della regola
)

/**
 * Tipi di regole disponibili
 */
enum class RuleType {
    // Regole di distanza
    DISTANCE_MIN,           // Distanza minima tra due keypoint
    DISTANCE_MAX,           // Distanza massima tra due keypoint
    DISTANCE_EQUALS,        // Distanza deve essere uguale a valore
    
    // Regole di angolo
    ANGLE_MIN,              // Angolo minimo tra tre keypoint
    ANGLE_MAX,              // Angolo massimo
    ANGLE_EQUALS,           // Angolo deve essere uguale
    
    // Regole di simmetria
    SYMMETRY_LEFT_RIGHT,    // Simmetria sinistra-destra
    SYMMETRY_UP_DOWN,       // Simmetria alto-basso
    
    // Regole di posizione
    POSITION_ABOVE,         // Keypoint A deve essere sopra B
    POSITION_BELOW,         // Keypoint A deve essere sotto B
    POSITION_LEFT,          // Keypoint A deve essere a sinistra di B
    POSITION_RIGHT,         // Keypoint A deve essere a destra di B
    
    // Regole di visibilità
    VISIBILITY_REQUIRED,    // Keypoint deve essere visibile (confidence > threshold)
    
    // Regole temporali
    TIME_MIN,               // Tempo minimo per completare movimento
    TIME_MAX;               // Tempo massimo per completare movimento
}

/**
 * Preset predefinito per un esercizio
 * Contiene configurazione completa pronta all'uso
 */
data class ExercisePreset(
    val name: String,
    val type: ExerciseType,
    val description: String,
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
 * Risultato della validazione di una rep secondo le regole
 */
data class RepValidationResult(
    val isValid: Boolean,               // Se la rep è valida
    val totalScore: Float,              // Score complessivo (0-1)
    val ruleResults: List<RuleResult>,  // Risultati per ogni regola
    val warnings: List<String> = emptyList() // Warning sulla postura
)

/**
 * Risultato di una singola regola
 */
data class RuleResult(
    val rule: ExerciseRule,
    val actualValue: Float,            // Valore misurato
    val expectedValue: Float,          // Valore atteso
    val passed: Boolean,               // Se la regola è passata
    val score: Float                   // Score per questa regola (0-1)
)

/**
 * Type converters per Room Database
 */
class ExerciseTypeConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val type = object : TypeToken<FloatArray>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun fromRuleList(value: List<ExerciseRule>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toRuleList(value: String): List<ExerciseRule> {
        val type = object : TypeToken<List<ExerciseRule>>() {}.type
        return gson.fromJson(value, type)
    }
    
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
}
