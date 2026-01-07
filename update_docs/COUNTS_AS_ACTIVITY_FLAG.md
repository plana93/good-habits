# 🎯 Flag `countsAsActivity` - Documentazione

## 📋 Overview

Il campo `countsAsActivity` è stato aggiunto al modello `DailySessionItem` per identificare quali elementi devono essere contati come **attività fisica effettiva** nel calcolo della streak e nelle statistiche.

**Versione Database**: 8  
**Data Implementazione**: 4 Gennaio 2026

---

## 🔧 Struttura

### Campo Database
```kotlin
val countsAsActivity: Boolean = true  // Default: true
```

### Posizione nel Model
```kotlin
@Entity(tableName = "daily_session_items")
data class DailySessionItem(
    // ... altri campi ...
    
    /**
     * Flag per tracking attività fisica
     * - true: Esercizio o allenamento (conta per streak)
     * - false: Altro tipo di elemento (NON conta per streak)
     */
    val countsAsActivity: Boolean = true
)
```

---

## ✅ Utilizzo

### 1. **Esercizi e Allenamenti** (Default)
Tutti gli esercizi e allenamenti hanno `countsAsActivity = true` di default.

```kotlin
val exerciseItem = DailySessionItem(
    sessionId = sessionId,
    itemType = SessionItemType.EXERCISE,
    exerciseId = 123,
    // countsAsActivity = true (default, conta per streak)
)

val workoutItem = DailySessionItem(
    sessionId = sessionId,
    itemType = SessionItemType.WORKOUT,
    workoutId = 456,
    // countsAsActivity = true (default, conta per streak)
)
```

### 2. **Item che NON Contano** (Futuro)
Per elementi che non devono influenzare la streak:

```kotlin
val noteItem = DailySessionItem(
    sessionId = sessionId,
    itemType = SessionItemType.NOTE,  // Esempio futuro
    notes = "Ricorda di bere acqua",
    countsAsActivity = false  // ❌ NON conta per streak
)

val reminderItem = DailySessionItem(
    sessionId = sessionId,
    itemType = SessionItemType.REMINDER,  // Esempio futuro
    notes = "Prossimo allenamento domani",
    countsAsActivity = false  // ❌ NON conta per streak
)
```

---

## 📊 Impatto sul Sistema

### Query Database (Aggiornate)

#### 1. Riepilogo Giornaliero
```sql
SELECT s.date as date,
       -- Conta SOLO item con countsAsActivity = 1
       COUNT(CASE WHEN i.countsAsActivity = 1 THEN i.itemId END) as itemCount,
       SUM(CASE WHEN i.isCompleted = 1 AND i.countsAsActivity = 1 THEN 1 ELSE 0 END) as completedCount,
       ...
FROM daily_sessions s
LEFT JOIN daily_session_items i ON s.sessionId = i.sessionId
HAVING COUNT(CASE WHEN i.countsAsActivity = 1 THEN i.itemId END) > 0
```

#### 2. Date con Attività
```sql
SELECT DISTINCT date FROM daily_sessions s
JOIN daily_session_items i ON s.sessionId = i.sessionId
WHERE i.countsAsActivity = 1  -- ✅ Filtro aggiunto
```

### Calcolo Streak
La logica di calcolo della streak ora considera solo i giorni con almeno 1 item dove `countsAsActivity = true`:

```kotlin
// Un giorno conta per la streak se:
val hasDailySession = dailySummaries.containsKey(currentDay)  
// dailySummaries contiene SOLO giorni con countsAsActivity=true

if (hasSessions || hasRecoveryByType || hasDailySession) {
    streak++  // ✅ Conta solo se ha attività reali
}
```

---

## 🔄 Migrazione Database

### Versione 7 → 8

```kotlin
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Aggiunge colonna con default TRUE
        // Tutti gli item esistenti saranno contati come attività
        database.execSQL(
            "ALTER TABLE daily_session_items ADD COLUMN countsAsActivity INTEGER NOT NULL DEFAULT 1"
        )
    }
}
```

**Comportamento**:
- Tutti gli item esistenti avranno `countsAsActivity = true`
- Nessuna perdita di dati o streak
- Compatibilità completa con versioni precedenti

---

## 💡 Esempi Pratici

### Scenario 1: Solo Esercizi (Comportamento Corrente)
```kotlin
// Giorno con 3 esercizi
DailySessionItem(exerciseId = 1, countsAsActivity = true)  // ✅ Conta
DailySessionItem(exerciseId = 2, countsAsActivity = true)  // ✅ Conta
DailySessionItem(exerciseId = 3, countsAsActivity = true)  // ✅ Conta

// Streak: +1 giorno ✅
```

