# 🔍 Analisi Architettura App - Duplicazioni e Confusioni

**Data:** 19 gennaio 2026  
**Obiettivo:** Identificare duplicazioni, confusioni e opportunità di semplificazione

---

## 🚨 PROBLEMA PRINCIPALE: Due Sistemi di Esercizi Paralleli

### Sistema 1: `Exercise` (Database Room)
**Location:** `data/model/Exercise.kt`
- Tabella Room: `exercises`
- Campi: `exerciseId`, `name`, `type`, `description`, `mode`, `imagePath`, etc.
- DAO: `ExerciseDao`
- **Uso:** Preselezione esercizi in `ExerciseSelectorActivity` (DISABILITATO)
- **Stato:** ❌ **PARZIALMENTE INUTILIZZATO**

### Sistema 2: `ExerciseTemplate` (JSON Assets)
**Location:** `data/model/CleanArchitecture.kt`
- File JSON: `app/src/main/assets/exercise_templates/*.json`
- Campi: `id`, `name`, `type`, `mode`, `description`, `defaultReps`, `defaultTime`, etc.
- Manager: `ExerciseTemplateFileManager`
- **Uso:** Caricamento esercizi in `NewMainActivity`, liste UI
- **Stato:** ✅ **USATO ATTIVAMENTE**

---

## 📊 Mappatura Uso Effettivo

### ✅ Cosa Viene Usato

1. **ExerciseTemplate (JSON)**
   - `ExerciseTemplateFileManager.loadExerciseTemplates()` → NewMainActivity
   - Mostra lista esercizi nella UI Today
   - 90 esercizi caricati da JSON (7 originali + 83 nuovi)
   - **Ordinamento:** Squat sempre per primo ✅

2. **DailySessionItem (Tracking)**
   - Salva ripetizioni/tempo reali eseguiti
   - Tabella `daily_session_items`
   - Collegato ai template via `templateId`

3. **WorkoutTemplate (JSON)**
   - File: `app/src/main/assets/workout_templates/*.json`
   - Manager: `WorkoutTemplateFileManager`
   - Circuiti predefiniti con sequenze esercizi

### ❌ Cosa NON Viene Usato

1. **Exercise (Database)**
   - `ExerciseDao.getAllExercises()` → Query modificata ma non chiamata
   - `ExercisePresetManager.kt.disabled` → File disabilitato
   - `ExerciseSelectorActivity` → Usa il DAO ma filtra solo SQUAT
   - **Problema:** Tabella `exercises` vuota o poco popolata

2. **ExerciseRepository**
   - Wrapper sopra ExerciseDao
   - `getAllExercises()`, `insertExercise()` → chiamati raramente
   - DailySessionRepository lo usa solo per creare Exercise da Template

---

## 🔄 Flusso Attuale (Confuso)

### Quando Aggiungi Esercizio nella Today View:

```
1. UI richiede lista esercizi
   ↓
2. ExerciseTemplateFileManager.loadExerciseTemplates(context)
   ↓
3. Carica 90 JSON da assets/exercise_templates/
   ↓
4. Mostra lista ordinata (Squat per primo)
   ↓
5. User seleziona template (es. "Push-up")
   ↓
6. DailySessionRepository.createExerciseFromTemplate()
   ↓
7. Converte ExerciseTemplate → Exercise (DB)
   ↓
8. Inserisce in tabella `exercises` (se non esiste)
   ↓
9. Crea DailySessionItem con templateId
   ↓
10. Salva in `daily_session_items`
```

**Problema:** Passi 7-8 creano duplicazione inutile!

---

## 🎯 DUPLICAZIONI IDENTIFICATE

### 1. **Exercise vs ExerciseTemplate**
- **Due definizioni** della stessa entità
- Exercise ha `exerciseId`, ExerciseTemplate ha `id`
- Exercise salvato in DB, Template in JSON
- **Conversione continua** Template → Exercise

### 2. **ExerciseType vs TemplateExerciseType**
```kotlin
// Exercise.kt
enum class ExerciseType { SQUAT, PUSH_UP, PULL_UP, LUNGE, PLANK, CUSTOM }

// CleanArchitecture.kt  
enum class TemplateExerciseType { STRENGTH, CARDIO, STRETCHING, FLEXIBILITY, BALANCE }
```
**Significati diversi!** Uno è "quale esercizio", l'altro è "categoria"

### 3. **ExerciseMode vs TemplateExerciseMode**
```kotlin
// Exercise.kt
enum class ExerciseMode { REPS, TIME }

// CleanArchitecture.kt
enum class TemplateExerciseMode { REPS, TIME }
```
**Identici!** Duplicazione pura.

### 4. **DAO Mai Usato Per Ordinamento**
- Modificato `ExerciseDao.getAllExercises()` con ORDER BY
- Ma la query non viene mai chiamata nella UI principale
- Ordinamento fatto in `ExerciseTemplateFileManager` ✅

