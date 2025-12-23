package com.programminghut.pose_detection.data.manager

import android.content.Context
import com.programminghut.pose_detection.data.dao.ExerciseDao
import com.programminghut.pose_detection.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Phase 6: Exercise Preset Manager
 * 
 * Gestisce i preset predefiniti di esercizi e permette la creazione
 * di esercizi personalizzati.
 * 
 * Fornisce:
 * - Preset predefiniti (Squat, Push-up, Pull-up, etc.)
 * - Creazione esercizi custom
 * - Import/Export configurazioni
 * - Validazione regole esercizi
 */
class ExercisePresetManager(
    private val exerciseDao: ExerciseDao,
    private val context: Context
) {
    
    /**
     * Inizializza il database con i preset predefiniti se vuoto
     */
    suspend fun initializePresetsIfNeeded() {
        val exerciseCount = exerciseDao.getExerciseCount()
        
        if (exerciseCount == 0) {
            // Inserisci tutti i preset predefiniti
            val presets = getAllPresets()
            val exercises = presets.map { it.createExercise() }
            exerciseDao.insertExercises(exercises)
        }
    }
    
    /**
     * Ottieni tutti i preset predefiniti disponibili
     */
    fun getAllPresets(): List<ExercisePreset> {
        return listOf(
            getSquatPreset(),
            getPushUpPreset(),
            getPullUpPreset(),
            getLungePreset(),
            getPlankPreset()
        )
    }
    
    /**
     * Ottieni preset per lo squat
     */
    fun getSquatPreset(): ExercisePreset {
        return ExercisePreset(
            name = "Squat",
            type = ExerciseType.SQUAT,
            description = "Squat classico: scendi piegando le ginocchia fino a 90° e risali",
            difficulty = ExerciseDifficulty.BEGINNER,
            muscleGroups = listOf(MuscleGroup.LEGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
            createExercise = {
                Exercise(
                    name = "Squat",
                    type = ExerciseType.SQUAT,
                    description = "Squat classico con peso corporeo",
                    startPositionKeypoints = FloatArray(51), // Placeholder - verrà calibrato
                    endPositionKeypoints = FloatArray(51),   // Placeholder - verrà calibrato
                    rules = listOf(
                        // Regola 1: Ginocchia devono scendere sotto un certo livello
                        ExerciseRule(
                            ruleType = RuleType.DISTANCE_MIN,
                            keypoints = listOf(5, 13),  // Spalla sinistra -> Ginocchio sinistro
                            targetValue = 0.3f,
                            tolerance = 0.05f,
                            weight = 1.5f,
                            description = "Scendi abbastanza in basso"
                        ),
                        // Regola 2: Simmetria sinistra-destra
                        ExerciseRule(
                            ruleType = RuleType.SYMMETRY_LEFT_RIGHT,
                            keypoints = listOf(5, 6, 13, 14),  // Spalle e ginocchia
                            targetValue = 0.0f,
                            tolerance = 0.1f,
                            weight = 1.0f,
                            description = "Mantieni il corpo simmetrico"
                        ),
                        // Regola 3: Piedi allineati
                        ExerciseRule(
                            ruleType = RuleType.POSITION_ABOVE,
                            keypoints = listOf(15, 16),  // Caviglie sinistre e destre
                            targetValue = 0.0f,
                            tolerance = 0.05f,
                            weight = 0.8f,
                            description = "Mantieni i piedi allineati"
                        ),
                        // Regola 4: Keypoint visibili
                        ExerciseRule(
                            ruleType = RuleType.VISIBILITY_REQUIRED,
                            keypoints = listOf(5, 6, 11, 12, 13, 14, 15, 16),
                            targetValue = 0.45f,
                            tolerance = 0.0f,
                            weight = 2.0f,
                            description = "Corpo completamente visibile"
                        )
                    ),
                    depthTolerance = 0.05f,
                    symmetryTolerance = 0.1f,
                    isCustom = false,
                    tags = listOf("legs", "beginner", "bodyweight")
                )
            }
        )
    }
    
    /**
     * Ottieni preset per le flessioni (push-up)
     */
    fun getPushUpPreset(): ExercisePreset {
        return ExercisePreset(
            name = "Push-up",
            type = ExerciseType.PUSH_UP,
            description = "Flessioni classiche: scendi con il petto verso terra e spingi su",
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.ARMS, MuscleGroup.CORE),
            createExercise = {
                Exercise(
                    name = "Push-up",
                    type = ExerciseType.PUSH_UP,
                    description = "Flessioni a corpo libero",
                    startPositionKeypoints = FloatArray(51),
                    endPositionKeypoints = FloatArray(51),
                    rules = listOf(
                        // Regola 1: Angolo gomiti < 90° in posizione bassa
                        ExerciseRule(
                            ruleType = RuleType.ANGLE_MIN,
                            keypoints = listOf(5, 7, 9),  // Spalla-Gomito-Polso sinistro
                            targetValue = 70f,
                            tolerance = 20f,
                            weight = 1.5f,
                            description = "Scendi abbastanza con il petto"
                        ),
                        // Regola 2: Corpo dritto (plank position)
                        ExerciseRule(
                            ruleType = RuleType.ANGLE_EQUALS,
                            keypoints = listOf(5, 11, 13),  // Spalla-Anca-Ginocchio
                            targetValue = 180f,
                            tolerance = 15f,
                            weight = 1.0f,
                            description = "Mantieni il corpo dritto"
                        ),
                        // Regola 3: Simmetria braccia
                        ExerciseRule(
                            ruleType = RuleType.SYMMETRY_LEFT_RIGHT,
                            keypoints = listOf(5, 6, 7, 8),  // Spalle e gomiti
                            targetValue = 0.0f,
                            tolerance = 0.1f,
                            weight = 1.0f,
                            description = "Braccia simmetriche"
                        )
                    ),
                    depthTolerance = 0.08f,
                    symmetryTolerance = 0.12f,
                    isCustom = false,
                    tags = listOf("chest", "arms", "intermediate", "bodyweight")
                )
            }
        )
    }
    
    /**
     * Ottieni preset per le trazioni (pull-up)
     */
    fun getPullUpPreset(): ExercisePreset {
        return ExercisePreset(
            name = "Pull-up",
            type = ExerciseType.PULL_UP,
            description = "Trazioni alla sbarra: solleva il corpo fino a portare il mento sopra la sbarra",
            difficulty = ExerciseDifficulty.ADVANCED,
            muscleGroups = listOf(MuscleGroup.BACK, MuscleGroup.ARMS, MuscleGroup.CORE),
            createExercise = {
                Exercise(
                    name = "Pull-up",
                    type = ExerciseType.PULL_UP,
                    description = "Trazioni complete alla sbarra",
                    startPositionKeypoints = FloatArray(51),
                    endPositionKeypoints = FloatArray(51),
                    rules = listOf(
                        // Regola 1: Mento sopra le mani (polsi)
                        ExerciseRule(
                            ruleType = RuleType.POSITION_ABOVE,
                            keypoints = listOf(0, 9),  // Naso -> Polso sinistro
                            targetValue = 0.0f,
                            tolerance = 0.05f,
                            weight = 2.0f,
                            description = "Porta il mento sopra la sbarra"
                        ),
                        // Regola 2: Braccia completamente estese in posizione bassa
                        ExerciseRule(
                            ruleType = RuleType.ANGLE_MIN,
                            keypoints = listOf(5, 7, 9),  // Spalla-Gomito-Polso
                            targetValue = 160f,
                            tolerance = 15f,
                            weight = 1.0f,
                            description = "Estendi completamente le braccia"
                        ),
                        // Regola 3: Simmetria braccia
                        ExerciseRule(
                            ruleType = RuleType.SYMMETRY_LEFT_RIGHT,
                            keypoints = listOf(5, 6, 7, 8),
                            targetValue = 0.0f,
                            tolerance = 0.1f,
                            weight = 1.0f,
                            description = "Tira con entrambe le braccia"
                        )
                    ),
                    depthTolerance = 0.06f,
                    symmetryTolerance = 0.1f,
                    isCustom = false,
                    tags = listOf("back", "arms", "advanced", "pull")
                )
            }
        )
    }
    
    /**
     * Ottieni preset per gli affondi (lunges)
     */
    fun getLungePreset(): ExercisePreset {
        return ExercisePreset(
            name = "Lunge",
            type = ExerciseType.LUNGE,
            description = "Affondi: un passo avanti e piega entrambe le ginocchia",
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            muscleGroups = listOf(MuscleGroup.LEGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
            createExercise = {
                Exercise(
                    name = "Lunge",
                    type = ExerciseType.LUNGE,
                    description = "Affondi alternati",
                    startPositionKeypoints = FloatArray(51),
                    endPositionKeypoints = FloatArray(51),
                    rules = listOf(
                        // Regola 1: Ginocchio anteriore a 90°
                        ExerciseRule(
                            ruleType = RuleType.ANGLE_EQUALS,
                            keypoints = listOf(11, 13, 15),  // Anca-Ginocchio-Caviglia
                            targetValue = 90f,
                            tolerance = 15f,
                            weight = 1.5f,
                            description = "Ginocchio anteriore a 90 gradi"
                        ),
                        // Regola 2: Busto eretto
                        ExerciseRule(
                            ruleType = RuleType.ANGLE_MIN,
                            keypoints = listOf(0, 11, 13),  // Testa-Anca-Ginocchio
                            targetValue = 150f,
                            tolerance = 20f,
                            weight = 1.0f,
                            description = "Mantieni il busto eretto"
                        )
                    ),
                    depthTolerance = 0.07f,
                    symmetryTolerance = 0.15f,
                    isCustom = false,
                    tags = listOf("legs", "intermediate", "unilateral")
                )
            }
        )
    }
    
    /**
     * Ottieni preset per il plank
     */
    fun getPlankPreset(): ExercisePreset {
        return ExercisePreset(
            name = "Plank",
            type = ExerciseType.PLANK,
            description = "Plank isometrico: mantieni il corpo dritto sui gomiti",
            difficulty = ExerciseDifficulty.BEGINNER,
            muscleGroups = listOf(MuscleGroup.CORE, MuscleGroup.SHOULDERS),
            createExercise = {
                Exercise(
                    name = "Plank",
                    type = ExerciseType.PLANK,
                    description = "Plank isometrico",
                    startPositionKeypoints = FloatArray(51),
                    endPositionKeypoints = FloatArray(51),
                    rules = listOf(
                        // Regola 1: Corpo dritto
                        ExerciseRule(
                            ruleType = RuleType.ANGLE_EQUALS,
                            keypoints = listOf(5, 11, 15),  // Spalla-Anca-Caviglia
                            targetValue = 180f,
                            tolerance = 10f,
                            weight = 2.0f,
                            description = "Mantieni il corpo dritto"
                        ),
                        // Regola 2: Gomiti sotto le spalle
                        ExerciseRule(
                            ruleType = RuleType.DISTANCE_MAX,
                            keypoints = listOf(5, 7),  // Spalla-Gomito
                            targetValue = 0.15f,
                            tolerance = 0.05f,
                            weight = 1.0f,
                            description = "Gomiti sotto le spalle"
                        )
                    ),
                    depthTolerance = 0.03f,
                    symmetryTolerance = 0.08f,
                    isCustom = false,
                    tags = listOf("core", "beginner", "isometric")
                )
            }
        )
    }
    
    /**
     * Ottieni tutti gli esercizi dal database
     */
    fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises()
    }
    
    /**
     * Ottieni esercizi per tipo
     */
    fun getExercisesByType(type: ExerciseType): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByType(type)
    }
    
    /**
     * Crea un esercizio personalizzato
     */
    suspend fun createCustomExercise(
        name: String,
        description: String,
        startPositionKeypoints: FloatArray,
        endPositionKeypoints: FloatArray,
        rules: List<ExerciseRule>,
        tags: List<String> = emptyList()
    ): Long {
        val exercise = Exercise(
            name = name,
            type = ExerciseType.CUSTOM,
            description = description,
            startPositionKeypoints = startPositionKeypoints,
            endPositionKeypoints = endPositionKeypoints,
            rules = rules,
            isCustom = true,
            tags = tags
        )
        
        return exerciseDao.insertExercise(exercise)
    }
    
    /**
     * Aggiorna un esercizio esistente
     */
    suspend fun updateExercise(exercise: Exercise) {
        val updated = exercise.copy(modifiedAt = System.currentTimeMillis())
        exerciseDao.updateExercise(updated)
    }
    
    /**
     * Elimina un esercizio
     */
    suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise)
    }
    
    /**
     * Esporta un esercizio in formato JSON per analisi LLM
     */
    fun exportExerciseForLLM(exercise: Exercise): String {
        return buildString {
            appendLine("=== EXERCISE DEFINITION ===")
            appendLine("Name: ${exercise.name}")
            appendLine("Type: ${exercise.type}")
            appendLine("Description: ${exercise.description}")
            appendLine()
            appendLine("=== RULES ===")
            exercise.rules.forEachIndexed { index, rule ->
                appendLine("Rule ${index + 1}:")
                appendLine("  Type: ${rule.ruleType}")
                appendLine("  Keypoints: ${rule.keypoints.joinToString()}")
                appendLine("  Target Value: ${rule.targetValue}")
                appendLine("  Tolerance: ${rule.tolerance}")
                appendLine("  Weight: ${rule.weight}")
                appendLine("  Description: ${rule.description}")
                appendLine()
            }
            appendLine("=== TOLERANCES ===")
            appendLine("Depth Tolerance: ${exercise.depthTolerance}")
            appendLine("Symmetry Tolerance: ${exercise.symmetryTolerance}")
            appendLine()
            appendLine("=== METADATA ===")
            appendLine("Tags: ${exercise.tags.joinToString()}")
            appendLine("Custom: ${exercise.isCustom}")
        }
    }
}