### Scenario 2: Mix di Elementi (Futuro)
```kotlin
// Giorno con esercizi + note
DailySessionItem(exerciseId = 1, countsAsActivity = true)   // ✅ Conta
DailySessionItem(notes = "...", countsAsActivity = false)   // ❌ Non conta
DailySessionItem(exerciseId = 2, countsAsActivity = true)   // ✅ Conta

// Streak: +1 giorno ✅ (ha almeno 1 attività)
```

### Scenario 3: Solo Note (Futuro)
```kotlin
// Giorno con solo note/promemoria
DailySessionItem(notes = "...", countsAsActivity = false)  // ❌ Non conta
DailySessionItem(notes = "...", countsAsActivity = false)  // ❌ Non conta

// Streak: NO ❌ (nessuna attività fisica)
```

---

## 🚀 Estensibilità Futura

### Possibili Nuovi Tipi di Item

```kotlin
enum class SessionItemType {
    EXERCISE,       // Esercizio (countsAsActivity = true)
    WORKOUT,        // Allenamento (countsAsActivity = true)
    NOTE,           // Nota (countsAsActivity = false)
    REMINDER,       // Promemoria (countsAsActivity = false)
    ACHIEVEMENT,    // Traguardo (countsAsActivity = false)
    MEASUREMENT     // Misurazione peso/misure (countsAsActivity = false)
}
```

### Implementazione Nuovi Item

```kotlin
// Aggiungere una nota al giorno SENZA influenzare la streak
fun addNoteToSession(sessionId: Long, note: String) {
    val noteItem = DailySessionItem(
        sessionId = sessionId,
        order = getNextOrder(sessionId),
        itemType = SessionItemType.NOTE,
        notes = note,
        countsAsActivity = false  // ❌ Non conta per streak
    )
    dailySessionDao.insertSessionItem(noteItem)
}
```

---

## ✅ Vantaggi

1. **Flessibilità**: Permette di aggiungere nuovi tipi di contenuti senza alterare la logica di streak
2. **Chiarezza**: Separazione esplicita tra "attività fisica" e "altri dati"
3. **Backward Compatible**: Tutti i dati esistenti mantengono il comportamento precedente
4. **Facilità di Query**: Un singolo flag invece di controllare multipli `itemType`
5. **Manutenibilità**: Facile aggiungere nuove funzionalità senza modificare la logica core

---

## 📝 Note Implementative

### Quando Usare `countsAsActivity = true`
- ✅ Esercizi fisici (squat, push-up, plank, ecc.)
- ✅ Allenamenti completi (circuiti, workout)
- ✅ Attività AI (squat AI, pose detection)
- ✅ Qualsiasi attività che contribuisce al fitness

### Quando Usare `countsAsActivity = false`
- ❌ Note testuali
- ❌ Promemoria
- ❌ Traguardi/achievements (già raggiunti)
- ❌ Misurazioni (peso, misure corporee)
- ❌ Foto di progresso
- ❌ Dati nutrizionali

---

## 🔍 Testing

### Verifica Query
```sql
-- Conta SOLO attività fisiche per un giorno
SELECT COUNT(*) 
FROM daily_session_items 
WHERE sessionId = ? AND countsAsActivity = 1;

-- Verifica mix di item
SELECT itemType, countsAsActivity, COUNT(*) as count
FROM daily_session_items 
WHERE sessionId = ?
GROUP BY itemType, countsAsActivity;
```

### Verifica Streak
```kotlin
// Un giorno con solo note NON dovrebbe contare
val dayWithOnlyNotes = createDayWithNotes()
val streak = calculateStreak()
// streak non dovrebbe aumentare

// Un giorno con esercizi + note dovrebbe contare
val dayWithExercisesAndNotes = createDayWithMix()
val streak2 = calculateStreak()
// streak dovrebbe aumentare
```

---

## 📌 Conclusione

Il flag `countsAsActivity` è una soluzione elegante e scalabile per distinguere contenuti che contribuiscono al fitness tracking da altri tipi di dati. Permette all'app di evolversi aggiungendo nuove funzionalità (note, promemoria, misurazioni) senza compromettere l'integrità del sistema di streak e statistiche.

**Status**: ✅ Implementato e pronto all'uso  
**Backward Compatibility**: ✅ 100% compatibile  
**Future-Proof**: ✅ Pronto per estensioni
