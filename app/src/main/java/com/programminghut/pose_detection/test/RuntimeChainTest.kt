package com.programminghut.pose_detection.test

import android.util.Log
import com.programminghut.pose_detection.data.model.ExerciseTemplate
import com.programminghut.pose_detection.data.model.WorkoutTemplate
import com.programminghut.pose_detection.data.model.TemplateExerciseType
import com.programminghut.pose_detection.data.model.TemplateExerciseMode

/**
 * Test runtime per verificare la catena logica durante l'esecuzione
 * 
 * Chiama questi metodi nelle Activity per vedere i log e verificare
 * che i dati fluiscano correttamente.
 */
object RuntimeChainTest {
    
    private const val TAG = "🔍 CHAIN_TEST"
    
    /**
     * Test da chiamare quando si apre ExerciseLibraryActivity
     */
    fun testExerciseLibraryFlow(isSelectionMode: Boolean, exerciseList: List<ExerciseTemplate>) {
        Log.d(TAG, "=== TEST EXERCISE LIBRARY ===")
        Log.d(TAG, "Modalità selezione: $isSelectionMode")
        Log.d(TAG, "Numero esercizi: ${exerciseList.size}")
        
        exerciseList.forEachIndexed { index, exercise ->
            Log.d(TAG, "Esercizio $index: ID=${exercise.id}, Nome=${exercise.name}, Tipo=${exercise.type}")
            
            // ✅ Verifica che gli ID siano reali
            if (exercise.id <= 0) {
                Log.e(TAG, "❌ ERRORE: Esercizio con ID non valido: ${exercise.id}")
            } else {
                Log.d(TAG, "✅ OK: ID valido per ${exercise.name}")
            }
        }
    }
    
    /**
     * Test da chiamare quando si seleziona un esercizio
     */
    fun testExerciseSelection(exerciseId: Long, exerciseName: String, fromWhere: String) {
        Log.d(TAG, "=== TEST SELEZIONE ESERCIZIO ===")
        Log.d(TAG, "Sorgente: $fromWhere")
        Log.d(TAG, "Esercizio selezionato: ID=$exerciseId, Nome=$exerciseName")
        
        // ✅ Verifica che l'ID sia reale
        if (exerciseId > 0) {
            Log.d(TAG, "✅ SUCCESSO: ID reale ricevuto da $fromWhere")
        } else {
            Log.e(TAG, "❌ FALLIMENTO: ID non valido da $fromWhere")
        }
        
        // ✅ Verifica che non stiamo creando oggetti finti dal nome
        if (exerciseName.isNotBlank() && exerciseId > 0) {
            Log.d(TAG, "✅ OK: Abbiamo sia ID reale che nome corretto")
        } else {
            Log.e(TAG, "❌ PROBLEMA: Dati incompleti - ID:$exerciseId, Nome:'$exerciseName'")
        }
    }
    
    /**
     * Test da chiamare in NewMainActivity quando si riceve un risultato
     */
    fun testTodayAddFlow(exerciseId: Long, workoutId: Long, action: String) {
        Log.d(TAG, "=== TEST AGGIUNTA TODAY ===")
        Log.d(TAG, "Azione: $action")
        
        if (exerciseId > 0) {
            Log.d(TAG, "✅ Aggiunta esercizio con ID reale: $exerciseId")
            Log.d(TAG, "✅ CATENA CORRETTA: ExerciseLibrary -> Today (ID: $exerciseId)")
        } else {
            Log.e(TAG, "❌ Problema esercizio: ID non valido $exerciseId")
        }
        
        if (workoutId > 0) {
            Log.d(TAG, "✅ Aggiunta workout con ID reale: $workoutId")  
            Log.d(TAG, "✅ CATENA CORRETTA: WorkoutLibrary -> Today (ID: $workoutId)")
        } else if (workoutId == -1L) {
            Log.d(TAG, "ℹ️ Nessun workout (normale se si aggiunge solo esercizio)")
        } else {
            Log.e(TAG, "❌ Problema workout: ID non valido $workoutId")
        }
    }
    
    /**
     * Test da chiamare per verificare la creazione degli oggetti sessione
     */
    fun testSessionCreation(sourceType: String, sourceId: Long, targetObject: String) {
        Log.d(TAG, "=== TEST CREAZIONE SESSIONE ===")
        Log.d(TAG, "Sorgente: $sourceType con ID $sourceId")
        Log.d(TAG, "Oggetto target: $targetObject")
        
        // ✅ Verifica che usiamo ID per creare, non stringhe
        if (sourceId > 0) {
            Log.d(TAG, "✅ CORRETTO: Creazione da ID reale ($sourceId)")
            Log.d(TAG, "✅ REGOLA D'ORO RISPETTATA: Template -> Copia Session")
        } else {
            Log.e(TAG, "❌ VIOLAZIONE: Creazione senza ID valido!")
            Log.e(TAG, "❌ REGOLA D'ORO VIOLATA: Oggetto creato dal nulla?")
        }
    }
    
    /**
     * Test finale per verificare la consistenza di tutta la catena
     */
    fun testFullChainConsistency(libraryCount: Int, workoutCount: Int, sessionCount: Int) {
        Log.d(TAG, "=== TEST CONSISTENZA CATENA COMPLETA ===")
        Log.d(TAG, "Esercizi in libreria: $libraryCount")
        Log.d(TAG, "Workout creati: $workoutCount") 
        Log.d(TAG, "Sessioni oggi: $sessionCount")
        
        // ✅ Verifica logica: se ho sessioni, devono venire dalla libreria
        if (sessionCount > 0 && libraryCount == 0) {
            Log.e(TAG, "❌ IMPOSSIBILE: Sessioni senza esercizi in libreria!")
        } else if (sessionCount > 0 && libraryCount > 0) {
            Log.d(TAG, "✅ COERENTE: Sessioni derivate da libreria")
        } else {
            Log.d(TAG, "ℹ️ Normale: Nessuna sessione ancora")
        }
        
        Log.d(TAG, "=== FINE TEST CATENA ===")
    }
}