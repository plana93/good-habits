# 🐛 Bug: Conteggio Squat non si decrementa per eliminazione Workout

## 📋 Descrizione Problema
Quando si elimina un workout che contiene esercizi squat, il conteggio totale degli squat nella dashboard **NON viene decrementato correttamente**.

## ✅ Funziona Correttamente
- ✅ Aggiunta esercizi squat individuali → conteggio incrementa
- ✅ Eliminazione esercizi squat individuali → conteggio decrementa
- ✅ Aggiunta workout contenenti squat → conteggio incrementa

## ❌ NON Funziona
- ❌ **Eliminazione workout contenenti squat → conteggio NON decrementa**

## 🔍 Analisi Tecnica

### Struttura Dati
```
Workout (parentWorkoutId=null, exerciseId=null)
├── Esercizio 1: Push-up (exerciseId=1)
├── Esercizio 2: Squat (exerciseId=3, isSquat=true)
└── Esercizio 3: Burpee (exerciseId=5)
```

### Problema Identificato
La funzione `removeItemFromSession()` nel `DailySessionRepository` dovrebbe:
1. ✅ Riconoscere che l'item eliminato è un workout (exerciseId=null)
2. ✅ Controllare se il workout contiene esercizi squat usando `getItemsByParentWorkout()`
3. ❌ **Invalidare correttamente la cache Room per il conteggio squat**

### Logica Implementata
```kotlin
// In DailySessionRepository.kt
if (deletedItem.exerciseId == null) {
    // È un workout - controllo contenuto
    val workoutItems = dailySessionDao.getItemsByParentWorkout(deletedItem.id!!)
    val containsSquat = workoutItems.any { 
        exerciseRepository.isSquatExercise(it.exerciseId ?: 0L) 
    }
    
    if (containsSquat) {
        Log.d(TAG, "🏋️ Workout conteneva squat - invalidando cache")
        dailySessionDao.invalidateSquatCountCache()
    }
}
```

## 🧪 Test Case per Riproduzione
1. Avviare app e andare su "Today"
2. Notare conteggio squat corrente nella dashboard
3. Aggiungere workout "Upper Body" o "Cardio Blast" (contengono squat)
4. ✅ Verificare che conteggio squat aumenta
5. Long press sul workout aggiunto → "Elimina"
6. ❌ **BUG**: Conteggio squat rimane invariato invece di decrementare

## 🔧 Possibili Cause
1. **Room Cache**: `invalidateSquatCountCache()` potrebbe non funzionare correttamente
2. **Flow Reattivo**: Il Flow `getTodaySquatCount()` potrebbe non reagire all'invalidazione
3. **Transaction Scope**: L'invalidazione potrebbe avvenire prima del commit della transazione
4. **Query Timing**: La query `getItemsByParentWorkout()` potrebbe eseguire prima dell'eliminazione

## 📝 Soluzioni da Investigare
1. **Forzare refresh manuale** del Flow dopo eliminazione workout
2. **Usare @Transaction** per garantire atomicità operazione + invalidazione
3. **Implementare trigger SQL** per invalidazione automatica
4. **Sostituire invalidazione cache** con ricalcolo diretto conteggio

## 📊 Status
- **Priorità**: Media (funzionalità core ma edge case)
- **Impact**: UX - utenti vedono conteggio squat non aggiornato
- **Workaround**: Eliminare singoli esercizi squat invece del workout intero
- **Stato**: Bug identificato, logica implementata ma non funzionante

---
*Documentato il: 24 dicembre 2025*
*Ultimo test: Conteggio squat persiste dopo eliminazione workout*