---

## 🏗️ ARCHITETTURA CORRETTA (Semplificata)

### Proposta 1: **Eliminare Exercise dal DB**

```
ExerciseTemplate (JSON) → DailySessionItem (DB tracking)
```

**Vantaggi:**
- ✅ Unica sorgente di verità (JSON)
- ✅ No conversioni Template → Exercise
- ✅ Più semplice da mantenere
- ✅ JSON facili da aggiornare

**Svantaggi:**
- ❌ Esercizi custom devono andare in JSON o serve altro meccanismo

### Proposta 2: **Migrare Template in Database**

```
Exercise (DB) ← Inizializza da JSON al primo avvio
```

**Vantaggi:**
- ✅ Query SQL potenti
- ✅ Esercizi custom nello stesso posto
- ✅ Ordinamenti dinamici via DAO

**Svantaggi:**
- ❌ Più complesso gestire aggiornamenti template
- ❌ Migration necessaria

### Proposta 3: **Ibrido (Attuale, ma semplificato)**

```
ExerciseTemplate (JSON) → Sola lettura, preset
Exercise (DB) → Solo custom creati dall'utente
DailySessionItem → Usa templateId per riferirsi ai preset
```

**Vantaggi:**
- ✅ Preset immutabili in JSON
- ✅ Custom flessibili in DB
- ✅ No duplicazione per preset

**Implementazione:**
- `DailySessionItem.templateId` punta sempre al JSON
- `Exercise` tabella contiene SOLO esercizi custom (isCustom=true)
- UI carica `loadedExercises` da JSON per mostrare lista

---

## ✅ RACCOMANDAZIONI IMMEDIATE

### 1. **Eliminare Confusione ExerciseType**
```kotlin
// Rinominare per chiarezza
TemplateExerciseType → ExerciseCategory (STRENGTH, CARDIO, etc.)
ExerciseType → ExerciseName (SQUAT, PUSH_UP, etc.) o eliminare
```

### 2. **Unificare ExerciseMode**
```kotlin
// Usare solo uno
typealias TemplateExerciseMode = ExerciseMode
```

### 3. **Documentare Flusso Template**
Aggiungere commento in `DailySessionRepository`:
```kotlin
/**
 * IMPORTANTE: templateId si riferisce a ExerciseTemplate.id (da JSON)
 * NON a Exercise.exerciseId (DB Room)
 */
```

### 4. **Rimuovere Codice Morto**
- `ExercisePresetManager.kt.disabled` → Eliminare file
- `ExerciseSelectorActivity` → Semplificare o rimuovere filtro SQUAT only

### 5. **Consolidare Ordinamento**
- ✅ Già fatto in `ExerciseTemplateFileManager.loadExerciseTemplates()`
- ❌ Rimuovere modifica a `ExerciseDao.getAllExercises()` (inutilizzata)

---

## 📈 STATO ATTUALE

### Tabelle Database Utilizzate
```
✅ daily_sessions          - Sessioni giornaliere
✅ daily_session_items     - Esercizi/workout eseguiti
✅ wellness_trackers       - Tracciamento benessere
✅ workout_templates       - Template allenamenti (se in DB)
⚠️ exercises               - Solo per esercizi custom (poco usata)
❌ workout_sessions        - Sistema legacy (deprecato)
```

### File Manager Attivi
```
✅ ExerciseTemplateFileManager    - Carica 90 esercizi da JSON
✅ WorkoutTemplateFileManager     - Carica circuiti da JSON  
✅ WellnessTrackerFileManager     - Carica tracker benessere
❌ ExercisePresetManager.disabled - Disabilitato
```

---

## 🎯 PROSSIMI STEP

### Priorità Alta
1. ✅ **Ordinamento Squat** → Completato in ExerciseTemplateFileManager
2. 🔄 **Documentare dualità Template vs Exercise** → In corso (questo doc)
3. ⏳ **Rimuovere ExercisePresetManager.disabled**

### Priorità Media
4. ⏳ **Rinominare enum per chiarezza**
5. ⏳ **Consolidare ExerciseMode duplicato**

### Priorità Bassa
6. ⏳ **Considerare migrazione completa a solo Template**
7. ⏳ **Semplificare ExerciseSelectorActivity**

---

## 💡 CONCLUSIONE

**La tua intuizione era corretta!** 

L'app ha **due sistemi paralleli** per gestire gli esercizi:
- ✅ **Template JSON** (usato attivamente)
- ❌ **Database Exercise** (quasi inutilizzato)

Questo crea confusione perché:
1. Modifiche al DAO non hanno effetto (come l'ordinamento)
2. Enum duplicati con nomi simili
3. Conversioni continue Template → Exercise → Template

**Soluzione adottata:** Ordinamento in `ExerciseTemplateFileManager` ✅

**Next step consigliato:** Decidere se eliminare tabella `exercises` o consolidare tutto nel DB.
