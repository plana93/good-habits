# 🎯 Piano Migrazione: Solo Template JSON

## ✅ ANALISI COMPLETATA

### Uso Attuale ExerciseDao in DailySessionRepository

1. **findOrCreateExerciseFromTemplate()** (linea 206)
   - Verifica se Exercise esiste nel DB
   - Se no, lo crea da template JSON
   - **SOLUZIONE:** Eliminare completamente, usare solo template.id

2. **getExerciseById()** (linea 240)
   - Recupera Exercise per validazione
   - **SOLUZIONE:** Usare ExerciseTemplateFileManager.loadExerciseTemplateById()

3. **getExerciseByName()** (linea 782, 1068, 1123)
   - Lookup esercizio per nome
   - **SOLUZIONE:** Caricare templates e fare .find { it.name == nome }

4. **getExerciseNameById()** (linea 941)
   - Resolve ID → nome per export/UI
   - **SOLUZIONE:** ExerciseTemplateFileManager.loadExerciseTemplateById()?.name

5. **getExercisesByType(SQUAT)** (linea 1137)
   - Trova ID esercizio squat
   - **SOLUZIONE:** templates.find { it.name == "Squat" }?.id

---

## 🔧 MODIFICHE NECESSARIE

### Step 1: Refactor DailySessionRepository

```kotlin
// RIMUOVERE
private val exerciseDao: ExerciseDao

// AGGIUNGERE funzioni helper
private fun loadTemplateById(context: Context, id: Long): ExerciseTemplate? {
    return ExerciseTemplateFileManager.loadExerciseTemplateById(context, id)
}

private fun loadTemplateByName(context: Context, name: String): ExerciseTemplate? {
    return ExerciseTemplateFileManager.loadExerciseTemplateByName(context, name)
}
```

### Step 2: Sostituire Chiamate

**PRIMA:**
```kotlin
val exercise = exerciseDao.getExerciseById(templateId)
if (exercise == null) return null
```

**DOPO:**
```kotlin
val template = loadTemplateById(context, templateId)
if (template == null) return null
```

### Step 3: Rimuovere createExerciseFromTemplate()

Questa funzione (linea 470-512) crea Exercise nel DB.
**ELIMINARE COMPLETAMENTE** - non serve più!

### Step 4: Semplificare addExerciseToToday()

**PRIMA:**
```kotlin
val foundExerciseId = findOrCreateExerciseFromTemplate(context, exerciseId)
val exercise = exerciseDao.getExerciseById(foundExerciseId)
val template = ExerciseTemplateFileManager.loadExerciseTemplateById(context, exerciseId)
```

**DOPO:**
```kotlin
val template = loadTemplateById(context, exerciseId)
if (template == null) return null
// Usa direttamente template.id come exerciseId
```

### Step 5: Rimuovere Tabella e DAO

```kotlin
// AppDatabase.kt
@Database(
    entities = [
        // Exercise::class,  // ❌ RIMUOVERE
        DailySession::class,
        // ...
    ],
    version = 11,  // ✅ INCREMENTARE
    exportSchema = false
)
```

---

## ⚠️ PUNTI DI ATTENZIONE

### DailySessionItem.exerciseId

Attualmente punta a:
- `Exercise.exerciseId` (DB) ❌
- Dovrebbe puntare a `ExerciseTemplate.id` (JSON) ✅

**VERIFICA:** Già allineato? 
- Template squat.json ha `"id": 2`
- DailySessionItem salva `exerciseId = 2`
- **SÌ, GIÀ ALLINEATO!** ✅

### Migration Database

```kotlin
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Elimina tabella exercises (non più usata)
        database.execSQL("DROP TABLE IF EXISTS exercises")
        
        // daily_session_items.exerciseId ora punta a template.id (JSON)
        // Nessuna modifica necessaria - già usa template.id
    }
}
```

---

## ✅ VANTAGGI

1. **Semplicità** - Una sola sorgente di verità (JSON)
2. **Manutenibilità** - Aggiornare esercizi = modificare JSON
3. **Performance** - No duplicazione dati DB↔JSON
4. **Chiarezza** - templateId punta sempre a template.id
5. **Spazio** - Tabella DB rimossa

---

## 🚀 IMPLEMENTAZIONE

### Ordine Raccomandato

1. ✅ **Backup** - Commit current state
2. 🔧 **Refactor DailySessionRepository** - Sostituisci exerciseDao con template lookups
3. 🔧 **Aggiorna altri Repository** - SessionRepository se necessario
4. 🗃️ **Migration 10→11** - DROP TABLE exercises
5. 🧪 **Test completo** - Verifica tutte le funzionalità
6. 🧹 **Cleanup** - Rimuovi ExerciseDao, Exercise.kt, ExerciseRepository

### File da Modificare

```
✅ DailySessionRepository.kt      - Sostituisci exerciseDao
✅ SessionRepository.kt            - Verifica dipendenze
✅ AppDatabase.kt                  - Rimuovi Exercise entity, migration
✅ NewMainActivity.kt             - Verifica costruttori
✅ StreakCalendarActivity.kt      - Verifica costruttori
✅ DashboardActivity.kt           - Verifica costruttori
```

### File da Eliminare

```
❌ Exercise.kt                     - Data class non più necessaria
❌ ExerciseDao.kt                  - DAO non più necessario
❌ ExerciseRepository.kt           - Repository non più necessario
❌ ExercisePresetManager.kt.disabled - Già disabilitato
```

---

## 🎯 PROSSIMI PASSI

**Vuoi che proceda con l'implementazione?**

Posso:
1. Fare commit di sicurezza dello stato attuale
2. Implementare il refactor passo-passo
3. Testare ogni step prima di procedere

**Oppure preferisci:**
- Vedere prima solo le modifiche a DailySessionRepository?
- Analizzare altri potenziali problemi?
- Mantenere lo stato attuale (funziona già)?

