package com.programminghut.pose_detection.data.repository

import android.content.Context
import androidx.room.Transaction
import com.programminghut.pose_detection.data.dao.DailySessionDao
import com.programminghut.pose_detection.data.dao.DailySessionRelationDao
import com.programminghut.pose_detection.data.dao.ExerciseDao
import com.programminghut.pose_detection.data.dao.WorkoutDao
import com.programminghut.pose_detection.data.dao.DailySessionSummary
import com.programminghut.pose_detection.data.dao.ExerciseStats
import com.programminghut.pose_detection.data.dao.WorkoutStats
import com.programminghut.pose_detection.data.model.*
import com.programminghut.pose_detection.data.repository.WorkoutTemplateFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * Repository per gestire le sessioni giornaliere
 * Integra esercizi, allenamenti e sessioni giornaliere modulari
 */
class DailySessionRepository(
    private val dailySessionDao: DailySessionDao,
    private val dailySessionRelationDao: DailySessionRelationDao,
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao
) {
    
    // ============================================================================
    // GESTIONE SESSIONI GIORNALIERE
    // ============================================================================
    
    /**
     * Ottieni o crea la sessione per oggi
     */
    suspend fun getTodaySession(): DailySession {
        return getSessionForDate(Calendar.getInstance().timeInMillis)
    }
    
    /**
     * Ottieni o crea la sessione per una data specifica
     */
    suspend fun getSessionForDate(dateMillis: Long): DailySession {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        android.util.Log.d("SESSION_DEBUG", "🔍 getSessionForDate: input=$dateMillis (${formatDate(dateMillis)}), range=$startOfDay-$endOfDay")
        
        val existingSession = dailySessionDao.getSessionForDate(startOfDay, endOfDay)
        android.util.Log.d("SESSION_DEBUG", "🔍 Sessione trovata: ${existingSession?.sessionId} (data=${existingSession?.date?.let { formatDate(it) }})")
        
        return existingSession ?: createSessionForDate(dateMillis).also {
            android.util.Log.d("SESSION_DEBUG", "🆕 Creata nuova sessione: ${it.sessionId} (data=${formatDate(it.date)})")
        }
    }
    
    /**
     * Crea una nuova sessione per oggi
     */
    private suspend fun createTodaySession(): DailySession {
        return createSessionForDate(System.currentTimeMillis())
    }
    
    /**
     * Crea una nuova sessione per una data specifica
     */
    private suspend fun createSessionForDate(dateMillis: Long): DailySession {
        val now = System.currentTimeMillis()
        val targetDay = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 12) // Mezzogiorno come riferimento
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val session = DailySession(
            name = "Allenamento ${formatDate(targetDay)}",
            date = targetDay,
            createdAt = now
        )
        
        val sessionId = dailySessionDao.insertDailySession(session)
        return session.copy(sessionId = sessionId)
    }
    
    /**
     * Ottieni sessione completa con elementi - crea automaticamente se non esiste
     */
    fun getTodaySessionWithItems(): Flow<DailySessionWithItems?> {
        return getSessionWithItemsForDate(Calendar.getInstance().timeInMillis)
    }
    
    /**
     * Ottieni sessione completa con elementi per una data specifica
     * NON crea automaticamente sessioni per giorni passati vuoti
     */
    fun getSessionWithItemsForDate(dateMillis: Long): Flow<DailySessionWithItems?> = flow {
        android.util.Log.d("TODAY_DEBUG", "🌊🌊🌊 === INIZIO getSessionWithItemsForDate ===")
        android.util.Log.d("TODAY_DEBUG", "🌊 Input dateMillis: $dateMillis (${formatDate(dateMillis)})")
        
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        
        android.util.Log.d("TODAY_DEBUG", "🌊 Range calcolato: startOfDay=$startOfDay, endOfDay=$endOfDay")
        
        // ✅ Controlla se è oggi - solo per oggi creiamo automaticamente la sessione
        val isToday = isDateToday(dateMillis)
        android.util.Log.d("TODAY_DEBUG", "🌊 isToday check: $isToday")
        
        if (isToday) {
            // 🎯 CRITICA: NON creare nuove sessioni per oggi - usa quella esistente se disponibile
            android.util.Log.d("TODAY_DEBUG", "🌊 È oggi - controllando sessioni esistenti...")
            try {
                val existingSession = dailySessionDao.getSessionForDate(startOfDay, endOfDay)
                if (existingSession != null) {
                    android.util.Log.d("TODAY_DEBUG", "🌊 ✅ Sessione esistente trovata: ID=${existingSession.sessionId}")
                } else {
                    android.util.Log.d("TODAY_DEBUG", "🌊 ❌ Nessuna sessione per oggi - sarà creata automaticamente se necessario")
                    // Non creare automaticamente - lascia che venga creata al bisogno
                }
            } catch (e: Exception) {
                android.util.Log.d("TODAY_DEBUG", "🌊 ❌ ERRORE check sessione esistente: ${e.message}")
            }
        }
        
        // ✅ Ora ottieni il Flow che osserva la sessione con items
        android.util.Log.d("TODAY_DEBUG", "🌊 Chiamando dailySessionRelationDao.getSessionWithItemsForDate...")
        dailySessionRelationDao.getSessionWithItemsForDate(startOfDay, endOfDay).collect { sessionWithItems ->
            android.util.Log.d("TODAY_DEBUG", "🌊 Flow emitted: $sessionWithItems")
            if (sessionWithItems != null) {
                android.util.Log.d("TODAY_DEBUG", "🌊 Sessione trovata: ID=${sessionWithItems.session.sessionId}, items=${sessionWithItems.items.size}")
            } else {
                android.util.Log.d("TODAY_DEBUG", "🌊 Nessuna sessione trovata per questo range di date")
            }
            emit(sessionWithItems)
        }
    }
    
    /**
     * Ottieni elementi della sessione con dettagli
     */
    fun getSessionItemsWithDetails(sessionId: Long): Flow<List<DailySessionItemWithDetails>> {
        return dailySessionRelationDao.getSessionItemsWithDetailsFlow(sessionId)
    }
    
    // ============================================================================
    // AGGIUNGERE ESERCIZI E ALLENAMENTI ALLA SESSIONE
    // ============================================================================
    
    /**
     * Aggiungi esercizio alla sessione odierna
     */
    @Transaction
    suspend fun addExerciseToTodaySession(context: Context, exerciseId: Long, customRepsParam: Int? = null, customTimeParam: Int? = null): DailySessionItem? {
        android.util.Log.d("TODAY_DEBUG", "🔧 addExerciseToTodaySession() chiamato con exerciseId: $exerciseId, customReps: $customRepsParam, customTime: $customTimeParam")

        val session = getTodaySession()
        android.util.Log.d("TODAY_DEBUG", "🔧 Sessione ottenuta: ${session.sessionId}")

        var exercise = exerciseDao.getExerciseById(exerciseId)
        android.util.Log.d("TODAY_DEBUG", "🔧 Esercizio trovato nel DB: $exercise")

        // ✅ Se l'esercizio non esiste, crealo dal template JSON
        if (exercise == null) {
            android.util.Log.d("TODAY_DEBUG", "⚡ Esercizio non trovato - creazione dal template ID: $exerciseId")
            exercise = createExerciseFromTemplate(context, exerciseId)
            android.util.Log.d("TODAY_DEBUG", "✅ Esercizio creato dal template: $exercise")
        }

        if (exercise == null) {
            android.util.Log.d("TODAY_DEBUG", "❌ Impossibile creare esercizio per ID: $exerciseId")
            return null
        }

        // ✅ Usa parametri personalizzati se forniti, altrimenti valori default dal template JSON
        val template = com.programminghut.pose_detection.util.ExerciseTemplateFileManager.loadExerciseTemplateById(context, exerciseId)
        val customReps = customRepsParam ?: if (template?.mode == TemplateExerciseMode.REPS) template.defaultReps else null
        val customTime = customTimeParam ?: if (template?.mode == TemplateExerciseMode.TIME) template.defaultTime else null
        android.util.Log.d("TODAY_DEBUG", "🔧 Final values - reps: $customReps, time: $customTime (template: ${template?.defaultReps}/${template?.defaultTime})")
        
        // Trova prossimo ordine
        val currentItems = dailySessionDao.getSessionItems(session.sessionId)
        val nextOrder = (currentItems.maxOfOrNull { it.order } ?: -1) + 1
        android.util.Log.d("TODAY_DEBUG", "🔧 Prossimo ordine: $nextOrder, items esistenti: ${currentItems.size}")
        
        // Usa l'ID dell'esercizio nel DB (exercise.exerciseId), non l'ID del template JSON
        val item = DailySessionItem(
            sessionId = session.sessionId,
            order = nextOrder,
            itemType = SessionItemType.EXERCISE,
            exerciseId = exercise.exerciseId,
            workoutId = null,
            customReps = customReps,
            customTime = customTime
        )
        android.util.Log.d("TODAY_DEBUG", "🔧 Item creato: $item")
        
        val itemId = dailySessionDao.insertSessionItem(item)
        android.util.Log.d("TODAY_DEBUG", "🔧 Item inserito con ID: $itemId")
        
        val finalItem = item.copy(itemId = itemId)
        android.util.Log.d("TODAY_DEBUG", "✅ Item finale: $finalItem")
        
        return finalItem
    }
    
    /**
     * ✅ Aggiungi Squat AI alla sessione odierna
     */
    @Transaction
    suspend fun addAISquatToTodaySession(context: Context, targetReps: Int = 0): DailySessionItem? {
        android.util.Log.d("BRIDGE_DEBUG", "🚀🚀🚀 === INIZIO addAISquatToTodaySession ===")
        android.util.Log.d("BRIDGE_DEBUG", "🤖 Parametri: targetReps=$targetReps, context=$context")
        try {
            // ✅ Debug: verifica data di oggi vs sessione esistente
            val todayMillis = System.currentTimeMillis()
            val todayFormatted = formatDate(todayMillis)
            android.util.Log.d("BRIDGE_DEBUG", "🕒 OGGI timestamp: $todayMillis ($todayFormatted)")
            
            // 🎯 CRITICAL FIX: Usa ESATTAMENTE la stessa logica dell'UI per ottenere la sessione
            // Questo forza l'uso della stessa sessione che vede l'UI Today screen
            val calendar = Calendar.getInstance().apply { timeInMillis = todayMillis }
            val startOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val endOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            android.util.Log.d("BRIDGE_DEBUG", "� Range UI calcolato: startOfDay=$startOfDay, endOfDay=$endOfDay")
            
            // 🎯 Prima assicurati che esista una sessione (come fa l'UI)
            if (isDateToday(todayMillis)) {
                try {
                    getSessionForDate(todayMillis)
                    android.util.Log.d("BRIDGE_DEBUG", "✅ Sessione per oggi verificata")
                } catch (e: Exception) {
                    android.util.Log.d("BRIDGE_DEBUG", "❌ ERRORE verifica sessione: ${e.message}")
                }
            }
            
            // 🎯 Usa DIRETTAMENTE il metodo getSessionForDate dell'UI
            android.util.Log.d("BRIDGE_DEBUG", "📡 Usando getSessionForDate() per ottenere la sessione dell'UI...")
            val session = try {
                getSessionForDate(todayMillis)
            } catch (e: Exception) {
                android.util.Log.d("BRIDGE_DEBUG", "❌ ERRORE getSessionForDate: ${e.message}")
                null
            }
            
            android.util.Log.d("BRIDGE_DEBUG", "📡 Sessione ottenuta: ${session?.sessionId}")
            
            if (session == null) {
                android.util.Log.d("BRIDGE_DEBUG", "❌ Nessuna sessione trovata - ERRORE CRITICO!")
                return null
            }
            
            android.util.Log.d("BRIDGE_DEBUG", "🔧 Sessione FINALE (stessa dell'UI): ID=${session.sessionId}, data=${session.date} (${formatDate(session.date)})")
            
            // ✅ Debug: controlla se la sessione è davvero per oggi
            val sessionFormatted = formatDate(session.date)
            android.util.Log.d("BRIDGE_DEBUG", "📅 Controllo date: sessione=$sessionFormatted vs OGGI=$todayFormatted")
        
        // Trova prossimo ordine
        android.util.Log.d("BRIDGE_DEBUG", "🔢 Calcolando prossimo ordine per sessione ${session.sessionId}...")
        val currentItems = try {
            dailySessionDao.getSessionItems(session.sessionId)
        } catch (e: Exception) {
            android.util.Log.d("BRIDGE_DEBUG", "❌ ERRORE getSessionItems: ${e.message}")
            emptyList()
        }
        
        val nextOrder = (currentItems.maxOfOrNull { it.order } ?: -1) + 1
        android.util.Log.d("BRIDGE_DEBUG", "� Ordine calcolato: nextOrder=$nextOrder, items esistenti: ${currentItems.size}")
        
        // ✅ Debug: lista items esistenti
        android.util.Log.d("BRIDGE_DEBUG", "📋 Lista items esistenti nella sessione ${session.sessionId}:")
        if (currentItems.isEmpty()) {
            android.util.Log.d("BRIDGE_DEBUG", "📋   (nessun item esistente)")
        } else {
            currentItems.forEachIndexed { index, item ->
                android.util.Log.d("BRIDGE_DEBUG", "📋   $index: ID=${item.itemId}, order=${item.order}, exerciseId=${item.exerciseId}, aiData=${item.aiData}")
            }
        }
        
        // ✅ Crea item AI Squat (stesso exerciseId=2 del template Squat normale)
        android.util.Log.d("BRIDGE_DEBUG", "🏗️ Creando AI Squat item...")
            // ✅ Crea item AI Squat: trova o crea l'Exercise corrispondente al template "Squat"
            android.util.Log.d("BRIDGE_DEBUG", "🏗️ Creando AI Squat item...")
            var squatExerciseId: Long? = null
            try {
                val squatTemplate = com.programminghut.pose_detection.util.ExerciseTemplateFileManager.loadExerciseTemplateByName(context, "Squat")
                if (squatTemplate != null) {
                    val existing = exerciseDao.getExerciseByName(squatTemplate.name)
                    squatExerciseId = existing?.exerciseId ?: createExerciseFromTemplate(context, squatTemplate.id)?.exerciseId
                }
            } catch (e: Exception) {
                android.util.Log.d("BRIDGE_DEBUG", "⚠️ Errore risoluzione template Squat: ${e.message}")
            }

            val aiSquatItem = DailySessionItem(
                sessionId = session.sessionId,
                order = nextOrder,
                itemType = SessionItemType.EXERCISE,
                exerciseId = squatExerciseId,
                workoutId = null,
                customReps = targetReps,
                customTime = null,
                notes = "AI Squat Detection",
                aiData = "squat_ai"
            )
        android.util.Log.d("BRIDGE_DEBUG", "🏗️ AI Squat item creato: $aiSquatItem")
        
        // ✅ Inserimento nel database
        android.util.Log.d("BRIDGE_DEBUG", "💾 Inserendo AI Squat item nel database...")
        val itemId = try {
            dailySessionDao.insertSessionItem(aiSquatItem)
        } catch (e: Exception) {
            android.util.Log.d("BRIDGE_DEBUG", "❌ ERRORE CRITICO inserimento: ${e.message}")
            android.util.Log.d("BRIDGE_DEBUG", "❌ Stack trace: ${e.stackTraceToString()}")
            return null
        }
        
        android.util.Log.d("BRIDGE_DEBUG", "� AI Squat item inserito con ID: $itemId")
        
        val finalItem = aiSquatItem.copy(itemId = itemId)
        android.util.Log.d("BRIDGE_DEBUG", "✅ Item finale creato: $finalItem")
        
        // ✅ Debug: verifica inserimento immediato
        android.util.Log.d("BRIDGE_DEBUG", "🔍 VERIFICA POST-INSERIMENTO...")
        val updatedItems = try {
            dailySessionDao.getSessionItems(session.sessionId)
        } catch (e: Exception) {
            android.util.Log.d("BRIDGE_DEBUG", "❌ ERRORE verifica post-inserimento: ${e.message}")
            emptyList()
        }
        
        android.util.Log.d("BRIDGE_DEBUG", "🔍 POST-INSERIMENTO: ${updatedItems.size} items nella sessione ${session.sessionId}")
        updatedItems.forEach { item ->
            android.util.Log.d("BRIDGE_DEBUG", "�   ID=${item.itemId}, aiData=${item.aiData}, order=${item.order}")
        }
        
        android.util.Log.d("BRIDGE_DEBUG", "🎉🎉🎉 === FINE addAISquatToTodaySession - SUCCESS ===")
        return finalItem
        
        } catch (e: Exception) {
            android.util.Log.d("BRIDGE_DEBUG", "💥💥💥 ERRORE FATALE in addAISquatToTodaySession: ${e.message}")
            android.util.Log.d("BRIDGE_DEBUG", "💥 Stack trace completo: ${e.stackTraceToString()}")
            return null
        }
    }
    
    /**
     * ✅ Aggiorna AI Squat con conteggio reale dopo la sessione
     */
    @Transaction
    suspend fun updateAISquatWithRealCount(aiSquatItemId: Long, actualReps: Int): Boolean {
        android.util.Log.d("BRIDGE_DEBUG", "🔄🔄🔄 === INIZIO updateAISquatWithRealCount ===")
        android.util.Log.d("BRIDGE_DEBUG", "📊 Parametri: itemId=$aiSquatItemId, actualReps=$actualReps")
        
        try {
            // Ottieni l'item AI Squat esistente
            val existingItem = dailySessionDao.getSessionItemById(aiSquatItemId)
            if (existingItem == null) {
                android.util.Log.d("BRIDGE_DEBUG", "❌ Item AI Squat non trovato con ID: $aiSquatItemId")
                return false
            }
            
            // Verifica che sia effettivamente un AI Squat
            if (existingItem.aiData?.contains("squat_ai") != true) {
                android.util.Log.d("BRIDGE_DEBUG", "❌ Item non è un AI Squat: aiData=${existingItem.aiData}")
                return false
            }
            
            // Aggiorna con valori reali
            val updatedItem = existingItem.copy(
                customReps = actualReps,           // ✅ Reps reali dal conteggio AI
                actualReps = actualReps,           // ✅ Stesso valore in actualReps
                isCompleted = true,                // ✅ Marca come completato
                completedAt = System.currentTimeMillis(),
                notes = "AI Squat Detection - $actualReps reps completati"
            )
            
            android.util.Log.d("BRIDGE_DEBUG", "🔄 Aggiornando item: $updatedItem")
            
            // Aggiorna nel database
            val updateCount = dailySessionDao.updateSessionItem(updatedItem)
            android.util.Log.d("BRIDGE_DEBUG", "💾 Update result: $updateCount righe aggiornate")
            
            android.util.Log.d("BRIDGE_DEBUG", "🎉🎉🎉 === FINE updateAISquatWithRealCount - SUCCESS ===")
            return updateCount > 0
            
        } catch (e: Exception) {
            android.util.Log.d("BRIDGE_DEBUG", "💥💥💥 ERRORE FATALE in updateAISquatWithRealCount: ${e.message}")
            android.util.Log.d("BRIDGE_DEBUG", "💥 Stack trace: ${e.stackTraceToString()}")
            return false
        }
    }
    
    /**
     * Crea un esercizio nel database partendo dal template JSON
     */
    private suspend fun createExerciseFromTemplate(context: Context, templateId: Long): Exercise? {
        android.util.Log.d("TODAY_DEBUG", "🏗️ createExerciseFromTemplate() iniziato per ID: $templateId")

        // Carica template dall'asset usando il file manager
        val template = com.programminghut.pose_detection.util.ExerciseTemplateFileManager.loadExerciseTemplateById(context, templateId)
        if (template == null) {
            android.util.Log.d("TODAY_DEBUG", "❌ Template esercizio non trovato per ID: $templateId")
            return null
        }
        android.util.Log.d("TODAY_DEBUG", "✅ Template esercizio trovato: ${template.name}")

        val exercise = Exercise(
            exerciseId = 0, // Auto-generate
            name = template.name,
            type = ExerciseType.CUSTOM, // Default per ora
            description = template.description ?: "",
            mode = when (template.mode) {
                TemplateExerciseMode.TIME -> ExerciseMode.TIME
                TemplateExerciseMode.REPS -> ExerciseMode.REPS
                else -> ExerciseMode.REPS
            },
            imagePath = null,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            isCustom = false,
            tags = emptyList()
        )
        android.util.Log.d("TODAY_DEBUG", "🔧 Exercise object creato: ${exercise.name}")

        // Inserisci nel database
        val insertedId = try {
            exerciseDao.insertExercise(exercise)
        } catch (e: Exception) {
            android.util.Log.d("TODAY_DEBUG", "❌ Errore inserimento exercise: ${e.message}")
            return null
        }

        val result = exercise.copy(exerciseId = insertedId)
        android.util.Log.d("TODAY_DEBUG", "✅ Exercise inserito con ID: $insertedId")
        return result
    }
    
    // NOTE: Eliminato helper hardcoded. I template vengono caricati da JSON in assets tramite ExerciseTemplateFileManager.
    
    /**
     * Crea un workout nel database partendo dal template JSON
     */
    private suspend fun createWorkoutFromTemplate(context: Context, templateId: Long): WorkoutWithExercises? {
        android.util.Log.d("TODAY_DEBUG", "🏗️ createWorkoutFromTemplate() iniziato per ID: $templateId")
        
        // Ottieni il template dal file JSON
        val template = WorkoutTemplateFileManager.loadWorkoutTemplateById(context, templateId)
        if (template == null) {
            android.util.Log.d("TODAY_DEBUG", "❌ Template workout non trovato per ID: $templateId")
            return null
        }
        android.util.Log.d("TODAY_DEBUG", "✅ Template workout trovato: ${template.name}")
        
        val workout = Workout(
            workoutId = 0, // Auto-generate
            name = template.name,
            description = template.description,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            isCustom = false,
            imagePath = null
        )
        android.util.Log.d("TODAY_DEBUG", "🔧 Workout object creato: ${workout.name}")
        
        // Inserisci nel database
        val insertedId = try {
            workoutDao.insertWorkout(workout)
        } catch (e: Exception) {
            android.util.Log.d("TODAY_DEBUG", "❌ Errore inserimento workout: ${e.message}")
            return null
        }
        android.util.Log.d("TODAY_DEBUG", "✅ Workout inserito con ID: $insertedId")
        
        val createdWorkout = workout.copy(workoutId = insertedId)
        
        // Aggiungi gli esercizi del template
        val workoutExercises = mutableListOf<WorkoutExercise>()
        android.util.Log.d("TODAY_DEBUG", "🔧 Processando ${template.exercises.size} esercizi del template")
        
        template.exercises.forEach { exerciseTemplate ->
            android.util.Log.d("TODAY_DEBUG", "🔧 Processando esercizio template: exerciseId=${exerciseTemplate.exerciseId}")
            
            // Assicurati che l'esercizio esista
            var exercise = try {
                exerciseDao.getExerciseById(exerciseTemplate.exerciseId)
            } catch (e: Exception) {
                android.util.Log.d("TODAY_DEBUG", "❌ Errore query esercizio: ${e.message}")
                null
            }
            
            if (exercise == null) {
                android.util.Log.d("TODAY_DEBUG", "⚡ Creando esercizio dal template: ${exerciseTemplate.exerciseId}")
                    exercise = try {
                    createExerciseFromTemplate(context, exerciseTemplate.exerciseId)
                } catch (e: Exception) {
                    android.util.Log.d("TODAY_DEBUG", "❌ Errore creazione esercizio: ${e.message}")
                    null
                }
            }
            
            if (exercise != null) {
                android.util.Log.d("TODAY_DEBUG", "✅ Esercizio pronto: ${exercise.name}")
                val workoutExercise = WorkoutExercise(
                    workoutId = insertedId,
                    exerciseId = exercise.exerciseId,
                    orderIndex = exerciseTemplate.orderIndex,
                    targetReps = exerciseTemplate.targetReps,
                    targetSets = 1, // Default
                    targetTime = exerciseTemplate.targetTime,
                    restTime = exerciseTemplate.restTime,
                    notes = ""
                )
                workoutExercises.add(workoutExercise)
                android.util.Log.d("TODAY_DEBUG", "➕ WorkoutExercise aggiunto alla lista")
            } else {
                android.util.Log.d("TODAY_DEBUG", "❌ Impossibile creare/ottenere esercizio ${exerciseTemplate.exerciseId}")
            }
        }
        
        // Inserisci tutti gli esercizi in batch
        if (workoutExercises.isNotEmpty()) {
            android.util.Log.d("TODAY_DEBUG", "💾 Inserendo ${workoutExercises.size} workout exercises")
            try {
                workoutDao.insertWorkoutExercises(workoutExercises)
                android.util.Log.d("TODAY_DEBUG", "✅ Workout exercises inseriti con successo")
            } catch (e: Exception) {
                android.util.Log.d("TODAY_DEBUG", "❌ Errore inserimento workout exercises: ${e.message}")
                return null
            }
        } else {
            android.util.Log.d("TODAY_DEBUG", "⚠️ Nessun workout exercise da inserire")
        }
        
        val result = WorkoutWithExercises(
            workout = createdWorkout,
            exercises = emptyList() // Non serve per questo uso
        )
        android.util.Log.d("TODAY_DEBUG", "🎉 createWorkoutFromTemplate completato con successo")
        return result
    }
    
    /**
     * Aggiungi allenamento alla sessione odierna
     */
    
    /**
     * Aggiungi allenamento alla sessione odierna
     */
    @Transaction
    suspend fun addWorkoutToTodaySession(context: Context, workoutId: Long): List<DailySessionItem> {
        android.util.Log.d("TODAY_DEBUG", "🔧 addWorkoutToTodaySession() chiamato con workoutId: $workoutId")
        
        try {
            val session = getTodaySession()
            android.util.Log.d("TODAY_DEBUG", "🔧 Sessione ottenuta: ${session.sessionId}")
            
            var workout = try {
                workoutDao.getWorkoutWithExercises(workoutId)
            } catch (e: Exception) {
                android.util.Log.d("TODAY_DEBUG", "❌ Errore query workout: ${e.message}")
                null
            }
            android.util.Log.d("TODAY_DEBUG", "🔧 Workout trovato nel DB: $workout")
            
            // ✅ Se il workout non esiste, crealo dal template
            if (workout == null) {
                android.util.Log.d("TODAY_DEBUG", "⚡ Workout non trovato - creazione dal template ID: $workoutId")
                workout = try {
                    createWorkoutFromTemplate(context, workoutId)
                } catch (e: Exception) {
                    android.util.Log.d("TODAY_DEBUG", "❌ Errore creazione workout dal template: ${e.message}")
                    null
                }
                android.util.Log.d("TODAY_DEBUG", "✅ Workout creato dal template: $workout")
            }
            
            if (workout == null) {
                android.util.Log.d("TODAY_DEBUG", "❌ Impossibile creare workout per ID: $workoutId")
                return emptyList()
            }
            
            // Trova prossimo ordine
            val currentItems = dailySessionDao.getSessionItems(session.sessionId)
            var nextOrder = (currentItems.maxOfOrNull { it.order } ?: -1) + 1
            android.util.Log.d("TODAY_DEBUG", "🔧 Prossimo ordine: $nextOrder, items esistenti: ${currentItems.size}")
            
            val items = mutableListOf<DailySessionItem>()
            
            // Aggiungi l'allenamento come elemento wrapper
            val workoutItem = DailySessionItem(
                sessionId = session.sessionId,
                order = nextOrder++,
                itemType = SessionItemType.WORKOUT,
                exerciseId = null,
                workoutId = workout.workout.workoutId
            )
            android.util.Log.d("TODAY_DEBUG", "🔧 WorkoutItem creato: $workoutItem")
            
            val workoutItemId = try {
                dailySessionDao.insertSessionItem(workoutItem)
            } catch (e: Exception) {
                android.util.Log.d("TODAY_DEBUG", "❌ Errore inserimento workout item: ${e.message}")
                return emptyList()
            }
            items.add(workoutItem.copy(itemId = workoutItemId))
            android.util.Log.d("TODAY_DEBUG", "🔧 WorkoutItem inserito con ID: $workoutItemId")
            
            // Aggiungi gli esercizi dell'allenamento usando la junction table
            val workoutExercises = try {
                workoutDao.getWorkoutExercises(workout.workout.workoutId)
            } catch (e: Exception) {
                android.util.Log.d("TODAY_DEBUG", "❌ Errore query workout exercises: ${e.message}")
                emptyList()
            }
            android.util.Log.d("TODAY_DEBUG", "🔧 Workout exercises: ${workoutExercises.size}")
            
            workoutExercises.sortedBy { it.orderIndex }.forEach { we ->
                try {
                    var exercise = exerciseDao.getExerciseById(we.exerciseId)
                    
                    // ✅ Se l'esercizio non esiste, crealo dal template
                    if (exercise == null) {
                        android.util.Log.d("TODAY_DEBUG", "⚡ Esercizio ${we.exerciseId} non trovato - creazione dal template")
                        exercise = createExerciseFromTemplate(context, we.exerciseId)
                    }
                    
                    if (exercise == null) {
                        android.util.Log.d("TODAY_DEBUG", "❌ Saltando esercizio ${we.exerciseId} - impossibile creare")
                        return@forEach
                    }

                    val exerciseItem = DailySessionItem(
                        sessionId = session.sessionId,
                        order = nextOrder++,
                        itemType = SessionItemType.EXERCISE,
                        exerciseId = exercise.exerciseId,
                        workoutId = null,
                        customReps = we.targetReps,
                        customTime = we.targetTime,
                        parentWorkoutItemId = workoutItemId
                    )

                    val exerciseItemId = dailySessionDao.insertSessionItem(exerciseItem)
                    items.add(exerciseItem.copy(itemId = exerciseItemId))
                    android.util.Log.d("TODAY_DEBUG", "🔧 ExerciseItem inserito: ${exercise.name} con ID: $exerciseItemId")
                } catch (e: Exception) {
                    android.util.Log.d("TODAY_DEBUG", "❌ Errore processamento esercizio ${we.exerciseId}: ${e.message}")
                }
            }
            
            android.util.Log.d("TODAY_DEBUG", "✅ Workout aggiunto alla sessione: ${items.size} items totali")
            return items
            
        } catch (e: Exception) {
            android.util.Log.d("TODAY_DEBUG", "❌ ERRORE CRITICO addWorkoutToTodaySession: ${e.message}")
            android.util.Log.d("TODAY_DEBUG", "❌ Stack trace: ${e.stackTraceToString()}")
            return emptyList()
        }
    }
    
    /**
     * Crea sessione da template allenamento
     */
    @Transaction
    suspend fun createSessionFromWorkout(context: Context, workoutId: Long, sessionName: String? = null): DailySession {
        val workout = workoutDao.getWorkoutWithExercises(workoutId)
            ?: throw IllegalArgumentException("Workout not found: $workoutId")
        
        val now = System.currentTimeMillis()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val session = DailySession(
            name = sessionName ?: "Allenamento: ${workout.workout.name}",
            date = today,
            createdAt = now
        )
        
        val sessionId = dailySessionDao.insertDailySession(session)
        val createdSession = session.copy(sessionId = sessionId)
        
        // Aggiungi tutti gli esercizi dell'allenamento
        addWorkoutToTodaySession(context, workoutId)
        
        return createdSession
    }
    
    // ============================================================================
    // GESTIONE COMPLETAMENTO
    // ============================================================================
    
    /**
     * Aggiorna completamento elemento
     */
    suspend fun updateItemCompletion(
        itemId: Long,
        isCompleted: Boolean,
        actualReps: Int? = null,
        actualTime: Int? = null,
        notes: String = ""
    ) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        dailySessionDao.updateItemCompletion(
            itemId = itemId,
            isCompleted = isCompleted,
            actualReps = actualReps,
            actualTime = actualTime,
            completedAt = completedAt,
            notes = notes
        )
    }
    
    /**
     * Avvia sessione di allenamento
     */
    suspend fun startSession(sessionId: Long) {
        val session = dailySessionDao.getSessionForDate(0L, Long.MAX_VALUE) // Trova la sessione
        session?.let {
            val updated = it.copy(startTime = System.currentTimeMillis())
            dailySessionDao.updateSession(updated)
        }
    }
    
    /**
     * Completa sessione di allenamento
     */
    suspend fun completeSession(sessionId: Long) {
        val session = dailySessionDao.getSessionForDate(0L, Long.MAX_VALUE) // Trova la sessione
        session?.let {
            val now = System.currentTimeMillis()
            val updated = it.copy(
                endTime = now,
                isCompleted = true
            )
            dailySessionDao.updateSession(updated)
        }
    }
    
    // ============================================================================
    // STATISTICHE E CRONOLOGIA
    // ============================================================================
    
    /**
     * Ottieni cronologia sessioni
     */
    fun getSessionsHistory(): Flow<List<DailySessionSummary>> {
        return dailySessionRelationDao.getSessionsHistoryFlow()
    }
    
    /**
     * Ottieni statistiche esercizi per periodo
     */
    suspend fun getExerciseStats(days: Int = 30): List<ExerciseStats> {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (days * 24 * 60 * 60 * 1000L)
        return dailySessionRelationDao.getExerciseStats(startDate, endDate)
    }
    
    /**
     * Ottieni statistiche allenamenti per periodo
     */
    suspend fun getWorkoutStats(days: Int = 30): List<WorkoutStats> {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (days * 24 * 60 * 60 * 1000L)
        return dailySessionRelationDao.getWorkoutStats(startDate, endDate)
    }
    
    // ============================================================================
    // UTILITY
    // ============================================================================
    
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
    }
    
    /**
     * ✅ Verifica se una data è oggi
     */
    private fun isDateToday(dateMillis: Long): Boolean {
        val targetDate = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return targetDate.timeInMillis == today.timeInMillis
    }
    
    /**
     * Elimina elemento dalla sessione
     */
    suspend fun removeItemFromSession(itemId: Long) {
        android.util.Log.d("TODAY_DEBUG", "🗑️ Repository.removeItemFromSession chiamato per itemId: $itemId")
        
        // Prima controlliamo se l'item da rimuovere è uno squat o un workout con squat
    val itemToRemove = dailySessionDao.getSessionItemById(itemId)
        
        android.util.Log.d("TODAY_DEBUG", "🔍 Item da rimuovere: exerciseId=${itemToRemove?.exerciseId}, workoutId=${itemToRemove?.workoutId}")
        
        // Se è un workout, raccogli gli exerciseId dei figli per eventuale invalidazione cache
        val affectedExerciseIds = mutableSetOf<Long>()
        if (itemToRemove?.exerciseId != null) affectedExerciseIds.add(itemToRemove.exerciseId!!)
        if (itemToRemove?.workoutId != null) {
            val childItems = dailySessionDao.getItemsByParentWorkout(itemId)
            childItems.forEach { it.exerciseId?.let { id -> affectedExerciseIds.add(id) } }
            android.util.Log.d("TODAY_DEBUG", "🏋️ È un workout con ${childItems.size} esercizi figli. Affected exerciseIds: $affectedExerciseIds")
        }
        
        // Elimina l'item (e automaticamente tutti i figli se è un workout)
        dailySessionDao.deleteSessionItem(itemId)
        
        android.util.Log.d("TODAY_DEBUG", "✅ Item eliminato dal database. ID: $itemId")
        
        // Forza l'invalidazione della cache per tutti gli exerciseId impactati
        if (affectedExerciseIds.isNotEmpty()) {
            android.util.Log.d("TODAY_DEBUG", "🔄 Forzando invalidazione cache per exerciseIds: $affectedExerciseIds")
            try {
                affectedExerciseIds.forEach { id ->
                    dailySessionDao.invalidateCountCacheForExercise(id)
                }
            } catch (e: Exception) {
                android.util.Log.d("TODAY_DEBUG", "⚠️ Errore invalidazione cache: ${e.message}")
            }
        }
    }
    
    /**
     * Riordina elementi nella sessione
     */
    @Transaction
    suspend fun reorderSessionItems(sessionId: Long, itemOrders: List<Pair<Long, Int>>) {
        // Implementazione per riordinare gli elementi
        // Questo richiede update specifiche per ogni elemento
    }
    
    // NOTE: getTotalSquatsCount removed. Use getTotalCountForTemplate/getTotalCountByTemplateName instead.

    /**
     * Ottieni conteggio totale per un esercizio dato il template ID (JSON)
     */
    fun getTotalCountForTemplate(context: android.content.Context, templateId: Long): Flow<Int> {
        val template = com.programminghut.pose_detection.util.ExerciseTemplateFileManager.loadExerciseTemplateById(context, templateId)
        return if (template != null) {
            // Trova l'esercizio nel DB per nome
            kotlinx.coroutines.flow.flow {
                val exercise = exerciseDao.getExerciseByName(template.name)
                if (exercise != null) {
                    dailySessionDao.getTotalCountForExercise(exercise.exerciseId).collect { value -> emit(value) }
                } else {
                    emit(0)
                }
            }
        } else kotlinx.coroutines.flow.flowOf(0)
    }

    /**
     * Ottieni conteggio totale per esercizio dato il nome del template JSON
     */
    fun getTotalCountByTemplateName(context: android.content.Context, templateName: String): Flow<Int> {
        val template = com.programminghut.pose_detection.util.ExerciseTemplateFileManager.loadExerciseTemplates(context).find { it.name == templateName }
        return if (template != null) getTotalCountForTemplate(context, template.id) else kotlinx.coroutines.flow.flowOf(0)
}

// End of DailySessionRepository
}